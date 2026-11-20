package fr.cellule.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LaboCouleurTest {

    @Test
    fun `chaque procede a ses bains, son revelateur critique et ses sources`() {
        assertEquals(ProcedeCouleur.entries.toSet(), LaboCouleur.FICHES.map { it.procede }.toSet())
        LaboCouleur.FICHES.forEach { f ->
            assertTrue(f.bains.isNotEmpty(), f.procede.code)
            assertEquals(1, f.bains.count { it.critique }, f.procede.code)
            /* Le bain critique est toujours relu : durée et température. */
            assertFalse(f.critique.aVerifier, f.procede.code)
            assertTrue(f.critique.temperature.isNotBlank(), f.procede.code)
            assertTrue(f.sources.isNotEmpty() && f.sources.all { it.adresse.startsWith("https://") }, f.procede.code)
        }
    }

    @Test
    fun `les chiffres relus chez Kodak`() {
        val c41 = LaboCouleur.C41.critique
        assertEquals(195.0, c41.duree)
        assertEquals("37,8 ± 0,15 °C", c41.temperature)
        assertEquals(180.0, LaboCouleur.ECN2.critique.duree)
        assertEquals(360.0, LaboCouleur.E6.critique.duree)
        assertEquals(45.0, LaboCouleur.RA4.critique.duree)
        assertEquals(listOf(45.0, 45.0, 90.0), LaboCouleur.RA4.bains.map { it.duree })
        assertEquals("Révélateur", LaboCouleur.C41.bains.first().nom)
        assertEquals("Blanchiment", LaboCouleur.C41.bains[1].nom)
    }

    @Test
    fun `un bain non relu bloque le chrono plutot que d'inventer`() {
        val ecn2 = LaboCouleur.ECN2.sequence
        assertFalse(ecn2.complete)
        assertTrue(LaboCouleur.ECN2.aVerifier > 0)
        assertTrue(LaboCouleur.ECN2.bains.filter { it.aVerifier }.all { "notice" in it.consigne })
        assertTrue(LaboCouleur.C41.sequence.complete)
        assertTrue(LaboCouleur.RA4.sequence.complete)
        assertEquals(LaboCouleur.C41.dureeConnue, LaboCouleur.C41.sequence.duree)
    }

    @Test
    fun `les sequences couleur se reconnaissent au chrono`() {
        LaboCouleur.SEQUENCES.forEach { assertTrue(LaboCouleur.estCouleur(it)) }
        Sequences.TOUTES.forEach { assertFalse(LaboCouleur.estCouleur(it)) }
        assertEquals(listOf("C-41", "ECN-2", "E-6", "RA-4"), LaboCouleur.SEQUENCES.map { it.court })
    }

    @Test
    fun `corriger une dominante`() {
        val f = Filtration(40, 30)
        assertEquals(Filtration(50, 30), Dominantes.corriger(f, Dominante.JAUNE, 10).apres)
        assertEquals(Filtration(40, 40), Dominantes.corriger(f, Dominante.MAGENTA, 10).apres)
        assertEquals(Filtration(50, 40), Dominantes.corriger(f, Dominante.ROUGE, 10).apres)
        assertEquals(Filtration(30, 30), Dominantes.corriger(f, Dominante.BLEU, 10).apres)
        assertEquals(Filtration(40, 20), Dominantes.corriger(f, Dominante.VERT, 10).apres)
        assertEquals(Filtration(30, 20), Dominantes.corriger(f, Dominante.CYAN, 10).apres)
        assertEquals(0, Dominantes.corriger(f, Dominante.JAUNE, 10).neutreAjoute)
    }

    @Test
    fun `un filtre ne descend jamais sous zero`() {
        /* Retirer 30 de jaune à Y 20 : on ajoute 10 de neutre, soit Y 0 M 50 C 10. */
        val c = Dominantes.corriger(Filtration(20, 40), Dominante.BLEU, 30)
        assertEquals(Filtration(0, 50, 10), c.apres)
        assertEquals(10, c.neutreAjoute)
        /* Trois filtres présents : leur part commune n'est que de la densité neutre. */
        val n = Dominantes.corriger(Filtration(30, 50, 10), Dominante.JAUNE, 5)
        assertEquals(Filtration(25, 40, 0), n.apres)
        assertEquals(-10, n.neutreAjoute)
        Dominante.entries.forEach { d ->
            Dominantes.PAS.forEach { pas ->
                val r = Dominantes.corriger(Filtration(0, 0), d, pas).apres
                assertTrue(r.jaune >= 0 && r.magenta >= 0 && r.cyan >= 0, "$d $pas")
                assertEquals(0, minOf(r.jaune, r.magenta, r.cyan), "$d $pas")
            }
        }
    }

    @Test
    fun `le geste inverse est bien l'inverse`() {
        Dominante.entries.forEach { d ->
            val i = Dominantes.inverse(d)
            assertEquals(d, Dominantes.inverse(i))
            assertEquals(-d.jaune, i.jaune)
            assertEquals(-d.magenta, i.magenta)
        }
    }

    @Test
    fun `le quiz couleur tient debout`() {
        val alea = Random(11)
        repeat(500) {
            val e = QuizCouleur.exercice(alea)
            assertEquals(ThemeLabo.COULEUR, e.theme)
            assertTrue(e.choix.size >= 3, "${e.enonce} ${e.choix}")
            assertEquals(e.choix.size, e.choix.distinct().size, e.enonce)
            assertTrue(e.bonne in e.choix.indices)
            assertTrue("-" !in e.choix.filter { it.startsWith("Y ") }.joinToString(), e.choix.toString())
        }
    }
}
