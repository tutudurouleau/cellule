# Échelle de lumière — base physique et repères de terrain

Document de référence pour calibrer l'œil sur des valeurs réelles : de la lumière incidente en lux jusqu'à l'exposition finale sur pellicule ou capteur.

> **Version relue.** Les valeurs ont été recalculées une à une ; six erreurs ont été corrigées et sont signalées en note à l'endroit où elles se trouvaient. Le récapitulatif est en fin de fichier. C'est cette version qui sert de source aux tables de l'application.

---

## 1. Les trois grandeurs, et le pont entre elles

Tout le système repose sur trois quantités distinctes qu'il faut arrêter de confondre.

| Grandeur | Unité | Ce que c'est | Ce qui la mesure |
|---|---|---|---|
| **Éclairement** (illuminance) | lux (lx) | La lumière **qui arrive** sur une surface | Cellule incidente (dôme blanc) |
| **Luminance** | cd/m² | La lumière **qui repart** d'une surface vers l'œil | Spotmètre / cellule réfléchie |
| **Réflectance** (albédo) | ρ, sans unité | La fraction renvoyée par le matériau | Table de matériaux |

**L'équation qui relie les trois** (surface lambertienne, diffusion parfaite) :

```
L = (E × ρ) / π
```

`L` en cd/m², `E` en lux, `ρ` entre 0 et 1, π ≈ 3,1416.

C'est le pont central. Une fois que tu connais la lumière qui tombe et la matière qui la reçoit, tu connais la lumière qui part vers l'objectif — donc l'exposition.

---

## 2. L'échelle EV — la colonne vertébrale

L'EV (exposure value) est l'échelle logarithmique en base 2 : **+1 EV = ×2 de lumière = 1 diaph**.

### Définition côté appareil

```
EV = log₂(N² / t)
```
`N` = ouverture, `t` = temps de pose en secondes.

Vérification : f/16 à 1/125 → log₂(256 × 125) = log₂(32000) = **EV 15**. C'est Sunny 16 à 100 ISO.

### Définition côté lumière incidente

Avec la constante de calibration standard C = 250 (dôme plat), à 100 ISO :

```
E(lux) = 2,5 × 2^EV₁₀₀        et        EV₁₀₀ = log₂(E / 2,5)
```

### Définition côté luminance réfléchie

Avec K = 12,5 (standard Sekonic / Canon / Nikon ; Minolta et Pentax utilisent 14) :

```
L(cd/m²) = 0,125 × 2^EV₁₀₀     et      EV₁₀₀ = log₂(L / 0,125)
```

### Table maîtresse (100 ISO)

| EV₁₀₀ | Lux (incident) | cd/m² (luminance) | Réglage type | Situation réelle |
|---|---|---|---|---|
| 16 | 164 000 | 8 200 | f/22 @ 1/125 | Neige/sable, soleil zénithal |
| **15** | **82 000** | **4 100** | **f/16 @ 1/125** | **Plein soleil, ombres dures** |
| 14 | 41 000 | 2 050 | f/11 @ 1/125 | Voile léger, ombres douces |
| 13 | 20 500 | 1 020 | f/8 @ 1/125 | Nuageux, ombres à peine visibles |
| 12 | 10 200 | 512 | f/5,6 @ 1/125 | Couvert, aucune ombre |
| 11 | 5 100 | 256 | f/4 @ 1/125 | Couvert sombre, ombre ouverte |
| 10 | 2 560 | 128 | f/2,8 @ 1/125 | Sous-bois dense, orage |
| 9 | 1 280 | 64 | f/2 @ 1/125 | Crépuscule clair |
| 8 | 640 | 32 | f/2 @ 1/60 | Intérieur très éclairé, vitrine |
| 7 | 320 | 16 | f/2 @ 1/30 | Bureau bien éclairé |
| 6 | 160 | 8 | f/2 @ 1/15 | Salon éclairé |
| 5 | 80 | 4 | f/2 @ 1/8 | Rue de nuit bien éclairée |
| 3 | 20 | 1 | f/2 @ 1/2 | Rue de nuit moyenne |
| 0 | 2,5 | 0,125 | f/2 @ 4s | Crépuscule avancé |
| -3 | 0,3 | 0,016 | f/2 @ 30s | Pleine lune sur paysage |
| -6 | 0,04 | 0,002 | f/2 @ 4 min | Quart de lune |

