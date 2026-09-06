import XCTest
@testable import CelluleCore

final class PosemetreTests: XCTestCase {

    /// Un plan Y uniforme, tel que la caméra le livrerait.
    private func planUniforme(_ valeur: Int, cote: Int = 160) -> [UInt8] {
        [UInt8](repeating: UInt8(valeur.borne(0, 255)), count: cote * cote)
    }

    /// Encodage sRGB d'une luminance relative, pour fabriquer des cas de test.
    private func lumaPour(_ lineaire: Double, plageVideo: Bool) -> Int {
        let e = lineaire <= 0.0031308
            ? lineaire * 12.92
            : 1.055 * pow(lineaire, 1 / 2.4) - 0.055
        return plageVideo ? arrondi(16 + e * 219) : arrondi(e * 255)
    }

    func testLEvSeDeduitDuTripletDExposition() {
        /* f/1,8 au 1/120 à 100 ISO : ce que donne un téléphone en intérieur. */
        let e = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)
        XCTAssertEqual(e.evAppareil, 8.6, accuracy: 0.05)
        XCTAssertEqual(e.ev100, 8.6, accuracy: 0.05)
    }

    func testLaSensibiliteEstBienRetireeDeLaMesure() {
        /* Mêmes réglages, sensibilité multipliée par huit : la scène est
           trois diaphs plus sombre. */
        let cent = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)
        let huitCents = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 800)
        XCTAssertEqual(cent.ev100 - huitCents.ev100, 3.0, accuracy: 1e-6)
    }

    func testLaCompensationDExpositionSAjouteDansLeBonSens() {
        /* Demander un diaph de plus fait choisir à l'appareil une exposition
           plus généreuse d'un diaph — la scène mesurée, elle, n'a pas bougé. */
        let sansCorrection = ExpositionCamera(ouverture: 16.0, tempsPoseNs: 8_000_000, iso: 100)
        /* Un diaph exact vaut 16/√2, pas f/11 : l'échelle des diaphragmes est
           arrondie, et c'est le calcul qu'on teste ici, pas l'échelle. */
        let avecPlusUn = ExpositionCamera(
            ouverture: 16.0 / 2.0.squareRoot(), tempsPoseNs: 8_000_000, iso: 100, compensationEv: 1.0
        )
        XCTAssertEqual(avecPlusUn.ev100, sansCorrection.ev100, accuracy: 1e-9)

        /* Et une correction négative fait bien descendre la mesure. */
        let avecMoinsUn = ExpositionCamera(
            ouverture: 16.0 * 2.0.squareRoot(), tempsPoseNs: 8_000_000, iso: 100, compensationEv: -1.0
        )
        XCTAssertEqual(avecMoinsUn.ev100, sansCorrection.ev100, accuracy: 1e-9)
    }

    func testLeGrisMoyenSeDecodeA18PourCent() {
        for video in [true, false] {
            let d = DecodeurLuma(plageVideo: video)
            let luma = lumaPour(0.18, plageVideo: video)
            XCTAssertEqual(d.lineaire(luma), 0.18, accuracy: 0.005, "plage vidéo = \(video)")
        }
        /* Le fameux 118 sur 255 en pleine plage. */
        XCTAssertEqual(lumaPour(0.18, plageVideo: false), 118)
    }

    func testLeDecodageEstMonotoneEtBorne() {
        let d = DecodeurLuma()
        XCTAssertEqual(d.lineaire(0), 0.0, accuracy: 1e-9)
        XCTAssertEqual(d.lineaire(255), 1.0, accuracy: 0.01)
        var precedent = -1.0
        for y in 0...255 {
            let v = d.lineaire(y)
            XCTAssertGreaterThanOrEqual(v, precedent, "décroissance à y = \(y)")
            precedent = v
        }
    }

    func testUnSpotSurLeGrisMoyenLitLaMemeChoseQueLaMesureGlobale() {
        let d = DecodeurLuma(plageVideo: true)
        let plan = planUniforme(lumaPour(0.18, plageVideo: true))
        let spot = d.analyser(luma: plan, largeur: 160, hauteur: 160, rowStride: 160)
        let expo = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)
        XCTAssertEqual(
            Posemetre.ev100Spot(expo, spot), Posemetre.ev100Moyen(expo), accuracy: 0.05
        )
    }

    func testUnSpotDeuxDiaphsPlusClairLitDeuxDiaphsDePlus() {
        let d = DecodeurLuma(plageVideo: true)
        let expo = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)
        for diaphs in [-2.0, -1.0, 0.0, 1.0, 2.0] {
            let plan = planUniforme(lumaPour(0.18 * pow(2.0, diaphs), plageVideo: true))
            let spot = d.analyser(luma: plan, largeur: 160, hauteur: 160, rowStride: 160)
            XCTAssertEqual(
                Posemetre.ev100Spot(expo, spot),
                Posemetre.ev100Moyen(expo) + diaphs,
                accuracy: 0.06,
                "à \(diaphs) diaph du gris"
            )
        }
    }

    func testLaMoyenneSeFaitEnLineairePasEnGamma() {
        /* Moitié noir, moitié blanc. En linéaire la moyenne vaut ~0,5 ;
           moyenner les valeurs gamma d'abord donnerait ~0,21 — plus de un
           diaph d'erreur, systématiquement dans les hautes lumières. */
        let d = DecodeurLuma(plageVideo: false)
        let cote = 160
        let plan = (0..<(cote * cote)).map { i -> UInt8 in (i % cote) < cote / 2 ? 0 : 255 }
        let spot = d.analyser(
            luma: plan, largeur: cote, hauteur: cote, rowStride: cote, rayonRelatif: 0.45
        )
        XCTAssertEqual(spot.moyenneLineaire, 0.5, accuracy: 0.05)
    }

    func testUnSpotCrameOuBoucheEstSignaleCommeNonFiable() {
        let d = DecodeurLuma()
        XCTAssertFalse(d.analyser(luma: planUniforme(255), largeur: 160, hauteur: 160, rowStride: 160).fiable)
        XCTAssertFalse(d.analyser(luma: planUniforme(0), largeur: 160, hauteur: 160, rowStride: 160).fiable)
        XCTAssertTrue(d.analyser(luma: planUniforme(120), largeur: 160, hauteur: 160, rowStride: 160).fiable)
    }

    func testLeSpotNeLitQueLeDisqueVise() {
        /* Fond blanc, pastille sombre au centre : le spot doit lire la pastille. */
        let cote = 200
        let d = DecodeurLuma(plageVideo: false)
        let sombre = UInt8(lumaPour(0.045, plageVideo: false))
        let plan = (0..<(cote * cote)).map { i -> UInt8 in
            let x = Double(i % cote)
            let y = Double(i / cote)
            let dx = x - Double(cote) / 2.0
            let dy = y - Double(cote) / 2.0
            return dx * dx + dy * dy < 20.0 * 20.0 ? sombre : 235
        }
        let spot = d.analyser(
            luma: plan, largeur: cote, hauteur: cote, rowStride: cote, rayonRelatif: 0.06
        )
        XCTAssertEqual(spot.moyenneLineaire, 0.045, accuracy: 0.01)
    }

    func testLePasDeLigneDecaleLesLignesCorrectement() {
        /* Un plan avec du remplissage en fin de ligne ne doit pas fausser la lecture. */
        let largeur = 160, hauteur = 160, rowStride = 192
        let d = DecodeurLuma(plageVideo: false)
        let cible = UInt8(lumaPour(0.18, plageVideo: false))
        var plan = [UInt8](repeating: 0, count: rowStride * hauteur)
        for y in 0..<hauteur {
            for x in 0..<largeur { plan[y * rowStride + x] = cible }
        }
        let spot = d.analyser(luma: plan, largeur: largeur, hauteur: hauteur, rowStride: rowStride)
        XCTAssertEqual(spot.moyenneLineaire, 0.18, accuracy: 0.005)
    }

    func testLEtalonnageSurCharteGriseRetrouveLeDecalage() {
        let d = DecodeurLuma(plageVideo: true)
        let expo = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)
        let spot = d.analyser(
            luma: planUniforme(lumaPour(0.18, plageVideo: true)),
            largeur: 160, hauteur: 160, rowStride: 160
        )
        /* On sait par ailleurs que la scène vaut 9,3 EV : l'écart mesuré doit
           être exactement le décalage à mémoriser. */
        let vrai = 9.3
        let offset = Posemetre.etalonnageDepuis(expo, spot, ev100Vrai: vrai)
        XCTAssertEqual(Posemetre.ev100Spot(expo, spot, etalonnage: offset), vrai, accuracy: 0.02)
    }

    func testLeCapteurDAmbianceLitEnIncident() {
        let plein = LectureIncidente(lux: 82000, luxMax: 100000, resolution: 1)
        XCTAssertEqual(plein.ev100, 15.0, accuracy: 0.02)
        XCTAssertFalse(plein.sature)

        /* La plupart des capteurs butent bien avant le plein soleil. */
        let bute = LectureIncidente(lux: 29800, luxMax: 30000, resolution: 1)
        XCTAssertTrue(bute.sature)

        /* Et quantifient trop grossièrement dans le bas. */
        let faible = LectureIncidente(lux: 2, luxMax: 30000, resolution: 1)
        XCTAssertTrue(faible.tropFaible)
    }

    func testCameraEtCapteurDAmbianceConcordentSurUneCharteGrise() {
        /* Une charte grise sous 82 000 lx : la voie incidente donne EV 15,0 ;
           la voie réfléchie, calibrée, doit retomber dessus. */
        let incident = LectureIncidente(lux: 82000, luxMax: 200000, resolution: 1)
        let d = DecodeurLuma(plageVideo: true)
        let spot = d.analyser(
            luma: planUniforme(lumaPour(0.18, plageVideo: true)),
            largeur: 160, hauteur: 160, rowStride: 160
        )
        /* f/1,8 au 1/22000 à 50 ISO, ce qu'exige EV 15 sur un téléphone. */
        let t = Photometrie.tempsPosePour(ev: 15.0 + Photometrie.decalageIso(50), ouverture: 1.8)
        let expo = ExpositionCamera(ouverture: 1.8, tempsPoseNs: Int64(t * 1e9), iso: 50)
        XCTAssertEqual(Posemetre.ev100Spot(expo, spot), incident.ev100, accuracy: 0.06)
    }
}

