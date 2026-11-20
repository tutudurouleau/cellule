# Refonte des sous-menus de Cellule — guide pour une nouvelle session

Ce guide contient tout ce qu'il faut pour refaire la navigation **à l'intérieur** des écrans
(sous-menus, onglets, retour, plan au sol), sans relire l'historique de la conversation
d'origine ni tout le dépôt.

---

## Où on en est

- **Étapes 1, 2 et 3 faites** (branche `claude/adoring-lamport-yln7ao`). L'utilisateur aime le
  sommaire Cinéma : « beaucoup plus lisible ». Reste l'étape 4 (finitions, §8).
- **Réviser** (`ecrans/EcranReviser.kt`) regroupe tous les quiz. Pour chaque thème, deux onglets :
  - Apprendre : les fiches de `core/FichesFormules.kt`, à lire ou à réciter ;
  - S'entraîner : les exercices de `core/QuizFormules.kt` (10 thèmes, 70 types), à choix ou à
    taper, réponses vérifiées par `SaisieReponse`.

  La carte « À revoir » reprend les erreurs. Il prépare le concours de Louis-Lumière : les
  exercices restent courts et faisables dans l'appli.
- **Sur le plateau**, en plus de la refonte :
  - le rapport caméra (`ecrans/RapportCamera.kt`, `core/RapportCamera.kt`), dans chaque jour de
    tournage, avec son PDF ;
  - la liste de location et le bilan électrique (`core/ListeLocation.kt`) : plan au sol › ⋯, et
    « À louer pour ce jour » ;
  - le raccord lumière (`ecrans/RaccordLumiere.kt`, `core/RaccordLumiere.kt`), dans la fiche de
    chaque plan.
- **Piège de compilation vu en vrai** : deux déclarations `private` de même nom dans deux
  fichiers du même paquet entrent en conflit (K2). Vérifie les noms avant de pousser.

## 0. À lire d'abord : économiser les crédits

- **Ne relis pas tout le dépôt.** La carte des fichiers (§3) dit où est chaque chose. N'ouvre
  que les fichiers de l'étape en cours, et seulement les fonctions citées.
- **Une étape = un commit = un APK = un retour de l'utilisateur.** N'enchaîne pas deux étapes
  sans son accord (§8).
- **Ne refais pas ce qui marche** : le lanceur « Tout », la barre des pages épinglées et le
  Labo plaisent à l'utilisateur. On ne touche pas aux calculs, seulement à la présentation.
- Réponds en **français**, en tutoyant, court et concret. L'utilisateur est étudiant en cinéma
  et teste sur son téléphone.

---

## 1. Le projet en bref

- **Cellule** : appli Android, posemètre photo et outils de tournage. Deux modules :
  - `core` : Kotlin pur, testé — `./gradlew :core:test --offline -q`.
  - `app` : Jetpack Compose + Material3. **L'appli ne compile que sur GitHub Actions** (pas de
    SDK Android dans la session).
- **Compilation** : le workflow « Compiler » (`.github/workflows/compiler.yml`) se lance à
  chaque push, **sur toutes les branches**. Il compile l'APK, vérifie que la signature est la
  même et que le numéro de version augmente, puis publie l'APK ici :
  https://github.com/tutudurouleau/cellule/releases/download/apk/cellule.apk
  - Un job vert = un APK installable par-dessus l'ancien.
  - Pour attendre la fin :
    `curl -s "https://api.github.com/repos/tutudurouleau/cellule/actions/runs?branch=<ta-branche>&per_page=1"`
    (environ 6 minutes).
  - Ensuite, télécharge l'APK et envoie-le à l'utilisateur avec 5 lignes de résumé.
  - Pas deux sessions qui poussent en même temps : la dernière écrase l'APK publié.
- **Branche** : le travail est sur `claude/sunny-f16-rule-euj7nr`. Si la session t'en impose
  une autre, crée-la depuis celle-ci.
- **Pour un commit qui ne touche que la doc**, ajoute `[skip ci]` au message.

---

## 2. Ce que l'utilisateur a décidé

