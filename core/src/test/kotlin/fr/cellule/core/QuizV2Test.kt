package fr.cellule.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuizV2Test {

    @Test
    fun `les leurres sont de la meme marque quand la marque le permet`() {
        val alea = Random(5)
        repeat(400) {
            val q = assertNotNull(QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, CATALOGUE_MATERIEL, alea))
            val soeurs = CATALOGUE_MATERIEL.count {
                it.genre == q.reponse.genre && it.nomComplet != q.reponse.nomComplet && QuizMateriel.memeMarque(it, q.reponse)
            }
            if (soeurs >= QuizMateriel.NOMBRE_DE_CHOIX - 1) {
                assertTrue(
                    q.choix.all { QuizMateriel.memeMarque(it, q.reponse) },
                    "${q.reponse.nomComplet} : ${q.choix.map { it.nomComplet }}"
                )
            }
        }
    }

    @Test
    fun `un indice que partagent les quatre propositions est retire`() {
        val alea = Random(9)
        repeat(400) {
            val q = assertNotNull(QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, CATALOGUE_MATERIEL, alea))
            if (q.choix.all { QuizMateriel.memeMarque(it, q.reponse) }) {
                assertFalse(q.indices.any { it.startsWith("Fabricant") }, q.indices.toString())
            }
            if (q.choix.all { it.categorie == q.reponse.categorie }) {
                assertFalse(q.indices.any { it.startsWith("Famille") }, q.indices.toString())
            }
            /* La phrase à retenir ouvre toujours la marche. */
            assertEquals(QuizMateriel.masquerTout(q.reponse.aRetenir, q.reponse), q.indices.first())
            /* Seul « Fabricant » nomme la marque. */
            q.indices.filterNot { it.startsWith("Fabricant") }.forEach {
                assertFalse(QuizMateriel.nommeLaMarque(it, q.reponse), it)
            }
            q.indices.forEach { assertFalse(QuizMateriel.devoile(it, q.reponse), it) }
        }
    }

    @Test
    fun `une chronologie se remet dans un seul ordre`() {
        Niveau.entries.forEach { niveau ->
            val alea = Random(13)
            repeat(200) {
                val q = assertNotNull(QuizMateriel.question(GenreQuestion.CHRONOLOGIE, CATALOGUE_MATERIEL, alea, niveau = niveau))
                assertEquals(QuizMateriel.NOMBRE_DE_CHOIX, q.choix.toSet().size)
                assertTrue(q.choix.all { it.genre == q.choix.first().genre }, "genres mélangés")
                for (i in q.choix.indices) for (j in q.choix.indices) if (i < j) {
                    assertTrue(QuizMateriel.departageables(q.choix[i], q.choix[j]), q.explication)
                }
                val annees = q.ordre.map { q.choix[it].annee }
                assertEquals(annees.sorted(), annees)
                assertEquals(q.reponse, q.choix[q.ordre.first()])
            }
        }
    }

    @Test
    fun `l'intrus est seul de son espece`() {
        listOf(CATALOGUE_CAMERAS, CATALOGUE_OBJECTIFS, CATALOGUE_MATERIEL).forEach { parmi ->
            val alea = Random(17)
            repeat(200) {
                val q = assertNotNull(QuizMateriel.question(GenreQuestion.INTRUS, parmi, alea))
                assertEquals(QuizMateriel.NOMBRE_DE_CHOIX, q.choix.toSet().size)
                assertTrue(q.choix.all { it.genre == q.reponse.genre })
                assertTrue(q.explication.startsWith(q.reponse.nomComplet), q.explication)
                assertEquals(1, q.indices.size)
            }
        }
    }

    @Test
    fun `une fiche ratee revient jusqu'a deux reussites`() {
        var r = ARevoir()
        r = r.apres("alexa", juste = false)
        assertEquals(setOf("alexa"), r.identifiants)
        r = r.apres("alexa", juste = true)
        assertEquals(setOf("alexa"), r.identifiants)
        r = r.apres("alexa", juste = false)
        r = r.apres("alexa", juste = true)
        assertEquals(setOf("alexa"), r.identifiants, "une erreur remet le compte à zéro")
        r = r.apres("alexa", juste = true)
        assertTrue(r.identifiants.isEmpty())
        assertEquals(r, r.apres("venice", juste = true), "une réussite hors révision ne change rien")
    }

    @Test
    fun `les fiches a revoir reviennent plus souvent`() {
        val cible = CATALOGUE_CAMERAS.first { it.nom == "ALEXA 35" }
        val alea = Random(21)
        val fois = (1..600).count {
            QuizMateriel.question(
                GenreQuestion.QUI_SUIS_JE, CATALOGUE_CAMERAS, alea, aRevoir = setOf(cible.identifiant)
            )!!.reponse == cible
        }
        assertTrue(fois > 600 * QuizMateriel.PART_REVISION * 0.8, "revue $fois fois")
    }

    @Test
    fun `le bilan range les marques de la plus ratee a la mieux reconnue`() {
        val arri = CATALOGUE_CAMERAS.first { it.fabricant == "ARRI" }
        val red = CATALOGUE_CAMERAS.first { it.fabricant == "RED" }
        var b = BilanQuiz()
        repeat(3) { b = b.apres(arri, juste = true) }
        repeat(3) { b = b.apres(red, juste = it == 0) }
        b = b.apres(CATALOGUE_CAMERAS.first { it.fabricant == "Sony" }, juste = false)
        assertEquals(listOf("RED", "ARRI"), b.classement().map { it.marque })
        assertEquals(1.0, b.parMarque.getValue("ARRI").part)
    }
}
