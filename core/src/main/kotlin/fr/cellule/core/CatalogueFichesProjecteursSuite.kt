package fr.cellule.core

/*
 * Second rayon de grandes fiches projecteurs. Même règle que le premier :
 * les chiffres viennent du catalogue photométrique (sources nommées), ce
 * qui se contredit est dit « à relire », et une année qu'on n'a pas
 * recoupée s'écrit « vers ».
 */

internal fun ledPonctuellesSuite(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "LS 600x Pro",
        fabricant = "Aputure",
        annee = 2021,
        categorie = "LED ponctuelle bicolore",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Le 600 qui passe du tungstène au jour d'un tour de molette.",
        chiffresCles = listOf(
            "Couleur" vaut "2700 à 6500 K",
            "Hyper-Reflector" vaut "5 610 lux à 3 m (5600 K)",
            "Fresnel F10 serrée" vaut "18 510 lux à 3 m (15°)",
            "À 2700 K" vaut "3 220 lux à 3 m"
        ),
        histoire = "Annoncé à l'été 2021, le 600x Pro reprend le corps du 600d Pro avec deux familles de LED, chaudes et froides, mélangées pour choisir sa température. C'est la réponse aux équipes qui passaient leur temps à gélatiner un projecteur lumière du jour.",
        ficheTechnique = "Source LED bicolore de 2700 à 6500 K, monture Bowens, tête et boîtier séparés. Aputure publie 5 610 lux à 3 m à 5600 K avec l'Hyper-Reflector, mais 3 220 seulement à 2700 K ; avec la Fresnel F10 serrée à 15°, 18 510 lux à 3 m.",
        commentLUtiliser = "En face douce derrière un cadre, en effet de lampe chaude en intérieur nuit, ou en raccord avec le jour par une fenêtre. Régler la température sur la balance caméra plutôt que gélatiner.",
        atouts = "Une seule tête pour le chaud et le froid, sans perte de gélatine ni stock de CTO.",
        limites = "Le mélange coûte de la lumière : aux extrêmes, une seule famille de LED travaille. Plus faible qu'un 600d Pro en lumière du jour.",
        aRetenir = "Un bicolore donne son maximum au milieu de sa plage : à 2700 K, ce 600 rend 40 % de moins qu'à 5600 K.",
        configurations = listOf("cat-aputure-ls-600x-pro-hyper", "cat-aputure-ls-600x-pro-f10")
    ),
    FicheProjecteur(
        nom = "STORM 1200x",
        fabricant = "Aputure",
        annee = 2024,
        anneeApprox = true,
        categorie = "LED ponctuelle bicolore de forte puissance",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La nouvelle génération des grosses têtes Aputure, bâtie sur le moteur BLAIR.",
        chiffresCles = listOf(
            "Réflecteur 45°" vaut "22 500 lux à 3 m",
            "Réflecteur 30°" vaut "41 800 lux à 3 m",
            "Réflecteur 15°" vaut "96 100 lux à 3 m",
            "Tête nue" vaut "8 490 lux à 3 m"
        ),
        histoire = "La gamme STORM succède aux Light Storm chez Aputure. Le 1200x en est la tête bicolore de forte puissance, présentée comme un outil de travail quotidien autour d'un nouveau moteur de LED baptisé BLAIR.",
        ficheTechnique = "Source LED ponctuelle bicolore, réflecteurs interchangeables. Aputure publie à 3 m et à 5600 K : 8 490 lux tête nue, 22 500 avec le réflecteur 45° fourni, 41 800 avec le 30°, 96 100 avec le 15°. À 3200 K, 19 500 lux avec le réflecteur 45°.",
        commentLUtiliser = "Pour traverser un grand cadre de diffusion, faire un soleil de fenêtre ou déboucher un extérieur. Le réflecteur 15° porte loin ; le 45° et une grande toile en font une source douce puissante.",
        atouts = "La puissance d'un HMI moyen, en bicolore, avec une famille de réflecteurs qui multiplie la portée.",
        limites = "Lourd, cher, et demande un circuit sérieux. La plage bicolore ne couvre pas les teintes saturées.",
        aRetenir = "Du réflecteur 45° au 15°, l'éclairement à 3 m passe de 22 500 à 96 100 lux : plus de quatre fois.",
        configurations = listOf("cat-aputure-storm-1200x")
    ),
    FicheProjecteur(
        nom = "STORM 80c",
        fabricant = "Aputure",
        annee = 2024,
        categorie = "Petite LED ponctuelle couleur",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une tête couleur de 80 W qui tient dans la main, avec les accessoires des grandes.",
        chiffresCles = listOf(
            "Puissance" vaut "80 W",
            "Réflecteur (35°)" vaut "19 850 lux à 1 m (5600 K)",
            "Avec Fresnel" vaut "36 000 lux à 1 m (5600 K)"
        ),
        histoire = "Présenté fin 2024, le STORM 80c met le moteur couleur de la gamme STORM dans une tête de 80 W, pensée pour les kits de trois projecteurs et les petits plateaux.",
        ficheTechnique = "Source LED ponctuelle couleur. Avec son réflecteur (≈ 35°), 19 850 lux à 1 m à 5600 K, 19 443 à 3200 K ; avec la Fresnel accessoire, 36 000 lux à 1 m en lumière du jour.",
        commentLUtiliser = "En contre, en effet de couleur dans un décor, en face à travers une petite boîte à lumière pour un entretien, ou en kit de trois pour un éclairage complet en voiture ou en appartement.",
        atouts = "Petit, couleur, et compatible avec la logique d'accessoires des grandes têtes.",
        limites = "80 W restent 80 W : c'est une source d'appoint, pas une face pour lutter contre le jour.",
        aRetenir = "Une petite tête couleur en contre fait souvent plus pour l'image qu'une grosse tête en face.",
        configurations = listOf("cat-aputure-storm-80c")
    ),
    FicheProjecteur(
        nom = "Electro Storm XT26",
        fabricant = "Aputure",
        annee = 2023,
        categorie = "LED ponctuelle bicolore de très forte puissance",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une tête LED pensée pour rivaliser avec les gros HMI, avec sa Fresnel de 14 pouces.",
        chiffresCles = listOf(
            "Réflecteur" vaut "65 200 lux à 3 m (à relire)",
            "Fresnel F14" vaut "18 à 45°",
            "Couleur" vaut "bicolore"
        ),
        histoire = "Annoncé en 2023, l'Electro Storm XT26 ouvre chez Aputure une gamme de projecteurs « de plateau », avec des accessoires motorisés, sur le terrain des HMI de plusieurs kilowatts.",
        ficheTechnique = "Source LED bicolore, réflecteur et Fresnel motorisée F14 de 14 pouces (18 à 45° selon le distributeur). Avec le réflecteur à 5600 K, les distributeurs donnent 65 200 lux à 3 m, un autre 58 600 : à relire.",
        commentLUtiliser = "En soleil lointain, en face à travers une très grande toile, ou en contre puissant de nuit extérieure. La Fresnel motorisée se règle à distance, sans monter à la nacelle.",
        atouts = "Une puissance de gros HMI sans ballast, sans temps d'amorçage, gradable sans changer de couleur.",
        limites = "Un projecteur lourd et cher, qui demande une vraie distribution électrique et du personnel pour le porter.",
        aRetenir = "Au-delà de 2 kW de LED, on ne remplace plus une lampe : on remplace une logistique d'HMI.",
        configurations = listOf("cat-aputure-electro-storm-xt26")
    ),
    FicheProjecteur(
        nom = "Forza 720B",
        fabricant = "Nanlite",
        annee = 2022,
        categorie = "LED ponctuelle bicolore",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La grosse bicolore de Nanlite, quand on veut chaud et froid avec de la portée.",
        chiffresCles = listOf(
            "Réflecteur 45°" vaut "7 919 lux à 3 m (5600 K)",
            "À 3200 K" vaut "6 604 lux à 3 m",
            "Couleur" vaut "bicolore"
        ),
        histoire = "Présentés au printemps 2022, les Forza 720 (lumière du jour) et 720B (bicolore) sont les plus puissants de la série Forza, face aux grosses têtes d'Aputure.",
        ficheTechnique = "Source LED bicolore, monture Bowens, réflecteur 45°. Nanlite publie à 3 m : 7 919 lux à 5600 K et 6 604 à 3200 K avec le réflecteur.",
        commentLUtiliser = "Derrière une grande diffusion en face, en lumière de fenêtre, en rattrapage du jour. Le passage du chaud au froid se fait au menu, sans filtre.",
        atouts = "Puissance et bicolore dans une monture Bowens, avec un large choix d'accessoires.",
        limites = "Moins puissante que la version lumière du jour, comme tout bicolore. Encombrante.",
        aRetenir = "Le « B » de Nanlite veut dire bicolore : même corps, moins de lumière, plus de souplesse.",
        configurations = listOf("cat-nanlite-forza-720b")
    ),
    FicheProjecteur(
        nom = "FS-300B",
        fabricant = "Nanlite",
        annee = 2022,
        anneeApprox = true,
        categorie = "LED ponctuelle bicolore de studio",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une tête de studio sur secteur, simple et abordable, pour les petits plateaux.",
        chiffresCles = listOf(
            "Réflecteur standard" vaut "38 720 lux à 1 m (5600 K)",
            "À 3200 K" vaut "38 900 lux à 1 m",
            "Tête nue" vaut "11 130 lux à 1 m"
        ),
        histoire = "La série FS de Nanlite vise le studio : alimentation intégrée sur secteur, pas de batterie, un prix contenu. Le FS-300B en est la version bicolore.",
        ficheTechnique = "Source LED bicolore, monture Bowens, alimentation intégrée. Nanlite publie à 1 m avec le réflecteur standard : 38 720 lux à 5600 K, 38 900 à 3200 K ; sans réflecteur, 11 130 lux.",
        commentLUtiliser = "En face dans une boîte à lumière pour les entretiens, en contre, en studio photo et vidéo. Idéal pour s'équiper sans exploser le budget.",
        atouts = "Bonne puissance pour le prix, bicolore, et tout dans un seul boîtier.",
        limites = "Pas d'alimentation sur batterie : il faut une prise.",
        aRetenir = "Un réflecteur multiplie ici l'éclairement par trois et demi à 1 m.",
        configurations = listOf("cat-nanlite-fs-300b")
    ),
    FicheProjecteur(
        nom = "Evoke 1200",
        fabricant = "Nanlux",
        annee = 2021,
        anneeApprox = true,
        categorie = "LED ponctuelle lumière du jour de forte puissance",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La marque « plateau » de Nanlite, et sa tête de 1 200 W pour les gros éclairages.",
        chiffresCles = listOf(
            "Réflecteur 45°" vaut "18 460 lux à 3 m",
            "Réflecteur 26°" vaut "58 050 lux à 3 m",
            "Réflecteur 60°" vaut "12 460 lux à 3 m",
            "Tête nue" vaut "5 567 lux à 3 m"
        ),
        histoire = "Nanlux est la gamme professionnelle du groupe Nanguang (Nanlite). L'Evoke 1200 vise les usages des HMI de taille moyenne.",
        ficheTechnique = "Source LED lumière du jour, réflecteurs dédiés de 26°, 45° et 60°. À 3 m, Nanlux publie 5 567 lux tête nue, 12 460 avec le 60°, 18 460 avec le 45° et 58 050 avec le 26°.",
        commentLUtiliser = "Pour traverser une grande toile, éclairer un décor de loin ou faire un soleil. Changer de réflecteur règle la portée sans toucher à la puissance.",
        atouts = "Très forte puissance, réflecteurs bien étagés, pilotage de plateau.",
        limites = "Lumière du jour uniquement (une version bicolore existe à part). Gros et cher.",
        aRetenir = "Le choix du réflecteur compte autant que la puissance : 12 460 ou 58 050 lux à 3 m avec la même tête.",
        configurations = listOf("cat-nanlux-evoke-1200")
    ),
    FicheProjecteur(
        nom = "KNOWLED MG1200Bi",
        fabricant = "Godox",
        annee = 2022,
        anneeApprox = true,
        categorie = "LED ponctuelle bicolore de forte puissance",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "L'entrée de Godox dans le cinéma lourd, avec sa gamme KNOWLED.",
        chiffresCles = listOf(
            "Réflecteur GR15" vaut "149 000 lux à 3 m (5600 K)",
            "Réflecteur GR45" vaut "21 900 ou 29 100 lux à 3 m (à relire)",
            "Couleur" vaut "bicolore"
        ),
        histoire = "Godox, connu pour ses flashs, a créé la gamme KNOWLED pour les plateaux de cinéma. Le MG1200Bi est sa tête bicolore de 1 200 W, présentée au salon IBC.",
        ficheTechnique = "Source LED bicolore, réflecteurs de 15° à 45° et Fresnel accessoire. Le catalogue Godox annonce 149 000 lux à 3 m à 5600 K avec le réflecteur GR15. Avec le GR45, les distributeurs donnent 21 900 ou 29 100 lux : à relire.",
        commentLUtiliser = "Comme une grosse tête Aputure ou Nanlux : soleil, traversée de grands cadres, face lointaine.",
        atouts = "Une très forte puissance en bicolore, à un prix souvent inférieur à la concurrence.",
        limites = "Des chiffres publiés qui varient selon les sources ; un réseau de loueurs moins dense que pour les marques installées.",
        aRetenir = "Le réflecteur le plus serré donne le chiffre le plus spectaculaire : comparer à réflecteur égal.",
        configurations = listOf("cat-godox-mg1200bi-gr15")
    ),
    FicheProjecteur(
        nom = "Orion 675 FS",
        fabricant = "Prolycht",
        annee = 2022,
        categorie = "LED ponctuelle couleur « full spectrum »",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une tête couleur à six familles de LED, pour une lumière blanche très fidèle.",
        chiffresCles = listOf(
            "Puissance" vaut "675 W",
            "Réflecteur 55°" vaut "10 170 lux à 3 m (5600 K)",
            "Réflecteur 30°" vaut "17 210 lux à 3 m",
            "Couleur" vaut "1600 à 20 000 K, RGBACL"
        ),
        histoire = "Prolycht a présenté l'Orion 675 FS en 2022. « FS » pour full spectrum : six familles de LED (rouge, vert, bleu, ambre, cyan, citron vert) pour remplir le spectre comme le fait une lampe à filament.",
        ficheTechnique = "Source LED RGBACL de 675 W, de 1600 à 20 000 K. À 3 m et à 5600 K, avec les réflecteurs : 10 170 lux (55°), 17 210 lux (30°) ; Prolycht annonce 28 100 lux avec sa Fresnel de 10 pouces serrée à 15°.",
        commentLUtiliser = "Quand la couleur de peau compte et qu'il faut raccorder des sources différentes. En face douce, en contre coloré, ou en Fresnel pour un faisceau propre.",
        atouts = "Un spectre plus complet que les LED à trois ou quatre couleurs, donc des peaux et des costumes plus justes.",
        limites = "Cher, et moins puissant qu'une tête lumière du jour de même consommation.",
        aRetenir = "Plus il y a de familles de LED, plus le blanc ressemble à celui d'une vraie lampe.",
        configurations = listOf("cat-prolycht-orion-675")
    ),
    FicheProjecteur(
        nom = "MOLUS X100",
        fabricant = "Zhiyun",
        annee = 2023,
        anneeApprox = true,
        categorie = "LED ponctuelle de poche",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une tête de 100 W qui tient dans une poche de veste.",
        chiffresCles = listOf(
            "Puissance" vaut "100 W",
            "Mini-réflecteur" vaut "17 317 lux à 1 m (à relire)",
            "Tête nue" vaut "3 881 lux à 1 m (4500 K)"
        ),
        histoire = "Zhiyun, connu pour ses stabilisateurs, a lancé la gamme MOLUS de lumières « de poche » : des sources COB très compactes, refroidies par ventilateur, pour les vidéastes qui voyagent léger.",
        ficheTechnique = "Source LED bicolore de 100 W. La boutique Zhiyun annonce 3 881 lux à 1 m tête nue et 17 317 avec le mini-réflecteur, à 4500 K ; une FAQ du fabricant donne 11 194 lux avec le réflecteur : à relire.",
        commentLUtiliser = "En contre, en effet de fenêtre dans une petite pièce, en face dans une mini boîte à lumière. Sur batterie, il suit la caméra partout.",
        atouts = "La taille et le poids : une vraie source dans un sac photo.",
        limites = "La chaleur limite la durée à pleine puissance ; le ventilateur s'entend.",
        aRetenir = "La miniaturisation des LED a mis 100 W dans la main : il reste à les diffuser.",
        configurations = listOf("cat-zhiyun-molus-x100")
    ),
    FicheProjecteur(
        nom = "DS6",
        fabricant = "Digital Sputnik",
        annee = 2017,
        anneeApprox = true,
        categorie = "LED modulaire couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Six modules LED en grappe, qu'on règle chacun : une source qui se dessine.",
        chiffresCles = listOf(
            "Consommation" vaut "600 W au plus",
            "Faisceau" vaut "20° sans diffusion",
            "Couleur" vaut "RGBW"
        ),
        histoire = "La marque estonienne Digital Sputnik a construit son système autour de modules LED identiques qu'on assemble : DS1, DS3, DS6, selon le nombre de modules.",
        ficheTechnique = "Six modules LED RGBW pilotables individuellement, faisceau de 20° sans diffusion, alimentation tirant au plus 600 W. Le fabricant ne publie pas d'éclairement dans les fiches consultées ; B&H le compare à un HMI 4 kW Fresnel en position ouverte, ce qui est une comparaison qualitative.",
        commentLUtiliser = "En source dure et colorée, en effet (police, néon, feu), ou avec une diffusion devant pour une face douce. Chaque module pouvant prendre sa couleur, on fabrique des dégradés.",
        atouts = "Modulaire, pilotable module par module, robuste.",
        limites = "Pas de photométrie publiée : il faut mesurer soi-même.",
        aRetenir = "Sans chiffre du fabricant, le seul juge est la cellule.",
        configurations = emptyList()
    )
)

