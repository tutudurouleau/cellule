package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CarnetTirageTest {

    private val tirage = TirageNote(
        1, "2026-09-25", negatif = "HP5 · film 12 · vue 7", papier = "RC brillant", filtre = "3", ouverture = "f/8",
        base = 10.0, corrections = listOf(Correction("Ciel", 1.0 / 3), Correction("Visage", -0.5))
    )

    @Test
    fun `le deroule du minuteur`() {
        val d = tirage.deroule()
        assertEquals("Base : 10 s", d[0])
        assertEquals("Ciel +⅓ : brûler 2,6 s de plus", d[1])
        assertEquals("Visage −½ : masquer 2,9 s pendant la base", d[2])
    }

    @Test
    fun `changer de base garde les corrections`() {
        val r = CarnetTirage.rebaser(tirage, 20.0)
        assertEquals(tirage.corrections, r.corrections)
        assertEquals("Ciel +⅓ : brûler 5,2 s de plus", r.deroule()[1])
    }

    @Test
    fun `les demis restent des demis`() {
        assertEquals("−½", libelleCorrection(-0.5))
        assertEquals("+1 ½", libelleCorrection(1.5))
        assertEquals("+⅓", libelleCorrection(1.0 / 3))
        assertEquals("+⅔", libelleCorrection(2.0 / 3))
        assertEquals("+1", libelleCorrection(1.0))
        assertEquals("juste", libelleCorrection(0.0))
    }

    @Test
    fun `sans base, pas de deroule`() {
        assertTrue(tirage.copy(base = null).deroule().isEmpty())
    }
}
