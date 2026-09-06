import SwiftUI
import UIKit

/*
 Un seul accent, ambre, comme l'aiguille d'une cellule ou la lampe d'un
 labo. Le reste est neutre : on lit des chiffres, pas des couleurs.

 Les mêmes valeurs que la version Android, à l'octet près. Elles sont
 déclarées en couleurs dynamiques UIKit plutôt qu'en catalogue d'assets :
 une seule source, lisible, et rien à tenir d'accord dans un fichier binaire.
 */

private extension UIColor {
    convenience init(hexa: UInt32) {
        self.init(
            red: CGFloat((hexa >> 16) & 0xFF) / 255.0,
            green: CGFloat((hexa >> 8) & 0xFF) / 255.0,
            blue: CGFloat(hexa & 0xFF) / 255.0,
            alpha: 1.0
        )
    }
}

private func dynamique(clair: UInt32, sombre: UInt32) -> Color {
    Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(hexa: sombre)
            : UIColor(hexa: clair)
    })
}

enum Teinte {
    static let primaire = dynamique(clair: 0x9C5108, sombre: 0xE8A13C)
    static let surPrimaire = dynamique(clair: 0xFFFFFF, sombre: 0x17140D)
    static let conteneurPrimaire = dynamique(clair: 0xF7E7D3, sombre: 0x3A2C17)
    static let surConteneurPrimaire = dynamique(clair: 0x7A3F06, sombre: 0xF3C075)
    static let fond = dynamique(clair: 0xF5F3EF, sombre: 0x0E0E0D)
    static let surface = dynamique(clair: 0xFFFDFA, sombre: 0x181816)
    static let surSurface = dynamique(clair: 0x17160F, sombre: 0xF2EFE7)
    static let varianteSurface = dynamique(clair: 0xEDE9E1, sombre: 0x232320)
    static let surVarianteSurface = dynamique(clair: 0x6B665C, sombre: 0x9E9889)
    static let contour = dynamique(clair: 0xDCD7CD, sombre: 0x35342E)
    static let contourVariante = dynamique(clair: 0xE8E4DB, sombre: 0x26251F)
    static let erreur = dynamique(clair: 0x9A3B12, sombre: 0xE58B5C)
}

/*
 Chiffres à chasse fixe partout.

 Sans cela, passer de 13,7 à 11,1 change la largeur du nombre, et tout ce qui
 suit se déplace. Un posemètre dont l'affichage sautille à chaque image est
 illisible à bout de bras. `monospacedDigit` est l'équivalent exact du
 réglage OpenType « tnum » demandé côté Android.
 */
enum Police {
    static let chiffreEnorme = Font.system(size: 52, weight: .semibold).monospacedDigit()
    static let chiffreGrand = Font.system(size: 26, weight: .semibold).monospacedDigit()
    static let chiffreMoyen = Font.system(size: 17, weight: .medium).monospacedDigit()
    static let corps = Font.system(size: 15).monospacedDigit()
    static let detail = Font.system(size: 13).monospacedDigit()
    static let etiquette = Font.system(size: 11, weight: .semibold)
    static let titreCarte = Font.system(size: 16, weight: .semibold)
}

/// Rythme vertical unique, pour que rien ne flotte au hasard.
enum Espace {
    static let gouttiere: CGFloat = 14
    static let interligne: CGFloat = 8
}