final class PosemetreRobustesseTests: XCTestCase {

    private let expo = ExpositionCamera(ouverture: 1.8, tempsPoseNs: 8_333_333, iso: 100)

    func testUnDisqueDegenereLitQuandMemeLePixelCentral() {
        let d = DecodeurLuma(plageVideo: false)
        let plan = [UInt8](repeating: 118, count: 64 * 64)
        /* Rayon nul : la boucle ne retient rien, le repli doit prendre le relais. */
        let spot = d.analyser(luma: plan, largeur: 64, hauteur: 64, rowStride: 64, rayonRelatif: 0.0)
        XCTAssertEqual(spot.echantillons, 1)
        XCTAssertEqual(spot.moyenneLineaire, 0.18, accuracy: 0.01)
        XCTAssertFalse(Posemetre.ev100Spot(expo, spot).isNaN)
    }

    func testUneZoneEntierementBoucheeResteBorneeAuLieuDePartirALInfini() {
        let d = DecodeurLuma(plageVideo: true)
        let noir = d.analyser(
            luma: [UInt8](repeating: 0, count: 64 * 64), largeur: 64, hauteur: 64, rowStride: 64
        )
        let ev = Posemetre.ev100Spot(expo, noir)
        XCTAssertFalse(ev.isNaN)
        XCTAssertTrue(ev.isFinite, "EV = \(ev)")
        /* Plafonnée au premier niveau de quantification : douze diaphs sous le gris. */
        XCTAssertEqual(ev, Posemetre.ev100Moyen(expo) - 9.5, accuracy: 0.6)
        XCTAssertFalse(noir.fiable, "la lecture doit rester signalée comme non fiable")
    }

