package fr.cellule.core

import kotlin.math.tan

/*
 * Le matériel de l'onglet Cinéma : sur quoi on tourne (pellicule ou capteur),
 * avec quelle caméra, quelle série d'objectifs.
 *
 * Même règle que le reste de l'appli : chaque taille, chaque indice vient
 * d'une source nommée. Ce qui n'est pas publié ne figure pas ici ; on le
 * saisit à la main dans le cadre de travail.
 */

enum class TypePelliculeCine(val libelle: String) {
    NEGATIF_COULEUR("Négatif couleur"),
    NOIR_ET_BLANC("Noir et blanc"),
    INVERSIBLE("Inversible")
}

/**
 * Une pellicule de cinéma et ses indices publiés : à la lumière du jour et en
 * tungstène (3200 K), filtre de conversion compris quand le fabricant en
 * prescrit un.
 */
data class PelliculeCine(
    val nom: String,
    /** Codes 35 mm / 16 mm du fabricant ; vide si le format n'existe pas. */
    val code35: String,
    val code16: String,
    val type: TypePelliculeCine,
    val eiJour: Int,
    val eiTungstene: Int,
    /** Le filtre que l'indice suppose, dans chaque lumière ; vide : aucun. */
    val filtreJour: String,
    val filtreTungstene: String,
    val source: String
) {
    val codes: String get() = listOf(code35, code16).filter { it.isNotBlank() }.joinToString(" / ")
    val equilibreTungstene: Boolean get() = filtreTungstene.isBlank() && filtreJour.isNotBlank()

    fun ei(tungstene: Boolean): Int = if (tungstene) eiTungstene else eiJour
    fun filtre(tungstene: Boolean): String = if (tungstene) filtreTungstene else filtreJour
}

private const val KODAK = "Kodak, fiche technique du film"

val PELLICULES_CINE: List<PelliculeCine> = listOf(
    PelliculeCine("Kodak VISION3 50D", "5203", "7203", TypePelliculeCine.NEGATIF_COULEUR, 50, 12, "", "80A", "$KODAK (H-1-5203)"),
    PelliculeCine("Kodak VISION3 250D", "5207", "7207", TypePelliculeCine.NEGATIF_COULEUR, 250, 64, "", "80A", "$KODAK (H-1-5207)"),
    PelliculeCine("Kodak VISION3 200T", "5213", "7213", TypePelliculeCine.NEGATIF_COULEUR, 125, 200, "85", "", "$KODAK (H-1-5213)"),
    PelliculeCine("Kodak VISION3 500T", "5219", "7219", TypePelliculeCine.NEGATIF_COULEUR, 320, 500, "85", "", "$KODAK (H-1-5219)"),
    PelliculeCine("Eastman DOUBLE-X", "5222", "7222", TypePelliculeCine.NOIR_ET_BLANC, 250, 200, "", "", "$KODAK (F-1-5222)"),
    PelliculeCine("Kodak EKTACHROME 100D", "5294", "7294", TypePelliculeCine.INVERSIBLE, 100, 25, "", "80A", "$KODAK (H-1-5294)"),
    PelliculeCine("Kodak TRI-X inversible", "", "7266", TypePelliculeCine.INVERSIBLE, 200, 160, "", "", "$KODAK (F-1-7266)")
)

/** Une fenêtre de prise de vues sur pellicule, ou un mode d'enregistrement d'un capteur. */
data class ZoneUtile(
    val nom: String,
    val largeurMm: Double,
    val hauteurMm: Double,
    val source: String,
    /** Faux tant que la valeur n'a pas été retrouvée sur une fiche publiée : l'appli affiche « à relire ». */
    val relue: Boolean = true
) {
    val ratio: Double get() = largeurMm / hauteurMm
    val dimensions: String get() = nombreCourt(largeurMm, 2) + " × " + nombreCourt(hauteurMm, 2) + " mm"
}

private const val ASC = "ASC Manual, ouvertures caméra normalisées SMPTE (source reconnue, non fabricant)"