| Sujet | Décision |
|---|---|
| Lanceur « Tout » + barre de 4 pages épinglées | **Aimé, on garde** |
| Labo (accueil en cartes) | **Aimé : c'est le modèle à suivre** |
| Les autres sous-menus | **Confus** : à refaire (diagnostic §4) |
| Unités (m/ft, lux/fc) | **Une seule fois, dans les Paramètres** |
| Fil d'Ariane | « Pourquoi pas » : à essayer |
| Règle des onglets | **Mitigé** : la version clarifiée est en §5 (R1), à lui faire valider à l'étape 1 |
| Plan au sol | Veut un rendu **plus professionnel** ; aujourd'hui « fouillis, un peu prototype » |

---

## 3. Carte des fichiers utiles

Tout est sous `app/src/main/kotlin/fr/cellule/app/` sauf mention contraire.

| Fichier | Ce qu'il contient |
|---|---|
| `MainActivity.kt` (≈ 810 l.) | Voir le détail ci-dessous |
| `Reglages.kt` | Préférences (`TextePref`, `BooleenPref`) : `lumiereMetres`, `lumiereLux`, `lumiereOutil`, `cineVue`, `navEpingles`, `navRecents` |
| `ecrans/Composants.kt` | Composants maison (liste plus bas) |
| `Theme.kt` | Couleurs, `Teinte` par onglet, dimensions (`Gouttiere`, `Interligne`, `RayonControle`, `RayonCarte`, `ZoneNavFlottante`) |
| `ecrans/EcranLabo.kt` | **Le modèle** : `VueLabo` (Accueil en cartes, Calcul, Chrono, Fiches, Tables, Maison, Quiz) et un `BackHandler` qui ramène à l'accueil. Reprendre le style de ses cartes d'accueil |
| `ecrans/OngletCinema.kt` | Voir le détail ci-dessous |
| `ecrans/Lumiere.kt` | Voir le détail ci-dessous |
| `ecrans/EcranCinema.kt`, `ecrans/Plans.kt` | `CarnetCinemaEcran` : jours de tournage, plans, PDF |
| `ecrans/EditeurSol.kt` (≈ 1 030 l.) | L'éditeur du plan au sol (détail en §7) |
| `sol/DessinSol.kt`, `sol/DessinCote.kt`, `sol/DessinVueCamera.kt`, `sol/ImageSol.kt` | Dessin du plan (`OptionsDessin`, `VueSol`, fantôme), vue de côté, vue caméra 3D, export PNG/PDF |
| `ecrans/DeplacerProjecteur.kt` | `BlocDeplacerProjecteur` (reculer, viser des lux, un diaph, un rapport), `CarteDeplacerSurPlan` |
| `PlansSol.kt` | Stockage JSON des plans |
| `core/src/main/kotlin/fr/cellule/core/PlanSol.kt` | `ElementSol`, `TypeElement` (12 types), `PlanSol`, `Sol` |
| `ecrans/EcranCameras.kt`, `ecrans/QuizCameras.kt` | Fiches : `ModeCameras` APPRENDRE / QUIZ en `ChoixSegmente`, puis dans `Catalogue` un `GenreMateriel` (Caméras / Objectifs / Projecteurs) en `ChoixSegmente` : **deux barres empilées** |
| `ecrans/EcranJournal.kt` | Carnet : `ModeCarnet` VUES / DEVELOPPEMENTS / TIRAGES en `ChoixSegmente` |
| `ecrans/EcranTables.kt`, `ecrans/TablesFormules.kt` | Tables : `RayonTables` EXPOSITION / DEVELOPPEMENT / FORMULES en `ChoixSegmente` |
| `ecrans/EcranMesurer.kt`, `ecrans/EcranEstimer.kt`, `ecrans/EcranFocales.kt` | Posemètre plein cadre (`ModeMesure` Spot / Moyenne / Incident), estimation, exercices de focales |

**Détail de `MainActivity.kt` :**
- `enum Onglet` : les 8 écrans racine (MESURER, ESTIMER, FOCALES, CARNET, LABO, TABLES,
  CAMERAS, CINEMA). `plein = true` : pas d'en-tête.
- `Application()` : l'onglet courant, le lanceur, l'en-tête repliable `EnTeteEcran` et le
  `when (onglet)`.
