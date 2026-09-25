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

Usage : fiches.py URL
  PDF  → texte mis en page (pdftotext -layout), page par page ;
  HTML → liens de la page (pour trouver les PDF), puis son texte.
"""

import html.parser
import subprocess
import sys
import tempfile
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


def main():
    adresse = sys.argv[1]
    finale, type_, corps = telecharger(adresse)
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


if __name__ == "__main__":
    main()