    func testUnCadreEntierementCrameResteLisibleEtSignale() {
        let d = DecodeurLuma(plageVideo: true)
        let blanc = d.analyser(
            luma: [UInt8](repeating: 255, count: 64 * 64), largeur: 64, hauteur: 64, rowStride: 64
        )
        let ev = Posemetre.ev100Spot(expo, blanc)
        XCTAssertTrue(ev.isFinite)
        XCTAssertGreaterThan(ev, Posemetre.ev100Moyen(expo) + 2)
        XCTAssertFalse(blanc.fiable)
    }
}

/**
 La voie incidente propre à iOS : caméra frontale et diffuseur, faute d'API
 publique pour le capteur de luminosité ambiante.
 */
final class IncidentParDiffuseurTests: XCTestCase {

    func testUneCharteGriseNeChangeRienALaLecture() {
        /* Coefficient 0,18 : le diffuseur *est* le gris moyen, la correction
           est nulle et la lecture réfléchie vaut déjà l'incidente. */
        XCTAssertEqual(
            Posemetre.ev100Incident(ev100Reflechi: 12.0, coefficient: 0.18),
            12.0, accuracy: 1e-9
        )
    }

    func testUnDiffuseurClairFaitSurAnnoncerLaCellule() {
        /* Une feuille blanche renvoie bien plus qu'un gris 18 % : la lecture
           brute annonce trop de lumière, et il faut lui retirer log₂(ρ/0,18). */
        let brut = 15.0
        let corrige = Posemetre.ev100Incident(ev100Reflechi: brut, coefficient: 0.80)
        XCTAssertEqual(corrige, brut - 2.152, accuracy: 0.005)
        XCTAssertLessThan(corrige, brut)
    }

