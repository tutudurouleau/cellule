package fr.cellule.core

/*
 * Le catalogue des caméras.
 *
 * Chaque attribution de film a été recoupée avec des sources de la profession
 * (fabricants, American Society of Cinematographers, entretiens de chefs
 * opérateurs). Quand un chiffre varie selon les versions ou les firmwares, la
 * fiche donne un ordre de grandeur plutôt qu'une fausse précision.
 *
 * Une fonction par étagère : chacune reste une méthode de taille raisonnable
 * une fois compilée, et l'ordre des étagères est celui de l'écran.
 */

val ETAGERES_CAMERAS: List<Etagere<Camera>> = listOf(
    Etagere("ARRI — le numérique", arriNumerique()),
    Etagere("ARRI — la pellicule", arriPellicule()),
    Etagere("RED", red()),
    Etagere("Sony", sony()),
    Etagere("Canon", canon()),
    Etagere("Blackmagic Design", blackmagic()),
    Etagere("Panavision", panavision()),
    Etagere("Autres numériques", autresNumeriques()),
    Etagere("L'histoire en pellicule", histoirePellicule())
)

private fun arriNumerique(): List<Camera> = listOf(
    Camera(
        nom = "ALEXA (Classic, XT, SXT)",
        fabricant = "ARRI",
        annee = 2010,
        categorie = "Numérique Super 35, studio",
        silhouette = Silhouette.STUDIO,
        punchline = "La caméra qui a fait basculer le cinéma de la pellicule au numérique.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · ALEV III",
            "Dynamique" vaut "14 diaphs et plus",
            "EI de base" vaut "800",
            "Résolution" vaut "2,8K à 3,4K (open gate)"
        ),
        histoire = "Présentée en 2010 par un fabricant qui ne vendait jusque-là que des caméras argentiques, elle a convaincu en quelques années les chefs opérateurs les plus attachés à la pellicule : ses hautes lumières et ses carnations se rapprochaient enfin du négatif. Les déclinaisons Plus, Studio (avec viseur optique), XT puis SXT ont affiné la même recette jusqu'en 2018.",
        ficheTechnique = "Capteur CMOS ALEV III au format Super 35, environ 14 diaphs de dynamique, EI de base 800. Enregistrement ProRes interne ; l'ARRIRAW, d'abord confié à un enregistreur externe, devient interne sur la génération XT, qui ajoute le mode « open gate » 3,4K. Mode 4:3 plein capteur pour l'anamorphique. Monture PL. Boîtier de studio, lourd pour l'épaule.",
        commentLUtiliser = "Pensée pour le plateau : sur pied, grue ou Dolly, avec un assistant qui tire le point à distance. On l'expose comme un négatif — à 800 EI on protège les hautes lumières et on laisse respirer les ombres — puis on étalonne depuis le Log C. Son enregistrement ProRes l'a rendue immédiatement compatible avec toute la chaîne de post-production.",
        atouts = "Des carnations et un roulé des hautes lumières qui ont servi de référence au numérique pendant dix ans. Fiable et prévisible, avec une image qui pardonne une exposition approximative.",
        limites = "Résolution modeste face aux capteurs 4K et plus apparus ensuite, poids et encombrement de studio, capteur aujourd'hui dépassé en dynamique par la génération suivante.",
        films = listOf(
            "Skyfall" par "Roger Deakins",
            "Gravity" par "Emmanuel Lubezki",
            "Mad Max: Fury Road" par "John Seale",
            "Blade Runner 2049" par "Roger Deakins"
        ),
        aRetenir = "Sa courbe Log C est devenue une langue commune entre le plateau et l'étalonnage : d'innombrables LUT et émulations de pellicule partent encore d'elle."
    ),
    Camera(
        nom = "AMIRA",
        fabricant = "ARRI",
        annee = 2014,
        categorie = "Numérique Super 35, épaule",
        silhouette = Silhouette.EPAULE,
        punchline = "L'image des ALEXA dans un corps pensé pour l'épaule et le documentaire.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · ALEV III",
            "Cadence" vaut "jusqu'à 200 im/s",
            "Filtres" vaut "ND intégrés",
            "Support" vaut "CFast 2.0"
        ),
        histoire = "Sortie en 2014, elle reprend le capteur des ALEXA de studio mais le loge dans un boîtier équilibré pour l'épaule, avec des commandes à portée de l'opérateur seul. ARRI visait les petites équipes du documentaire, du reportage haut de gamme et de la télévision, qui voulaient l'image du cinéma sans le plateau qui va avec.",
        ficheTechnique = "Capteur ALEV III Super 35, même rendu que les ALEXA de la même génération. Enregistrement ProRes, et ARRIRAW selon la licence, sur cartes CFast 2.0. Filtres neutres motorisés intégrés, cadence jusqu'à 200 images par seconde. Monture PL, EF ou B4 selon la configuration. Environ 4 kg nu, pensé pour reposer sur l'épaule.",
        commentLUtiliser = "On la porte comme une caméra de reportage : épaulée, œil au viseur, main droite sur la poignée. Les ND intégrés permettent de passer du dehors au dedans sans changer de filtre, ce qui compte quand on ne peut pas arrêter la scène. On prévoit des batteries et des cartes CFast en rotation.",
        atouts = "La colorimétrie ARRI sans compromis dans un format mobile, tenue par une seule personne. Robuste et taillée pour de longues journées.",
        limites = "Plus lourde et plus chère que les caméras documentaires concurrentes, sans la résolution des capteurs récents. Sur un plateau, une ALEXA de studio ou une Mini restent plus modulaires.",
        films = listOf("Fuocoammare, par-delà Lampedusa" par "Gianfranco Rosi"),
        aRetenir = "Fuocoammare, documentaire tourné avec elle sur l'île de Lampedusa, a remporté l'Ours d'or à Berlin en 2016 : l'image documentaire rejoignait celle du cinéma."
    ),
    Camera(
        nom = "ALEXA 65",
        fabricant = "ARRI",
        annee = 2014,
        categorie = "Numérique 65 mm, grand format",
        silhouette = Silhouette.STUDIO,
        punchline = "Un capteur de la taille d'un négatif 65 mm, uniquement en location.",
        chiffresCles = listOf(
            "Capteur" vaut "54,12 × 25,58 mm",
            "Résolution" vaut "6,5K",
            "Enregistrement" vaut "ARRIRAW seulement",
            "Disponibilité" vaut "Location ARRI Rental"
        ),
        histoire = "Présentée en 2014, elle répond aux productions qui voulaient l'ampleur du 65 mm argentique sans la pellicule. ARRI ne l'a jamais vendue : elle se loue uniquement auprès de sa branche de location, avec des optiques grand format dédiées.",
        ficheTechnique = "Capteur CMOS A3X de 54,12 × 25,58 mm, environ 6,5K, même science couleur que les ALEXA Super 35. Enregistrement ARRIRAW non compressé sur magasins Codex, des volumes de données considérables. Monture dédiée au grand format. Boîtier de studio.",
        commentLUtiliser = "On la choisit quand l'image doit remplir un écran géant. À focale égale, le champ est bien plus large qu'en Super 35 : il faut des focales longues pour le même cadre, et la profondeur de champ devient très courte, ce qui met l'assistant opérateur sous pression. La post-production doit être dimensionnée pour ces fichiers.",
        atouts = "Une ampleur et une douceur incomparables, où les visages se détachent du décor comme en moyen format photo.",
        limites = "Location exclusive et coûteuse, fichiers massifs, optiques spécifiques et poids de studio : aucun sens pour un tournage léger.",
        films = listOf(
            "The Revenant" par "Emmanuel Lubezki",
            "Rogue One: A Star Wars Story" par "Greig Fraser",
            "Roma" par "Alfonso Cuarón",
            "Avengers: Infinity War" par "Trent Opaloch"
        ),
        aRetenir = "Une version modifiée pour IMAX a permis de tourner Avengers: Infinity War et Endgame intégralement avec des caméras IMAX numériques."
    ),
    Camera(
        nom = "ALEXA Mini",
        fabricant = "ARRI",
        annee = 2015,
        categorie = "Numérique Super 35, compacte",
        silhouette = Silhouette.COMPACTE,
        punchline = "La caméra la plus louée de sa décennie : l'image ALEXA dans un cube de 2,3 kg.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · ALEV III",
            "Poids" vaut "2,3 kg nu",
            "Cadence" vaut "jusqu'à 200 im/s",
            "Filtres" vaut "ND intégrés"
        ),
        histoire = "Conçue en 2015 d'abord pour les drones, les stabilisateurs et les caméras embarquées, elle a vite remplacé les grandes ALEXA comme caméra principale : même capteur, même image, dans un boîtier qu'on tient à bout de bras. Elle est devenue la caméra par défaut du cinéma indépendant comme des séries.",
        ficheTechnique = "Capteur ALEV III Super 35, rendu identique aux ALEXA de studio. ProRes et ARRIRAW sur cartes CFast 2.0, open gate 3,4K, mode 4:3 pour l'anamorphique. Filtres neutres motorisés intégrés. Monture interchangeable (PL en titane, EF, B4). Environ 2,3 kg nu.",
        commentLUtiliser = "Nue sur un drone ou un stabilisateur, habillée en configuration studio avec viseur, batterie et moteurs de mise au point. On la pilote souvent à distance. Sur un gros tournage, elle est presque toujours là, en caméra B ou en caméra embarquée.",
        atouts = "La mobilité sans perte d'image : le cadre va là où une caméra de studio ne passe pas, sans rupture de rendu au montage avec les autres ALEXA.",
        limites = "Une fois équipée, elle perd une partie de sa légèreté ; son capteur de 2010 est dépassé en résolution et en dynamique par les générations suivantes.",
        films = listOf(
            "Get Out" par "Toby Oliver",
            "A Star Is Born" par "Matthew Libatique"
        ),
        aRetenir = "Certaines années, plus des trois quarts des fictions présentées au festival de Sundance ont été tournées avec elle ou avec sa sœur grand format."
    ),
    Camera(
        nom = "ALEXA LF",
        fabricant = "ARRI",
        annee = 2018,
        categorie = "Numérique grand format, studio",
        silhouette = Silhouette.STUDIO,
        punchline = "Le passage d'ARRI au grand format, avec une nouvelle monture pensée pour lui.",
        chiffresCles = listOf(
            "Capteur" vaut "36,70 × 25,54 mm",
            "Résolution" vaut "4,5K",
            "Monture" vaut "LPL",
            "Dynamique" vaut "14 diaphs et plus"
        ),
        histoire = "Présentée en 2018, elle agrandit le capteur ALEV III aux dimensions d'un plein format photo, entre le Super 35 et le 65 mm. ARRI introduit avec elle la monture LPL, plus large que la PL, et une gamme d'optiques adaptées : tout le cinéma haut de gamme passait alors au grand format.",
        ficheTechnique = "Capteur ALEV III LF de 36,70 × 25,54 mm, 4,5K en open gate, même science couleur que les ALEXA Super 35. ARRIRAW et ProRes sur magasins Codex. Filtres neutres intégrés. Monture LPL, adaptateur PL fourni. Boîtier de studio, bien plus encombrant que sa version compacte sortie un an plus tard.",
        commentLUtiliser = "S'emploie comme une ALEXA de plateau, avec des optiques qui couvrent le plein format. À cadre égal, on choisit une focale plus longue qu'en Super 35, avec une profondeur de champ plus courte. Très présente en production virtuelle, devant des murs d'écrans LED.",
        atouts = "La douceur du grand format — visages détachés, perspectives amples — avec la colorimétrie ARRI éprouvée.",
        limites = "Lourde et encombrante, vite éclipsée par sa version compacte ; gourmande en optiques plein format, plus chères.",
        films = listOf(
            "The Batman" par "Greig Fraser",
            "Dune" par "Greig Fraser",
            "The Mandalorian (série)" par "Greig Fraser, Baz Idoine"
        ),
        aRetenir = "The Mandalorian l'a utilisée dans le premier grand plateau cerné de murs LED, où le décor projeté éclairait les acteurs en temps réel : l'acte de naissance de la production virtuelle moderne."
    ),
    Camera(
        nom = "ALEXA Mini LF",
        fabricant = "ARRI",
        annee = 2019,
        categorie = "Numérique grand format, compacte",
        silhouette = Silhouette.COMPACTE,
        punchline = "Le grand format dans un petit boîtier : la caméra de référence des années 2020.",
        chiffresCles = listOf(
            "Capteur" vaut "36,70 × 25,54 mm",
            "Résolution" vaut "4,5K",
            "Poids" vaut "environ 2,6 kg nu",
            "Monture" vaut "LPL"
        ),
        histoire = "Sortie en 2019, elle loge le capteur grand format de l'ALEXA LF dans un corps à peine plus grand que celui de la Mini. Elle a fait de la Steadicam, du drone et du stabilisateur des outils de grand format, et s'est imposée sur les longs-métrages et les séries haut de gamme.",
        ficheTechnique = "Capteur ALEV III LF de 36,70 × 25,54 mm, 4,5K en open gate. ARRIRAW et ProRes sur Codex Compact Drive. Filtres neutres motorisés intégrés, monture LPL, environ 2,6 kg nu. Cadences élevées disponibles en format réduit.",
        commentLUtiliser = "On la monte légère pour les plans en mouvement, complète pour le plateau. Comme toute caméra plein format, on choisit ses optiques en conséquence et l'on anticipe une profondeur de champ courte à pleine ouverture.",
        atouts = "Toute l'image du grand format ARRI sans son poids : un rendu doux et ample devenu la signature des productions haut de gamme récentes.",
        limites = "Capteur de génération 2010, dépassé en dynamique par l'ALEXA 35 ; 4,5K modeste face aux 8K de la concurrence.",
        films = listOf(
            "1917" par "Roger Deakins",
            "Kantara" par "Arvind Kashyap"
        ),
        aRetenir = "1917 a été tourné avec des prototypes, avant même sa sortie commerciale : Deakins avait besoin d'une caméra grand format assez légère pour suivre les soldats dans des plans-séquences de plusieurs minutes."
    ),
    Camera(
        nom = "ALEXA 35",
        fabricant = "ARRI",
        annee = 2022,
        categorie = "Numérique Super 35, compacte",
        silhouette = Silhouette.COMPACTE,
        punchline = "Retour au Super 35, avec la plus grande dynamique jamais annoncée par ARRI.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 4,6K",
            "Dynamique" vaut "17 diaphs",
            "EI" vaut "160 à 6400",
            "Poids" vaut "2,9 kg"
        ),
        histoire = "Sortie en 2022, c'est la première ALEXA dotée d'un capteur entièrement nouveau depuis 2010. Plutôt que de suivre la course aux pixels, ARRI a gardé le Super 35 et misé sur la latitude : la priorité reste la façon dont l'image encaisse hautes lumières et ombres profondes.",
        ficheTechnique = "Capteur ALEV 4 Super 35 de 27,99 × 19,22 mm, 4,6K en open gate 3:2, 17 diaphs de dynamique annoncés. EI de 160 à 6400, avec un mode de sensibilité renforcée pour les très basses lumières. Nouvelle chaîne couleur REVEAL, courbe LogC4. ARRIRAW et ProRes sur Codex Compact Drive, jusqu'à 120 images par seconde. Monture LPL, adaptable PL. Environ 2,9 kg.",
        commentLUtiliser = "Elle s'utilise comme une Mini : légère sur les rigs, complète sur le plateau. Sa latitude autorise des contrastes qu'il fallait autrefois compresser à l'éclairage, comme une fenêtre en plein soleil derrière un visage dans l'ombre. Ses « Textures » se choisissent avant de tourner : elles façonnent grain et contraste à la prise de vue et ne se défont pas en post-production.",
        atouts = "La plus large latitude d'exposition de sa génération, des carnations très naturelles même en lumière mélangée, et une vraie tolérance aux erreurs d'exposition.",
        limites = "4,6K seulement : un choix assumé, mais une limite pour qui veut recadrer massivement ou livrer en 8K. Nouvelle chaîne couleur à apprivoiser en étalonnage.",
        films = listOf(
            "Air" par "Robert Richardson",
            "Parthenope" par "Daria D'Antonio"
        ),
        aRetenir = "Elle introduit les « ARRI Textures », des profils de grain et de contraste inscrits à la prise de vue : on choisit un rendu avant de tourner, comme on choisissait autrefois une pellicule."
    )
)

