import Foundation

/**
 Mesurer la lumière avec un téléphone.

 La **caméra** donne une mesure *réfléchie*. Quand l'algorithme d'exposition
 a convergé, le triplet (ouverture, temps de pose, sensibilité) qu'il a choisi
 encode exactement la luminance moyenne de ce qu'il vise, puisqu'il l'a placée
 sur le gris moyen. C'est un spotmètre, avec tout ce que ça implique : sur la
 neige il lira 2⅓ diaphs de trop, comme n'importe quelle cellule réfléchie.

 Une mesure *incidente* — celle qui ignore la matière visée — demande un
 organe séparé. Android a le capteur de luminosité ambiante ; iOS n'expose
 aucune API publique pour lui, et la voie incidente y passe donc par la
 caméra frontale et un diffuseur, dont le coefficient se retire du calcul.
 */

/// Le triplet lu dans les métadonnées de capture, à convergence de l'exposition.
struct ExpositionCamera: Equatable {
    let ouverture: Double
    let tempsPoseNs: Int64
    let iso: Int
    /// Correction d'exposition demandée à l'appareil, en diaphs.
    var compensationEv: Double = 0.0

    var tempsPoseSec: Double { Double(tempsPoseNs) / 1_000_000_000.0 }

    /// Ce que valent les réglages, sensibilité mise à part.
    var evAppareil: Double {
        Photometrie.evDepuisReglages(ouverture: ouverture, tempsPoseSec: tempsPoseSec)
    }

    /**
     La luminance de la scène, ramenée à 100 ISO.

     La compensation s'ajoute : demander +1 diaph fait choisir à l'appareil
     une exposition plus généreuse d'un diaph que ce que sa mesure dicte, donc
     des réglages inférieurs d'un diaph à la valeur réellement mesurée.
     */
    var ev100: Double {
        evAppareil - Photometrie.decalageIso(iso) + compensationEv
    }
}

/**
 Décodage du plan de luminance.

 Le plan Y arrive encodé en gamma. Pour moyenner correctement, il faut
 linéariser *avant* de moyenner — moyenner les valeurs gamma puis linéariser
 sous-estime les hautes lumières.

 Sur iOS la plage n'est pas une inconnue comme sur Android : c'est le format
 de pixel demandé à la capture qui la fixe, `420YpCbCr8BiPlanarVideoRange`
 pour 16-235 et `...FullRange` pour 0-255. Le réglage de l'application choisit
 les deux ensemble, si bien qu'ils ne peuvent pas se contredire.
 */
struct DecodeurLuma {

    let plageVideo: Bool
    private let table: [Double]

    init(plageVideo: Bool = true) {
        self.plageVideo = plageVideo
        self.table = (0...255).map { y -> Double in
            let code = plageVideo ? (Double(y) - 16.0) / 219.0 : Double(y) / 255.0
            let c = code.borne(0.0, 1.0)
            return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
        }
    }

    func lineaire(_ y: Int) -> Double { table[y.borne(0, 255)] }

