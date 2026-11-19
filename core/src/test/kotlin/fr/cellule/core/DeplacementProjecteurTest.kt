package fr.cellule.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeplacementProjecteurTest {

    private val aucun: (String) -> Projecteur? = { null }

    /* Un sujet au centre ; un projecteur 3 m au sud qui le regarde (vers le haut du plan), 1 000 lux à 3 m. */
    private val sujet = ElementSol(1, TypeElement.COMEDIEN, 0.0, 0.0, hauteur = 1.6)
    private val projo = ElementSol(
        2, TypeElement.PROJECTEUR, 0.0, 3.0, rotation = 0.0, hauteur = 1.6,
        luxReference = 1000.0, distanceReference = 3.0
    )
    private val plan = PlanSol(10, "test", elements = listOf(sujet, projo))

    private fun proche(attendu: Double, obtenu: Double, tolerance: Double, message: String = "") =
        assertTrue(abs(attendu - obtenu) <= tolerance, "$message attendu $attendu, obtenu $obtenu")

    @Test
    fun `reculer d'un metre recule dans l'axe`() {
        val d = DeplacementProjecteur.deplacer(plan, projo, sujet, 1.0, GardeHauteur.ANGLE, null, aucun)
        proche(4.0, d.element.y, 1e-9, "y")
        proche(0.0, d.element.x, 1e-9, "x")
        proche(1000.0, d.avant.lux!!, 1e-6)
        proche(1000.0 * 9 / 16, d.apres.lux!!, 1e-6, "1/d²")
    }

    @Test
    fun `en reculant il garde son angle et monte`() {
        val haut = projo.copy(hauteur = 3.6) /* 2 m au-dessus du visage, à 3 m au sol */
        val p = plan.remplacer(haut)
        val d = DeplacementProjecteur.deplacer(p, haut, sujet, 3.0, GardeHauteur.ANGLE, null, aucun)
        proche(1.6 + 2.0 * 6 / 3, d.element.hauteur!!, 0.01, "hauteur")
        proche(d.avant.angleVertical, d.apres.angleVertical, 0.2, "angle")
        val h = DeplacementProjecteur.deplacer(p, haut, sujet, 3.0, GardeHauteur.HAUTEUR, null, aucun)
        assertEquals(3.6, h.element.hauteur)
    }

    @Test
    fun `le plafond arrete la montee`() {
        val haut = projo.copy(hauteur = 3.6)
        val p = plan.remplacer(haut)
        val d = DeplacementProjecteur.deplacer(p, haut, sujet, 3.0, GardeHauteur.ANGLE, 4.0, aucun)
        assertEquals(4.0, d.element.hauteur)
        assertTrue(d.plafondAtteint)
    }

    @Test
    fun `viser des lux trouve la distance`() {
        /* 250 lux = 1 000 / 4 : deux fois plus loin, donc 3 m de recul. */
        val c = DeplacementProjecteur.pourLux(plan, projo, sujet, 250.0, PorteeCible.SEUL, GardeHauteur.ANGLE, null, aucun)
        val d = assertNotNull(c.deplacement, c.probleme)
        proche(3.0, d.recul, 0.02, "recul")
        proche(250.0, d.apres.lux!!, 3.0, "lux")
        /* Et 4 000 lux : moitié moins loin, il avance de 1,5 m. */
        val a = assertNotNull(DeplacementProjecteur.pourLux(plan, projo, sujet, 4000.0, PorteeCible.SEUL, GardeHauteur.ANGLE, null, aucun).deplacement)
        proche(-1.5, a.recul, 0.02, "avance")
    }

    @Test
    fun `viser le total compte les autres projecteurs`() {
        val autre = ElementSol(3, TypeElement.PROJECTEUR, 3.0, 0.0, rotation = 270.0, hauteur = 1.6, luxReference = 200.0, distanceReference = 3.0)
        val p = plan.copy(elements = plan.elements + autre)
        val c = DeplacementProjecteur.pourLux(p, projo, sujet, 450.0, PorteeCible.TOTAL, GardeHauteur.ANGLE, null, aucun)
        val d = assertNotNull(c.deplacement, c.probleme)
        proche(250.0, c.apportVoulu!!, 1e-6, "apport voulu")
        proche(450.0, d.apres.total, 5.0, "total")
        val trop = DeplacementProjecteur.pourLux(p, projo, sujet, 150.0, PorteeCible.TOTAL, GardeHauteur.ANGLE, null, aucun)
        assertNull(trop.deplacement)
        assertTrue(trop.probleme!!.contains("200"))
    }

    @Test
    fun `il ne depasse pas le sujet`() {
        val c = DeplacementProjecteur.pourLux(plan, projo, sujet, 1_000_000.0, PorteeCible.SEUL, GardeHauteur.ANGLE, null, aucun)
        assertNull(c.deplacement)
        assertNotNull(c.probleme)
    }

    @Test
    fun `hors du faisceau on le dit`() {
        val detourne = projo.copy(rotation = 180.0, faisceau = 30.0)
        val p = plan.remplacer(detourne)
        val c = DeplacementProjecteur.pourLux(p, detourne, sujet, 500.0, PorteeCible.SEUL, GardeHauteur.ANGLE, null, aucun)
        assertNull(c.deplacement)
        assertTrue(c.probleme!!.contains("faisceau"))
    }

    @Test
    fun `l'intensite inconnue est signalee`() {
        val inconnu = projo.copy(luxReference = null, distanceReference = null)
        val p = plan.remplacer(inconnu)
        val c = DeplacementProjecteur.pourLux(p, inconnu, sujet, 500.0, PorteeCible.SEUL, GardeHauteur.ANGLE, null, aucun)
        assertNull(c.deplacement)
        assertTrue(c.probleme!!.contains("inconnue"))
    }

    @Test
    fun `la gradation donne la part a garder`() {
        proche(0.25, DeplacementProjecteur.gradation(1000.0, 250.0), 1e-12)
        assertTrue(DeplacementProjecteur.virageAuGradateur(PROJECTEURS_CATALOGUE.first { it.identifiant == "cat-arri-t2" }))
        assertTrue(!DeplacementProjecteur.virageAuGradateur(PROJECTEURS_CATALOGUE.first { it.identifiant == "cat-arri-s60c" }))
    }
}