- `FeuilleParametres` : apparence, police, version.
- La barre du bas : `NavigationFlottante` et `ElementBarre`.
- `Aiguillage` : lien direct `"ECRAN:PAGE"`, lu par Carnet, Tables et Caméras.
- `Destination` / `DESTINATIONS` : les 26 pages, avec leur univers et leurs mots-clés.
- Le lanceur : `Lanceur` et `Tuile`.

**Détail de `ecrans/OngletCinema.kt` :**
- `EcranOngletCinema` : `VueCinema` VISEUR / PLANS / LUMIERE / MATERIEL, mémorisée dans
  `reglages.cineVue`.
- Le Viseur = `EcranMesurer(modeCinema = true, enTete = …, enTeteVertical = …)`.
- `EnTeteCinema` : un `ChoixSegmente` et le résumé du matériel (`resumeMateriel(reglages)`).
  En paysage, `SelecteurVueVertical`.
- `PageMateriel` et `BoutonFiche`.

**Détail de `ecrans/Lumiere.kt` :**
- `PageLumiere` : une rangée de 9 `Pastille` (`OutilLumiere`) **qui défile sur le côté**, puis
  2 `ChoixSegmente` d'unités (vers la ligne 150).
- Les outils : `OutilProjecteur`, `OutilPlacer`, `OutilRapports`, `OutilCouleur`,
  `OutilChamp`, `OutilElectricite`, `OutilScintillement`, `OutilConversions`.

**Composants maison** (`Composants.kt`) et leurs signatures :
- `Carte(titre, sousTitre, modifier) { ColumnScope }`
- `Ligne(intitule, valeur, detail, accent)`
- `Etiquette(texte, modifier, accent)`
- `ChoixSegmente(options, selection, libelle, surChoix)`
- `Deroulant(intitule, options, selection, libelle, modifier, surChoix)`
- `Champ(valeur, intitule, modifier, surUneLigne, indication, surChangement)`
- `BandeauEtat(texte, alerte)`
- `Pastille(texte, accent, modifier, surClic?)`
- `BoutonPlat(texte, actif, accent, erreur, modifier, surClic)`
- `LigneBoutons { RowScope }`
- Aussi : `Separateur`, `Tableau`, `Poignee`.

**Styles** : `Corps`, `Detail`, `TitreCarte`, `TitreEcran`, `ChiffreGrand`.

**Icônes** : elles sont dessinées par `icone(nom) { … }` dans `MainActivity.kt` (24 × 24,
trait 1,7). Les nouvelles icônes doivent suivre ce style.

---

## 4. Le diagnostic : pourquoi c'est confus

1. **Un même composant pour deux sens.** `ChoixSegmente` (les gélules côte à côte) sert :
   - à **changer de page** : Viseur | Plans | Lumière | Matériel, Apprendre | Quiz, Vues | Films | Tirages ;
   - à **choisir une valeur** : mètres | pieds, Seul | Total, Spot | Moyenne | Incident,
     Numérique | Pellicule.

   On ne sait jamais si on change d'écran ou un réglage.
2. **Des rangées qui défilent sur le côté** : les 9 outils Lumière, les boutons du plan au sol,
   la palette d'ajout. La moitié des choix est invisible.
3. **Deux barres empilées** (Fiches : Apprendre | Quiz, puis Caméras | Objectifs | Projecteurs).
4. **On ne sait pas où on est** : Cinéma n'a pas d'en-tête. Le bouton retour du téléphone ne
   remonte pas d'un niveau, sauf dans le Labo.
5. **Les unités répétées** en haut de chaque page Lumière.
6. **Plan au sol** : une barre de pastilles qui défile, 12 types à ajouter en ligne, les
   réglages d'un élément en un long formulaire, et des feuilles qui cachent le plan.

---

## 5. Les règles

### R1 — Trois façons de naviguer, pas une de plus

1. **Le lanceur** (global). Il existe déjà, on n'y touche pas.
2. **Le sommaire en cartes, façon Labo.** On l'utilise dès qu'une rubrique a **plus de 4 pages**,
   ou des pages qui sont des **outils différents**.
   - Chaque carte : icône, titre, une ligne « à quoi ça sert ».
   - Si c'est utile, un **résumé vivant** (par exemple « ALEXA 35 · 50 mm · EI 800 »).
