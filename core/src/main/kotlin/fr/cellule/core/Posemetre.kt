package fr.cellule.core

import kotlin.math.log2
import kotlin.math.pow

/**
 * Mesurer la lumière avec un téléphone.
 *
 * L'appareil dispose de deux capteurs, et ils ne mesurent pas la même chose.
 *
 * La **caméra** donne une mesure *réfléchie*. Quand l'algorithme d'exposition
 * a convergé, le triplet (ouverture, temps de pose, sensibilité) qu'il a choisi
 * encode exactement la luminance moyenne de ce qu'il vise, puisqu'il l'a placée
 * sur le gris moyen. C'est un spotmètre, avec tout ce que ça implique : sur la
 * neige il lira 2⅓ diaphs de trop, comme n'importe quelle cellule réfléchie.
 *
 * Le **capteur de luminosité ambiante** donne des lux directement, donc une
 * mesure *incidente*, indépendante de la matière visée. C'est la bonne mesure,
 * mais le capteur est grossier : il sature en plein soleil et quantifie mal
 * dans le bas.
 *
 * Les deux sont utiles, et savoir lequel on lit est la moitié du travail.
 */

/** Le triplet lu dans les métadonnées de capture, à convergence de l'exposition. */
data class ExpositionCamera(
    val ouverture: Double,
    val tempsPoseNs: Long,
    val iso: Int,
    /** Correction d'exposition demandée à l'appareil, en diaphs. */
    val compensationEv: Double = 0.0
) {
    val tempsPoseSec: Double get() = tempsPoseNs / 1_000_000_000.0

    /** Ce que valent les réglages, sensibilité mise à part. */
    val evAppareil: Double get() = Photometrie.evDepuisReglages(ouverture, tempsPoseSec)

    /**
     * La luminance de la scène, ramenée à 100 ISO.
     *
     * La compensation s'ajoute : demander +1 diaph fait choisir à l'appareil
     * une exposition plus généreuse d'un diaph que ce que sa mesure dicte, donc
     * des réglages inférieurs d'un diaph à la valeur réellement mesurée.
     */
    val ev100: Double get() = evAppareil - Photometrie.decalageIso(iso) + compensationEv
}

/**
 * Décodage du plan de luminance.
 *
 * Le plan Y d'une image YUV_420_888 est encodé en gamma. Pour moyenner
 * correctement, il faut linéariser *avant* de moyenner — moyenner les valeurs
 * gamma puis linéariser sous-estime les hautes lumières.
 *
 * Deux inconnues subsistent, que l'API Android ne renseigne pas : la plage
 * (vidéo 16-235 ou pleine 0-255) et la courbe exacte (BT.709 plutôt que sRGB,
 * assez proches). L'étalonnage sur charte grise absorbe ce qu'il en reste.
 */
class DecodeurLuma(val plageVideo: Boolean = true) {

    private val table = DoubleArray(256) { lineaireDepuisLuma(it) }