### Compensation ISO

```
EV_réel = EV₁₀₀ + log₂(ISO / 100)
```

| ISO | Décalage |
|---|---|
| 50 | −1 EV |
| 100 | 0 (référence) |
| 200 | +1 EV |
| 400 | +2 EV |
| 800 | +3 EV |
| 1600 | +4 EV |
| 3200 | +5 EV |

À 400 ISO, Sunny 16 devient EV 17 → f/16 @ 1/500 (ou 1/400 selon le boîtier).

---

## 3. Réflectances — la table des matériaux

Écart en diaphs par rapport au gris moyen : `stops = log₂(ρ / 0,18)`

| Matériau | ρ | Écart au gris 18 % |
|---|---|---|
| Neige fraîche | 0,80–0,90 | **+2⅓** |
| Peinture blanche, mur chaulé | 0,70–0,80 | +2 |
| Béton neuf | 0,40–0,55 | +1⅓ |
| Sable de désert, plage claire | 0,40 | +1⅙ |
| Granite clair (Corse, Restonica) | 0,25–0,35 | +½ à +1 |
| Peau claire (type caucasien) | 0,32–0,38 | **+1** |
| Herbe verte vive | 0,25 | +½ |
| Béton vieilli, pierre grise | 0,20–0,30 | +¼ à +¾ |
| **Gris moyen (charte)** | **0,18** | **0** |
| Sol nu, terre sèche | 0,17 | ≈ 0 |
| Feuillage caduc, arbres | 0,15–0,18 | 0 à −¼ |
| Herbe sèche, maquis grillé | 0,15 | −¼ |
| Peau foncée | 0,10–0,15 | **−½ à −1** |
| Asphalte usé | 0,12 | −⅗ |
| Forêt de conifères | 0,08–0,15 | −½ à −1 |
| Océan, eau profonde (diffus) | 0,06 | −1½ |
| Asphalte neuf | 0,04–0,05 | −2 |
| Velours noir, charbon | 0,01–0,02 | −3½ |

**Attention à l'eau** : sa réflectance diffuse est très basse (0,06), mais sa composante **spéculaire** peut approcher 1,0. Une rivière au soleil rasant renvoie donc à la fois presque rien (dans l'ombre du reflet) et presque tout (dans le miroir). C'est pour ça qu'une scène d'eau a une dynamique énorme et qu'elle piège les cellules moyennes.

---

## 4. La chaîne de calcul complète

### Formule maîtresse

```
EV₁₀₀ = log₂(E_lux / 2,5) + log₂(ρ / 0,18)
```

Le premier terme = ce que dirait une cellule incidente. Le second = la correction liée à la matière visée.

### Ce que ça implique concrètement

**Une cellule incidente est directement juste.** Elle mesure `E`, elle ignore `ρ`, et c'est précisément ce qu'on veut : l'exposition ne doit pas dépendre de la couleur du sujet.

**Une cellule réfléchie (ou la matricielle du boîtier) donne un nombre qu'il faut corriger de `log₂(ρ/0,18)`.** Elle suppose toujours ρ = 0,18. C'est exactement l'origine du problème classique de la neige : le spotmètre sur la neige lit +2⅓ EV de trop, tu suis, tu sous-expose de 2⅓ diaphs et la neige devient grise.

### Exemple vérifié