    func testLaVoieIncidenteRetrouveLEclairementReel() {
        /* Plein soleil, 82 000 lx, une feuille blanche (ρ = 0,80) posée sur
           l'objectif : la caméra place la feuille sur le gris moyen, donc lit
           2,15 diaphs de trop. Corrigée, elle doit retomber sur EV 15. */
        let eclairementVrai = 82000.0
        let evVrai = Photometrie.evDepuisLux(eclairementVrai)
        let coefficient = 0.80

        /* Ce que la caméra annonce en visant ce diffuseur. */
        let evLu = evVrai + log2(coefficient / Photometrie.gris)

        XCTAssertEqual(
            Posemetre.ev100Incident(ev100Reflechi: evLu, coefficient: coefficient),
            evVrai, accuracy: 1e-9
        )
        XCTAssertEqual(evVrai, 15.0, accuracy: 0.02)
    }

    func testLEtalonnageIncidentAbsorbeLeCoefficientExact() {
        /* On ne connaît jamais le coefficient au centième. L'étalonnage sur
           une cellule de confiance absorbe ce qu'il en reste, exactement comme
           la charte grise absorbe la cible du gris moyen en réfléchi. */
        let evLu = 13.4
        let coefficientSuppose = 0.80
        let evVrai = 11.0
        let offset = Posemetre.etalonnageIncidentDepuis(
            ev100Reflechi: evLu, coefficient: coefficientSuppose, ev100Vrai: evVrai
        )
        let corrige = Posemetre.ev100Incident(
            ev100Reflechi: evLu, coefficient: coefficientSuppose
        ) + offset
        XCTAssertEqual(corrige, evVrai, accuracy: 1e-9)
    }

    func testChaqueDiffuseurAUnCoefficientPlausible() {
        for d in DIFFUSEURS {
            XCTAssertGreaterThan(d.coefficient, 0.05, d.nom)
            XCTAssertLessThanOrEqual(d.coefficient, 1.0, d.nom)
        }
        XCTAssertNotNil(DIFFUSEURS.first { $0.id == "papier" })
    }
}