private fun arriPellicule(): List<Camera> = listOf(
    Camera(
        nom = "ARRIFLEX 16SR",
        fabricant = "ARRI",
        annee = 1975,
        categorie = "Argentique 16 mm et Super 16, épaule",
        silhouette = Silhouette.EPAULE,
        punchline = "La 16 mm d'épaule qui a porté des décennies de documentaire et de cinéma fauché.",
        chiffresCles = listOf(
            "Format" vaut "16 mm, Super 16 (SR3)",
            "Magasin" vaut "Coaxial de 120 m",
            "Visée" vaut "Reflex",
            "Son" vaut "Silencieuse, synchrone"
        ),
        histoire = "Lancée en 1975, la série SR a rendu le tournage 16 mm synchrone vraiment mobile : une caméra silencieuse, à visée reflex, qui se pose sur l'épaule. Trois générations se sont succédé ; la SR3 de 1992 est la première conçue d'origine pour le Super 16, format élargi qui se gonfle bien en 35 mm.",
        ficheTechnique = "Pellicule 16 mm (Super 16 sur la SR3 ou sur les modèles modifiés), magasins coaxiaux de 120 m clipsés à l'arrière : un peu plus de onze minutes à 24 images par seconde. Visée reflex par obturateur à miroir, fonctionnement assez silencieux pour le son direct.",
        commentLUtiliser = "On la porte à l'épaule, magasin en arrière pour l'équilibre. On mesure la lumière à la cellule, on connaît sa pellicule, et l'on économise : chaque minute tournée se paie en négatif et en laboratoire. Le rythme du tournage s'organise autour des changements de magasin.",
        atouts = "Fiabilité légendaire, ergonomie d'épaule, coût d'exploitation bien plus bas que le 35 mm. Le grain du 16 mm, texturé et vivant, reste recherché pour lui-même.",
        limites = "Définition et grain limités face au 35 mm, bobines courtes, et aucune image fiable avant le développement.",
        films = listOf(
            "Clerks" par "David Klein",
            "Les Berkman se séparent (The Squid and the Whale)" par "Robert Yeoman"
        ),
        aRetenir = "Clerks, tourné en noir et blanc pour environ 27 000 dollars, en est le symbole : un premier film fauché en 16 mm qui a lancé une carrière."
    ),
    Camera(
        nom = "ARRIFLEX 535",
        fabricant = "ARRI",
        annee = 1990,
        categorie = "Argentique 35 mm, studio",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La 35 mm de studio des années 1990, capable de changer de vitesse en pleine prise.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Cadence" vaut "3 à 60 im/s",
            "Obturateur" vaut "Réglable en marche",
            "Son" vaut "Silencieuse"
        ),
        histoire = "Introduite en 1990, suivie d'une version allégée 535B en 1994, elle a été l'une des grandes caméras de studio de la décennie. Roger Deakins en a fait sa caméra de prédilection pendant une grande partie de sa carrière en pellicule.",
        ficheTechnique = "Caméra 35 mm silencieuse pour le son direct, cadence de 3 à 60 images par seconde. Obturateur à miroir dont l'angle se modifie pendant la prise, viseur réglable pour le sphérique, l'anamorphique et la vidéo d'assistance. Monture PL. Lourde : c'est une caméra de pied et de grue.",
        commentLUtiliser = "On la pose sur tête fluide ou tête à manivelles, avec un assistant au point. Sa grande spécialité : faire varier la cadence pendant le plan en compensant automatiquement l'exposition par l'angle d'obturation, pour passer sans coupe du temps réel au ralenti.",
        atouts = "Robuste, précise, silencieuse, et capable d'effets de vitesse en caméra qui demandaient auparavant des trucages de laboratoire.",
        limites = "Poids et encombrement de studio, inadaptée à l'épaule ; comme toute caméra argentique, aucune image avant le développement.",
        films = listOf(
            "Dracula" par "Michael Ballhaus",
            "Little Buddha" par "Vittorio Storaro"
        ),
        aRetenir = "Son angle d'obturation réglable en pleine prise a popularisé les « speed ramps », ces accélérations et ralentis dans un même plan devenus un tic de la publicité et du clip."
    ),
    Camera(
        nom = "ARRIFLEX 435",
        fabricant = "ARRI",
        annee = 1995,
        categorie = "Argentique 35 mm, grande vitesse",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La 35 mm des ralentis, des cascades et des obturations trafiquées.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Cadence" vaut "1 à 150 im/s",
            "Son" vaut "Non silencieuse",
            "Obturateur" vaut "Électronique réglable"
        ),
        histoire = "Apparue au milieu des années 1990, elle n'est pas faite pour le son direct : elle sacrifie le silence à la vitesse et à la légèreté. Déclinée en ES, Advanced et Xtreme, elle est devenue la caméra des ralentis, des scènes d'action et des effets en caméra.",
        ficheTechnique = "Pellicule 35 mm, cadence de 1 à 150 images par seconde, en marche avant comme en arrière. Obturateur à miroir réglable électroniquement, rampes de vitesse programmables avec compensation d'exposition. Relativement compacte pour du 35 mm. Monture PL.",
        commentLUtiliser = "On la sort pour les ralentis, les cascades et la Steadicam en pellicule. Doubler la cadence retire un diaph : on compense à l'éclairage ou à l'ouverture. Réduire l'angle d'obturation donne des mouvements saccadés et nets, au prix de la lumière.",
        atouts = "Grande vitesse en 35 mm, effets de cadence et d'obturation programmables, format assez léger pour l'épaule ou la Steadicam.",
        limites = "Trop bruyante pour le son direct : les scènes dialoguées se tournent avec une autre caméra ou se postsynchronisent.",
        films = listOf("Il faut sauver le soldat Ryan" par "Janusz Kamiński"),
        aRetenir = "Pour le débarquement d'Il faut sauver le soldat Ryan, Kamiński a réduit l'angle d'obturation : chaque image, plus brève, fige les mouvements et les projections et rappelle les actualités filmées de la guerre."
    ),
    Camera(
        nom = "ARRICAM ST / LT",
        fabricant = "ARRI",
        annee = 2000,
        categorie = "Argentique 35 mm, studio et épaule",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "Le dernier grand système 35 mm d'ARRI : une version studio, une version légère.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Versions" vaut "Studio (ST), Lite (LT)",
            "Son" vaut "Silencieuse",
            "Cadence" vaut "jusqu'à 60 im/s (ST)"
        ),
        histoire = "Lancé en 2000, le système réunit deux caméras qui partagent magasins, accessoires et mécanique : la Studio, lourde et très silencieuse, et la Lite, allégée pour l'épaule et la Steadicam. C'est la génération des dernières grandes années de la pellicule, et celle que l'on loue encore pour les tournages qui la défendent.",
        ficheTechnique = "Pellicule 35 mm, fonctionnement très silencieux pour le son direct, cadence jusqu'à environ 60 images par seconde sur la Studio. Visée reflex, retour vidéo, gestion électronique de l'obturateur et des données d'objectif. Magasins communs aux deux corps. Monture PL.",
        commentLUtiliser = "La Studio reste sur pied, grue ou Dolly pour les scènes dialoguées ; la Lite prend le relais à l'épaule ou en Steadicam avec les mêmes magasins. On travaille à la cellule et au rapport de laboratoire : l'image se découvre aux rushes.",
        atouts = "Silence, fiabilité et modularité : un seul système pour le plateau et le mouvement.",
        limites = "Coût de la pellicule et du laboratoire, aucune image avant le développement : tourner en pellicule aujourd'hui est un choix qui se défend en production.",
        films = listOf("Killers of the Flower Moon" par "Rodrigo Prieto"),
        aRetenir = "Pour Killers of the Flower Moon, en 2023, Prieto a tourné l'essentiel en 35 mm avec ce système et n'a gardé le numérique que pour quelques scènes : la pellicule reste un choix actif."
    )
)

