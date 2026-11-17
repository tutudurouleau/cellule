package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlanSolTest {

    private fun el(id: Long, type: TypeElement, x: Double, y: Double, rot: Double = 0.0, nom: String = "", etape: Int = 1) =
        ElementSol(id, type, x, y, rot, nom = nom, etape = etape)

    @Test
    fun directionsEtRotations() {
        val (x, y) = Sol.direction(90.0)
        assertEquals(1.0, x, 1e-9)
        assertEquals(0.0, y, 1e-9)
        assertEquals(180.0, Sol.rotationVers(0.0, 0.0, 0.0, 5.0), 1e-9)
        assertEquals(270.0, Sol.rotationVers(0.0, 0.0, -2.0, 0.0), 1e-9)
        assertEquals(20.0, Sol.ecartAngulaire(350.0, 10.0), 1e-9)
    }

    @Test
    fun apportDUnProjecteur() {
        val sujet = el(1, TypeElement.COMEDIEN, 0.0, 0.0, rot = 0.0).copy(hauteur = 1.6)
        /* Projecteur 3 m au-dessus… non : à 3 m devant (en haut), même hauteur, qui regarde vers le sujet. */
        val p = el(2, TypeElement.PROJECTEUR, 0.0, -3.0, rot = 180.0)
            .copy(hauteur = 1.6, luxReference = 900.0, distanceReference = 1.0, faisceau = 40.0)
        val plan = PlanSol(1, "test", elements = listOf(sujet, p))
        val a = Sol.apports(plan, sujet) { null }.single()
        assertEquals(3.0, a.distance, 1e-9)
        assertTrue(a.dansFaisceau)
        assertEquals("face", a.position)
        assertEquals(100.0, a.lux!!, 1e-9)

        val deTravers = plan.remplacer(p.copy(rotation = 90.0))
        val b = Sol.apports(deTravers, sujet) { null }.single()
        assertFalse(b.dansFaisceau)
        assertEquals(0.0, b.lux!!, 1e-9)

        val derriere = plan.remplacer(p.copy(y = 3.0, rotation = 0.0))
        assertEquals("contre", Sol.apports(derriere, sujet) { null }.single().position)
    }

    @Test
    fun flechesParNom() {
        val plan = PlanSol(
            1, "p", elements = listOf(
                el(1, TypeElement.COMEDIEN, 0.0, 0.0, nom = "Marie", etape = 2),
                el(2, TypeElement.COMEDIEN, 1.0, 0.0, nom = "Marie", etape = 1),
                el(3, TypeElement.COMEDIEN, 2.0, 0.0, nom = "Paul", etape = 1),
                el(4, TypeElement.CAMERA, 0.0, 3.0, nom = "A", etape = 1),
                el(5, TypeElement.CAMERA, 1.0, 3.0, nom = "A", etape = 2)
            )
        )
        val f = Sol.fleches(plan)
        assertEquals(2, f.size)
        assertTrue(f.any { it.first.identifiant == 2L && it.second.identifiant == 1L })
        assertEquals("B", Sol.prochaineCamera(plan))
        assertEquals("A2", plan.element(5)!!.libelle)
        assertEquals("Marie (2)", plan.element(1)!!.libelle)
        assertEquals("Paul", plan.element(3)!!.libelle)
    }

    @Test
    fun champDeLaCamera() {
        val c = el(1, TypeElement.CAMERA, 0.0, 0.0).copy(focaleMm = 24.89 / 2, capteurLargeurMm = 24.89, capteurHauteurMm = 18.66)
        assertEquals(90.0, Sol.champHorizontal(c)!!, 1e-9)
    }

    @Test
    fun heuresDoreesAParisEnJuin() {
        val h = Sol.heuresDuJour(48.86, 2.35, 172, 2.0)
        val soir = assertNotNull(h.doreeSoir)
        val bleue = assertNotNull(h.bleueSoir)
        /* Le soleil se couche vers 21h55 à Paris au solstice : l'heure dorée l'encadre, la bleue suit. */
        assertTrue(soir.debut in 20.5..21.6, "début ${soir.debut}")
        assertTrue(soir.fin in 22.0..22.6, "fin ${soir.fin}")
        assertTrue(bleue.debut >= soir.fin - 0.05)
        assertEquals("21h05", Sol.libelleHeure(21.0833))
    }
}
