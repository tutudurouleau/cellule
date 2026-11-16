package fr.cellule.core

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/*
 * La géométrie des cartes en tuiles (OpenStreetMap et tous ses cousins) :
 * la projection de Mercator sphérique, découpée en carrés de 256 pixels.
 * Au zoom z, le monde fait 2^z tuiles de côté.
 *
 * Rien d'autre ici que des conversions : latitude/longitude ↔ position dans
 * la grille. Le téléchargement, lui, est l'affaire de l'appli.
 */
object Tuiles {

    const val COTE = 256

    /** Au-delà, la projection de Mercator part à l'infini. */
    const val LATITUDE_MAX = 85.0511

    const val ZOOM_MIN = 2
    const val ZOOM_MAX = 18

    fun cote(zoom: Int): Double = 2.0.pow(zoom)

    /** Abscisse dans la grille, en tuiles (fractionnaire), de 0 à 2^z. */
    fun x(longitude: Double, zoom: Int): Double = (longitude + 180.0) / 360.0 * cote(zoom)

    /** Ordonnée dans la grille, en tuiles (fractionnaire), du nord vers le sud. */
    fun y(latitude: Double, zoom: Int): Double {
        val phi = Math.toRadians(latitude.coerceIn(-LATITUDE_MAX, LATITUDE_MAX))
        return (1 - ln(tan(phi) + 1 / cos(phi)) / PI) / 2 * cote(zoom)
    }

    fun longitude(x: Double, zoom: Int): Double = x / cote(zoom) * 360.0 - 180.0

    fun latitude(y: Double, zoom: Int): Double =
        Math.toDegrees(atan(sinh(PI * (1 - 2 * y / cote(zoom)))))

    /** Le numéro de tuile, ramené dans la grille : la carte reboucle d'est en ouest. */
    fun colonne(x: Double, zoom: Int): Int {
        val n = cote(zoom).toInt()
        return ((floor(x).toInt() % n) + n) % n
    }

    /** Null hors de la grille : il n'y a pas de tuile au-delà des pôles. */
    fun ligne(y: Double, zoom: Int): Int? {
        val l = floor(y).toInt()
        return if (l < 0 || l >= cote(zoom).toInt()) null else l
    }

    /** Mètres par pixel à cette latitude et à ce zoom (équateur de 40 075 km). */
    fun metresParPixel(latitude: Double, zoom: Int): Double =
        40_075_016.686 * cos(Math.toRadians(latitude)) / (COTE * cote(zoom))

    /** L'adresse d'une tuile sur le serveur d'OpenStreetMap. */
    fun adresse(zoom: Int, colonne: Int, ligne: Int): String =
        "https://tile.openstreetmap.org/$zoom/$colonne/$ligne.png"

    /** La mention que la licence d'OpenStreetMap demande d'afficher avec la carte. */
    const val MENTION = "© les contributeurs d'OpenStreetMap"
}

/** Un lieu qu'on a choisi une fois et qu'on veut retrouver. */
data class LieuRecent(val nom: String, val latitude: Double, val longitude: Double) {
    val coordonnees: String get() = LieuPlan(latitude, longitude).coordonnees
}

object Lieux {

    const val MAXIMUM = 8

    /**
     * Lit « 43,6045 1,4440 », « 43.6045, 1.4440 » ou « 43.6045;1.444 » :
     * latitude puis longitude, en degrés décimaux. Null si ce n'en est pas.
     */
    fun lireCoordonnees(texte: String): Pair<Double, Double>? {
        val brut = texte.trim()
        if (brut.isEmpty()) return null
        /* Une virgule suivie d'un espace sépare ; une virgule collée est décimale. */
        val morceaux = brut.replace(";", " ").replace(", ", " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        val nombres = if (morceaux.size == 2) morceaux
        else if (morceaux.size == 1 && brut.count { it == ',' } == 1 && brut.count { it == '.' } == 2) brut.split(',')
        else return null
        val lat = nombres[0].replace(',', '.').toDoubleOrNull() ?: return null
        val lon = nombres[1].replace(',', '.').toDoubleOrNull() ?: return null
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) return null
        return lat to lon
    }

    /** Ajoute un lieu en tête des récents ; un lieu à moins de 200 m d'un autre le remplace. */
    fun ajouter(recents: List<LieuRecent>, lieu: LieuRecent): List<LieuRecent> =
        (listOf(lieu) + recents.filter {
            distanceMetres(it.latitude, it.longitude, lieu.latitude, lieu.longitude) > 200
        }).take(MAXIMUM)

    /**
     * Le décalage horaire le plus probable d'une longitude, à défaut de mieux :
     * un fuseau par tranche de 15°. Les fuseaux légaux s'en écartent souvent —
     * c'est pourquoi l'appli préfère celui du téléphone quand le lieu est proche.
     */
    fun fuseauGeographique(longitude: Double): Double = Math.round(longitude / 15.0).toDouble()
}