private fun red(): List<Camera> = listOf(
    Camera(
        nom = "ONE",
        fabricant = "RED",
        annee = 2007,
        categorie = "Numérique Super 35",
        silhouette = Silhouette.STUDIO,
        punchline = "La caméra qui a rendu le 4K et le RAW accessibles, et bousculé toute l'industrie.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 4K",
            "Enregistrement" vaut "REDCODE RAW",
            "Prix à la sortie" vaut "17 500 dollars le corps",
            "Monture" vaut "PL"
        ),
        histoire = "Portée par Jim Jannard, fondateur des lunettes Oakley, elle arrive en 2007 avec un capteur Mysterium 4K et un prix sans commune mesure avec les caméras numériques de cinéma de l'époque. Elle ouvre la voie au RAW compressé et au 4K pour les productions indépendantes, puis convainc Steven Soderbergh et David Fincher.",
        ficheTechnique = "Capteur CMOS Mysterium au format Super 35, jusqu'à 4K. Enregistrement REDCODE RAW, un RAW compressé par ondelettes. Monture PL. Boîtier lourd, gourmand en énergie, qui chauffe ; la version MX améliore nettement la sensibilité et le bruit.",
        commentLUtiliser = "On la traite comme une pellicule numérique : on expose pour garder les hautes lumières et l'on décide la couleur à l'étalonnage, puisque le RAW n'impose rien à la prise de vue. Il faut une chaîne de post-production capable de décoder le REDCODE, ce qui était encore rare à sa sortie.",
        atouts = "La résolution 4K et la souplesse du RAW à un prix accessible : elle a démocratisé l'image de cinéma numérique.",
        limites = "Premières versions capricieuses (démarrage lent, chauffe, bruit en basse lumière), ergonomie lourde, post-production exigeante pour l'époque.",
        films = listOf(
            "Che" par "Steven Soderbergh",
            "The Social Network" par "Jeff Cronenweth",
            "District 9" par "Trent Opaloch"
        ),
        aRetenir = "Soderbergh a tourné les deux volets de Che avec des exemplaires de présérie : l'un des premiers grands projets à parier sur elle avant sa commercialisation."
    ),
    Camera(
        nom = "EPIC (MX, Dragon)",
        fabricant = "RED",
        annee = 2011,
        categorie = "Numérique Super 35, modulaire",
        silhouette = Silhouette.COMPACTE,
        punchline = "Un « cerveau » compact en 5K autour duquel on construit sa caméra.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 5K (MX)",
            "Évolution" vaut "Dragon 6K",
            "Mode" vaut "HDRx",
            "Poids" vaut "environ 2,3 kg nu"
        ),
        histoire = "Sortie en 2011, elle inaugure chez RED le principe du « brain » : un corps compact auquel on ajoute écran, alimentation, poignées et enregistreur selon le tournage. Son capteur Mysterium-X 5K sera remplacé par le Dragon 6K, meilleur en couleur et en dynamique.",
        ficheTechnique = "Capteur Super 35 Mysterium-X 5K puis Dragon 6K, REDCODE RAW sur SSD. Mode HDRx qui combine deux expositions pour étendre la dynamique. Cadences élevées en résolution réduite. Monture interchangeable.",
        commentLUtiliser = "On la configure selon le besoin : nue sur un rig 3D ou un drone, complète sur le plateau. Sa compacité en a fait la caméra idéale des rigs stéréoscopiques, où deux caméras doivent tenir côte à côte.",
        atouts = "Haute résolution, compacité, modularité : une caméra de long-métrage qui tient dans les mains.",
        limites = "Accessoires propriétaires et coûteux, colorimétrie qui demande de l'expérience à l'étalonnage, consommation électrique notable.",
        films = listOf(
            "Le Hobbit : Un voyage inattendu" par "Andrew Lesnie",
            "Prometheus" par "Dariusz Wolski",
            "The Amazing Spider-Man" par "John Schwartzman"
        ),
        aRetenir = "La trilogie du Hobbit a été tournée avec des dizaines de ces caméras montées par paires sur des rigs 3D, à 48 images par seconde : le double de la cadence du cinéma."
    ),
    Camera(
        nom = "WEAPON 8K VV",
        fabricant = "RED",
        annee = 2016,
        categorie = "Numérique grand format (VistaVision)",
        silhouette = Silhouette.COMPACTE,
        punchline = "La première caméra 8K grand format d'un blockbuster.",
        chiffresCles = listOf(
            "Capteur" vaut "Dragon 8K VV",
            "Dimensions" vaut "40,96 × 21,60 mm",
            "Résolution" vaut "8192 × 4320",
            "Corps" vaut "DSMC2 modulaire"
        ),
        histoire = "En 2016, RED place dans son corps modulaire DSMC2 un capteur Dragon 8K au format VistaVision, plus grand que le Super 35. Les Gardiens de la Galaxie Vol. 2 est tourné avec les premiers exemplaires : le grand format numérique entre dans le blockbuster.",
        ficheTechnique = "Capteur Dragon 8K VV de 40,96 × 21,60 mm, environ 35 millions de photosites, REDCODE RAW sur SSD. Corps DSMC2 modulaire, commun à plusieurs capteurs de la marque. Monture interchangeable.",
        commentLUtiliser = "Sa résolution sert d'abord la post-production : effets visuels, recadrages, stabilisation. Le grand capteur impose des optiques qui le couvrent et une mise au point exigeante.",
        atouts = "Définition très élevée en grand format : du détail à revendre pour les effets visuels et les livraisons haute résolution.",
        limites = "Fichiers lourds, capteur vite remplacé par le Monstro, colorimétrie Dragon plus délicate à étalonner que les générations suivantes.",
        films = listOf("Les Gardiens de la Galaxie Vol. 2" par "Henry Braham"),
        aRetenir = "Les Gardiens de la Galaxie Vol. 2 est présenté comme le premier long-métrage tourné en 8K grand format."
    ),
    Camera(
        nom = "MONSTRO 8K VV",
        fabricant = "RED",
        annee = 2017,
        categorie = "Numérique grand format (VistaVision)",
        silhouette = Silhouette.COMPACTE,
        punchline = "Le capteur 8K grand format de RED, jusqu'en version noir et blanc pure.",
        chiffresCles = listOf(
            "Capteur" vaut "40,96 × 21,60 mm",
            "Résolution" vaut "8K",
            "Dynamique" vaut "17 diaphs et plus annoncés",
            "Variante" vaut "Monochrome"
        ),
        histoire = "Successeur du Dragon 8K VV dès 2017, il améliore nettement la dynamique, le bruit et la couleur. Il existe en version monochrome, sans filtres colorés sur le capteur, pour un noir et blanc plus fin et plus sensible.",
        ficheTechnique = "Capteur 8K VV de 40,96 × 21,60 mm, REDCODE RAW, 8K jusqu'à environ 60 images par seconde. Proposé dans le corps modulaire DSMC2 puis dans le corps intégré Ranger. Version monochrome sans matrice de Bayer.",
        commentLUtiliser = "Même logique que les autres RED : on protège les hautes lumières et l'on décide à l'étalonnage. En monochrome, on filtre comme en pellicule noir et blanc : un filtre jaune ou rouge devant l'objectif assombrit le ciel et adoucit les peaux.",
        atouts = "Grande latitude et définition élevée ; la version monochrome offre un noir et blanc sans dématriçage, plus net et plus sensible.",
        limites = "Fichiers volumineux, et un rendu qui reste plus « numérique » que celui d'ARRI sans un étalonnage attentif.",
        films = listOf(
            "Mank" par "Erik Messerschmidt",
            "Portrait de la jeune fille en feu" par "Claire Mathon"
        ),
        aRetenir = "Mank, en noir et blanc, a été tourné avec sa version monochrome : aucune couleur n'a jamais été enregistrée."
    ),
    Camera(
        nom = "KOMODO (et Komodo-X)",
        fabricant = "RED",
        annee = 2020,
        categorie = "Numérique Super 35, ultracompacte",
        silhouette = Silhouette.COMPACTE,
        punchline = "Un cube d'un kilo à obturateur global, taillé pour les caméras de cascade.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 6K",
            "Obturateur" vaut "Global",
            "Poids" vaut "environ 1 kg",
            "Monture" vaut "RF"
        ),
        histoire = "Sortie en 2020, elle concentre un capteur 6K dans un petit cube. Son obturateur global, qui expose toute l'image au même instant, la rend précieuse pour l'action rapide et les caméras embarquées. La version X de 2023 accélère le capteur et l'enregistrement.",
        ficheTechnique = "Capteur Super 35 6K à obturateur global, REDCODE RAW sur cartes CFast 2.0. Monture RF native, adaptable PL ou EF. Environ un kilo nu, écran tactile sur le dessus, pilotage sans fil.",
        commentLUtiliser = "On l'accroche là où l'on n'oserait pas mettre une caméra principale : capot de voiture, casque, cascade, drone léger. On en pose souvent plusieurs pour couvrir une action en une seule prise. Sur un plateau, elle sert de caméra B à l'image compatible avec les grandes RED.",
        atouts = "Aucune déformation d'obturateur sur les mouvements rapides, poids plume, coût modéré pour une image RAW de cinéma.",
        limites = "Sensibilité et dynamique inférieures aux grandes caméras, ergonomie minimale qu'il faut équiper pour un usage principal.",
        films = listOf(
            "Furiosa : une saga Mad Max" par "Simon Duggan",
            "The Killer" par "Erik Messerschmidt"
        ),
        aRetenir = "Son obturateur global expose toute l'image en même temps : les pales d'hélice et les poteaux défilant à grande vitesse ne se tordent pas, contrairement aux capteurs à obturateur roulant."
    ),
    Camera(
        nom = "V-RAPTOR",
        fabricant = "RED",
        annee = 2021,
        categorie = "Numérique grand format (VistaVision)",
        silhouette = Silhouette.COMPACTE,
        punchline = "Du 8K grand format à 120 images par seconde, dans un corps compact.",
        chiffresCles = listOf(
            "Capteur" vaut "8K VV · 40,96 × 21,60 mm",
            "Cadence" vaut "8K jusqu'à 120 im/s",
            "Support" vaut "CFexpress",
            "Variante" vaut "[X] à obturateur global"
        ),
        histoire = "Sortie fin 2021, elle ouvre la génération DSMC3 de RED : un capteur 8K grand format, des cadences très élevées et un corps compact. La version [X] de 2023 adopte un obturateur global à cette résolution, un argument décisif pour les effets visuels.",
        ficheTechnique = "Capteur 8K VV de 40,96 × 21,60 mm (existe aussi en Super 35), 8K jusqu'à 120 images par seconde et bien davantage en résolution réduite. REDCODE RAW sur cartes CFexpress. Monture RF native, adaptable PL. Environ 1,8 kg nu.",
        commentLUtiliser = "Caméra principale d'un tournage à effets visuels ou machine à ralentis en haute définition. Le RAW permet de retoucher exposition et balance en post-production ; il faut une chaîne capable d'absorber des fichiers 8K.",
        atouts = "Résolution, cadence et, en version [X], aucune déformation d'obturateur : une source idéale pour le compositing et le recadrage.",
        limites = "Moins de latitude annoncée que l'ALEXA 35 et un rendu qui demande un étalonnage soigné pour paraître organique.",
        films = listOf(
            "The Killer" par "Erik Messerschmidt",
            "Les Gardiens de la Galaxie Vol. 3" par "Henry Braham"
        ),
        aRetenir = "David Fincher et Erik Messerschmidt, fidèles à la marque depuis ses débuts, l'ont adoptée pour The Killer avec des optiques Leitz Summilux-C."
    )
)

