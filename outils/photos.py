#!/usr/bin/env python3
"""
Photos du matériel, depuis Wikimedia Commons et Openverse.

Tourne sur les machines de GitHub : l'environnement de développement
n'atteint ni Commons ni Openverse. Openverse rassemble les photos sous
licence libre d'autres sites, Flickr surtout, où le matériel de tournage
est bien plus photographié que sur Commons. Pour chaque fiche de
outils/photos.json :

- sans « choix » : cherche des photos sous licence libre et en dépose des
  vignettes dans outils/candidats/<id>/, avec leurs métadonnées, pour qu'un
  humain regarde et choisisse. « recherche » sert à Commons (syntaxe
  intitle: permise), « openverse » à Openverse (texte simple) ;
- avec « choix » : un titre « File:… » de Commons ou un identifiant
  « openverse:… ». La photo est téléchargée, ramenée à 960 px de large,
  rangée dans app/src/main/assets/photos/<id>.jpg, et son auteur, sa
  licence et sa source notés dans app/src/main/assets/photos/credits.json ;
- avec « aucune » : ni candidates ni photo, la fiche garde sa silhouette.

Seules les licences libres passent : domaine public, CC0, CC BY, CC BY-SA.
L'application affiche toujours le crédit à côté de la photo.
"""

import html
import io
import json
import os
import re
import shutil
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LISTE = os.path.join(RACINE, "outils", "photos.json")
CANDIDATS = os.path.join(RACINE, "outils", "candidats")
PHOTOS = os.path.join(RACINE, "app", "src", "main", "assets", "photos")
CREDITS = os.path.join(PHOTOS, "credits.json")

API = "https://commons.wikimedia.org/w/api.php"
OPENVERSE = "https://api.openverse.org/v1/images/"
AGENT = "CelluleApp/1.0 (https://github.com/tutudurouleau/cellule)"
LIBRE = re.compile(r"^(cc0|public domain|pd\b|pd-|cc by(-sa)? \d)", re.IGNORECASE)
LICENCES_OPENVERSE = {"cc0": "CC0", "pdm": "domaine public", "by": "CC BY", "by-sa": "CC BY-SA"}
PLATEFORMES = {"flickr": "Flickr", "wikimedia": "Wikimedia Commons"}
NOMBRE_DE_CANDIDATES = 6     # par source
FICHIERS_PAR_CATEGORIE = 4
LARGEUR_VIGNETTE = 250       # tailles standard de Commons : d'autres sont refusées
LARGEUR_PHOTO = 960
PAUSE_OPENVERSE = 4          # secondes : l'accès anonyme est limité par minute et par jour


class Limite(Exception):
    """Openverse refuse de répondre davantage aujourd'hui : on garde le reste pour plus tard."""


def lire(url):
    requete = urllib.request.Request(url, headers={"User-Agent": AGENT})
    for essai in range(4):
        try:
            with urllib.request.urlopen(requete, timeout=60) as reponse:
                return reponse.read()
        except urllib.error.HTTPError as e:
            # Openverse compte chaque essai contre sa limite : un 429 de sa
            # part arrête la recherche au lieu d'insister.
            reessayer = e.code in (500, 502, 503) or (e.code == 429 and not url.startswith(OPENVERSE))
            if reessayer and essai < 3:
                time.sleep(5 * (essai + 1))
                continue
            raise


def api(**parametres):
    parametres.update(action="query", format="json", formatversion="2")
    reponse = json.loads(lire(API + "?" + urllib.parse.urlencode(parametres)))
    # Une erreur d'API arrive en HTTP 200 : sans ce contrôle, elle passerait
    # pour une recherche sans résultat.
    for cle in ("error", "warnings"):
        if cle in reponse:
            print(f"  API {cle} : {json.dumps(reponse[cle], ensure_ascii=False)[:300]}", file=sys.stderr)
    return reponse


