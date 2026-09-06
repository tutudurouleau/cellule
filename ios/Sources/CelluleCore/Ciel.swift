import Foundation

/**
 Le modèle d'éclairement.

 Faisceau direct : E₀·sin(h)·τ^(AM^0,678), avec la masse d'air de
 Kasten-Young. C'est ce terme qui s'effondre sous 15° — bien plus vite que le
 simple sin(h) — et qui rend la golden hour si rapide à basculer.

 Ciel diffus : en sin(h)^0,8, beaucoup plus plat. Il devient l'essentiel de la
 lumière dès que le soleil est bas ou caché, et c'est pourquoi la hauteur du
 soleil compte beaucoup moins par temps couvert.

 Étalonnage : ciel pur, soleil à 52° → EV 15,0, c'est-à-dire Sunny 16.
 */
enum Ciel {

    private static let e0 = 128000.0
    private static let partDiffus = 0.155

    /// Part directe d'un plein soleil franc, qui sert de référence à 1.
    private static let partDirecteRef = 0.80

    private static func rad(_ d: Double) -> Double { d * Double.pi / 180 }

    /// Masse d'air relative, formule de Kasten-Young.
    static func masseAir(_ hauteurDeg: Double) -> Double {
        let h = max(hauteurDeg, -0.5)
        return 1.0 / (sin(rad(h)) + 0.50572 * pow(h + 6.07995, -1.6364))
    }

    /* Crépuscule : interpolation logarithmique sur des points d'ancrage mesurés. */
    private static let crepuscule: [(Double, Double)] = [
        (0.0, 400.0), (-2.0, 100.0), (-4.0, 16.0), (-6.0, 3.4), (-8.0, 0.6),
        (-10.0, 0.09), (-12.0, 0.008), (-14.0, 0.002), (-16.0, 0.0016), (-18.0, 0.0015)
    ]

    static func luxCrepuscule(_ hauteurDeg: Double) -> Double {
        if hauteurDeg <= -18 { return 0.0015 }
        for i in 0..<(crepuscule.count - 1) {
            let (ha, la) = crepuscule[i]
            let (hb, lb) = crepuscule[i + 1]
            if hauteurDeg <= ha && hauteurDeg >= hb {
                let f = (ha - hauteurDeg) / (ha - hb)
                return pow(10.0, log10(la) * (1 - f) + log10(lb) * f)
            }
        }
        return 400.0
    }

    static func eclairement(hauteurDeg: Double, ciel: EtatDuCiel) -> Eclairement {
        var direct = 0.0
        var diffus: Double
        if hauteurDeg > 0 {
            let s = sin(rad(hauteurDeg))
            let tau = pow(0.7, pow(masseAir(hauteurDeg), 0.678))
            direct = e0 * s * tau
            diffus = e0 * partDiffus * pow(s, 0.8) + 350
        } else {
            diffus = luxCrepuscule(hauteurDeg)
        }
        direct *= ciel.partDirecte
        diffus *= ciel.partDiffuse
        let total = (direct + diffus).auMoins(1e-6)
        return Eclairement(
            direct: direct,
            diffus: diffus,
            total: total,
            ev100: Photometrie.evDepuisLux(total),
            partSoleil: ((direct / total) / partDirecteRef).borne(0.0, 1.0)
        )
    }

    /**
     Table du document source : perte en diaphs = log₂(sin h). Sert au mode
     « grille mentale », celui qu'on peut tenir de tête.
     */
    private static let tablePlate: [(Double, Double)] = [
        (90.0, 0.0), (60.0, -0.2), (45.0, -0.5), (30.0, -1.0), (20.0, -1.5),
        (15.0, -2.0), (10.0, -2.5), (5.0, -3.5), (0.0, -6.0),
        (-3.0, -9.0), (-6.0, -12.0), (-9.0, -15.0), (-18.0, -24.0)
    ]

    static func perteHauteurPlate(_ hauteurDeg: Double) -> Double {
        if hauteurDeg >= 90 { return 0.0 }
        for i in 0..<(tablePlate.count - 1) {
            let (ha, va) = tablePlate[i]
            let (hb, vb) = tablePlate[i + 1]
            if hauteurDeg <= ha && hauteurDeg >= hb {
                let f = (ha - hauteurDeg) / (ha - hb)
                return va * (1 - f) + vb * f
            }
        }
        return -24.0
    }
}

struct Eclairement: Equatable {
    let direct: Double
    let diffus: Double
    let total: Double
    let ev100: Double
    /// Part de soleil direct, ramenée à 1 pour un plein soleil franc.
    let partSoleil: Double
}

