#!/usr/bin/env python3
"""
Relevé des catalogues des loueurs (et des documents techniques à relire).

La session de développement n'atteint pas ces sites : ce script tourne sur
GitHub Actions (workflow « Loueurs »), un loueur par job. Il parcourt le site
à partir de ses pages de départ, en privilégiant les pages de catalogue, avec
un vrai navigateur (Playwright) pour les sites qui se construisent en
JavaScript. Il garde, pour chaque page, son adresse, son titre et son texte
visible ; pour chaque PDF lié (catalogue, tarif, bon de commande), son texte.

Ce relevé sert à savoir QUI loue QUOI et sous quel nom, et à repérer les
modèles de demande de devis. Les fiches techniques de l'appli, elles,
restent tirées des fabricants.

Usage : loueurs.py ID   (une entrée de outils/loueurs.json)
Sortie : outils/releves/ID/  (pages.tsv, texte.txt, pdf/*.txt, liens-pdf.tsv)
"""

import json
import os
import re
import subprocess
import sys
import tempfile
import time
import urllib.parse
import urllib.request
from collections import Counter, deque

NAVIGATEUR = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0 Safari/537.36"
)
RACINE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

MOTS_UTILES = re.compile(
    r"catalog|produit|product|materiel|matériel|location|rental|lumi|eclair|éclair|light|"
    r"projecteur|camera|caméra|optique|objectif|lens|machin|grip|electri|électri|power|"
    r"accessoire|famille|categor|catégor|gamme|shop|devis|tarif|price|prix|commande|"
    r"hmi|led|tungsten|fresnel|dolly|grue|crane|pied|stand|clamp|cable|câble",
    re.I,
)
MOTS_INUTILES = re.compile(
    r"login|connexion|compte|account|panier|cart|wp-json|feed|xmlrpc|\.(jpg|jpeg|png|gif|webp|svg|mp4|zip)$|"
    r"facebook|instagram|linkedin|twitter|youtube|vimeo|mentions|cookies|cgv|privacy|"
    r"recrut|job|career|actualit|news|blog|presse|press",
    re.I,
)


def telecharger(adresse, delai=40):
    req = urllib.request.Request(adresse, headers={"User-Agent": NAVIGATEUR, "Accept": "*/*", "Accept-Language": "fr,en"})
    with urllib.request.urlopen(req, timeout=delai) as r:
        return r.geturl(), r.headers.get("Content-Type", ""), r.read()


def texte_pdf(corps):
    with tempfile.NamedTemporaryFile(suffix=".pdf") as f:
        f.write(corps)
        f.flush()
        return subprocess.run(["pdftotext", "-layout", f.name, "-"], capture_output=True, text=True).stdout


class Navigateur:
    """Playwright, ouvert à la demande : seulement si une page est vide sans lui."""

    def __init__(self):
        self.pw = self.nav = None

    def page(self, adresse):
        if self.nav is None:
            from playwright.sync_api import sync_playwright
            self.pw = sync_playwright().start()
            self.nav = self.pw.chromium.launch()
        p = self.nav.new_page(user_agent=NAVIGATEUR, locale="fr-FR")
        try:
            p.goto(adresse, timeout=45000, wait_until="domcontentloaded")
            try:
                p.wait_for_load_state("networkidle", timeout=12000)
            except Exception:
                pass
            titre = p.title()
            texte = p.inner_text("body")
            liens = p.eval_on_selector_all(
                "a[href]", "els => els.map(e => [e.href, (e.innerText || '').trim().slice(0, 120)])"
            )
            return titre, texte, liens
        finally:
            p.close()

    def fermer(self):
        if self.nav:
            self.nav.close()
            self.pw.stop()


def lire_html(corps, base):
    from html.parser import HTMLParser

    class P(HTMLParser):
        def __init__(self):
            super().__init__()
            self.liens, self.texte, self.cache, self.lien, self.titre, self.dans_titre = [], [], 0, None, "", False

        def handle_starttag(self, b, a):
            if b in ("script", "style", "noscript", "svg"):
                self.cache += 1
            elif b == "title":
                self.dans_titre = True
            elif b == "a":
                h = dict(a).get("href")
                if h and not h.startswith(("#", "javascript:", "mailto:", "tel:")):
                    self.lien = [urllib.parse.urljoin(base, h), ""]
            if b in ("br", "p", "div", "li", "tr", "h1", "h2", "h3", "h4", "td", "section", "article"):
                self.texte.append("\n")

        def handle_endtag(self, b):
            if b in ("script", "style", "noscript", "svg"):
                self.cache = max(0, self.cache - 1)
            elif b == "title":
                self.dans_titre = False
            elif b == "a" and self.lien:
                self.liens.append(self.lien)
                self.lien = None

        def handle_data(self, d):
            if self.dans_titre:
                self.titre += d.strip()
            if self.cache:
                return
            m = " ".join(d.split())
            if m:
                self.texte.append(m + " ")
                if self.lien:
                    self.lien[1] = (self.lien[1] + " " + m).strip()

    p = P()
    p.feed(corps.decode("utf-8", errors="replace"))
    texte = re.sub(r"\n\s*\n+", "\n", "".join(p.texte))
    return p.titre, texte, p.liens