Plein soleil, 82 000 lux, sujet = charte grise.
- L = 82 000 × 0,18 / π = **4 700 cd/m²**
- EV = log₂(4 700 / 0,125) = log₂(37 600) = **15,2**
- → f/16 @ 1/125 à 100 ISO ✓

> Ces 0,2 diaph d'écart entre la voie incidente (EV 15,0) et la voie réfléchie (EV 15,2) ne sont pas un arrondi : ils sont structurels. La cohérence des deux constantes exigerait C = K·π/ρ = 12,5 × 3,1416 / 0,18 ≈ 218, pas 250. C'est l'origine de l'écart permanent entre une cellule incidente et un spotmètre pointé sur une charte grise.

Même soleil, sujet = peau claire (ρ = 0,35).
- L = 82 000 × 0,35 / π = **9 130 cd/m²**
- EV lu par un spot = log₂(73 000) = **16,2**
- Correction : −1 EV → on revient à EV 15,2. Une peau claire doit être placée **1 diaph au-dessus** du gris moyen.

---

## 5. Hauteur du soleil — la loi qui gouverne tout le reste

L'éclairement direct suit approximativement le sinus de la hauteur solaire :

```
E ≈ E_max × sin(h)        →        perte en diaphs = log₂(sin h)
```

| Hauteur du soleil | sin(h) | Perte vs zénith |
|---|---|---|
| 90° | 1,00 | 0 |
| 60° | 0,87 | −0,2 |
| 55° | 0,82 | −0,3 |
| 45° | 0,71 | −0,5 |
| 30° | 0,50 | **−1** |
| 20° | 0,34 | −1,5 |
| 15° | 0,26 | −2 |
| 10° | 0,17 | −2,5 |
| 5° | 0,09 | −3,5 |

