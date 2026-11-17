# Cahier des charges — Cellule V3 : Lumière, plan au sol, mon matériel

Décidé avec l'utilisateur les 16 et 17 novembre 2026. La V3 s'affiche « 3.0 (n° de compilation) ».
Une étape = un ou plusieurs commits, compilés par GitHub Actions, APK vérifié (versionCode croissant,
certificat EC43DDA8C3343749) puis livré.

## Règles de l'appli (inchangées)

- Textes en français, tutoiement, ton sobre. Calculatrice rapide pour le plateau ; le pourquoi dans
  des « Comprendre » repliables.
- **Aucun chiffre sans origine.** Les formules (1/d², cosinus, mireds, angles, électricité) sont de la
  physique. Les données (photométrie des projecteurs, gélatines Lee et Rosco, caméras, optiques)
  viennent d'une fiche nommée ; sinon « à relire », et l'utilisateur peut corriger. Les repères de
  plateau (« filet simple ≈ ½ diaph ») sont marqués comme repères, pas comme données.
- Hors connexion d'abord ; batterie ménagée ; les 8 icônes du bas ne changent pas.
- Logique pure dans `core` avec tests ; interface dans `app`.

## Où ça vit : l'onglet Cinéma

Quatre vues : **Viseur | Plans | Lumière | Matériel**.
- **Lumière** : les calculs.
- **Plan au sol** : rangé dans Plans (attaché à un jour ou une séquence, donc dans les dossiers et
  les PDF), ouvrable aussi depuis Lumière.
- Tout reprend l'EI, l'obturateur, la cadence et le capteur du Matériel Cinéma.

## Étape 1 — Mon matériel

Dans Matériel, « Ajouter le mien » :
- **Caméra** : nom ; modes de capteur (nom, largeur × hauteur en mm).
- **Série d'objectifs** : nom ; focales exactes ; T max ; désanamorphose.
- **Projecteur** : nom, fabricant, famille (panneau LED, LED ponctuelle, tube LED, Fresnel tungstène,
  ouvert tungstène, HMI, fluo, autre), puissance (W), température de couleur (K), angle de faisceau
  flood et spot, éclairement mesuré ou publié (lux à une distance, en flood et en spot).
- Se choisissent comme ceux du catalogue, se modifient, se suppriment (en deux temps), voyagent
  dans la sauvegarde.

## Étape 2 — Les calculs Lumière

- **Projecteurs** : « Combien j'ai ici ? » (projecteur, distance, spot/flood, gélatines, diffusion →
  lux, foot-candles, T-stop à l'EI et à l'obturateur) ; « Où je le mets ? » (T voulu → distance ;
  largeur à couvrir → distance d'après le faisceau) ; géométrie (hauteur, recul → distance réelle,
  inclinaison, angle d'arrivée, loi du cosinus) ; rapports face / débouchage / contre, somme de
  sources. Avertir que la loi en 1/d² ne vaut qu'au-delà d'environ 5 fois la taille de la source.
- **Couleur** : kelvins ↔ mireds ; décalage de la source vers la cible ; combinaison de gélatines Lee
  et Rosco la plus proche et l'écart restant ; vert / magenta ; perte de chaque gélatine.
- **Champ** : focale ↔ angle (H, V, diagonale) selon le capteur ; largeur cadrée à une distance ;
  focale pour un cadre ; profondeur de champ et hyperfocale (cercle de confusion indiqué comme
  convention) ; « la lumière couvre-t-elle le cadre ? ».
- **Conversions** : lux ↔ foot-candles ↔ EV ; diaphs ↔ densité ND ↔ % transmis ; T ↔ f ;
  angle d'obturateur ↔ vitesse. Bascule mètres / pieds et lux / foot-candles.
- **Électricité** : puissance ↔ intensité, charge par circuit, groupes en kVA. Europe d'abord
  (230 V mono, 400 V tri, prises domestiques 16 A, P17 16/32/63/125 A), États-Unis aussi (120 V,
  208/240 V). Indicatif, pas un outil de sécurité : le dire.
- **Scintillement** (désactivable) : avec le secteur à 50 Hz (60 Hz aux États-Unis), cadences et
  angles d'obturateur sûrs ou à risque pour les sources qui battent à 100 (120) Hz.
- **Ambiance** : rapports de contraste types (high key, low key, nuit américaine, intérieur jour…),
  donnés comme repères.

## Étape 3 — Le catalogue des projecteurs

Les projecteurs les plus courants, du matériel accessible au parc de location (Aputure, Amaran,
Nanlite, Godox, ARRI SkyPanel et Orbiter, HMI, Fresnels tungstène, Kino Flo, Astera…), avec leur
photométrie publiée ; « à relire » sinon. On choisit un projecteur existant ou le sien. Les fiches
fabricants seront revues ensemble ensuite.

## Étape 4 — Le plan au sol

- Grille à l'échelle (désactivable), éléments qu'on déplace et fait tourner au doigt, cotes
  saisissables. Mètres ou pieds.
- Éléments : murs, fenêtres, portes, mobilier ; caméras A/B… avec leur vrai cône de champ (focale +
  capteur) et leurs numéros de plan ; comédiens avec nom et positions numérotées (A1 → A2) reliées
  par des flèches ; projecteurs avec leur cône de faisceau ; réflecteurs, cadres de diffusion,
  drapeaux ; rails de travelling. Une **légende**.
- **Hauteur** de chaque projecteur et caméra, et une **petite vue de côté**.
- Les distances du plan alimentent les calculs : lux et T-stop au sujet, rapports.
- **Soleil** (désactivable) : nord par la boussole, direction du soleil et des ombres à l'heure
  prévue, heures dorée et bleue.
- **Extérieur** : la carte OpenStreetMap à l'échelle en fond quand il y a du réseau, grille
  activable par-dessus.
- Export dans les PDF (plan seul et dossier) **et** en image à part, à envoyer à l'équipe.
- Plus tard : un mode lecture qui anime les déplacements.

## Étape 5 — La fiche lumière

Pour un plan : projecteurs, positions, gélatines, diffusions, réglages — dans la fiche et le PDF.

## Étape 6 — Le raccord lumière

Enregistrer les mesures d'une scène (incident et spot en plusieurs points, photo, températures de
couleur **saisies à la main**) pour la refaire un autre jour. Pas d'estimation de couleur par la
caméra du téléphone : pas fiable.

## Étape 7 — Liste de location et bilan électrique

D'après les plans d'une journée : total des projecteurs, accessoires, puissance et circuits.