def openverse(chemin, **parametres):
    url = OPENVERSE + chemin + ("?" + urllib.parse.urlencode(parametres) if parametres else "")
    try:
        reponse = json.loads(lire(url))
    except urllib.error.HTTPError as e:
        if e.code == 429:
            raise Limite() from e
        raise
    time.sleep(PAUSE_OPENVERSE)
    return reponse


def texte(meta, cle, longueur=160):
    valeur = str(meta.get(cle, {}).get("value", ""))
    valeur = re.sub(r"<[^>]+>", " ", valeur)
    valeur = re.sub(r"\s+", " ", html.unescape(valeur)).strip()
    return valeur[:longueur]


def licence_libre(info):
    nom = texte(info.get("extmetadata", {}), "LicenseShortName")
    return nom if LIBRE.match(nom) else None


def licence_openverse(resultat):
    nom = LICENCES_OPENVERSE.get(resultat.get("license", ""))
    if nom and nom.startswith("CC BY") and resultat.get("license_version"):
        nom += " " + resultat["license_version"]
    return nom


def extension(url):
    fin = url.split("?", 1)[0].rsplit("/", 1)[-1].rsplit(".", 1)[-1].lower()
    return "jpg" if fin in ("jpg", "jpeg") else fin


FORMATS = ("jpg", "png", "webp")  # Android décode les trois


def en_jpeg(donnees, largeur):
    """Ramène une image à la largeur voulue (sans l'agrandir), en JPEG."""
    image = Image.open(io.BytesIO(donnees))
    image = image.convert("RGB")
    if image.width > largeur:
        image = image.resize((largeur, round(image.height * largeur / image.width)), Image.LANCZOS)
    sortie = io.BytesIO()
    image.save(sortie, "JPEG", quality=85, optimize=True)
    return sortie.getvalue()


INFOS_IMAGE = dict(prop="imageinfo", iiprop="url|extmetadata|size|mime|user")


def pages_trouvees(recherche):
    """Les fichiers des catégories dont le nom répond à la recherche, puis ceux
    de la recherche plein texte. Les catégories de Commons (« Category:Cooke
    S4/i », etc.) sont bien plus sûres que le texte libre, qui ramène des
    avions S7 pour un objectif Cooke S7."""
    pages = []
    categories = api(list="search", srsearch=recherche, srnamespace="14", srlimit="2")
    for categorie in categories.get("query", {}).get("search", []):
        membres = api(
            generator="categorymembers", gcmtitle=categorie["title"], gcmtype="file",
            gcmlimit=str(FICHIERS_PAR_CATEGORIE), iiurlwidth=str(LARGEUR_VIGNETTE), **INFOS_IMAGE,
        )
        trouves = membres.get("query", {}).get("pages", [])
        print(f"  {categorie['title']} : {len(trouves)} fichier(s)")
        pages += trouves
    reponse = api(
        generator="search", gsrsearch=f"{recherche} filetype:bitmap",
        gsrnamespace="6", gsrlimit="10", iiurlwidth=str(LARGEUR_VIGNETTE), **INFOS_IMAGE,
    )
    pages += sorted(reponse.get("query", {}).get("pages", []), key=lambda p: p.get("index", 0))
    return pages


def auteur(info):
    """L'auteur déclaré ou, à défaut, le compte qui a versé le fichier."""
    declare = texte(info.get("extmetadata", {}), "Artist", 80)
    if re.match(r"unknown", declare, re.IGNORECASE):
        return "auteur inconnu"
    return declare or info.get("user", "")


