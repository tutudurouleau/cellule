package fr.cellule.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/*
 * La vue caméra du plan au sol : une projection en perspective, faite à la
 * main. Le monde est celui du plan (x vers la droite, y vers le bas du
 * plan, en mètres) avec une hauteur z au-dessus du sol. La caméra regarde
 * selon sa rotation sur le plan (lacet) et son inclinaison (tangage).
 */

data class P3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: P3) = P3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: P3) = P3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Double) = P3(x * k, y * k, z * k)
    infix fun scal(o: P3) = x * o.x + y * o.y + z * o.z
}

class CameraVirtuelle(
    val position: P3,
    lacetDeg: Double,
    tangageDeg: Double,
    val champH: Double,
    val champV: Double
) {
    private val l = Math.toRadians(lacetDeg)
    private val t = Math.toRadians(tangageDeg)

    /** Devant, droite, haut : le repère de la caméra exprimé dans le monde. */
    val devant = P3(sin(l) * cos(t), -cos(l) * cos(t), sin(t))
    val droite = P3(cos(l), sin(l), 0.0)
    val haut = P3(-sin(l) * sin(t), cos(l) * sin(t), cos(t))

    private val kx = 1 / tan(Math.toRadians(champH / 2))
    private val ky = 1 / tan(Math.toRadians(champV / 2))

    /** Un point du monde dans le repère de la caméra : (droite, haut, profondeur). */
    fun versCamera(p: P3): P3 {
        val v = p - position
        return P3(v scal droite, v scal haut, v scal devant)
    }

    /** Position à l'écran d'un point du repère caméra (profondeur > 0), en pixels. */
    fun ecran(c: P3, largeur: Double, hauteur: Double): Pair<Double, Double> =
        (largeur / 2 + c.x / c.z * kx * largeur / 2) to (hauteur / 2 - c.y / c.z * ky * hauteur / 2)

    /** Taille à l'écran d'une longueur [r] (en mètres) vue à la profondeur [z]. */
    fun echelle(r: Double, z: Double, largeur: Double): Double = r / z * kx * largeur / 2

    companion object {
        /** La caméra d'un élément du plan ; null si sa focale ou son capteur manque. */
        fun depuis(e: ElementSol): CameraVirtuelle? {
            val h = Sol.champHorizontal(e) ?: return null
            val v = Sol.champVertical(e) ?: return null
            return CameraVirtuelle(P3(e.x, e.y, e.hauteur ?: Sol.HAUTEUR_CAMERA), e.rotation, e.inclinaison, h, v)
        }
    }
}

object Rendu3D {
    /** Le plan de coupe avant, en mètres : rien de plus proche ne se dessine. */
    const val PROCHE = 0.05

    /** Coupe un polygone (repère caméra) par le plan de profondeur [PROCHE]. */
    fun couper(poly: List<P3>): List<P3> {
        if (poly.isEmpty()) return poly
        val sortie = mutableListOf<P3>()
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val aDedans = a.z >= PROCHE
            val bDedans = b.z >= PROCHE
            if (aDedans) sortie.add(a)
            if (aDedans != bDedans) {
                val k = (PROCHE - a.z) / (b.z - a.z)
                sortie.add(a + (b - a) * k)
            }
        }
        return sortie
    }

    /** Coupe un segment (repère caméra) ; null s'il est entièrement derrière. */
    fun couperSegment(a: P3, b: P3): Pair<P3, P3>? {
        if (a.z < PROCHE && b.z < PROCHE) return null
        if (a.z >= PROCHE && b.z >= PROCHE) return a to b
        val k = (PROCHE - a.z) / (b.z - a.z)
        val m = a + (b - a) * k
        return if (a.z < PROCHE) m to b else a to m
    }

    /** Les quatre coins au sol d'un rectangle du plan (centre, largeur, profondeur, rotation). */
    fun coins(e: ElementSol, largeur: Double = e.largeur, profondeur: Double = e.profondeur): List<Pair<Double, Double>> {
        val r = Math.toRadians(e.rotation)
        val ux = cos(r); val uy = sin(r)
        val vx = -sin(r); val vy = cos(r)
        val l = largeur / 2; val p = profondeur / 2
        return listOf(-l to -p, l to -p, l to p, -l to p).map { (a, b) -> (e.x + a * ux + b * vx) to (e.y + a * uy + b * vy) }
    }

    /** Les deux bouts d'un élément en trait. */
    fun bouts(e: ElementSol): Pair<P3, P3> {
        val r = Math.toRadians(e.rotation)
        val dx = cos(r) * e.largeur / 2
        val dy = sin(r) * e.largeur / 2
        return P3(e.x - dx, e.y - dy, 0.0) to P3(e.x + dx, e.y + dy, 0.0)
    }
}
