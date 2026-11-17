package fr.cellule.core

/*
 * Les grandes fiches des projecteurs, au format des caméras et des
 * objectifs. Les chiffres viennent des configurations photométriques du
 * catalogue (sources des fabricants) ; le reste est la pratique du plateau,
 * dite comme telle. Aucune année n'est donnée au jour près quand elle n'a
 * pas été recoupée : « vers ».
 */

val ETAGERES_PROJECTEURS: List<Etagere<FicheProjecteur>> by lazy {
    val panneauxEtTubes = panneauxEtTubes()
    val hmiEtTungstene = hmiEtTungstene()
    listOf(
        Etagere("LED ponctuelles", ledPonctuelles() + ledPonctuellesSuite()),
        Etagere("Fresnels LED, découpes et petites optiques", fresnelsEtDecoupes()),
        Etagere("Panneaux LED et fluorescents", panneauxEtTubes.filter { it.silhouette != Silhouette.PROJ_TUBE } + panneauxSuite()),
        Etagere("Tubes, barres et petites sources", panneauxEtTubes.filter { it.silhouette == Silhouette.PROJ_TUBE } + tubesSuite()),
        Etagere("Ambiances : spacelights et ballons", ambiances()),
        Etagere("HMI", hmiEtTungstene.filter { it.silhouette == Silhouette.PROJ_HMI } + hmiSuite()),
        Etagere("Tungstène", hmiEtTungstene.filter { it.silhouette != Silhouette.PROJ_HMI } + tungsteneSuite())
    )
}

val CATALOGUE_FICHES_PROJECTEURS: List<FicheProjecteur> by lazy { ETAGERES_PROJECTEURS.flatMap { it.entrees } }

/** La grande fiche qui décrit une configuration photométrique, s'il y en a une. */
fun ficheDuProjecteur(identifiant: String): FicheProjecteur? =
    CATALOGUE_FICHES_PROJECTEURS.firstOrNull { identifiant in it.configurations }

