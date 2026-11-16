# Cahier des charges — Cellule tournage, repérage et dossiers PDF

Décidé avec l'utilisateur le 16 novembre 2026. À réaliser étape par étape, dans l'ordre ci-dessous :
une étape = un ou plusieurs commits, compilés par GitHub Actions (workflow « Compiler »), APK vérifié
(versionCode croissant, même certificat de signature), puis livré.

## Règles de l'appli à respecter (non négociables)

- **Répondre en français** à l'utilisateur ; textes de l'appli en français, tutoiement, ton sobre.
- **Aucun chiffre sans origine.** Toute donnée technique (taille de capteur, perte d'un filtre, etc.)
  vient d'une source nommée (fabricant de préférence). Ce qui n'est pas relu est affiché « à relire »
  ou saisi à la main, jamais inventé. Les sources reconnues non-fabricants sont acceptées si elles sont
  marquées comme telles.
- **Hors connexion d'abord.** Tout fonctionne sans réseau ; le réseau n'ajoute que des conforts
  (carte, adresse).
- **Batterie** (Redmi Note 8 Pro, écran LCD) : pas de flou, pas d'animation continue ; les outils
  lourds ne tournent que si on les active d'un bouton, et s'arrêtent quand on quitte l'écran.
- **Ne pas changer les 7 icônes de la barre du bas.**
- Logique pure et testable dans `core` (tests : `./gradlew :core:test --offline -q`), interface dans
  `app` (Compose). L'appli ne compile que sur GitHub Actions.
- Pousser uniquement sur la branche `claude/sunny-f16-rule-euj7nr`. Pas de pull request sans demande.

## Étape 1 — Cellule tournage (onglet Mesurer)

Un mode « Tournage » à côté de Spot / Moyenne / Incident (ou un sélecteur Photo | Tournage).

- **Cadence et obturateur**, modifiables : cadences 23,976 / 24 / 25 / 29,97 / 30 / 48 / 50 / 60 /
  100 / 120 im/s + saisie libre ; angles 360 / 270 / 180 / 172,8 / 144 / 90 / 45° + saisie libre.
  Vitesse = angle / (360 × cadence). Mémoriser les derniers choix.
- **EI** et **ouverture en T-stop** (T-stops au tiers).
- **ND proposé** : pour garder l'ouverture voulue, le ND (0,3 / 0,6 / 0,9 / 1,2 / 1,5 / 1,8 / 2,1…)
  à ajouter, avec l'écart résiduel. Mode inverse : « je veux T2,8 » → quel ND.
- **Autres filtres** cumulés dans l'exposition : polarisant, diffusion, couleur… Perte en diaphs
  issue d'une fiche fabricant quand elle existe, sinon réglable à la main (le filtre « maison »
  affiche « perte saisie par toi »). Ne pas inventer de pertes pour les diffusions.
- **Outils d'image, chacun derrière un bouton** (désactivés par défaut) :
  - faux contraste (palette par niveaux IRE) ;
  - zébras, deux seuils réglables (ex. 95 % et 70 %) ;
  - histogramme et forme d'onde, rafraîchis 5 à 10 fois par seconde au plus.
  - Rappeler qu'ils lisent l'image du téléphone, pas le capteur de la caméra.
- **Tous les capteurs du téléphone** : utiliser les différentes caméras disponibles (ultra grand-angle,
  principale, téléobjectif) via CameraX/Camera2 quand le téléphone les expose, pour couvrir les focales.

## Étape 2 — Capture de plan

Un bouton « Capturer le plan » dans la cellule tournage (et la cellule photo).

- **Photo pleine qualité enregistrée dans la galerie** (MediaStore, album « Cellule »). L'appli ne
  garde qu'une référence (URI) ; on peut aussi **importer une photo de la galerie** et lui attacher
  des métadonnées.
