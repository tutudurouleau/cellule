package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RaccordLumiereTest {

    @Test
    fun `l'ecart du jour, en diaphs`() {
        assertEquals(1.0, Raccord.ecart(400.0, 800.0), 1e-9)
        assertEquals(-2.0, Raccord.ecart(800.0, 200.0), 1e-9)
        assertEquals("Raccord : c'est juste", Raccord.conseil(Raccord.ecart(800.0, 850.0)))
        assertTrue(Raccord.conseil(1.0).startsWith("+1 diaph"))
        assertTrue(Raccord.conseil(-0.5).startsWith("−0,5 diaph"))
    }

    @Test
    fun `rapport et resume`() {
        val r = Raccord.nouveau(1, "15:30").copy(
            ciel = "Soleil",
            mesures = listOf(MesureRaccord("Face", 800.0), MesureRaccord("Débouchage", 200.0), MesureRaccord("Fond"))
        )
        assertEquals(4.0, Raccord.rapport(r)!!, 1e-9)
        assertEquals("15:30 · Soleil · 2 mesures", Raccord.resume(r))
        assertEquals("Rien de noté pour l'instant", Raccord.resume(RaccordLumiere(1)))
        assertEquals(Raccord.ENDROITS, Raccord.nouveau(2).mesures.map { it.endroit })
    }

    @Test
    fun `les sources d'un plan au sol`() {
        val plan = PlanSol(
            1, "Salon", elements = listOf(
                ElementSol(1, TypeElement.PROJECTEUR, 0.0, 0.0, nom = "Face", projecteur = "sky", hauteur = 2.5, pertesDiaphs = 1.0, circuit = "A"),
                ElementSol(2, TypeElement.PROJECTEUR, 0.0, 0.0, projecteur = "fresnel"),
                ElementSol(3, TypeElement.CAMERA, 0.0, 0.0)
            )
        )
        val s = Raccord.sourcesDuPlan(plan) { if (it.projecteur == "sky") "SkyPanel S60" else "Fresnel 650" }
        assertEquals(2, s.size)
        assertEquals("Face", s[0].nom)
        assertEquals("SkyPanel S60, à 2,5 m de haut, gélatine ou diffusion : −1 diaph, circuit A", s[0].reglage)
        assertEquals("Fresnel 650", s[1].nom)
        assertEquals("", s[1].reglage)
    }
}
