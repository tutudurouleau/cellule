import Foundation

/**
 Position du soleil.

 Séries de Spencer plutôt que la formule courte en cos(0,98563·(n−173)) :
 cette dernière dérive de plus d'un degré près des équinoxes, là où la
 déclinaison change le plus vite.
 */
enum Soleil {

    private static func rad(_ d: Double) -> Double { d * Double.pi / 180 }
    private static func deg(_ r: Double) -> Double { r * 180 / Double.pi }

    private static func angleJour(_ jourDeLAnnee: Int) -> Double {
        2 * Double.pi * Double(jourDeLAnnee - 1) / 365.0
    }

    /// Déclinaison solaire, en degrés.
    static func declinaison(_ jourDeLAnnee: Int) -> Double {
        let g = angleJour(jourDeLAnnee)
        return deg(
            0.006918
                - 0.399912 * cos(g) + 0.070257 * sin(g)
                - 0.006758 * cos(2 * g) + 0.000907 * sin(2 * g)
                - 0.002697 * cos(3 * g) + 0.001480 * sin(3 * g)
        )
    }

    /// Équation du temps, en minutes.
    static func equationDuTemps(_ jourDeLAnnee: Int) -> Double {
        let g = angleJour(jourDeLAnnee)
        return 229.18 * (
            0.000075
                + 0.001868 * cos(g) - 0.032077 * sin(g)
                - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g)
        )
    }

    /// Heure solaire vraie, en heures décimales.
    static func tempsSolaire(
        heureLegale: Double,
        jourDeLAnnee: Int,
        longitudeEst: Double,
        decalageFuseau: Double
    ) -> Double {
        heureLegale - decalageFuseau + longitudeEst / 15 + equationDuTemps(jourDeLAnnee) / 60
    }

    /// Heure légale du midi solaire, en heures décimales.
    static func midiSolaire(
        jourDeLAnnee: Int,
        longitudeEst: Double,
        decalageFuseau: Double
    ) -> Double {
        12 + decalageFuseau - longitudeEst / 15 - equationDuTemps(jourDeLAnnee) / 60
    }

    /// Hauteur du soleil au-dessus de l'horizon, en degrés.
    static func hauteur(
        latitudeNord: Double,
        jourDeLAnnee: Int,
        tempsSolaire: Double
    ) -> Double {
        let dec = rad(declinaison(jourDeLAnnee))
        let lat = rad(latitudeNord)
        let angleHoraire = rad(15 * (tempsSolaire - 12))
        let s = sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(angleHoraire)
        return deg(asin(s.borne(-1.0, 1.0)))
    }

    /**
     Hauteur du soleil déduite de l'ombre portée — le goniomètre qu'on a
     toujours sur soi. `rapport` est la longueur de l'ombre divisée par la
     hauteur de l'objet qui la projette.
     */
    static func hauteurDepuisOmbre(_ rapport: Double) -> Double {
        deg(atan(1.0 / rapport.auMoins(0.001)))
    }
}

struct Lieu: Equatable, Hashable {
    let nom: String
    let latitude: Double
    let longitude: Double
}

let LIEUX: [Lieu] = [
    Lieu(nom: "Corte (Corse)", latitude: 42.30, longitude: 9.15),
    Lieu(nom: "Auch (Gers)", latitude: 43.65, longitude: 0.59),
    Lieu(nom: "Toulouse", latitude: 43.60, longitude: 1.44),
    Lieu(nom: "Paris", latitude: 48.86, longitude: 2.35),
    Lieu(nom: "Marseille", latitude: 43.30, longitude: 5.37),
    Lieu(nom: "Brest", latitude: 48.39, longitude: -4.49)
]
