#!/usr/bin/env python3
"""
Qui loue quoi : croise les fiches de l'appli avec les relevés des sites des
loueurs (outils/releves/<loueur>/ : texte des pages, des PDF, et plan du
site) et écrit core/.../DisponibiliteLoueurs.kt.

Une fiche est « chez » un loueur quand l'une de ses appellations apparaît
telle quelle dans ce que son site publie. Les appellations viennent du nom
de la fiche (découpé sur « et », « / », parenthèses) et, pour la machinerie
et l'électrique, de mots-clés choisis à la main (MOTS) : « C-stand » se dit
aussi « Century ».

Usage : python3 outils/dispo.py
"""

import glob
import os
import re
import unicodedata

RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CORE = os.path.join(RACINE, "core", "src", "main", "kotlin", "fr", "cellule", "core")
RELEVES = os.path.join(RACINE, "outils", "releves")
SORTIE = os.path.join(CORE, "DisponibiliteLoueurs.kt")
LOUEURS = ["tsf", "rvz", "transpalux", "panavision", "nextshot", "vantage", "cinesyl"]

MOTS = {
    "matthews-c-stand": ["c stand", "century"],
    "pied-de-projecteur-baby-et-junior": ["pied baby", "pied junior", "pied de 1000"],
    "pied-u126": ["u126"],
    "wind-up": ["wind up", "crank up", "manivelle"],
    "low-boy-et-embase-tortue": ["low boy", "turtle", "tortue"],
    "pied-a-roulettes": ["pied a roulette", "roller stand"],
    "combo-stand": ["combo"],
    "manfrotto-super-clamp": ["super clamp"],
    "matthews-mafer": ["mafer"],
    "cardellini": ["cardellini", "matthellini"],
    "gaffer-grip": ["gaffer grip"],
    "pinces-a-ressort-a-clamps": ["a clamp", "spring clamp"],
    "pinces-a-linge-c-47": ["c47", "c 47"],
    "serre-joint": ["serre joint"],
    "bec-de-canard": ["bec de canard", "duckbill", "platypus"],
    "big-ben": ["big ben"],
    "collier-d-echafaudage": ["collier", "coupleur", "scaff"],
    "plaques-baby-et-junior": ["plaque baby", "plaque junior", "wall plate", "baby plate"],
    "ventouse": ["ventouse", "suction"],
    "rotule-de-c-stand": ["rotule", "grip head", "gobo head"],
    "bras-de-rallonge": ["gobo arm", "bras de rallonge", "bras de deport"],
    "manfrotto-bras-magique": ["bras magique", "magic arm"],
    "manfrotto-autopole": ["autopole", "polecat"],
    "ecarteur-mural": ["wall spreader", "ecarteur"],
    "menace": ["menace"],
    "adaptateurs-de-spigot": ["spigot"],
    "speed-rail-et-tubes": ["speed rail", "tube alu", "tube acier"],
    "apple-boxes": ["apple box", "pancake"],
    "sac-de-lest": ["sac de lest", "sac de sable", "shot bag", "sandbag"],
    "gueuse": ["gueuse"],
    "cales-et-cales-a-gradins": ["cale", "wedge", "step block"],
    "cup-blocks": ["cup block"],
    "drapeau": ["drapeau", "flag"],
    "cutter-et-floppy": ["floppy", "cutter"],
    "filets-simple-et-double": ["filet", "scrim", "single net", "double net"],
    "cookie": ["cookie", "cucoloris"],
    "cadres-et-toiles-butterfly": ["butterfly", "12x12", "6x6", "20x20", "grid cloth"],
    "borniol": ["borniol", "duvetyne"],
    "nid-d-abeille": ["nid d abeille", "honeycomb", "egg crate"],
    "reflecteurs-et-polystyrene": ["polystyrene", "reflecteur", "ultrabounce"],
    "trepied-camera": ["trepied", "baby legs", "high hat", "hi hat"],
    "sachtler-tete-fluide": ["tete fluide", "fluid head"],
    "arri-tete-a-manivelles": ["tete a manivelle", "arrihead", "geared head"],
    "demi-boule-et-embase-de-niveau": ["demi boule", "embase de niveau", "leveling"],
    "tiffen-steadicam": ["steadicam"],
    "dji-stabilisateur-motorise-gimbal": ["ronin", "movi", "gimbal"],
    "easyrig": ["easyrig"],
    "slider": ["slider"],
    "rails-de-travelling": ["rail de travelling", "rails", "travelling"],
    "chapman-leonard-peewee": ["peewee", "pee wee"],
    "j-l-fisher-dolly-fisher": ["fisher"],
    "panther-dolly-panther": ["panther"],
    "dolly-plateau-et-western": ["doorway", "western"],
    "dolly-araignee": ["elemack", "spider dolly"],
    "louma": ["louma"],
    "technocrane": ["technocrane"],
    "jimmy-jib": ["jimmy jib"],
    "filmotechnic-grue-sur-vehicule-russian-arm": ["russian arm"],
    "car-mount-et-hostess-tray": ["hostess", "car mount"],
    "p17": ["p17"],
    "powerlock": ["powerlock"],
    "socapex": ["socapex"],
    "neutrik-powercon": ["powercon", "true1"],
    "prolongateur-p17": ["prolongateur", "prolong"],
    "triplette-et-doublette": ["triplette", "doublette"],
    "cable-de-tete-hmi": ["cable de tete", "tete hmi"],
    "enrouleur": ["enrouleur"],
    "cable-dmx": ["dmx"],
    "groupe-electrogene": ["groupe electrogene", "kva"],
    "armoire-de-distribution": ["armoire", "distribution"],
    "ballast-hmi": ["ballast"],
    "gradateur": ["gradateur", "dimmer"],
    "console-dmx": ["console"],
    "lumenradio-dmx-sans-fil-crmx": ["crmx", "lumenradio"],
}