def normaliser(adresse):
    u = urllib.parse.urlsplit(adresse)
    requete = urllib.parse.urlencode(
        [(k, v) for k, v in urllib.parse.parse_qsl(u.query) if not k.startswith(("utm_", "fbclid", "gclid"))]
    )
    return urllib.parse.urlunsplit((u.scheme, u.netloc.lower(), u.path.rstrip("/") or "/", requete, ""))


def score(adresse, texte_lien):
    s = 0
    if MOTS_UTILES.search(adresse):
        s += 2
    if MOTS_UTILES.search(texte_lien or ""):
        s += 1
    return s


def relever(entree):
    ident = entree["id"]
    sortie = os.path.join(RACINE, "outils", "releves", ident)
    os.makedirs(os.path.join(sortie, "pdf"), exist_ok=True)
    domaines = set(entree.get("domaines", []))
    maximum = entree.get("pages", 220)
    limite_temps = time.time() + entree.get("minutes", 40) * 60
    file = deque()
    vus = set()
    for d in entree["depart"]:
        domaines.add(urllib.parse.urlsplit(d).netloc.lower().removeprefix("www."))
        file.append((10, d))
    nav = Navigateur()
    pages, pdfs, journal = [], [], []

    def dans_le_site(adresse):
        h = urllib.parse.urlsplit(adresse).netloc.lower().removeprefix("www.")
        return any(h == d or h.endswith("." + d) for d in domaines)

    while file and len(pages) < maximum and time.time() < limite_temps:
        # La page la plus prometteuse d'abord.
        file = deque(sorted(file, key=lambda x: -x[0]))
        _, adresse = file.popleft()
        cle = normaliser(adresse)
        if cle in vus:
            continue
        vus.add(cle)
        try:
            finale, type_, corps = telecharger(adresse)
        except Exception as e:
            journal.append(f"ÉCHEC {adresse} : {e}")
            # Un site qui refuse urllib accepte parfois un vrai navigateur.
            if not pages and entree.get("navigateur", True):
                try:
                    titre, texte, liens = nav.page(adresse)
                    pages.append((adresse, titre, texte))
                    for h, t in liens:
                        if dans_le_site(h) and not MOTS_INUTILES.search(h):
                            file.append((score(h, t), h))
                except Exception as e2:
                    journal.append(f"ÉCHEC navigateur {adresse} : {e2}")
            continue
        if corps[:5] == b"%PDF-" or "pdf" in type_:
            n = len(pdfs) + 1
            try:
                t = texte_pdf(corps)
            except Exception as e:
                t = f"(illisible : {e})"
            with open(os.path.join(sortie, "pdf", f"{n:03d}.txt"), "w", encoding="utf-8") as f:
                f.write(f"# {adresse}\n\n{t}")
            pdfs.append((adresse, n))
            continue
        if "html" not in type_:
            continue
        titre, texte, liens = lire_html(corps, finale)
        # Peu de texte : le site se construit en JavaScript, on le rouvre dans un navigateur.
        if len(texte.strip()) < 400 and entree.get("navigateur", True):
            try:
                titre, texte, liens = nav.page(finale)
            except Exception as e:
                journal.append(f"navigateur {finale} : {e}")
        pages.append((finale, titre, texte))
        for h, t in liens:
            if h.lower().endswith(".pdf") and dans_le_site(h) or h.lower().endswith(".pdf") and re.search(r"tarif|devis|commande|catalog|location|rental", h + t, re.I):
                file.append((20, h))
            elif dans_le_site(h) and not MOTS_INUTILES.search(h):
                file.append((score(h, t), h))
        time.sleep(entree.get("pause", 0.4))
    nav.fermer()

    # Les lignes répétées sur beaucoup de pages (menus, pieds de page) ne disent rien.
    compte = Counter()
    for _, _, t in pages:
        compte.update(set(l.strip() for l in t.splitlines() if l.strip()))
    seuil = max(4, len(pages) // 4)
    with open(os.path.join(sortie, "texte.txt"), "w", encoding="utf-8") as f:
        for adresse, titre, t in pages:
            lignes = [l.strip() for l in t.splitlines() if l.strip() and compte[l.strip()] < seuil]
            if not lignes:
                continue
            f.write(f"\n════ {titre} · {adresse}\n")
            f.write("\n".join(lignes)[:20000] + "\n")
    with open(os.path.join(sortie, "pages.tsv"), "w", encoding="utf-8") as f:
        for adresse, titre, _ in pages:
            f.write(f"{adresse}\t{titre}\n")
    with open(os.path.join(sortie, "liens-pdf.tsv"), "w", encoding="utf-8") as f:
        for adresse, n in pdfs:
            f.write(f"{n:03d}\t{adresse}\n")
    with open(os.path.join(sortie, "journal.txt"), "w", encoding="utf-8") as f:
        f.write(f"{len(pages)} pages, {len(pdfs)} PDF, {len(vus)} adresses vues\n")
        f.write("\n".join(journal[:300]))
    print(f"{ident} : {len(pages)} pages, {len(pdfs)} PDF")


def main():
    with open(os.path.join(RACINE, "outils", "loueurs.json"), encoding="utf-8") as f:
        entrees = {e["id"]: e for e in json.load(f)}
    if len(sys.argv) > 1 and sys.argv[1] == "--liste":
        print(json.dumps(list(entrees)))
        return
    relever(entrees[sys.argv[1]])


if __name__ == "__main__":
    main()
