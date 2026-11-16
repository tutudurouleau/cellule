package fr.cellule.core

/*
 * Les outils d'analyse d'image de la cellule de tournage : faux contraste,
 * zébras, histogramme, forme d'onde.
 *
 * Ils lisent l'image du TÉLÉPHONE — son capteur, sa courbe, son exposition
 * automatique — et pas le signal de la caméra de tournage. Ils montrent où la
 * scène est claire ou sombre, pas ce que la caméra enregistrera.
 *
 * Tout part d'une grille réduite du plan de luminance, redressée dans le sens
 * de l'écran et exprimée en pourcentage du signal (0 à 100 %). La réduire une
 * fois suffit aux quatre outils, et ne coûte presque rien à la batterie.
 */

/** Le plan de luminance réduit, dans le sens de l'écran, en pourcentage du signal. */
class GrilleLuma(val largeur: Int, val hauteur: Int, val pourcents: ByteArray) {
    fun niveau(x: Int, y: Int): Int = pourcents[y * largeur + x].toInt()
}

/** Une plage du faux contraste : de [de] inclus à [a] exclu, en pourcentage du signal. */
data class PlageFauxContraste(val de: Int, val a: Int, val nom: String, val argb: Int)

/** Les deux seuils des zébras, en pourcentage du signal. */
data class SeuilsZebras(val haut: Int = 95, val moyen: Int = 70) {
    /** Le seuil moyen marque une bande de ± 5 % autour de sa valeur. */
    val moyenBas: Int get() = moyen - DEMI_BANDE
    val moyenHaut: Int get() = moyen + DEMI_BANDE

    companion object {
        const val DEMI_BANDE = 5
    }
}

object OutilsImage {

    const val TRANSPARENT = 0

    /**
     * La palette du faux contraste. C'est une convention de lecture propre à
     * Cellule, pas une norme : les extrêmes en couleurs franches, le milieu
     * laissé à l'image.
     */
    val PALETTE: List<PlageFauxContraste> = listOf(
        PlageFauxContraste(0, 3, "bouché", 0xE06A2FB5.toInt()),
        PlageFauxContraste(3, 12, "ombres profondes", 0xD02B5FD9.toInt()),
        PlageFauxContraste(12, 38, "ombres", TRANSPARENT),
        PlageFauxContraste(38, 46, "milieu", 0xD02E9E4F.toInt()),
        PlageFauxContraste(46, 78, "clairs", TRANSPARENT),
        PlageFauxContraste(78, 92, "hautes lumières", 0xD0E3C52D.toInt()),
        PlageFauxContraste(92, 98, "presque cramé", 0xE0E8862A.toInt()),
        PlageFauxContraste(98, 101, "cramé", 0xF0D9342B.toInt())
    )

    private val COULEUR_PAR_NIVEAU = IntArray(101) { n ->
        PALETTE.firstOrNull { n >= it.de && n < it.a }?.argb ?: TRANSPARENT
    }

    /** Couleurs des rayures des zébras. */
    const val ZEBRA_HAUT = 0xF0D9342B.toInt()
    const val ZEBRA_MOYEN = 0xE02E9E4F.toInt()

    /** Le code de luminance, en pourcentage du signal selon la plage du flux. */
    fun pourcent(y: Int, plageVideo: Boolean): Int {
        val p = if (plageVideo) (y - 16) * 100.0 / 219.0 else y * 100.0 / 255.0
        return Math.round(p).toInt().coerceIn(0, 100)
    }

