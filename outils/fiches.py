#!/usr/bin/env python3
"""
Lecture des fiches techniques des fabricants (Ilford, Kodak, Foma, Adox…).

Les temps, dilutions et températures du Labo doivent venir des fiches que
les fabricants publient eux-mêmes, jamais d'une base recopiée. L'environnement
de développement n'atteint pas leurs sites : ce script tourne sur GitHub
Actions et imprime le texte de la fiche dans le journal du job, où on le relit
avant de recopier le moindre chiffre dans core. Les fiches restent la
propriété de leurs auteurs : le dépôt ne garde que leur adresse
(outils/fiches.txt), qui sert aussi de bibliographie au Labo.

Usage : fiches.py [LISTE]   (outils/fiches.txt par défaut)
  PDF  → texte mis en page (pdftotext -layout), page par page ;
  HTML → liens de la page (pour trouver les PDF), puis son texte.
Un site qui refuse les robots (Ilford répond 403) est lu dans la copie
qu'en garde la Wayback Machine : c'est le même PDF, daté par l'archive.
"""

import html.parser
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request

NAVIGATEUR = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0 Safari/537.36"
)


class Page(html.parser.HTMLParser):
    """Liens et texte visible d'une page, sans scripts ni styles."""

    def __init__(self, base):
        super().__init__()
        self.base, self.liens, self.texte = base, [], []
        self.lien, self.cache = None, 0

    def handle_starttag(self, balise, attributs):
        if balise in ("script", "style", "noscript"):
            self.cache += 1
        elif balise == "a":
            href = dict(attributs).get("href")
            if href and not href.startswith(("#", "javascript:", "mailto:")):
                self.lien = [urllib.parse.urljoin(self.base, href), ""]

    def handle_endtag(self, balise):
        if balise in ("script", "style", "noscript"):
            self.cache = max(0, self.cache - 1)
        elif balise == "a" and self.lien:
            self.liens.append(self.lien)
            self.lien = None

    def handle_data(self, donnees):
        if self.cache:
            return
        morceau = " ".join(donnees.split())
        if morceau:
            self.texte.append(morceau)
            if self.lien:
                self.lien[1] = (self.lien[1] + " " + morceau).strip()


def telecharger(adresse):
    requete = urllib.request.Request(adresse, headers={"User-Agent": NAVIGATEUR, "Accept": "*/*"})
    with urllib.request.urlopen(requete, timeout=60) as reponse:
        return reponse.geturl(), reponse.headers.get("Content-Type", ""), reponse.read()


def lire(adresse):
    try:
        return telecharger(adresse)
    except urllib.error.HTTPError as e:
        if e.code not in (403, 429, 503):
            raise
        print(f"{adresse} : {e.code}, lecture dans la Wayback Machine")
        # « id_ » demande le fichier tel qu'archivé, sans le bandeau de l'archive.
        return telecharger(f"https://web.archive.org/web/2026id_/{adresse}")


def afficher(adresse):
    finale, type_, corps = lire(adresse)
    print(f"Adresse : {adresse}")
    if finale != adresse:
        print(f"Redirigée vers : {finale}")
    print(f"Type : {type_} · {len(corps)} octets\n")
    if corps[:5] == b"%PDF-":
        with tempfile.NamedTemporaryFile(suffix=".pdf") as f:
            f.write(corps)
            f.flush()
            infos = subprocess.run(["pdfinfo", f.name], capture_output=True, text=True).stdout
            print(infos)
            texte = subprocess.run(["pdftotext", "-layout", f.name, "-"], capture_output=True, text=True).stdout
        for n, page in enumerate(texte.split("\f"), 1):
            if page.strip():
                print(f"──────── page {n} ────────")
                print(page.rstrip())
        return
    page = Page(finale)
    page.feed(corps.decode("utf-8", errors="replace"))
    vus = set()
    print("──────── liens ────────")
    for href, texte in page.liens:
        if href not in vus:
            vus.add(href)
            print(f"{texte[:80]:80} {href}")
    print("──────── texte ────────")
    print("\n".join(page.texte))


def main():
    racine = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    liste = sys.argv[1] if len(sys.argv) > 1 else os.path.join(racine, "outils", "fiches.txt")
    with open(liste, encoding="utf-8") as f:
        adresses = [l.split("#")[0].split()[0] for l in f if l.split("#")[0].strip()]
    echecs = 0
    for adresse in adresses:
        print(f"\n════════════════ {adresse}")
        try:
            afficher(adresse)
        except Exception as e:  # une fiche en échec ne doit pas bloquer les autres
            echecs += 1
            print(f"ÉCHEC {adresse} : {e}")
    print(f"\n{len(adresses)} adresses, {echecs} échecs")


if __name__ == "__main__":
    main()
