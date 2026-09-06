import Foundation

/**
 Les constantes du système photométrique et les conversions entre les trois
 grandeurs qu'il faut cesser de confondre : l'éclairement qui arrive sur une
 surface (lux), la luminance qui en repart (cd/m²), et l'exposition qu'on
 affiche sur l'appareil (EV).

 C = 250 pour la voie incidente, K = 12,5 pour la voie réfléchie. Ces deux
 valeurs ne sont pas exactement cohérentes entre elles : la cohérence
 exigerait C = K·π/ρ ≈ 218. Il reste donc un écart structurel de 0,2 diaph
 entre une cellule incidente et un spotmètre pointé sur une charte grise.
 */
enum Photometrie {

    static let cIncident = 250.0
    static let kReflechi = 12.5
    static let gris = 0.18

    /// Éclairement incident correspondant à un EV, à 100 ISO.
    static func lux(_ ev100: Double) -> Double { 2.5 * pow(2.0, ev100) }

    static func evDepuisLux(_ lux: Double) -> Double { log2(lux / 2.5) }

    /// Luminance d'un gris moyen correspondant à un EV, à 100 ISO.
    static func cdm2(_ ev100: Double) -> Double { 0.125 * pow(2.0, ev100) }

    static func evDepuisCdm2(_ luminance: Double) -> Double { log2(luminance / 0.125) }

    /// L = E·ρ/π — le pont entre ce qui tombe et ce qui repart.
    static func luminance(eclairement: Double, reflectance: Double) -> Double {
        eclairement * reflectance / Double.pi
    }

    /// Décalage en diaphs d'une sensibilité par rapport à 100 ISO.
    static func decalageIso(_ iso: Int) -> Double { log2(Double(iso) / 100.0) }

    /// EV = log₂(N²/t)
    static func evDepuisReglages(ouverture: Double, tempsPoseSec: Double) -> Double {
        log2(ouverture * ouverture / tempsPoseSec)
    }

    /// Ouverture exacte donnant cet EV à cette vitesse.
    static func ouverturePour(ev: Double, tempsPoseSec: Double) -> Double {
        (pow(2.0, ev) * tempsPoseSec).squareRoot()
    }

    /// Temps de pose exact donnant cet EV à cette ouverture.
    static func tempsPosePour(ev: Double, ouverture: Double) -> Double {
        ouverture * ouverture / pow(2.0, ev)
    }
}

/**
 L'arrondi de `Math.round` en Java : la moitié va toujours vers le haut.
 `Foundation.rounded()` écarte du zéro — les deux ne s'accordent pas sur les
 négatifs exactement à la moitié, et le portage doit rendre les mêmes chiffres
 que l'application Android.
 */
func arrondi(_ x: Double) -> Int { Int((x + 0.5).rounded(.down)) }

let OUVERTURES: [Double] = [1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0, 45.0, 64.0]

struct Vitesse: Equatable, Hashable {
    let libelle: String
    let secondes: Double
}

let VITESSES: [Vitesse] = [
    Vitesse(libelle: "1/8000", secondes: 1.0 / 8000), Vitesse(libelle: "1/4000", secondes: 1.0 / 4000),
    Vitesse(libelle: "1/2000", secondes: 1.0 / 2000), Vitesse(libelle: "1/1000", secondes: 1.0 / 1000),
    Vitesse(libelle: "1/500", secondes: 1.0 / 500), Vitesse(libelle: "1/250", secondes: 1.0 / 250),
    Vitesse(libelle: "1/125", secondes: 1.0 / 125), Vitesse(libelle: "1/60", secondes: 1.0 / 60),
    Vitesse(libelle: "1/30", secondes: 1.0 / 30), Vitesse(libelle: "1/15", secondes: 1.0 / 15),
    Vitesse(libelle: "1/8", secondes: 1.0 / 8), Vitesse(libelle: "1/4", secondes: 1.0 / 4),
    Vitesse(libelle: "1/2", secondes: 1.0 / 2),
    Vitesse(libelle: "1 s", secondes: 1.0), Vitesse(libelle: "2 s", secondes: 2.0),
    Vitesse(libelle: "4 s", secondes: 4.0), Vitesse(libelle: "8 s", secondes: 8.0),
    Vitesse(libelle: "15 s", secondes: 15.0), Vitesse(libelle: "30 s", secondes: 30.0),
    Vitesse(libelle: "1 min", secondes: 60.0), Vitesse(libelle: "2 min", secondes: 120.0),
    Vitesse(libelle: "4 min", secondes: 240.0), Vitesse(libelle: "8 min", secondes: 480.0),
    Vitesse(libelle: "15 min", secondes: 900.0), Vitesse(libelle: "30 min", secondes: 1800.0)
]

