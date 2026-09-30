package fr.cellule.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecettesMaisonTest {

    private fun recette(nom: String) = RecettesMaison.TOUTES.first { it.nom == nom }
    private fun ingredient(r: RecetteMaison, debut: String) = r.ingredients.first { it.nom.startsWith(debut) }

    @Test
    fun `les formes du carbonate se convertissent par les masses molaires`() {
        assertEquals(1.0, FormeCarbonate.ANHYDRE.facteur)
        assertTrue(abs(FormeCarbonate.DECAHYDRATE.facteur - 2.70) < 0.01)
        assertTrue(abs(FormeCarbonate.MONOHYDRATE.facteur - 1.17) < 0.01)
        /* 2 NaHCO3 → Na2CO3 : il faut 1,585 g de bicarbonate par gramme de carbonate. */
        assertTrue(abs(FormeCarbonate.BICARBONATE.facteur - 1.585) < 0.01)
    }

    @Test
    fun `le Caffenol-C-M retombe sur les proportions de son auteur`() {
        val r = recette("Caffenol-C-M")
        val c = ingredient(r, "Carbonate")
        assertEquals("16,2 g", RecettesMaison.libelleQuantite(c, 300.0, FormeCarbonate.ANHYDRE))
        assertEquals("43,7 g", RecettesMaison.libelleQuantite(c, 300.0, FormeCarbonate.DECAHYDRATE))
        assertEquals("12 g", RecettesMaison.libelleQuantite(ingredient(r, "Café"), 300.0, FormeCarbonate.ANHYDRE))
        assertEquals(15.0, r.tempsDepart)
    }

    @Test
    fun `les recettes du guide retrouvent leurs quantites pour 300 mL`() {
        val brou = recette("Brou de noix")
        assertEquals("16 g", RecettesMaison.libelleQuantite(ingredient(brou, "Carbonate"), 300.0, FormeCarbonate.ANHYDRE))
        assertEquals("43,2 g", RecettesMaison.libelleQuantite(ingredient(brou, "Carbonate"), 300.0, FormeCarbonate.DECAHYDRATE))
        assertEquals("5 g", RecettesMaison.libelleQuantite(ingredient(brou, "Vitamine"), 300.0, FormeCarbonate.ANHYDRE))
        assertEquals("55 mL", RecettesMaison.libelleQuantite(ingredient(brou, "Extrait"), 300.0, FormeCarbonate.ANHYDRE))
        /* Le sel corrigé : 1 %, soit 3 g dans 300 mL. */
        assertEquals("3 g", RecettesMaison.libelleQuantite(ingredient(brou, "Sel"), 300.0, FormeCarbonate.ANHYDRE))
        assertEquals("≈ 4 sachets", RecettesMaison.libelleQuantite(ingredient(recette("Thé (« Teanol »)"), "Thé"), 300.0, FormeCarbonate.ANHYDRE))
    }

    @Test
    fun `le vinaigre dilue garde la meme force quel que soit le vinaigre`() {
        /* 62,5 mL de vinaigre à 8 % par litre : 5 g d'acide, comme 42 mL à 12 % et 36 mL à 14 %. */
        val acide8 = 62.5 * 0.08
        assertTrue(abs(acide8 - 41.7 * 0.12) < 0.05 && abs(acide8 - 35.7 * 0.14) < 0.05)
    }

    @Test
    fun `seules les recettes a tester n'ont pas de source`() {
        RecettesMaison.TOUTES.forEach { r ->
            if (r.niveau == NiveauRecette.HYPOTHESE) assertTrue(r.sources.isEmpty(), r.nom)
            else assertTrue(r.sources.isNotEmpty(), r.nom)
        }
    }

    @Test
    fun `l'eau complete au volume et les quantites textuelles restent telles quelles`() {
        val r = recette("Fixateur au thiosulfate de sodium")
        assertEquals("complément à 500 mL", RecettesMaison.libelleQuantite(r.ingredients.last(), 500.0, FormeCarbonate.ANHYDRE))
        assertEquals("1 à 2 c. à café par litre", RecettesMaison.libelleQuantite(ingredient(r, "Sulfite"), 500.0, FormeCarbonate.ANHYDRE))
        assertNull(RecettesMaison.quantite(ingredient(r, "Sulfite"), 500.0, FormeCarbonate.ANHYDRE))
    }

    @Test
    fun `la seance au chrono part du temps de la recette`() {
        RecettesMaison.TOUTES.filter { it.role == RoleRecette.REVELATEUR }.forEach { r ->
            val s = RecettesMaison.sequence(r)
            assertEquals(listOf("Révélateur", "Arrêt à l'eau", "Fixateur", "Lavage", "Mouillant"), s.etapes.map { it.nom })
            assertEquals((r.tempsDepart ?: 0.0) * 60, s.etapes.first().duree)
        }
        /* Sans temps publié, le révélateur reste à saisir : la séance n'est pas « complète ». */
        assertTrue(!RecettesMaison.sequence(recette("Caffenol-C-M au sel")).complete)
        assertTrue(RecettesMaison.sequence(recette("Caffenol-C-M")).complete)
    }

    @Test
    fun `chaque recette dit ce qu'elle corrige du guide`() {
        RecettesMaison.TOUTES.forEach { assertTrue(it.corrections.isNotEmpty(), it.nom) }
        assertTrue(RecettesMaison.A_EVITER.any { it.first.contains("citron") })
    }
}
