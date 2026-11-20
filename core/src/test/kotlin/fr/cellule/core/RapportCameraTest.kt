package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals

class RapportCameraTest {

    @Test
    fun `le numero qui suit`() {
        assertEquals("1", RapportCamera.suivant(""))
        assertEquals("13", RapportCamera.suivant("12"))
        assertEquals("3B", RapportCamera.suivant("3A"))
        assertEquals("13", RapportCamera.suivant("12 bis"))
        assertEquals("10", RapportCamera.suivant("9"))
        assertEquals("ext 2", RapportCamera.suivant("ext"))
    }

    @Test
    fun `prise suivante et plan suivant gardent les reglages`() {
        val p = PriseCamera(1, 7, sequence = "12", plan = "3", prise = 2, objectif = "50 mm", tStop = "2.8", verdict = VerdictPrise.BONNE, remarques = "flou")
        val s = RapportCamera.priseSuivante(p, 2)
        assertEquals(3, s.prise)
        assertEquals("50 mm", s.objectif)
        assertEquals(VerdictPrise.A_VOIR, s.verdict)
        assertEquals("", s.remarques)
        val n = RapportCamera.planSuivant(p, 3)
        assertEquals("4", n.plan)
        assertEquals(1, n.prise)
        assertEquals("12", n.sequence)
    }

    @Test
    fun `reference, reglage et bilan`() {
        val p = PriseCamera(1, 7, sequence = "12", plan = "3", prise = 2, objectif = "50 mm", tStop = "2,8", ei = "800", cadence = "25", obturateur = "180")
        assertEquals("Séq. 12 · Pl. 3 · Pr. 2", p.reference)
        assertEquals("50 mm · T2,8 · EI 800 · 25 im/s · 180°", p.reglage)
        val prises = listOf(p, p.copy(identifiant = 2, verdict = VerdictPrise.BONNE), p.copy(identifiant = 3, verdict = VerdictPrise.A_REFAIRE))
        assertEquals("3 prises · 1 bonne · 1 à refaire", RapportCamera.bilan(prises))
        assertEquals(listOf(1L, 2L, 3L), RapportCamera.duJour(prises.reversed() + p.copy(identifiant = 9, jourId = 8), 7).map { it.identifiant })
    }
}
