package fr.cellule.core

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/*
 * La cellule de tournage.
 *
 * En prise de vues animées, la vitesse n'est pas un choix libre : elle découle
 * de la cadence et de l'angle d'obturation. Ce qui reste à régler, c'est le
 * diaphragme — gradué en T (transmission réelle) sur une optique de cinéma —
 * et, quand on veut garder une ouverture précise, la densité neutre à poser
 * devant.
 *
 * Rien ici ne vient d'une fiche : ce sont des définitions.
 *   temps de pose = angle / (360 × cadence)
 *   une densité de 0,3 retient un diaph (log₁₀ 2 = 0,301)
 */

/** Cadences proposées, en images par seconde. */
val CADENCES = listOf(23.976, 24.0, 25.0, 29.97, 30.0, 48.0, 50.0, 60.0, 100.0, 120.0)

/** Angles d'obturation proposés, en degrés. */
val ANGLES_OBTURATION = listOf(360.0, 270.0, 180.0, 172.8, 144.0, 90.0, 45.0)

/** Indices d'exposition proposés, au tiers de diaph. */
val INDICES_EXPOSITION = listOf(
    50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800,
    1000, 1280, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 12800
)

/**
 * Une graduation de la bague de diaphragme, au tiers. [rang] compte les tiers
 * depuis T1 ; la valeur exacte est 2^(rang/6), le [nominal] est ce qui est
 * gravé sur la bague.
 */
data class TStop(val rang: Int, val nominal: Double) {
    val exact: Double get() = 2.0.pow(rang / 6.0)
    val libelle: String get() = "T" + nombreCourt(nominal)
}

private val NOMINAUX = listOf(
    1.0, 1.1, 1.3, 1.4, 1.6, 1.8, 2.0, 2.2, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 5.0, 5.6,
    6.3, 7.1, 8.0, 9.0, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 20.0, 22.0, 25.0, 29.0, 32.0
)

/** De T1 à T32, tous les tiers. */
val T_STOPS: List<TStop> = NOMINAUX.mapIndexed { i, n -> TStop(i, n) }

/** « 2,8 », « 4 », « 172,8 » : pas de zéro inutile, la virgule française. */
fun nombreCourt(x: Double, decimales: Int = 3): String {
    if (x.isNaN() || x.isInfinite()) return "—"
    val facteur = 10.0.pow(decimales)
    val arrondi = Math.round(x * facteur) / facteur
    val entier = arrondi.toLong()
    if (arrondi == entier.toDouble()) return entier.toString()
    return arrondi.toBigDecimal().stripTrailingZeros().toPlainString().replace('.', ',')
}

/** D'où vient la perte de lumière d'un filtre. */
enum class OriginePerte(val libelle: String) {
    /** Une définition : la densité d'un neutre. */
    DEFINITION("par définition"),
    /** Lue sur une fiche du fabricant, nommée. */
    FABRICANT("fiche fabricant"),
    /** Réglée à la main, faute de fiche. */
    SAISIE("perte saisie par toi")
}

/** Les familles de filtres qu'on pose devant l'objectif. */
enum class FamilleFiltre(val libelle: String) {
    POLARISANT("Polarisant"),
    DIFFUSION("Diffusion"),
    COULEUR("Couleur / conversion"),
    AUTRE("Filtre maison")
}

/**
 * Un filtre en place, et ce qu'il coûte en lumière.
 *
 * [source] nomme la fiche d'où vient la perte quand il y en a une ; un filtre
 * dont la perte a été réglée à la main l'affiche comme telle.
 */
data class FiltrePose(
    val nom: String,
    val famille: FamilleFiltre,
    val perteDiaphs: Double,
    val origine: OriginePerte = OriginePerte.SAISIE,
    val source: String = ""
) {
    val mention: String
        get() = if (origine == OriginePerte.FABRICANT && source.isNotBlank()) source else origine.libelle
}

/** Une densité neutre : 0,3 par diaph. */
data class DensiteNeutre(val diaphs: Int) {
    val densite: Double get() = diaphs * 0.3
    val libelle: String
        get() = if (diaphs == 0) "Aucun" else "ND " + nombreCourt(densite, 1) + " · " + diaphs + " diaph" + (if (diaphs > 1) "s" else "")
    val court: String get() = if (diaphs == 0) "aucun ND" else "ND " + nombreCourt(densite, 1)
}

/** De rien à ND 3,0, par pas de 0,3. */
val DENSITES_NEUTRES: List<DensiteNeutre> = (0..10).map { DensiteNeutre(it) }

/** Ce qu'on a réglé sur la caméra. */
data class ReglageTournage(
    val cadence: Double = 24.0,
    val angle: Double = 180.0,
    val ei: Int = 800,
    /** Le neutre déjà posé, en diaphs. */
    val ndEnPlace: Int = 0,
    val filtres: List<FiltrePose> = emptyList()
) {
    /** Temps de pose, en secondes : angle / (360 × cadence). */
    val tempsPose: Double get() = Tournage.tempsPose(angle, cadence)

    /** Tout ce que les filtres retiennent, neutre compris, en diaphs. */
    val perteFiltres: Double get() = ndEnPlace + filtres.sumOf { it.perteDiaphs }
}