3. **Les onglets soulignés** : 2 à 4 **vues d'une même chose**.
   - Exemples : Carnet (Vues · Films · Tirages), Tables, Fiches (Caméras · Objectifs ·
     Projecteurs), plan au sol (Dessus · Caméra · Côté).
   - Forme : texte simple, un trait sous l'onglet actif, toute la largeur, jamais de défilement.
   - C'est un nouveau composant `Onglets` dans `Composants.kt`, qui doit **se voir différent**
     de `ChoixSegmente`.

**Interdit :**
- changer de page avec `ChoixSegmente` ;
- une rangée qui défile pour naviguer ;
- deux rangées de navigation l'une sur l'autre.

`ChoixSegmente` reste, mais **seulement pour choisir une valeur**.

### R2 — Le fil d'Ariane et le retour

- **Le chemin au-dessus du titre.** Chaque page qui n'est pas à la racine l'affiche :
  « Cinéma › Lumière », puis le titre « Plan au sol ». Chaque morceau se touche et mène à son
  sommaire. Un bouton « ‹ » aussi.
- **Le retour du téléphone** (`BackHandler`) remonte d'un niveau, comme dans le Labo. À la
  racine, il garde son comportement normal.
- **Écrans plein cadre** (Viseur, Mesurer, Focales) : à la place de l'en-tête, une petite
  pastille translucide en haut à gauche, « ‹ Cinéma ». Elle ne doit pas cacher les mesures.
  - Le Viseur passe déjà par les emplacements `enTete` / `enTeteVertical` d'`EcranMesurer`.
- **Une seule source de vérité pour la navigation.**
  - Déplace `Destination`, `DESTINATIONS` et `Aiguillage` dans un nouveau fichier
    `Navigation.kt`.
  - Ajoute à `Destination` un champ `parent: String?`.
  - Ajoute les destinations des sommaires (`"cinema"`, `"lumiere"`).
  - Le fil d'Ariane, les sommaires et le lanceur lisent tous cette liste.
  - **Garde les identifiants actuels** : la barre épinglée les mémorise. Ce sont `mesurer`,
    `estimer`, `focales`, `carnet-vues`, `carnet-films`, `carnet-tirages`, `labo`,
    `cine-viseur`, `cine-plans`, `cine-materiel`, `lum-plan`, `lum-combien`, `lum-placer`,
    `lum-rapports`, `lum-couleur`, `lum-champ`, `lum-elec`, `lum-scint`, `lum-conv`,
    `fiches-cameras`, `fiches-objectifs`, `fiches-projecteurs`, `quiz`, `tables-exposition`,
    `tables-developpement`, `tables-formules`.

### R3 — Le même squelette pour chaque outil

1. En haut : le fil d'Ariane, le titre, **une phrase « à quoi ça sert »**.
2. Une carte **« Ce que tu donnes »** : les champs.
3. Une carte **« Résultat »** mise en avant : fond `primaryContainer`, chiffres en
   `ChiffreGrand`. C'est un nouveau composant `CarteResultat`.
4. **« Pour aller plus loin »**, replié : explications, formules, sources. C'est un nouveau
   composant `Depliable`.

Ne réécris aucun calcul : on **range** les blocs existants.

### R4 — Les unités dans les Paramètres

- Dans `FeuilleParametres`, ajoute une section « Unités » :
  - Distances : mètres | pieds → `reglages.lumiereMetres`
  - Éclairement : lux | footcandles → `reglages.lumiereLux`
- Retire les deux sélecteurs d'unités de `PageLumiere`.
- Tout le reste lit déjà ces deux préférences.

### R5 — Ce qui ne change pas

Le Labo (au plus, le fil d'Ariane si c'est simple), le lanceur, la barre épinglée, et tous
les calculs.

---

## 6. Écran par écran

**Cinéma** : un **sommaire de 4 grandes cartes**.
- **Viseur** : résumé du matériel.
- **Plans** : le prochain jour, le nombre de plans.
- **Lumière.**
- **Matériel** : caméra ou pellicule, objectif, sensibilité.