struct EtatDuCiel: Equatable, Hashable, Identifiable {
    let id: String
    let nom: String
    let detail: String
    let partDirecte: Double
    let partDiffuse: Double
    /// Valeur plate du document source, pour le mode grille mentale.
    let plat: Double
}

let CIELS: [EtatDuCiel] = [
    EtatDuCiel(id: "soleil", nom: "Plein soleil", detail: "ombre franche, bord net",
               partDirecte: 1.00, partDiffuse: 1.00, plat: 0.0),
    EtatDuCiel(id: "voile", nom: "Voile léger", detail: "ombre visible mais bord flou",
               partDirecte: 0.30, partDiffuse: 1.45, plat: -1.0),
    EtatDuCiel(id: "blanc", nom: "Couvert clair", detail: "ciel blanc lumineux, sans ombre",
               partDirecte: 0.0, partDiffuse: 1.60, plat: -1.5),
    EtatDuCiel(id: "couvert", nom: "Ciel couvert", detail: "gris uniforme, aucune ombre",
               partDirecte: 0.0, partDiffuse: 1.00, plat: -2.5),
    EtatDuCiel(id: "lourd", nom: "Couvert lourd", detail: "orage, il fait sombre à midi",
               partDirecte: 0.0, partDiffuse: 0.30, plat: -4.5)
]

/**
 Un obstacle ne coupe que ce qui existe. Sans soleil direct, « ombre portée »
 ne veut plus rien dire ; « sous-bois » garde tout son sens, puisque la
 canopée bouche le ciel lui-même. D'où les deux valeurs.
 */
struct Obstacle: Equatable, Hashable, Identifiable {
    let id: String
    let nom: String
    let detail: String
    let auSoleil: Double
    let sousDiffus: Double

    func diaphs(partSoleil: Double) -> Double {
        sousDiffus + (auSoleil - sousDiffus) * partSoleil
    }
}

let OBSTACLES: [Obstacle] = [
    Obstacle(id: "ouvert", nom: "Terrain ouvert", detail: "sujet directement éclairé",
             auSoleil: 0.0, sousDiffus: 0.0),
    Obstacle(id: "ombre", nom: "Ombre portée", detail: "ciel dégagé au-dessus",
             auSoleil: -2.5, sousDiffus: 0.0),
    Obstacle(id: "cj", nom: "Contre-jour", detail: "soleil derrière le sujet",
             auSoleil: -1.5, sousDiffus: 0.0),
    Obstacle(id: "arbre", nom: "Sous un arbre isolé", detail: "feuillage clair, taches de jour",
             auSoleil: -1.5, sousDiffus: -1.0),
    Obstacle(id: "sousbois", nom: "Sous-bois dense", detail: "canopée fermée",
             auSoleil: -3.5, sousDiffus: -3.0),
    Obstacle(id: "versant", nom: "Ombre de versant", detail: "vallée encaissée, relief",
             auSoleil: -2.5, sousDiffus: -0.5),
    Obstacle(id: "rue", nom: "Rue étroite, cour", detail: "ciel réduit à une bande",
             auSoleil: -3.5, sousDiffus: -2.5),
    Obstacle(id: "vitre", nom: "Près de la fenêtre", detail: "intérieur, à un mètre de la vitre",
             auSoleil: -4.5, sousDiffus: -4.0),
    Obstacle(id: "interieur", nom: "Intérieur, fond de pièce", detail: "lumière du jour seule",
             auSoleil: -8.0, sousDiffus: -7.5)
]

/// Surfaces qui renvoient de la lumière sur le sujet — un gain d'éclairement réel.
struct Renvoi: Equatable, Hashable, Identifiable {
    let id: String
    let nom: String
    let diaphs: Double
}

let RENVOIS: [Renvoi] = [
    Renvoi(id: "neige", nom: "Neige fraîche plein cadre", diaphs: 1.5),
    Renvoi(id: "sable", nom: "Sable clair, plage", diaphs: 1.0),
    Renvoi(id: "eau", nom: "Eau au soleil, galets clairs", diaphs: 1.0),
    Renvoi(id: "granite", nom: "Granite clair, calcaire", diaphs: 0.75),
    Renvoi(id: "mur", nom: "Mur blanc proche", diaphs: 0.75),
    Renvoi(id: "altitude", nom: "Altitude > 2 000 m", diaphs: 0.75),
    Renvoi(id: "sec", nom: "Air très sec, méditerranéen", diaphs: 0.4)
]

/**
 Une surface réverbérante rend surtout du soleil direct. Par temps couvert
 elle rend encore, mais nettement moins.
 */
func facteurRenvoi(partSoleil: Double) -> Double { 0.4 + 0.6 * partSoleil }