def candidates_commons(recherches, vus):
    retenues = []
    for recherche in recherches:
        pages = pages_trouvees(recherche)
        rejets = {"licence": 0, "format": 0}
        for page in pages:
            titre = page["title"]
            if titre in vus or titre.lower().endswith(".svg"):
                continue
            vus.add(titre)
            info = (page.get("imageinfo") or [{}])[0]
            licence = licence_libre(info)
            if not licence:
                rejets["licence"] += 1
                continue
            if "thumburl" not in info or extension(info["thumburl"]) not in FORMATS:
                rejets["format"] += 1
                continue
            meta = info.get("extmetadata", {})
            retenues.append({
                "titre": titre,
                "licence": licence,
                "auteur": auteur(info),
                "description": texte(meta, "ImageDescription", 200),
                "taille": f"{info.get('width')}x{info.get('height')}",
                "vignette": info["thumburl"],
            })
            if len(retenues) >= NOMBRE_DE_CANDIDATES:
                break
        print(f"  Commons « {recherche} » : {len(pages)} résultat(s), refus {rejets}")
        if len(retenues) >= NOMBRE_DE_CANDIDATES:
            break
        time.sleep(0.5)
    return retenues


def candidates_openverse(recherches, vus):
    retenues = []
    for recherche in recherches:
        reponse = openverse("", q=recherche, license="by,by-sa,cc0,pdm", page_size="20")
        resultats = reponse.get("results", [])
        for r in resultats:
            titre = "openverse:" + r["id"]
            # Commons est déjà fouillé directement ; ses fichiers vus par
            # Openverse feraient doublon.
            if titre in vus or r.get("source") == "wikimedia":
                continue
            vus.add(titre)
            licence = licence_openverse(r)
            if not licence or not r.get("url"):
                continue
            retenues.append({
                "titre": titre,
                "licence": licence,
                "auteur": (r.get("creator") or "")[:80],
                "description": f"{r.get('title') or ''} ({PLATEFORMES.get(r.get('source'), r.get('source'))})"[:200],
                "taille": f"{r.get('width')}x{r.get('height')}",
                "vignette": r["url"],
            })
            if len(retenues) >= NOMBRE_DE_CANDIDATES:
                break
        print(f"  Openverse « {recherche} » : {reponse.get('result_count', 0)} résultat(s)")
        if len(retenues) >= NOMBRE_DE_CANDIDATES:
            break
    return retenues


def chercher_candidates(ident, entree, etat):
    dossier = os.path.join(CANDIDATS, ident)
    index = os.path.join(dossier, "candidats.json")
    demande = {"recherche": entree.get("recherche", []), "openverse": entree.get("openverse", [])}
    if os.path.exists(index):
        with open(index, encoding="utf-8") as f:
            deja = json.load(f)
        faite = {"recherche": deja.get("recherche", []), "openverse": deja.get("openverse", [])}
        if faite == demande and deja.get("candidates") and deja.get("complet", True):
            return  # déjà fait avec les mêmes recherches
        shutil.rmtree(dossier)

    vus = set()
    retenues = candidates_commons(demande["recherche"], vus)
    complet = True
    if demande["openverse"]:
        if etat["limite"]:
            complet = False
        else:
            try:
                retenues += candidates_openverse(demande["openverse"], vus)
            except Limite:
                print("  Openverse : limite atteinte, la suite au prochain passage", file=sys.stderr)
                etat["limite"] = True
                complet = False

    os.makedirs(dossier, exist_ok=True)
    gardees = []
    for candidate in retenues:
        nom = f"{len(gardees)}.jpg"
        try:
            with open(os.path.join(dossier, nom), "wb") as f:
                f.write(en_jpeg(lire(candidate["vignette"]), LARGEUR_VIGNETTE))
        except Exception as e:  # une vignette illisible ne doit pas bloquer les autres
            print(f"  vignette illisible {candidate['titre']} : {e}", file=sys.stderr)
            continue
        candidate["fichier"] = nom
        gardees.append(candidate)
        time.sleep(0.3)
    with open(index, "w", encoding="utf-8") as f:
        json.dump({**demande, "complet": complet, "candidates": gardees}, f, ensure_ascii=False, indent=1)
    print(f"{ident} : {len(gardees)} candidate(s)")