Ce qu'il faut changer :
- Retire le `ChoixSegmente` d'`EnTeteCinema` et `SelecteurVueVertical`.
- Ajoute une valeur pour l'accueil de `reglages.cineVue`.
- Garde `resumeMateriel(reglages)`.
- Le lanceur et la barre continuent d'ouvrir directement chaque page. Ajoute le sommaire
  « Cinéma » au lanceur.

**Lumière** : un **sommaire de 9 cartes en 3 groupes**.

| Groupe | Outils |
|---|---|
| Placer la lumière | Plan au sol, Combien j'ai, Où le placer, Rapports |
| Couleur et image | Couleur, Champ, Conversions |
| Sur le plateau | Électricité, Scintillement |

Ce qu'il faut changer :
- Retire la rangée de pastilles et les sélecteurs d'unités.
- Ajoute une valeur « sommaire » pour `reglages.lumiereOutil`.
- Chaque outil suit R3.

**Fiches (onglet racine CAMERAS)** :
- **Une seule rangée** d'onglets : Caméras · Objectifs · Projecteurs, plus la recherche.
- Le **Quiz devient une page à part** : une carte « Tester tes connaissances » en haut du
  catalogue l'ouvre, avec le fil « Fiches › Quiz ».
- Les liens `CAMERAS:CAMERA`, `CAMERAS:OBJECTIF`, `CAMERAS:PROJECTEUR` et `CAMERAS:QUIZ`
  doivent continuer de marcher.

**Carnet** : des onglets Vues · Films · Tirages à la place du `ChoixSegmente`. On garde
l'`Aiguillage` `CARNET`.

**Tables** : des onglets Exposition · Développement · Formules. On garde l'`Aiguillage`
`TABLES`.

**Mesurer, Estimer, Focales** : leurs modes sont des choix de valeur, donc on les garde. On
ajoute seulement la pastille de retour (R2) si c'est simple.

---

## 7. Le plan au sol « pro » (une étape à lui seul)

**Aujourd'hui (`EditeurPlanSol`)**, c'est un `Dialog` plein écran avec :
- un en-tête : « ‹ » et le nom du plan ;
- une rangée de pastilles qui défile : Réglages du plan, Lumière au sujet, Vue caméra, Vue de
  côté, Exporter, Tout voir ;
- la toile ;
- un panneau en bas : soit la palette « + type » qui défile, soit `ReglagesElement`, soit
  `BlocDeplacerProjecteur` ;
- des feuilles modales qui recouvrent le plan : `FeuilleOptionsSol`, `FeuilleLumiereSol`,
  `FeuilleVueCamera`, `FeuilleCoteSol`, `FeuilleExportSol`.

**La cible, en portrait :**

```
┌──────────────────────────────────────────────┐
│ ‹  Salon — séq. 12          ↶   ↷   ⋯        │  barre du haut
│ ──Dessus──    Caméra       Côté              │  onglets soulignés (3 vues du plan)
│                                              │
│                 (la toile)          [ + ]    │
│                                     [ − ]    │  zoom et « Tout voir »
│  0──1 m   N↑                        [ ⤢ ]    │
├──────────────────────────────────────────────┤
│  ＋Ajouter   ☀Lumière   📏Règle   ⇪Exporter   │  barre d'outils (icônes + noms)
└──────────────────────────────────────────────┘
```

- **Barre du haut** :
  - « ‹ » ;
  - le nom du plan (on le touche pour le renommer) ;
  - **Annuler ↶ / Rétablir ↷** ;
  - le menu « ⋯ » : Réglages du plan, Exporter, Supprimer le plan.
- **Onglets Dessus · Caméra · Côté** : les vues caméra et de côté passent **en plein
  espace** au lieu d'être des feuilles. Leurs réglages vont dans un panneau compact.
- **La toile** :
  - des boutons flottants zoom +, zoom − et « Tout voir » ;
  - l'échelle et le nord comme aujourd'hui ;
  - une option « Aimanter à la grille » (dans Réglages du plan) qui cale les positions sur
    `pasGrille`.
- **Barre d'outils fixe**, avec icônes et noms, sans défilement :
  - **Ajouter** ;
  - **Lumière** (lumière au sujet) ;
  - **Règle** : deux touches sur le plan → la distance, en mètres ou en pieds ;
  - **Exporter**.