private fun sony(): List<Camera> = listOf(
    Camera(
        nom = "HDW-F900",
        fabricant = "Sony",
        annee = 2000,
        categorie = "Numérique HD, caméscope 2/3 de pouce",
        silhouette = Silhouette.EPAULE,
        punchline = "Le caméscope HD à 24 images par seconde du premier Star Wars numérique.",
        chiffresCles = listOf(
            "Capteur" vaut "3 CCD de 2/3 de pouce",
            "Résolution" vaut "1920 × 1080",
            "Cadence" vaut "24p",
            "Support" vaut "Cassette HDCAM"
        ),
        histoire = "Lancée en 2000 sous le label CineAlta, c'est l'une des premières caméras haute définition capables d'enregistrer à 24 images par seconde progressives, la cadence du cinéma. George Lucas s'en sert pour L'Attaque des clones : le numérique pouvait prétendre au grand écran.",
        ficheTechnique = "Trois capteurs CCD de 2/3 de pouce derrière un prisme, image 1920 × 1080, enregistrement sur cassette HDCAM compressée. Monture B4 de télévision : les optiques de cinéma s'y adaptent mal à cause du petit capteur.",
        commentLUtiliser = "S'utilise comme un caméscope de télévision haut de gamme, à l'épaule ou sur pied, avec des zooms de reportage. Sa faible latitude oblige à éclairer et exposer avec précision : les hautes lumières saturent vite.",
        atouts = "Une image HD nette et un vrai 24p, avec un flux de travail sur cassette familier de la télévision.",
        limites = "Petit capteur, profondeur de champ très étendue et difficile à réduire, dynamique étroite, compression de la cassette.",
        films = listOf(
            "Star Wars, épisode II : L'Attaque des clones" par "David Tattersall",
            "Il était une fois au Mexique" par "Robert Rodriguez"
        ),
        aRetenir = "L'Attaque des clones, en 2002, est présenté comme le premier grand film hollywoodien tourné intégralement en numérique haute définition."
    ),
    Camera(
        nom = "F65",
        fabricant = "Sony",
        annee = 2012,
        categorie = "Numérique Super 35, studio",
        silhouette = Silhouette.STUDIO,
        punchline = "Le vaisseau amiral de Sony, avec un vrai obturateur mécanique.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35, annoncé « 8K »",
            "Sortie" vaut "4K",
            "Obturateur" vaut "Mécanique rotatif",
            "Enregistrement" vaut "F65RAW"
        ),
        histoire = "Présentée en 2011 et livrée début 2012, c'est la réponse de Sony aux ALEXA et aux RED : un capteur Super 35 d'environ 20 millions de photosites, présenté comme « 8K », conçu pour produire une 4K très fine. Elle a servi à de grands films de science-fiction avant d'être supplantée par la VENICE.",
        ficheTechnique = "Capteur Super 35 d'environ 20 millions de photosites, disposition particulière des filtres colorés pour une vraie 4K. Obturateur rotatif mécanique qui supprime l'effet d'obturateur roulant. F65RAW sur cartes SRMemory via un enregistreur accolé. Monture PL.",
        commentLUtiliser = "Caméra de plateau lourde, à traiter comme une caméra de studio. Son RAW demande une post-production solide ; son obturateur mécanique la rend sûre pour les mouvements rapides et les effets visuels.",
        atouts = "Grande finesse 4K, aucune déformation d'obturateur, couleur très fidèle.",
        limites = "Poids, encombrement, flux de travail lourd : elle a vite été éclipsée par des caméras plus maniables.",
        films = listOf(
            "Oblivion" par "Claudio Miranda",
            "After Earth" par "Peter Suschitzky"
        ),
        aRetenir = "Son obturateur mécanique rotatif, rare en numérique, occulte le capteur entre deux images, exactement comme sur une caméra argentique."
    ),
    Camera(
        nom = "F55 et F5",
        fabricant = "Sony",
        annee = 2013,
        categorie = "Numérique Super 35, polyvalente",
        silhouette = Silhouette.COMPACTE,
        punchline = "La caméra d'une Palme d'or tournée à l'épaule, en 4K.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 4K",
            "Obturateur" vaut "Global sur la F55",
            "Codec" vaut "XAVC",
            "RAW" vaut "Par enregistreur accolé"
        ),
        histoire = "Sorties en 2013, les deux sœurs partagent un corps compact et modulaire ; la F55 ajoute un obturateur global et une couleur plus étendue. Pendant plusieurs années, elles ont été un outil de base de la série, du documentaire et d'une partie du cinéma d'auteur.",
        ficheTechnique = "Capteur Super 35 4K (4096 × 2160), obturateur global sur la F55. XAVC interne, RAW via un enregistreur externe accolé. Monture FZ avec adaptateur PL. Corps compact configurable en épaule ou en studio.",
        commentLUtiliser = "On l'équipe en caméra d'épaule pour le tournage mobile, ou en configuration studio. Le XAVC interne suffit à la plupart des productions ; le RAW se réserve aux projets qui prévoient un étalonnage poussé.",
        atouts = "Légèreté, polyvalence, vraie 4K et, sur la F55, un obturateur global qui évite les déformations.",
        limites = "Rendu moins organique que les ALEXA de la même époque, ergonomie très dépendante des accessoires.",
        films = listOf(
            "Dheepan" par "Éponine Momenceau",
            "Mon roi" par "Claire Mathon"
        ),
        aRetenir = "Dheepan, de Jacques Audiard, tourné entièrement caméra à l'épaule avec elle, a remporté la Palme d'or à Cannes en 2015."
    ),
    Camera(
        nom = "VENICE et VENICE 2",
        fabricant = "Sony",
        annee = 2017,
        categorie = "Numérique plein format, studio",
        silhouette = Silhouette.STUDIO,
        punchline = "Le plein format de Sony, dont la tête peut se détacher du corps.",
        chiffresCles = listOf(
            "Capteur" vaut "Plein format 36 × 24 mm",
            "Résolution" vaut "6K, puis 8,6K",
            "ISO de base" vaut "Double (500/2500, puis 800/3200)",
            "Filtres" vaut "ND intégrés, 8 crans"
        ),
        histoire = "Annoncée en 2017, elle a fait entrer Sony dans le cercle des caméras de long-métrage de premier plan. Son extension Rialto permet de détacher le bloc capteur du corps et de le relier par câble, pour glisser une vraie caméra de cinéma dans un cockpit ou sur un rig 3D. La version 2, en 2021, ajoute un capteur 8,6K et le RAW interne.",
        ficheTechnique = "Capteur plein format 36 × 24 mm, 6K puis 8,6K sur la version 2. Double sensibilité de base : 500 et 2500 ISO, puis 800 et 3200 sur le capteur 8,6K. Filtres neutres optiques intégrés de 0,3 à 2,4. Enregistrement X-OCN, un RAW compressé. Monture PL, monture E sous la bague.",
        commentLUtiliser = "La double sensibilité se choisit selon la lumière : la plus haute sert la nuit et les intérieurs sombres sans bruit notable. Les ND intégrés font gagner un temps précieux. En Rialto, la tête pèse peu et se loge dans des espaces impossibles, pendant que le corps reste à distance.",
        atouts = "Superbe tenue en basse lumière, carnations réputées, plein format ou Super 35 au choix, extension Rialto unique.",
        limites = "Corps imposant et lourd en configuration studio ; fichiers volumineux en 8,6K.",
        films = listOf(
            "Top Gun: Maverick" par "Claudio Miranda",
            "Avatar : La Voie de l'eau" par "Russell Carpenter",
            "Killers of the Flower Moon (scènes numériques)" par "Rodrigo Prieto"
        ),
        aRetenir = "Pour Top Gun: Maverick, six caméras étaient installées dans le cockpit des avions de chasse, dont plusieurs têtes détachées : les acteurs lançaient eux-mêmes l'enregistrement en vol."
    ),
    Camera(
        nom = "BURANO",
        fabricant = "Sony",
        annee = 2023,
        categorie = "Numérique plein format, compacte",
        silhouette = Silhouette.COMPACTE,
        punchline = "Le capteur de la VENICE 2 dans un corps stabilisé, pour les petites équipes.",
        chiffresCles = listOf(
            "Capteur" vaut "Plein format 8,6K",
            "Stabilisation" vaut "Capteur stabilisé",
            "Filtres" vaut "ND variables intégrés",
            "ISO de base" vaut "800 et 3200"
        ),
        histoire = "Sortie en 2023, elle reprend le capteur 8,6K de la VENICE 2 dans un boîtier plus léger. C'est l'une des premières caméras de cinéma à stabiliser son capteur, ce qui la destine aux tournages sans machinerie lourde.",
        ficheTechnique = "Capteur plein format 8,6K, double sensibilité de base 800 et 3200 ISO. Stabilisation mécanique du capteur, filtres neutres variables électroniques. X-OCN et XAVC sur cartes CFexpress. Montures PL et E.",
        commentLUtiliser = "On la prend à la main ou à l'épaule pour le documentaire ou la fiction légère : la stabilisation absorbe les petits tremblements, les ND variables suivent la lumière sans couper. Ses fichiers se raccordent directement à ceux d'une VENICE 2 sur le même tournage.",
        atouts = "L'image d'une caméra de long-métrage avec l'autonomie d'une caméra documentaire.",
        limites = "Stabilisation utile mais pas miraculeuse aux longues focales ; fichiers 8,6K lourds pour une petite structure.",
        films = listOf("The Knowing (court métrage)" par "Emmanuel Lubezki"),
        aRetenir = "Emmanuel Lubezki, triple oscarisé, a tourné avec elle le court métrage The Knowing en Australie occidentale, dès sa sortie."
    ),
    Camera(
        nom = "FX9, FX6 et FX3",
        fabricant = "Sony",
        annee = 2019,
        categorie = "Numérique plein format, gamme Cinema Line",
        silhouette = Silhouette.COMPACTE,
        punchline = "La gamme cinéma accessible de Sony, dont la plus petite a tourné un film de science-fiction à grand spectacle.",
        chiffresCles = listOf(
            "Capteur" vaut "Plein format",
            "ISO de base" vaut "Doubles (800 et 12 800 sur la FX6)",
            "Filtres" vaut "ND variables (FX9, FX6)",
            "Poids" vaut "FX3 : environ 700 g"
        ),
        histoire = "Lancée avec la FX9 en 2019, la gamme Cinema Line décline la science couleur de la VENICE dans des corps abordables : la FX9 pour le documentaire à l'épaule, la FX6 pour le tournage mobile, la FX3 dans un boîtier d'appareil photo.",
        ficheTechnique = "Capteurs plein format, 6K sur la FX9, 4K sur la FX6 et la FX3, doubles sensibilités de base très élevées. Filtres neutres électroniques variables sur la FX9 et la FX6. XAVC interne sur cartes, RAW par sortie externe. Monture E.",
        commentLUtiliser = "La FX9 s'épaule pour le reportage, la FX6 se tient à la main par sa poignée, la FX3 se glisse dans une cage ou sur un stabilisateur. Leur très haute sensibilité permet de tourner en lumière existante, à condition de soigner l'exposition du profil log.",
        atouts = "Rapport qualité-prix remarquable, autofocus fiable, très grande sensibilité, image raccord avec une VENICE.",
        limites = "Codecs compressés en interne, ergonomie limitée pour la FX3, construction moins robuste qu'une caméra de plateau.",
        films = listOf(
            "The Creator" par "Greig Fraser, Oren Soffer",
            "Searching for Amani (documentaire)" par "Nicole Gormley"
        ),
        aRetenir = "The Creator, en 2023, a été tourné en grande partie avec la plus petite de la gamme, un boîtier d'appareil photo, pour alléger l'équipe et tourner en décors réels."
    )
)