internal fun fresnelsEtDecoupes(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "L7-C",
        fabricant = "ARRI",
        annee = 2012,
        anneeApprox = true,
        categorie = "Fresnel LED couleur",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "La Fresnel de studio passée à la LED, en gardant son faisceau.",
        chiffresCles = listOf(
            "Lentille" vaut "175 mm",
            "Serré (15°)" vaut "5 180 lux à 3 m (5600 K)",
            "Ouvert (50°)" vaut "688 lux à 3 m",
            "Couleur" vaut "réglable, RGBW"
        ),
        histoire = "La série L d'ARRI (L5, L7, L10) a transposé à la LED la Fresnel classique : une lentille à échelons, un faisceau qu'on serre ou qu'on ouvre, des volets — et en plus la couleur réglable. Le L7-C a été adopté par de nombreux studios de télévision.",
        ficheTechnique = "Moteur LED couleur, lentille Fresnel de 175 mm, faisceau de 15° à 50°. Pour la première version à 5600 K : 5 180 lux à 3 m en position serrée, 688 en position ouverte (relayé par B&H). La version LE2 annonce 25 % de lumière en plus.",
        commentLUtiliser = "Comme une Fresnel tungstène de 650 W à 1 kW : en contre, en découpe, en face à travers une diffusion. Couleur et intensité se règlent depuis la console.",
        atouts = "Un vrai faisceau de Fresnel, net et découpable aux volets, sans chaleur et avec la couleur.",
        limites = "Moins puissant qu'une Fresnel tungstène de même taille ; plusieurs versions coexistent, aux photométries différentes.",
        aRetenir = "Toujours vérifier la version (C, C LE2, Plus) : les chiffres changent du simple au double.",
        configurations = listOf("cat-arri-l7c")
    ),
    FicheProjecteur(
        nom = "L10-C",
        fabricant = "ARRI",
        annee = 2015,
        anneeApprox = true,
        categorie = "Fresnel LED couleur",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "La grande sœur du L7 : une Fresnel LED de 10 pouces pour les studios.",
        chiffresCles = listOf(
            "Serré (15°)" vaut "5 552 lux à 5 m (5600 K)",
            "Ouvert (50°)" vaut "768 lux à 5 m",
            "Couleur" vaut "2800 à 10 000 K"
        ),
        histoire = "Le L10 complète la série L d'ARRI avec une lentille plus grande et plus de puissance, pour les plateaux de télévision et les studios.",
        ficheTechnique = "Moteur LED couleur de 2800 à 10 000 K, faisceau de 15° à 50°. À 5 m et 5600 K : 5 552 lux serré, 768 ouvert (fiche ARRI reproduite par un loueur). Le calculateur ARRI donne 1 300 lux à 10 m en serré à 3200 K.",
        commentLUtiliser = "En face ou en contre dans un studio, en lumière découpée sur un décor, en effet coloré. Accroché au gril, piloté en DMX.",
        atouts = "Faisceau propre, couleur réglable, pas de chaleur sur le plateau.",
        limites = "Lourd pour la location légère ; moins puissant qu'un 2 kW tungstène serré.",
        aRetenir = "Une Fresnel LED se règle comme une Fresnel tungstène : seule la lampe a changé.",
        configurations = listOf("cat-arri-l10c")
    ),
    FicheProjecteur(
        nom = "Source Four",
        fabricant = "ETC",
        annee = 1992,
        anneeApprox = true,
        categorie = "Découpe (ellipsoïdal)",
        silhouette = Silhouette.PROJ_DECOUPE,
        punchline = "La découpe de théâtre devenue l'outil des formes nettes au cinéma.",
        chiffresCles = listOf(
            "Lampe" vaut "750 W",
            "Lentille 26°" vaut "176 255 cd",
            "Soit" vaut "1 763 lux à 10 m"
        ),
        histoire = "Le Source Four d'ETC a redéfini la découpe de scène : un réflecteur ellipsoïdal, une lampe compacte, et des tubes d'objectif interchangeables (19°, 26°, 36°…). Il est passé du théâtre au cinéma pour tout ce qui demande un bord net.",
        ficheTechnique = "Projecteur à réflecteur ellipsoïdal et lentilles, lampe de 750 W. ETC publie 176 255 candelas au centre avec la lentille 26°, soit environ 1 763 lux à 10 m. Couteaux de découpe intégrés, porte-gobo, iris en option.",
        commentLUtiliser = "Pour projeter une forme : fenêtre sur un mur, feuillage avec un gobo, rai de lumière sur un visage. On règle la netteté à l'objectif et on taille aux couteaux.",
        atouts = "Des bords parfaitement nets, des formes projetées, un faisceau qui ne déborde pas.",
        limites = "Peu de lumière pour sa consommation, beaucoup de chaleur ; l'angle est fixé par le tube d'objectif.",
        aRetenir = "La découpe ne sert pas à éclairer : elle sert à dessiner avec la lumière.",
        configurations = listOf("cat-etc-source-four-26")
    ),
    FicheProjecteur(
        nom = "DLH4",
        fabricant = "Dedolight",
        annee = 1990,
        anneeApprox = true,
        categorie = "Petite optique tungstène focalisable",
        silhouette = Silhouette.PROJ_DECOUPE,
        punchline = "Le petit projecteur à optique asphérique qui a fait le tour des tournages légers.",
        chiffresCles = listOf(
            "Lampe" vaut "150 W, 3200 K",
            "Serré" vaut "9 100 lux à 3 m",
            "Ouvert" vaut "445 lux à 3 m"
        ),
        histoire = "Dedo Weigert a conçu le Dedolight autour d'une double optique asphérique qui concentre la lumière d'une petite lampe bien mieux qu'une Fresnel classique. Le DLH4 de 150 W est devenu le projecteur des kits d'interview et de documentaire.",
        ficheTechnique = "Lampe halogène de 150 W à 3200 K, optique à deux lentilles. Le loueur Panavision publie 9 100 lux à 3 m en serré et 445 en ouvert ; B&H donne 3 228 lux à 1 m en ouvert (48°).",
        commentLUtiliser = "En contre sur une chevelure, en accent sur un objet, en découpe sur un fond, en projection avec l'accessoire d'imagerie. Trois Dedo font un éclairage d'entretien complet.",
        atouts = "Un faisceau très propre et très efficace pour 150 W, un rapport serré-ouvert immense, un poids plume.",
        limites = "Peu de lumière en absolu, et une lampe qui chauffe. Les modèles LED (DLED) l'ont largement remplacé.",
        aRetenir = "Bien concentrer la lumière vaut parfois mieux que la multiplier : 150 W bien serrés font 9 100 lux à 3 m.",
        configurations = listOf("cat-dedolight-dlh4")
    )
)

