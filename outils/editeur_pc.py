#!/usr/bin/env python3
"""
Construit pc/editeur-plan.html, l'éditeur de plans au sol pour PC, en un seul
fichier qui marche hors ligne : la page source (pc/editeur-plan.src.html),
la bibliothèque de QR codes (outils/vendeur, licence MIT) et le catalogue des
projecteurs de l'appli, relu dans core/CatalogueProjecteurs*.kt.

Usage : python3 -I outils/editeur_pc.py
"""
import glob
import json
import os
import re

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def projecteurs():
    liste = []
    for f in sorted(glob.glob(os.path.join(RACINE, "core/src/main/kotlin/fr/cellule/core/CatalogueProjecteurs*.kt"))):
        s = open(f, encoding="utf-8").read()
        for m in re.finditer(r'Projecteur\(\s*"(cat-[^"]+)",\s*"([^"]+)",\s*"([^"]*)",\s*FamilleProjecteur\.(\w+)(.*?)\n    \)', s, re.S):
            corps = m.group(5)

            def v(cle):
                x = re.search(cle + r"\s*=\s*([\d.]+)", corps)
                return float(x.group(1)) if x else None

            liste.append({"id": m.group(1), "nom": m.group(2), "fab": m.group(3), "fam": m.group(4),
                          "w": v("puissanceW"), "af": v("angleFlood"), "as_": v("angleSpot")})
    return liste


def main():
    src = open(os.path.join(RACINE, "pc/editeur-plan.src.html"), encoding="utf-8").read()
    qr = open(os.path.join(RACINE, "outils/vendeur/qrcode-generator-1.4.4.js"), encoding="utf-8").read()
    p = projecteurs()
    page = src.replace("/*QRCODE*/", qr).replace("/*PROJECTEURS*/", json.dumps(p, ensure_ascii=False))
    open(os.path.join(RACINE, "pc/editeur-plan.html"), "w", encoding="utf-8").write(page)
    # La même page sans son squelette, pour la publier en Artifact (claude.ai l'ajoute).
    nu = page
    for t in ("<!doctype html>\n", '<html lang="fr">\n', "<head>\n", '<meta charset="utf-8">\n',
              '<meta name="viewport" content="width=device-width, initial-scale=1">\n', "</head>\n", "<body>\n", "</body>\n", "</html>\n"):
        nu = nu.replace(t, "", 1)
    os.makedirs(os.path.join(RACINE, "pc", "artifact"), exist_ok=True)
    open(os.path.join(RACINE, "pc/artifact/editeur-plan.html"), "w", encoding="utf-8").write(nu)
    print(f"pc/editeur-plan.html : {len(page) // 1024} Ko, {len(p)} projecteurs")


if __name__ == "__main__":
    main()
