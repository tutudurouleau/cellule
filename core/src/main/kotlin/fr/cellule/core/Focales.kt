package fr.cellule.core

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.tan

/**
 * Angles de champ et équivalences de focale.
 *
 * Une focale ne veut rien dire seule : elle ne prend un sens qu'avec la taille
 * du capteur derrière. Ce qui se voit, ce qui se compare, ce qui s'apprend,
 * c'est l'angle de champ.
 *
 *     angle = 2 · arctan(dimension / 2f)
 *
 * L'équivalence 24×36 consiste à chercher la focale qui, sur un format
 * 36 × 24 mm, donnerait le même angle.
 */
object Focales {

    const val LARGEUR_REFERENCE = 36.0
    const val HAUTEUR_REFERENCE = 24.0
    val DIAGONALE_REFERENCE = hypot(LARGEUR_REFERENCE, HAUTEUR_REFERENCE)

    /** Angle de champ en degrés, pour une dimension de capteur et une focale. */
    fun angleDeChamp(dimensionMm: Double, focaleMm: Double): Double {
        if (focaleMm <= 0) return Double.NaN
        return 2 * Math.toDegrees(atan(dimensionMm / (2 * focaleMm)))
    }

    /** La focale qui donne cet angle sur cette dimension. */
    fun focalePourAngle(angleDegres: Double, dimensionMm: Double): Double {
        val demi = Math.toRadians(angleDegres / 2)
        if (demi <= 0) return Double.NaN
        return dimensionMm / (2 * tan(demi))
    }

    /**
     * Le rectangle de capteur réellement exposé.
     *
     * Le zoom numérique en recadre une fraction ; le format de sortie en
     * retaille encore, puisqu'une image en 4:3 ne se sert pas de la même part
     * d'un capteur qu'une image en 16:9.
     */
    fun cadreUtile(
        capteurLargeurMm: Double,
        capteurHauteurMm: Double,
        fractionLargeur: Double,
        fractionHauteur: Double,
        rapportImage: Double
    ): Cadre {
        val l = capteurLargeurMm * fractionLargeur
        val h = capteurHauteurMm * fractionHauteur
        return if (l / h > rapportImage) Cadre(h * rapportImage, h)
        else Cadre(l, l / rapportImage)
    }

    fun equivalence(cadre: Cadre, focaleMm: Double): Equivalence {
        val ah = angleDeChamp(cadre.largeurMm, focaleMm)
        val av = angleDeChamp(cadre.hauteurMm, focaleMm)
        val ad = angleDeChamp(cadre.diagonaleMm, focaleMm)
        return Equivalence(
            focaleReelleMm = focaleMm,
            cadre = cadre,
            angleHorizontal = ah,
            angleVertical = av,
            angleDiagonal = ad,
            equivalentHorizontal = focalePourAngle(ah, LARGEUR_REFERENCE),
            equivalentDiagonal = focalePourAngle(ad, DIAGONALE_REFERENCE)
        )
    }
}

data class Cadre(val largeurMm: Double, val hauteurMm: Double) {
    val diagonaleMm: Double get() = hypot(largeurMm, hauteurMm)
}

data class Equivalence(
    val focaleReelleMm: Double,
    val cadre: Cadre,
    val angleHorizontal: Double,
    val angleVertical: Double,
    val angleDiagonal: Double,
    /**
     * Focale 24×36 cadrant la même largeur. C'est celle qui répond à la
     * question qu'on se pose vraiment : « en tournant sur moi-même, est-ce que
     * je verrais la même chose d'un bord à l'autre ? »
     */
    val equivalentHorizontal: Double,
    /** Focale 24×36 de même angle diagonal — la convention des fabricants. */
    val equivalentDiagonal: Double
) {
    /** Le facteur qu'on appelle « de conversion », ou « de recadrage ». */
    val facteurDeConversion: Double
        get() = if (focaleReelleMm > 0) equivalentHorizontal / focaleReelleMm else Double.NaN
}

/* ── Les focales qu'on apprend à reconnaître ─────────────────────────────── */

data class FocaleClassique(val mm: Int, val surnom: String, val usage: String) {
    val angleHorizontal: Double
        get() = Focales.angleDeChamp(Focales.LARGEUR_REFERENCE, mm.toDouble())
    val angleDiagonal: Double
        get() = Focales.angleDeChamp(Focales.DIAGONALE_REFERENCE, mm.toDouble())
}

val FOCALES_CLASSIQUES = listOf(
    FocaleClassique(14, "Ultra grand-angle", "architecture serrée, paysage dramatisé — les lignes fuient"),
    FocaleClassique(20, "Très grand-angle", "intérieurs, ciels ; le premier plan écrase déjà le fond"),
    FocaleClassique(24, "Grand-angle", "paysage et reportage large ; il faut s'approcher pour remplir"),
    FocaleClassique(28, "Grand-angle doux", "rue, scène de vie ; on est dedans sans déformer"),
    FocaleClassique(35, "Semi grand-angle", "la focale du reportage — le sujet et son décor"),
    FocaleClassique(40, "Normale courte", "l'entre-deux discret, très proche du regard"),
    FocaleClassique(50, "Normale", "l'angle qui ne raconte rien de plus que l'œil ne voit"),
    FocaleClassique(85, "Petit téléobjectif", "le portrait ; les traits se remettent en place"),
    FocaleClassique(105, "Téléobjectif court", "portrait serré, détail à distance"),
    FocaleClassique(135, "Téléobjectif", "isole ; l'arrière-plan se rapproche et se dissout"),
    FocaleClassique(200, "Long téléobjectif", "compression franche, sujet découpé du fond")
)

object Cadrage {

    /** La classique la plus proche, en écart de focale relatif. */
    fun laPlusProche(mm: Double): FocaleClassique =
        FOCALES_CLASSIQUES.minByOrNull { abs(Math.log(it.mm / mm)) } ?: FOCALES_CLASSIQUES[6]

    /**
     * Part de la largeur du cadre actuel qu'occuperait une focale plus longue.
     *
     * À 28 mm équivalents, un 85 mm ne prend qu'un tiers de la largeur : c'est
     * ce rectangle qu'on dessine par-dessus l'aperçu pour apprendre à voir.
     * Une focale plus courte que la nôtre déborde du cadre, on ne peut pas la
     * montrer — d'où le null.
     */
    fun fractionDuCadre(equivalentActuel: Double, focaleVisee: Double): Double? {
        if (equivalentActuel <= 0 || focaleVisee <= 0) return null
        val fraction = equivalentActuel / focaleVisee
        return if (fraction > 1.0) null else fraction
    }

    /** Écart entre deux focales, exprimé comme on le ressent : en proportion. */
    fun ecartRelatif(estimee: Double, vraie: Double): Double =
        if (vraie <= 0) Double.NaN else (estimee - vraie) / vraie

    /**
     * Le zoom à appliquer pour atteindre une focale équivalente donnée, sachant
     * celle obtenue au zoom courant.
     */
    fun zoomPour(
        equivalentVise: Double,
        equivalentCourant: Double,
        zoomCourant: Double
    ): Double = zoomCourant * (equivalentVise / equivalentCourant)
}
