package fr.cellule.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow

/*
 * Temps de développement, température et push/pull, d'après les tables
 * publiées par les fabricants. Chaque table garde sa source : rien ici n'est
 * interpolé d'une base tierce, et une combinaison que le fabricant ne publie
 * pas n'existe pas.
 *
 * Les fractions de minutes sont celles des fiches : Kodak arrondit au quart
 * de minute, Ilford à la demi-minute.
 */

/* ── Température ──────────────────────────────────────────────────────────── */

/**
 * Pourquoi la courbe n'est pas une droite : le révélateur est une réaction
 * chimique, et la vitesse d'une réaction croît d'un même facteur à chaque
 * degré gagné (loi d'Arrhenius). Chaque degré multiplie donc le temps par le
 * même nombre au lieu de lui retirer le même nombre de secondes : c'est une
 * exponentielle, raide à froid, plus douce à chaud.
 */
interface Compensation {
    val intitule: String
    val source: Source

    /** Temps à [temperature] divisé par le temps à 20 °C. */
    fun facteur(temperature: Double): Double

    /** Les points que le fabricant publie, en (°C, facteur) : de quoi vérifier la courbe. */
    val reperes: List<Pair<Double, Double>>

    /** La plage où le fabricant publie des chiffres ; au-delà, on extrapole. */
    val plage: ClosedFloatingPointRange<Double>

    /**
     * Combien de temps ajoute un degré de moins, en fraction (0,1 = 10 %) : le
     * « taux » de la courbe, identique d'un bout à l'autre d'une exponentielle.
     */
    val tauxParDegre: Double
}

/**
 * La règle générale d'Ilford : « augmenter les temps de 10 % par degré en
 * moins, les diminuer de 10 % par degré en plus ». Ses propres exemples
 * montrent qu'il faut composer les 10 % — 6 min à 20 °C deviennent 4 min 30 à
 * 23 °C et 9 min à 16 °C, là où des 10 % simplement additionnés donneraient
 * 4 min 12 et 8 min 24.
 */
object RegleIlford : Compensation {
    const val RAISON = 1.1

    override val intitule = "Règle Ilford — 10 % par degré"
    override val source = Sources.ILFORD_ID11

    override fun facteur(temperature: Double): Double = RAISON.pow(20 - temperature)

    override val tauxParDegre = RAISON - 1

    /** L'exemple chiffré de la fiche : 6 min à 20 °C, 4½ à 23 °C, 9 à 16 °C. */
    override val reperes = listOf(16.0 to 9.0 / 6, 20.0 to 1.0, 23.0 to 4.5 / 6)

    override val plage = 16.0..23.0

    /** La même règle appliquée à plat, pour montrer l'écart. */
    fun lineaire(temperature: Double): Double = 1 - 0.1 * (temperature - 20)
}

/**
 * Une table du fabricant : le même film dans le même révélateur à plusieurs
 * températures. Entre deux points publiés, on suit l'exponentielle qui les
 * relie ; au-delà, on prolonge la tendance de toute la table (ajustement des
 * moindres carrés sur le logarithme du temps) — et on le dit.
 */