    /**
     Moyenne linéaire d'un disque du plan Y.

     Travaille directement sur les octets fournis par la caméra ; aucune
     dépendance système, donc testable sans appareil.
     */
    func analyser(
        luma: UnsafePointer<UInt8>,
        octets: Int,
        largeur: Int,
        hauteur: Int,
        rowStride: Int,
        pixelStride: Int = 1,
        centreX: Float = 0.5,
        centreY: Float = 0.5,
        rayonRelatif: Float = 0.06
    ) -> ResultatSpot {
        guard largeur > 0, hauteur > 0, octets > 0 else {
            return ResultatSpot(moyenneLineaire: Double.nan, fractionCramee: 0, fractionBouchee: 0, echantillons: 0)
        }

        let cx = Double(centreX) * Double(largeur)
        let cy = Double(centreY) * Double(hauteur)
        let rayon = Double(rayonRelatif) * Double(min(largeur, hauteur))
        let r2 = rayon * rayon

        let xMin = Int(cx - rayon).borne(0, largeur - 1)
        let xMax = Int(cx + rayon).borne(0, largeur - 1)
        let yMin = Int(cy - rayon).borne(0, hauteur - 1)
        let yMax = Int(cy + rayon).borne(0, hauteur - 1)

        /* Au-delà d'une centaine d'échantillons par axe, on ne gagne plus rien
           en précision et on perd des images par seconde. */
        let pas = max(1, (xMax - xMin) / 64)

        var somme = 0.0
        var n = 0
        var cramees = 0
        var bouchees = 0

        var y = yMin
        while y <= yMax {
            let ligne = y * rowStride
            var x = xMin
            while x <= xMax {
                let dx = Double(x) - cx
                let dy = Double(y) - cy
                if dx * dx + dy * dy <= r2 {
                    let index = ligne + x * pixelStride
                    if index >= 0 && index < octets {
                        let v = Int(luma[index])
                        somme += table[v]
                        n += 1
                        if v >= 250 { cramees += 1 }
                        if v <= 6 { bouchees += 1 }
                    }
                }
                x += pas
            }
            y += pas
        }

        if n == 0 {
            /* Disque dégénéré — rayon minuscule, bord de cadre, pas de ligne
               inattendu. On lit le pixel central plutôt que de ne rien rendre. */
            let x = Int(cx).borne(0, largeur - 1)
            let y = Int(cy).borne(0, hauteur - 1)
            let index = y * rowStride + x * pixelStride
            if index >= 0 && index < octets {
                let v = Int(luma[index])
                return ResultatSpot(
                    moyenneLineaire: table[v],
                    fractionCramee: v >= 250 ? 1.0 : 0.0,
                    fractionBouchee: v <= 6 ? 1.0 : 0.0,
                    echantillons: 1
                )
            }
            return ResultatSpot(moyenneLineaire: Double.nan, fractionCramee: 0, fractionBouchee: 0, echantillons: 0)
        }
        return ResultatSpot(
            moyenneLineaire: somme / Double(n),
            fractionCramee: Double(cramees) / Double(n),
            fractionBouchee: Double(bouchees) / Double(n),
            echantillons: n
        )
    }

    /// La même analyse sur un tableau d'octets, pour les tests.
    func analyser(
        luma: [UInt8],
        largeur: Int,
        hauteur: Int,
        rowStride: Int,
        pixelStride: Int = 1,
        centreX: Float = 0.5,
        centreY: Float = 0.5,
        rayonRelatif: Float = 0.06
    ) -> ResultatSpot {
        luma.withUnsafeBufferPointer { tampon in
            guard let base = tampon.baseAddress else {
                return ResultatSpot(moyenneLineaire: Double.nan, fractionCramee: 0, fractionBouchee: 0, echantillons: 0)
            }
            return analyser(
                luma: base, octets: tampon.count,
                largeur: largeur, hauteur: hauteur,
                rowStride: rowStride, pixelStride: pixelStride,
                centreX: centreX, centreY: centreY, rayonRelatif: rayonRelatif
            )
        }
    }
}

struct ResultatSpot: Equatable {
    let moyenneLineaire: Double
    let fractionCramee: Double
    let fractionBouchee: Double
    let echantillons: Int

    /// Au-delà d'un cinquième de pixels butés, la moyenne ne veut plus rien dire.
    var fiable: Bool {
        echantillons > 0 && fractionCramee < 0.2 && fractionBouchee < 0.2
    }
}

/**
 Assemble les métadonnées d'exposition et l'analyse du spot.

 `cibleGris` est la valeur linéaire à laquelle l'algorithme d'exposition place
 le gris moyen — 0,18 en théorie, un peu autre chose sur chaque téléphone.
 `etalonnage` absorbe cette différence, ainsi que la courbe exacte du plan Y :
 c'est le décalage mesuré une fois sur une charte grise.
 */
enum Posemetre {

    static let cibleGrisParDefaut = 0.18

    /// Mesure moyenne de la scène, telle que la donne l'exposition automatique.
    static func ev100Moyen(_ exposition: ExpositionCamera, etalonnage: Double = 0.0) -> Double {
        exposition.ev100 + etalonnage
    }

    /**
     Mesure du spot. La zone visée est rapportée au gris moyen : plus elle
     rend clair dans l'image, plus sa luminance est haute dans la scène.
     */
    static func ev100Spot(
        _ exposition: ExpositionCamera,
        _ spot: ResultatSpot,
        etalonnage: Double = 0.0,
        cibleGris: Double = Posemetre.cibleGrisParDefaut
    ) -> Double {
        if spot.echantillons == 0 || spot.moyenneLineaire.isNaN { return Double.nan }
        /* Une zone entièrement bouchée donne une moyenne nulle, dont le
           logarithme part à l'infini. On la plafonne au premier niveau de
           quantification : c'est faux, mais borné, et le drapeau « non fiable »
           dit déjà de ne pas s'y fier. */
        let moyenne = spot.moyenneLineaire.auMoins(1.0 / 4096)
        return ev100Moyen(exposition, etalonnage: etalonnage) + log2(moyenne / cibleGris)
    }

