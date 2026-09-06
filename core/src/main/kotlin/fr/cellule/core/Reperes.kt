package fr.cellule.core

/**
 * Le tampon d'analyse de la caméra arrive dans l'orientation du capteur, pas
 * dans celle de l'écran. Quand on touche l'aperçu pour déplacer le spot, il
 * faut donc défaire la rotation d'affichage pour savoir quel coin du tampon
 * on vise réellement — sans quoi le spot mesure ailleurs que là où il est
 * dessiné, et le posemètre ment sans jamais avoir l'air de se tromper.
 */
object Reperes {

    /**
     * @param u, v position normalisée dans l'aperçu affiché, 0..1
     * @param rotationDegres la valeur d'ImageInfo.rotationDegrees
     * @return la position normalisée correspondante dans le tampon du capteur
     */
    fun ecranVersCapteur(u: Float, v: Float, rotationDegres: Int): Pair<Float, Float> =
        when (((rotationDegres % 360) + 360) % 360) {
            90 -> v to (1f - u)
            180 -> (1f - u) to (1f - v)
            270 -> (1f - v) to u
            else -> u to v
        }

    /** La transformation inverse, pour dessiner le repère par-dessus l'aperçu. */
    fun capteurVersEcran(x: Float, y: Float, rotationDegres: Int): Pair<Float, Float> =
        when (((rotationDegres % 360) + 360) % 360) {
            90 -> (1f - y) to x
            180 -> (1f - x) to (1f - y)
            270 -> y to (1f - x)
            else -> x to y
        }
}