private fun ledPonctuelles(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "LS 600d Pro",
        fabricant = "Aputure",
        annee = 2021,
        categorie = "LED ponctuelle lumière du jour",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La « tête » LED qui a remplacé le petit HMI sur les plateaux légers.",
        chiffresCles = listOf(
            "Couleur" vaut "5600 K",
            "Consommation" vaut "720 W au plus",
            "Hyper-Reflector" vaut "8 500 lux à 3 m (55°)",
            "Fresnel F10" vaut "29 300 lux à 3 m (15°)"
        ),
        histoire = "Aputure, marque née de l'éclairage pour vidéastes, a bâti sa gamme Light Storm sur une idée simple : une source LED unique et très puissante, montée comme un flash de studio, qu'on façonne ensuite avec des accessoires. Le 600d Pro a amené cette logique au niveau des petits HMI, à une fraction de leur prix, et s'est imposé dans les productions légères, la publicité et le clip.",
        ficheTechnique = "Une source LED ponctuelle lumière du jour (5600 K), une tête séparée de son boîtier d'alimentation, et la monture Bowens qui accepte réflecteurs, boîtes à lumière, ballons et lentilles. Avec son Hyper-Reflector de 55°, Aputure publie 8 500 lux à 3 m ; avec la lentille Fresnel F10, 29 300 lux à 3 m en position serrée (15°) et 11 400 lux en position ouverte (45°). Gradation continue, pilotage DMX et sans fil.",
        commentLUtiliser = "On la traite comme un petit HMI : en face à travers un grand cadre de diffusion, en soleil artificiel avec la Fresnel, ou en lumière de rattrapage par une fenêtre. Nue ou avec le réflecteur, la source est très dure : c'est la diffusion qui fait la qualité de la lumière. Elle tire assez peu de courant pour une prise domestique, et fonctionne sur batteries V-Mount.",
        atouts = "Une puissance de petit HMI sans ballast ni temps de chauffe, sans scintillement à presque toutes les cadences, et une consommation qui passe sur le secteur domestique. Tout l'écosystème Bowens devient utilisable.",
        limites = "Une seule température de couleur : pour du tungstène il faut une gélatine, qui coûte de la lumière. La source très petite donne des ombres dures et des reflets ponctuels dans les yeux si on ne la diffuse pas. La ventilation s'entend dans les plans calmes, sauf en mode silencieux, qui réduit la puissance.",
        aRetenir = "Le 600d Pro n'est pas une « lumière » : c'est un moteur. Tout ce qui la rend belle — diffusion, réflecteur, lentille — se pose devant.",
        configurations = listOf("cat-aputure-600d-pro-hyper", "cat-aputure-600d-pro-f10")
    ),
    FicheProjecteur(
        nom = "LS 1200d Pro",
        fabricant = "Aputure",
        annee = 2021,
        categorie = "LED ponctuelle lumière du jour",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Le plus gros Light Storm lumière du jour : de quoi se mesurer aux HMI moyens.",
        chiffresCles = listOf(
            "Couleur" vaut "5600 K",
            "Réflecteur large" vaut "13 010 lux à 3 m (55°)",
            "Réflecteur moyen" vaut "22 400 lux à 3 m (30°)",
            "Réflecteur étroit" vaut "83 100 lux à 3 m (15°)"
        ),
        histoire = "Arrivé en 2021, le 1200d Pro pousse la logique du 600d : une source ponctuelle plus grosse encore, avec une famille de réflecteurs dédiés pour atteindre la portée d'un HMI de taille moyenne sans en avoir les contraintes.",
        ficheTechnique = "Source LED lumière du jour, tête et alimentation séparées, réflecteurs Hyper dédiés étroit (15°), moyen (30°) et large (55°). Aputure publie à 3 m : 13 010 lux avec le réflecteur large, 22 400 avec le moyen et 83 100 avec l'étroit.",
        commentLUtiliser = "Pour un soleil à travers une fenêtre de loin, pour éclairer un grand cadre de diffusion de 4 × 4 m ou plus, ou pour déboucher un extérieur jour. Avec le réflecteur étroit, il porte loin ; avec le large et une grande diffusion, il devient une source douce puissante.",
        atouts = "La puissance d'un HMI moyen, sans ballast, sans restrike à chaud, avec une gradation continue et sans changer de couleur quand on baisse.",
        limites = "Lourde et chère pour une production légère ; elle demande un circuit costaud. Lumière du jour uniquement.",
        aRetenir = "Changer de réflecteur change la portée du simple au sextuple : 13 010 lux à 3 m en large, 83 100 en étroit.",
        configurations = listOf("cat-aputure-1200d-pro-hyper")
    ),
    FicheProjecteur(
        nom = "LS 600c Pro",
        fabricant = "Aputure",
        annee = 2022,
        categorie = "LED ponctuelle couleur",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Le 600 en couleur : la même tête, mais toutes les températures et toutes les teintes.",
        chiffresCles = listOf(
            "Couleur" vaut "2300 à 10 000 K, RGBWW",
            "Hyper-Reflector" vaut "5 020 lux à 3 m (5600 K)",
            "Sans accessoire" vaut "1 937 lux à 3 m"
        ),
        histoire = "Arrivé en 2022, il reprend le format des 600 lumière du jour avec un moteur à cinq couleurs (rouge, vert, bleu, deux blancs), pour qu'une même tête serve en tungstène, en jour et en effet.",
        ficheTechnique = "Source LED RGBWW de 2300 à 10 000 K, avec correction vert / magenta et couleurs saturées. À 5600 K, Aputure publie 5 020 lux à 3 m avec l'Hyper-Reflector, 1 937 lux sans accessoire. Monture Bowens, pilotage DMX et sans fil.",
        commentLUtiliser = "Quand une seule tête doit faire le soleil couchant, le tungstène d'un intérieur nuit et une couleur d'effet dans la même journée. On la cale sur la balance de la caméra plutôt que de gélatiner.",
        atouts = "Plus besoin de CTO ni de CTB : la couleur se règle, sans perte de gélatine, et se raccorde aux autres sources au mired près.",
        limites = "À 5600 K, elle donne moins que le 600d Pro lumière du jour : une partie des LED sert aux autres couleurs. Plus chère.",
        aRetenir = "Une tête couleur échange de la puissance contre de la souplesse : 5 020 lux à 3 m ici, contre 8 500 pour le 600d Pro.",
        configurations = listOf("cat-aputure-600c-pro-hyper")
    ),
    FicheProjecteur(
        nom = "LS 300d II",
        fabricant = "Aputure",
        annee = 2019,
        anneeApprox = true,
        categorie = "LED ponctuelle lumière du jour",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Le projecteur qui a mis la LED ponctuelle dans les sacs des petites équipes.",
        chiffresCles = listOf(
            "Couleur" vaut "5600 K",
            "Hyper Reflector" vaut "45 000 lux à 1 m"
        ),
        histoire = "La seconde version du 300d a popularisé la formule « tête compacte, alimentation séparée, monture Bowens » auprès des vidéastes et des courts métrages, avant les 600 et 1200.",
        ficheTechnique = "Source LED lumière du jour, monture Bowens, gradation continue. Aputure annonce plus de 45 000 lux à 1 m avec l'Hyper Reflector.",
        commentLUtiliser = "En face à travers une boîte à lumière, en contre, en effet de fenêtre en intérieur. Assez léger pour une perche ou un petit pied.",
        atouts = "Petit, léger, alimentable sur batterie, et assez puissant pour un intérieur.",
        limites = "Trop juste pour lutter contre le jour en extérieur. Lumière du jour seulement.",
        aRetenir = "Le 300d a appris à une génération à travailler avec une source ponctuelle et de la diffusion, comme avec un HMI.",
        configurations = listOf("cat-aputure-300d-ii-hyper")
    ),
    FicheProjecteur(
        nom = "série amaran",
        fabricant = "Aputure",
        annee = 2021,
        anneeApprox = true,
        categorie = "LED ponctuelles d'entrée de gamme",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La gamme abordable d'Aputure : les outils du plateau pour un budget d'étudiant.",
        chiffresCles = listOf(
            "100d" vaut "39 500 lux à 1 m",
            "200d" vaut "65 000 lux à 1 m",
            "300c" vaut "26 580 lux à 1 m (5600 K)",
            "Monture" vaut "Bowens"
        ),
        histoire = "amaran est la marque d'entrée de gamme d'Aputure. Ses têtes ponctuelles 100d et 200d, puis leurs versions « S », et les modèles couleur comme le 150c et le 300c, ont apporté la monture Bowens et la LED ponctuelle aux petits budgets.",
        ficheTechnique = "Avec leur Hyper-Reflector : 100d, 39 500 lux à 1 m (nu : 4 300) ; 100d S, 34 600 ; 200d, 65 000 lux à 1 m (55°), 6 100 à 3 m ; 200d S, 55 800 lux à 1 m (26°) ; 150c, 15 610 lux à 1 m à 5600 K (≈ 63°) ; 300c, 26 580 lux à 1 m à 5600 K (nu : 9 370) — le 150c et le 300c sont réglables en couleur.",
        commentLUtiliser = "En face dans une boîte à lumière pour les entretiens, en contre, en débouchage. Le 300c pour les effets de couleur et les raccords de température.",
        atouts = "Le prix, le poids, et la compatibilité avec tous les accessoires Bowens.",
        limites = "Moins de puissance et de finition que les Light Storm ; à pleine puissance, la ventilation s'entend.",
        aRetenir = "Pour apprendre à éclairer, deux amaran et un grand cadre de diffusion font déjà un vrai plateau.",
        configurations = listOf("cat-amaran-100d-hyper", "cat-amaran-100d-s-hyper", "cat-amaran-150c-hyper", "cat-amaran-200d-hyper", "cat-amaran-200d-s-hyper", "cat-amaran-300c-hyper")
    ),
    FicheProjecteur(
        nom = "Forza 60, 300 et 500",
        fabricant = "Nanlite",
        annee = 2020,
        anneeApprox = true,
        categorie = "LED ponctuelles",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "La concurrente directe des Light Storm, avec la même logique de monture Bowens.",
        chiffresCles = listOf(
            "Forza 500" vaut "6 257 lux à 3 m (55°)",
            "Forza 300" vaut "43 060 lux à 1 m (à relire)",
            "Forza 60 II" vaut "17 620 lux à 1 m (à relire)"
        ),
        histoire = "Nanlite, marque du fabricant chinois Nanguang, a construit sa série Forza en face des Aputure : des têtes LED ponctuelles de 60 à 500, puis une seconde génération « II ».",
        ficheTechnique = "Source LED ponctuelle, monture Bowens, réflecteur de 55°. Forza 500 : 6 257 lux à 3 m à 5600 K avec le réflecteur. Pour les Forza 60 et 300, les valeurs varient selon la génération et le distributeur : à relire sur la fiche du modèle exact.",
        commentLUtiliser = "Comme une tête Aputure de même puissance : derrière une diffusion, en face ou en contre, ou avec une lentille Fresnel de la marque.",
        atouts = "Un rapport puissance-prix serré, et une gamme qui va du très petit au gros.",
        limites = "D'une génération à l'autre, les chiffres changent : il faut savoir quel modèle on a entre les mains.",
        aRetenir = "Toujours vérifier « II » ou pas sur l'étiquette : la photométrie n'est pas la même.",
        configurations = listOf("cat-nanlite-forza-500", "cat-nanlite-forza-300-55", "cat-nanlite-forza-60-ii")
    ),
    FicheProjecteur(
        nom = "SL et VL",
        fabricant = "Godox",
        annee = 2020,
        anneeApprox = true,
        categorie = "LED ponctuelles économiques",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Les têtes les moins chères du plateau, venues du monde du flash photo.",
        chiffresCles = listOf(
            "SL150W II" vaut "58 000 lux à 1 m (à relire)",
            "VL300 II" vaut "90 000 lux à 1 m (à relire)"
        ),
        histoire = "Godox, grand fabricant de flashs photo, a décliné ses montures Bowens en lumière continue : la série SL, de studio, et la série VL, plus portable avec un boîtier séparé.",
        ficheTechnique = "Sources LED lumière du jour à monture Bowens et réflecteur standard. Les valeurs publiées par les distributeurs divergent (pour le VL300 II, 90 000 ou 77 000 lux à 1 m) : à relire sur la fiche du modèle.",
        commentLUtiliser = "En photo comme en vidéo, en face dans une boîte à lumière ou en contre. Pratique pour compléter un parc à petit prix.",
        atouts = "Le prix, et la compatibilité avec les accessoires photo qu'on a souvent déjà.",
        limites = "Des chiffres publiés moins fiables, une finition et une ventilation moins soignées.",
        aRetenir = "Une bonne affaire pour apprendre, à condition de mesurer soi-même ce qu'elle donne.",
        configurations = listOf("cat-godox-sl150-ii", "cat-godox-vl300-ii")
    ),
    FicheProjecteur(
        nom = "Orbiter",
        fabricant = "ARRI",
        annee = 2020,
        categorie = "LED ponctuelle à optiques interchangeables",
        silhouette = Silhouette.PROJ_PONCTUEL,
        punchline = "Une seule tête, et toutes les optiques : ouverte, Fresnel, découpe, boîte à lumière.",
        chiffresCles = listOf(
            "Moteur" vaut "ARRI Spectra, six couleurs",
            "Consommation" vaut "500 W au plus",
            "Open Face 30°" vaut "2 320 lux à 5 m (5600 K)"
        ),
        histoire = "Présenté par ARRI en 2020, l'Orbiter part d'une idée simple : plutôt qu'un projecteur par usage, une tête unique dont on change l'optique, pour en faire un projecteur ouvert, une Fresnel, une découpe ou une source douce.",
        ficheTechnique = "Moteur LED à six couleurs ARRI Spectra (RGBACL), réglable en température et en couleur. Optiques interchangeables : Open Face 15°, 30° et 60°, Fresnel, projection, dômes et boîtes à lumière. Avec l'Open Face 30°, 2 320 lux à 5 m à 5600 K.",
        commentLUtiliser = "On choisit l'optique selon le rôle du jour : Open Face pour un rebond ou un grand cadre, Fresnel pour un faisceau propre, dôme pour une source douce rapide. Sa couleur se règle pour raccorder tungstène, jour ou toute autre source.",
        atouts = "Une couleur de très grande qualité sur toute la plage, et la polyvalence d'un parc entier dans un seul corps.",
        limites = "Cher, et chaque optique est un achat à part. Moins puissant qu'un HMI de même encombrement.",
        aRetenir = "L'Orbiter vend moins de la puissance qu'une palette : la couleur juste, quelle que soit l'optique.",
        configurations = listOf("cat-arri-orbiter-of")
    )
)

