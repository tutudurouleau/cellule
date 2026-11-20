package fr.cellule.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LexiqueTest {

    @Test
    fun `chaque renvoi vers une fiche existe`() {
        val ids = (CATALOGUE_MACHINERIE + CATALOGUE_ELECTRIQUE + CATALOGUE_MATERIEL).map { it.identifiant }.toSet()
        Lexique.TERMES.mapNotNull { it.fiche }.forEach { assertTrue(it in ids, "fiche inconnue : $it") }
        assertTrue(Lexique.TERMES.size >= 120)
        assertEquals(Lexique.TERMES.size, Lexique.TERMES.map { it.cle }.distinct().size, "mots en double")
    }

    @Test
    fun `chercher trouve par synonyme et sans accents`() {
        assertEquals("Wind-up", Lexique.chercher("crank-up", Lexique.TERMES).first().mot)
        assertTrue(Lexique.chercher("gelatine", Lexique.TERMES).any { it.mot == "Gélatine" })
    }

    @Test
    fun `les questions tiennent debout`() {
        val alea = Random(5)
        repeat(300) {
            val q = assertNotNull(Lexique.question(Lexique.TERMES, alea))
            assertEquals(4, q.choix.distinct().size)
            assertEquals(q.terme.mot, q.choix[q.bonne])
        }
    }

    @Test
    fun `mes mots se relisent tels quels`() {
        val perso = listOf(TermePlateau("Un U", "Ce que m'a dit le chef machino\nà vérifier", RubriqueLexique.PERSO))
        assertEquals(perso, LexiquePerso.lire(LexiquePerso.ecrire(perso)))
        assertTrue(LexiquePerso.lire("").isEmpty())
    }

    @Test
    fun `machinerie et marques sont larges`() {
        assertTrue(CATALOGUE_MACHINERIE.size >= 45)
        assertTrue(CATALOGUE_ELECTRIQUE.size >= 18)
        assertTrue(Marques.TOUTES.size >= 50)
        assertEquals(Marques.TOUTES.size, Marques.TOUTES.map { it.nom }.distinct().size)
        val ids = (CATALOGUE_MACHINERIE + CATALOGUE_ELECTRIQUE).map { it.identifiant }
        assertEquals(ids.size, ids.distinct().size, "identifiants en double")
    }
}