private fun canon(): List<Camera> = listOf(
    Camera(
        nom = "EOS 5D Mark II",
        fabricant = "Canon",
        annee = 2008,
        categorie = "Appareil photo reflex plein format",
        silhouette = Silhouette.REFLEX,
        punchline = "L'appareil photo qui a déclenché la révolution du tournage au reflex.",
        chiffresCles = listOf(
            "Capteur" vaut "Plein format 36 × 24 mm",
            "Vidéo" vaut "1080p, 24p ajouté en 2010",
            "Codec" vaut "H.264 compressé",
            "Monture" vaut "EF"
        ),
        histoire = "Sorti en 2008, c'est un appareil photo de 21 millions de pixels qui filme aussi en haute définition. Pour la première fois, un grand capteur plein format et ses optiques photo devenaient accessibles à la vidéo pour quelques milliers d'euros : clips, courts-métrages puis séries s'en emparent.",
        ficheTechnique = "Capteur plein format de 21 millions de pixels, vidéo 1080p en H.264 sur cartes CompactFlash ; la cadence de 24 images par seconde n'arrive qu'avec une mise à jour en 2010. Monture EF, sans filtres neutres ni vraie gestion du son.",
        commentLUtiliser = "On l'équipe d'une loupe sur l'écran, d'un enregistreur son séparé et de filtres neutres devant l'objectif pour ouvrir le diaphragme en plein jour. On règle tout en manuel et l'on évite les panoramiques rapides, qui révèlent l'obturateur roulant.",
        atouts = "Faible profondeur de champ du plein format, sensibilité excellente pour l'époque, discrétion et coût dérisoire.",
        limites = "Compression lourde, moiré, obturateur roulant, surchauffe, son médiocre : un outil détourné, pas une caméra.",
        films = listOf(
            "Dr House (saison 6, épisode Help Me)" par "Gale Tattersall",
            "Act of Valor" par "Shane Hurlbut"
        ),
        aRetenir = "Le dernier épisode de la saison 6 de Dr House a été tourné entièrement avec lui : une série de grande audience, filmée avec un appareil photo."
    ),
    Camera(
        nom = "EOS C300",
        fabricant = "Canon",
        annee = 2011,
        categorie = "Numérique Super 35, documentaire",
        silhouette = Silhouette.COMPACTE,
        punchline = "La caméra qui a fait passer le documentaire au grand capteur.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35",
            "Sortie" vaut "1080p",
            "Codec" vaut "50 Mb/s 4:2:2",
            "Courbe" vaut "Canon Log"
        ),
        histoire = "Présentée fin 2011, c'est la première caméra de la gamme Cinema EOS. Elle garde les optiques photo EF et la faible profondeur de champ que le reflex avait popularisées, mais dans un vrai corps de caméra, avec un codec robuste et un son professionnel.",
        ficheTechnique = "Capteur Super 35 d'environ 8 millions de pixels, sortie 1080p très propre, courbe Canon Log. Codec MPEG-2 4:2:2 à 50 Mb/s sur cartes CompactFlash, accepté par les diffuseurs. Filtres neutres intégrés. Monture EF ou PL.",
        commentLUtiliser = "Poignée et écran modulaires, entrées son XLR : on tourne seul, image et son. Le Canon Log se travaille en post-production avec une LUT de rendu. Idéale pour de longues journées de tournage documentaire.",
        atouts = "Robustesse, fichiers sobres, carnations flatteuses, autonomie.",
        limites = "Haute définition seulement, cadences limitées, aucune sortie RAW.",
        films = listOf(
            "Blue Ruin" par "Jeremy Saulnier",
            "Cartel Land (documentaire)" par "Matthew Heineman",
            "Icarus (documentaire)" par "Bryan Fogel"
        ),
        aRetenir = "Icarus, en partie tourné avec elle, a remporté l'Oscar du meilleur documentaire en 2018."
    ),
    Camera(
        nom = "EOS C300 Mark II et Mark III",
        fabricant = "Canon",
        annee = 2015,
        categorie = "Numérique Super 35, documentaire",
        silhouette = Silhouette.COMPACTE,
        punchline = "Le 4K et l'autofocus de cinéma, jusqu'au capteur à double gain.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 4K",
            "Mise au point" vaut "Autofocus Dual Pixel",
            "Capteur de la Mark III" vaut "DGO, à double gain",
            "Cadence" vaut "jusqu'à 120 im/s (Mark III)"
        ),
        histoire = "La Mark II, en 2015, ajoute le 4K interne et un autofocus à détection de phase sur le capteur, précieux pour l'opérateur seul. La Mark III, en 2020, adopte un capteur qui lit chaque image avec deux gains pour étendre la dynamique.",
        ficheTechnique = "Capteur Super 35 4K, XF-AVC interne puis Cinema RAW Light sur la Mark III. Autofocus Dual Pixel, filtres neutres intégrés. Capteur à double gain sur la Mark III, annoncé à plus de 16 diaphs, 4K jusqu'à 120 images par seconde. Monture EF ou PL.",
        commentLUtiliser = "À la main, à l'épaule ou sur trépied, souvent seul. L'autofocus sert de filet de sécurité pour les sujets imprévisibles, ce qui compte dans le documentaire d'aventure.",
        atouts = "Autofocus fiable, large dynamique sur la Mark III, fichiers maîtrisés, ergonomie de reportage.",
        limites = "Pas de plein format, image moins recherchée en fiction haut de gamme que les références ARRI ou Sony.",
        films = listOf("Free Solo (documentaire)" par "Jimmy Chin, Elizabeth Chai Vasarhelyi"),
        aRetenir = "Free Solo, l'ascension sans corde d'El Capitan, a été tourné avec la Mark II et a remporté l'Oscar du meilleur documentaire en 2019."
    )
)