internal fun panneauxSuite(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "SkyPanel X",
        fabricant = "ARRI",
        annee = 2023,
        categorie = "Panneau LED modulaire couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le successeur du SkyPanel : des modules qu'on assemble, durs ou doux.",
        chiffresCles = listOf(
            "Mode dur, 5600 K" vaut "4 800 à 14 400 lux à 10 m selon la taille (à relire)",
            "Flux" vaut "51 809 à 155 427 lm",
            "Couleur" vaut "réglable, six couleurs"
        ),
        histoire = "Présenté par ARRI à l'IBC 2023, le SkyPanel X abandonne le panneau unique : on assemble des modules (X21, X22, X23…) et on choisit entre une lumière dure à face ouverte et une lumière douce diffusée.",
        ficheTechnique = "Modules LED six couleurs, configurations en un, deux ou trois modules. La fiche ARRI de septembre 2023 donne 4 800, 9 600 et 14 400 lux à 10 m en mode dur à 5600 K, sans dire quelle configuration correspond à chaque valeur (à relire) ; flux de 51 809 à 155 427 lumens. La fiche de 2024 renvoie à l'application ARRI Photometrics.",
        commentLUtiliser = "En lumière dure qui porte loin, comme un HMI, ou diffusée en grande face douce. Les modules se combinent selon la taille de source voulue.",
        atouts = "Un même système pour la lumière dure et douce, et la couleur ARRI.",
        limites = "Lourd et cher ; les chiffres dépendent fortement de la configuration choisie.",
        aRetenir = "Un panneau modulaire n'a pas « une » photométrie : il en a autant que de façons de l'assembler.",
        configurations = emptyList()
    ),
    FicheProjecteur(
        nom = "NOVA II 2x1",
        fabricant = "Aputure",
        annee = 2025,
        categorie = "Panneau LED couleur de forte puissance",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "La seconde génération du Nova : un panneau 2×1 qui pousse très fort.",
        chiffresCles = listOf(
            "Sans diffuseur" vaut "28 380 lux à 3 m (5600 K)",
            "Diffuseur plat" vaut "5 130 lux à 3 m",
            "À 3200 K" vaut "27 750 lux à 3 m"
        ),
        histoire = "Annoncé en septembre 2025, le NOVA II 2x1 reprend le format du Nova P600c avec le moteur couleur BLAIR d'Aputure et beaucoup plus de puissance.",
        ficheTechnique = "Panneau LED couleur 2×1. La fiche Aputure de mars 2026 donne 28 380 lux à 3 m à 5600 K sans diffuseur (24 700 lux annoncés au lancement), 27 750 à 3200 K, et 5 130 lux avec le diffuseur plat.",
        commentLUtiliser = "Sans diffuseur pour traverser un grand cadre comme le ferait une tête ponctuelle ; avec diffuseur en face douce directe.",
        atouts = "La surface d'un panneau avec une puissance proche des grosses têtes ponctuelles, et la couleur.",
        limites = "Encombrant et lourd ; la diffusion divise l'éclairement par plus de cinq.",
        aRetenir = "Un diffuseur ne « perd » pas la lumière pour rien : il agrandit la source, donc adoucit les ombres.",
        configurations = listOf("cat-aputure-nova-ii-2x1")
    ),
    FicheProjecteur(
        nom = "Vortex8",
        fabricant = "Creamsource",
        annee = 2020,
        categorie = "Panneau LED couleur étanche",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Un panneau australien de 650 W qui ne craint pas la pluie.",
        chiffresCles = listOf(
            "Consommation" vaut "650 W",
            "Lentille 20°" vaut "13 900 lux à 3 m (5600 K)",
            "Face ouverte" vaut "110°"
        ),
        histoire = "Creamsource, marque australienne, a présenté le Vortex8 en 2020 : un panneau couleur étanche, pensé pour les tournages en extérieur et les effets de pluie.",
        ficheTechnique = "Panneau LED couleur de 650 W, face ouverte à 110° (demi-intensité) ou lentille 20°. Avec la lentille 20° : 13 900 lux à 3 m à 5600 K, 12 700 à 3200 K.",
        commentLUtiliser = "En extérieur sous la pluie, en face douce avec diffusion, ou serré à 20° pour porter. Les Vortex s'accrochent en grappes pour de grandes sources.",
        atouts = "Étanche, robuste, couleur réglable, et une lentille qui change sa portée.",
        limites = "Lourd pour sa taille ; la lentille n'est pas fournie d'office selon les kits.",
        aRetenir = "L'étanchéité change la journée : on n'arrête pas de tourner à la première averse.",
        configurations = listOf("cat-creamsource-vortex8")
    ),
    FicheProjecteur(
        nom = "Vortex24",
        fabricant = "Creamsource",
        annee = 2020,
        anneeApprox = true,
        categorie = "Grand panneau LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le grand Vortex, pour les grandes sources de plateau.",
        chiffresCles = listOf(
            "Sans diffuseur" vaut "27 560 lux à 3 m (à relire)",
            "Diffuseur plat" vaut "3 730 lux à 3 m",
            "Famille" vaut "Vortex, comme le Vortex8"
        ),
        histoire = "Grand frère du Vortex8 chez Creamsource, le Vortex24 vise les grandes sources de plateau.",
        ficheTechnique = "Grand panneau LED couleur. La fiche Creamsource reproduite par un loueur donne 27 560 lux à 3 m sans diffuseur, sans préciser la température de couleur (à relire), et 3 730 lux avec le diffuseur plat.",
        commentLUtiliser = "En grande face douce, en ciel au-dessus d'un décor, ou en source dure puissante sans diffuseur.",
        atouts = "Une très grande source couleur, pilotable.",
        limites = "Gros, lourd, et demande des accroches solides.",
        aRetenir = "Le diffuseur divise ici l'éclairement par sept : la douceur se paie.",
        configurations = listOf("cat-creamsource-vortex24")
    ),
    FicheProjecteur(
        nom = "Astra 6X",
        fabricant = "Litepanels",
        annee = 2017,
        anneeApprox = true,
        categorie = "Panneau LED 1×1 bicolore",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le 1×1 de reportage par excellence : léger, sur batterie, bicolore.",
        chiffresCles = listOf(
            "À 3 m, 5600 K" vaut "1 583 lux",
            "À 3 m, 3200 K" vaut "1 421 lux",
            "Faisceau" vaut "46°"
        ),
        histoire = "La gamme Astra de Litepanels a popularisé le panneau 1×1 sur batterie dans le reportage et l'entretien. L'Astra 6X en est la version la plus puissante.",
        ficheTechnique = "Panneau LED 1×1 bicolore, faisceau de 46°, alimentation secteur ou batterie. Litepanels publie 1 583 lux à 3 m à 5600 K et 1 421 à 3200 K.",
        commentLUtiliser = "En face à travers un petit cadre de diffusion, en débouchage, en lumière d'appoint en reportage. Il tient sur un pied léger.",
        atouts = "Léger, autonome, bicolore, et une lumière plus douce qu'une tête ponctuelle nue.",
        limites = "Peu de portée ; sans diffusion, les nombreuses LED donnent des ombres multiples.",
        aRetenir = "Un 1×1 nu n'est pas une source douce : c'est une source large mais granuleuse.",
        configurations = listOf("cat-litepanels-astra-6x")
    ),
    FicheProjecteur(
        nom = "Titan X2",
        fabricant = "Rotolight",
        annee = 2020,
        anneeApprox = true,
        categorie = "Panneau LED 2×1 couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le panneau britannique qui mise sur la puissance douce et les effets.",
        chiffresCles = listOf(
            "À 3 m, 3200 K" vaut "2 220 lux",
            "À 3 m, 5600 K" vaut "2 260 lux",
            "Format" vaut "2×1"
        ),
        histoire = "Rotolight, fabricant britannique, a présenté le Titan X2 fin 2019 comme « le panneau doux 2×1 le plus lumineux ».",
        ficheTechnique = "Panneau LED couleur 2×1. La notice Rotolight (essais de septembre 2020) donne 2 220 lux à 3 m à 3200 K et 2 260 à 5600 K ; les distributeurs annoncent 2 415 à 5600 K.",
        commentLUtiliser = "En face douce, en ambiance, et pour ses effets programmés (orage, feu, télévision).",
        atouts = "Une grande surface, la couleur, des effets intégrés.",
        limites = "Encombrant ; la portée reste celle d'un panneau.",
        aRetenir = "Un panneau 2×1 tient bien la comparaison avec une tête diffusée, sans le cadre à monter.",
        configurations = listOf("cat-rotolight-titan-x2")
    ),
    FicheProjecteur(
        nom = "FreeStyle 41",
        fabricant = "Kino Flo",
        annee = 2018,
        anneeApprox = true,
        categorie = "Panneau LED allongé",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le 4Bank réinventé en LED : la même forme, la même douceur, la couleur en plus.",
        chiffresCles = listOf(
            "À 1 m" vaut "2 450 lux",
            "À 2 m" vaut "712 lux",
            "Nid d'abeille" vaut "90° (60° en option)"
        ),
        histoire = "Avec la gamme FreeStyle, Kino Flo a transposé ses projecteurs à tubes fluorescents en LED, en gardant la forme allongée et les nids d'abeille qui avaient fait leur succès.",
        ficheTechnique = "Panneau LED allongé, couleur réglable. Valeurs relayées par les distributeurs avec le nid d'abeille 90° : 2 450 lux à 1 m, 712 à 2 m.",
        commentLUtiliser = "Comme un Kino à tubes : en face douce proche, avec le nid d'abeille pour tenir la lumière hors du décor.",
        atouts = "Une lumière douce et plate, ergonomique, avec la couleur réglable.",
        limites = "Portée courte, comme toute source large.",
        aRetenir = "Le nid d'abeille garde la douceur mais empêche la lumière de baver sur les murs.",
        configurations = listOf("cat-kinoflo-freestyle-41")
    ),
    FicheProjecteur(
        nom = "Diva-Lite 41 LED",
        fabricant = "Kino Flo",
        annee = 2016,
        anneeApprox = true,
        categorie = "Panneau LED de studio",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le Diva-Lite des plateaux de télévision, passé à la LED.",
        chiffresCles = listOf(
            "À 1 m" vaut "2 450 lux (à relire)",
            "À 3 m" vaut "325 lux (à relire)",
            "Couleur" vaut "2700 à 6500 K"
        ),
        histoire = "Le Diva-Lite à tubes était un classique des plateaux d'interview. Kino Flo en a montré la version LED au NAB 2016.",
        ficheTechnique = "Panneau LED de 2700 à 6500 K. Deux distributeurs donnent 2 450 lux à 1 m et 325 lux à 3 m ; un troisième précise que le fabricant ne publie pas ces valeurs : à relire.",
        commentLUtiliser = "En face douce pour un entretien, en débouchage, avec son nid d'abeille.",
        atouts = "Simple, doux, réglable en température.",
        limites = "Des chiffres non confirmés par le fabricant ; portée courte.",
        aRetenir = "Quand le fabricant ne publie rien, les chiffres des revendeurs sont des indices, pas des preuves.",
        configurations = listOf("cat-kinoflo-diva-lite-41")
    ),
    FicheProjecteur(
        nom = "LiteMat Spectrum 4",
        fabricant = "LiteGear",
        annee = 2019,
        anneeApprox = true,
        categorie = "Tapis LED souple couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Un tapis de LED qu'on scratche dans un cadre : la source douce qui se plie.",
        chiffresCles = listOf(
            "À 3 m (version 2023)" vaut "834 lux",
            "À 1 m, 3200 K" vaut "7 510 lux",
            "Format" vaut "souple, en cadre"
        ),
        histoire = "LiteGear a fait du tapis LED souple un standard : des LED cousues sur une toile, tendues dans un cadre léger avec leur diffusion, qu'on glisse dans un plafond, une voiture ou un décor exigu.",
        ficheTechnique = "Tapis LED couleur. Pour la version 2023, le loueur Panavision donne 834 lux à 3 m à 3200 et 5600 K, et un distributeur 7 510 lux à 1 m à 3200 K. La version 2019 mesurait autrement : ne pas mélanger les générations.",
        commentLUtiliser = "En plafond de voiture, dans un faux plafond de décor, au-dessus d'une table, ou en face douce légère au bout d'une perche.",
        atouts = "Léger, plat, pliable : une grande source douce là où rien d'autre n'entre.",
        limites = "Portée courte ; il faut un gradateur dédié.",
        aRetenir = "Le tapis LED a remplacé les cadres de tubes fluo dans les voitures et les plafonds.",
        configurations = listOf("cat-litegear-litemat-spectrum-4")
    ),
    FicheProjecteur(
        nom = "SL1 Mix",
        fabricant = "DMG Lumière",
        annee = 2018,
        anneeApprox = true,
        categorie = "Panneau LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le panneau français de Rosco, réglable de 1700 à 10 000 K.",
        chiffresCles = listOf(
            "Consommation" vaut "200 W au plus",
            "Couleur" vaut "1700 à 10 000 K",
            "Petit modèle" vaut "Mini Mix, 100 W"
        ),
        histoire = "DMG Lumière, né en France et repris par Rosco, a conçu la technologie Mix : un mélange de LED pensé pour des blancs justes sur toute la plage, et des couleurs saturées.",
        ficheTechnique = "Panneau LED couleur, 200 W au plus (100 W pour le Mini Mix), de 1700 à 10 000 K avec correction vert / magenta. La notice consultée ne donne pas d'éclairement : à mesurer.",
        commentLUtiliser = "En face douce, en lumière d'appoint couleur, en raccord avec des sources très chaudes comme une bougie (1700 K).",
        atouts = "Une plage de température exceptionnellement large, une construction légère.",
        limites = "Pas de photométrie dans les documents consultés ; puissance modeste.",
        aRetenir = "Descendre à 1700 K permet de raccorder une flamme sans gélatine.",
        configurations = emptyList()
    ),
    FicheProjecteur(
        nom = "4Bank",
        fabricant = "Kino Flo",
        annee = 1990,
        anneeApprox = true,
        categorie = "Projecteur à tubes fluorescents",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le projecteur fluo qui a changé la lumière douce du cinéma.",
        chiffresCles = listOf(
            "À 1 m" vaut "2 599 lux",
            "À 2 m" vaut "775 lux",
            "Tubes" vaut "quatre, de 4 pieds"
        ),
        histoire = "Kino Flo est né de tubes fluorescents bricolés sur un tournage pour éclairer un espace exigu. Le 4Bank, avec son ballast séparé et ses tubes à la couleur maîtrisée, est devenu pendant des décennies le synonyme de lumière douce qui ne chauffe pas.",
        ficheTechnique = "Quatre tubes fluorescents de 4 pieds, ballast séparé, nid d'abeille. La brochure Kino Flo donne 2 599 lux à 1 m et 775 à 2 m. Tubes interchangeables lumière du jour ou tungstène.",
        commentLUtiliser = "En face douce proche, en débouchage, en lumière de plafond. On allume deux ou quatre tubes pour doser.",
        atouts = "Une source large, très douce, qui ne chauffe pas : une révolution face au tungstène.",
        limites = "Portée courte, tubes fragiles, et une couleur qui dépend des tubes. Largement remplacé par la LED.",
        aRetenir = "Le mot « Kino » désigne encore sur les plateaux une source douce et plate.",
        configurations = listOf("cat-kinoflo-4bank-4ft")
    )
)

