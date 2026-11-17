package fr.cellule.core

/*
 * Le catalogue des projecteurs : seulement des valeurs publiées par le
 * fabricant (ou relayées telles quelles par ses distributeurs), mesurées
 * dans l'axe à une distance donnée. Une même tête change de photométrie avec
 * son réflecteur ou sa lentille : chaque configuration est une entrée.
 * « Serré » et « ouvert » désignent la position spot / flood d'une lentille,
 * ou, à défaut, le réflecteur le plus serré et le plus ouvert.
 *
 * Relevé le 17 novembre 2026 par recherche web (fiches et pages de
 * photométrie des fabricants, distributeurs). Ce qui se contredit d'une
 * source à l'autre est marqué « à relire ».
 */

private fun m(spot: Boolean, d: Double, lux: Double) = MesureProjecteur(spot, d, lux)

val PROJECTEURS_CATALOGUE: List<Projecteur> = listOf(
    Projecteur(
        "cat-aputure-600d-pro-hyper", "LS 600d Pro · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 720.0, temperatureK = 5600, angleFlood = 55.0,
        mesures = listOf(m(false, 3.0, 8500.0)),
        source = "Aputure, photométrie du LS 600d Pro (puissance : consommation maximale)"
    ),
    Projecteur(
        "cat-aputure-600d-pro-f10", "LS 600d Pro · Fresnel F10", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 720.0, temperatureK = 5600, angleFlood = 45.0, angleSpot = 15.0,
        mesures = listOf(m(false, 3.0, 11400.0), m(true, 3.0, 29300.0)),
        source = "Aputure, page du Fresnel F10"
    ),
    Projecteur(
        "cat-aputure-1200d-pro-hyper", "LS 1200d Pro · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 55.0, angleSpot = 15.0,
        mesures = listOf(m(false, 3.0, 13010.0), m(true, 3.0, 83100.0)),
        source = "Aputure, photométrie du LS 1200d Pro : ouvert = réflecteur large 55°, serré = réflecteur étroit 15° (moyen 30° : 22 400 lux à 3 m)"
    ),
    Projecteur(
        "cat-aputure-300d-ii-hyper", "LS 300d II · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 45000.0)),
        source = "Aputure, page du LS 300d II (« 45 000+ lux à 1 m »)"
    ),
    Projecteur(
        "cat-amaran-200d-hyper", "amaran 200d · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 200.0, temperatureK = 5600, angleFlood = 55.0,
        mesures = listOf(m(false, 1.0, 65000.0)),
        source = "Fiche amaran 200d (distributeur) : 6 100 lux à 3 m, 2 100 lux à 5 m"
    ),
    Projecteur(
        "cat-amaran-200d-s-hyper", "amaran 200d S · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 26.0,
        mesures = listOf(m(false, 1.0, 55800.0)),
        source = "amaran, photométrie du 200d S"
    ),
    Projecteur(
        "cat-aputure-600c-pro-hyper", "LS 600c Pro · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 3.0, 5020.0)),
        source = "Aputure, photométrie du LS 600c Pro à 5600 K (couleur réglable de 2300 à 10 000 K ; sans accessoire : 1 937 lux à 3 m)"
    ),
    Projecteur(
        "cat-aputure-nova-p600c", "Nova P600c", "Aputure", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600,
        mesures = listOf(m(false, 3.0, 2167.0)),
        source = "Aputure, photométrie du Nova P600c à 5600 K (couleur réglable)"
    ),
    Projecteur(
        "cat-aputure-nova-p300c", "Nova P300c", "Aputure", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 4300, angleFlood = 120.0,
        mesures = listOf(m(false, 3.0, 1000.0)),
        source = "Aputure, photométrie du Nova P300c à 4300 K (pas de valeur publiée à 5600 K) ; faisceau 120° annoncé"
    ),
    Projecteur(
        "cat-amaran-100d-hyper", "amaran 100d · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 100.0, temperatureK = 5600, angleFlood = 55.0,
        mesures = listOf(m(false, 1.0, 39500.0)),
        source = "Fiche amaran 100d (nu : 4 300 lux à 1 m)"
    ),
    Projecteur(
        "cat-amaran-100d-s-hyper", "amaran 100d S · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 34600.0)),
        source = "amaran, photométrie du 100d S"
    ),
    Projecteur(
        "cat-amaran-300c-hyper", "amaran 300c · Hyper-Reflector", "Aputure", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 26580.0)),
        source = "amaran, fiche du 300c à 5600 K (sans réflecteur : 9 370 lux à 1 m ; couleur réglable)"
    ),
    Projecteur(
        "cat-godox-sl150-ii", "SL150W II · réflecteur", "Godox", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 58000.0)),
        source = "à relire : valeur relayée par les distributeurs",
        relu = false
    ),
    Projecteur(
        "cat-godox-vl300-ii", "VL300 II · réflecteur standard", "Godox", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 90000.0)),
        source = "à relire : 90 000 lux à 1 m chez la plupart des distributeurs, 77 000 chez un autre (nu : 11 600)",
        relu = false
    ),
    Projecteur(
        "cat-nanlite-forza-60-ii", "Forza 60 II · réflecteur", "Nanlite", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600,
        mesures = listOf(m(false, 1.0, 17620.0)),
        source = "à relire : valeur annoncée par Nanlite au lancement ; les distributeurs varient selon la génération",
        relu = false
    ),
    Projecteur(
        "cat-nanlite-forza-300-55", "Forza 300 · réflecteur 55°", "Nanlite", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 55.0,
        mesures = listOf(m(false, 1.0, 43060.0)),
        source = "à relire : valeur relayée par les distributeurs",
        relu = false
    ),
    Projecteur(
        "cat-astera-titan", "Titan Tube", "Astera", FamilleProjecteur.TUBE_LED,
        mesures = listOf(m(false, 3.0, 87.0)),
        source = "Astera, photométrie relayée par Panavision (même valeur à 3200 et 5600 K)"
    ),
    Projecteur(
        "cat-astera-helios", "Helios Tube", "Astera", FamilleProjecteur.TUBE_LED,
        temperatureK = 3200,
        mesures = listOf(m(false, 1.0, 360.0)),
        source = "Astera, fiche technique du Helios Tube (393 lux à 4000 K)"
    ),
    Projecteur(
        "cat-litepanels-gemini-2x1", "Gemini 2x1 Soft", "Litepanels", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 6000, angleFlood = 110.0,
        mesures = listOf(m(false, 1.0, 9713.0)),
        source = "Litepanels, photométrie relayée par les distributeurs (à 6000 K) ; panneau proche : la loi en 1/d² surestime au loin"
    ),
    Projecteur(
        "cat-kinoflo-celeb-450", "Celeb 450", "Kino Flo", FamilleProjecteur.PANNEAU_LED,
        mesures = listOf(m(false, 1.0, 4860.0)),
        source = "à relire : valeur relayée par les distributeurs, sans température de couleur indiquée (1 420 lux à 2 m)",
        relu = false
    ),
    Projecteur(
        "cat-nanlite-forza-500", "Forza 500 · réflecteur 55°", "Nanlite", FamilleProjecteur.LED_PONCTUELLE,
        temperatureK = 5600, angleFlood = 55.0,
        mesures = listOf(m(false, 3.0, 6257.0)),
        source = "Nanlite, photométrie relayée par les distributeurs (les valeurs à 1 m divergent)"
    ),
    Projecteur(
        "cat-arri-s60c", "SkyPanel S60-C · diffusion standard", "ARRI", FamilleProjecteur.PANNEAU_LED,
        puissanceW = 400.0, temperatureK = 5600, angleFlood = 115.0,
        mesures = listOf(m(false, 5.0, 416.0)),
        source = "ARRI, photométrie relayée par les distributeurs (400 W nominal)"
    ),
    Projecteur(
        "cat-arri-s30c", "SkyPanel S30-C · diffusion standard", "ARRI", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600,
        mesures = listOf(m(false, 5.0, 222.0)),
        source = "ARRI, photométrie relayée par les distributeurs (≈ 600 lux à 3 m)"
    ),
    Projecteur(
        "cat-arri-s360c", "SkyPanel S360-C · diffusion standard", "ARRI", FamilleProjecteur.PANNEAU_LED,
        temperatureK = 5600,
        mesures = listOf(m(false, 3.0, 4833.0)),
        source = "à relire : les distributeurs donnent de 4 833 à 6 444 lux à 3 m",
        relu = false
    ),
    Projecteur(
        "cat-arri-orbiter-of", "Orbiter · Open Face 30°", "ARRI", FamilleProjecteur.LED_PONCTUELLE,
        puissanceW = 500.0, temperatureK = 5600, angleFlood = 30.0,
        mesures = listOf(m(false, 5.0, 2320.0)),
        source = "ARRI, photométrie relayée par les distributeurs (500 W maximum)"
    ),
    Projecteur(
        "cat-arri-m18", "M18", "ARRI", FamilleProjecteur.HMI,
        puissanceW = 1800.0, angleFlood = 58.0, angleSpot = 15.0,
        mesures = listOf(m(false, 10.0, 2245.0), m(true, 10.0, 11250.0)),
        source = "ARRI, fiche technique M18"
    ),
    Projecteur(
        "cat-arri-t2", "True Blue T2 (2 kW)", "ARRI", FamilleProjecteur.FRESNEL_TUNGSTENE,
        puissanceW = 2000.0, temperatureK = 3200, angleFlood = 54.0, angleSpot = 10.0,
        mesures = listOf(m(false, 6.0, 958.0), m(true, 6.0, 6017.0)),
        source = "ARRI, fiche technique True Blue T2 (lampe CP/73)"
    ),
    Projecteur(
        "cat-arri-650-plus", "650 Plus", "ARRI", FamilleProjecteur.FRESNEL_TUNGSTENE,
        puissanceW = 650.0, temperatureK = 3200, angleFlood = 52.0, angleSpot = 14.5,
        mesures = listOf(m(true, 5.0, 2110.0)),
        source = "ARRI, photométrie relayée par les distributeurs (serré seulement)"
    )
)