/** La proposition de densité neutre pour tenir une ouverture voulue. */
data class PropositionNd(
    val voulu: TStop,
    /** Ce qu'il faudrait retenir exactement, en diaphs ; négatif s'il manque de la lumière. */
    val aRetenir: Double,
    val nd: DensiteNeutre,
    /** Ce qui reste après le neutre proposé : positif, l'image est encore trop claire d'autant. */
    val residuel: Double
) {
    /** Il n'y a pas assez de lumière pour cette ouverture : aucun neutre n'y changera rien. */
    val manqueDeLumiere: Boolean get() = aRetenir < -1.0 / 6

    /** Le neutre nécessaire dépasse le plus dense de la série. */
    val horsSerie: Boolean get() = aRetenir > DENSITES_NEUTRES.last().diaphs + 0.5
}

data class ResultatTournage(
    val tempsPose: Double,
    /** L'ouverture T exacte que demande la mesure, filtres en place compris. */
    val tExact: Double,
    /** La graduation au tiers la plus proche. */
    val tProche: TStop,
    /** Ce que coûte l'arrondi au tiers, en diaphs — négatif : sous-exposé. */
    val ecartArrondi: Double,
    /** La lecture sort de la bague (plus ouvert que T1 ou plus fermé que T32). */
    val horsBague: Boolean,
    val nd: PropositionNd?
)

object Tournage {

    /** Vitesse d'obturation : angle / (360 × cadence). */
    fun tempsPose(angle: Double, cadence: Double): Double =
        if (cadence <= 0 || angle <= 0) Double.NaN else angle / (360.0 * cadence)

    /** L'angle qui donne ce temps de pose à cette cadence. */
    fun anglePour(tempsPose: Double, cadence: Double): Double = tempsPose * 360.0 * cadence

    /** « 1/48 s », « 1/50 s », « 1/96 s » ; au-delà de la seconde, en secondes. */
    fun libelleTempsPose(secondes: Double): String = when {
        secondes.isNaN() || secondes <= 0 -> "—"
        secondes >= 1 -> nombreCourt(secondes, 1) + " s"
        else -> "1/" + nombreCourt(1 / secondes, if (1 / secondes < 100) 1 else 0) + " s"
    }

    fun libelleCadence(cadence: Double): String = nombreCourt(cadence) + " im/s"

    fun libelleAngle(angle: Double): String = nombreCourt(angle, 1) + "°"

    fun tStopLePlusProche(t: Double): TStop =
        T_STOPS.minByOrNull { abs(log2(it.exact) - log2(t)) } ?: T_STOPS.first()

    /**
     * L'ouverture T qui expose juste : N² = 2^EV · t, l'EV étant celui de la
     * scène à 100 ISO, décalé de l'indice d'exposition et diminué de ce que
     * retiennent les filtres.
     */
    fun ouvertureExacte(ev100: Double, reglage: ReglageTournage): Double {
        val ev = ev100 + log2(reglage.ei / 100.0) - reglage.perteFiltres
        return sqrt(2.0.pow(ev) * reglage.tempsPose)
    }

    /**
     * Le neutre à ajouter pour ouvrir (ou fermer) jusqu'à [voulu] : on retient
     * l'écart entre l'ouverture que demande la lumière et celle qu'on veut,
     * arrondi au diaph entier puisque les neutres vont de 0,3 en 0,3.
     */
    fun ndPour(tExact: Double, voulu: TStop): PropositionNd {
        val aRetenir = 2 * log2(tExact / voulu.exact)
        val diaphs = aRetenir.roundToInt().coerceIn(0, DENSITES_NEUTRES.last().diaphs)
        return PropositionNd(voulu, aRetenir, DENSITES_NEUTRES[diaphs], aRetenir - diaphs)
    }

    fun calculer(ev100: Double, reglage: ReglageTournage, voulu: TStop? = null): ResultatTournage {
        val exact = ouvertureExacte(ev100, reglage)
        val proche = tStopLePlusProche(exact)
        return ResultatTournage(
            tempsPose = reglage.tempsPose,
            tExact = exact,
            tProche = proche,
            ecartArrondi = -2 * log2(proche.exact / exact),
            horsBague = exact < T_STOPS.first().exact * 0.94 || exact > T_STOPS.last().exact * 1.06,
            nd = voulu?.let { ndPour(exact, it) }
        )
    }

    /** Lit une saisie libre (« 23,976 », « 172.8 ») ; null si ce n'est pas un nombre positif. */
    fun lireNombre(texte: String): Double? =
        texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && !it.isInfinite() }
}