internal fun tubesSuite(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "INFINIBAR",
        fabricant = "Aputure",
        annee = 2023,
        categorie = "Barres LED pixel couleur",
        silhouette = Silhouette.PROJ_TUBE,
        punchline = "Des barres qu'on aboute sans trou de lumière, pour des lignes et des effets.",
        chiffresCles = listOf(
            "PB12 (4 pieds)" vaut "593 lux à 1 m (5600 K)",
            "Faisceau" vaut "120°",
            "Couleur" vaut "2000 à 10 000 K, pixels"
        ),
        histoire = "Lancées par Aputure début 2023, les INFINIBAR sont des barres LED pixellisées conçues pour être assemblées bout à bout sans interruption visible.",
        ficheTechnique = "Barres LED couleur pixellisées (PB3, PB6, PB12). Aputure publie pour la PB12 à 1 m : 593 lux à 5600 K, 542 à 3200 K, 617 à 7500 K, faisceau de 120°.",
        commentLUtiliser = "En lignes de lumière dans le champ, en lisières cachées, en effets (défilement, police, feu). Aboutées, elles font des contours ou des plafonds.",
        atouts = "Modulaires, pixellisées, assemblables sans trou.",
        limites = "Peu de puissance : ce sont des accents et des éléments de décor.",
        aRetenir = "Les barres pixel servent autant à être vues qu'à éclairer.",
        configurations = listOf("cat-aputure-infinibar-pb12")
    ),
    FicheProjecteur(
        nom = "Rainbow 2",
        fabricant = "Quasar Science",
        annee = 2022,
        anneeApprox = true,
        categorie = "Tubes LED couleur",
        silhouette = Silhouette.PROJ_TUBE,
        punchline = "Le tube qui remplace un tube fluo, en couleur et pilotable.",
        chiffresCles = listOf(
            "4 pieds, à 3 m" vaut "92 lux (5600 K)",
            "À 1 m (calculé)" vaut "828 lux",
            "Couleur" vaut "RGBX"
        ),
        histoire = "Quasar Science s'est fait connaître avec des tubes LED de la forme des tubes fluorescents, pour remplacer les tubes des décors et des plafonds. Le Rainbow 2 en est la version couleur pilotable.",
        ficheTechnique = "Tube LED couleur RGBX de 4 pieds. B&H relaie 92 lux à 3 m à 5600 K, et 828 lux à 1 m, valeur calculée et non mesurée.",
        commentLUtiliser = "En remplacement des néons d'un décor, en tubes visibles dans le champ, en lisière, ou en grappe pour une source plus grosse.",
        atouts = "La forme du tube fluo, la couleur, le pilotage.",
        limites = "Très peu de puissance à distance.",
        aRetenir = "Un tube à 1 m et le même tube à 3 m : neuf fois moins de lumière.",
        configurations = listOf("cat-quasar-rainbow-2-4ft")
    ),
    FicheProjecteur(
        nom = "PavoTube II 30X",
        fabricant = "Nanlite",
        annee = 2021,
        anneeApprox = true,
        categorie = "Tube LED pixel couleur sur batterie",
        silhouette = Silhouette.PROJ_TUBE,
        punchline = "Le tube couleur sur batterie le plus répandu des petits plateaux.",
        chiffresCles = listOf(
            "À 1 m, 5600 K" vaut "746 lux",
            "À 1 m, 3200 K" vaut "688 lux",
            "Longueur" vaut "4 pieds"
        ),
        histoire = "La gamme PavoTube de Nanlite a rendu les tubes couleur sur batterie accessibles aux étudiants et aux petites productions. La série II X ajoute les effets pixel.",
        ficheTechnique = "Tube LED RGBWW de 4 pieds, batterie intégrée, effets pixel. Nanlite publie à 1 m : 746 lux à 5600 K, 688 à 3200 K.",
        commentLUtiliser = "Caché derrière un meuble, posé dans le champ, en lisière, en effet de couleur. Sans câble, il se pose en quelques secondes.",
        atouts = "Autonome, léger, abordable, et les effets intégrés.",
        limites = "Peu de puissance ; autonomie limitée à pleine intensité.",
        aRetenir = "Deux tubes bien placés valent mieux qu'une tête mal placée.",
        configurations = listOf("cat-nanlite-pavotube-ii-30x")
    ),
    FicheProjecteur(
        nom = "MC Pro",
        fabricant = "Aputure",
        annee = 2023,
        anneeApprox = true,
        categorie = "Mini panneau LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Une lumière de la taille d'un téléphone, qu'on cache n'importe où.",
        chiffresCles = listOf(
            "À 1 m, 5600 K" vaut "396 lux",
            "À 0,5 m" vaut "1 585 lux",
            "Étanchéité" vaut "pour l'extérieur"
        ),
        histoire = "Le MC d'Aputure a fait du mini panneau couleur un accessoire de tous les sacs. Le MC Pro le rend plus lumineux, avec une optique plus serrée et un boîtier étanche.",
        ficheTechnique = "Mini panneau LED couleur sur batterie. Aputure publie, nu, 396 lux à 1 m et 1 585 lux à 0,5 m à 5600 K ; avec la grille 30°, 1 322 lux à 0,5 m.",
        commentLUtiliser = "Dans une voiture, sous un écran, derrière un objet, en reflet dans les yeux, en effet de téléphone. Il se colle ou s'aimante partout.",
        atouts = "Minuscule, autonome, étanche, couleur.",
        limites = "Une source d'appoint : quelques centaines de lux à 1 m.",
        aRetenir = "Les petites sources servent aux accents : un reflet, une lueur, un détail.",
        configurations = listOf("cat-aputure-mc-pro")
    ),
    FicheProjecteur(
        nom = "NYX Bulb",
        fabricant = "Astera",
        annee = 2020,
        anneeApprox = true,
        categorie = "Ampoule LED couleur pilotable",
        silhouette = Silhouette.PROJ_PRATIQUE,
        punchline = "Une ampoule de décor qu'on pilote depuis la console.",
        chiffresCles = listOf(
            "Consommation" vaut "10 W",
            "À 1 m, 5500 K" vaut "124 lux (à relire)",
            "Culot" vaut "E27 / E26"
        ),
        histoire = "Astera a mis sa technologie de tubes dans une ampoule à culot standard : on la visse dans une lampe de chevet ou un lustre du décor, et on la règle sans fil.",
        ficheTechnique = "Ampoule LED couleur de 10 W à culot E27 / E26, pilotage sans fil. Les distributeurs donnent 114 lux à 1 m à 3200 K et 124 à 5500 K ; un banc d'essai mesure 171 lux à 5600 K, avec une température décalée de plus de 300 K : à relire.",
        commentLUtiliser = "Dans les lampes du décor (les « pratiques »), pour régler leur intensité et leur couleur au plan près, sans gradateur ni électricien sous la table.",
        atouts = "Le contrôle total des lampes visibles dans le champ.",
        limites = "Très peu de lumière : elle sert le décor, pas l'éclairage des visages.",
        aRetenir = "Une lampe de décor bien réglée raconte l'heure et l'ambiance d'une scène.",
        configurations = listOf("cat-astera-nyx")
    ),
    FicheProjecteur(
        nom = "AX9 PowerPAR",
        fabricant = "Astera",
        annee = 2017,
        anneeApprox = true,
        categorie = "PAR LED couleur sur batterie",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Un PAR sans fil qui va là où les câbles ne passent pas.",
        chiffresCles = listOf(
            "À 1 m, 5500 K" vaut "44 355 lux",
            "À 1 m, 3200 K" vaut "47 078 lux",
            "Alimentation" vaut "batterie intégrée"
        ),
        histoire = "Astera a appliqué sa logique de sources sans fil au projecteur PAR : l'AX9 est un faisceau couleur concentré, sur batterie, piloté à distance.",
        ficheTechnique = "PAR LED couleur sur batterie, lentilles et filtres accessoires. Astera publie 44 355 lux à 1 m à 5500 K et 47 078 à 3200 K.",
        commentLUtiliser = "En accent sur un décor, en contre caché, en éclairage d'architecture extérieure, en effet coloré dans un lieu sans électricité.",
        atouts = "Autonome, robuste, faisceau concentré, couleur.",
        limites = "Autonomie limitée à pleine puissance ; le faisceau serré demande d'être bien placé.",
        aRetenir = "Sans câble, on éclaire des lieux impossibles : un toit, un arbre, une cage d'escalier.",
        configurations = listOf("cat-astera-ax9")
    )
)