private fun panneauxEtTubes(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "SkyPanel",
        fabricant = "ARRI",
        annee = 2015,
        anneeApprox = true,
        categorie = "Panneaux LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le panneau LED devenu standard des plateaux, du studio au téléfilm.",
        chiffresCles = listOf(
            "S30-C" vaut "222 lux à 5 m",
            "S60-C" vaut "416 lux à 5 m",
            "S360-C" vaut "≈ 4 800 lux à 3 m (à relire)",
            "Couleur" vaut "température et teintes réglables"
        ),
        histoire = "Lancée par ARRI au milieu des années 2010, la gamme SkyPanel a remplacé dans bien des parcs les tubes fluorescents : une source douce, réglable en couleur, pilotable à distance, déclinée en plusieurs tailles (S30, S60, S120, S360).",
        ficheTechnique = "Panneaux LED couleur (versions « -C ») réglables en température, en vert / magenta et en teinte, avec des effets programmés. Diffuseurs interchangeables : Lite, Standard, Heavy, et intensificateur. Avec la diffusion standard à 5600 K : S30-C, 222 lux à 5 m ; S60-C, 416 lux à 5 m (400 W nominal). Pour le S360-C, les distributeurs donnent de 4 833 à 6 444 lux à 3 m : à relire.",
        commentLUtiliser = "En lumière douce de face, en débouchage, en ambiance plafonnière, ou en source d'effet (télévision, néon, gyrophare). On l'accroche, on le pilote depuis la console ou une appli, sans monter à l'échelle.",
        atouts = "Une couleur fiable, la douceur d'un panneau, et le pilotage DMX qui fait gagner des heures sur un plateau.",
        limites = "Une source large : la loi en 1/d² ne s'applique qu'à distance. Pas de faisceau serré sans accessoire.",
        aRetenir = "Le S60 est l'unité de compte : quand un chef électro dit « un Sky », c'est souvent lui.",
        configurations = listOf("cat-arri-s30c", "cat-arri-s60c", "cat-arri-s360c")
    ),
    FicheProjecteur(
        nom = "Nova",
        fabricant = "Aputure",
        annee = 2021,
        anneeApprox = true,
        categorie = "Panneaux LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "La réponse d'Aputure au panneau de studio, en 2×1 et 1×1.",
        chiffresCles = listOf(
            "Nova P600c" vaut "2 167 lux à 3 m (5600 K)",
            "Nova P300c" vaut "1 000 lux à 3 m (4300 K)",
            "Couleur" vaut "RGBWW"
        ),
        histoire = "Le Nova P600c est un panneau 2×1 couleur à quatre zones pilotables ; le P300c, plus petit, l'a précédé.",
        ficheTechnique = "Panneaux LED RGBWW. P600c : 2 167 lux à 3 m à 5600 K (2 298 à 6500 K), quatre zones de couleur indépendantes. P300c : 1 000 lux à 3 m à 4300 K, faisceau annoncé de 120°.",
        commentLUtiliser = "En lumière douce principale, en débouchage, ou en ambiance colorée avec des zones différentes sur un même panneau.",
        atouts = "Une grande source douce couleur, pilotable, à un prix inférieur aux panneaux de location classiques.",
        limites = "Encombrant à transporter ; comme tout panneau, sa photométrie à courte distance ne suit pas la loi en 1/d².",
        aRetenir = "Quatre zones sur un même panneau : un dégradé de couleur sans deux projecteurs.",
        configurations = listOf("cat-aputure-nova-p600c", "cat-aputure-nova-p300c")
    ),
    FicheProjecteur(
        nom = "Gemini",
        fabricant = "Litepanels",
        annee = 2018,
        anneeApprox = true,
        categorie = "Panneaux LED couleur",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le panneau couleur d'un pionnier de la LED de cinéma.",
        chiffresCles = listOf(
            "Gemini 2x1 Soft" vaut "9 713 lux à 1 m (6000 K)",
            "Faisceau" vaut "110°"
        ),
        histoire = "Litepanels a été l'un des premiers fabricants de panneaux LED pour l'image animée. La gamme Gemini, en 1×1 et 2×1, a apporté la couleur réglable et la douceur d'un grand panneau.",
        ficheTechnique = "Panneau LED RGBWW. Gemini 2x1 Soft : 9 713 lux à 1 m à 6000 K, faisceau de 110°. Une valeur mesurée de près : sur un panneau de cette taille, l'éclairement à plusieurs mètres est plus faible que ce que la loi en 1/d² en déduirait.",
        commentLUtiliser = "En face douce, en débouchage, en lumière d'ambiance colorée.",
        atouts = "Une source large et homogène, réglable en couleur, réputée pour la qualité de ses teintes.",
        limites = "Peu de portée ; encombrant.",
        aRetenir = "Pour un panneau, une valeur publiée à 1 m ne se transpose pas telle quelle à 5 m.",
        configurations = listOf("cat-litepanels-gemini-2x1")
    ),
    FicheProjecteur(
        nom = "Celeb",
        fabricant = "Kino Flo",
        annee = 2014,
        anneeApprox = true,
        categorie = "Panneaux LED",
        silhouette = Silhouette.PROJ_PANNEAU,
        punchline = "Le panneau LED de la marque qui a fait la lumière fluorescente du cinéma.",
        chiffresCles = listOf(
            "Celeb 450" vaut "4 860 lux à 1 m (à relire)",
            "Couleur" vaut "2500 à 9900 K"
        ),
        histoire = "Kino Flo s'est fait un nom avec ses projecteurs à tubes fluorescents, devenus des classiques de la lumière douce. La gamme Celeb a fait passer ce savoir-faire à la LED.",
        ficheTechnique = "Panneaux LED réglables de 2500 à 9900 K. Celeb 450 : 4 860 lux à 1 m selon les distributeurs, sans température indiquée — à relire.",
        commentLUtiliser = "En lumière douce de face, avec des nids d'abeille pour la tenir, comme on le faisait avec les Kino à tubes.",
        atouts = "La douceur et l'ergonomie héritées des tubes, et une couleur réglable.",
        limites = "Peu de portée ; des chiffres publiés peu détaillés.",
        aRetenir = "« Un Kino » désigne encore, sur les plateaux, une lumière douce et plate, même en LED.",
        configurations = listOf("cat-kinoflo-celeb-450")
    ),
    FicheProjecteur(
        nom = "Titan et Helios",
        fabricant = "Astera",
        annee = 2017,
        anneeApprox = true,
        categorie = "Tubes LED sur batterie",
        silhouette = Silhouette.PROJ_TUBE,
        punchline = "Des tubes sans fil qu'on cache partout : la lumière qui fait partie du décor.",
        chiffresCles = listOf(
            "Titan Tube" vaut "87 lux à 3 m",
            "Helios Tube" vaut "360 lux à 1 m (3200 K)",
            "Alimentation" vaut "batterie intégrée"
        ),
        histoire = "Les tubes Astera ont changé la façon d'éclairer les décors : des tubes LED couleur sur batterie, pilotés sans fil, qu'on peut poser dans le champ, cacher derrière un meuble ou suspendre en grappe.",
        ficheTechnique = "Tubes LED couleur à batterie intégrée, pilotage sans fil. Le Helios est la version courte du Titan. Titan Tube : 87 lux à 3 m (même valeur à 3200 et 5600 K). Helios Tube : 360 lux à 1 m à 3200 K (393 à 4000 K).",
        commentLUtiliser = "En source visible dans le cadre (néon, lampe de décor), en lisière cachée dans un coin, en effet de couleur, ou en rangée pour un plafond lumineux. Aucun câble : l'installation dure le temps de les poser.",
        atouts = "Autonomes, légers, sans câble, et pilotables pixel par pixel pour des effets.",
        limites = "Très peu de puissance : ce sont des accents, pas des faces. L'autonomie limite les longues journées.",
        aRetenir = "Un tube Astera s'utilise souvent dans le champ : sa lumière est un élément du décor autant qu'un éclairage.",
        configurations = listOf("cat-astera-titan", "cat-astera-helios")
    )
)