/** Les fenêtres de prise de vues courantes. Pour la pellicule, c'est la fenêtre qui fait le « capteur ». */
val FORMATS_PELLICULE: List<ZoneUtile> = listOf(
    ZoneUtile("35 mm 4 perfs, pleine fenêtre (Super 35)", 24.89, 18.66, ASC),
    ZoneUtile("35 mm 4 perfs, Academy", 21.95, 16.00, ASC),
    ZoneUtile("35 mm 3 perfs", 24.89, 13.87, ASC),
    ZoneUtile("35 mm 2 perfs (Techniscope)", 22.05, 9.47, ASC),
    ZoneUtile("Super 16", 12.52, 7.41, ASC),
    ZoneUtile("16 mm standard", 10.26, 7.49, ASC),
    ZoneUtile("65 mm 5 perfs", 52.15, 23.07, "Kodak, « Film Format Choices »"),
    ZoneUtile("Super 8", 5.79, 4.01, "Kodak, « Film Format Choices »")
)

/** Les modes de capteur d'une caméra du catalogue (clé : son [Camera.nom]). */
data class ModesCamera(val camera: String, val modes: List<ZoneUtile>)

private const val ARRI = "ARRI, fiche technique"
private const val ARRI_ALEV3 = "ARRI, fiche technique (capteur ALEV III)"
private const val ARRI_UHD = "ARRI : 3840 × 2160 photosites de 8,25 µm"
private const val A_RELIRE = "valeur courante, pas encore retrouvée sur la fiche du fabricant"

val MODES_CAPTEUR: List<ModesCamera> = listOf(
    ModesCamera(
        "ALEXA (Classic, XT, SXT)", listOf(
            ZoneUtile("16:9", 23.76, 13.37, ARRI_ALEV3),
            ZoneUtile("4:3", 23.76, 17.82, ARRI_ALEV3),
            ZoneUtile("Open Gate (XT, SXT)", 28.25, 18.17, ARRI_ALEV3)
        )
    ),
    ModesCamera("AMIRA", listOf(ZoneUtile("16:9", 23.76, 13.37, ARRI_ALEV3))),
    ModesCamera("ALEXA 65", listOf(ZoneUtile("Open Gate 6,5K", 54.12, 25.58, ARRI))),
    ModesCamera(
        "ALEXA Mini", listOf(
            ZoneUtile("16:9 HD / 2,8K", 23.76, 13.37, ARRI_ALEV3),
            ZoneUtile("4:3 2,8K", 23.76, 17.82, ARRI_ALEV3),
            ZoneUtile("Open Gate 3,4K", 28.25, 18.17, ARRI_ALEV3)
        )
    ),
    ModesCamera(
        "ALEXA LF", listOf(
            ZoneUtile("Open Gate 4,5K", 36.70, 25.54, ARRI),
            ZoneUtile("16:9 UHD", 31.68, 17.82, ARRI_UHD),
            ZoneUtile("2,39:1 4,5K", 36.70, 15.31, ARRI)
        )
    ),
    ModesCamera(
        "ALEXA Mini LF", listOf(
            ZoneUtile("Open Gate 4,5K", 36.70, 25.54, ARRI),
            ZoneUtile("16:9 UHD", 31.68, 17.82, ARRI_UHD),
            ZoneUtile("2,39:1 4,5K", 36.70, 15.31, ARRI)
        )
    ),
    ModesCamera(
        "ALEXA 35", listOf(
            ZoneUtile("Open Gate 4,6K 3:2", 27.99, 19.22, ARRI),
            ZoneUtile("16:9 4K", 24.88, 14.00, ARRI)
        )
    ),
    ModesCamera("MONSTRO 8K VV", listOf(ZoneUtile("8K VV plein capteur", 40.96, 21.60, "RED, fiche technique"))),
    ModesCamera("V-RAPTOR", listOf(ZoneUtile("8K VV plein capteur", 40.96, 21.60, "RED, fiche technique"))),
    ModesCamera("KOMODO (et Komodo-X)", listOf(ZoneUtile("6K 17:9", 27.03, 14.26, A_RELIRE, relue = false))),
    ModesCamera("VENICE et VENICE 2", listOf(ZoneUtile("6K 3:2 plein format (VENICE)", 35.9, 24.0, A_RELIRE, relue = false))),
    ModesCamera("EOS 5D Mark II", listOf(ZoneUtile("Plein capteur 3:2", 36.0, 24.0, "Canon, fiche technique"))),
    ModesCamera("EOS C300", listOf(ZoneUtile("Super 35 utile", 24.6, 13.8, A_RELIRE, relue = false))),
    ModesCamera(
        "Pocket 4K et 6K", listOf(
            ZoneUtile("Pocket 4K, 4K DCI", 18.96, 10.00, A_RELIRE, relue = false),
            ZoneUtile("Pocket 6K, 6K", 23.10, 12.99, A_RELIRE, relue = false)
        )
    ),
    ModesCamera("URSA Mini Pro 12K", listOf(ZoneUtile("12K 17:9", 27.03, 14.25, "Blackmagic Design, fiche technique")))
)