- **« Ajouter »** ouvre une grille par familles, avec des icônes dessinées. On touche un type :
  l'élément est posé au centre et sélectionné.

  | Famille | Types |
  |---|---|
  | Personnes et caméra | Caméra, Comédien, Rail |
  | Lumière | Projecteur, Réflecteur, Cadre de diffusion, Drapeau |
  | Décor | Mur, Fenêtre, Porte, Meuble |
  | Repères | Repère |

- **L'inspecteur**, quand un élément est sélectionné, remplace la barre d'outils.
  - **En-tête** : icône, nom, type, bouton « OK ».
  - **Actions** (icônes + noms) : Dupliquer, Position suivante (caméra, comédien), Calculer
    (projecteur : ouvre `BlocDeplacerProjecteur`), Supprimer.
  - **Contenu en onglets** :
    - **Position** : x, y, orientation, hauteur ;
    - **Réglages** : les champs propres au type, tirés de `ReglagesElement` ;
    - **Lumière**, pour un projecteur : ce qu'il apporte aux sujets, et Déplacer.
  - **Hauteur** : 45 % de l'écran au plus, avec une poignée pour l'agrandir. **Le plan reste
    visible au-dessus.**
- **Annuler / Rétablir** : une pile de `PlanSol` (50 états au plus).
  - On empile un état **quand le doigt se lève**, et après la saisie d'un champ (attendre
    environ 600 ms).
  - Jamais à chaque image d'un glissé.
  - Tout reste en mémoire ; l'enregistrement (`PlansSol.enregistrer`) ne change pas.
- **Le style** :
  - barres d'outils sobres (`surfaceContainerHigh`) ;
  - icônes de 22 dp avec un nom en 10–11 sp ;
  - grille de 8 dp ;
  - **rien ne défile sur le côté**.
- **On garde tout ce qui existe** : le fantôme du déplacement, la vue caméra 3D, la vue de
  côté, l'export PNG et PDF, la carte OpenStreetMap, le soleil, la légende.

---

## 8. Les étapes (une à la fois, avec un APK à chaque fois)

1. **Les fondations, puis la Lumière** (le gain le plus visible, et le test des règles R1 et R2).
   - `Navigation.kt`, avec `parent` et les sommaires « cinema » et « lumiere ».
   - Les composants `Onglets`, `CarteSommaire` (même look que les cartes du Labo),
     `FilAriane`, `CarteResultat` et `Depliable`.
   - Les unités dans les Paramètres.
   - Le sommaire Lumière, le fil d'Ariane et le retour du téléphone.

   → Envoie l'APK et demande : **« La règle des onglets et des sommaires te va ? »**
2. **Le sommaire Cinéma** et la pastille du Viseur ; les onglets de Fiches, Carnet et Tables ;
   la page Quiz.
3. **Le plan au sol pro** (§7).
4. **Les finitions** : R3 sur tous les outils Lumière ; la pastille de retour sur Mesurer,
   Estimer et Focales ; le paysage.

**À chaque étape :**
1. `./gradlew :core:test --offline -q` (si `core` a bougé).
2. Un commit, puis un push.
3. Attendre la compilation (environ 6 min).
4. Envoyer l'APK et 5 lignes de résumé en français.

**À vérifier à chaque fois :**
- le paysage (`LocalPaysage`) ;
- le mode clair et le mode sombre ;
- un petit écran (moins de 360 dp de large) ;
- le lanceur ouvre toujours la bonne page.

**Pièges de compilation** (l'appli ne compile que sur la CI, donc relis-toi avant de pousser) :
- les imports de `LaunchedEffect`, `BackHandler` et `horizontalScroll` ;
- `@file:OptIn(ExperimentalMaterial3Api::class)` dans les fichiers qui utilisent
  `ModalBottomSheet` ;
- `Modifier.weight` n'existe que dans une `Row` ou une `Column` ;
- `return` dans une lambda Compose : `return@Carte`.

---

## 9. Hors du sujet de cette refonte

- `docs/coupes-objectifs/` : le dossier des coupes d'objectifs, prévu pour plus tard. N'y
  touche pas.
- La suite de la V3 (fiche lumière d'un plan dans le PDF, raccord lumière, liste de location
  et bilan électrique) viendra après la refonte.
