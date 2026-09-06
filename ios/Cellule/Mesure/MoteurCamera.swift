import AVFoundation
import CoreMedia
import ImageIO
import SwiftUI

/// Ce qu'il faut savoir pour expliquer une lecture qui ne vient pas.
struct Diagnostic: Equatable {
    let largeur: Int
    let hauteur: Int
    let pasDeLigne: Int
    let pasDePixel: Int
    let octets: Int
    let rotation: Int
    let cibleCapteurX: Float
    let cibleCapteurY: Float
    let echantillons: Int
    let sourceMetadonnees: String
}

/**
 La caméra en posemètre réfléchi.

 Sur Android, deux flux séparés concourent à la mesure — l'aperçu porte les
 métadonnées d'exposition, l'analyse porte le plan de luminance — et ils ne se
 correspondent que lorsque l'exposition automatique a convergé.

 iOS retire une bonne part de ce problème. Un seul flux alimente l'aperçu et
 l'analyse, et surtout chaque tampon transporte ses propres métadonnées Exif :
 le temps de pose et la sensibilité lus sont alors ceux de l'image qu'on est
 en train de mesurer, et non l'état courant de l'appareil. Quand l'Exif manque,
 on retombe sur les propriétés de `AVCaptureDevice`, et c'est là seulement que
 `aeStable` redevient une précaution nécessaire.
 */
final class MoteurCamera: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {

    /// Métadonnées de la dernière capture aboutie.
    @Published private(set) var exposition: ExpositionCamera?

    /// Analyse du disque visé.
    @Published private(set) var spot: ResultatSpot?

    /// Moyenne linéaire de tout le cadre — la voie incidente s'en sert.
    @Published private(set) var cadreEntier: ResultatSpot?

    /**
     Proportions de l'image telle qu'elle s'affiche, largeur sur hauteur.

     Le cadre de l'aperçu doit les adopter exactement : si l'image y était
     mise en boîte aux lettres, la position touchée ne correspondrait plus au
     pixel visé, et le spot mesurerait ailleurs que là où on le voit.
     */
    @Published private(set) var rapportApercu: CGFloat = 3.0 / 4.0

    /// Dernier diagnostic d'analyse, pour comprendre une lecture qui refuse.
    @Published private(set) var diagnostic: Diagnostic?

    /// L'exposition automatique a convergé.
    @Published private(set) var aeStable = false

    @Published private(set) var erreur: String?

    @Published private(set) var autorisee = false

    /// Position visée, en coordonnées d'écran normalisées.
    @Published private(set) var cibleU: Float = 0.5
    @Published private(set) var cibleV: Float = 0.5

    @Published private(set) var rayon: Float = 0.055

    /// Lecture figée : ni les métadonnées ni le spot ne bougent plus.
    @Published private(set) var gele = false

    /**
     Rotation à appliquer au tampon pour l'afficher droit.

     L'application est verrouillée en portrait et le capteur travaille en
     paysage : c'est le quart de tour du cas `90` de `Reperes`, celui-là même
     que teste le portage.
     */
    let rotationAnalyse = 90

    let session = AVCaptureSession()

    var plageVideo: Bool = true {
        didSet {
            guard plageVideo != oldValue else { return }
            decodeur = DecodeurLuma(plageVideo: plageVideo)
            fileConfiguration.async { [weak self] in self?.appliquerFormatPixel() }
        }
    }

    private var decodeur = DecodeurLuma(plageVideo: true)
    private let fileConfiguration = DispatchQueue(label: "fr.cellule.camera.configuration")
    private let fileAnalyse = DispatchQueue(label: "fr.cellule.camera.analyse")
    private let sortie = AVCaptureVideoDataOutput()
    private var entree: AVCaptureDeviceInput?
    private var appareil: AVCaptureDevice?
    private var positionCourante: AVCaptureDevice.Position = .back
    private var configuree = false

    /// Repli quand l'appareil ne publie pas son ouverture.
    private var ouvertureParDefaut: Double = 1.8

    /* ── Commandes ────────────────────────────────────────────────────── */

    func viser(u: Float, v: Float) {
        cibleU = u.borne(0, 1)
        cibleV = v.borne(0, 1)
    }

    func reglerRayon(_ r: Float) {
        rayon = r.borne(0.02, 0.30)
    }

    /**
     Fige la lecture. On verrouille aussi l'exposition côté matériel : sans
     cela l'appareil continue de se réajuster et les métadonnées cessent de
     correspondre à l'image analysée.
     */
    func basculerGel() {
        gele.toggle()
        let verrouiller = gele
        fileConfiguration.async { [weak self] in
            guard let appareil = self?.appareil else { return }
            do {
                try appareil.lockForConfiguration()
                if verrouiller {
                    if appareil.isExposureModeSupported(.locked) {
                        appareil.exposureMode = .locked
                    }
                } else if appareil.isExposureModeSupported(.continuousAutoExposure) {
                    appareil.exposureMode = .continuousAutoExposure
                }
                appareil.unlockForConfiguration()
            } catch {
                /* Le gel logiciel suffit ; le verrou matériel n'est qu'un confort. */
            }
        }
    }