internal fun ambiances(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "Spacelite 6K",
        fabricant = "Mole-Richardson",
        annee = 1990,
        anneeApprox = true,
        categorie = "Spacelight tungstène",
        silhouette = Silhouette.PROJ_BALLON,
        punchline = "La lanterne de six lampes qui fait le ciel au-dessus d'un plateau.",
        chiffresCles = listOf(
            "Puissance" vaut "6 × 1 000 W",
            "Jupe de soie" vaut "≈ 16 fc à 15 pieds (à relire)",
            "Circuits" vaut "six, sur Socapex"
        ),
        histoire = "Le spacelight est une lanterne cylindrique suspendue au-dessus d'un décor, avec une jupe de toile. Accrochés en rangées sur le gril, les spacelights font une lumière d'ensemble venue du ciel.",
        ficheTechnique = "Type 7291 de Mole-Richardson : six lampes de 1 000 W sur six circuits (Socapex). Un distributeur annonce, avec jupe et cible de soie blanche, 16 footcandles sur 15 pieds de diamètre à 15 pieds de distance : à relire. Jupe noire en option pour diriger la lumière vers le bas.",
        commentLUtiliser = "En rangées au-dessus d'un grand décor pour une ambiance générale, avec la jupe blanche pour éclairer partout, la noire pour tenir la lumière vers le sol. Chaque circuit se gradue à part.",
        atouts = "Une lumière d'ensemble douce et homogène, qui laisse les projecteurs de face libres pour modeler.",
        limites = "Beaucoup de chaleur et de puissance électrique ; la gradation réchauffe la couleur.",
        aRetenir = "Le spacelight éclaire le lieu, pas les comédiens : c'est le fond sur lequel on construit.",
        configurations = listOf("cat-mole-spacelite-6k")
    ),
    FicheProjecteur(
        nom = "SpaceX",
        fabricant = "Creamsource",
        annee = 2019,
        anneeApprox = true,
        categorie = "Spacelight LED couleur",
        silhouette = Silhouette.PROJ_BALLON,
        punchline = "Le spacelight passé à la LED : la même lumière d'en haut, réglable en couleur.",
        chiffresCles = listOf(
            "Puissance" vaut "1 200 W, six moteurs LED",
            "Couleur" vaut "2200 à 15 000 K",
            "À 5 m, 3200 K" vaut "555 lux (à relire)"
        ),
        histoire = "Creamsource a présenté le SpaceX comme l'équivalent LED d'un spacelight tungstène de 5 kW, avec la couleur et le pilotage en plus.",
        ficheTechnique = "Six moteurs LED pour 1 200 W, de 2200 à 15 000 K avec correction vert / magenta, faisceau de 115°. Un distributeur donne 555 lux à 5 m à 3200 K, un autre 591 à 5600 K, une troisième page 18 908 lux à 1 m : à relire. Une optique accessoire double presque l'intensité.",
        commentLUtiliser = "Comme un spacelight : en rangées au-dessus d'un décor, pour une ambiance d'ensemble pilotable en couleur.",
        atouts = "Moins de chaleur et de puissance qu'un 6K tungstène, et la couleur réglable.",
        limites = "Des chiffres publiés incohérents d'une source à l'autre.",
        aRetenir = "Pour remplacer un spacelight, on cherche la même douceur, pas seulement les mêmes lux.",
        configurations = listOf("cat-creamsource-spacex")
    ),
    FicheProjecteur(
        nom = "Ballons éclairants",
        fabricant = "Airstar",
        annee = 2000,
        anneeApprox = true,
        categorie = "Ballon éclairant",
        silhouette = Silhouette.PROJ_BALLON,
        punchline = "Une lune artificielle au bout d'une perche : la lumière qui n'a pas d'ombre dure.",
        chiffresCles = listOf(
            "Sirocco 4000 W HMI" vaut "172 lux à 15 m sous le ballon",
            "Flux" vaut "380 000 lm",
            "Surface à 10 lux" vaut "3 200 m²"
        ),
        histoire = "Airstar, fabricant français, a fait du ballon éclairant un outil courant des tournages de nuit : une enveloppe diffusante gonflée autour d'une lampe, hissée sur un mât ou une grue.",
        ficheTechnique = "Pour le Sirocco 4000 W HMI, Airstar publie 172 lux sous le ballon placé à 15 m, un flux de 380 000 lumens, 3 200 m² éclairés à 10 lux au moins, une température de 6000 K et un ballast magnétique. D'autres modèles existent, du tungstène à la LED.",
        commentLUtiliser = "En lumière d'ensemble de nuit sur une rue ou un champ, en « lune » au-dessus d'une scène, ou en grande source douce verticale. Hauteur et diffusion se choisissent avec le loueur.",
        atouts = "Une source immense, très douce, sans projecteur visible ni ombre dure, rapide à installer sur un mât.",
        limites = "Le vent ; la hauteur à gérer ; une lumière qui éclaire tout et qu'il faut parfois couper au drapeau noir.",
        aRetenir = "Le ballon éclaire un lieu entier : on modèle ensuite les visages avec d'autres sources.",
        configurations = listOf("cat-airstar-sirocco-4000")
    )
)