private fun hmiEtTungstene(): List<FicheProjecteur> = listOf(
    FicheProjecteur(
        nom = "M18",
        fabricant = "ARRI",
        annee = 2007,
        anneeApprox = true,
        categorie = "HMI à réflecteur focalisable",
        silhouette = Silhouette.PROJ_HMI,
        punchline = "Le HMI de 1 800 W qu'on branche sur une prise ordinaire, et qui éclaire comme un plus gros.",
        chiffresCles = listOf(
            "Lampe" vaut "HMI 1 800 W",
            "Serré (15°)" vaut "11 250 lux à 10 m",
            "Ouvert (58°)" vaut "2 245 lux à 10 m",
            "Couleur" vaut "lumière du jour (HMI)"
        ),
        histoire = "La série M d'ARRI (M18, M40, M90) a rapproché deux familles : l'efficacité d'un projecteur ouvert à réflecteur et la netteté d'un faisceau focalisable. Le M18 est devenu un classique des tournages en décors réels, parce qu'il tient sur un circuit domestique.",
        ficheTechnique = "Lampe aux halogénures métalliques (HMI) de 1 800 W, alimentée par un ballast. Faisceau réglable de 15° à 58°. ARRI publie à 10 m : 11 250 lux en position serrée et 2 245 lux en position ouverte.",
        commentLUtiliser = "En soleil à travers une fenêtre, en face lointaine à travers un grand cadre, en rebond sur un panneau blanc. Le ballast électronique « flicker free » permet les cadences non standard ; avec un ballast magnétique, s'en tenir aux angles sûrs (voir Lumière › Scintillement).",
        atouts = "Une très forte puissance pour sa consommation, une lumière du jour franche, une portée qui permet de travailler loin du décor.",
        limites = "Une lampe à amorcer (et à laisser refroidir avant de la rallumer à chaud), un ballast à câbler, une lampe qui s'use et dérive en couleur. Les UV sont filtrés par la lentille de protection, qu'il ne faut jamais retirer.",
        aRetenir = "Le M18 est la porte d'entrée du HMI : assez puissant pour un vrai soleil de fenêtre, assez sobre pour un appartement.",
        configurations = listOf("cat-arri-m18")
    ),
    FicheProjecteur(
        nom = "True Blue T2",
        fabricant = "ARRI",
        annee = 2008,
        anneeApprox = true,
        categorie = "Fresnel tungstène 2 kW",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "Le « deux kilos » : la Fresnel tungstène de référence, avec sa lumière à spectre continu.",
        chiffresCles = listOf(
            "Lampe" vaut "halogène 2 000 W, 3200 K",
            "Serré (10°)" vaut "6 017 lux à 6 m",
            "Ouvert (54°)" vaut "958 lux à 6 m"
        ),
        histoire = "La Fresnel tungstène est le projecteur historique du cinéma : une lampe à filament, un miroir, et la lentille à échelons inventée par Augustin Fresnel pour les phares. La gamme True Blue est la génération actuelle des Fresnels tungstène d'ARRI.",
        ficheTechnique = "Lampe halogène de 2 000 W à 3200 K, lentille Fresnel, faisceau réglable de 10° à 54°. Avec la lampe CP/73 : 6 017 lux à 6 m en position serrée, 958 lux en position ouverte.",
        commentLUtiliser = "En face ou en contre dans un intérieur nuit, à travers une diffusion ou en direct pour une lumière dure aux bords nets. Les volets coupent proprement le faisceau ; les filets et les gradateurs règlent l'intensité (la gradation réchauffe la couleur).",
        atouts = "Un spectre continu, des peaux superbes, un faisceau qui se règle et se découpe parfaitement, une fiabilité totale.",
        limites = "Beaucoup de chaleur et de consommation pour la lumière produite ; il chauffe le décor et les comédiens. Le gradateur fait virer au chaud.",
        aRetenir = "Baisser un tungstène au gradateur le rend plus orange : pour garder la couleur, on filtre ou on recule.",
        configurations = listOf("cat-arri-t2")
    ),
    FicheProjecteur(
        nom = "650 Plus",
        fabricant = "ARRI",
        annee = 2000,
        anneeApprox = true,
        categorie = "Fresnel tungstène 650 W",
        silhouette = Silhouette.PROJ_FRESNEL,
        punchline = "La petite Fresnel qu'on trouve dans tous les kits : le premier projecteur de beaucoup.",
        chiffresCles = listOf(
            "Lampe" vaut "halogène 650 W, 3200 K",
            "Serré (14,5°)" vaut "2 110 lux à 5 m",
            "Ouvert" vaut "52°"
        ),
        histoire = "Petite sœur des Fresnels de studio, la 650 Plus est un pilier des kits tungstène d'entretien et de court métrage : légère, simple, increvable.",
        ficheTechnique = "Lampe halogène de 650 W à 3200 K, lentille Fresnel, faisceau de 14,5° (serré) à 52° (ouvert). En position serrée : 2 110 lux à 5 m.",
        commentLUtiliser = "En contre, en effet de lampe, en lumière de découpe sur un mur, ou en face à travers une petite diffusion. Elle se range dans les petites pièces où les gros projecteurs n'entrent pas.",
        atouts = "Légère, pas chère, un beau faisceau, et elle se branche partout.",
        limites = "Peu de puissance face au jour ; elle chauffe ; tungstène seulement.",
        aRetenir = "La 650 est le meilleur professeur : avec elle, on apprend ce que font le spot, le flood et les volets.",
        configurations = listOf("cat-arri-650-plus")
    )
)
