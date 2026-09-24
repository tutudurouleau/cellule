package fr.cellule.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogueMaterielTest {

    @Test
    fun `aucun nom en double`() {
        val noms = CATALOGUE_MATERIEL.map { sansAccents(it.nomComplet) }
        val doublons = noms.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue(doublons.isEmpty(), "Noms en double : $doublons")
    }

    @Test
    fun `chaque fiche est complete`() {
        CATALOGUE_MATERIEL.forEach { m ->
            val champs = mapOf(
                "nom" to m.nom, "fabricant" to m.fabricant, "catégorie" to m.categorie,
                "punchline" to m.punchline, "histoire" to m.histoire,
                "fiche technique" to m.ficheTechnique, "comment l'utiliser" to m.commentLUtiliser,
                "atouts" to m.atouts, "limites" to m.limites, "à retenir" to m.aRetenir
            )
            champs.forEach { (champ, texte) -> assertTrue(texte.isNotBlank(), "${m.nomComplet} : $champ vide") }
            assertTrue(m.films.isNotEmpty(), "${m.nomComplet} : aucun film")
            assertTrue(m.films.all { it.titre.isNotBlank() }, "${m.nomComplet} : titre de film vide")
            assertTrue(m.chiffresCles.size >= 3, "${m.nomComplet} : pas assez de chiffres clés")
            assertTrue(m.chiffresCles.all { it.libelle.isNotBlank() && it.valeur.isNotBlank() }, m.nomComplet)
        }
    }

    @Test
    fun `les annees sont plausibles`() {
        CATALOGUE_MATERIEL.forEach { m ->
            assertTrue(m.annee in 1920..2026, "${m.nomComplet} : ${m.annee}")
        }
    }

    @Test
    fun `chaque silhouette correspond au genre et sert au moins une fois`() {
        CATALOGUE_MATERIEL.forEach { m ->
            assertEquals(m.genre, m.silhouette.genre, "${m.nomComplet} : silhouette ${m.silhouette}")
        }
        val utilisees = CATALOGUE_MATERIEL.map { it.silhouette }.toSet()
        assertEquals(Silhouette.entries.toSet(), utilisees)
    }

    @Test
    fun `les etageres ne sont pas vides et le catalogue est large`() {
        (ETAGERES_CAMERAS + ETAGERES_OBJECTIFS).forEach { assertTrue(it.entrees.isNotEmpty(), it.titre) }
        assertTrue(CATALOGUE_CAMERAS.size >= 40)
        assertTrue(CATALOGUE_OBJECTIFS.size >= 25)
    }

    @Test
    fun `le nom complet ne double pas la marque`() {
        val arriflex = CATALOGUE_CAMERAS.first { it.nom == "ARRIFLEX 535" }
        assertEquals("ARRIFLEX 535", arriflex.nomComplet)
        val mini = CATALOGUE_CAMERAS.first { it.nom == "ALEXA Mini" }
        assertEquals("ARRI ALEXA Mini", mini.nomComplet)
    }

    @Test
    fun `une annee approximative se dit`() {
        val bnc = CATALOGUE_CAMERAS.first { it.fabricant == "Mitchell" }
        assertEquals("vers 1935", bnc.libelleAnnee)
    }

    @Test
    fun `la recherche ignore accents et casse et fouille les films`() {
        assertTrue(chercherMateriel("cameflex", CATALOGUE_CAMERAS).any { it.nom == "Caméflex" })
        assertTrue(chercherMateriel("DUNKERQUE", CATALOGUE_CAMERAS).any { it.fabricant == "IMAX" })
        assertTrue(chercherMateriel("deakins 1917", CATALOGUE_MATERIEL).size >= 2)
        assertEquals(CATALOGUE_CAMERAS, chercherMateriel("   ", CATALOGUE_CAMERAS))
        assertTrue(chercherMateriel("xyzzy introuvable", CATALOGUE_MATERIEL).isEmpty())
    }
}

class QuizMaterielTest {

    private val alexa35 = CATALOGUE_CAMERAS.first { it.nom == "ALEXA 35" }

    @Test
    fun `le masque cache le nom et ses mots distinctifs, sans toucher au reste`() {
        val masque = QuizMateriel.masquer("L'ALEXA 35 est une Alexa ; Alexandra n'est pas concernée.", alexa35)
        assertEquals("L'… est une … ; Alexandra n'est pas concernée.", masque)
    }

    @Test
    fun `les mots courants ne sont pas masques`() {
        val superSpeed = CATALOGUE_OBJECTIFS.first { it.nom == "Super Speed" }
        assertEquals("Couverture : Super 35", QuizMateriel.masquer("Couverture : Super 35", superSpeed))
    }

    @Test
    fun `aucun indice ne donne la reponse`() {
        CATALOGUE_MATERIEL.forEach { m ->
            QuizMateriel.indices(m).forEach { indice ->
                assertFalse(QuizMateriel.devoile(indice, m), "${m.nomComplet} se trahit : $indice")
            }
        }
    }

    @Test
    fun `une question qui suis-je est bien formee`() {
        val alea = Random(7)
        repeat(300) {
            val q = assertNotNull(QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, CATALOGUE_MATERIEL, alea))
            assertEquals(QuizMateriel.NOMBRE_DE_CHOIX, q.choix.size)
            assertEquals(q.choix.size, q.choix.map { it.nomComplet }.toSet().size, "propositions en double")
            assertTrue(q.bonneReponse in q.choix.indices)
            assertTrue(q.choix.all { it.genre == q.reponse.genre }, "genres mélangés")
            assertTrue(q.indices.isNotEmpty())
        }
    }

    @Test
    fun `une question de film n'a qu'une bonne reponse`() {
        val alea = Random(11)
        repeat(500) {
            val q = assertNotNull(QuizMateriel.question(GenreQuestion.QUEL_FILM, CATALOGUE_MATERIEL, alea))
            val titre = q.enonce.substringAfter("« ").substringBefore(" »")
            val base = QuizMateriel.titreDeBase(titre)
            assertTrue(q.reponse.films.any { QuizMateriel.titreDeBase(it.titre) == base }, q.enonce)
            q.choix.filter { it != q.reponse }.forEach { leurre ->
                assertFalse(
                    leurre.films.any { QuizMateriel.titreDeBase(it.titre) == base },
                    "${leurre.nomComplet} a aussi servi sur « $titre »"
                )
            }
            q.indices.forEach { assertFalse(QuizMateriel.devoile(it, q.reponse), it) }
        }
    }

    @Test
    fun `le meme alea donne la meme question`() {
        val a = QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, CATALOGUE_MATERIEL, Random(42))
        val b = QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, CATALOGUE_MATERIEL, Random(42))
        assertEquals(a, b)
    }

    @Test
    fun `on ne repose pas la question qu'on vient de poser`() {
        val alea = Random(3)
        val parmi = CATALOGUE_OBJECTIFS
        var precedente: Materiel? = null
        repeat(200) {
            val q = assertNotNull(QuizMateriel.question(GenreQuestion.QUI_SUIS_JE, parmi, alea, eviter = precedente))
            assertTrue(q.reponse != precedente)
            precedente = q.reponse
        }
    }

    @Test
    fun `les titres se comparent sans leurs precisions`() {
        assertEquals(
            QuizMateriel.titreDeBase("Killers of the Flower Moon"),
            QuizMateriel.titreDeBase("Killers of the Flower Moon (scènes numériques)")
        )
    }
}
