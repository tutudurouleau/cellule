import Foundation

/**
 Le carnet.

 Il sert deux usages qui n'en font qu'un sur le terrain : se souvenir de ce
 qu'on a photographié — pellicule, numéro de vue, sujet, réglage retenu — et
 mesurer son biais d'estimation quand on a pris la peine d'annoncer avant de
 mesurer.

 D'où `annonce` et `mesure` facultatives : on note une photo sans forcément
 faire l'exercice, et l'exercice ne compte que là où les deux chiffres sont là.
 */
struct EntreeJournal: Codable, Identifiable, Equatable {
    let identifiant: Int64
    var date: String
    var heure: String = ""
    var pellicule: String = ""
    var vue: String = ""
    var sujet: String = ""
    var typeScene: String = ""
    var annonce: Double? = nil
    var mesure: Double? = nil
    var reglage: String = ""
    var note: String = ""

    var id: Int64 { identifiant }

    /// Positif : tu as cru qu'il faisait plus clair qu'il ne faisait.
    var ecart: Double? {
        guard let annonce = annonce, let mesure = mesure else { return nil }
        return annonce - mesure
    }

    /// Une entrée compte pour l'entraînement dès que les deux chiffres sont là.
    var compteAuBilan: Bool { ecart != nil }

    var resume: String {
        var morceaux: [String] = []
        if !vue.isEmpty { morceaux.append("vue \(vue)") }
        if !pellicule.isEmpty { morceaux.append(pellicule) }
        if !reglage.isEmpty { morceaux.append(reglage) }
        return morceaux.joined(separator: " · ")
    }
}

let TYPES_DE_SCENE: [String] = [
    "Plein soleil", "Ombre au soleil", "Couvert", "Sous-bois",
    "Intérieur jour", "Intérieur artificiel", "Crépuscule", "Nuit urbaine"
]

/* ── Pellicules ──────────────────────────────────────────────────────────── */

enum TypeFilm: String, CaseIterable {
    case negatifCouleur
    case noirEtBlanc
    case inversible
    case numerique

    var libelle: String {
        switch self {
        case .negatifCouleur: return "Négatif couleur"
        case .noirEtBlanc: return "Négatif N&B"
        case .inversible: return "Inversible"
        case .numerique: return "Numérique"
        }
    }
}

struct Pellicule: Equatable, Hashable, Identifiable {
    let nom: String
    let iso: Int
    let type: TypeFilm
    var id: String { nom }
}

let PELLICULES: [Pellicule] = [
    Pellicule(nom: "Kodak Portra 160", iso: 160, type: .negatifCouleur),
    Pellicule(nom: "Kodak Portra 400", iso: 400, type: .negatifCouleur),
    Pellicule(nom: "Kodak Portra 800", iso: 800, type: .negatifCouleur),
    Pellicule(nom: "Kodak Gold 200", iso: 200, type: .negatifCouleur),
    Pellicule(nom: "Kodak ColorPlus 200", iso: 200, type: .negatifCouleur),
    Pellicule(nom: "Kodak Ultramax 400", iso: 400, type: .negatifCouleur),
    Pellicule(nom: "Kodak Ektar 100", iso: 100, type: .negatifCouleur),
    Pellicule(nom: "Fujifilm C200", iso: 200, type: .negatifCouleur),
    Pellicule(nom: "Fujifilm Superia 400", iso: 400, type: .negatifCouleur),
    Pellicule(nom: "Cinestill 400D", iso: 400, type: .negatifCouleur),
    Pellicule(nom: "Cinestill 800T", iso: 800, type: .negatifCouleur),
    Pellicule(nom: "Lomography 400", iso: 400, type: .negatifCouleur),
    Pellicule(nom: "Kodak Tri-X 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Kodak T-Max 100", iso: 100, type: .noirEtBlanc),
    Pellicule(nom: "Kodak T-Max 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Ilford HP5 Plus 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Ilford FP4 Plus 125", iso: 125, type: .noirEtBlanc),
    Pellicule(nom: "Ilford Delta 100", iso: 100, type: .noirEtBlanc),
    Pellicule(nom: "Ilford Delta 3200", iso: 3200, type: .noirEtBlanc),
    Pellicule(nom: "Ilford XP2 Super 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Kentmere 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Fomapan 100", iso: 100, type: .noirEtBlanc),
    Pellicule(nom: "Fomapan 400", iso: 400, type: .noirEtBlanc),
    Pellicule(nom: "Fujifilm Provia 100F", iso: 100, type: .inversible),
    Pellicule(nom: "Fujifilm Velvia 50", iso: 50, type: .inversible),
    Pellicule(nom: "Fujifilm Velvia 100", iso: 100, type: .inversible),
    Pellicule(nom: "Kodak Ektachrome E100", iso: 100, type: .inversible),
    Pellicule(nom: "Numérique", iso: 100, type: .numerique)
]