    /**
     Décalage d'étalonnage à retenir après avoir visé une matière connue.
     Charte grise → reflectanceVisee = 0,18.
     */
    static func etalonnageDepuis(
        _ exposition: ExpositionCamera,
        _ spot: ResultatSpot,
        ev100Vrai: Double,
        reflectanceVisee: Double = Photometrie.gris
    ) -> Double {
        let brut = ev100Spot(exposition, spot, etalonnage: 0.0)
        let ecartMatiere = log2(reflectanceVisee / Photometrie.gris)
        return ev100Vrai + ecartMatiere - brut
    }

    /**
     La voie incidente d'iOS : caméra frontale, écran — donc objectif — tourné
     vers la source, avec un diffuseur devant.

     Une lecture réfléchie suppose toujours viser du gris 18 %. Visant un
     diffuseur de coefficient ρ, elle sur-annonce donc de log₂(ρ/0,18), et
     c'est exactement ce qu'on lui retire pour retrouver l'éclairement qui
     arrive vraiment sur le diffuseur.

     Le coefficient vaut la réflectance d'une carte blanche tenue au sujet, ou
     la transmission d'un dépoli posé sur l'objectif : la géométrie change, le
     calcul non.
     */
    static func ev100Incident(ev100Reflechi: Double, coefficient: Double) -> Double {
        ev100Reflechi - Zones.correctionReflectance(coefficient.borne(0.01, 1.0))
    }

    /// Décalage à mémoriser pour la voie incidente, mesuré contre une cellule de confiance.
    static func etalonnageIncidentDepuis(
        ev100Reflechi: Double,
        coefficient: Double,
        ev100Vrai: Double
    ) -> Double {
        ev100Vrai - ev100Incident(ev100Reflechi: ev100Reflechi, coefficient: coefficient)
    }
}

/**
 Ce qu'on met devant l'objectif pour transformer un spotmètre en cellule à
 dôme. Les valeurs sont des ordres de grandeur : l'étalonnage incident retire
 ce qu'il en reste.
 */
struct Diffuseur: Equatable, Hashable, Identifiable {
    let id: String
    let nom: String
    let detail: String
    let coefficient: Double
}

let DIFFUSEURS: [Diffuseur] = [
    Diffuseur(id: "papier", nom: "Feuille de papier blanc", detail: "posée sur l'objectif, face à la source", coefficient: 0.80),
    Diffuseur(id: "carte", nom: "Carte blanche au sujet", detail: "tenue au sujet, face à l'appareil", coefficient: 0.90),
    Diffuseur(id: "charte", nom: "Charte grise au sujet", detail: "la lecture brute, sans correction", coefficient: 0.18),
    Diffuseur(id: "dome", nom: "Dôme ou dépoli de cellule", detail: "translucide, sur l'objectif", coefficient: 0.55),
    Diffuseur(id: "sac", nom: "Sachet plastique dépoli", detail: "deux épaisseurs sur l'objectif", coefficient: 0.45)
]

/**
 Lecture d'un capteur de luminosité ambiante : la voie incidente d'Android.

 iOS n'expose aucune API publique pour cet organe. Le type reste ici pour que
 les deux portages partagent exactement la même photométrie, et pour le jour
 où une telle API existerait.
 */
struct LectureIncidente: Equatable {
    let lux: Float
    /// Portée maximale annoncée par le capteur, en lux.
    let luxMax: Float
    /// Pas de quantification annoncé, en lux.
    let resolution: Float

    var ev100: Double { Photometrie.evDepuisLux(Double(lux).auMoins(1e-4)) }

    /// Le plein soleil dépasse la plupart de ces capteurs.
    var sature: Bool { luxMax > 0 && lux >= luxMax * 0.95 }

    /// En dessous de quelques pas de quantification, la lecture est du bruit.
    var tropFaible: Bool { resolution > 0 && lux <= resolution * 3 }
}
