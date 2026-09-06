import Foundation

struct Matiere: Equatable, Hashable, Identifiable {
    let nom: String
    let reflectance: Double
    var id: String { nom }

    /// Écart au gris 18 %, en diaphs.
    var ecartAuGris: Double { log2(reflectance / Photometrie.gris) }
}

let MATIERES: [Matiere] = [
    Matiere(nom: "Neige fraîche", reflectance: 0.85),
    Matiere(nom: "Peinture blanche, mur chaulé", reflectance: 0.75),
    Matiere(nom: "Béton neuf", reflectance: 0.475),
    Matiere(nom: "Sable de désert, plage claire", reflectance: 0.40),
    Matiere(nom: "Peau claire", reflectance: 0.35),
    Matiere(nom: "Granite clair (Restonica)", reflectance: 0.30),
    Matiere(nom: "Herbe verte vive", reflectance: 0.25),
    Matiere(nom: "Béton vieilli, pierre grise", reflectance: 0.25),
    Matiere(nom: "Gris moyen (charte)", reflectance: 0.18),
    Matiere(nom: "Sol nu, terre sèche", reflectance: 0.17),
    Matiere(nom: "Feuillage caduc", reflectance: 0.165),
    Matiere(nom: "Herbe sèche, maquis grillé", reflectance: 0.15),
    Matiere(nom: "Peau foncée", reflectance: 0.125),
    Matiere(nom: "Asphalte usé", reflectance: 0.12),
    Matiere(nom: "Forêt de conifères", reflectance: 0.11),
    Matiere(nom: "Océan, eau profonde (diffus)", reflectance: 0.06),
    Matiere(nom: "Asphalte neuf", reflectance: 0.045),
    Matiere(nom: "Velours noir, charbon", reflectance: 0.015)
]

struct Zone: Equatable, Hashable, Identifiable {
    let chiffre: String
    let ecart: Int
    let rendu: String
    var id: String { chiffre }

    var reflectance: Double { Photometrie.gris * pow(2.0, Double(ecart)) }
}

let ZONES: [Zone] = [
    Zone(chiffre: "0", ecart: -5, rendu: "Noir absolu, aucune densité"),
    Zone(chiffre: "I", ecart: -4, rendu: "Noir, aucune texture"),
    Zone(chiffre: "II", ecart: -3, rendu: "Premier noir texturé"),
    Zone(chiffre: "III", ecart: -2, rendu: "Ombre texturée"),
    Zone(chiffre: "IV", ecart: -1, rendu: "Ombre ouverte, feuillage sombre, peau foncée"),
    Zone(chiffre: "V", ecart: 0, rendu: "Gris moyen — ce que donne toute cellule"),
    Zone(chiffre: "VI", ecart: 1, rendu: "Peau claire, ciel bleu profond"),
    Zone(chiffre: "VII", ecart: 2, rendu: "Blanc texturé, neige à l’ombre"),
    Zone(chiffre: "VIII", ecart: 3, rendu: "Dernier blanc texturé"),
    Zone(chiffre: "IX", ecart: 4, rendu: "Blanc sans texture")
]

enum Zones {

    /// La zone où tombe naturellement une matière.
    static func zoneNaturelle(reflectance: Double) -> Double {
        5 + log2(reflectance / Photometrie.gris)
    }

    static func zoneLaPlusProche(_ valeur: Double) -> Zone {
        ZONES[arrondi(valeur).borne(0, ZONES.count - 1)]
    }

    /**
     Une lecture réfléchie rend toujours la matière visée en zone V. Pour la
     placer ailleurs, on décale l'exposition — et comme un EV plus haut veut
     dire moins de lumière, monter d'une zone fait descendre l'EV d'autant.
     */
    static func expositionPourPlacer(ev100Lu: Double, zone: Zone) -> Double {
        ev100Lu - Double(zone.ecart)
    }

    /**
     La correction à appliquer à une lecture réfléchie pour rendre une matière
     à sa clarté vraie. Sur la neige : +2⅓ diaphs, sans quoi elle sort grise.
     */
    static func correctionReflectance(_ reflectance: Double) -> Double {
        log2(reflectance / Photometrie.gris)
    }
}
