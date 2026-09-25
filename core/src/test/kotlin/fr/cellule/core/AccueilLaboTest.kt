package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccueilLaboTest {

    @Test
    fun `chaque calculateur a son domaine et son theme`() {
        Calculateur.entries.forEach { c ->
            assertTrue(DomaineLabo.entries.any { c in it.calculateurs }, c.name)
            assertTrue(c.theme in ThemeLabo.entries)
        }
        assertTrue(DomaineLabo.COULEUR.calculateurs.isEmpty())
    }

    @Test
    fun `les fiches se rangent par marque`() {
        assertEquals(Marque.ILFORD, RechercheFiches.marque(FichesLabo.TOUTES.first { it.nom == "Ilford HP5 Plus" }))
        assertEquals(Marque.KODAK, RechercheFiches.marque(FichesLabo.TOUTES.first { it.nom == "D-76" }))
        assertEquals(Marque.AUTRES, RechercheFiches.marque(FichesLabo.TOUTES.first { it.nom == "Rodinal" }))
        assertEquals(FichesLabo.TOUTES.size, Marque.entries.sumOf { m -> RechercheFiches.chercher("", m).size })
    }

    @Test
    fun `la recherche ignore accents et majuscules`() {
        val fixateurs = RechercheFiches.chercher("FIXATEUR", null).map { it.nom }
        assertTrue("RAPID FIXER" in fixateurs && "KODAK Fixer" in fixateurs, fixateurs.toString())
        assertTrue(RechercheFiches.chercher("revelateur poudre", null).any { it.nom == "D-76" })
        assertTrue(RechercheFiches.chercher("hp5", Marque.KODAK).none { it.nom == "Ilford HP5 Plus" })
        assertTrue(RechercheFiches.chercher("introuvable xyz", null).isEmpty())
    }
}
