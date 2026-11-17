package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FichesProjecteursTest {

    @Test
    fun `aucun nom en double`() {
        val noms = CATALOGUE_FICHES_PROJECTEURS.map { sansAccents(it.nomComplet) }
        assertEquals(noms.size, noms.toSet().size, "Noms en double : $noms")
    }

    @Test
    fun `chaque fiche est complete`() {
        CATALOGUE_FICHES_PROJECTEURS.forEach { m ->
            listOf(
                m.nom, m.fabricant, m.categorie, m.punchline, m.histoire, m.ficheTechnique,
                m.commentLUtiliser, m.atouts, m.limites, m.aRetenir
            ).forEach { assertTrue(it.isNotBlank(), "${m.nomComplet} : champ vide") }
            assertTrue(m.chiffresCles.size >= 2, "${m.nomComplet} : pas assez de chiffres clés")
            assertTrue(m.configurations.isNotEmpty(), "${m.nomComplet} : aucune photométrie liée")
            assertTrue(m.annee in 1950..2026, "${m.nomComplet} : ${m.annee}")
        }
    }

    @Test
    fun `chaque configuration existe et n'appartient qu'a une fiche`() {
        val ids = PROJECTEURS_CATALOGUE.map { it.identifiant }.toSet()
        val liees = CATALOGUE_FICHES_PROJECTEURS.flatMap { it.configurations }
        liees.forEach { assertTrue(it in ids, "Configuration inconnue : $it") }
        assertEquals(liees.size, liees.toSet().size, "Configuration liée deux fois")
    }

    @Test
    fun `tout le catalogue photometrique a sa grande fiche`() {
        PROJECTEURS_CATALOGUE.forEach { p ->
            val f = ficheDuProjecteur(p.identifiant)
            assertNotNull(f, "${p.identifiant} : pas de grande fiche")
            assertEquals(p.fabricant, f.fabricant, p.identifiant)
        }
        assertNull(ficheDuProjecteur("perso-inconnu"))
    }

    @Test
    fun `les etageres ne sont pas vides`() {
        ETAGERES_PROJECTEURS.forEach { assertTrue(it.entrees.isNotEmpty(), it.titre) }
        assertTrue(CATALOGUE_FICHES_PROJECTEURS.size >= 15)
    }

    @Test
    fun `une valeur a relire dans le catalogue le reste dans la fiche`() {
        PROJECTEURS_CATALOGUE.filter { !it.relu }.forEach { p ->
            val f = ficheDuProjecteur(p.identifiant)!!
            val texte = f.ficheTechnique + f.chiffresCles.joinToString { it.valeur }
            assertTrue("à relire" in texte, "${f.nomComplet} : ${p.identifiant} non relu, la fiche doit le dire")
        }
    }

    @Test
    fun `la recherche trouve les projecteurs`() {
        assertTrue(chercherMateriel("arri hmi", CATALOGUE_FICHES_PROJECTEURS).any { it.nom == "M18" })
        assertTrue(chercherMateriel("FRESNEL", CATALOGUE_FICHES_PROJECTEURS).size >= 2)
    }
}