private fun blackmagic(): List<Camera> = listOf(
    Camera(
        nom = "Pocket 4K et 6K",
        fabricant = "Blackmagic",
        annee = 2018,
        categorie = "Numérique Micro 4/3 et Super 35, abordable",
        silhouette = Silhouette.REFLEX,
        punchline = "Le RAW de cinéma pour le prix d'un appareil photo.",
        chiffresCles = listOf(
            "Capteur" vaut "Micro 4/3 (4K) · Super 35 (6K)",
            "Codec" vaut "Blackmagic RAW",
            "ISO de base" vaut "400 et 3200 (4K)",
            "Prix à la sortie" vaut "environ 1 300 dollars (4K)"
        ),
        histoire = "Sortie en 2018, la version 4K met un enregistrement RAW et une vraie courbe de cinéma dans un boîtier en forme d'appareil photo. La 6K, en 2019, passe au capteur Super 35 et à la monture EF. Elle a été la première caméra de nombreux jeunes réalisateurs.",
        ficheTechnique = "Capteur Micro 4/3 (4K) ou Super 35 6K (6K), Blackmagic RAW ou ProRes sur cartes CFast, SD ou disque USB-C. Double sensibilité de base. Monture Micro 4/3 (4K) ou EF (6K). Grand écran arrière, sans filtres neutres sur les premières versions.",
        commentLUtiliser = "On l'installe dans une cage avec une poignée, une batterie externe et un écran ou un viseur : son autonomie d'origine est courte. Le RAW se développe dans DaVinci Resolve, le logiciel d'étalonnage de la même marque, fourni avec la caméra.",
        atouts = "Image RAW et couleur de cinéma pour un prix d'appareil photo, logiciel d'étalonnage professionnel inclus.",
        limites = "Autonomie faible, écran fixe, autofocus médiocre, ergonomie qui impose des accessoires.",
        films = listOf("Three Headed Beast (Tribeca 2022)" par ""),
        aRetenir = "Elle est vendue avec la version complète de DaVinci Resolve, l'un des logiciels d'étalonnage les plus utilisés au cinéma : le fabricant gagne sur tout l'écosystème, pas seulement sur le boîtier."
    ),
    Camera(
        nom = "URSA Mini Pro 12K",
        fabricant = "Blackmagic",
        annee = 2020,
        categorie = "Numérique Super 35, très haute résolution",
        silhouette = Silhouette.EPAULE,
        punchline = "Du 12K sur un capteur Super 35, pour le prix d'une caméra de milieu de gamme.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 12 288 × 6480",
            "Matrice" vaut "RGBW",
            "Codec" vaut "Blackmagic RAW",
            "Cadence" vaut "12K jusqu'à 60 im/s"
        ),
        histoire = "Présentée en 2020, elle surprend par une résolution de 12K sur un capteur Super 35, obtenue grâce à une matrice qui ajoute des photosites blancs aux photosites colorés. L'idée : suréchantillonner pour une 4K ou une 8K très propre et garder une marge de recadrage.",
        ficheTechnique = "Capteur Super 35 de 12 288 × 6480 photosites, matrice RGBW, Blackmagic RAW jusqu'à 12K à 60 images par seconde, davantage en format réduit. Filtres neutres intégrés, monture PL interchangeable. Corps d'épaule.",
        commentLUtiliser = "Épaulée ou sur pied comme une caméra de tournage classique. On ne livre presque jamais en 12K : on tourne en 12K ou en 8K pour extraire une 4K détaillée, stabiliser ou recadrer.",
        atouts = "Résolution extrême à prix bas, filtres neutres intégrés, RAW efficace et bien intégré à Resolve.",
        limites = "Sensibilité et bruit en basse lumière en retrait des références, fichiers lourds en pleine définition.",
        films = listOf(
            "Les Banshees d'Inisherin (en complément)" par "Ben Davis",
            "The Desperate Hour" par "John Brawley"
        ),
        aRetenir = "Ses 12K ne sont pas faits pour être projetés tels quels : ils servent à suréchantillonner, comme on tirait autrefois un grand négatif pour obtenir un petit tirage plus fin."
    )
)

private fun panavision(): List<Camera> = listOf(
    Camera(
        nom = "Panaflex",
        fabricant = "Panavision",
        annee = 1972,
        categorie = "Argentique 35 mm, studio et épaule",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La première 35 mm silencieuse assez légère pour quitter le pied.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Son" vaut "Silencieuse",
            "Usage" vaut "Studio et épaule",
            "Disponibilité" vaut "Location Panavision"
        ),
        histoire = "Introduite en 1972, elle réunit ce que les caméras de studio séparaient : le silence pour le son direct et la légèreté pour l'épaule. Panavision ne vend pas ses caméras, elle les loue — le modèle qui a fait sa puissance à Hollywood. Les versions Gold et Platinum ont suivi.",
        ficheTechnique = "Pellicule 35 mm, fonctionnement silencieux sans caisson, magasins sur le dessus ou à l'arrière. Visée reflex. Monture Panavision, pensée pour les optiques sphériques et anamorphiques de la maison.",
        commentLUtiliser = "Elle passe du pied à l'épaule en changeant quelques accessoires. On la prend avec le parc d'optiques Panavision et le service de la maison, qui entretient et prépare chaque caméra pour le tournage.",
        atouts = "Silence, mobilité, écosystème d'optiques unique et service de location réputé.",
        limites = "Liée au parc Panavision : on ne l'achète pas, on dépend du loueur.",
        films = listOf("Les Dents de la mer" par "Bill Butler"),
        aRetenir = "Les Dents de la mer fut l'un des premiers films à profiter de sa mobilité : une caméra silencieuse assez légère pour tourner en mer, sur un bateau qui tangue."
    ),
    Camera(
        nom = "Genesis",
        fabricant = "Panavision",
        annee = 2004,
        categorie = "Numérique Super 35, pionnière",
        silhouette = Silhouette.STUDIO,
        punchline = "L'une des premières caméras numériques au format Super 35, pour garder les optiques de cinéma.",
        chiffresCles = listOf(
            "Capteur" vaut "CCD Super 35",
            "Sortie" vaut "1920 × 1080",
            "Enregistrement" vaut "HDCAM SR",
            "Monture" vaut "Panavision"
        ),
        histoire = "Développée avec Sony et présentée en 2004, elle adopte un capteur de la taille d'une image 35 mm : pour la première fois, les optiques de cinéma gardent en numérique leur angle de champ et leur profondeur de champ habituels.",
        ficheTechnique = "Capteur CCD au format Super 35 d'environ 12 millions de photosites, sortie HD 1920 × 1080. Enregistrement sur magnétoscope HDCAM SR accolé ou déporté. Monture Panavision : tout le parc d'optiques 35 mm de la maison s'y monte.",
        commentLUtiliser = "Caméra de plateau reliée à un enregistreur, avec un moniteur étalonné pour juger l'image en direct — une nouveauté pour des chefs opérateurs habitués aux rushes du lendemain.",
        atouts = "Compatibilité complète avec les optiques 35 mm, profondeur de champ de cinéma, image HD propre pour l'époque.",
        limites = "Résolution HD, dynamique limitée, chaîne d'enregistrement lourde : vite dépassée par les capteurs CMOS.",
        films = listOf(
            "Superman Returns" par "Newton Thomas Sigel",
            "Apocalypto" par "Dean Semler"
        ),
        aRetenir = "Son capteur de la taille d'un photogramme 35 mm est la vraie rupture : avant elle, les caméras HD à petit capteur donnaient une profondeur de champ de télévision."
    ),
    Camera(
        nom = "Millennium XL2",
        fabricant = "Panavision",
        annee = 2004,
        categorie = "Argentique 35 mm, légère",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La 35 mm légère que les défenseurs de la pellicule louent encore.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Configurations" vaut "Épaule, Steadicam, studio",
            "Son" vaut "Silencieuse",
            "Disponibilité" vaut "Location Panavision"
        ),
        histoire = "Sortie en 2004, elle affine la Millennium XL : plus rapide, meilleur retour vidéo, ergonomie revue. Elle est restée la 35 mm de référence de Panavision, choisie par ceux qui continuent de tourner en pellicule à l'ère numérique.",
        ficheTechnique = "Pellicule 35 mm, fonctionnement silencieux, corps compact et équilibré qui passe en quelques instants de l'épaule à la Steadicam et au studio. Monture Panavision pour les optiques sphériques et anamorphiques de la maison.",
        commentLUtiliser = "On la reconfigure selon la séquence sans changer de caméra. Comme toute pellicule, on travaille à la cellule, on connaît son négatif et l'on attend les rushes.",
        atouts = "Légèreté pour du 35 mm, ergonomie saluée, polyvalence de configuration.",
        limites = "Coût de la pellicule et du laboratoire ; parc limité à la location Panavision.",
        films = listOf(
            "Once Upon a Time… in Hollywood" par "Robert Richardson",
            "The Lighthouse" par "Jarin Blaschke"
        ),
        aRetenir = "Robert Richardson l'a décrite comme « la 35 mm la plus équilibrée » pour Once Upon a Time… in Hollywood, tourné entièrement en pellicule."
    ),
    Camera(
        nom = "Millennium DXL2",
        fabricant = "Panavision",
        annee = 2018,
        categorie = "Numérique grand format, studio",
        silhouette = Silhouette.STUDIO,
        punchline = "Un capteur RED 8K dans une caméra Panavision, avec la couleur de Light Iron.",
        chiffresCles = listOf(
            "Capteur" vaut "RED Monstro 8K VV",
            "Couleur" vaut "Light Iron",
            "Monture" vaut "Panavision grand format",
            "Disponibilité" vaut "Location Panavision"
        ),
        histoire = "La DXL de 2016 puis la DXL2 de 2018 associent trois maisons : le capteur de RED, la science couleur de Light Iron, laboratoire de post-production de Panavision, et le corps, les optiques et le service de Panavision.",
        ficheTechnique = "Capteur 8K VV d'origine RED, enregistrement RAW, traitement couleur Light Iron. Monture Panavision grand format pour les optiques Primo 70 et les autres séries de la maison. Location uniquement.",
        commentLUtiliser = "Choisie avec un parc d'optiques Panavision grand format ; on l'étalonne avec les courbes Light Iron plutôt qu'avec la couleur RED d'origine.",
        atouts = "Résolution 8K grand format, couleur retravaillée pour les peaux, intégration complète au parc Panavision.",
        limites = "Location exclusive, dépendance au loueur, poids d'une caméra de plateau.",
        films = listOf(
            "The Dry" par "Stefan Duscio",
            "American Horror Story, saison 9" par "Gavin Kelly"
        ),
        aRetenir = "Sous le capot bat un capteur RED : la même puce que des caméras concurrentes, mais une autre couleur, parce qu'une image dépend autant de son traitement que de son capteur."
    )
)