internal fun hmiSuite(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "M40",
        fabricant = "ARRI",
        annee = 2005,
        anneeApprox = true,
        categorie = "HMI à réflecteur focalisable",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Le 4 kW de la série M : l'efficacité d'un réflecteur, la netteté d'une focale.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 4 000 W",
            "Serré (18°)" vaut "19 700 lux à 10 m",
            "Ouvert (52°)" vaut "2 450 lux à 10 m"
        ),
        histoire = "La série M d'ARRI réunit l'efficacité d'un projecteur ouvert à réflecteur et un faisceau focalisable. Le M40 en est la tête de 4 kW, entre le M18 et le M90.",
        ficheTechnique = "Lampe HMI de 4 000 W (2 500 W possible), ballast séparé. ARRI publie à 10 m : 19 700 lux en position serrée (18°), 6 500 en moyenne (30°), 2 450 en ouverte (52°).",
        commentLUtiliser = "En soleil à travers une fenêtre de loin, en face à travers un grand cadre, en rebond sur une grande surface blanche, en contre de nuit.",
        atouts = "Beaucoup de lumière pour sa puissance, un faisceau propre et focalisable.",
        limites = "Ballast, amorçage, refroidissement avant rallumage, et une distribution électrique dédiée.",
        aRetenir = "À 10 m, passer de l'ouvert au serré multiplie l'éclairement par huit.",
        configurations = listOf("cat-arri-m40")
    ),
    FicheProjecteur(
        nom = "M90",
        fabricant = "ARRI",
        annee = 2010,
        anneeApprox = true,
        categorie = "HMI à réflecteur focalisable",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Le 9 kW de la série M, pour les décors entiers et les soleils de loin.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 9 000 W",
            "Serré (15°)" vaut "51 000 lux à 10 m",
            "Ouvert (49°)" vaut "7 590 lux à 10 m"
        ),
        histoire = "Le M90 porte la logique de la série M au niveau des gros HMI de 6 et 9 kW.",
        ficheTechnique = "Lampe HMI de 9 000 W (6 kW possible), ballast séparé. ARRI publie à 10 m : 51 000 lux serré (15°), 14 450 en moyenne (30°), 7 590 ouvert (49°).",
        commentLUtiliser = "En soleil sur un décor extérieur ou à travers les fenêtres d'un studio, en contre de nuit sur une rue, à travers des toiles de 6 × 6 m.",
        atouts = "Une puissance considérable pour son poids, et un faisceau focalisable.",
        limites = "Une logistique lourde : groupe électrogène, câbles, nacelle ou grand pied.",
        aRetenir = "Serré, le M90 donne à 10 m près de sept fois plus qu'ouvert : 51 000 lux contre 7 590.",
        configurations = listOf("cat-arri-m90")
    ),
    FicheProjecteur(
        nom = "ARRIMAX 18/12",
        fabricant = "ARRI",
        annee = 2004,
        anneeApprox = true,
        categorie = "HMI 18 kW à réflecteur",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Le soleil de location : 18 kW dans un réflecteur.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 18 000 ou 12 000 W",
            "Ouvert (50°)" vaut "5 692 lux à 15 m (à relire)",
            "Faisceau" vaut "15 à 50° (réflecteur standard)"
        ),
        histoire = "L'ARRIMAX a fait entrer le réflecteur focalisable dans la catégorie des 18 kW, jusque-là réservée aux grandes Fresnels. C'est l'un des plus gros projecteurs couramment loués.",
        ficheTechnique = "Lampe HMI de 18 kW ou 12 kW, faisceau de 15° à 50° avec le réflecteur standard (8 à 15° avec le réflecteur spot). B&H donne 5 692 lux à 15 m en ouvert à 18 kW ; l'ancienne notice ARRI conduit à environ 5 980 : à relire.",
        commentLUtiliser = "En soleil sur un grand décor, en contre de nuit sur une place, à travers des toiles géantes. Il se monte sur grue ou nacelle.",
        atouts = "Une puissance énorme, focalisable, dans une tête plus légère qu'une Fresnel 18 kW.",
        limites = "Groupe électrogène dédié, équipe électrique nombreuse, poids, coût.",
        aRetenir = "Quand un plan de nuit montre une rue entière éclairée, il y a souvent un 18 kW derrière.",
        configurations = listOf("cat-arri-arrimax-18-12")
    ),
    FicheProjecteur(
        nom = "ARRISUN 18",
        fabricant = "ARRI",
        annee = 2008,
        anneeApprox = true,
        categorie = "PAR HMI 1 800 W",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Un PAR HMI à lentilles qui porte très loin pour sa puissance.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 1 800 W",
            "Lentille spot (8°)" vaut "2 000 lux à 40,4 m",
            "Lentilles" vaut "spot, narrow flood, flood, super flood"
        ),
        histoire = "L'ARRISUN 18 applique le principe du PAR aux lampes HMI de 1 800 W : un réflecteur parabolique et des lentilles interchangeables qui règlent l'angle.",
        ficheTechnique = "Lampe HMI de 1 800 W, lentilles spot (8°), narrow flood, flood et super flood. La fiche ARRI AS 18 donne 2 000 lux à 40,4 m avec la lentille spot (500 lux à 80,7 m).",
        commentLUtiliser = "Pour porter de très loin : un rai de soleil dans une église, une façade de nuit, un contre lointain. Les lentilles larges en font une source d'ambiance.",
        atouts = "Une portée énorme pour 1 800 W, sur un circuit domestique dans certaines versions.",
        limites = "Changer de lentille demande d'éteindre ou de se protéger ; faisceau moins régulier qu'une Fresnel.",
        aRetenir = "Un PAR ne se focalise pas : on change de lentille.",
        configurations = listOf("cat-arri-arrisun-18-spot")
    ),
    FicheProjecteur(
        nom = "True Blue D",
        fabricant = "ARRI",
        annee = 2008,
        anneeApprox = true,
        categorie = "Fresnels HMI",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "Les Fresnels HMI de référence, du 575 W au 4 kW.",
        chiffresCles = listOf(
            "Gamme" vaut "D5, D12, D25, D40",
            "Puissances" vaut "575, 1 200, 2 500, 4 000 W",
            "Lentille du D40" vaut "30 cm"
        ),
        histoire = "La série True Blue D est la génération actuelle des Fresnels HMI d'ARRI : la lentille à échelons, et une lampe aux halogénures métalliques.",
        ficheTechnique = "Quatre tailles : D5 (575 W), D12 (1 200 W), D25 (2 500 W) et D40 (4 000 W, lentille de 30 cm). Les tableaux photométriques d'ARRI n'ont pas pu être lus de façon fiable dans les documents consultés : à reprendre dans l'application ARRI Photometrics.",
        commentLUtiliser = "Le D5 et le D12 en face ou en contre sur un petit décor, le D25 et le D40 en soleil de fenêtre et en face lointaine. Ballast électronique pour les cadences non standard.",
        atouts = "Un faisceau Fresnel net, beaucoup de lumière du jour, une construction robuste.",
        limites = "Ballast, amorçage, refroidissement ; plus lourd qu'une tête LED de lumière comparable.",
        aRetenir = "« Un 4K » sur un plateau désigne souvent une Fresnel HMI de 4 000 W comme le D40.",
        configurations = emptyList()
    ),
    FicheProjecteur(
        nom = "Joker-Bug 800",
        fabricant = "K5600",
        annee = 2000,
        anneeApprox = true,
        categorie = "HMI compact polyvalent",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Le petit HMI à tout faire : PAR, Fresnel, boîte à lumière ou lanterne.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 800 W",
            "Sans lentille (5°)" vaut "260 000 lux à 3 m",
            "Fresnel dépolie" vaut "3 100 lux à 3 m"
        ),
        histoire = "Le fabricant franco-américain K5600 a imaginé le Joker comme un HMI universel : une tête courte qui se glisse dans un PAR, une boîte à lumière, une lanterne chinoise ou une découpe.",
        ficheTechnique = "Lampe HMI de 800 W, ballast léger, faisceau de 5° à 45° (jusqu'à 60° selon la brochure française). Panavision et B&H donnent à 3 m et 5600 K : 260 000 lux sans lentille, 3 100 lux avec la Fresnel dépolie ; avec les lentilles moyenne, large et très large, 37 600, 20 500 et 5 560 lux (B&H).",
        commentLUtiliser = "En PAR serré pour un rai de soleil, dans une boîte à lumière pour une face douce, dans une lanterne pour une ambiance.",
        atouts = "Une polyvalence rare, une puissance de lumière du jour pour 800 W, un poids réduit.",
        limites = "Changer d'accessoire demande du temps ; la lampe HMI reste à ménager.",
        aRetenir = "Selon la lentille, le même Joker donne 3 100 ou 260 000 lux à 3 m.",
        configurations = listOf("cat-k5600-joker-bug-800")
    )
)

