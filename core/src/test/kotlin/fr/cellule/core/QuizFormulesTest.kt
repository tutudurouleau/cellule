package fr.cellule.core

import kotlin.math.atan
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuizFormulesTest {

    @Test
    fun `chaque exercice tient debout, quel que soit le tirage`() {
        ThemeFormule.entries.forEach { theme ->
            QuizFormules.generateurs(theme).forEachIndexed { g, generateur ->
                repeat(300) { graine ->
                    val q = generateur(Random(graine * 31 + g))
                    val ou = "${theme.name} n°$g, graine $graine : ${q.enonce}"
                    assertEquals(theme, q.theme, ou)
                    assertEquals(4, q.options.size, ou)
                    assertEquals(4, q.options.distinct().size, "$ou → ${q.options}")
                    assertTrue(q.bonne in 0..3, ou)
                    assertTrue(q.enonce.isNotBlank() && q.correction.isNotBlank() && q.sujet.isNotBlank(), ou)
                    q.options.forEach { o ->
                        assertTrue(o.isNotBlank() && "—" !in o && "NaN" !in o && "Infinity" !in o, "$ou → « $o »")
                    }
                }
            }
        }
    }

    @Test
    fun `la bonne reponse change de place`() {
        val places = (0 until 200).map { QuizFormules.question(ThemeFormule.LUMIERE, Random(it)).bonne }.toSet()
        assertEquals(setOf(0, 1, 2, 3), places)
    }

    @Test
    fun `le melange touche tous les themes`() {
        val themes = (0 until 500).map { QuizFormules.question(null, Random(it)).theme }.toSet()
        assertEquals(ThemeFormule.entries.toSet(), themes)
    }

    @Test
    fun `les nombres s'ecrivent a la francaise`() {
        assertEquals("2 560", nb(2560.0))
        assertEquals("0,125", nb(0.125, 3))
        assertEquals("313", nb(312.5))
        assertEquals("−134", nb(-134.0))
        assertEquals("1,26", nb(1.259921, 2))
        assertEquals("0,9", nb(0.3 * 3, 1))
        assertEquals("0", nb(-0.0001, 1))
    }

    @Test
    fun `les angles surs sont ceux de la table`() {
        assertEquals(listOf(86.4, 172.8, 259.2, 345.6), anglesSurs(24, 50).map { Math.round(it * 10) / 10.0 })
        assertEquals(listOf(90.0, 180.0, 270.0, 360.0), anglesSurs(25, 50))
    }

    @Test
    fun `l'angle de champ du quiz est celui de l'appli`() {
        /* La formule du quiz, écrite à la main, contre celle du viseur. */
        val quiz = Math.toDegrees(2 * atan(36.0 / (2 * 50.0)))
        assertEquals(Focales.angleDeChamp(36.0, 50.0), quiz, 0.01)
        val q = (0 until 400).map { QuizFormules.generateurs(ThemeFormule.OPTIQUE)[0](Random(it)) }
            .first { "plein format" in it.enonce && "de 50 mm" in it.enonce }
        assertEquals("40°", q.options[q.bonne])
    }

    @Test
    fun `la correction donne la bonne reponse sur un cas connu`() {
        /* 180° à 25 im/s : 1/50 s, comme l'écrit la table. */
        val q = (0 until 400).map { QuizFormules.generateurs(ThemeFormule.TOURNAGE)[0](Random(it)) }
            .first { it.enonce.startsWith("Obturateur à 180° et 25 im/s") }
        assertEquals("1/50 s", q.options[q.bonne])
    }

    @Test
    fun `chaque question renvoie a sa fiche`() {
        ThemeFormule.entries.forEach { theme ->
            QuizFormules.generateurs(theme).forEach { generateur ->
                repeat(60) { graine ->
                    val q = generateur(Random(graine))
                    assertTrue(FichesFormules.pour(theme, q.sujet) != null, "Pas de fiche pour ${theme.name} « ${q.sujet} »")
                }
            }
        }
        /* Et pas de fiche orpheline, ni en double. */
        val sujets = FichesFormules.TOUTES.map { it.theme to it.sujet }
        assertEquals(sujets.size, sujets.toSet().size)
        FichesFormules.TOUTES.forEach { f ->
            assertTrue(f.formule.isNotBlank() && f.lettres.isNotEmpty() && f.exemple.isNotBlank() && f.repere.isNotBlank() && f.piege.isNotBlank(), f.sujet)
            val posee = (0 until 600).any { QuizFormules.question(f.theme, Random(it)).sujet == f.sujet }
            assertTrue(posee, "La fiche « ${f.sujet} » n'a aucune question")
        }
    }

    @Test
    fun `une question sur une formule precise`() {
        val q = QuizFormules.questionSur(ThemeFormule.LUMIERE, "Tache de lumière", Random(3))
        assertEquals("Tache de lumière", q.sujet)
    }

    @Test
    fun `la reponse tapee juste est acceptee, et seulement elle`() {
        var tapables = 0
        ThemeFormule.entries.forEach { theme ->
            QuizFormules.generateurs(theme).forEach { generateur ->
                repeat(80) { graine ->
                    val q = generateur(Random(graine))
                    val gabarit = SaisieReponse.gabarit(q) ?: return@repeat
                    tapables++
                    assertTrue("…" in gabarit, gabarit)
                    val attendu = SaisieReponse.attendu(q)!!
                    val tape = nb(attendu, 6)
                    assertTrue(SaisieReponse.accepte(q, tape), "« $tape » refusé pour ${q.options[q.bonne]}")
                    assertTrue(!SaisieReponse.accepte(q, nb(attendu * 1.5 + 1, 3)), "réponse fausse acceptée pour ${q.options[q.bonne]}")
                }
            }
        }
        assertTrue(tapables > 2000, "seulement $tapables questions à taper")
        /* La fraction se tape par son dénominateur, ou en entier. */
        val q = (0 until 400).map { QuizFormules.generateurs(ThemeFormule.TOURNAGE)[0](Random(it)) }
            .first { it.enonce.startsWith("Obturateur à 180° et 25 im/s") }
        assertEquals("1/… s", SaisieReponse.gabarit(q))
        assertTrue(SaisieReponse.accepte(q, "50"))
        assertTrue(SaisieReponse.accepte(q, "1/50"))
        assertTrue(!SaisieReponse.accepte(q, "48"))
    }
}