private fun autresNumeriques(): List<Camera> = listOf(
    Camera(
        nom = "VariCam 35 et LT",
        fabricant = "Panasonic",
        annee = 2014,
        categorie = "Numérique Super 35",
        silhouette = Silhouette.EPAULE,
        punchline = "Celle qui a popularisé la double sensibilité native.",
        chiffresCles = listOf(
            "Capteur" vaut "Super 35 · 4K",
            "ISO natifs" vaut "800 et 5000",
            "Version allégée" vaut "LT (2016)",
            "Monture" vaut "PL ou EF"
        ),
        histoire = "Lancée en 2014, la VariCam 35 met en avant une idée reprise ensuite partout : deux sensibilités natives sur le même capteur, 800 et 5000 ISO, pour tourner la nuit sans monter le bruit. La LT de 2016 garde le capteur dans un corps plus léger.",
        ficheTechnique = "Capteur Super 35 4K, deux sensibilités natives de 800 et 5000 ISO. Enregistrement AVC-Intra interne, RAW par enregistreur accolé. Monture PL, EF sur la LT. Corps d'épaule.",
        commentLUtiliser = "On bascule sur 5000 ISO pour les nuits et les intérieurs peu éclairés : la lumière existante suffit souvent. En plein jour, on revient à 800 avec des filtres neutres.",
        atouts = "Basses lumières propres, couleur appréciée en série télévisée, image stable sur de longues journées.",
        limites = "Marque restée minoritaire au cinéma, gamme moins suivie ces dernières années.",
        films = listOf(
            "Black Christmas (2019)" par "Sophia Takal (réal.)",
            "Cheer (série documentaire)" par "Greg Whiteley (réal.)"
        ),
        aRetenir = "Deux sensibilités « natives » signifient deux circuits de lecture du capteur, et non une simple amplification : c'est pour cela que la sensibilité haute reste propre."
    ),
    Camera(
        nom = "E2",
        fabricant = "Z CAM",
        annee = 2018,
        categorie = "Numérique compacte, du Micro 4/3 au plein format",
        silhouette = Silhouette.COMPACTE,
        punchline = "Un petit cube chinois devenu caméra d'action de blockbuster.",
        chiffresCles = listOf(
            "Capteur" vaut "Micro 4/3, puis Super 35 et plein format",
            "Cadence" vaut "4K jusqu'à 120 im/s",
            "Codec" vaut "ProRes, H.265",
            "Format" vaut "Cube de quelques centaines de grammes"
        ),
        histoire = "Lancée en 2018 par une jeune marque de Shenzhen, elle met une 4K à haute cadence dans un cube minuscule. Déclinée ensuite en versions Super 35 et plein format 6K, elle séduit les tournages qui ont besoin de caméras discrètes, nombreuses ou sacrifiables.",
        ficheTechnique = "Capteur Micro 4/3 sur le modèle d'origine, Super 35 ou plein format 6K sur les déclinaisons S6 et F6. ProRes ou H.265 sur carte, pilotage par application ou réseau. Monture Micro 4/3 ou EF selon la version. Très légère.",
        commentLUtiliser = "On l'installe en multicaméra, sur un véhicule ou en caméra embarquée, et on la pilote à distance. Ses fichiers ProRes s'intègrent directement au montage.",
        atouts = "Taille, prix, haute cadence et pilotage réseau pour les dispositifs multicaméras.",
        limites = "Dynamique et rendu en retrait des caméras de plateau, ergonomie dépendante des accessoires.",
        films = listOf("Mission: Impossible – Dead Reckoning (caméras d'action)" par "Fraser Taggart"),
        aRetenir = "Elle a servi de caméra d'action sur les derniers Mission: Impossible, là où une caméra de plateau n'aurait pas trouvé sa place."
    ),
    Camera(
        nom = "Ronin 4D",
        fabricant = "DJI",
        annee = 2021,
        categorie = "Numérique plein format stabilisée",
        silhouette = Silhouette.STABILISEE,
        punchline = "Une caméra plein format intégrée à sa propre nacelle stabilisée sur quatre axes.",
        chiffresCles = listOf(
            "Capteur" vaut "Plein format 6K ou 8K",
            "Stabilisation" vaut "4 axes",
            "Mise au point" vaut "Télémètre LiDAR",
            "Codec" vaut "ProRes RAW interne"
        ),
        histoire = "Présentée en 2021 par le fabricant de drones DJI, elle fusionne caméra et stabilisateur : le quatrième axe compense les secousses verticales de la marche. Son télémètre LiDAR assiste la mise au point, y compris avec des optiques manuelles.",
        ficheTechnique = "Tête caméra plein format 6K ou 8K, double sensibilité de base, filtres neutres intégrés. Stabilisation mécanique sur quatre axes, télémètre LiDAR pour l'autofocus et le suivi. ProRes RAW interne. Monture propriétaire adaptable (E, L, M).",
        commentLUtiliser = "Un seul opérateur fait un plan de Steadicam : il marche, court, monte des escaliers, pendant qu'un assistant peut faire le point à distance. On l'utilise au plus près des acteurs, en décor réel, sans machinerie.",
        atouts = "Mouvements fluides immédiats, autofocus LiDAR, caméra et stabilisateur réunis en un seul outil.",
        limites = "Lourde à bout de bras sur la durée, écosystème fermé, image un cran en dessous des caméras de plateau haut de gamme.",
        films = listOf("Civil War" par "Rob Hardy"),
        aRetenir = "Civil War, d'Alex Garland, a été tourné en grande partie avec elle : sa légèreté et sa stabilisation permettaient de suivre les personnages comme un reporter de guerre."
    ),
    Camera(
        nom = "SI-2K",
        fabricant = "Silicon Imaging",
        annee = 2007,
        categorie = "Numérique 2/3 de pouce, tête déportée",
        silhouette = Silhouette.COMPACTE,
        punchline = "La petite caméra 2K reliée à un ordinateur qui a gagné l'Oscar de la photographie.",
        chiffresCles = listOf(
            "Capteur" vaut "2/3 de pouce",
            "Résolution" vaut "2K",
            "Enregistrement" vaut "RAW sur ordinateur",
            "Tête" vaut "Déportable, grosse comme un poing"
        ),
        histoire = "Disponible à partir de 2007, elle sépare une minuscule tête caméra de son unité d'enregistrement informatique. Danny Boyle et Anthony Dod Mantle en ont fait l'outil de Slumdog Millionaire, en portant l'ordinateur dans un sac à dos pour courir dans les rues de Mumbai.",
        ficheTechnique = "Capteur de 2/3 de pouce, résolution 2K, enregistrement RAW compressé sur un ordinateur relié par câble. Tête déportable équipée d'optiques de cinéma ou photo.",
        commentLUtiliser = "Tête à la main, ordinateur dans un sac, un câble entre les deux : on tourne comme avec une caméra amateur, pour une image destinée au cinéma. La latitude est faible, il faut exposer juste.",
        atouts = "Discrétion et mobilité extrêmes pour une image 2K RAW, à une époque où les caméras de cinéma pesaient des kilos.",
        limites = "Petit capteur, dynamique étroite, dépendance à un ordinateur et à ses câbles.",
        films = listOf(
            "Slumdog Millionaire" par "Anthony Dod Mantle",
            "127 heures" par "Anthony Dod Mantle, Enrique Chediak"
        ),
        aRetenir = "Slumdog Millionaire est le premier film tourné principalement en numérique à remporter l'Oscar de la meilleure photographie, en 2009."
    ),
    Camera(
        nom = "Phantom",
        fabricant = "Vision Research",
        annee = 2007,
        categorie = "Numérique haute vitesse",
        silhouette = Silhouette.STUDIO,
        punchline = "Des milliers d'images par seconde : la caméra des super-ralentis.",
        chiffresCles = listOf(
            "Cadence" vaut "jusqu'à 2570 im/s en 1080p (Flex)",
            "En 4K" vaut "jusqu'à 1000 im/s (Flex4K)",
            "Mémoire" vaut "Quelques secondes en mémoire vive",
            "Usage" vaut "Inserts, effets, sport"
        ),
        histoire = "Fabricant de caméras scientifiques, Vision Research adapte ses caméras au cinéma vers 2007. La version HD, puis la Flex en 2010 et la Flex4K ensuite, rendent possibles des ralentis extrêmes en qualité de cinéma : gouttes, explosions, impacts.",
        ficheTechnique = "Capteur haute vitesse, jusqu'à environ 2570 images par seconde en 1080p sur la Flex et 1000 images par seconde en 4K sur la Flex4K. Les images s'enregistrent d'abord en mémoire vive, quelques secondes seulement, puis se déchargent vers des magasins. Monture PL et autres.",
        commentLUtiliser = "On travaille en déclenchement après coup : la caméra enregistre en boucle et l'on valide quand l'action a eu lieu. Il faut énormément de lumière, le temps de pose étant minuscule, et se méfier des sources qui scintillent au rythme du secteur.",
        atouts = "Des ralentis qu'aucune autre caméra de cinéma ne permet, avec une image professionnelle.",
        limites = "Enregistrements très courts, besoin de lumière énorme, déchargement lent, location coûteuse.",
        films = listOf(
            "Sherlock Holmes" par "Philippe Rousselot",
            "Sherlock Holmes : Jeu d'ombres" par "Philippe Rousselot"
        ),
        aRetenir = "À 2500 images par seconde, une seconde d'action dure plus d'une minute et demie à l'écran."
    )
)

