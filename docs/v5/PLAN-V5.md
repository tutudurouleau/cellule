# Cellule v5 — le plan validé avec l'utilisateur

Débrief fait le 20 novembre 2026. Rien n'est encore codé pour la v5 :
**attendre le « top » de l'utilisateur avant de coder.** Une étape = un commit
= un APK envoyé = un retour de l'utilisateur.

L'idée de la v5 : rendre l'appli concrète en tournage. On prépare son tournage
avec, et on arrive chez le loueur avec un vrai dossier. Ce n'est pas
« une appli stylée dans le téléphone ».

## Les étapes, dans l'ordre

1. **Icônes normées du plan de feu** (`ecrans/EditeurSol.kt`, `sol/IconesSol.kt`).
   Un symbole par famille, dans les conventions des plans de feu (théâtre,
   logiciels de plans) : Fresnel, open face, découpe, PAR/HMI, panneau LED,
   tube, ballon, boîte à lumière, Kino. Chaque symbole garde son orientation et
   une étiquette (modèle, puissance, gélatine). La légende s'ajoute toute seule
   à l'export PDF. Il n'existe pas de norme officielle unique au cinéma :
   le dire, ne pas prétendre le contraire.
2. **Machinerie + électrique + lexique + marques, avec photos.**
   - Machinerie, large : clamps (Mafer, Super Clamp, Cardellini,
     Matthellini…), barres et tubes, rotules, embases, pieds (C-stand,
     wind-up, crank-up), gueuses et sacs, apple box (pleine, demi, quart,
     pancake), cadres, drapeaux, rails, dollies (Peewee, Fisher, Panther…),
     têtes et trépieds (bol 100/150, Mitchell, baby, high hat), grues.
     Pour chacune : à quoi ça sert, son nom sur le plateau, les chiffres clés
     (charge, poids, tailles), et un quiz dans Réviser.
   - Électrique : P17 en 16/32/63/125 A, mono et tri, Powerlock, Socapex,
     Powercon, triplettes, boîtiers de distribution, prolongateurs spéciaux,
     groupes électrogènes.
   - **Lexique « les mots du plateau »** : cherchable, avec flashcards dans
     Réviser (Apprendre / S'entraîner). **L'utilisateur peut ajouter ses
     propres mots** (mot, définition, photo facultative), révisés avec les
     autres. Vérifier chaque terme avant de l'écrire, le jargon varie selon
     les équipes. Exemple de l'utilisateur à vérifier, ne pas inventer :
     « passe-moi un U » = « U126 » ?
   - **Les marques** : une fiche par marque (pays, spécialité, produits
     phares). Les grosses (ARRI, Cooke, Zeiss, Angénieux, Matthews, Chapman,
     Sachtler, Kino Flo, K5600…) et les petites très présentes sur les
     plateaux (Kupo, Avenger, Nanlite, Astera, DZOFilm, Tilta, Teradek…).
3. **Catalogues des loueurs** : RVZ, TSF, Transpalux, Panavision (Alga),
   Next Shot, Cinesyl, Vantage. On ne fait pas « tout ce qui existe »,
   seulement ce qui est à leurs catalogues : projecteurs, objectifs, caméras,
   machinerie, électrique. Pour chaque référence : sa fiche technique
   (constructeur) et les loueurs qui l'ont. Chercher aussi s'ils publient un
   modèle de demande de devis (PDF, Excel) à reproduire et pré-remplir.
   + compléter les **photos qui manquent** (objectifs et caméras sans photo).
   + relire les PDF Kodak et remplir les durées ECN-2 restées vides
     (`core/LaboCouleur.kt`), ainsi que le lavage final de l'E-6.
4. **La liste de tout le tournage**, pas que la lumière : caméra (corps,
   optiques, follow focus, matte box, filtres, batteries, cartes, moniteurs,
   HF), machinerie, électrique, rangée par jour de tournage.
   + **accessoires ajoutés automatiquement** (un HMI amène son ballast, son
     câble de tête, son pied et sa gueuse ; une caméra amène ses batteries,
     son chargeur et ses cartes).
   + **prolongs calculés depuis le plan de feu** : longueurs de câble,
     répartition sur les phases, taille du groupe électrogène.
   + **export/import d'un plan en fichier et en QR code** (le pont vers
     l'éditeur PC de la v5.5 ; sert aussi à passer un plan à un collègue).
5. **PDF de demande de location par loueur** + **estimation de prix** +
   **check-list loueur** → `versionName = "5.0 (…)"`.
   - **Jamais d'envoi de mail depuis l'appli.** Seulement un PDF très
     professionnel, que l'utilisateur envoie lui-même.
   - Prix : fourchette par jour au tarif public quand le loueur le publie, et
     un carnet « mes tarifs » rempli avec les vrais devis. Plus il sert, plus
     l'estimation est juste. Ne pas promettre de précision qu'on n'a pas : les
     remises (école, prod) se négocient au cas par cas.
   - Check-list : cocher chaque pièce à l'enlèvement et au retour, noter la
     casse et les manques, avec photo.

Plus tard (pas dans la v5) : poids et volume du camion (12 m³, 20 m³…).

## v5.5 : l'éditeur de plan de feu sur PC

Une page web hors ligne (comme `index.html`), avec l'éditeur de plan à la
souris et au clavier, dans le même format de fichier que l'appli. Transfert
par QR code (plans raisonnables, plusieurs QR à la suite pour les gros) ou par
fichier, dans les deux sens.

## Règles fixées par l'utilisateur

- **Une photo pour chaque fiche technique**, même un bloc caméra nu (une
  Venice 2 sans rien). C'est indispensable pour les clamps, qui se ressemblent
  tous. Les photos embarquées restent sous licence libre (Commons, Flickr via
  Openverse, workflow `photos.yml`). Pour couvrir 100 % : chercher plus large,
  un lien « voir chez le fabricant ↗ », un bouton « ma photo » (l'utilisateur
  photographie lui-même), un dessin précis en dernier recours.
- Fiches techniques : chiffres du fabricant, sources citées, rien de seconde
  main (même règle que le Labo).

## Accès au web

- Cette session cloud ne voit ni le Chrome ni le PC de l'utilisateur.
- **Chemin GitHub** : les sites qui bloquent la session répondent aux machines
  de GitHub Actions (voir `.github/workflows/fiches.yml` et `photos.yml`). On
  peut y ajouter Playwright pour les sites des loueurs.
- Pour Claude in Chrome : il faut une session lancée **sur le PC** de
  l'utilisateur, dans un clone local de ce dépôt.
