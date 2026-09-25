package fr.cellule.core

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Une dilution « 1+N » : une part de concentré pour N parts d'eau, soit N+1
 * parts en tout. C'est là que tout le monde se trompe une fois : 1+9 ne veut
 * pas dire « un neuvième » mais « un dixième » — 50 mL de concentré dans
 * 500 mL de solution, pas 55,6.
 *
 * Kodak écrit « D-76 (1:1) » ce qu'Ilford, dans sa fiche HP5 Plus, écrit
 * « Kodak D-76 1+1 » : une part de révélateur pour une part d'eau — la seule
 * lecture possible, puisque « un sur un » voudrait dire pur. Ailleurs (en
 * chimie de laboratoire), « 1:10 » se lit parfois comme un dixième du total :
 * dans le doute, on revient à la fiche du révélateur.
 */
data class Dilution(val partsEau: Double) {

    init {
        require(partsEau >= 0) { "une dilution ne retire pas d'eau" }
    }

    val partsTotales: Double get() = 1 + partsEau

    /** « 1+9 », « 1+31 », « 1+0 » pour le concentré pur. */
    val libelle: String get() = "1+" + nombreSansZero(partsEau)

    /** La part du concentré dans la solution : 1+9 → 10 %. */
    val fraction: Double get() = 1 / partsTotales

    fun concentre(volumeTotal: Double): Double = volumeTotal / partsTotales

    fun eau(volumeTotal: Double): Double = volumeTotal - concentre(volumeTotal)

    /** Le volume de solution que donne une quantité de concentré. */
    fun volumePour(concentre: Double): Double = concentre * partsTotales

    companion object {
        /**
         * Lit « 1+9 », « 1:31 », « 1 + 50 », « 9 » (sous-entendu 1+9) ou
         * « stock » (pur). Rend null pour ce qui n'est pas une dilution.
         */
        fun lire(texte: String): Dilution? {
            val t = texte.trim().lowercase().replace(',', '.')
            if (t in setOf("stock", "pur", "pure", "0")) return Dilution(0.0)
            val motif = Regex("""^(?:(\d+(?:\.\d+)?)\s*[+:]\s*)?(\d+(?:\.\d+)?)$""")
            val m = motif.find(t) ?: return null
            val concentre = m.groupValues[1].ifEmpty { "1" }.toDouble()
            val eau = m.groupValues[2].toDouble()
            if (concentre <= 0) return null
            return Dilution(eau / concentre)
        }
    }
}

/** 9 plutôt que 9.0, 1,5 plutôt que 1.5. */
internal fun nombreSansZero(x: Double): String {
    val arrondi = (x * 10).roundToInt() / 10.0
    return if (abs(arrondi - arrondi.toLong()) < 1e-9) arrondi.toLong().toString()
    else arrondi.toString().replace('.', ',')
}

/**
 * Les deux quantités à mesurer pour une cuve, avec ce qu'il faut retenir :
 * le concentré se mesure à la seringue ou à l'éprouvette graduée fine — c'est
 * lui qui fixe l'activité du bain, une erreur de 2 mL sur 500 d'eau ne compte
 * pas, 2 mL de trop sur 10 de concentré font 20 %.
 */
data class Preparation(val dilution: Dilution, val volumeTotal: Double) {
    val concentre: Double get() = dilution.concentre(volumeTotal)
    val eau: Double get() = dilution.eau(volumeTotal)
}

/** Une dilution telle qu'une fiche la donne, pour préremplir le calculateur. */
data class DilutionPubliee(
    val produit: String,
    val dilution: Dilution,
    val usage: String,
    val source: Source,
    val note: String = ""
) {
    val intitule: String get() = "$produit ${dilution.libelle}"
}

object DilutionsPubliees {

    /*
     * Toutes lues dans la fiche HP5 Plus d'Ilford (tableau des révélateurs et
     * chaîne de traitement), sauf le D-76 1+1, pris chez Kodak.
     */
    val LISTE: List<DilutionPubliee> = listOf(
        DilutionPubliee("ILFOTEC DD-X", Dilution(4.0), "révélateur film, usage unique", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOSOL 3", Dilution(9.0), "révélateur film, usage unique", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOSOL 3", Dilution(14.0), "révélateur film, plus économique", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOTEC HC", Dilution(31.0), "révélateur film", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOTEC HC", Dilution(15.0), "révélateur film, temps courts", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOTEC LC29", Dilution(19.0), "révélateur film", Sources.ILFORD_HP5),
        DilutionPubliee("ID-11", Dilution(1.0), "révélateur film, usage unique", Sources.ILFORD_HP5),
        DilutionPubliee("ID-11", Dilution(3.0), "révélateur film, netteté et économie", Sources.ILFORD_HP5),
        DilutionPubliee(
            "Kodak D-76", Dilution(1.0), "révélateur film, usage unique", Sources.KODAK_D76,
            "Kodak : 473 mL de solution diluée par film 135-36 ; avec 237 mL seulement, allonger le temps de 10 %."
        ),
        DilutionPubliee("Rodinal", Dilution(25.0), "révélateur film", Sources.ILFORD_HP5),
        DilutionPubliee("Rodinal", Dilution(50.0), "révélateur film", Sources.ILFORD_HP5),
        DilutionPubliee("ILFOSTOP", Dilution(19.0), "bain d'arrêt, 10 s à 20 °C", Sources.ILFORD_HP5),
        DilutionPubliee("RAPID FIXER", Dilution(4.0), "fixateur film, 2 à 5 min à 20 °C", Sources.ILFORD_HP5),
        DilutionPubliee(
            "ILFOTOL", Dilution(200.0), "agent mouillant, rinçage final", Sources.ILFORD_HP5,
            "Ilford : 5 mL par litre pour commencer, à ajuster selon l'eau — trop ou trop peu laisse des traces."
        )
    )
}