let SENSIBILITES: [Int] = [25, 50, 64, 100, 125, 160, 200, 400, 800, 1600, 3200, 6400, 12800]

struct FiltreNd: Equatable, Hashable {
    let libelle: String
    let diaphs: Int
}

let FILTRES_ND: [FiltreNd] = [
    FiltreNd(libelle: "Aucun", diaphs: 0), FiltreNd(libelle: "ND2 · 0,3", diaphs: 1),
    FiltreNd(libelle: "ND4 · 0,6", diaphs: 2), FiltreNd(libelle: "ND8 · 0,9", diaphs: 3),
    FiltreNd(libelle: "ND16 · 1,2", diaphs: 4), FiltreNd(libelle: "ND32 · 1,5", diaphs: 5),
    FiltreNd(libelle: "ND64 · 1,8", diaphs: 6), FiltreNd(libelle: "ND256 · 2,4", diaphs: 8),
    FiltreNd(libelle: "ND1000 · 3,0", diaphs: 10)
]

/**
 Un couple diaph/vitesse. `ouvertureExacte` est la valeur que demande le
 calcul, `ouverture` la graduation la plus proche, et `ecartDiaphs` ce que
 coûte l'arrondi — négatif si le cliché sera sous-exposé.
 */
struct Couple: Equatable {
    let vitesse: Vitesse
    let ouvertureExacte: Double
    let ouverture: Double
    let ecartDiaphs: Double
}

func ouvertureLaPlusProche(_ n: Double) -> Double {
    OUVERTURES.min(by: { abs(log2($0) - log2(n)) < abs(log2($1) - log2(n)) }) ?? OUVERTURES[0]
}

func vitesseLaPlusProche(_ secondes: Double) -> Vitesse {
    VITESSES.min(by: {
        abs(log2($0.secondes / secondes)) < abs(log2($1.secondes / secondes))
    }) ?? VITESSES[0]
}

/// Tous les couples praticables pour cet EV appareil.
func couples(_ evAppareil: Double) -> [Couple] {
    VITESSES.compactMap { v in
        let exacte = Photometrie.ouverturePour(ev: evAppareil, tempsPoseSec: v.secondes)
        guard exacte >= 0.9, exacte <= 76.0 else { return nil }
        let proche = ouvertureLaPlusProche(exacte)
        return Couple(
            vitesse: v,
            ouvertureExacte: exacte,
            ouverture: proche,
            ecartDiaphs: -2 * log2(proche / exacte)
        )
    }
}

/// Le couple le plus proche d'une vitesse voulue.
func coupleConseille(evAppareil: Double, vitesseVoulueSec: Double) -> Couple? {
    couples(evAppareil).min(by: {
        abs(log2($0.vitesse.secondes / vitesseVoulueSec))
            < abs(log2($1.vitesse.secondes / vitesseVoulueSec))
    })
}

/**
 Couple de référence pour les tables : on reste dans les ouvertures qu'un
 objectif possède vraiment (f/2 à f/22) et on ne s'écarte du 1/125 que
 lorsqu'il le faut. C'est ce qui reproduit la table du document source.
 */
func coupleDeTable(_ evAppareil: Double) -> Couple? {
    let tous = couples(evAppareil)
    let praticables = tous.filter { $0.ouverture >= 2.0 && $0.ouverture <= 22.0 }
    let pool = praticables.isEmpty ? tous : praticables
    return pool.min(by: {
        abs(log2($0.vitesse.secondes * 125)) < abs(log2($1.vitesse.secondes * 125))
    })
}

/// « f/5,6 » et non « f/5.6 » ni « f/22.0 ».
func libelleOuverture(_ n: Double) -> String {
    let valeur = (Double(arrondi(n * 10))) / 10.0
    let entier = Int(valeur)
    if valeur == Double(entier) { return "f/\(entier)" }
    return "f/" + String(format: "%.1f", valeur).replacingOccurrences(of: ".", with: ",")
}

/// Écart en diaphs, arrondi au tiers, avec les fractions typographiques.
func libelleDiaphs(_ x: Double) -> String {
    if abs(x) < 0.08 { return "juste" }
    let tiers = arrondi(x * 3)
    if tiers == 0 { return "juste" }
    let entier = abs(tiers) / 3
    let reste = abs(tiers) % 3
    let fraction = reste == 1 ? "⅓" : (reste == 2 ? "⅔" : "")
    let corps: String
    if entier > 0 && !fraction.isEmpty {
        corps = "\(entier) \(fraction)"
    } else if entier > 0 {
        corps = "\(entier)"
    } else {
        corps = fraction
    }
    return (x < 0 ? "−" : "+") + corps
}
