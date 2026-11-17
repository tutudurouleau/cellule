package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MaterielPersoTest {

    @Test
    fun focalesTapeesALaMain() {
        assertEquals(listOf(18.0, 25.0, 35.0, 50.0), SaisieMateriel.lireFocales("18, 25, 35, 50"))
        assertEquals(listOf(14.5, 18.0, 25.0), SaisieMateriel.lireFocales("14,5 18 25"))
        assertEquals(listOf(18.0, 25.0, 35.0), SaisieMateriel.lireFocales("35/18/25/18"))
        assertEquals(listOf(18.0, 25.0, 35.0), SaisieMateriel.lireFocales("18,25,35"))
        assertEquals(listOf(14.5, 21.0), SaisieMateriel.lireFocales("14.5 mm et 21 mm"))
        assertEquals(emptyList(), SaisieMateriel.lireFocales("aucune"))
    }

    @Test
    fun nombresTapes() {
        assertEquals(23.76, SaisieMateriel.lireNombre(" 23,76 ")!!, 1e-9)
        assertNull(SaisieMateriel.lireNombre("0"))
        assertNull(SaisieMateriel.lireNombre("abc"))
    }

    @Test
    fun projecteurNomEtFaisceau() {
        val p = Projecteur("x", "600d Pro", "Aputure", angleFlood = 55.0)
        assertEquals("Aputure 600d Pro", p.nomComplet)
        assertEquals(55.0, p.angle(spot = true))
        assertEquals("Aputure LS 600d", Projecteur("y", "Aputure LS 600d", "Aputure").nomComplet)
    }
}