internal fun tungsteneSuite(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "True Blue T5",
        fabricant = "ARRI",
        annee = 2008,
        anneeApprox = true,
        categorie = "Fresnel tungstène 5 kW",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "Le « cinq kilos » : la grosse Fresnel tungstène des studios.",
        chiffresCles = listOf(
            "Lampe" vaut "5 000 W, 3200 K",
            "Serré (9°)" vaut "11 213 lux à 9 m",
            "Ouvert (62°)" vaut "1 110 lux à 9 m"
        ),
        histoire = "La Fresnel 5 kW (« le 5K », « la Senior » chez Mole) est un pilier des studios : assez puissante pour éclairer un décor de loin, avec la lumière à spectre continu du filament.",
        ficheTechnique = "Lampe halogène de 5 000 W à 3200 K, lentille Fresnel. ARRI publie à 9 m : 11 213 lux en serré (9°), 1 110 en ouvert (62°).",
        commentLUtiliser = "En soleil couchant à travers une fenêtre de studio, en face lointaine à travers une grande toile, en contre de nuit.",
        atouts = "Un spectre continu, un faisceau superbe et découpable, une fiabilité totale.",
        limites = "5 kW de consommation et autant de chaleur ; la gradation la rend plus orange.",
        aRetenir = "Du serré à l'ouvert, l'éclairement à 9 m est divisé par dix.",
        configurations = listOf("cat-arri-t5")
    ),
    FicheProjecteur(
        nom = "True Blue T12",
        fabricant = "ARRI",
        annee = 2008,
        anneeApprox = true,
        categorie = "Fresnel tungstène 12 kW",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "La très grosse Fresnel tungstène : un soleil chaud pour les grands studios.",
        chiffresCles = listOf(
            "Lampe" vaut "10 ou 12 kW, 3200 K",
            "Serré" vaut "209 566 lux à 3 m",
            "Ouvert" vaut "35 891 lux à 3 m"
        ),
        histoire = "Les Fresnels tungstène de 10 et 12 kW sont les plus grosses sources à filament couramment utilisées, pour les grands décors de studio éclairés en lumière chaude.",
        ficheTechnique = "Lampe halogène de 10 ou 12 kW à 3200 K, réflecteur sphérique, grande lentille Fresnel. Le loueur Panavision publie à 3 m : 209 566 lux serré, 35 891 ouvert.",
        commentLUtiliser = "En soleil de studio, à travers des toiles de plusieurs mètres, en contre sur un grand décor.",
        atouts = "Une énorme quantité de lumière chaude à spectre continu, au faisceau maîtrisé.",
        limites = "Consommation, chaleur, poids : un projecteur de studio, pas de décor réel.",
        aRetenir = "12 kW de tungstène, c'est surtout 12 kW de chaleur sur le plateau.",
        configurations = listOf("cat-arri-t12")
    ),
    FicheProjecteur(
        nom = "Solarspot",
        fabricant = "Mole-Richardson",
        annee = 1950,
        anneeApprox = true,
        categorie = "Fresnels tungstène",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "La Baby, la Junior, la Senior, la Tener : les noms des Fresnels d'Hollywood.",
        chiffresCles = listOf(
            "Baby" vaut "1 000 W",
            "Junior" vaut "2 000 W",
            "Senior" vaut "5 000 W",
            "Tener" vaut "10 000 W"
        ),
        histoire = "Mole-Richardson, fondé à Hollywood en 1927, a donné leurs noms aux Fresnels du cinéma américain : Baby (1 kW), Junior (2 kW), Senior (5 kW), Tener (10 kW). Les électriciens les appellent encore ainsi, quelle que soit la marque.",
        ficheTechnique = "Fresnels tungstène de 1 à 10 kW, construction en acier, lentille à échelons. Les fiches Mole donnent les éclairements en footcandles par distance ; leurs tableaux n'ont pas pu être lus de façon fiable dans les documents consultés : à reprendre sur le site du fabricant.",
        commentLUtiliser = "Comme toutes les Fresnels : en face, en contre, à travers une diffusion, avec volets, filets et drapeaux. La Baby et la Junior pour les petits décors, la Senior et la Tener pour les grands.",
        atouts = "Robustes, réparables, au faisceau classique que tous les électriciens connaissent.",
        limites = "Lourdes, chaudes, gourmandes en électricité.",
        aRetenir = "« Passe-moi une Junior » : 2 kW de Fresnel, dans le vocabulaire de tous les plateaux.",
        configurations = emptyList()
    ),
    FicheProjecteur(
        nom = "Arrilite 2000 Plus",
        fabricant = "ARRI",
        annee = 2012,
        anneeApprox = true,
        categorie = "Projecteur ouvert tungstène 2 kW (« blonde »)",
        silhouette = Silhouette.PROJ_OUVERT,
        punchline = "La « blonde » : 2 kW de lumière ouverte pour les rebonds et les toiles.",
        chiffresCles = listOf(
            "Lampe" vaut "FEX 2 000 W, 3200 K",
            "Serré (24°)" vaut "4 400 lux à 5 m",
            "Ouvert (63°)" vaut "940 lux à 5 m"
        ),
        histoire = "Les projecteurs ouverts à réflecteur, sans lentille, donnent plus de lumière qu'une Fresnel de même puissance mais un faisceau moins net. La « blonde » de 2 kW est un outil de base des kits tungstène ; l'Arrilite 2000 Plus en est la version actuelle chez ARRI.",
        ficheTechnique = "Lampe FEX de 2 000 W à 3200 K, réflecteur focalisable sans lentille. ARRI publie à 5 m : 4 400 lux en serré (24°), 940 en ouvert (63°).",
        commentLUtiliser = "En rebond sur un plafond ou un panneau blanc, à travers une grande toile de diffusion, en éclairage d'ensemble d'une pièce. Rarement en direct sur un visage.",
        atouts = "Beaucoup de lumière pour 2 kW, léger, simple.",
        limites = "Un faisceau aux bords flous et un peu irrégulier, des ombres multiples en direct ; chaleur.",
        aRetenir = "Un projecteur ouvert est fait pour être diffusé ou réfléchi.",
        configurations = listOf("cat-arri-arrilite-2000-plus")
    ),
    FicheProjecteur(
        nom = "Redhead 800",
        fabricant = "Ianiro",
        annee = 1980,
        anneeApprox = true,
        categorie = "Projecteur ouvert tungstène 800 W (« mandarine »)",
        silhouette = Silhouette.PROJ_OUVERT,
        punchline = "La « mandarine » : le petit projecteur ouvert de toutes les écoles de cinéma.",
        chiffresCles = listOf(
            "Lampe" vaut "800 W, 3200 K",
            "Poids" vaut "0,91 kg",
            "Lampe DXX" vaut "≈ 21 400 lm, ≈ 75 h"
        ),
        histoire = "La Redhead italienne d'Ianiro, que la marque présente comme le projecteur tungstène le plus vendu depuis quarante ans, est le petit projecteur ouvert des kits. En France, on l'appelle « mandarine », comme sa lampe de 800 W.",
        ficheTechnique = "Projecteur ouvert à réflecteur focalisable, lampe de 800 W à 3200 K, 0,91 kg. Une lampe DXX 800 W donne environ 21 400 lumens pour une durée moyenne d'environ 75 heures (Osram). Ianiro ne publie pas d'éclairement dans les documents consultés ; une version LTM du même type donne 6 700 lux à 2 m en serré (47°), à titre indicatif.",
        commentLUtiliser = "En rebond sur un mur ou un plafond, à travers une diffusion, en lumière de fond sur un décor. Les volets coupent le faisceau ; attention à la chaleur sur les gélatines.",
        atouts = "Léger, pas cher, puissant pour sa taille, increvable.",
        limites = "Lumière dure aux bords flous, très chaude ; la lampe s'use vite et ne se touche pas à mains nues.",
        aRetenir = "Une mandarine nue est dure et irrégulière : elle donne son meilleur réfléchie ou diffusée.",
        configurations = emptyList()
    ),
    FicheProjecteur(
        nom = "Maxi-Brute",
        fabricant = "Mole-Richardson",
        annee = 1970,
        anneeApprox = true,
        categorie = "Batterie de PAR tungstène",
        silhouette = Silhouette.PROJ_OUVERT,
        punchline = "Neuf PAR de 1 000 W dans un cadre : la force brute pour les nuits.",
        chiffresCles = listOf(
            "Puissance" vaut "9 × 1 000 W (PAR 64)",
            "Intensité" vaut "75 A au plus",
            "PAR très serré" vaut "405 fc à 100 pieds (à relire)"
        ),
        histoire = "Les batteries de lampes PAR (Maxi-Brute, Dino, Wendy) sont nées pour les très grands éclairages de nuit : on additionne des lampes à faisceau fixe pour atteindre la portée des plus gros projecteurs.",
        ficheTechnique = "Neuf lampes PAR 64 de 1 000 W (type 5771 chez Mole), 75 A au plus, lampes de différents faisceaux interchangeables. Un distributeur annonce 405 footcandles sur 23 × 15 pieds à 100 pieds avec des lampes très serrées : à relire. La photométrie dépend entièrement des lampes installées.",
        commentLUtiliser = "En contre de nuit sur une rue ou un stade, en rattrapage du jour, à travers des toiles géantes. On allume les lampes par rangées pour doser.",
        atouts = "Une puissance énorme et chaude, simple, modulable lampe par lampe.",
        limites = "Consommation et chaleur énormes, faisceau non focalisable : on change les lampes.",
        aRetenir = "Avec une batterie de PAR, la photométrie se choisit au moment de choisir les lampes.",
        configurations = emptyList()
    )
)
