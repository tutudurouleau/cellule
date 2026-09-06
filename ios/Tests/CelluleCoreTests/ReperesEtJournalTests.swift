import XCTest
@testable import CelluleCore

final class ReperesTests: XCTestCase {

    func testSansRotationLeRepereNeBougePas() {
        let (x, y) = Reperes.ecranVersCapteur(u: 0.3, v: 0.7, rotationDegres: 0)
        XCTAssertEqual(x, 0.3, accuracy: 1e-6)
        XCTAssertEqual(y, 0.7, accuracy: 1e-6)
    }

    func testLeCentreResteLeCentreQuelleQueSoitLaRotation() {
        for r in [0, 90, 180, 270] {
            let (x, y) = Reperes.ecranVersCapteur(u: 0.5, v: 0.5, rotationDegres: r)
            XCTAssertEqual(x, 0.5, accuracy: 1e-6, "rotation \(r)")
            XCTAssertEqual(y, 0.5, accuracy: 1e-6, "rotation \(r)")
        }
    }

    func testAllerEtRetourRedonnentLePointDeDepart() {
        for r in [0, 90, 180, 270, 450, -90] {
            for (u, v) in [(Float(0.1), Float(0.2)), (Float(0.8), Float(0.3)), (Float(0), Float(1))] {
                let (x, y) = Reperes.ecranVersCapteur(u: u, v: v, rotationDegres: r)
                let (u2, v2) = Reperes.capteurVersEcran(x: x, y: y, rotationDegres: r)
                XCTAssertEqual(u2, u, accuracy: 1e-6, "rotation \(r)")
                XCTAssertEqual(v2, v, accuracy: 1e-6, "rotation \(r)")
            }
        }
    }

    func testEnPortraitLeHautDeLEcranEstLeBordDroitDuCapteur() {
        /* Rotation 90 : le tampon est en paysage, tourné d'un quart de tour
           pour l'affichage. Viser le haut de l'écran, c'est viser la colonne
           de droite du tampon. */
        let (x, y) = Reperes.ecranVersCapteur(u: 0.5, v: 0.02, rotationDegres: 90)
        XCTAssertEqual(x, 0.02, accuracy: 1e-6)
        XCTAssertTrue(y > 0.4 && y < 0.6)
    }
}

final class JournalTests: XCTestCase {

    private func entree(_ annonce: Double, _ mesure: Double, _ type: String = "Plein soleil") -> EntreeJournal {
        EntreeJournal(
            identifiant: 1, date: "2026-09-06", typeScene: type,
            annonce: annonce, mesure: mesure
        )
    }

    func testUnJournalVideNAPasDeBilan() {
        XCTAssertNil(Statistiques.bilan([]))
    }

    func testLeBiaisEstLEcartMoyenSigne() {
        let b = Statistiques.bilan([entree(15.0, 14.0), entree(13.0, 13.0), entree(12.0, 11.0)])!
        XCTAssertEqual(b.nombre, 3)
        XCTAssertEqual(b.biais, 0.667, accuracy: 0.01)
        XCTAssertEqual(b.erreurAbsolue, 0.667, accuracy: 0.01)
    }

    func testDesErreursOpposeesSAnnulentEnBiaisMaisPasEnAbsolu() {
        let b = Statistiques.bilan([entree(15.0, 14.0), entree(13.0, 14.0)])!
        XCTAssertEqual(b.biais, 0.0, accuracy: 1e-9)
        XCTAssertEqual(b.erreurAbsolue, 1.0, accuracy: 1e-9)
    }

    func testLesProportionsDansLaCible() {
        let b = Statistiques.bilan(
            [entree(15.0, 14.8), entree(13.0, 12.3), entree(12.0, 9.0), entree(10.0, 10.0)]
        )!
        XCTAssertEqual(b.partDansDemiDiaph, 0.5, accuracy: 1e-9)
        XCTAssertEqual(b.partDansUnDiaph, 0.75, accuracy: 1e-9)
    }

    func testLeDecoupageParTypeSortLePlusBiaiseEnPremier() {
        /* Quatre entrées au minimum par type, sinon le bilan refuse de
           conclure — et il a raison de refuser. */
        let entrees = [
            entree(15.0, 15.0, "Plein soleil"), entree(14.0, 14.1, "Plein soleil"),
            entree(13.0, 12.9, "Plein soleil"), entree(15.0, 15.1, "Plein soleil"),
            entree(11.0, 9.0, "Sous-bois"), entree(10.0, 8.2, "Sous-bois"),
            entree(11.5, 9.4, "Sous-bois"), entree(9.0, 7.1, "Sous-bois")
        ]
        let parType = Statistiques.parType(entrees)
        XCTAssertEqual(parType.first?.type, "Sous-bois")
        XCTAssertGreaterThan(parType.first!.bilan.biais, 1.5)
        XCTAssertEqual(parType.last?.bilan.verdict, "calé")
    }

