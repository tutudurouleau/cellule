import Foundation

enum Modele: String, CaseIterable, Identifiable {
    case physique
    case grilleMentale
    var id: String { rawValue }
}

struct Scene {
    var ciel: EtatDuCiel = CIELS[0]
    var hauteurSoleil: Double = 52.0
    var obstacle: Obstacle = OBSTACLES[0]
    var renvois: Set<String> = []
    var iso: Int = 100
    var modele: Modele = .physique
}

/// Une ligne de la chaîne, telle qu'on l'annonce à voix haute sur le terrain.
struct Terme: Identifiable {
    let nom: String
    let detail: String
    let diaphs: Double?
    var ev: Double? = nil
    var id: String { nom }
}

struct Estimation {
    let hauteurSoleil: Double
    let ev100Ciel: Double
    let ev100Scene: Double
    let evAppareil: Double
    let partSoleil: Double
    let diaphsObstacle: Double
    let diaphsRenvoi: Double
    let termes: [Terme]
}

enum Estimateur {

    static func calculer(_ scene: Scene) -> Estimation {
        let partSoleil: Double
        let ev100Ciel: Double

        switch scene.modele {
        case .physique:
            let e = Ciel.eclairement(hauteurDeg: scene.hauteurSoleil, ciel: scene.ciel)
            ev100Ciel = e.ev100
            partSoleil = e.partSoleil
        case .grilleMentale:
            ev100Ciel = 15 + scene.ciel.plat + Ciel.perteHauteurPlate(scene.hauteurSoleil)
            partSoleil = scene.ciel.partDirecte > 0 ? 1.0 : 0.0
        }

        let diaphsObstacle: Double
        switch scene.modele {
        case .physique: diaphsObstacle = scene.obstacle.diaphs(partSoleil: partSoleil)
        case .grilleMentale: diaphsObstacle = scene.obstacle.auSoleil
        }

        let facteur: Double
        switch scene.modele {
        case .physique: facteur = facteurRenvoi(partSoleil: partSoleil)
        case .grilleMentale: facteur = 1.0
        }

        let renvoisActifs = RENVOIS.filter { scene.renvois.contains($0.id) }
        let diaphsRenvoi = renvoisActifs.reduce(0.0) { $0 + $1.diaphs * facteur }

        let ev100Scene = ev100Ciel + diaphsObstacle + diaphsRenvoi
        let evAppareil = ev100Scene + Photometrie.decalageIso(scene.iso)

        var termes: [Terme] = []
        termes.append(
            Terme(
                nom: "Lumière du ciel",
                detail: "\(scene.ciel.nom.lowercased()) · soleil à \(arrondiDixieme(scene.hauteurSoleil))°",
                diaphs: nil,
                ev: ev100Ciel
            )
        )
        if scene.obstacle.id != "ouvert" {
            let detail: String
            if scene.modele == .physique && partSoleil < 0.85 {
                detail = "\(scene.obstacle.detail) · il ne reste que \(Int(partSoleil * 100)) % de soleil direct à couper"
            } else {
                detail = scene.obstacle.detail
            }
            termes.append(Terme(nom: scene.obstacle.nom, detail: detail, diaphs: diaphsObstacle))
        }
        for r in renvoisActifs {
            let detail = (scene.modele == .physique && facteur < 0.9)
                ? "atténué : peu de soleil direct" : ""
            termes.append(Terme(nom: r.nom, detail: detail, diaphs: r.diaphs * facteur))
        }
        termes.append(Terme(nom: "Scène", detail: Situations.nom(ev100Scene), diaphs: nil, ev: ev100Scene))
        if scene.iso != 100 {
            termes.append(
                Terme(
                    nom: "\(scene.iso) ISO",
                    detail: "EV = EV₁₀₀ + log₂(ISO/100)",
                    diaphs: Photometrie.decalageIso(scene.iso)
                )
            )
        }
        termes.append(
            Terme(
                nom: "Appareil",
                detail: "ce que doit lire ton couple diaph/vitesse",
                diaphs: nil,
                ev: evAppareil
            )
        )

        return Estimation(
            hauteurSoleil: scene.hauteurSoleil,
            ev100Ciel: ev100Ciel,
            ev100Scene: ev100Scene,
            evAppareil: evAppareil,
            partSoleil: partSoleil,
            diaphsObstacle: diaphsObstacle,
            diaphsRenvoi: diaphsRenvoi,
            termes: termes
        )
    }

    private static func arrondiDixieme(_ x: Double) -> String {
        let v = Double(arrondi(x * 10)) / 10.0
        return String(format: "%.1f", v).replacingOccurrences(of: ".", with: ",")
    }
}

/// Le niveau de lumière, nommé. Décrit une intensité, pas une météo.
enum Situations {

    private static let echelle: [(Double, String)] = [
        (15.5, "Neige ou sable au soleil zénithal"),
        (14.5, "Plein soleil franc"),
        (13.5, "Plein jour vif"),
        (12.5, "Plein jour doux"),
        (11.5, "Jour gris et plat"),
        (10.5, "Jour sombre, ombre ouverte"),
        (9.5, "Jour très sombre, sous-bois"),
        (8.5, "Fin de journée"),
        (7.5, "Intérieur très éclairé, vitrine"),
        (6.5, "Bureau bien éclairé"),
        (5.5, "Salon éclairé"),
        (4.5, "Rue de nuit bien éclairée"),
        (2.5, "Rue de nuit moyenne"),
        (0.5, "Éclairage public faible"),
        (-1.5, "Crépuscule avancé"),
        (-4.5, "Pleine lune sur paysage"),
        (-7.5, "Quart de lune")
    ]

    static func nom(_ ev100: Double) -> String {
        echelle.first(where: { ev100 >= $0.0 })?.1 ?? "Nuit noire, ciel étoilé"
    }
}