data class TableTemperature(
    val film: String,
    val revelateur: String,
    override val source: Source,
    val note: String,
    val minutes: List<Pair<Double, Double>>
) : Compensation {

    init {
        require(minutes.size >= 2 && minutes.any { it.first == 20.0 })
    }

    override val intitule: String get() = "$film · $revelateur"

    val t20: Double get() = minutes.first { it.first == 20.0 }.second

    override val reperes: List<Pair<Double, Double>> get() = minutes.map { it.first to it.second / t20 }

    override val plage: ClosedFloatingPointRange<Double>
        get() = minutes.minOf { it.first }..minutes.maxOf { it.first }

    /** Pente de ln(t) en fonction de la température : négative. */
    val pente: Double
        get() {
            val xs = minutes.map { it.first }
            val ys = minutes.map { ln(it.second) }
            val mx = xs.average()
            val my = ys.average()
            return xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) } / xs.sumOf { (it - mx) * (it - mx) }
        }

    /** Pour une table, le taux moyen : celui de l'exponentielle la plus proche des points. */
    override val tauxParDegre: Double get() = exp(-pente) - 1

    override fun facteur(temperature: Double): Double {
        val tries = minutes.sortedBy { it.first }
        val ln20 = ln(t20)
        if (temperature in plage) {
            val (a, b) = tries.zipWithNext().first { (a, b) -> temperature in a.first..b.first }
            val f = (temperature - a.first) / (b.first - a.first)
            return exp(ln(a.second) + f * (ln(b.second) - ln(a.second)) - ln20)
        }
        val bord = if (temperature < plage.start) tries.first() else tries.last()
        return exp(ln(bord.second) + pente * (temperature - bord.first) - ln20)
    }
}

object Temperatures {

    private fun kodakTriX(revelateur: String, vararg m: Double) = TableTemperature(
        "Kodak Tri-X 400", revelateur, Sources.KODAK_TRIX,
        "cuve, agitation toutes les 30 s",
        listOf(18.0, 20.0, 21.0, 22.0, 24.0).zip(m.toList())
    )

    private fun kodakD76(film: String, revelateur: String, vararg m: Double) = TableTemperature(
        film, revelateur, Sources.KODAK_D76,
        "cuve, agitation toutes les 30 s",
        listOf(18.0, 20.0, 21.0, 22.0, 24.0).zip(m.toList())
    )

    /** F-4017, « Small Tank » ; J-78, « Roll Films, Small Tank ». */
    val TABLES: List<TableTemperature> = listOf(
        kodakTriX("D-76", 8.0, 6.75, 6.25, 5.5, 4.75),
        kodakTriX("D-76 1+1", 10.75, 9.75, 9.0, 8.5, 7.75),
        kodakTriX("XTOL", 8.0, 7.0, 6.25, 5.75, 4.75),
        kodakTriX("XTOL 1+1", 10.0, 9.0, 8.5, 8.0, 7.25),
        kodakTriX("HC-110 dilution B", 4.5, 3.75, 3.5, 3.0, 2.5),
        kodakTriX("T-MAX Developer", 6.75, 6.0, 5.75, 5.5, 4.75),
        kodakD76("Kodak T-MAX 100", "D-76", 10.5, 9.0, 8.0, 7.0, 6.0),
        kodakD76("Kodak T-MAX 100", "D-76 1+1", 14.5, 12.0, 11.0, 10.0, 8.5),
        kodakD76("Kodak T-MAX 400", "D-76", 9.0, 8.0, 7.0, 6.5, 5.5),
        kodakD76("Kodak T-MAX 400", "D-76 1+1", 14.5, 12.5, 11.0, 10.0, 9.0)
    )

    val COMPENSATIONS: List<Compensation> = listOf(RegleIlford) + TABLES

    /** Kodak : « des temps de cuve de moins de 5 minutes peuvent donner un développement irrégulier ». */
    const val MINIMUM_UNIFORME = 5.0

    /**
     * Même chez Ilford, une table publiée s'écarte de la règle générale : la
     * Delta 3200 en ID-11 à EI 3200 passe de 10½ min à 20 °C à 9 min à 24 °C,
     * là où les 10 % par degré donneraient 7 min 10. Quand la table existe,
     * elle prime.
     */
    val DELTA_3200_ID11 = TableTemperature(
        "Ilford Delta 3200 à EI 3200", "ID-11", Sources.ILFORD_DELTA_3200,
        "cuve, agitation intermittente", listOf(20.0 to 10.5, 24.0 to 9.0)
    )

    fun temps(t20: Double, temperature: Double, compensation: Compensation): Double =
        t20 * compensation.facteur(temperature)
}

/* ── Push et pull ─────────────────────────────────────────────────────────── */

