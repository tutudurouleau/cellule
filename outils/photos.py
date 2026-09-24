#!/usr/bin/env python3
"""
Photos du matériel, depuis Wikimedia Commons.

Tourne sur les machines de GitHub : l'environnement de développement
n'atteint pas Commons. Pour chaque fiche de outils/photos.json :

- sans « choix » : cherche des photos sous licence libre et en dépose des
  vignettes dans outils/candidats/<id>/, avec leurs métadonnées, pour qu'un
  humain regarde et choisisse ;
- avec « choix » (un titre « File:… ») : télécharge la photo en 960 px dans
  app/src/main/assets/photos/<id>.<ext>, et note auteur, licence et source
  dans app/src/main/assets/photos/credits.json ;
- avec « aucune » : ni candidates ni photo, la fiche garde sa silhouette.

Seules les licences libres passent : domaine public, CC0, CC BY, CC BY-SA.
L'application affiche toujours le crédit à côté de la photo.
"""

import html
import json
import os
import re
import shutil
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LISTE = os.path.join(RACINE, "outils", "photos.json")
CANDIDATS = os.path.join(RACINE, "outils", "candidats")
PHOTOS = os.path.join(RACINE, "app", "src", "main", "assets", "photos")
CREDITS = os.path.join(PHOTOS, "credits.json")

API = "https://commons.wikimedia.org/w/api.php"
AGENT = "CelluleApp/1.0 (https://github.com/tutudurouleau/cellule)"
LIBRE = re.compile(r"^(cc0|public domain|pd\b|pd-|cc by(-sa)? \d)", re.IGNORECASE)
NOMBRE_DE_CANDIDATES = 6
FICHIERS_PAR_CATEGORIE = 4
LARGEUR_VIGNETTE = 250   # tailles standard de Commons : d'autres sont refusées
LARGEUR_PHOTO = 960


def lire(url):
    requete = urllib.request.Request(url, headers={"User-Agent": AGENT})
    for essai in range(4):
        try:
            with urllib.request.urlopen(requete, timeout=60) as reponse:
                return reponse.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 502, 503) and essai < 3:
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


def texte(meta, cle, longueur=160):
    valeur = str(meta.get(cle, {}).get("value", ""))
    valeur = re.sub(r"<[^>]+>", " ", valeur)
    valeur = re.sub(r"\s+", " ", html.unescape(valeur)).strip()
    return valeur[:longueur]


def licence_libre(info):
    nom = texte(info.get("extmetadata", {}), "LicenseShortName")
    return nom if LIBRE.match(nom) else None


def extension(url):
    fin = url.split("?", 1)[0].rsplit("/", 1)[-1].rsplit(".", 1)[-1].lower()
    return "jpg" if fin in ("jpg", "jpeg") else fin


FORMATS = ("jpg", "png", "webp")  # Android décode les trois


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


def chercher_candidates(ident, recherches):
    dossier = os.path.join(CANDIDATS, ident)
    index = os.path.join(dossier, "candidats.json")
    if os.path.exists(index):
        with open(index, encoding="utf-8") as f:
            deja = json.load(f)
        if deja.get("recherche") == recherches and deja.get("candidates"):
            return  # déjà fait avec les mêmes recherches
        shutil.rmtree(dossier)

    vus, retenues = set(), []
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
                if rejets["licence"] == 1:
                    nom = texte(info.get("extmetadata", {}), "LicenseShortName")
                    print(f"  licence refusée : « {nom} » ({titre})", file=sys.stderr)
                continue
            if "thumburl" not in info or extension(info["thumburl"]) not in FORMATS:
                rejets["format"] += 1
                if rejets["format"] == 1:
                    print(f"  format refusé : {info.get('thumburl')} ({titre})", file=sys.stderr)
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
        print(f"  « {recherche} » : {len(pages)} résultat(s), refus {rejets}")
        if len(retenues) >= NOMBRE_DE_CANDIDATES:
            break
        time.sleep(0.5)

    os.makedirs(dossier, exist_ok=True)
    for i, candidate in enumerate(retenues):
        nom = f"{i}.{extension(candidate['vignette'])}"
        with open(os.path.join(dossier, nom), "wb") as f:
            f.write(lire(candidate["vignette"]))
        candidate["fichier"] = nom
        time.sleep(0.3)
    with open(index, "w", encoding="utf-8") as f:
        json.dump({"recherche": recherches, "candidates": retenues}, f, ensure_ascii=False, indent=1)
    print(f"{ident} : {len(retenues)} candidate(s)")


def telecharger_photo(ident, titre, credits):
    reponse = api(titles=titre, iiurlwidth=str(LARGEUR_PHOTO), **INFOS_IMAGE)
    page = reponse["query"]["pages"][0]
    info = (page.get("imageinfo") or [{}])[0]
    licence = licence_libre(info)
    if not licence:
        print(f"{ident} : « {titre} » introuvable ou sous licence non libre, ignorée", file=sys.stderr)
        return
    url = info.get("thumburl") or info["url"]
    if extension(url) not in FORMATS:
        print(f"{ident} : format inattendu {url}, ignorée", file=sys.stderr)
        return
    fichier = f"{ident}.{extension(url)}"
    deja = credits.get(ident)
    if not (deja and deja.get("titre") == titre and os.path.exists(os.path.join(PHOTOS, fichier))):
        with open(os.path.join(PHOTOS, fichier), "wb") as f:
            f.write(lire(url))
        print(f"{ident} : photo téléchargée")
    credits[ident] = {
        "fichier": fichier,
        "titre": titre,
        "auteur": auteur(info) or "auteur inconnu",
        "licence": licence,
        "source": info.get("descriptionurl", ""),
    }


def main():
    with open(LISTE, encoding="utf-8") as f:
        liste = json.load(f)
    os.makedirs(PHOTOS, exist_ok=True)
    credits = {}
    if os.path.exists(CREDITS):
        with open(CREDITS, encoding="utf-8") as f:
            credits = json.load(f)

    for ident, entree in liste.items():
        try:
            if entree.get("aucune"):
                continue
            if entree.get("choix"):
                telecharger_photo(ident, entree["choix"], credits)
            elif entree.get("recherche"):
                chercher_candidates(ident, entree["recherche"])
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