    func testSousQuatreEntreesLeBilanRefuseDeConclure() {
        let b = Statistiques.bilan([entree(15.0, 12.0), entree(14.0, 11.0)])!
        XCTAssertEqual(b.verdict, "trop peu d’entrées")
    }

    func testLeVerdictNommeLeSensDeLErreur() {
        let sousExpose = Statistiques.bilan((0..<5).map { _ in entree(14.0, 12.0) })!
        XCTAssertTrue(sousExpose.verdict.contains("sous-expose"))
        let surExpose = Statistiques.bilan((0..<5).map { _ in entree(12.0, 14.0) })!
        XCTAssertTrue(surExpose.verdict.contains("surexpose"))
    }

    func testUnePhotoNoteeSansExerciceNeFaussePasLeBilan() {
        /* On note une vue sans avoir annoncé : elle appartient au carnet,
           pas à l'entraînement. */
        let photo = EntreeJournal(
            identifiant: 2, date: "2026-09-06", pellicule: "Kodak Portra 400",
            vue: "12", sujet: "La Restonica au soleil rasant", reglage: "f/8 · 1/500"
        )
        XCTAssertNil(photo.ecart)
        XCTAssertFalse(photo.compteAuBilan)
        let bilan = Statistiques.bilan([photo, entree(15.0, 14.0)])!
        XCTAssertEqual(bilan.nombre, 1)
        XCTAssertEqual(bilan.biais, 1.0, accuracy: 1e-9)
        XCTAssertEqual(photo.resume, "vue 12 · Kodak Portra 400 · f/8 · 1/500")
    }

    func testLeNumeroDeVueSuitLaPelliculeChargee() {
        let entrees = [
            EntreeJournal(identifiant: 1, date: "2026-09-06", pellicule: "Tri-X 400", vue: "11"),
            EntreeJournal(identifiant: 2, date: "2026-09-06", pellicule: "Tri-X 400", vue: "12"),
            EntreeJournal(identifiant: 3, date: "2026-09-06", pellicule: "Portra 400", vue: "3")
        ]
        XCTAssertEqual(Statistiques.vueSuivante(entrees, pellicule: "Tri-X 400"), "13")
        XCTAssertEqual(Statistiques.vueSuivante(entrees, pellicule: "Portra 400"), "4")
        /* Une pellicule qu'on vient de charger repart à un. */
        XCTAssertEqual(Statistiques.vueSuivante(entrees, pellicule: "HP5 Plus 400"), "1")
        XCTAssertEqual(Statistiques.vueSuivante([], pellicule: "Tri-X 400"), "1")
    }

    func testChaquePelliculeConnaitSaSensibiliteEtSaLatitude() {
        let portra = PELLICULES.first { $0.nom == "Kodak Portra 400" }!
        XCTAssertEqual(portra.iso, 400)
        XCTAssertEqual(portra.type, .negatifCouleur)
        XCTAssertTrue(conseilLatitude(.negatifCouleur).contains("surexpose"))
        XCTAssertTrue(conseilLatitude(.inversible).contains("ferme"))
        /* Aucune pellicule sans sensibilité plausible. */
        for p in PELLICULES {
            XCTAssertTrue(p.iso >= 25 && p.iso <= 6400, p.nom)
        }
    }

    func testLeTypeDeSceneSuggereSuitLEv() {
        let ouvert = OBSTACLES.first { $0.id == "ouvert" }!
        XCTAssertEqual(Statistiques.typeSuggere(ev100: 15.0, obstacle: ouvert), "Plein soleil")
        XCTAssertEqual(Statistiques.typeSuggere(ev100: 13.0, obstacle: ouvert), "Couvert")
        XCTAssertEqual(Statistiques.typeSuggere(ev100: 8.0, obstacle: ouvert), "Intérieur jour")
        XCTAssertEqual(
            Statistiques.typeSuggere(ev100: 15.0, obstacle: OBSTACLES.first { $0.id == "sousbois" }!),
            "Sous-bois"
        )
    }

    func testLeCarnetSurvitAUnAllerRetourEnJson() {
        /* La persistance passe par Codable : un carnet écrit puis relu doit
           rendre exactement les mêmes vues, valeurs absentes comprises. */
        let avant = [
            EntreeJournal(
                identifiant: 17, date: "2026-09-06", heure: "17:04",
                pellicule: "Kodak Tri-X 400", vue: "12",
                sujet: "La Restonica au soleil rasant", typeScene: "Sous-bois",
                annonce: 11.5, mesure: nil, reglage: "f/8 · 1/500", note: "filtre jaune"
            ),
            EntreeJournal(identifiant: 18, date: "2026-09-07")
        ]
        let donnees = try! JSONEncoder().encode(avant)
        let apres = try! JSONDecoder().decode([EntreeJournal].self, from: donnees)
        XCTAssertEqual(apres, avant)
        XCTAssertNil(apres[0].mesure)
        XCTAssertEqual(apres[0].annonce, 11.5)
    }
}