/**
 * Les temps d'un film dans un révélateur, à 20 °C, pour chaque indice
 * d'exposition que le fabricant publie. [iso] est la sensibilité nominale :
 * l'écart en diaphs se compte depuis elle.
 */
data class TablePush(
    val film: String,
    val iso: Int,
    val revelateur: String,
    val source: Source,
    val note: String,
    val minutes: Map<Int, Double>
) {
    val intitule: String get() = "$film · $revelateur"

    val normal: Double get() = minutes.getValue(iso)

    val indices: List<Int> get() = minutes.keys.sorted()

    fun diaphs(ei: Int): Double = kotlin.math.log2(ei.toDouble() / iso)

    /** Combien de fois le temps normal : ×1,4 pour 1,4 fois plus long. */
    fun facteur(ei: Int): Double? = minutes[ei]?.let { it / normal }
}

object Push {

    private fun hp5(revelateur: String, vararg paires: Pair<Int, Double>) = TablePush(
        "Ilford HP5 Plus", 400, revelateur, Sources.ILFORD_HP5, "cuve, agitation intermittente", mapOf(*paires)
    )

    /*
     * Tri-X 400 : Kodak précise qu'un diaph de sous-exposition se développe
     * au temps normal (« you can underexpose by one stop and use normal
     * processing times ») ; EI 800 reprend donc le temps d'EI 400.
     */
    private fun triX(revelateur: String, normal: Double, ei1600: Double, ei3200: Double?) = TablePush(
        "Kodak Tri-X 400", 400, revelateur, Sources.KODAK_TRIX, "cuve, agitation toutes les 30 s",
        buildMap {
            put(400, normal); put(800, normal); put(1600, ei1600)
            ei3200?.let { put(3200, it) }
        }
    )

    val TABLES: List<TablePush> = listOf(
        hp5("ILFOTEC DD-X 1+4", 400 to 9.0, 800 to 10.0, 1600 to 13.0, 3200 to 20.0),
        hp5("ILFOSOL 3 1+9", 200 to 5.0, 400 to 6.5, 800 to 13.5),
        hp5("ILFOSOL 3 1+14", 200 to 7.0, 400 to 11.0, 800 to 19.5),
        hp5("ILFOTEC HC 1+31", 400 to 6.5, 800 to 9.5, 1600 to 14.0),
        hp5("ID-11", 400 to 7.5, 800 to 10.5, 1600 to 14.0),
        hp5("ID-11 1+1", 400 to 13.0, 800 to 16.5),
        hp5("MICROPHEN", 400 to 6.5, 800 to 8.0, 1600 to 11.0, 3200 to 16.0),
        hp5("Kodak D-76", 400 to 7.5, 800 to 9.5, 1600 to 12.5),
        hp5("Kodak XTOL", 400 to 8.0, 800 to 11.0, 1600 to 14.0, 3200 to 19.0),
        hp5("Rodinal 1+25", 400 to 6.0, 800 to 8.0),
        triX("D-76", 6.75, 9.5, 11.0),
        triX("D-76 1+1", 9.75, 13.25, 16.0),
        triX("XTOL", 7.0, 9.75, 11.5),
        triX("XTOL 1+1", 9.0, 13.25, 15.5),
        triX("HC-110 dilution B", 3.75, 6.0, null),
        TablePush(
            "Kodak T-MAX 100", 100, "D-76", Sources.KODAK_D76, "cuve ; EI 200 au temps normal, dit Kodak",
            mapOf(100 to 9.0, 200 to 9.0, 400 to 11.0)
        ),
        TablePush(
            "Kodak T-MAX 400", 400, "D-76", Sources.KODAK_D76, "cuve ; EI 800 au temps normal, dit Kodak",
            mapOf(400 to 8.0, 800 to 8.0, 1600 to 10.5)
        )
    )

    val FILMS: List<String> get() = TABLES.map { it.film }.distinct()
}