def source_commons(titre):
    """Adresse, auteur, licence et page source d'un fichier de Commons."""
    reponse = api(titles=titre, iiurlwidth=str(LARGEUR_PHOTO), **INFOS_IMAGE)
    page = reponse["query"]["pages"][0]
    info = (page.get("imageinfo") or [{}])[0]
    licence = licence_libre(info)
    if not licence:
        return None
    return {
        "url": info.get("thumburl") or info["url"],
        "auteur": auteur(info) or "auteur inconnu",
        "licence": licence,
        "source": info.get("descriptionurl", ""),
        "plateforme": "Wikimedia Commons",
    }


def source_openverse(identifiant):
    r = openverse(identifiant.split(":", 1)[1] + "/")
    licence = licence_openverse(r)
    if not licence:
        return None
    return {
        "url": r["url"],
        "auteur": (r.get("creator") or "auteur inconnu")[:80],
        "licence": licence,
        "source": r.get("foreign_landing_url") or r.get("url"),
        "plateforme": PLATEFORMES.get(r.get("source"), (r.get("source") or "Openverse").capitalize()),
    }


def telecharger_photo(ident, titre, credits):
    deja = credits.get(ident)
    fichier = f"{ident}.jpg"
    chemin = os.path.join(PHOTOS, fichier)
    if deja and deja.get("titre") == titre and deja.get("plateforme") and os.path.exists(chemin):
        return  # déjà là, crédit complet : inutile de redemander
    origine = source_openverse(titre) if titre.startswith("openverse:") else source_commons(titre)
    if not origine:
        print(f"{ident} : « {titre} » introuvable ou sous licence non libre, ignorée", file=sys.stderr)
        return
    if not (deja and deja.get("titre") == titre and os.path.exists(chemin)):
        donnees = en_jpeg(lire(origine.pop("url")), LARGEUR_PHOTO)
        if deja and deja.get("fichier") != fichier:
            ancien = os.path.join(PHOTOS, deja["fichier"])
            if os.path.exists(ancien):
                os.remove(ancien)
        with open(chemin, "wb") as f:
            f.write(donnees)
        print(f"{ident} : photo téléchargée")
    origine.pop("url", None)
    credits[ident] = {"fichier": fichier, "titre": titre, **origine}


def main():
    with open(LISTE, encoding="utf-8") as f:
        liste = json.load(f)
    os.makedirs(PHOTOS, exist_ok=True)
    credits = {}
    if os.path.exists(CREDITS):
        with open(CREDITS, encoding="utf-8") as f:
            credits = json.load(f)

    etat = {"limite": False}
    for ident, entree in liste.items():
        try:
            if entree.get("aucune"):
                continue
            if entree.get("choix"):
                telecharger_photo(ident, entree["choix"], credits)
            elif entree.get("recherche") or entree.get("openverse"):
                chercher_candidates(ident, entree, etat)
        except Limite:
            print(f"{ident} : limite d'Openverse atteinte, la suite au prochain passage", file=sys.stderr)
            etat["limite"] = True
        except Exception as e:  # une fiche en échec ne doit pas bloquer les autres
            print(f"{ident} : {e}", file=sys.stderr)
        time.sleep(0.3)

    choisies = {i for i, e in liste.items() if e.get("choix") and not e.get("aucune")}
    for ident in list(credits):
        if ident not in choisies:
            chemin = os.path.join(PHOTOS, credits[ident]["fichier"])
            if os.path.exists(chemin):
                os.remove(chemin)
            del credits[ident]
    if os.path.isdir(CANDIDATS):
        for ident in os.listdir(CANDIDATS):
            if ident in choisies or ident not in liste or liste[ident].get("aucune"):
                shutil.rmtree(os.path.join(CANDIDATS, ident))

    with open(CREDITS, "w", encoding="utf-8") as f:
        json.dump(dict(sorted(credits.items())), f, ensure_ascii=False, indent=1)


if __name__ == "__main__":
    main()