- **Cadre réaliste** (version de repli d'abord) : taille de capteur (ou de zone utile) en mm saisie
  ou choisie + focale en mm → champ horizontal/vertical, comparé au champ de la caméra du téléphone
  utilisée ; le cadre est dessiné par-dessus l'image. Si la focale demandée est plus large que
  toutes les caméras du téléphone : le dire (« trop large pour ce téléphone »), ne pas tricher.
- **Désanamorphose** ×1 / 1,3 / 1,5 / 1,8 / 2.
- **Formats de cadre** : garder ceux existants, ajouter les courants qui manquent : 1,33 ; 1,37
  (Academy) ; 1,43 (IMAX) ; 1,66 ; 1,78 (16:9) ; 1,85 ; 1,90 (DCI) ; 2,00 ; 2,20 ; 2,39 ; 2,76 ;
  4:5 et 9:16 (réseaux). Plusieurs cadres superposables.
- **Métadonnées enregistrées** : date et heure ; GPS (lat/long, précision, altitude) + lien Google
  Maps ; **adresse** seulement s'il y a du réseau ; **cap de la boussole** ; **position du soleil**
  (azimut, hauteur — calcul solaire existant dans `core`) ; exposition mesurée et réglages (cadence,
  obturateur, EI, T-stop, filtres) ; cadre (capteur, focale, format, désanamorphose) ; caméra du
  téléphone utilisée ; champs libres.
- **Numérotation au choix** : « Séq. / Plan / Prise » ou simple numéro.
- Autorisations : localisation (nouvelle), caméra (existante). Tout reste hors ligne sans elles.
- **Modifiable après coup** : focale, cadre, notes, numérotation, rangement.

## Étape 3 — Carnet en dossiers (onglet Carnet)

Nouveau sous-onglet **« Cinéma »** à côté de Vues / Films / Tirages.

- **Hiérarchie fixe** : Projet › Jour de tournage (ou de repérage) › Plans.
- Créer, renommer, supprimer (en deux temps) projets et jours ; déplacer un plan.
- Les vues photo existantes peuvent aussi être rangées dans un projet (lien, pas copie).
- Inclure ces données dans la sauvegarde/restauration JSON existante (`DepotJournal.versSauvegarde`),
  avec fusion sans doublon.

## Étape 4 — Export PDF (hors connexion)

Généré sur le téléphone (`android.graphics.pdf.PdfDocument`), partagé par l'intent de partage Android.
En couleur, sans logo.

- **Plan seul** : grande photo avec le ou les cadres ; bloc caméra / optique / filtres / exposition ;
  bloc lieu (coordonnées, lien Maps cliquable, adresse si connue, boussole, soleil et heure) ; notes.
- **Dossier** (un jour ou un projet entier) : page de garde (projet, dates, lieux), sommaire, une
  page par plan.
- Une **mini-carte** n'est ajoutée que s'il y a du réseau au moment de l'export ; sinon coordonnées
  + lien seulement.

## Étape 5 — Caméras réelles et leurs modes

- Dans la capture : « Caméra du catalogue » → **mode de capteur** (ex. Open Gate, 16:9, 4:3 pour une
  ALEXA Mini) → taille de zone utile remplie automatiquement.
- Données : tailles publiées par le fabricant (fiche technique) ; sinon source reconnue marquée ; sinon
  « à relire » et saisie manuelle. Tests vérifiant que chaque taille a une source.
- Option : séries d'objectifs du catalogue pour proposer leurs focales réelles.

## Étape 6 — Carte dans l'onglet Estimer

- Aujourd'hui le lieu est limité à quelques villes (Paris, Toulouse, Auch…). Ajouter : choix sur une
  **carte** (OpenStreetMap) quand il y a du réseau ; **ma position** via le GPS ; **saisie de
  coordonnées** hors ligne ; mémoriser les lieux récents.
- Le lieu choisi alimente le calcul solaire et peut être réutilisé dans la capture de plan.

## Plus tard (ne pas faire maintenant)

Comptes, synchronisation, partage en ligne.
