package fr.cellule.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Les constantes du système photométrique et les conversions entre les trois
 * grandeurs qu'il faut cesser de confondre : l'éclairement qui arrive sur une
 * surface (lux), la luminance qui en repart (cd/m²), et l'exposition qu'on
 * affiche sur l'appareil (EV).
 *
 * C = 250 pour la voie incidente, K = 12,5 pour la voie réfléchie. Ces deux
 * valeurs ne sont pas exactement cohérentes entre elles : la cohérence
 * exigerait C = K·π/ρ ≈ 218. Il reste donc un écart structurel de 0,2 diaph
 * entre une cellule incidente et un spotmètre pointé sur une charte grise.
 */
object Photometrie {

    const val C_INCIDENT = 250.0
    const val K_REFLECHI = 12.5
    const val GRIS = 0.18

    /** Éclairement incident correspondant à un EV, à 100 ISO. */
    fun lux(ev100: Double): Double = 2.5 * 2.0.pow(ev100)

    fun evDepuisLux(lux: Double): Double = log2(lux / 2.5)

    /** Luminance d'un gris moyen correspondant à un EV, à 100 ISO. */
    fun cdm2(ev100: Double): Double = 0.125 * 2.0.pow(ev100)

    fun evDepuisCdm2(luminance: Double): Double = log2(luminance / 0.125)

    /** L = E·ρ/π — le pont entre ce qui tombe et ce qui repart. */
    fun luminance(eclairement: Double, reflectance: Double): Double =
        eclairement * reflectance / PI

    /** Décalage en diaphs d'une sensibilité par rapport à 100 ISO. */
    fun decalageIso(iso: Int): Double = log2(iso / 100.0)

    /** EV = log₂(N²/t) */
    fun evDepuisReglages(ouverture: Double, tempsPoseSec: Double): Double =
        log2(ouverture * ouverture / tempsPoseSec)

    /** Ouverture exacte donnant cet EV à cette vitesse. */
    fun ouverturePour(ev: Double, tempsPoseSec: Double): Double =
        sqrt(2.0.pow(ev) * tempsPoseSec)

    /** Temps de pose exact donnant cet EV à cette ouverture. */
    fun tempsPosePour(ev: Double, ouverture: Double): Double =
        ouverture * ouverture / 2.0.pow(ev)
}

val OUVERTURES = listOf(1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0, 45.0, 64.0)

data class Vitesse(val libelle: String, val secondes: Double)

val VITESSES = listOf(
    Vitesse("1/8000", 1.0 / 8000), Vitesse("1/4000", 1.0 / 4000),
    Vitesse("1/2000", 1.0 / 2000), Vitesse("1/1000", 1.0 / 1000),
    Vitesse("1/500", 1.0 / 500), Vitesse("1/250", 1.0 / 250),
    Vitesse("1/125", 1.0 / 125), Vitesse("1/60", 1.0 / 60),
    Vitesse("1/30", 1.0 / 30), Vitesse("1/15", 1.0 / 15),
    Vitesse("1/8", 1.0 / 8), Vitesse("1/4", 1.0 / 4), Vitesse("1/2", 1.0 / 2),
    Vitesse("1 s", 1.0), Vitesse("2 s", 2.0), Vitesse("4 s", 4.0),
    Vitesse("8 s", 8.0), Vitesse("15 s", 15.0), Vitesse("30 s", 30.0),
    Vitesse("1 min", 60.0), Vitesse("2 min", 120.0), Vitesse("4 min", 240.0),
    Vitesse("8 min", 480.0), Vitesse("15 min", 900.0), Vitesse("30 min", 1800.0)
)

val SENSIBILITES = listOf(25, 50, 64, 100, 125, 160, 200, 400, 800, 1600, 3200, 6400, 12800)

data class FiltreNd(val libelle: String, val diaphs: Int)

val FILTRES_ND = listOf(
    FiltreNd("Aucun", 0), FiltreNd("ND2 · 0,3", 1), FiltreNd("ND4 · 0,6", 2),
    FiltreNd("ND8 · 0,9", 3), FiltreNd("ND16 · 1,2", 4), FiltreNd("ND32 · 1,5", 5),
    FiltreNd("ND64 · 1,8", 6), FiltreNd("ND256 · 2,4", 8), FiltreNd("ND1000 · 3,0", 10)
)

/**
 * Un couple diaph/vitesse. [ouvertureExacte] est la valeur que demande le
 * calcul, [ouverture] la graduation la plus proche, et [ecartDiaphs] ce que
 * coûte l'arrondi — négatif si le cliché sera sous-exposé.
 */
data class Couple(
    val vitesse: Vitesse,
    val ouvertureExacte: Double,
    val ouverture: Double,
    val ecartDiaphs: Double
)

fun ouvertureLaPlusProche(n: Double): Double =
    OUVERTURES.minByOrNull { abs(log2(it) - log2(n)) } ?: OUVERTURES.first()

fun vitesseLaPlusProche(secondes: Double): Vitesse =
    VITESSES.minByOrNull { abs(log2(it.secondes / secondes)) } ?: VITESSES.first()

/** Tous les couples praticables pour cet EV appareil. */
fun couples(evAppareil: Double): List<Couple> = VITESSES.mapNotNull { v ->
    val exacte = Photometrie.ouverturePour(evAppareil, v.secondes)
    if (exacte < 0.9 || exacte > 76.0) null
    else {
        val proche = ouvertureLaPlusProche(exacte)
        Couple(v, exacte, proche, -2 * log2(proche / exacte))
    }
}

/** Le couple le plus proche d'une vitesse voulue. */
fun coupleConseille(evAppareil: Double, vitesseVoulueSec: Double): Couple? =
    couples(evAppareil).minByOrNull { abs(log2(it.vitesse.secondes / vitesseVoulueSec)) }

/**
 * Couple de référence pour les tables : on reste dans les ouvertures qu'un
 * objectif possède vraiment (f/2 à f/22) et on ne s'écarte du 1/125 que
 * lorsqu'il le faut. C'est ce qui reproduit la table du document source.
 */
fun coupleDeTable(evAppareil: Double): Couple? {
    val praticables = couples(evAppareil).filter { it.ouverture in 2.0..22.0 }
    val pool = praticables.ifEmpty { couples(evAppareil) }
    return pool.minByOrNull { abs(log2(it.vitesse.secondes * 125)) }
}

/** « f/5,6 » et non « f/5.6 » ni « f/22.0 ». */
fun libelleOuverture(n: Double): String {
    val arrondi = Math.round(n * 10) / 10.0
    val entier = arrondi.toLong()
    return if (arrondi == entier.toDouble()) "f/$entier"
    else "f/" + arrondi.toString().replace('.', ',')
}

/** Écart en diaphs, arrondi au tiers, avec les fractions typographiques. */
fun libelleDiaphs(x: Double): String {
    if (abs(x) < 0.08) return "juste"
    val tiers = Math.round(x * 3).toInt()
    if (tiers == 0) return "juste"
    val entier = abs(tiers) / 3
    val reste = abs(tiers) % 3
    val fraction = when (reste) { 1 -> "⅓"; 2 -> "⅔"; else -> "" }
    val corps = when {
        entier > 0 && fraction.isNotEmpty() -> "$entier $fraction"
        entier > 0 -> "$entier"
        else -> fraction
    }
    return (if (x < 0) "−" else "+") + corps
}
