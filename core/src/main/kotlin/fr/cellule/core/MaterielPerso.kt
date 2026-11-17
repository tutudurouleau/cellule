package fr.cellule.core

/*
 * Le matériel à soi : la caméra, la série d'objectifs, le projecteur qui ne
 * sont pas (ou pas encore) dans le catalogue. Leurs chiffres sont ceux que
 * la personne a saisis ; l'appli le dit à chaque fois qu'elle s'en sert.
 */

/** Une caméra à soi et ses modes de capteur. */
data class CameraPerso(
    val identifiant: Long,
    val nom: String,
    val modes: List<ZoneUtile> = emptyList(),
    val notes: String = ""
)

/** Une série d'objectifs à soi : ses focales exactes. */
data class SeriePerso(
    val identifiant: Long,
    val nom: String,
    val focales: List<Double> = emptyList(),
    /** Ouverture maximale, en T. */
    val tMax: Double? = null,
    /** 1 pour une série sphérique. */
    val desanamorphose: Double = 1.0,
    val notes: String = ""
)

enum class FamilleProjecteur(val libelle: String) {
    PANNEAU_LED("Panneau LED"),
    LED_PONCTUELLE("LED ponctuelle (COB)"),
    TUBE_LED("Tube LED"),
    FRESNEL_TUNGSTENE("Fresnel tungstène"),
    OUVERT_TUNGSTENE("Ouvert tungstène (mandarine, blonde)"),
    HMI("HMI"),
    FLUO("Fluorescent"),
    AUTRE("Autre")
}

/** Un éclairement relevé à une distance, faisceau ouvert (flood) ou serré (spot). */
data class MesureProjecteur(val spot: Boolean, val distanceM: Double, val lux: Double)

/**
 * Un projecteur : celui du catalogue (source nommée) ou le sien (chiffres
 * saisis). Tout est facultatif sauf le nom : un projecteur sans photométrie
 * se range quand même, il ne sert simplement pas aux calculs de lux.
 */
data class Projecteur(
    val identifiant: String,
    val nom: String,
    val fabricant: String = "",
    val famille: FamilleProjecteur = FamilleProjecteur.AUTRE,
    val puissanceW: Double? = null,
    val temperatureK: Int? = null,
    /** Angle du faisceau, ouvert (flood) et serré (spot), en degrés. */
    val angleFlood: Double? = null,
    val angleSpot: Double? = null,
    val mesures: List<MesureProjecteur> = emptyList(),
    val source: String = "",
    val perso: Boolean = false,
    val notes: String = ""
) {
    val nomComplet: String
        get() = if (fabricant.isBlank() || nom.startsWith(fabricant, ignoreCase = true)) nom else "$fabricant $nom"

    fun mesure(spot: Boolean): MesureProjecteur? = mesures.firstOrNull { it.spot == spot }
    fun angle(spot: Boolean): Double? = if (spot) angleSpot ?: angleFlood else angleFlood ?: angleSpot
}

object SaisieMateriel {

    /**
     * Lit une liste de focales tapée à la main : « 18, 25, 35, 50 »,
     * « 14,5 18 25 », « 18/25/35 ». Une virgule suivie d'un seul chiffre est
     * une décimale (« 14,5 ») ; sinon elle sépare. Triées, sans doublon.
     */
    fun lireFocales(texte: String): List<Double> =
        Regex("\\d+(?:[.,]\\d(?!\\d))?").findAll(texte)
            .mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }
            .filter { it in 1.0..2000.0 }
            .distinct()
            .sorted()
            .toList()

    /** Un nombre tapé à la main, virgule ou point ; null s'il n'y en a pas, ou s'il n'est pas positif. */
    fun lireNombre(texte: String): Double? =
        texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && !it.isNaN() && !it.isInfinite() }
}
