package fr.cellule.core

/*
 * Second relevé du catalogue photométrique (17 novembre 2026, recherche
 * web) : mêmes règles que le premier. Une valeur du fabricant, ou relayée
 * telle quelle par un distributeur ou un loueur, est relue ; ce qui se
 * contredit d'une source à l'autre est marqué « à relire ».
 */

private fun mm(spot: Boolean, d: Double, lux: Double) = MesureProjecteur(spot, d, lux)

internal val SECOND_RELEVE: List<Projecteur> = listOf(
    /* LED ponctuelles */
    Projecteur(
        "cat-aputure-storm-1200x", "STORM 1200x · réflecteurs 45° et 15°", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 45.0, angleSpot = 15.0,
        mesures = listOf(mm(false, 3.0, 22500.0), mm(true, 3.0, 96100.0)),
        source = "Aputure, fiche du STORM 1200x à 5600 K, à 3 m : réflecteur 45° 22 500 lux, 30° 41 800, 15° 96 100 ; nu 8 490"
    ),
    Projecteur(
        "cat-aputure-ls-600x-pro-hyper", "LS 600x Pro · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(mm(false, 3.0, 5610.0)),
        source = "Aputure, photométrie du LS 600x Pro à 5600 K (2700 K : 3 220 lux à 3 m)"
    ),
    Projecteur(
        "cat-aputure-ls-600x-pro-f10", "LS 600x Pro · Fresnel F10", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleSpot = 15.0,
        mesures = listOf(mm(true, 3.0, 18510.0)),
        source = "Aputure, LS 600x Pro avec la Fresnel F10 en position serrée (15°), 5600 K"
    ),
    Projecteur(
        "cat-aputure-storm-80c", "STORM 80c · réflecteur", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 35.0,
        mesures = listOf(mm(false, 1.0, 19850.0)),
        source = "Aputure, photométrie du STORM 80c relayée par les distributeurs (3200 K : 19 443 lux à 1 m) ; avec la Fresnel : 36 000 lux à 1 m à 5600 K"
    ),
    Projecteur(
        "cat-aputure-electro-storm-xt26", "Electro Storm XT26 · réflecteur", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(mm(false, 3.0, 65200.0)),
        source = "à relire : 65 200 lux à 3 m à 5600 K chez plusieurs distributeurs, 58 600 chez un autre",
        relu = false
    ),
    Projecteur(
        "cat-nanlite-forza-720b", "Forza 720B · réflecteur 45°", "Nanlite", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 45.0,
        mesures = listOf(mm(false, 3.0, 7919.0)),
        source = "Nanlite, fiche du Forza 720B : 7 919 lux à 3 m à 5600 K avec le réflecteur (3200 K : 6 604)"
    ),
    Projecteur(
        "cat-nanlite-fs-300b", "FS-300B · réflecteur standard", "Nanlite", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(mm(false, 1.0, 38720.0)),
        source = "Nanlite, fiche du FS-300B : 38 720 lux à 1 m à 5600 K avec le réflecteur (3200 K : 38 900 ; nu : 11 130)"
    ),
    Projecteur(
        "cat-nanlux-evoke-1200", "Evoke 1200 · réflecteurs 45° et 26°", "Nanlux", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 45.0, angleSpot = 26.0,
        mesures = listOf(mm(false, 3.0, 18460.0), mm(true, 3.0, 58050.0)),
        source = "Nanlux, Evoke 1200 à 3 m : réflecteur 45° 18 460 lux, 26° 58 050, 60° 12 460 ; tête nue 5 567"
    ),
    Projecteur(
        "cat-godox-mg1200bi-gr15", "KNOWLED MG1200Bi · réflecteur GR15", "Godox", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleSpot = 15.0,
        mesures = listOf(mm(true, 3.0, 149000.0)),
        source = "Godox, catalogue KNOWLED : 149 000 lux à 3 m à 5600 K avec le GR15 (avec le GR45, les distributeurs donnent 21 900 ou 29 100)"
    ),
    Projecteur(
        "cat-prolycht-orion-675", "Orion 675 FS · réflecteurs 55° et 30°", "Prolycht", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 675.0, temperatureK = 5600, angleFlood = 55.0, angleSpot = 30.0,
        mesures = listOf(mm(false, 3.0, 10170.0), mm(true, 3.0, 17210.0)),
        source = "Prolycht, relayé par les distributeurs, à 3 m à 5600 K : réflecteur 55° 10 170 lux, 30° 17 210 ; Fresnel 15° : 28 100 (Prolycht)"
    ),
    Projecteur(
        "cat-zhiyun-molus-x100", "MOLUS X100 · mini-réflecteur", "Zhiyun", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 100.0, temperatureK = 4500,
        mesures = listOf(mm(false, 1.0, 17317.0)),
        source = "à relire : 17 317 lux à 1 m à 4500 K sur la boutique Zhiyun (nu : 3 881), 11 194 dans une FAQ du fabricant",
        relu = false
    ),
    Projecteur(
        "cat-astera-ax9", "AX9 PowerPAR", "Astera", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5500,
        mesures = listOf(mm(false, 1.0, 44355.0)),
        source = "Astera, fiche AX9 PowerPAR : 44 355 lux à 1 m à 5500 K (3200 K : 47 078)"
    ),

    /* Fresnels LED, découpes, petites optiques */
    Projecteur(
        "cat-arri-l7c", "L7-C", "ARRI", FamilleProjecteur.FRESNEL_LED,
        temperatureK = 5600, angleFlood = 50.0, angleSpot = 15.0,
        mesures = listOf(mm(false, 3.0, 688.0), mm(true, 3.0, 5180.0)),
        source = "ARRI, photométrie relayée par B&H (première version du L7-C, 5600 K) ; la version LE2 annonce 25 % de plus"
    ),
    Projecteur(
        "cat-arri-l10c", "L10-C", "ARRI", FamilleProjecteur.FRESNEL_LED,
        temperatureK = 5600, angleFlood = 50.0, angleSpot = 15.0,
        mesures = listOf(mm(false, 5.0, 768.0), mm(true, 5.0, 5552.0)),
        source = "ARRI, fiche L-Series reproduite par un loueur (5600 K, 5 m) ; calculateur ARRI : 1 300 lux à 10 m en serré à 3200 K"
    ),
    Projecteur(
        "cat-etc-source-four-26", "Source Four · lentille 26°", "ETC", FamilleProjecteur.DECOUPE,
        puissanceW = 750.0, angleSpot = 26.0,
        mesures = listOf(mm(true, 10.0, 1763.0)),
        source = "ETC, fiche Source Four : 176 255 cd au centre du faisceau avec la lentille 26° (lampe 750 W), soit 1 763 lux à 10 m"
    ),
    Projecteur(
        "cat-dedolight-dlh4", "DLH4 150 W", "Dedolight", FamilleProjecteur.AUTRE,
        puissanceW = 150.0, temperatureK = 3200, angleFlood = 48.0,
        mesures = listOf(mm(false, 3.0, 445.0), mm(true, 3.0, 9100.0)),
        source = "Panavision (loueur) : 9 100 lux serré, 445 ouvert à 3 m à 3200 K ; B&H : 3 228 lux à 1 m ouvert (48°)"
    ),

    /* Panneaux, mats, fluorescents */
    Projecteur(
        "cat-aputure-nova-ii-2x1", "NOVA II 2x1", "Aputure", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600,
        mesures = listOf(mm(false, 3.0, 28380.0)),
        source = "Aputure, fiche technique de mars 2026 : 28 380 lux à 3 m à 5600 K sans diffuseur (24 700 annoncés au lancement) ; diffuseur plat : 5 130"
    ),
    Projecteur(
        "cat-creamsource-vortex8", "Vortex8 · lentille 20°", "Creamsource", FamilleProjecteur.PANNEAU_LED,
        puissanceW = 650.0, temperatureK = 5600, angleFlood = 110.0, angleSpot = 20.0,
        mesures = listOf(mm(true, 3.0, 13900.0)),
        source = "Creamsource, relayé par Panavision et la presse : 13 900 lux à 3 m à 5600 K avec la lentille 20° (3200 K : 12 700) ; 110° en face ouverte"
    ),
    Projecteur(
        "cat-creamsource-vortex24", "Vortex24", "Creamsource", FamilleProjecteur.PANNEAU_LED,
        mesures = listOf(mm(false, 3.0, 27560.0)),
        source = "à relire : 27 560 lux à 3 m sans diffuseur (fiche Creamsource reproduite par un loueur, température non précisée) ; diffuseur plat : 3 730",
        relu = false
    ),
    Projecteur(
        "cat-litepanels-astra-6x", "Astra 6X Bi-Color", "Litepanels", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600, angleFlood = 46.0,
        mesures = listOf(mm(false, 3.0, 1583.0)),
        source = "Litepanels, fiche Astra 6X : 1 583 lux à 3 m à 5600 K (3200 K : 1 421), faisceau 46°"
    ),
    Projecteur(
        "cat-rotolight-titan-x2", "Titan X2", "Rotolight", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 3200,
        mesures = listOf(mm(false, 3.0, 2220.0)),
        source = "Rotolight, notice (essais de septembre 2020) : 2 220 lux à 3 m à 3200 K, 2 260 à 5600 K ; distributeurs : 2 415 à 5600 K"
    ),
    Projecteur(
        "cat-kinoflo-freestyle-41", "FreeStyle 41 · nid d'abeille 90°", "Kino Flo", FamilleProjecteur.PANNEAU_LED,
        mesures = listOf(mm(false, 1.0, 2450.0)),
        source = "Kino Flo, valeurs relayées par les distributeurs (nid d'abeille 90°) : 2 450 lux à 1 m, 712 à 2 m"
    ),
    Projecteur(
        "cat-kinoflo-diva-lite-41", "Diva-Lite 41 LED", "Kino Flo", FamilleProjecteur.PANNEAU_LED,
        mesures = listOf(mm(false, 1.0, 2450.0)),
        source = "à relire : 2 450 lux à 1 m chez deux distributeurs ; un troisième indique « non publié par le fabricant »",
        relu = false
    ),
    Projecteur(
        "cat-litegear-litemat-spectrum-4", "LiteMat Spectrum 4 (2023)", "LiteGear", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 3200,
        mesures = listOf(mm(false, 3.0, 834.0)),
        source = "Panavision (loueur) : 834 lux à 3 m à 3200 et 5600 K ; distributeur : 7 510 lux à 1 m à 3200 K"
    ),
    Projecteur(
        "cat-kinoflo-4bank-4ft", "4Bank 4 pieds", "Kino Flo", FamilleProjecteur.FLUO,
        mesures = listOf(mm(false, 1.0, 2599.0)),
        source = "Kino Flo, brochure 4Bank : 2 599 lux à 1 m, 775 à 2 m"
    ),

    /* Tubes, barres, petites sources */
    Projecteur(
        "cat-aputure-infinibar-pb12", "INFINIBAR PB12", "Aputure", FamilleProjecteur.TUBE_LED,
        temperatureK = 5600, angleFlood = 120.0,
        mesures = listOf(mm(false, 1.0, 593.0)),
        source = "Aputure, fiche du PB12 : 593 lux à 1 m à 5600 K, faisceau 120°"
    ),
    Projecteur(
        "cat-quasar-rainbow-2-4ft", "Rainbow 2 · 4 pieds", "Quasar Science", FamilleProjecteur.TUBE_LED,
        temperatureK = 5600,
        mesures = listOf(mm(false, 3.0, 92.0)),
        source = "Quasar Science, relayé par B&H : 92 lux à 3 m à 5600 K (828 lux à 1 m, valeur calculée)"
    ),
    Projecteur(
        "cat-nanlite-pavotube-ii-30x", "PavoTube II 30X", "Nanlite", FamilleProjecteur.TUBE_LED,
        temperatureK = 5600,
        mesures = listOf(mm(false, 1.0, 746.0)),
        source = "Nanlite, fiche PavoTube II 30X(R) : 746 lux à 1 m à 5600 K"
    ),
    Projecteur(
        "cat-aputure-mc-pro", "MC Pro", "Aputure", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600,
        mesures = listOf(mm(false, 1.0, 396.0)),
        source = "Aputure, photométrie du MC Pro nu : 396 lux à 1 m et 1 585 à 0,5 m à 5600 K"
    ),
    Projecteur(
        "cat-astera-nyx", "NYX Bulb", "Astera", FamilleProjecteur.AUTRE,
        puissanceW = 10.0, temperatureK = 5500,
        mesures = listOf(mm(false, 1.0, 124.0)),
        source = "à relire : 124 lux à 1 m à 5500 K chez les distributeurs ; un banc d'essai mesure 171 lux à 5600 K",
        relu = false
    ),
    Projecteur(
        "cat-amaran-150c-hyper", "amaran 150c · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 63.0,
        mesures = listOf(mm(false, 1.0, 15610.0)),
        source = "Aputure, photométrie de l'amaran 150c : 15 610 lux à 1 m à 5600 K avec l'Hyper-Reflector (3200 K : 13 680), faisceau ≈ 63°"
    ),

    /* Ambiances */
    Projecteur(
        "cat-mole-spacelite-6k", "Spacelite 6K · jupe de soie", "Mole-Richardson", FamilleProjecteur.AMBIANCE,
        puissanceW = 6000.0, temperatureK = 3200,
        mesures = listOf(mm(false, 4.57, 172.0)),
        source = "à relire : un distributeur annonce 16 fc sur 15 pieds de diamètre à 15 pieds avec jupe et cible de soie blanche",
        relu = false
    ),
    Projecteur(
        "cat-creamsource-spacex", "SpaceX", "Creamsource", FamilleProjecteur.AMBIANCE,
        puissanceW = 1200.0, temperatureK = 3200, angleFlood = 115.0,
        mesures = listOf(mm(false, 5.0, 555.0)),
        source = "à relire : 555 lux à 5 m à 3200 K (115°) chez un distributeur, 591 à 5600 K chez un autre ; une troisième page donne 18 908 lux à 1 m",
        relu = false
    ),
    Projecteur(
        "cat-airstar-sirocco-4000", "Sirocco 4000 W HMI", "Airstar", FamilleProjecteur.AMBIANCE,
        puissanceW = 4000.0, temperatureK = 6000,
        mesures = listOf(mm(false, 15.0, 172.0)),
        source = "Airstar, fiche Sirocco 4000 W HMI : 172 lux sous le ballon placé à 15 m, 380 000 lm, 3 200 m² éclairés à 10 lux au moins"
    ),

    /* HMI */
    Projecteur(
        "cat-arri-m40", "M40", "ARRI", FamilleProjecteur.HMI,
        puissanceW = 4000.0, angleFlood = 52.0, angleSpot = 18.0,
        mesures = listOf(mm(false, 10.0, 2450.0), mm(true, 10.0, 19700.0)),
        source = "ARRI, fiche technique M40 (10 m ; moyen 30° : 6 500 lux)"
    ),
    Projecteur(
        "cat-arri-m90", "M90", "ARRI", FamilleProjecteur.HMI,
        puissanceW = 9000.0, angleFlood = 49.0, angleSpot = 15.0,
        mesures = listOf(mm(false, 10.0, 7590.0), mm(true, 10.0, 51000.0)),
        source = "ARRI, fiche technique M90 avec la lampe 9 000 W (10 m ; moyen 30° : 14 450 lux)"
    ),
    Projecteur(
        "cat-arri-arrimax-18-12", "ARRIMAX 18/12 · 18 kW", "ARRI", FamilleProjecteur.HMI,
        puissanceW = 18000.0, angleFlood = 50.0,
        mesures = listOf(mm(false, 15.0, 5692.0)),
        source = "à relire : 5 692 lux à 15 m en ouvert (50°) chez B&H ; l'ancienne notice ARRI conduit à ≈ 5 980",
        relu = false
    ),
    Projecteur(
        "cat-arri-arrisun-18-spot", "ARRISUN 18 · lentille spot", "ARRI", FamilleProjecteur.HMI,
        puissanceW = 1800.0, angleSpot = 8.0,
        mesures = listOf(mm(true, 40.4, 2000.0)),
        source = "ARRI, fiche AS 18 : 2 000 lux à 40,4 m avec la lentille spot (lampe 1 800 W)"
    ),
    Projecteur(
        "cat-k5600-joker-bug-800", "Joker-Bug 800", "K5600", FamilleProjecteur.HMI,
        puissanceW = 800.0, temperatureK = 5600, angleFlood = 45.0, angleSpot = 5.0,
        mesures = listOf(mm(false, 3.0, 3100.0), mm(true, 3.0, 260000.0)),
        source = "Panavision et B&H : 260 000 lux à 3 m sans lentille (5°), 3 100 avec la Fresnel dépolie, à 5600 K"
    ),

    /* Tungstène */
    Projecteur(
        "cat-arri-t5", "True Blue T5 (5 kW)", "ARRI", FamilleProjecteur.FRESNEL_TUNGSTENE,
        puissanceW = 5000.0, temperatureK = 3200, angleFlood = 62.0, angleSpot = 9.0,
        mesures = listOf(mm(false, 9.0, 1110.0), mm(true, 9.0, 11213.0)),
        source = "ARRI, photométrie True Blue T5 à 9 m"
    ),
    Projecteur(
        "cat-arri-t12", "True Blue T12 (12 kW)", "ARRI", FamilleProjecteur.FRESNEL_TUNGSTENE,
        puissanceW = 12000.0, temperatureK = 3200,
        mesures = listOf(mm(false, 3.0, 35891.0), mm(true, 3.0, 209566.0)),
        source = "Panavision (loueur) : 209 566 lux serré, 35 891 ouvert à 3 m, à 3200 K"
    ),
    Projecteur(
        "cat-arri-arrilite-2000-plus", "Arrilite 2000 Plus", "ARRI", FamilleProjecteur.OUVERT_TUNGSTENE,
        puissanceW = 2000.0, temperatureK = 3200, angleFlood = 63.0, angleSpot = 24.0,
        mesures = listOf(mm(false, 5.0, 940.0), mm(true, 5.0, 4400.0)),
        source = "ARRI, brochure Arrilite Plus (lampe FEX 2 000 W, 5 m)"
    )
)
