import Foundation
import SwiftUI

/**
 Préférences persistées et observables. Volontairement sur `UserDefaults` :
 quelques valeurs, aucune migration, aucune dépendance — l'équivalent exact
 des `SharedPreferences` du portage Android, avec les mêmes clés.
 */
final class Reglages: ObservableObject {

    private let defaut = UserDefaults.standard

    /// Sensibilité du film chargé dans le boîtier, pas celle du téléphone.
    @Published var iso: Int { didSet { defaut.set(iso, forKey: "iso") } }

    /// La pellicule en cours : elle porte l'ISO et la latitude à respecter.
    @Published var pellicule: String { didSet { defaut.set(pellicule, forKey: "pellicule") } }

    /// Vitesse de référence en secondes ; 0 signifie « 1/ISO », la règle du Sunny 16.
    @Published var vitesseReference: Double { didSet { defaut.set(vitesseReference, forKey: "vitesseReference") } }

    /// Décalage mesuré une fois sur charte grise, en diaphs.
    @Published var etalonnage: Double { didSet { defaut.set(etalonnage, forKey: "etalonnage") } }

    /**
     Plage du plan de luminance. Contrairement à Android, ce n'est pas une
     devinette : le réglage choisit le format de pixel demandé à la capture,
     `420YpCbCr8BiPlanarVideoRange` ou `...FullRange`, et le décodeur suit.
     Les deux ne peuvent donc pas se contredire.
     */
    @Published var plageVideo: Bool { didSet { defaut.set(plageVideo, forKey: "plageVideo") } }

    /// Coefficient du diffuseur employé pour la voie incidente.
    @Published var diffuseur: String { didSet { defaut.set(diffuseur, forKey: "diffuseur") } }

    /// Décalage propre à la voie incidente : le chemin optique n'est pas le même.
    @Published var etalonnageIncident: Double { didSet { defaut.set(etalonnageIncident, forKey: "etalonnageIncident") } }

    @Published var latitude: Double { didSet { defaut.set(latitude, forKey: "latitude") } }
    @Published var longitude: Double { didSet { defaut.set(longitude, forKey: "longitude") } }
    @Published var fuseau: Double { didSet { defaut.set(fuseau, forKey: "fuseau") } }

    init() {
        let d = UserDefaults.standard
        func reel(_ cle: String, _ valeurParDefaut: Double) -> Double {
            d.object(forKey: cle) == nil ? valeurParDefaut : d.double(forKey: cle)
        }
        iso = d.object(forKey: "iso") == nil ? 100 : d.integer(forKey: "iso")
        pellicule = d.string(forKey: "pellicule") ?? "Kodak Portra 400"
        vitesseReference = reel("vitesseReference", 0.0)
        etalonnage = reel("etalonnage", 0.0)
        plageVideo = d.object(forKey: "plageVideo") == nil ? true : d.bool(forKey: "plageVideo")
        diffuseur = d.string(forKey: "diffuseur") ?? "papier"
        etalonnageIncident = reel("etalonnageIncident", 0.0)
        latitude = reel("latitude", 42.30)
        longitude = reel("longitude", 9.15)
        fuseau = reel("fuseau", 2.0)
    }

    var diffuseurChoisi: Diffuseur {
        DIFFUSEURS.first { $0.id == diffuseur } ?? DIFFUSEURS[0]
    }
}

/**
 Le carnet, sérialisé en JSON dans les préférences.

 Chaque champ se relit avec un défaut : un carnet écrit par une version
 antérieure de l'application se rouvre sans rien perdre. Un carnet illisible
 ne fait pas perdre l'application — il repart vide plutôt que de planter.
 */
final class DepotJournal: ObservableObject {

    private let cle = "journal"
    private let defaut = UserDefaults.standard

    @Published private(set) var entrees: [EntreeJournal]

    init() {
        if let donnees = UserDefaults.standard.data(forKey: "journal"),
           let lues = try? JSONDecoder().decode([EntreeJournal].self, from: donnees) {
            entrees = lues
        } else {
            entrees = []
        }
    }

    func ajouter(_ entree: EntreeJournal) {
        entrees.append(entree)
        sauver()
    }

    func supprimer(identifiant: Int64) {
        entrees.removeAll { $0.identifiant == identifiant }
        sauver()
    }

    func vider() {
        entrees = []
        sauver()
    }

    func versCsv() -> String {
        var texte = "date;heure;pellicule;vue;sujet;scene;annonce;mesure;ecart;reglage;note\n"
        for e in entrees {
            let champs = [
                e.date, e.heure, e.pellicule, e.vue,
                guillemets(e.sujet), e.typeScene,
                e.annonce.map { String(format: "%.1f", $0) } ?? "",
                e.mesure.map { String(format: "%.1f", $0) } ?? "",
                e.ecart.map { String(format: "%.2f", $0) } ?? "",
                e.reglage, guillemets(e.note)
            ]
            texte += champs.joined(separator: ";") + "\n"
        }
        return texte
    }

    private func guillemets(_ t: String) -> String {
        "\"" + t.replacingOccurrences(of: "\"", with: "\"\"") + "\""
    }

    private func sauver() {
        if let donnees = try? JSONEncoder().encode(entrees) {
            defaut.set(donnees, forKey: cle)
        }
    }
}

/// Ce que les écrans se passent entre eux.
final class EtatApplication: ObservableObject {
    /// Estimation à verser dans la case « annoncé » du carnet.
    @Published var annonceProposee: Double? = nil

    /// Mesure du posemètre à verser dans la case « mesuré ».
    @Published var derniereMesure: Double? = nil

    /// Le couple retenu au moment de la mesure, pour le noter avec la vue.
    @Published var dernierReglage: String = ""

    @Published var derniereEstimation: Double? = nil
    @Published var dernierTypeScene: String = TYPES_DE_SCENE[0]
    @Published var derniereChaine: String = ""
}
