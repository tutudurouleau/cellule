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
    fun `les fiches se rangent par type de produit`() {
        fun type(nom: String) = RechercheFiches.type(FichesLabo.TOUTES.first { it.nom == nom })
        assertEquals(TypeFiche.FILM_400, type("Ilford HP5 Plus"))
        assertEquals(TypeFiche.FILM_100, type("Kodak T-MAX 100"))
        assertEquals(TypeFiche.REVELATEUR_POUDRE, type("D-76"))
        assertEquals(TypeFiche.REVELATEUR_LIQUIDE, type("ILFOTEC DD-X"))
        assertEquals(TypeFiche.REVELATEUR_CITE, type("Rodinal"))
        assertEquals(TypeFiche.ARRET, type("ILFOSTOP"))
        assertEquals(TypeFiche.FIXATEUR, type("KODAK Fixer"))
        assertEquals(TypeFiche.FINITION, type("ILFOTOL"))
        /* Chaque fiche tombe dans un type de son genre. */
        FichesLabo.TOUTES.forEach { assertEquals(it.genre, RechercheFiches.type(it).genre, it.nom) }
        assertEquals(FichesLabo.TOUTES.size, GenreFiche.entries.sumOf { g -> RechercheFiches.chercher("", g).size })
    }

    @Test
    fun `le rangement s'appuie sur ce que les fiches disent`() {
        FichesLabo.TOUTES.forEach { f ->
            when (RechercheFiches.type(f)) {
                TypeFiche.REVELATEUR_POUDRE -> assertTrue("poudre" in f.resume, f.nom)
                TypeFiche.REVELATEUR_LIQUIDE -> assertTrue("Liquide" in f.resume, f.nom)
                TypeFiche.FIXATEUR -> assertTrue("ixateur" in f.resume, f.nom)
                TypeFiche.ARRET -> assertTrue("arrêt" in f.resume, f.nom)
                else -> {}
            }
        }
    }

    @Test
    fun `la recherche ignore accents et majuscules`() {
        val fixateurs = RechercheFiches.chercher("FIXATEUR", null).map { it.nom }
        assertTrue("RAPID FIXER" in fixateurs && "KODAK Fixer" in fixateurs, fixateurs.toString())
        assertTrue(RechercheFiches.chercher("revelateur poudre", null).any { it.nom == "D-76" })
        assertTrue(RechercheFiches.chercher("hp5", GenreFiche.REVELATEUR).none { it.nom == "Ilford HP5 Plus" })
        assertTrue(RechercheFiches.chercher("introuvable xyz", null).isEmpty())
        /* Le type se cherche aussi : « liquide » trouve les révélateurs liquides. */
        assertTrue(RechercheFiches.chercher("liquide", GenreFiche.REVELATEUR).any { it.nom == "ILFOSOL 3" })
        /* Et la marque reste cherchable. */
        assertTrue(RechercheFiches.chercher("kodak", GenreFiche.FILM).all { it.fabricant.startsWith("Kodak") })
    }
}
