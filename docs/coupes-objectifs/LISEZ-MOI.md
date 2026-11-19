# Coupes d'objectifs — dossier pour Fable

Un sous-onglet **« Coupes »** dans Caméras › Apprendre (à côté de Caméras | Objectifs | Projecteurs) :
un **moniteur** qui montre la coupe d'un objectif et **les tracés de la lumière** qui le traversent.

> **Budget : 5 crédits.** Tout ce qui est recherche est déjà fait ici. Ne cherche rien sur le web,
> n'ajoute pas d'objectif, ne touche à aucun autre écran. Le travail, c'est le calcul des rayons
> et un visuel très soigné.

## L'idée, d'après la vidéo de référence

La vidéo montre une maquette 3D interactive du Signature Prime 47 : titre en haut à gauche
(« SIGNATURE PRIME 47 », « T1.8 · LPL »), et à droite un panneau sombre avec un curseur **Focus**
gradué en distances (∞ … 0,45 m, valeur en grand : « 2.19 m »), un curseur **Iris** gradué
T1.8 → T22 (valeur en grand : « T1.8 »), des lignes d'explication sous chaque curseur
(« focus group 2.18 mm toward the camera », « clear aperture ⌀ 36.8 mm »), un éclaté, des vues
Solid / X-ray / Cutaway et une légende des pièces. Couleur d'accent : jaune-vert sur fond anthracite.

**On garde :** la présentation (titre, panneau de curseurs, valeurs en grand, ligne d'explication),
les curseurs Focus et Iris, les lentilles en bleu.
**On ne fait pas :** la 3D, l'éclaté, la mécanique (fûts, bagues).
**On ajoute :** les rayons lumineux, calculés pour de vrai.

## Le moniteur

- Fond anthracite, cadre fin façon moniteur de plateau, mire discrète. Titre : nom de la série, focale, T, monture.
- **Coupe en 2D**, dans le plan qui contient l'axe optique : chaque lentille est un volume de verre
  bleu translucide (léger dégradé, bord plus clair), dessiné entre ses deux surfaces sur son
  `demiDiametreConseille`. Les surfaces collées (deux verres sans air entre eux) : un trait fin.
- Le **diaphragme** : deux lamelles sombres sur la surface `diaphragme: true`, leur ouverture suit le curseur Iris.
- Le **capteur** à droite (trait épais), de hauteur ± `hauteurImage`. L'axe optique en pointillés.
- **Les rayons** : trois faisceaux qui partent de l'infini — centre de l'image, 70 % et 100 % du
  champ —, chacun de 7 à 9 rayons répartis sur la pupille, en trois couleurs lumineuses
  (ambre, cyan, magenta), avec un léger halo. Un rayon qui touche un bord de lentille ou une
  lamelle s'arrête là : on voit le vignetage.
- Sous le dessin, une ligne d'infos : focale, nombre d'éléments et de groupes, asphériques,
  verres spéciaux (vd > 80 : « verre à faible dispersion » ; nd > 1,9 : « verre à très haut indice »).
- En bas, toujours visible : « Coupe du brevet [numéro] — pas forcément identique au modèle de
  série. Correspondance : Photons to Photos (W. J. Claff). »

**Curseur Focus** (si `ecartsVariables` existe) : de ∞ à `distanceProche_mm`. Les écarts variables
se calculent par interpolation linéaire entre leurs valeurs `infini` et `proche` (le dire : « mise
au point interpolée ») ; les rayons partent alors d'un point à cette distance. Afficher, comme la
vidéo, de combien le groupe a bougé. Master Prime : pas d'écarts dans le brevet → pas de curseur Focus.

**Curseur Iris** : de `ouvertureBrevet` à T22 par tiers ; le diamètre du faisceau à l'entrée est
`focale / N`. Afficher le diamètre d'ouverture au diaphragme, comme la vidéo.

## Les données : `formules.json`

7 coupes **vérifiées** (focale recalculée = focale du brevet, au centième) :

| Fiche de l'appli | Coupes |
|---|---|
| ARRI / Zeiss Master Prime | 50 mm, 75 mm (75 : probable, à confirmer) |
| Sigma Cine FF High Speed | 20, 24, 35, 50, 105 mm (d'après les Sigma Art ; Sigma dit que la gamme Cine reprend leur système optique) |

Chaque surface : `rayon` (null = plan), `epaisseurApres`, `indiceApres` / `abbeApres` (le verre qui
suit ; absent = air), `ecartVariable` (son épaisseur vient de `ecartsVariables`), `diaphragme`,
`diaphragmeAntiReflets`, `aspherique` (K, A4, A6…), et trois demi-diamètres estimés (lire `methode`).
Une lame plane en fin de formule est le verre de protection du capteur.

Copier le fichier tel quel dans les assets de l'appli (`app/src/main/assets/coupes/formules.json`).

Les **35 autres familles** du catalogue : la liste montre la fiche avec « Coupe non publiée ».
Pistes **non vérifiées, à ne pas utiliser** sans nouvelle vérification : Ultra Prime 135
(US 2017/0307860 : verres donnés par leur nom, focale recalculée 133,1 au lieu de 135), Leica R et
Nikon AI-S (brevets des années 70, seulement en scan).

## Le calcul (module `core`, Kotlin pur, testé)

- Tracé **réel** surface par surface : intersection avec la sphère ou l'asphère
  (z = c·h²/(1+√(1−(1+K)c²h²)) + A4·h⁴ + A6·h⁶ + …, c = 1/rayon ; résoudre par Newton), puis
  réfraction de Snell en vecteurs, avec l'indice nd. Arrêt si le rayon dépasse le demi-diamètre ou
  sort par réflexion totale.
- Tests à écrire : pour chaque coupe, la focale paraxiale recalculée = `focaleCalculee` à 0,1 % ;
  un rayon proche de l'axe venant de l'infini coupe l'axe au foyer (`tirageCalcule` après la dernière surface).
- Le calcul de ~25 surfaces × ~25 rayons est léger : à refaire à chaque mouvement de curseur, hors du fil principal si besoin.

## Règles du dépôt

- Interface en français, tutoiement, ton des autres écrans. Composants existants : `Carte`,
  `ChoixSegmente`, `Etiquette`, `Ligne`, `Pastille`, `Slider` de Material3 ; dessin dans un `Canvas` Compose.
- `./gradlew :core:test --offline` doit passer ; l'appli ne se compile que sur GitHub Actions
  (workflow « Compiler », APK publié en release `apk`).
- Une seule branche : `claude/sunny-f16-rule-euj7nr`.

Sources : correspondances objectif → brevet d'après l'[Optical Bench Hub](https://photonstophotos.net/GeneralTopics/Lenses/OpticalBench/OpticalBenchHub.htm)
de Photons to Photos ; formules recopiées des brevets sur [Google Patents](https://patents.google.com)
(US 2007/0236805 A1 ; JP 2014-048488 A, 2015-114366 A, 2016-012034 A, 2019-117419 A, 2019-144477 A).
