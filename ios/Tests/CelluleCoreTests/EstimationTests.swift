import XCTest
@testable import CelluleCore

final class EstimationTests: XCTestCase {

    func testLaSceneParDefautEstSunny16() {
        let e = Estimateur.calculer(Scene())
        XCTAssertEqual(e.ev100Scene, 15.0, accuracy: 0.06)
        let c = coupleConseille(evAppareil: e.evAppareil, vitesseVoulueSec: 1.0 / 100)!
        XCTAssertEqual(c.ouverture, 16.0, accuracy: 1e-9)
        XCTAssertEqual(c.vitesse.libelle, "1/125")
    }

    func testLExempleDeLaRestonica() {
        /* Corte, 6 septembre, 17 h, sous un arbre isolé au bord de l'eau. */
        let jour = Calendrier.jourDeLAnnee(annee: 2026, mois: 9, jour: 6)
        let hauteur = Soleil.hauteur(
            latitudeNord: 42.30,
            jourDeLAnnee: jour,
            tempsSolaire: Soleil.tempsSolaire(
                heureLegale: 17.0, jourDeLAnnee: jour, longitudeEst: 9.15, decalageFuseau: 2.0
            )
        )
        let e = Estimateur.calculer(
            Scene(
                ciel: CIELS[0],
                hauteurSoleil: hauteur,
                obstacle: OBSTACLES.first { $0.id == "arbre" }!,
                renvois: ["eau", "sec"],
                iso: 400
            )
        )
        XCTAssertEqual(e.ev100Scene, 14.1, accuracy: 0.25)
        let c = coupleConseille(evAppareil: e.evAppareil, vitesseVoulueSec: 1.0 / 400)!
        XCTAssertTrue(
            [8.0, 11.0].contains(c.ouverture),
            "attendu f/8 ou f/11, obtenu f/\(c.ouverture)"
        )
    }

    func testLaGrilleMentaleEtLeModelePhysiqueRestentProchesEnPleinSoleil() {
        var base = Scene()
        base.hauteurSoleil = 52.0
        var physique = base; physique.modele = .physique
        var plate = base; plate.modele = .grilleMentale
        let evPhysique = Estimateur.calculer(physique).ev100Scene
        let evPlate = Estimateur.calculer(plate).ev100Scene
        XCTAssertLessThan(
            abs(evPhysique - evPlate), 0.5,
            "physique \(evPhysique) contre plate \(evPlate)"
        )
    }

    func testParTempsCouvertLaHauteurDuSoleilCompteBeaucoupMoins() {
        let couvert = CIELS.first { $0.id == "couvert" }!
        let hautCouvert = Estimateur.calculer(Scene(ciel: couvert, hauteurSoleil: 52.0)).ev100Scene
        let basCouvert = Estimateur.calculer(Scene(ciel: couvert, hauteurSoleil: 20.0)).ev100Scene
        let hautSoleil = Estimateur.calculer(Scene(hauteurSoleil: 52.0)).ev100Scene
        let basSoleil = Estimateur.calculer(Scene(hauteurSoleil: 20.0)).ev100Scene
        /* Le même écart de hauteur coûte bien plus cher sous un ciel pur. */
        XCTAssertGreaterThan(hautSoleil - basSoleil, (hautCouvert - basCouvert) + 0.4)
    }

    func testLaChaineSAfficheDansLOrdreEtSeTermineSurLAppareil() {
        let e = Estimateur.calculer(
            Scene(
                obstacle: OBSTACLES.first { $0.id == "sousbois" }!,
                renvois: ["neige"],
                iso: 400
            )
        )
        XCTAssertEqual(e.termes.first?.nom, "Lumière du ciel")
        XCTAssertEqual(e.termes.last?.nom, "Appareil")
        XCTAssertTrue(e.termes.contains { $0.nom == "Sous-bois dense" })
        XCTAssertTrue(e.termes.contains { $0.nom == "Neige fraîche plein cadre" })
        XCTAssertTrue(e.termes.contains { $0.nom == "400 ISO" })
        /* La somme des termes doit redonner le total affiché. */
        let diaphs = e.termes
            .compactMap { $0.diaphs }
            .filter { $0 != Photometrie.decalageIso(400) }
            .reduce(0, +)
        XCTAssertEqual(e.ev100Ciel + diaphs, e.ev100Scene, accuracy: 1e-9)
    }
}

final class MatieresTests: XCTestCase {

    func testLesEcartsAuGris() {
        XCTAssertEqual(MATIERES.first { $0.nom == "Neige fraîche" }!.ecartAuGris, 2.24, accuracy: 0.02)
        XCTAssertEqual(MATIERES.first { $0.nom == "Peau claire" }!.ecartAuGris, 0.96, accuracy: 0.06)
        XCTAssertEqual(MATIERES.first { $0.reflectance == 0.18 }!.ecartAuGris, 0.0, accuracy: 1e-9)
    }

    func testLaZoneIVaut1Virgule1PourCentEtNon0Virgule6() {
        XCTAssertEqual(ZONES.first { $0.chiffre == "I" }!.reflectance * 100, 1.125, accuracy: 0.01)
        XCTAssertEqual(ZONES.first { $0.chiffre == "V" }!.reflectance * 100, 18.0, accuracy: 0.01)
        XCTAssertEqual(ZONES.first { $0.chiffre == "VII" }!.reflectance * 100, 72.0, accuracy: 0.5)
    }

    func testPlacerUneLectureEnZoneIIIRevientAFermerDeDeuxDiaphs() {
        let zoneIII = ZONES.first { $0.chiffre == "III" }!
        XCTAssertEqual(Zones.expositionPourPlacer(ev100Lu: 12.0, zone: zoneIII), 14.0, accuracy: 1e-9)
    }

    func testLePiegeDeLaNeige() {
        /* Le spot lit 2⅓ diaphs de trop ; suivre la lecture rend la neige grise. */
        let neige = MATIERES.first { $0.nom == "Neige fraîche" }!
        XCTAssertEqual(Zones.correctionReflectance(neige.reflectance), 2.24, accuracy: 0.02)
        XCTAssertEqual(Zones.zoneNaturelle(reflectance: neige.reflectance), 7.2, accuracy: 0.05)
        XCTAssertEqual(
            Zones.zoneLaPlusProche(Zones.zoneNaturelle(reflectance: neige.reflectance)).chiffre,
            "VII"
        )
    }
}