/**
 En cas de doute, de quel côté se tromper. L'asymétrie du négatif est réelle
 et large ; celle de l'inversible est inverse et impitoyable.
 */
func conseilLatitude(_ type: TypeFilm) -> String {
    switch type {
    case .negatifCouleur:
        return "En cas de doute, surexpose : +1 à +2 diaphs donnent souvent un meilleur négatif — grain plus fin, ombres ouvertes."
    case .noirEtBlanc:
        return "Expose pour les ombres, développe pour les hautes lumières. Le doute se règle en ouvrant."
    case .inversible:
        return "N'espère rien des hautes lumières : l'écrêtage est définitif. En cas de doute, ferme."
    case .numerique:
        return "Comme de l'inversible : protège les hautes lumières, les ombres se relèvent."
    }
}

/* ── Bilan d'entraînement ────────────────────────────────────────────────── */

struct Bilan: Equatable {
    let nombre: Int
    let biais: Double
    let erreurAbsolue: Double
    let partDansDemiDiaph: Double
    let partDansUnDiaph: Double

    var verdict: String {
        if nombre < 4 { return "trop peu d’entrées" }
        if biais > 0.5 { return "tu ouvres trop peu — tu sous-exposes" }
        if biais < -0.5 { return "tu ouvres trop — tu surexposes" }
        return "calé"
    }
}

/// Un bilan rattaché à son type de scène, pour la liste « où ton œil se trompe ».
struct BilanParType: Identifiable {
    let type: String
    let bilan: Bilan
    var id: String { type }
}

enum Statistiques {

    static func bilan(_ entrees: [EntreeJournal]) -> Bilan? {
        let ecarts = entrees.compactMap { $0.ecart }
        if ecarts.isEmpty { return nil }
        let n = Double(ecarts.count)
        return Bilan(
            nombre: ecarts.count,
            biais: ecarts.reduce(0, +) / n,
            erreurAbsolue: ecarts.reduce(0) { $0 + abs($1) } / n,
            partDansDemiDiaph: Double(ecarts.filter { abs($0) <= 0.5 }.count) / n,
            partDansUnDiaph: Double(ecarts.filter { abs($0) <= 1.0 }.count) / n
        )
    }

    /// Là où l'œil se trompe : un bilan par type de scène, du plus biaisé au moins.
    static func parType(_ entrees: [EntreeJournal]) -> [BilanParType] {
        let retenues = entrees.filter { $0.compteAuBilan && !$0.typeScene.isEmpty }
        var groupes: [String: [EntreeJournal]] = [:]
        var ordre: [String] = []
        for e in retenues {
            if groupes[e.typeScene] == nil { ordre.append(e.typeScene) }
            groupes[e.typeScene, default: []].append(e)
        }
        return ordre
            .compactMap { type in
                bilan(groupes[type] ?? []).map { BilanParType(type: type, bilan: $0) }
            }
            .sorted { abs($0.bilan.biais) > abs($1.bilan.biais) }
    }

    /// Le type de scène que suggère un EV, pour préremplir le carnet.
    static func typeSuggere(ev100: Double, obstacle: Obstacle) -> String {
        if obstacle.id == "sousbois" { return "Sous-bois" }
        if ev100 >= 14.2 { return "Plein soleil" }
        if ev100 >= 12.5 { return "Couvert" }
        if ev100 >= 11.0 { return "Ombre au soleil" }
        if ev100 >= 6.0 { return "Intérieur jour" }
        if ev100 >= 2.0 { return "Crépuscule" }
        return "Nuit urbaine"
    }

    /**
     Le numéro de vue suivant sur la même pellicule. Un carnet de prise de vue
     sans numéro de vue ne sert à rien : c'est lui qui raccroche la note au
     négatif une fois la planche-contact sortie.
     */
    static func vueSuivante(_ entrees: [EntreeJournal], pellicule: String) -> String {
        let numeros = entrees
            .filter { $0.pellicule == pellicule }
            .compactMap { e -> Int? in
                let chiffres = e.vue.trimmingCharacters(in: .whitespaces).prefix { $0.isNumber }
                return Int(chiffres)
            }
        return String((numeros.max() ?? 0) + 1)
    }
}
