package fr.cellule.core

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/**
 * Position du soleil.
 *
 * Séries de Spencer plutôt que la formule courte en cos(0,98563·(n−173)) :
 * cette dernière dérive de plus d'un degré près des équinoxes, là où la
 * déclinaison change le plus vite.
 */
object Soleil {

    private fun rad(d: Double) = d * PI / 180
    private fun deg(r: Double) = r * 180 / PI

    private fun angleJour(jourDeLAnnee: Int) = 2 * PI * (jourDeLAnnee - 1) / 365.0

    /** Déclinaison solaire, en degrés. */
    fun declinaison(jourDeLAnnee: Int): Double {
        val g = angleJour(jourDeLAnnee)
        return deg(
            0.006918 -
                0.399912 * cos(g) + 0.070257 * sin(g) -
                0.006758 * cos(2 * g) + 0.000907 * sin(2 * g) -
                0.002697 * cos(3 * g) + 0.001480 * sin(3 * g)
        )
    }

    /** Équation du temps, en minutes. */
    fun equationDuTemps(jourDeLAnnee: Int): Double {
        val g = angleJour(jourDeLAnnee)
        return 229.18 * (
            0.000075 +
                0.001868 * cos(g) - 0.032077 * sin(g) -
                0.014615 * cos(2 * g) - 0.040849 * sin(2 * g)
            )
    }

    /** Heure solaire vraie, en heures décimales. */
    fun tempsSolaire(
        heureLegale: Double,
        jourDeLAnnee: Int,
        longitudeEst: Double,
        decalageFuseau: Double
    ): Double = heureLegale - decalageFuseau + longitudeEst / 15 + equationDuTemps(jourDeLAnnee) / 60

    /** Heure légale du midi solaire, en heures décimales. */
    fun midiSolaire(jourDeLAnnee: Int, longitudeEst: Double, decalageFuseau: Double): Double =
        12 + decalageFuseau - longitudeEst / 15 - equationDuTemps(jourDeLAnnee) / 60

    /** Hauteur du soleil au-dessus de l'horizon, en degrés. */
    fun hauteur(latitudeNord: Double, jourDeLAnnee: Int, tempsSolaire: Double): Double {
        val dec = rad(declinaison(jourDeLAnnee))
        val lat = rad(latitudeNord)
        val angleHoraire = rad(15 * (tempsSolaire - 12))
        val s = sin(lat) * sin(dec) + cos(lat) * cos(dec) * cos(angleHoraire)
        return deg(asin(s.coerceIn(-1.0, 1.0)))
    }

    /**
     * Hauteur du soleil déduite de l'ombre portée — le goniomètre qu'on a
     * toujours sur soi. [rapport] est la longueur de l'ombre divisée par la
     * hauteur de l'objet qui la projette.
     */
    fun hauteurDepuisOmbre(rapport: Double): Double =
        deg(kotlin.math.atan(1.0 / rapport.coerceAtLeast(0.001)))
}

data class Lieu(val nom: String, val latitude: Double, val longitude: Double)

val LIEUX = listOf(
    Lieu("Corte (Corse)", 42.30, 9.15),
    Lieu("Auch (Gers)", 43.65, 0.59),
    Lieu("Toulouse", 43.60, 1.44),
    Lieu("Paris", 48.86, 2.35),
    Lieu("Marseille", 43.30, 5.37),
    Lieu("Brest", 48.39, -4.49)
)
