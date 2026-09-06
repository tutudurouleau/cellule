import Foundation

extension Double {
    /// L'équivalent de `coerceIn` : borne la valeur sans jamais rien inverser.
    func borne(_ bas: Double, _ haut: Double) -> Double {
        Swift.min(Swift.max(self, bas), haut)
    }
    func auMoins(_ bas: Double) -> Double { Swift.max(self, bas) }
}

extension Float {
    func borne(_ bas: Float, _ haut: Float) -> Float {
        Swift.min(Swift.max(self, bas), haut)
    }
}

extension Int {
    func borne(_ bas: Int, _ haut: Int) -> Int {
        Swift.min(Swift.max(self, bas), haut)
    }
}

/**
 Le quantième, calculé sur un calendrier grégorien en temps universel.

 Le fuseau est fixé volontairement : le jour de l'année ne doit pas dépendre
 de l'endroit où tourne le test, sans quoi la déclinaison solaire changerait
 de valeur selon la machine.
 */
enum Calendrier {

    static let gregorien: Calendar = {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone(secondsFromGMT: 0)!
        return c
    }()

    static func jourDeLAnnee(annee: Int, mois: Int, jour: Int) -> Int {
        var composants = DateComponents()
        composants.year = annee
        composants.month = mois
        composants.day = jour
        guard let date = gregorien.date(from: composants),
              let n = gregorien.ordinality(of: .day, in: .year, for: date) else { return 1 }
        return n
    }

    static func jourDeLAnnee(_ date: Date) -> Int {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone.current
        return c.ordinality(of: .day, in: .year, for: date) ?? 1
    }
}
