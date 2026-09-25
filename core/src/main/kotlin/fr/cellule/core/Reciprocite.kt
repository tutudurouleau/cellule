package fr.cellule.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow

/**
 * L'écart à la réciprocité.
 *
 * La loi de réciprocité dit que seule compte la quantité de lumière :
 * 1 s à f/8 vaut 2 s à f/11. Le film la respecte pour les poses ordinaires,
 * mais plus pour les longues. Pour qu'un grain d'halogénure devienne
 * développable, il faut que plusieurs photons l'atteignent en peu de temps :
 * sous une lumière faible, ils arrivent trop espacés, et les premiers
 * électrons libérés se recombinent avant que l'image latente ne se forme.
 * Une partie de la lumière est perdue — d'autant plus que la pose est longue.
 *
 * D'où une correction qui grandit plus vite que la pose : un diaph de plus à
 * 10 s, trois à 100 s selon le film. Chaque film a sa propre courbe ; il n'y a
 * pas de chiffre universel.
 */
interface Reciprocite {
    val film: String
    val source: Source

    /** Le temps de pose à donner réellement pour ce que la cellule a mesuré. */
    fun corrige(mesure: Double): Double?

    /** Pour afficher la courbe : de combien la correction reste fiable. */
    val maximum: Double?

    /** Ce que dit la fiche sur le développement, s'il faut le retoucher. */
    fun developpement(mesure: Double): String? = null

    fun diaphs(mesure: Double): Double? = corrige(mesure)?.let { log2(it / mesure) }
}

/**
 * La formule d'Ilford : Tc = Tm^p. L'exposant se lit sur chaque fiche. La
 * formule ne corrige qu'au-delà de [seuil] : en dessous d'une seconde, elle
 * raccourcirait même la pose, ce qui n'a pas de sens.
 */
data class LoiIlford(
    override val film: String,
    val exposant: Double,
    val seuil: Double,
    override val source: Source
) : Reciprocite {

    override fun corrige(mesure: Double): Double =
        if (mesure <= seuil || mesure <= 1.0) mesure else mesure.pow(exposant)

    override val maximum: Double? = null
}

/**
 * Le tableau de Kodak : quelques poses mesurées et la pose à donner. Entre
 * deux lignes, Kodak renvoie à ses graphiques ; on suit la droite qui relie
 * les deux points en échelles logarithmiques, ce que sont ces graphiques.
 * Au-delà de la dernière ligne, la fiche ne dit rien, et nous non plus.
 */
data class TableReciprocite(
    override val film: String,
    override val source: Source,
    /** (pose mesurée, pose à donner, retouche du développement) */
    val lignes: List<Triple<Double, Double, String?>>
) : Reciprocite {

    override val maximum: Double get() = lignes.last().first

    override fun corrige(mesure: Double): Double? {
        if (mesure <= lignes.first().first) return mesure
        if (mesure > maximum) return null
        val (a, b) = lignes.zipWithNext().first { (a, b) -> mesure in a.first..b.first }
        val f = (ln(mesure) - ln(a.first)) / (ln(b.first) - ln(a.first))
        return exp(ln(a.second) + f * (ln(b.second) - ln(a.second)))
    }

    /** La retouche de la ligne publiée la plus proche, en échelle logarithmique. */
    override fun developpement(mesure: Double): String? =
        lignes.minByOrNull { kotlin.math.abs(ln(it.first) - ln(mesure)) }?.third
}

object Reciprocites {

    val LISTE: List<Reciprocite> = listOf(
        LoiIlford("Ilford HP5 Plus", 1.31, 0.5, Sources.ILFORD_HP5),
        LoiIlford("Ilford Pan F Plus", 1.33, 0.5, Sources.ILFORD_PANF),
        LoiIlford("Ilford Delta 3200", 1.33, 1.0, Sources.ILFORD_DELTA_3200),
        /*
         * F-4017 : 1 s → 2 s, 10 s → 50 s, 100 s → 1200 s, avec 10, 20 et
         * 30 % de développement en moins ; aucune correction de 1/1000 à 1/10 s.
         */
        TableReciprocite(
            "Kodak Tri-X", Sources.KODAK_TRIX,
            listOf(
                Triple(0.1, 0.1, null),
                Triple(1.0, 2.0, "développement −10 %"),
                Triple(10.0, 50.0, "développement −20 %"),
                Triple(100.0, 1200.0, "développement −30 %")
            )
        )
    )
}
