#!/usr/bin/env python3
"""
Inventaire des fichiers de Wikimedia Commons rangés dans les catégories du
matériel de tournage (caméras de cinéma, objectifs, marques).

La recherche plein texte de Commons confond une RED Komodo avec un varan et
un Cooke S7 avec un Boeing : parcourir les catégories donne la liste de ce
qui existe vraiment, dans laquelle on repère ensuite les bonnes photos à
proposer comme candidates (clé « fichiers » de outils/photos.json).

Lit outils/inventaire.json : {"racines": [...], "profondeur": n}.
Écrit outils/inventaire-commons.tsv : catégorie, fichier.
"""

import hashlib
import json
import os
import re
import sys
import time
import urllib.parse

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from photos import API, lire  # noqa: E402

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REGLAGES = os.path.join(RACINE, "outils", "inventaire.json")
SORTIE = os.path.join(RACINE, "outils", "inventaire-commons.tsv")
PLAFOND = 12000  # fichiers au plus : certaines catégories débordent loin du sujet
# Les photos prises AVEC un objectif ou une caméra ne le montrent pas.
HORS_SUJET = re.compile(r"taken with|photographs by|shot with|shot on|videos|screenshots|logos|works by", re.IGNORECASE)


def membres(categorie):
    suite = {}
    while True:
        parametres = dict(
            action="query", format="json", formatversion="2", list="categorymembers",
            cmtitle=categorie, cmtype="file|subcat", cmlimit="500", **suite,
        )
        reponse = json.loads(lire(API + "?" + urllib.parse.urlencode(parametres)))
        for m in reponse.get("query", {}).get("categorymembers", []):
            yield m
        if "continue" not in reponse:
            return
        suite = {"cmcontinue": reponse["continue"]["cmcontinue"]}
        time.sleep(0.2)


def main():
    with open(REGLAGES, "rb") as f:
        brut = f.read()
    # Parcourir 9 000 fichiers prend plusieurs minutes : on ne recommence que
    # si la liste des catégories a changé depuis le dernier inventaire.
    empreinte = "# " + hashlib.sha256(brut).hexdigest()
    if os.path.exists(SORTIE):
        with open(SORTIE, encoding="utf-8") as f:
            if f.readline().strip() == empreinte:
                print("Inventaire à jour.")
                return
    reglages = json.loads(brut)
    profondeur = reglages.get("profondeur", 3)
    a_voir = [(c, 0) for c in reglages["racines"]]
    vues, fichiers = set(), []
    while a_voir and len(fichiers) < PLAFOND:
        categorie, niveau = a_voir.pop(0)
        if categorie in vues:
            continue
        vues.add(categorie)
        try:
            for m in membres(categorie):
                if m["ns"] == 14 and niveau < profondeur and not HORS_SUJET.search(m["title"]):
                    a_voir.append((m["title"], niveau + 1))
                elif m["ns"] == 6 and not m["title"].lower().endswith((".svg", ".pdf", ".ogv", ".webm")):
                    fichiers.append((categorie, m["title"]))
        except Exception as e:  # une catégorie en échec ne doit pas bloquer les autres
            print(f"{categorie} : {e}", file=sys.stderr)
        time.sleep(0.2)
    uniques = {}
    for categorie, titre in fichiers:
        uniques.setdefault(titre, categorie)
    with open(SORTIE, "w", encoding="utf-8") as f:
        f.write(empreinte + "\n")
        for titre, categorie in sorted(uniques.items(), key=lambda x: (x[1], x[0])):
            f.write(f"{categorie}\t{titre}\n")
    print(f"{len(vues)} catégories, {len(uniques)} fichiers")


if __name__ == "__main__":
    main()
