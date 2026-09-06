import XCTest
@testable import CelluleCore

final class PhotometrieTests: XCTestCase {

    func testLesConstantesDuSysteme() {
        XCTAssertEqual(Photometrie.lux(15.0), 81920.0, accuracy: 1.0)
        XCTAssertEqual(Photometrie.cdm2(15.0), 4096.0, accuracy: 1.0)
        XCTAssertEqual(Photometrie.evDepuisLux(82000.0), 15.0, accuracy: 0.02)
        XCTAssertEqual(
            Photometrie.luminance(eclairement: 82000.0, reflectance: Photometrie.gris),
            4698.0, accuracy: 2.0
        )
    }

    func testLEcartEntreCEtKEstStructurelPasUnArrondi() {
        let parIncident = Photometrie.evDepuisLux(82000.0)
        let parReflechi = Photometrie.evDepuisCdm2(
            Photometrie.luminance(eclairement: 82000.0, reflectance: Photometrie.gris)
        )
        XCTAssertEqual(parIncident - parReflechi, -0.2, accuracy: 0.03)
    }

    func testFSur16Au125eDonneBienEv15() {
        XCTAssertEqual(
            Photometrie.evDepuisReglages(ouverture: 16.0, tempsPoseSec: 1.0 / 125),
            14.97, accuracy: 0.02
        )
    }

    func testLaCompensationIso() {
        XCTAssertEqual(Photometrie.decalageIso(400), 2.0, accuracy: 1e-9)
        XCTAssertEqual(Photometrie.decalageIso(50), -1.0, accuracy: 1e-9)
        XCTAssertEqual(Photometrie.decalageIso(800), 3.0, accuracy: 1e-9)
    }

    func testLaTableMaitresseReproduitCelleDuDocument() {
        let attendu: [(Int, String)] = [
            (16, "f/22 · 1/125"), (15, "f/16 · 1/125"), (14, "f/11 · 1/125"),
            (13, "f/8 · 1/125"), (12, "f/5,6 · 1/125"), (11, "f/4 · 1/125"),
            (10, "f/2,8 · 1/125"), (9, "f/2 · 1/125"), (8, "f/2 · 1/60"),
            (7, "f/2 · 1/30"), (6, "f/2 · 1/15"), (5, "f/2 · 1/8"),
            (3, "f/2 · 1/2"), (0, "f/2 · 4 s"), (-3, "f/2 · 30 s"), (-6, "f/2 · 4 min")
        ]
        for (ev, libelle) in attendu {
            let c = coupleDeTable(Double(ev))
            XCTAssertNotNil(c, "à EV \(ev)")
            guard let c = c else { continue }
            XCTAssertEqual(
                "\(libelleOuverture(c.ouverture)) · \(c.vitesse.libelle)",
                libelle, "à EV \(ev)"
            )
        }
    }

    func testLeCasCineDemandeFSur64PasFSur45() {
        let evAppareil = 15.0 + Photometrie.decalageIso(800)
        XCTAssertEqual(evAppareil, 18.0, accuracy: 0.01)
        let exacte = Photometrie.ouverturePour(ev: evAppareil, tempsPoseSec: 1.0 / 50)
        XCTAssertEqual(exacte, 72.4, accuracy: 0.5)
        XCTAssertEqual(ouvertureLaPlusProche(exacte), 64.0, accuracy: 1e-9)
        XCTAssertEqual(
            Photometrie.ouverturePour(ev: evAppareil - 4, tempsPoseSec: 1.0 / 50),
            18.1, accuracy: 0.3
        )
        XCTAssertEqual(
            Photometrie.ouverturePour(ev: evAppareil - 6, tempsPoseSec: 1.0 / 50),
            9.05, accuracy: 0.2
        )
    }

    func testLesCouplesSontCoherentsAvecLeurEv() {
        for c in couples(15.0) {
            XCTAssertEqual(
                Photometrie.evDepuisReglages(ouverture: c.ouvertureExacte, tempsPoseSec: c.vitesse.secondes),
                15.0, accuracy: 1e-9
            )
            /* L'écart annoncé doit correspondre à l'arrondi réellement fait. */
            let evArrondi = Photometrie.evDepuisReglages(
                ouverture: c.ouverture, tempsPoseSec: c.vitesse.secondes
            )
            XCTAssertEqual(c.ecartDiaphs, 15.0 - evArrondi, accuracy: 1e-9, "sur \(c.vitesse.libelle)")
        }
    }

    func testLArrondiAuDiaphPleinNeDepasseJamaisUnSixiemeDeDiaph() {
        for ev in [6.0, 10.0, 15.0, 18.0] {
            for c in couples(ev) {
                XCTAssertLessThanOrEqual(abs(c.ecartDiaphs), 0.35, "\(c.vitesse.libelle) à EV \(ev)")
            }
        }
    }

    func testLeFormatageDesOuvertures() {
        XCTAssertEqual(libelleOuverture(22.0), "f/22")
        XCTAssertEqual(libelleOuverture(5.6), "f/5,6")
        XCTAssertEqual(libelleOuverture(1.4), "f/1,4")
        XCTAssertEqual(libelleOuverture(10.34), "f/10,3")
    }

    func testLesFractionsDeDiaph() {
        XCTAssertEqual(libelleDiaphs(0.0), "juste")
        XCTAssertEqual(libelleDiaphs(-1.0 / 3), "−⅓")
        XCTAssertEqual(libelleDiaphs(2.0 / 3), "+⅔")
        XCTAssertEqual(libelleDiaphs(-4.0 / 3), "−1 ⅓")
        XCTAssertEqual(libelleDiaphs(2.0), "+2")
        XCTAssertEqual(libelleDiaphs(2.24), "+2 ⅓")
    }

    func testLesNiveauxDeLumiereSontNommesAuBonEv() {
        XCTAssertEqual(Situations.nom(15.0), "Plein soleil franc")
        XCTAssertEqual(Situations.nom(14.0), "Plein jour vif")
        XCTAssertEqual(Situations.nom(8.0), "Intérieur très éclairé, vitrine")
        XCTAssertEqual(Situations.nom(0.0), "Crépuscule avancé")
    }
}