    /**
     * Réduit le plan Y à une grille dont le grand côté fait [grandCote] cases,
     * redressée dans le sens de l'écran ([rotation] est celle d'ImageInfo).
     */
    fun grille(
        luma: ByteArray,
        largeur: Int,
        hauteur: Int,
        rowStride: Int,
        pixelStride: Int = 1,
        rotation: Int = 0,
        plageVideo: Boolean = true,
        grandCote: Int = 160
    ): GrilleLuma {
        val tourne = ((rotation % 360) + 360) % 360 % 180 == 90
        val lEcran = if (tourne) hauteur else largeur
        val hEcran = if (tourne) largeur else hauteur
        val echelle = grandCote.toDouble() / maxOf(lEcran, hEcran)
        val gl = maxOf(1, Math.round(lEcran * echelle).toInt())
        val gh = maxOf(1, Math.round(hEcran * echelle).toInt())
        val table = ByteArray(256) { pourcent(it, plageVideo).toByte() }
        val sortie = ByteArray(gl * gh)
        for (gy in 0 until gh) {
            val v = (gy + 0.5f) / gh
            for (gx in 0 until gl) {
                val u = (gx + 0.5f) / gl
                val (cx, cy) = Reperes.ecranVersCapteur(u, v, rotation)
                val x = (cx * largeur).toInt().coerceIn(0, largeur - 1)
                val y = (cy * hauteur).toInt().coerceIn(0, hauteur - 1)
                val index = y * rowStride + x * pixelStride
                sortie[gy * gl + gx] = if (index in luma.indices) table[luma[index].toInt() and 0xFF] else 0
            }
        }
        return GrilleLuma(gl, gh, sortie)
    }

    /** Répartition des niveaux, en [classes] classes de 0 à 100 %. */
    fun histogramme(grille: GrilleLuma, classes: Int = 64): IntArray {
        val h = IntArray(classes)
        for (p in grille.pourcents) {
            h[(p.toInt() * classes / 101).coerceIn(0, classes - 1)]++
        }
        return h
    }

    /**
     * Forme d'onde : pour chaque colonne de l'image (de gauche à droite), le
     * nombre de points à chaque niveau. Rangée en lignes, le niveau le plus
     * haut en premier — prête à dessiner. Taille [colonnes] × [niveaux].
     */
    fun formeDOnde(grille: GrilleLuma, colonnes: Int = 96, niveaux: Int = 51): IntArray {
        val onde = IntArray(colonnes * niveaux)
        for (y in 0 until grille.hauteur) {
            for (x in 0 until grille.largeur) {
                val c = (x * colonnes / grille.largeur).coerceIn(0, colonnes - 1)
                val n = (grille.niveau(x, y) * (niveaux - 1) + 50) / 100
                onde[(niveaux - 1 - n) * colonnes + c]++
            }
        }
        return onde
    }

    /** Le calque du faux contraste : une couleur ARGB par case, transparente au milieu. */
    fun fauxContraste(grille: GrilleLuma): IntArray =
        IntArray(grille.pourcents.size) { COULEUR_PAR_NIVEAU[grille.pourcents[it].toInt().coerceIn(0, 100)] }

    /**
     * Le calque des zébras : rayures montantes au-dessus du seuil haut,
     * descendantes dans la bande du seuil moyen, rien ailleurs.
     */
    fun zebras(grille: GrilleLuma, seuils: SeuilsZebras): IntArray {
        val sortie = IntArray(grille.pourcents.size)
        for (y in 0 until grille.hauteur) {
            for (x in 0 until grille.largeur) {
                val n = grille.niveau(x, y)
                val i = y * grille.largeur + x
                if (n >= seuils.haut) {
                    if ((x + y) % 4 < 2) sortie[i] = ZEBRA_HAUT
                } else if (n >= seuils.moyenBas && n < seuils.moyenHaut) {
                    if ((x - y + 4 * grille.hauteur) % 4 < 2) sortie[i] = ZEBRA_MOYEN
                }
            }
        }
        return sortie
    }

    /** Part de l'image au-dessus d'un niveau, de 0 à 1. */
    fun partAuDessus(grille: GrilleLuma, niveau: Int): Double =
        if (grille.pourcents.isEmpty()) 0.0
        else grille.pourcents.count { it.toInt() >= niveau }.toDouble() / grille.pourcents.size
}