private fun histoirePellicule(): List<Camera> = listOf(
    Camera(
        nom = "trois bandes",
        fabricant = "Technicolor",
        annee = 1932,
        categorie = "Argentique 35 mm couleur, trois négatifs",
        silhouette = Silhouette.CAISSON,
        punchline = "La caméra qui a inventé la couleur hollywoodienne en impressionnant trois négatifs à la fois.",
        chiffresCles = listOf(
            "Négatifs" vaut "3 à la fois",
            "Séparation" vaut "Prisme diviseur",
            "Lumière" vaut "Énormément",
            "Période" vaut "Années 1930 à 1950"
        ),
        histoire = "Mise au point au début des années 1930, elle sépare la lumière par un prisme et l'enregistre sur trois négatifs noir et blanc, un par couleur primaire. Les trois images sont ensuite recombinées au tirage par des colorants : c'est la couleur saturée et stable de l'âge d'or hollywoodien.",
        ficheTechnique = "Un prisme derrière l'objectif renvoie la lumière vers trois pellicules noir et blanc placées derrière des filtres colorés. Le mécanisme, énorme, se loge dans un caisson insonorisé pour le son direct. Très faible sensibilité : les plateaux devaient être éclairés intensément.",
        commentLUtiliser = "Elle ne se louait qu'avec un technicien et un conseiller couleur de la société, qui supervisaient décors, costumes et éclairage. On éclairait abondamment et l'on concevait les couleurs dès la préparation.",
        atouts = "Des couleurs d'une saturation et d'une stabilité exceptionnelles, qui ne se sont pas décolorées avec le temps.",
        limites = "Poids et encombrement considérables, besoin de lumière intense, coût et dépendance complète à une seule société.",
        films = listOf(
            "Becky Sharp" par "Ray Rennahan",
            "Le Magicien d'Oz" par "Harold Rosson",
            "Autant en emporte le vent" par "Ernest Haller"
        ),
        aRetenir = "Becky Sharp, en 1935, est le premier long-métrage entièrement tourné selon ce procédé de couleur par trois négatifs."
    ),
    Camera(
        nom = "BNC",
        fabricant = "Mitchell",
        annee = 1935,
        anneeApprox = true,
        categorie = "Argentique 35 mm, studio",
        silhouette = Silhouette.CAISSON,
        punchline = "La caméra de studio de l'âge d'or d'Hollywood, silencieuse par construction.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Son" vaut "Silencieuse d'origine",
            "Visée" vaut "Par décalage du corps",
            "Usage" vaut "Pied, Dolly, grue"
        ),
        histoire = "Apparue au milieu des années 1930, quand le parlant imposait des caméras silencieuses, elle intègre son insonorisation au lieu de s'enfermer dans un caisson séparé. Elle a été la caméra standard des studios hollywoodiens pendant des décennies.",
        ficheTechnique = "Pellicule 35 mm, mécanisme très précis et silencieux, magasins sur le dessus. Pas de visée reflex : pour cadrer, on fait glisser le corps sur le côté afin de regarder à travers l'objectif, puis on le replace avant de tourner. Très lourde.",
        commentLUtiliser = "Exclusivement sur pied, Dolly ou grue. Le cadre se règle pendant la mise en place ; pendant la prise, l'opérateur suit l'action dans un viseur latéral, légèrement décalé de l'objectif.",
        atouts = "Une stabilité de défilement légendaire, le silence pour le son direct, une robustesse telle que des exemplaires tournent encore.",
        limites = "Poids énorme, aucune mobilité, pas de visée à travers l'objectif pendant la prise.",
        films = listOf("Citizen Kane" par "Gregg Toland"),
        aRetenir = "L'exemplaire numéro 2, celui de Gregg Toland pour Citizen Kane et ses profondeurs de champ légendaires, est exposé au siège de l'American Society of Cinematographers, à Hollywood."
    ),
    Camera(
        nom = "H16",
        fabricant = "Bolex",
        annee = 1935,
        categorie = "Argentique 16 mm, mécanique",
        silhouette = Silhouette.TOURELLE,
        punchline = "La 16 mm à ressort, sans batterie, des écoles de cinéma et de l'expérimental.",
        chiffresCles = listOf(
            "Format" vaut "16 mm",
            "Moteur" vaut "Ressort à remonter",
            "Objectifs" vaut "Tourelle de trois",
            "Bobine" vaut "30 m, environ 2 min 45 s"
        ),
        histoire = "Fabriquée en Suisse à partir de 1935, elle fonctionne sans électricité, grâce à un moteur à ressort que l'on remonte à la manivelle. Solide, simple et bon marché, elle a formé des générations d'étudiants et d'artistes et reste utilisée dans le cinéma expérimental.",
        ficheTechnique = "Pellicule 16 mm en bobines de 30 mètres chargées en plein jour, environ 2 minutes 45 à 24 images par seconde. Moteur à ressort : chaque remontage donne quelques dizaines de secondes de prise. Tourelle de trois objectifs, visée reflex sur les versions Reflex. Rembobinage possible pour les surimpressions.",
        commentLUtiliser = "On tourne des plans courts entre deux remontages. La tourelle change d'objectif d'un geste. On peut tourner image par image, rembobiner pour superposer deux prises, jouer sur la cadence : c'est une caméra d'expérimentation.",
        atouts = "Autonomie totale sans batterie, robustesse, trucages en caméra, prix modeste d'occasion.",
        limites = "Plans courts, bruit qui empêche le son direct, bobines de moins de trois minutes.",
        films = listOf("Meshes of the Afternoon" par "Maya Deren, Alexander Hammid"),
        aRetenir = "Maya Deren a tourné Meshes of the Afternoon en 1943 avec l'une d'elles, pour quelques centaines de dollars : l'un des films fondateurs du cinéma expérimental américain."
    ),
    Camera(
        nom = "Caméflex",
        fabricant = "Éclair",
        annee = 1947,
        categorie = "Argentique 35 mm, portable",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La caméra à l'épaule de la Nouvelle Vague.",
        chiffresCles = listOf(
            "Format" vaut "35 mm",
            "Magasins" vaut "À changement instantané",
            "Portée" vaut "À l'épaule",
            "Son" vaut "Bruyante"
        ),
        histoire = "Conçue par André Coutant et commercialisée en 1947 par la société française Éclair, c'est une 35 mm légère que l'on porte à l'épaule, aux magasins qui se changent en quelques secondes. La Nouvelle Vague s'en empare pour sortir des studios et tourner dans la rue.",
        ficheTechnique = "Pellicule 35 mm, visée reflex, magasins clipsables à changement instantané, construction légère pour l'époque. Mécanisme bruyant : impossible de prendre le son en direct.",
        commentLUtiliser = "On tourne caméra à l'épaule, en lumière naturelle, dans des lieux réels, et l'on postsynchronise tout le son. Les changements de magasin rapides évitent de casser l'élan de la scène.",
        atouts = "Légèreté et rapidité inégalées en 35 mm à sa sortie : la liberté de tourner partout.",
        limites = "Trop bruyante pour le son direct : tout le film doit être doublé.",
        films = listOf("À bout de souffle" par "Raoul Coutard"),
        aRetenir = "Pour À bout de souffle, Raoul Coutard y chargeait de la pellicule photo à 400 ASA raboutée en longues bandes, poussée au développement, pour tourner sans éclairage."
    ),
    Camera(
        nom = "Pro-600",
        fabricant = "Auricon",
        annee = 1950,
        anneeApprox = true,
        categorie = "Argentique 16 mm, son sur la pellicule",
        silhouette = Silhouette.ARGENTIQUE,
        punchline = "La 16 mm qui enregistrait le son sur la pellicule même : la caméra des actualités télévisées.",
        chiffresCles = listOf(
            "Format" vaut "16 mm",
            "Son" vaut "Sur le film (système simple)",
            "Magasin" vaut "180 m (600 pieds)",
            "Usage" vaut "Reportage, télévision"
        ),
        histoire = "Les caméras de cette marque ont été l'outil des journalistes de télévision des années 1950 et 1960 : image et son s'enregistraient sur la même pellicule, sans magnétophone séparé ni synchronisation au montage. Andy Warhol les a adoptées pour ses films parlants.",
        ficheTechnique = "Pellicule 16 mm avec piste sonore optique ou magnétique enregistrée dans la caméra, magasins de 600 pieds, soit plus de seize minutes à 24 images par seconde. Caméra lourde, pour le pied ou l'épaule avec un harnais.",
        commentLUtiliser = "On filme un entretien ou un événement d'une traite : la bobine longue et le son intégré évitent de couper. Au montage, le son étant décalé de quelques images sur la pellicule, les coupes demandent de l'habitude.",
        atouts = "Son synchrone sans matériel supplémentaire, longues prises, simplicité pour une petite équipe.",
        limites = "Qualité de son limitée, montage contraint par le décalage entre image et son, poids.",
        films = listOf("Chelsea Girls" par "Andy Warhol"),
        aRetenir = "Chelsea Girls, projeté sur deux écrans côte à côte, appartient à la période où Warhol tournait avec elle de longues prises continues, son compris."
    ),
    Camera(
        nom = "15/70",
        fabricant = "IMAX",
        annee = 1970,
        categorie = "Argentique 65 mm, quinze perforations",
        silhouette = Silhouette.CAISSON,
        punchline = "Le plus grand négatif du cinéma : du 65 mm qui défile à l'horizontale.",
        chiffresCles = listOf(
            "Négatif" vaut "65 mm, 15 perforations",
            "Image" vaut "environ 70 × 48,5 mm",
            "Magasin" vaut "environ 3 minutes",
            "Son" vaut "Très bruyante"
        ),
        histoire = "Le format naît en 1970 pour l'Exposition universelle d'Osaka. La pellicule 65 mm y défile à l'horizontale, chaque image occupant quinze perforations : un négatif environ dix fois plus grand qu'une image 35 mm classique. Longtemps réservé aux documentaires, il entre dans le long-métrage avec Christopher Nolan.",
        ficheTechnique = "Pellicule 65 mm défilant horizontalement, image d'environ 70 × 48,5 mm. Magasins de quelques minutes seulement. Caméras lourdes et très bruyantes, qui compliquent le son direct. Optiques spécifiques au format.",
        commentLUtiliser = "On la réserve aux séquences qui doivent écraser le spectateur. Chaque magasin dure environ trois minutes : les prises se préparent comme au temps du muet. Le bruit oblige souvent à postsynchroniser.",
        atouts = "Définition et ampleur sans équivalent, présence physique de l'image en projection sur écran géant.",
        limites = "Coût énorme, magasins très courts, bruit, poids, très peu d'exemplaires.",
        films = listOf(
            "The Dark Knight" par "Wally Pfister",
            "Interstellar" par "Hoyte van Hoytema",
            "Dunkerque" par "Hoyte van Hoytema",
            "Oppenheimer" par "Hoyte van Hoytema"
        ),
        aRetenir = "Pour Oppenheimer, Kodak a fabriqué une pellicule noir et blanc à ce format, qui n'existait pas : Nolan voulait ses séquences en noir et blanc aussi grandes que celles en couleur."
    ),
    Camera(
        nom = "XTR",
        fabricant = "Aaton",
        annee = 1980,
        anneeApprox = true,
        categorie = "Argentique 16 mm et Super 16, épaule",
        silhouette = Silhouette.EPAULE,
        punchline = "La caméra française « chat sur l'épaule » du documentaire et du Super 16.",
        chiffresCles = listOf(
            "Format" vaut "16 mm et Super 16",
            "Ergonomie" vaut "Couchée sur l'épaule",
            "Code temporel" vaut "Inscrit sur la pellicule",
            "Son" vaut "Silencieuse"
        ),
        histoire = "La marque est fondée à Grenoble en 1971 par Jean-Pierre Beauviala. Ses caméras 16 mm, dont cette famille, sont dessinées pour se lover sur l'épaule de l'opérateur comme un chat. Elle invente aussi le marquage d'un code temporel sur la pellicule, qui synchronise image et son sans claquette.",
        ficheTechnique = "Pellicule 16 mm ou Super 16, magasins arrière qui se changent très vite, fonctionnement silencieux. Code temporel inscrit en bordure de pellicule. La même maison a conçu l'A-Minima, minuscule Super 16 de 1999, et la Penelope en 35 mm.",
        commentLUtiliser = "On tourne à l'épaule pendant des heures grâce à l'équilibre du corps. Plusieurs caméras peuvent tourner ensemble et se synchroniser grâce au code temporel.",
        atouts = "Ergonomie exceptionnelle, silence, grain du Super 16 qui se gonfle bien en 35 mm, coût de pellicule modéré.",
        limites = "Définition du 16 mm, fabrication arrêtée, entretien confié à des spécialistes de plus en plus rares.",
        films = listOf("Démineurs (The Hurt Locker)" par "Barry Ackroyd"),
        aRetenir = "Démineurs a été tourné en Super 16 avec jusqu'à quatre de ces caméras à la fois, et a remporté l'Oscar du meilleur film en 2010."
    )
)