    func demanderAutorisation() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            autorisee = true
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] accorde in
                DispatchQueue.main.async { self?.autorisee = accorde }
            }
        default:
            autorisee = false
        }
    }

    /// Démarre — ou bascule d'objectif, ce qui reconfigure la session.
    func demarrer(position: AVCaptureDevice.Position) {
        guard autorisee else { return }
        fileConfiguration.async { [weak self] in
            guard let self = self else { return }
            if self.configuree && self.positionCourante == position {
                if !self.session.isRunning { self.session.startRunning() }
                return
            }
            self.positionCourante = position
            self.configurer(position: position)
            if !self.session.isRunning { self.session.startRunning() }
        }
    }

    func arreter() {
        fileConfiguration.async { [weak self] in
            guard let self = self else { return }
            if self.session.isRunning { self.session.stopRunning() }
        }
    }

    /* ── Configuration ────────────────────────────────────────────────── */

    private func configurer(position: AVCaptureDevice.Position) {
        session.beginConfiguration()
        defer { session.commitConfiguration() }

        /* 720p plutôt que la pleine définition : l'aperçu reste net, et la
           mesure ne demande de toute façon qu'un disque sous-échantillonné. */
        if session.canSetSessionPreset(.hd1280x720) {
            session.sessionPreset = .hd1280x720
        } else {
            session.sessionPreset = .medium
        }

        if let ancienne = entree {
            session.removeInput(ancienne)
            entree = nil
        }

        guard let appareil = AVCaptureDevice.default(
            .builtInWideAngleCamera, for: .video, position: position
        ) else {
            publier { self.erreur = "Aucun objectif \(position == .front ? "frontal" : "arrière") sur cet appareil." }
            return
        }
        self.appareil = appareil

        do {
            let nouvelle = try AVCaptureDeviceInput(device: appareil)
            guard session.canAddInput(nouvelle) else {
                publier { self.erreur = "Caméra indisponible : entrée refusée par la session." }
                return
            }
            session.addInput(nouvelle)
            entree = nouvelle
        } catch {
            publier { self.erreur = "Caméra indisponible : \(error.localizedDescription)" }
            return
        }

        if !session.outputs.contains(sortie) {
            sortie.alwaysDiscardsLateVideoFrames = true
            sortie.setSampleBufferDelegate(self, queue: fileAnalyse)
            if session.canAddOutput(sortie) {
                session.addOutput(sortie)
            } else {
                publier { self.erreur = "Caméra indisponible : sortie vidéo refusée." }
                return
            }
        }
        appliquerFormatPixel()

        if let lien = sortie.connection(with: .video), lien.isVideoMirroringSupported {
            /* Le tampon reste dans l'orientation du capteur : le faire tourner
               coûterait cher par image, alors que `Reperes` défait la rotation
               d'un simple échange de coordonnées. Le miroir de l'objectif
               frontal se désactive de même — mais il faut couper le réglage
               automatique avant d'y toucher, sinon AVFoundation lève. */
            lien.automaticallyAdjustsVideoMirroring = false
            lien.isVideoMirrored = false
        }

        ouvertureParDefaut = Double(appareil.lensAperture) > 0
            ? Double(appareil.lensAperture) : 1.8

        do {
            try appareil.lockForConfiguration()
            if appareil.isExposureModeSupported(.continuousAutoExposure) {
                appareil.exposureMode = .continuousAutoExposure
            }
            appareil.unlockForConfiguration()
        } catch {
            /* L'exposition automatique par défaut fera l'affaire. */
        }

        configuree = true
        publier {
            self.erreur = nil
            self.gele = false
        }
    }

    /**
     La plage du plan Y n'est pas une inconnue sur iOS : c'est le format
     demandé ici qui la fixe. Le décodeur et la capture sont donc réglés
     ensemble, et ne peuvent pas se contredire.
     */
    private func appliquerFormatPixel() {
        let voulu: OSType = plageVideo
            ? kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange
            : kCVPixelFormatType_420YpCbCr8BiPlanarFullRange
        let disponibles = sortie.availableVideoPixelFormatTypes
        let retenu = disponibles.contains(voulu) ? voulu : disponibles.first
        guard let retenu = retenu else { return }
        sortie.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: retenu]
    }

    private func publier(_ bloc: @escaping () -> Void) {
        DispatchQueue.main.async(execute: bloc)
    }

    /* ── Analyse ──────────────────────────────────────────────────────── */

    func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        if gele { return }
        guard let tampon = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let lecture = metadonnees(de: sampleBuffer)

        CVPixelBufferLockBaseAddress(tampon, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(tampon, .readOnly) }

        let largeur = CVPixelBufferGetWidthOfPlane(tampon, 0)
        let hauteur = CVPixelBufferGetHeightOfPlane(tampon, 0)
        let pasDeLigne = CVPixelBufferGetBytesPerRowOfPlane(tampon, 0)
        guard largeur > 0, hauteur > 0,
              let base = CVPixelBufferGetBaseAddressOfPlane(tampon, 0) else { return }
        let luma = base.assumingMemoryBound(to: UInt8.self)
        let octets = pasDeLigne * hauteur

        let (x, y) = Reperes.ecranVersCapteur(u: cibleU, v: cibleV, rotationDegres: rotationAnalyse)
        let resultat = decodeur.analyser(
            luma: luma, octets: octets,
            largeur: largeur, hauteur: hauteur,
            rowStride: pasDeLigne, pixelStride: 1,
            centreX: x, centreY: y,
            rayonRelatif: rayon
        )
        /* Le cadre entier sert la voie incidente : un diffuseur posé sur
           l'objectif remplit toute l'image, il n'y a plus rien à viser. */
        let entier = decodeur.analyser(
            luma: luma, octets: octets,
            largeur: largeur, hauteur: hauteur,
            rowStride: pasDeLigne, pixelStride: 1,
            centreX: 0.5, centreY: 0.5,
            rayonRelatif: 0.45
        )

        let rapport: CGFloat = rotationAnalyse % 180 == 90
            ? CGFloat(hauteur) / CGFloat(largeur)
            : CGFloat(largeur) / CGFloat(hauteur)

        let diag = Diagnostic(
            largeur: largeur, hauteur: hauteur,
            pasDeLigne: pasDeLigne, pasDePixel: 1,
            octets: octets, rotation: rotationAnalyse,
            cibleCapteurX: x, cibleCapteurY: y,
            echantillons: resultat.echantillons,
            sourceMetadonnees: lecture.source
        )

        publier { [weak self] in
            guard let self = self, !self.gele else { return }
            self.exposition = lecture.exposition
            self.aeStable = lecture.stable
            self.spot = resultat
            self.cadreEntier = entier
            self.rapportApercu = rapport
            self.diagnostic = diag
        }
    }

    private struct Metadonnees {
        let exposition: ExpositionCamera?
        let stable: Bool
        let source: String
    }

    /**
     Les métadonnées de l'image, Exif d'abord.

     L'Exif attaché au tampon décrit *cette* image ; les propriétés de
     `AVCaptureDevice` décrivent l'appareil à l'instant où on les lit, ce qui
     n'est pas tout à fait la même chose tant que l'exposition cherche encore.
     */
    private func metadonnees(de tampon: CMSampleBuffer) -> Metadonnees {
        let appareil = self.appareil
        let compensation = Double(appareil?.exposureTargetBias ?? 0)
        let stable = !(appareil?.isAdjustingExposure ?? true)

        if let brut = CMGetAttachment(
            tampon, key: kCGImagePropertyExifDictionary, attachmentModeOut: nil
        ) as? [String: Any] {
            let temps = brut[kCGImagePropertyExifExposureTime as String] as? Double
            let ouverture = brut[kCGImagePropertyExifFNumber as String] as? Double
            var iso: Int?
            if let liste = brut[kCGImagePropertyExifISOSpeedRatings as String] as? [NSNumber] {
                iso = liste.first?.intValue
            } else if let seul = brut[kCGImagePropertyExifISOSpeedRatings as String] as? NSNumber {
                iso = seul.intValue
            }
            if let temps = temps, temps > 0, let iso = iso, iso > 0 {
                return Metadonnees(
                    exposition: ExpositionCamera(
                        ouverture: (ouverture ?? 0) > 0 ? ouverture! : ouvertureParDefaut,
                        tempsPoseNs: Int64(temps * 1e9),
                        iso: iso,
                        compensationEv: compensation
                    ),
                    /* L'Exif décrit l'image elle-même : la lecture vaut ce
                       qu'elle vaut même pendant que l'exposition se cale. */
                    stable: true,
                    source: "Exif de l'image"
                )
            }
        }

        guard let appareil = appareil else {
            return Metadonnees(exposition: nil, stable: false, source: "—")
        }
        let secondes = CMTimeGetSeconds(appareil.exposureDuration)
        let iso = Int(appareil.iso)
        guard secondes > 0, iso > 0 else {
            return Metadonnees(exposition: nil, stable: stable, source: "—")
        }
        return Metadonnees(
            exposition: ExpositionCamera(
                ouverture: Double(appareil.lensAperture) > 0
                    ? Double(appareil.lensAperture) : ouvertureParDefaut,
                tempsPoseNs: Int64(secondes * 1e9),
                iso: iso,
                compensationEv: compensation
            ),
            stable: stable,
            source: "propriétés de l'appareil"
        )
    }
}