    private fun lineaireDepuisLuma(y: Int): Double {
        val code = if (plageVideo) (y - 16.0) / 219.0 else y / 255.0
        val c = code.coerceIn(0.0, 1.0)
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun lineaire(y: Int): Double = table[y.coerceIn(0, 255)]

    /**
     * Moyenne linéaire d'un disque du plan Y.
     *
     * Travaille directement sur les octets fournis par la caméra ; aucune
     * dépendance Android, donc testable sans appareil.
     */
    fun analyser(
        luma: ByteArray,
        largeur: Int,
        hauteur: Int,
        rowStride: Int,
        pixelStride: Int = 1,
        centreX: Float = 0.5f,
        centreY: Float = 0.5f,
        rayonRelatif: Float = 0.06f
    ): ResultatSpot {
        val cx = centreX * largeur
        val cy = centreY * hauteur
        val rayon = rayonRelatif * minOf(largeur, hauteur)
        val r2 = rayon * rayon

        val xMin = (cx - rayon).toInt().coerceIn(0, largeur - 1)
        val xMax = (cx + rayon).toInt().coerceIn(0, largeur - 1)
        val yMin = (cy - rayon).toInt().coerceIn(0, hauteur - 1)
        val yMax = (cy + rayon).toInt().coerceIn(0, hauteur - 1)

        /* Au-delà d'une centaine d'échantillons par axe, on ne gagne plus rien
           en précision et on perd des images par seconde. */
        val pas = maxOf(1, (xMax - xMin) / 64)

        var somme = 0.0
        var n = 0
        var cramees = 0
        var bouchees = 0

        var y = yMin
        while (y <= yMax) {
            val ligne = y * rowStride
            var x = xMin
            while (x <= xMax) {
                val dx = x - cx
                val dy = y - cy
                if (dx * dx + dy * dy <= r2) {
                    val index = ligne + x * pixelStride
                    if (index in luma.indices) {
                        val v = luma[index].toInt() and 0xFF
                        somme += table[v]
                        n++
                        if (v >= 250) cramees++
                        if (v <= 6) bouchees++
                    }
                }
                x += pas
            }
            y += pas
        }

        return if (n == 0) ResultatSpot(Double.NaN, 0.0, 0.0, 0)
        else ResultatSpot(somme / n, cramees.toDouble() / n, bouchees.toDouble() / n, n)
    }
}

data class ResultatSpot(
    val moyenneLineaire: Double,
    val fractionCramee: Double,
    val fractionBouchee: Double,
    val echantillons: Int
) {
    /** Au-delà d'un cinquième de pixels butés, la moyenne ne veut plus rien dire. */
    val fiable: Boolean get() = echantillons > 0 && fractionCramee < 0.2 && fractionBouchee < 0.2
}

/**
 * Assemble les métadonnées d'exposition et l'analyse du spot.
 *
 * [cibleGris] est la valeur linéaire à laquelle l'algorithme d'exposition place
 * le gris moyen — 0,18 en théorie, un peu autre chose sur chaque téléphone.
 * [etalonnage] absorbe cette différence, ainsi que la plage et la courbe
 * exactes du plan Y : c'est le décalage mesuré une fois sur une charte grise.
 */
object Posemetre {

    const val CIBLE_GRIS = 0.18

    /** Mesure moyenne de la scène, telle que la donne l'exposition automatique. */
    fun ev100Moyen(exposition: ExpositionCamera, etalonnage: Double = 0.0): Double =
        exposition.ev100 + etalonnage

    /**
     * Mesure du spot. La zone visée est rapportée au gris moyen : plus elle
     * rend clair dans l'image, plus sa luminance est haute dans la scène.
     */
    fun ev100Spot(
        exposition: ExpositionCamera,
        spot: ResultatSpot,
        etalonnage: Double = 0.0,
        cibleGris: Double = CIBLE_GRIS
    ): Double {
        if (spot.echantillons == 0 || spot.moyenneLineaire <= 0.0) return Double.NaN
        return ev100Moyen(exposition, etalonnage) + log2(spot.moyenneLineaire / cibleGris)
    }

    /**
     * Décalage d'étalonnage à retenir après avoir visé une matière connue.
     * Charte grise → reflectanceVisee = 0,18.
     */
    fun etalonnageDepuis(
        exposition: ExpositionCamera,
        spot: ResultatSpot,
        ev100Vrai: Double,
        reflectanceVisee: Double = Photometrie.GRIS
    ): Double {
        val brut = Posemetre.ev100Spot(exposition, spot, etalonnage = 0.0)
        val ecartMatiere = log2(reflectanceVisee / Photometrie.GRIS)
        return ev100Vrai + ecartMatiere - brut
    }
}

/** Lecture du capteur de luminosité ambiante : une vraie mesure incidente. */
data class LectureIncidente(
    val lux: Float,
    /** Portée maximale annoncée par le capteur, en lux. */
    val luxMax: Float,
    /** Pas de quantification annoncé, en lux. */
    val resolution: Float
) {
    val ev100: Double get() = Photometrie.evDepuisLux(lux.toDouble().coerceAtLeast(1e-4))

    /** Le plein soleil dépasse la plupart de ces capteurs. */
    val sature: Boolean get() = luxMax > 0f && lux >= luxMax * 0.95f

    /** En dessous de quelques pas de quantification, la lecture est du bruit. */
    val tropFaible: Boolean get() = resolution > 0f && lux <= resolution * 3f
}