/** Ce qu'on sait des focales d'une série d'objectifs, lu dans sa fiche. */
data class FocalesSerie(
    val min: Double?,
    val max: Double?,
    /** Les focales proposées : celles de la fiche si elle les énumère, sinon des focales courantes dans la plage. */
    val choix: List<Double>,
    /** Vrai si [choix] reprend exactement les focales publiées de la série. */
    val exactes: Boolean
)

object MaterielCine {

    /** Des focales courantes en cinéma, pour proposer des choix quand une série ne les énumère pas. */
    val FOCALES_COURANTES: List<Double> = listOf(
        8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 21.0, 25.0, 27.0, 32.0, 35.0, 40.0,
        50.0, 65.0, 75.0, 85.0, 100.0, 135.0, 150.0, 180.0, 200.0, 250.0, 300.0
    )

    /** Des angles de champ horizontaux pour choisir « par l'angle ». */
    val ANGLES_COURANTS: List<Double> = listOf(100.0, 90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 30.0, 20.0, 15.0, 10.0, 5.0)

    fun modesDe(camera: String): List<ZoneUtile> = MODES_CAPTEUR.firstOrNull { it.camera == camera }?.modes ?: emptyList()

    /** La focale qui donne cet angle de champ sur cette dimension de capteur. */
    fun focalePourAngle(dimensionMm: Double, angleDegres: Double): Double {
        if (dimensionMm <= 0 || angleDegres <= 0 || angleDegres >= 180) return Double.NaN
        return dimensionMm / (2 * tan(Math.toRadians(angleDegres / 2)))
    }

    /**
     * Lit le champ « focales » d'une fiche objectif : « 16 à 135 mm »,
     * « 14,5-45, 18-85 mm », « 18, 24, 35, 55 et 85 mm »… Ce qui est entre
     * parenthèses est ignoré ; sans « mm », rien n'est lu.
     */
    fun lireFocales(texte: String): FocalesSerie {
        val nu = texte.replace(Regex("\\([^)]*\\)"), " ")
        if (!nu.contains("mm")) return FocalesSerie(null, null, FOCALES_COURANTES, false)
        val nombres = Regex("\\d+(?:,\\d+)?").findAll(nu)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }
            .filter { it in 4.0..1200.0 }
            .toList()
        if (nombres.isEmpty()) return FocalesSerie(null, null, FOCALES_COURANTES, false)
        val plage = nu.contains(" à ") || nu.contains("-")
        if (!plage) {
            val liste = nombres.distinct().sorted()
            return FocalesSerie(liste.first(), liste.last(), liste, true)
        }
        val min = nombres.min()
        val max = nombres.max()
        val dedans = FOCALES_COURANTES.filter { it in min..max }
        val choix = (listOf(min) + dedans + listOf(max)).distinct().sorted()
        return FocalesSerie(min, max, choix, false)
    }
}