**Correctif important** : sous 15°, l'épaisseur d'atmosphère traversée (masse d'air) ajoute une extinction supplémentaire. La chute réelle est plus raide que le sinus — compte facilement −4 à −5 diaphs à 5° au lieu de −3,5. C'est ce qui rend la golden hour si rapide à basculer.

> L'écart s'ouvre plus tôt que 15°. Avec la masse d'air de Kasten-Young, une transmission de 0,7, et en tenant compte de la part de ciel diffus qui ne s'effondre pas au même rythme : à 15° la perte réelle est de −2,0 diaphs (le sinus dit −2,0, ça concorde encore), à 10° de −2,7 (sinus : −2,5), à 5° de **−4,0** (sinus : −3,5). Le sinus reste juste tant que le soleil est haut et devient franchement optimiste sous 10°.

### Hauteur du soleil au midi solaire

`h = 90° − latitude + déclinaison`

| Lieu | Solstice d'été | Équinoxes | Solstice d'hiver |
|---|---|---|---|
| **Corte** (42,3° N) | 71° | 48° | 24° |
| **Gers** (43,7° N) | 70° | 46° | 23° |

Traduit en diaphs de perte par rapport au zénith théorique : été −0,15 · équinoxes −0,4 · hiver **−1,4**.

Voilà l'explication chiffrée de tout ce qu'on avait posé empiriquement : f/16 en été, f/11 aux mi-saisons, f/8–f/11 en hiver. Ce n'est pas une convention, c'est du sin(h).

**Début septembre à Corte**, déclinaison ≈ +6,5°, soleil à **54,2°** au midi solaire (**13 h 22** heure légale) : −0,3 diaph par rapport au plein été. En pratique tu restes sur f/16.

> Le midi solaire se calcule : 12 h + décalage de fuseau − longitude/15 − équation du temps. Pour Corte (9,15° E, UTC+2) au 6 septembre, l'équation du temps vaut +1,4 min, ce qui donne 13 h 22 et non 13 h 10.

---

## 6. Éclairements de référence (lux)

| Condition | Lux | EV₁₀₀ |
|---|---|---|
| Soleil d'été zénithal, ciel pur | 100 000–130 000 | 15,3–15,7 |
| Plein soleil moyen | 80 000–100 000 | 15,0–15,3 |
| Soleil voilé | 40 000–70 000 | 14–14,8 |
| Ciel bleu à l'ombre (pas de soleil direct) | 10 000–25 000 | 12–13,3 |
| Journée couverte | 1 000–20 000 | 8,6–13 |
| Couvert lourd, orage | 100–1 000 | 5,3–8,6 |
| Lever/coucher, ciel dégagé | 400–1 000 | 7,3–8,6 |
| Crépuscule civil | 3–10 | 0,3–2 |
| Crépuscule nautique | 0,01–3 | −8 à 0,3 |
| Pleine lune, ciel clair | 0,05–0,3 | −5,6 à −3 |
| Ciel étoilé sans lune | 0,001–0,002 | −11 à −10 |
| Bureau normé | 300–500 | 6,9–7,6 |
| Salon, éclairage domestique | 50–150 | 4,3–5,9 |
| Rue éclairée la nuit | 5–30 | 1–3,6 |

> La fourchette « journée couverte » est large parce qu'elle recouvre deux choses très différentes : un couvert clair au ciel blanc lumineux (15 000–25 000 lx, EV 12,5–13,3) et un couvert lourd d'avant-orage (1 000–3 000 lx, EV 8,6–10). La section 12 chiffre le couvert ordinaire à −2 à −3 diaphs sous le Sunny 16, soit 10 000–20 500 lx : c'est le milieu de la fourchette. Ne confonds pas les deux bouts.

---

## 7. La vraie « courbe gamma » : perception vs physique

C'est le cœur de ta question, et la réponse est nette.

La sensibilité visuelle à la clarté suit approximativement une **racine cubique** de la luminance (fonction CIE L\*) :

```
L* = 116 × (Y)^⅓ − 16          (pour Y > 0,0089)
```

`Y` = luminance relative (0 à 1), `L*` = clarté perçue (0 à 100).

### La conséquence qui explique tout

Applique ça à 18 % :

```
L* = 116 × 0,18^⅓ − 16 = 116 × 0,5646 − 16 = 49,5
```

**Une surface qui ne renvoie que 18 % de la lumière est perçue comme exactement à mi-chemin entre le noir et le blanc.** Le gris moyen n'est pas 50 % de réflectance, il est 50 % de *clarté perçue*. C'est ça, la courbe gamma entre le monde réel et ta perception — un exposant ≈ 1/3.

### La table des quarts perceptuels

| Clarté perçue (L\*) | Réflectance réelle | Écart au gris 18 % |
|---|---|---|
| 100 (blanc) | 100 % | +2½ diaphs |
| 75 | 48 % | +1,4 diaph |
| **50 (gris moyen)** | **18 %** | **0** |
| 25 | 4,4 % | −2 diaphs |
| 0 (noir) | 0 % | −∞ |

Regarde les écarts : passer de 25 % à 50 % de clarté perçue coûte **2 diaphs**, mais passer de 50 % à 75 % n'en coûte que **1,4**. La perception **compresse fortement les hautes lumières et dilate les ombres**. C'est pour ça qu'un ciel cramé se voit tout de suite alors qu'une ombre bouchée passe souvent inaperçue à l'œil nu mais explose à l'image.

C'est aussi pour ça que les courbes de transfert (gamma 2,2, Rec.709, log) existent : elles allouent les bits selon la perception, pas selon la physique.

---

## 8. De la scène à l'image — les fonctions de transfert

### Placement du gris moyen selon l'encodage

| Encodage | Gris moyen | En IRE |
|---|---|---|
| Linéaire (scene-referred) | 0,18 | — |
| sRGB | 0,46 | 46 % |
| Gamma 2,4 pur | 0,489 | 49 % |
| **Rec.709 (OETF caméra)** | **0,409** | **41 %** |
| ARRI LogC3 | 0,391 | 39 % |
| ARRI LogC4 | 0,28 | 28 % |
| Sony S-Log3 | 0,411 | 41 % |
| Panasonic V-Log | 0,423 | 42 % |
| Canon Log 2 | 0,398 | 40 % |
| Canon Log 3 | 0,343 | 34 % |
| RED Log3G10 | 0,333 | 33 % |
| Blackmagic Film Gen5 | 0,466 | 47 % |
| DaVinci Wide Gamut / Intermediate | 0,336 | 34 % |

Règle : **on utilise toujours la valeur de l'espace dans lequel on se trouve à cet instant du pipeline.** Après une CST, c'est la valeur du nouvel espace qui compte.

Repères d'exposition en Rec.709 : blancs texturés vers 80–90 IRE, peau claire 55–70 IRE, peau moyenne 45–55 IRE, ombres vers 20 IRE.

### Plage dynamique des supports

| Support | Diaphs exploitables |
|---|---|
| Œil, adaptation locale instantanée | 10–14 |
| Œil, avec adaptation complète | 20–24 |
| Caméra ciné haut de gamme (ALEXA 35) | ~17 |
| Capteur plein format récent | 13–15 |
| **Négatif couleur (Portra 400, Ultramax)** | **13–14** |
| Négatif N&B (Tri-X, HP5) | 12–13 |
| Diapositive / inversible | 5–6 |
| Rec.709 / écran SDR | ~6 |
| Tirage papier | ~7 |

### Latitude pratique du négatif couleur

| Écart | Résultat |
|---|---|
| −2 diaphs | Ombres bouchées, grain marqué, récupérable de justesse |
| −1 diaph | Correct, contraste un peu sec |
| Nominal | Optimal |
| +1 à +2 | Souvent **meilleur** — grain plus fin, ombres ouvertes |
| +3 | Toujours exploitable, hautes lumières qui se tassent |
| +4 à +5 | Densité forte, mais l'info est encore là |

D'où la règle de terrain : **en cas de doute avec du négatif, surexpose.** L'asymétrie est réelle et large. Avec de l'inversible ou du numérique en log, c'est exactement l'inverse — l'écrêtage des hautes lumières est définitif.

### Dynamique typique des scènes

| Scène | Amplitude |
|---|---|
| Plein soleil, ombres dures | 8–11 diaphs |
| Soleil voilé | 6–8 |
| Couvert uniforme | 3–5 |
| Intérieur avec fenêtre | 10–14 |
| Nuit urbaine avec sources | 12–16 |

Quand l'amplitude de la scène dépasse la plage du support, tu ne « rates » pas l'exposition — tu **choisis** ce que tu sacrifies. C'est une décision artistique, pas une erreur technique.

---

## 9. Zone System — la traduction en placement

Chaque zone = 1 diaph. Zone V = gris 18 % = ce que donne une cellule.

| Zone | Écart | Rendu | Réflectance équivalente |
|---|---|---|---|
| 0 | −5 | Noir absolu | — |
| I | −4 | Noir, aucune texture | 1,1 % |
| II | −3 | Premier noir texturé | 2,2 % |
| III | −2 | Ombre texturée | 4,5 % |
| IV | −1 | Ombre ouverte, feuillage sombre, peau foncée | 9 % |
| **V** | **0** | **Gris moyen** | **18 %** |
| VI | +1 | Peau claire, ciel bleu profond | 36 % |
| VII | +2 | Blanc texturé, neige à l'ombre | 72 % |
| VIII | +3 | Dernier blanc texturé | — |
| IX | +4 | Blanc sans texture | — |

Usage terrain : **spot sur la zone la plus sombre où tu veux de la matière → place-la en Zone III → ferme de 2 diaphs par rapport à la lecture.** Le reste tombe où il tombe, et tu vérifies que tes hautes lumières restent sous Zone VIII.

---

## 10. Lumière artificielle — loi du carré inverse

```
E = I / d²
```

| Rapport de distance | Écart |
|---|---|
| ×1,4 | −1 diaph |
| ×2 | **−2 diaphs** |
| ×2,8 | −3 diaphs |
| ×4 | −4 diaphs |

Conséquence pratique en ciné : plus la source est **proche**, plus la chute est brutale entre le sujet et le fond. Source lointaine = éclairement quasi uniforme sur toute la profondeur. C'est l'outil principal pour contrôler le rapport sujet/décor sans toucher aux projecteurs du fond.

Ratios d'éclairage classiques : 2:1 = 1 diaph d'écart key/fill · 4:1 = 2 diaphs · 8:1 = 3 diaphs.

---

## 11. Contrainte ciné : vitesse verrouillée

Règle des 180° : `t = 1 / (2 × fps)`.

| Cadence | Obturation |
|---|---|
| 24 fps | 1/48 |
| 25 fps | 1/50 |
| 30 fps | 1/60 |
| 50 fps | 1/100 |
| 60 fps | 1/120 |

La vitesse étant fixée, il ne reste que **l'ouverture, l'ISO/EI et les ND** pour gérer l'exposition.

| ND | Densité | Diaphs |
|---|---|---|
| ND2 / 0,3 | 0,3 | 1 |
| ND4 / 0,6 | 0,6 | 2 |
| ND8 / 0,9 | 0,9 | 3 |
| ND16 / 1,2 | 1,2 | 4 |
| ND64 / 1,8 | 1,8 | 6 |
| ND1000 / 3,0 | 3,0 | 10 |

Cas concret : 25 fps, 800 EI, plein soleil (EV 15 à 100 ISO → EV 18 à 800). À 1/50 il faudrait **f/64** — impossible. Avec un ND1,2 (4 diaphs) tu retombes à f/18, avec un ND1,8 à f/9. C'est exactement pourquoi les ND variables ou les roues internes sont indispensables en extérieur jour.

> Vérification : EV 18 = log₂(N² × 50) → N² = 2¹⁸/50 = 5 243 → N = 72,4, soit f/64 au diaph plein. Sunny 16 dit la même chose autrement : à 800 ISO l'exposition de base est f/16 au 1/800 ; passer au 1/50 gagne quatre diaphs de temps, qu'il faut rendre à l'ouverture — f/16 → f/22 → f/32 → f/45 → f/64.

---

## 12. Facteurs cumulables — la grille de terrain

Départ : **plein soleil d'été, midi solaire, terrain ouvert, sujet ρ = 0,18 → EV₁₀₀ 15 (f/16 @ 1/ISO)**.

### Pertes (tu ouvres)

| Facteur | Diaphs |
|---|---|
| Voile léger sur le soleil | −1 |
| Ciel couvert | −2 à −3 |
| Couvert lourd / orage | −4 à −5 |
| Ombre portée, ciel dégagé au-dessus | −2 à −3 |
| Sous arbre isolé, feuillage clair | −1 à −2 |
| Sous-bois dense, canopée fermée | −3 à −4 |
| Ombre de versant / relief (vallée encaissée) | −2 à −3 |
| Rue étroite, cour intérieure | −3 à −4 |
| Golden hour (soleil 5–10°) | −3 à −4 |
| Soleil à 30° (mi-saison tardive, hiver) | −1 |
| Contre-jour direct sur le sujet | −1 à −2 |
| Latitude nord vs sud, même date | −0,5 à −1 |
| Fenêtre, intérieur près de la vitre | −4 à −5 |
| Intérieur, loin de la fenêtre | −7 à −9 |

### Gains (tu fermes)

| Facteur | Diaphs |
|---|---|
| Neige fraîche remplissant le cadre | +1 à +2 |
| Sable clair, plage | +1 |
| Eau au soleil, galets clairs (Restonica) | +1 |
| Granite clair / calcaire en plein soleil | +½ à +1 |
| Altitude > 2 000 m (air raréfié) | +½ à +1 |
| Air méditerranéen très sec vs océanique | +⅓ à +½ |
| Réverbération latérale (mur blanc proche) | +½ à +1 |

**Ces facteurs s'additionnent en diaphs, pas en pourcentages.** C'est tout l'intérêt de raisonner en log₂.

### Vérification sur un cas réel

Rivière de la Restonica, début septembre, 17h, sujet à l'ombre d'un arbre isolé au bord de l'eau :

| Terme | Diaphs |
|---|---|
| Base plein soleil été midi | EV 15 |
| Soleil à ~30° (17h, début sept) | −0,8 |
| Ombre d'arbre isolé, feuillage clair | −1,5 |
| Réverbération eau + galets clairs | +1,0 |
| Air sec méditerranéen | +0,3 |
| **Total** | **≈ EV 14,0** |

EV 14,0 à 400 ISO → EV 16,0 → f/11 @ 1/500, ou **f/8 @ 1/1000**, ou f/16 @ 1/250.

Ce qui correspond bien au f/8–f/11 estimé à vue.

> Le soleil est plus bas que ce que dit l'intuition. Le midi solaire tombant à 13 h 22, 17 h heure légale se situe 3 h 38 après la culmination : à 42,3° N avec une déclinaison de +6,5°, le soleil est alors à **30,2°**, pas à 40°. Quarante degrés correspondraient plutôt à 15 h 40. La conclusion f/8–f/11 tient malgré tout — l'erreur de hauteur et l'arrondi du total se compensent presque.

---

## 13. Protocole d'entraînement

L'objectif est de remplacer le calcul par la reconnaissance directe. Trois phases.

**Phase 1 — ancrage (2 à 3 semaines).**
Devant chaque scène, annonce un EV avant toute mesure. Puis vérifie avec une appli posemètre (Lux Light Meter, myLightMeter) réglée en mode incident. Note l'écart. L'objectif n'est pas d'avoir juste, c'est de mesurer ton biais systématique — la plupart des gens sous-estiment les ombres de 1 à 2 diaphs.

**Phase 2 — décomposition (1 à 2 mois).**
Arrête d'annoncer un chiffre global. Annonce la **chaîne de facteurs** : « base 15, moins 0,5 pour l'heure, moins 2 pour l'ombre, plus 1 pour l'eau → 13,5 ». C'est cette décomposition qui se grave, pas les résultats.

**Phase 3 — reconnaissance directe.**
Le chiffre arrive sans calcul. Tu ne vérifies plus que sur les scènes ambiguës — mixtes, contre-jour, lumière artificielle mêlée.

### Repères physiques à vérifier sur toi

- **Ombre portée plus courte que ta taille** → soleil > 45° → EV 14,5–15
- **Ombre égale à ta taille** → soleil à 45° → EV 14,5
- **Ombre deux fois ta taille** → soleil à ~27° → EV 14
- **Ombre nette avec bord franc** → soleil direct non voilé
- **Ombre visible mais bord flou** → voile → −1
- **Aucune ombre discernable** → couvert → −2 à −3

### Reporter l'écart directement sur l'ouverture

Une fois que tu sais lire un EV de scène, tu n'as plus besoin de repasser par le lux. Le principe : pars de ton EV Sunny 16 (15 à 100 ISO), compte l'écart total en EV de la scène observée, et reporte cet écart directement en diaphs sur l'ouverture — vitesse fixée une fois pour toutes par l'ISO (1/ISO).

`écart en diaphs = EV_base − EV_scène`

Exemple : tu estimes une scène à EV 13,5 (ombre + facteur d'eau qui compense un peu, cf. exemple de la section 12). Écart = 15 − 13,5 = **1,5 diaph**. Tu ouvres donc de 1,5 diaph par rapport à f/16, ce qui tombe entre f/8 et f/11 (à 100 ISO, vitesse 1/125 inchangée).

Si la scène était plutôt à EV 12 (couvert franc, écart de 3 diaphs), tu ouvrirais bien jusqu'à f/8, vitesse toujours 1/125 en 100 ISO — c'est le même mécanisme, seul le nombre d'EV à reporter change selon ce que tu lis sur le terrain.

### Trois calibrations locales à faire

1. **Ta rivière corse à midi** — mesure une fois précisément, tu auras le point haut de ton échelle avec réverbération.
2. **Un sous-bois du Gers en automne** — le point bas de ta plage utile en extérieur jour.
3. **Ton intérieur habituel le soir** — le raccord vers la lumière artificielle, là où l'œil se trompe le plus (l'adaptation te fait croire qu'il fait bien plus clair qu'il ne fait).

---

## 14. Les six relations à retenir par cœur

```
1.  L = E × ρ / π                      luminance depuis éclairement et matière
2.  EV₁₀₀ = log₂(E / 2,5)              éclairement incident → EV
3.  EV₁₀₀ = log₂(L / 0,125)            luminance réfléchie → EV
4.  EV = log₂(N² / t)                  réglages → EV
5.  ΔEV = log₂(ρ / 0,18)               correction de réflectance
6.  L* = 116 × Y^⅓ − 16                luminance physique → clarté perçue
```

Les cinq premières font le lien entre le monde et l'appareil. La sixième fait le lien entre le monde et ton œil — c'est celle qui explique pourquoi ton intuition et ton posemètre ne sont pas d'accord.

---

## Notes sur les constantes

Les valeurs C = 250 et K = 12,5 sont les conventions les plus répandues (Sekonic, Canon, Nikon). Certains fabricants utilisent C = 320 pour dôme hémisphérique, ou K = 14 (Minolta, Pentax) — écart d'environ ⅙ de diaph, négligeable en pratique mais à connaître si tu compares deux cellules.

Les réflectances sont des valeurs médianes issues de tables d'albédo standard. Elles varient avec l'humidité, l'angle d'incidence, la saison et l'état de surface : de l'herbe sèche perd facilement 0,10 par rapport à de l'herbe verte irriguée, et de l'asphalte mouillé chute vers 0,05.

---

## Récapitulatif des corrections

| § | Ce que disait le document | Valeur exacte | Pourquoi |
|---|---|---|---|
| 9 | Zone I à 0,6 % de réflectance | **1,1 %** | −4 diaphs sur 18 % font 18/16 = 1,125 % |
| 11 | 25 fps, 800 EI, plein soleil → f/45 | **f/64** | EV 18 au 1/50 donne N = 72,4 ; f/45 correspondrait à EV 16,6 |
| 11 | Avec ND1,2 → f/11 ; ND1,8 → f/5,6 | **f/18** et **f/9** | conséquence directe de la ligne précédente |
| 5 | Midi solaire à Corte : 13 h 10 | **13 h 22** | longitude 9,15° E et équation du temps de +1,4 min début septembre |
| 12 | Restonica : soleil à ~40° à 17 h | **30,2°** | 3 h 38 après le midi solaire, à 42,3° N avec δ = +6,5° |
| 6 | Journée couverte : 1 000–10 000 lx | **1 000–20 000 lx** | contredisait le facteur « couvert = −2 à −3 diaphs » de la section 12 |

Deux points ajoutés plutôt que corrigés : l'écart structurel de 0,2 diaph entre C = 250 et K = 12,5 (§ 4), et le fait que la table en log₂(sin h) devient nettement optimiste dès 10° de hauteur, pas seulement sous 5° (§ 5).

Le reste a été vérifié ligne à ligne et tient : la table maîtresse EV/lux/cd·m⁻², les réflectances et leurs écarts au gris, les hauteurs solaires aux solstices pour Corte et le Gers, la table L\*, les latitudes de négatif, les ND et les cadences ciné.