def normal(t):
    t = "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn").lower()
    return " " + re.sub(r"[^a-z0-9]+", " ", t).strip() + " "


def slug(t):
    t = "".join(c for c in unicodedata.normalize("NFD", t) if unicodedata.category(c) != "Mn").lower()
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")


def fiches():
    """(identifiant, nom, fabricant, mots-clés) de chaque fiche du catalogue."""
    liste = []
    for f in glob.glob(os.path.join(CORE, "Catalogue*.kt")):
        texte = open(f, encoding="utf-8").read()
        for nom, fab in re.findall(r'nom = "([^"]+)",\s*fabricant = "([^"]*)"', texte):
            liste.append((nom, fab))
    texte = open(os.path.join(CORE, "Machinerie.kt"), encoding="utf-8").read()
    for nom, fab in re.findall(r'(?:machino|elec)\(\s*"([^"]+)", "([^"]*)"', texte):
        liste.append((nom, fab))
    resultat = []
    for nom, fab in liste:
        complet = nom if nom.lower().startswith(fab.lower()) else f"{fab} {nom}"
        ident = slug(complet)
        if ident in MOTS:
            mots = MOTS[ident]
        else:
            base = re.sub(r"\(([^)]*)\)", r" / \1", nom)
            alternatives = [a.strip() for a in re.split(r"\s+et\s+|/|,", base) if a.strip()]
            mots = []
            for a in alternatives:
                n = normal(a).strip()
                if len(n) < 6 or " " not in n:
                    n = normal(fab + " " + a).strip()
                mots.append(n)
        resultat.append((ident, [normal(m).strip() for m in mots if m]))
    return resultat


def corpus(loueur):
    dossier = os.path.join(RELEVES, loueur)
    morceaux = []
    for f in [os.path.join(dossier, "texte.txt"), os.path.join(dossier, "plan.txt")] + glob.glob(os.path.join(dossier, "pdf", "*.txt")):
        if os.path.exists(f):
            morceaux.append(open(f, encoding="utf-8", errors="replace").read())
    return normal(" ".join(morceaux))


def main():
    textes = {l: corpus(l) for l in LOUEURS if os.path.isdir(os.path.join(RELEVES, l))}
    dispo = {}
    for ident, mots in fiches():
        chez = [l for l, t in textes.items() if any(f" {m} " in t for m in mots)]
        if chez:
            dispo[ident] = chez
    lignes = [
        "package fr.cellule.core",
        "",
        "/*",
        " * Généré par outils/dispo.py d'après les relevés des sites des loueurs",
        " * (outils/releves/). Ne pas modifier à la main : relancer le script.",
        " */",
        "object DisponibiliteLoueurs {",
        "    val DISPO: Map<String, Set<String>> = mapOf(",
    ]
    lignes += [f'        "{i}" to setOf({", ".join(chr(34) + l + chr(34) for l in ls)}),' for i, ls in sorted(dispo.items())]
    if dispo:
        lignes[-1] = lignes[-1].rstrip(",")
    lignes += ["    )", "}", ""]
    open(SORTIE, "w", encoding="utf-8").write("\n".join(lignes))
    print(f"{len(dispo)} fiches trouvées chez au moins un loueur")
    for l in textes:
        print(f"  {l} : {sum(1 for v in dispo.values() if l in v)}")


if __name__ == "__main__":
    main()
