import Foundation

/**
 Le tampon d'analyse de la caméra arrive dans l'orientation du capteur, pas
 dans celle de l'écran. Quand on touche l'aperçu pour déplacer le spot, il
 faut donc défaire la rotation d'affichage pour savoir quel coin du tampon
 on vise réellement — sans quoi le spot mesure ailleurs que là où il est
 dessiné, et le posemètre ment sans jamais avoir l'air de se tromper.
 */
enum Reperes {

    /**
     - Parameters:
        - u, v: position normalisée dans l'aperçu affiché, 0..1
        - rotationDegres: rotation à appliquer au tampon pour l'afficher droit
     - Returns: la position normalisée correspondante dans le tampon du capteur
     */
    static func ecranVersCapteur(u: Float, v: Float, rotationDegres: Int) -> (Float, Float) {
        switch ((rotationDegres % 360) + 360) % 360 {
        case 90: return (v, 1 - u)
        case 180: return (1 - u, 1 - v)
        case 270: return (1 - v, u)
        default: return (u, v)
        }
    }

    /// La transformation inverse, pour dessiner le repère par-dessus l'aperçu.
    static func capteurVersEcran(x: Float, y: Float, rotationDegres: Int) -> (Float, Float) {
        switch ((rotationDegres % 360) + 360) % 360 {
        case 90: return (1 - y, x)
        case 180: return (1 - x, 1 - y)
        case 270: return (y, 1 - x)
        default: return (x, y)
        }
    }
}
