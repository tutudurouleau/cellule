package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarnetLaboTest {

    private fun note(id: Long, estime: Double?, calcule: Double?, r: Resultat? = null) =
        DeveloppementNote(id, "2026-09-25", film = "Ilford HP5 Plus", ei = 400, revelateur = "ID-11", dilution = "stock",
            estimation = estime, tempsCalcule = calcule, resultat = r)

    @Test
    fun `les trois temps et l ecart`() {
        val n = note(1, 540.0, 450.0).copy(tempsDonne = 480.0)
        assertEquals(0.2, n.ecartEstimation!!, 1e-9)
        assertEquals(480.0 / 450 - 1, n.ecartDonne!!, 1e-9)
        assertEquals("Ilford HP5 Plus à EI 400 · ID-11 stock", n.titre)
        assertNull(note(2, null, 450.0).ecartEstimation)
    }

    @Test
    fun `le bilan suit l intuition`() {
        val notes = listOf(600.0, 580.0, 330.0, 560.0, 350.0, 460.0, 455.0, 440.0, 450.0, 452.0)
            .mapIndexed { i, e -> note(i.toLong(), e, 450.0, if (i % 3 == 0) Resultat.DUR else Resultat.CORRECT) }
        val b = assertNotNull(CarnetLabo.bilan(notes))
        assertEquals(10, b.nombre)
        assertTrue(b.erreurRecente!! < b.erreurDebut!!)
        assertEquals(4, b.resultats[Resultat.DUR])
    }

    @Test
    fun `un conseil sourcé pour chaque defaut`() {
        Resultat.entries.filter { it != Resultat.CORRECT }.forEach { assertNotNull(CarnetLabo.conseil(it), it.name) }
        assertNull(CarnetLabo.conseil(Resultat.CORRECT))
        assertEquals(Sources.KODAK_D76, CarnetLabo.conseil(Resultat.DUR)!!.second)
    }

    @Test
    fun `restaurer deux fois ne dedouble rien`() {
        val a = listOf(note(1, null, null), note(2, null, null))
        val fusion = CarnetLabo.fusionner(a, listOf(note(2, null, null), note(3, null, null)))
        assertEquals(listOf(1L, 2L, 3L), fusion.map { it.identifiant })
        assertEquals(fusion, CarnetLabo.fusionner(fusion, fusion))
    }
}
