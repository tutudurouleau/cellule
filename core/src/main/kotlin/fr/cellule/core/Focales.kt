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

/* ── Les ratios de cadrage, photo et ciné ─────────────────────────────────── */

enum class FamilleRatio(val libelle: String) { PHOTO("Photo"), CINE("Ciné") }

/**
 * Un ratio d'image nommé. [ratio] est toujours largeur/hauteur — un nombre
 * inférieur à 1 (comme le 4:5 portrait) est donc un cadre plus haut que large,
 * et le partage de la géométrie avec [cadreUtile] n'a besoin de rien de plus :
 * un rapport est un rapport, qu'il soit vertical ou horizontal.
 */
data class RatioCadre(val nom: String, val ratio: Double, val famille: FamilleRatio, val detail: String)

val RATIOS_PHOTO = listOf(
    RatioCadre("1:1", 1.0, FamilleRatio.PHOTO, "Carré — moyen format, Instagram"),
    RatioCadre("5:4", 5.0 / 4.0, FamilleRatio.PHOTO, "Grand format 4×5 pouces, tirage classique"),
    RatioCadre("4:3", 4.0 / 3.0, FamilleRatio.PHOTO, "Compact, Micro 4/3 — le capteur natif de la plupart des téléphones"),
    RatioCadre("3:2", 3.0 / 2.0, FamilleRatio.PHOTO, "24×36 argentique et plein format numérique"),
    RatioCadre("16:9", 16.0 / 9.0, FamilleRatio.PHOTO, "Photo large, mode natif de certains téléphones"),
    RatioCadre("4:5", 4.0 / 5.0, FamilleRatio.PHOTO, "Portrait — cadrage vertical, réseaux sociaux")
)

val RATIOS_CINE = listOf(
    RatioCadre("1.37:1", 1.37, FamilleRatio.CINE, "Academy — le standard du 35 mm avant le grand écran"),
    RatioCadre("1.66:1", 1.66, FamilleRatio.CINE, "European widescreen"),
    RatioCadre("1.78:1", 16.0 / 9.0, FamilleRatio.CINE, "16:9 — HD, UHD, streaming"),
    RatioCadre("1.85:1", 1.85, FamilleRatio.CINE, "Flat — le standard américain en salle"),
    RatioCadre("2.35:1", 2.35, FamilleRatio.CINE, "Scope — le format large d'avant 1970"),
    RatioCadre("2.39:1", 2.39, FamilleRatio.CINE, "Scope moderne — anamorphique")
)

val TOUS_LES_RATIOS: List<RatioCadre> = RATIOS_PHOTO + RATIOS_CINE

/**
 * Le rectangle qu'occuperait un ratio de sortie dans le cadre du capteur,
 * en fractions [0, 1] de sa largeur et de sa hauteur — de quoi dessiner le
 * cache qui matérialise le recadrage sur l'aperçu.
 *
 * Un ratio de sortie plus large que le capteur mange de la hauteur (bandes
 * en haut et en bas, comme un cinémascope tiré d'un capteur 4:3) ; un ratio
 * plus étroit mange de la largeur (bandes latérales, comme un cadrage
 * portrait). Le capteur ne fournit jamais plus que ce qu'il a : on ne peut
 * que soustraire, jamais élargir le champ par recadrage.
 */
fun guideDeCadrage(rapportCapteur: Double, rapportSortie: Double): GuideCadrage {
    return if (rapportSortie > rapportCapteur) {
        val hauteur = rapportCapteur / rapportSortie
        GuideCadrage(largeur = 1.0, hauteur = hauteur)
    } else {
        val largeur = rapportSortie / rapportCapteur
        GuideCadrage(largeur = largeur, hauteur = 1.0)
    }
}

data class GuideCadrage(val largeur: Double, val hauteur: Double)

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
