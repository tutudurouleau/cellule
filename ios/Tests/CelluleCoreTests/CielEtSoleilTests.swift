import XCTest
@testable import CelluleCore

final class SoleilTests: XCTestCase {

    private func jour(_ annee: Int, _ mois: Int, _ jour: Int) -> Int {
        Calendrier.jourDeLAnnee(annee: annee, mois: mois, jour: jour)
    }

    /* Éphéméride du 6 septembre 2026 à Corte (42,30° N — 9,15° E, UTC+2). */
    private var sept6: Int { jour(2026, 9, 6) }

    func testPositionDuSoleilACorte() {
        XCTAssertEqual(sept6, 249)
        XCTAssertEqual(Soleil.declinaison(sept6), 6.5, accuracy: 0.4)
        XCTAssertEqual(Soleil.equationDuTemps(sept6), 1.4, accuracy: 0.4)
        XCTAssertEqual(
            Soleil.midiSolaire(jourDeLAnnee: sept6, longitudeEst: 9.15, decalageFuseau: 2.0),
            13.37, accuracy: 0.06
        )
        XCTAssertEqual(
            Soleil.hauteur(latitudeNord: 42.30, jourDeLAnnee: sept6, tempsSolaire: 12.0),
            54.2, accuracy: 0.4
        )
    }

    func testA17HeuresLeSoleilEstA30DegresPasA40() {
        let tsv = Soleil.tempsSolaire(
            heureLegale: 17.0, jourDeLAnnee: sept6, longitudeEst: 9.15, decalageFuseau: 2.0
        )
        XCTAssertEqual(
            Soleil.hauteur(latitudeNord: 42.30, jourDeLAnnee: sept6, tempsSolaire: tsv),
            30.2, accuracy: 0.5
        )
    }

    func testSolsticesEtEquinoxes() {
        let ete = jour(2026, 6, 21)
        let hiver = jour(2026, 12, 21)
        let equinoxe = jour(2026, 3, 20)
        XCTAssertEqual(Soleil.declinaison(ete), 23.44, accuracy: 0.1)
        XCTAssertEqual(Soleil.declinaison(hiver), -23.44, accuracy: 0.1)
        XCTAssertEqual(Soleil.declinaison(equinoxe), 0.0, accuracy: 0.6)
        XCTAssertEqual(Soleil.hauteur(latitudeNord: 42.30, jourDeLAnnee: ete, tempsSolaire: 12.0), 71.1, accuracy: 0.5)
        XCTAssertEqual(Soleil.hauteur(latitudeNord: 42.30, jourDeLAnnee: hiver, tempsSolaire: 12.0), 24.3, accuracy: 0.5)
        XCTAssertEqual(Soleil.hauteur(latitudeNord: 43.65, jourDeLAnnee: ete, tempsSolaire: 12.0), 69.8, accuracy: 0.5)
        XCTAssertEqual(Soleil.hauteur(latitudeNord: 43.65, jourDeLAnnee: hiver, tempsSolaire: 12.0), 22.9, accuracy: 0.5)
    }

    func testLOmbrePorteeSertDeGoniometre() {
        XCTAssertEqual(Soleil.hauteurDepuisOmbre(1.0), 45.0, accuracy: 0.01)
        XCTAssertEqual(Soleil.hauteurDepuisOmbre(2.0), 26.6, accuracy: 0.1)
        XCTAssertEqual(Soleil.hauteurDepuisOmbre(6.0), 9.5, accuracy: 0.1)
    }
}

final class CielTests: XCTestCase {

    func testLEtalonnageTombeSurSunny16() {
        XCTAssertEqual(Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[0]).ev100, 15.0, accuracy: 0.06)
        XCTAssertEqual(Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[0]).total, 82000.0, accuracy: 3000.0)
    }

    func testLesEtatsDuCielValentCeQueDitLeDocument() {
        let ref = Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[0]).ev100
        // voile : −1
        XCTAssertEqual(Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[1]).ev100 - ref, -1.0, accuracy: 0.2)
        // couvert : −2 à −3
        XCTAssertEqual(Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[3]).ev100 - ref, -2.4, accuracy: 0.45)
        // orage : −4 à −5
        XCTAssertEqual(Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[4]).ev100 - ref, -4.3, accuracy: 0.6)
    }

    func testSousQuinzeDegresLeSinusDevientTropOptimiste() {
        let attendus: [(Double, Double)] = [
            (90.0, 0.42), (60.0, 0.18), (45.0, -0.17), (30.0, -0.77),
            (15.0, -1.98), (10.0, -2.73), (5.0, -4.0)
        ]
        for (h, attendu) in attendus {
            XCTAssertEqual(
                Ciel.eclairement(hauteurDeg: h, ciel: CIELS[0]).ev100 - 15,
                attendu, accuracy: 0.12, "à \(h)°"
            )
        }
        /* À 5°, la table plate du document annonce −3,5 : un demi-diaph de trop. */
        let ecart = (Ciel.eclairement(hauteurDeg: 5.0, ciel: CIELS[0]).ev100 - 15)
            - Ciel.perteHauteurPlate(5.0)
        XCTAssertEqual(ecart, -0.5, accuracy: 0.3)
    }

    func testLaLumiereDecroitToujoursQuandLeSoleilDescend() {
        var precedent = Double.greatestFiniteMagnitude
        var h = 90.0
        while h >= -18.0 {
            let ev = Ciel.eclairement(hauteurDeg: h, ciel: CIELS[0]).ev100
            XCTAssertLessThan(ev, precedent, "remontée à \(h)°")
            precedent = ev
            h -= 0.5
        }
    }

    func testUnObstacleNeCoupeQueLeSoleilQuiExiste() {
        let plein = Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[0])
        let couvert = Ciel.eclairement(hauteurDeg: 52.0, ciel: CIELS[3])
        XCTAssertEqual(plein.partSoleil, 1.0, accuracy: 0.05)
        XCTAssertEqual(couvert.partSoleil, 0.0, accuracy: 1e-9)

        let ombre = OBSTACLES.first { $0.id == "ombre" }!
        let sousbois = OBSTACLES.first { $0.id == "sousbois" }!
        XCTAssertEqual(ombre.diaphs(partSoleil: plein.partSoleil), -2.5, accuracy: 0.05)
        XCTAssertEqual(ombre.diaphs(partSoleil: couvert.partSoleil), 0.0, accuracy: 1e-9)
        /* La canopée, elle, bouche le ciel lui-même. */
        XCTAssertEqual(sousbois.diaphs(partSoleil: couvert.partSoleil), -3.0, accuracy: 0.05)
    }

    func testLeCrepusculeSuitLesReperesDuDocument() {
        XCTAssertEqual(Ciel.luxCrepuscule(-6.0), 3.4, accuracy: 0.2)          // civil
        XCTAssertEqual(Photometrie.evDepuisLux(Ciel.luxCrepuscule(-6.0)), 0.44, accuracy: 0.2)
        XCTAssertEqual(Ciel.luxCrepuscule(-12.0), 0.008, accuracy: 0.002)     // nautique
    }

    func testLaMasseDAirVautUnAuZenith() {
        XCTAssertEqual(Ciel.masseAir(90.0), 1.0, accuracy: 0.001)
        XCTAssertEqual(Ciel.masseAir(45.0), 1.41, accuracy: 0.02)
        XCTAssertGreaterThan(Ciel.masseAir(5.0), 10)
    }
}
