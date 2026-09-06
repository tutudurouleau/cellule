package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReperesTest {

    @Test
    fun `sans rotation le repere ne bouge pas`() {
        assertEquals(0.3f to 0.7f, Reperes.ecranVersCapteur(0.3f, 0.7f, 0))
    }

    @Test
    fun `le centre reste le centre quelle que soit la rotation`() {
        listOf(0, 90, 180, 270).forEach { r ->
            val (x, y) = Reperes.ecranVersCapteur(0.5f, 0.5f, r)
            assertEquals(0.5f, x, 1e-6f, "rotation $r")
            assertEquals(0.5f, y, 1e-6f, "rotation $r")
        }
    }

    @Test
    fun `aller et retour redonnent le point de depart`() {
        listOf(0, 90, 180, 270, 450, -90).forEach { r ->
            listOf(0.1f to 0.2f, 0.8f to 0.3f, 0f to 1f).forEach { (u, v) ->
                val (x, y) = Reperes.ecranVersCapteur(u, v, r)
                val (u2, v2) = Reperes.capteurVersEcran(x, y, r)
                assertEquals(u, u2, 1e-6f, "rotation $r")
                assertEquals(v, v2, 1e-6f, "rotation $r")
            }
        }
    }

    @Test
    fun `en portrait le haut de l ecran est le bord droit du capteur`() {
        /* Rotation 90 : le tampon est en paysage, tourné d'un quart de tour
           pour l'affichage. Viser le haut de l'écran, c'est viser la colonne
           de droite du tampon. */
        val (x, y) = Reperes.ecranVersCapteur(0.5f, 0.02f, 90)
        assertEquals(0.02f, x, 1e-6f)
        assertTrue(y > 0.4f && y < 0.6f)
    }
}

class JournalTest {

    private fun entree(annonce: Double, mesure: Double, type: String = "Plein soleil") =
        EntreeJournal(
            identifiant = 1, date = "2026-09-06", typeScene = type,
            annonce = annonce, mesure = mesure
        )

    @Test
    fun `un journal vide n a pas de bilan`() {
        assertEquals(null, Statistiques.bilan(emptyList()))
    }

    @Test
    fun `le biais est l ecart moyen signe`() {
        val b = Statistiques.bilan(listOf(entree(15.0, 14.0), entree(13.0, 13.0), entree(12.0, 11.0)))!!
        assertEquals(3, b.nombre)
        assertEquals(0.667, b.biais, 0.01)
        assertEquals(0.667, b.erreurAbsolue, 0.01)
    }

    @Test
    fun `des erreurs opposees s annulent en biais mais pas en absolu`() {
        val b = Statistiques.bilan(listOf(entree(15.0, 14.0), entree(13.0, 14.0)))!!
        assertEquals(0.0, b.biais, 1e-9)
        assertEquals(1.0, b.erreurAbsolue, 1e-9)
    }

    @Test
    fun `les proportions dans la cible`() {
        val b = Statistiques.bilan(
            listOf(entree(15.0, 14.8), entree(13.0, 12.3), entree(12.0, 9.0), entree(10.0, 10.0))
        )!!
        assertEquals(0.5, b.partDansDemiDiaph, 1e-9)
        assertEquals(0.75, b.partDansUnDiaph, 1e-9)
    }

    @Test
    fun `le decoupage par type sort le plus biaise en premier`() {
        /* Quatre entrées au minimum par type, sinon le bilan refuse de
           conclure — et il a raison de refuser. */
        val entrees = listOf(
            entree(15.0, 15.0, "Plein soleil"), entree(14.0, 14.1, "Plein soleil"),
            entree(13.0, 12.9, "Plein soleil"), entree(15.0, 15.1, "Plein soleil"),
            entree(11.0, 9.0, "Sous-bois"), entree(10.0, 8.2, "Sous-bois"),
            entree(11.5, 9.4, "Sous-bois"), entree(9.0, 7.1, "Sous-bois")
        )
        val parType = Statistiques.parType(entrees)
        assertEquals("Sous-bois", parType.first().first)
        assertTrue(parType.first().second.biais > 1.5)
        assertEquals("calé", parType.last().second.verdict)
    }

    @Test
    fun `sous quatre entrees le bilan refuse de conclure`() {
        val b = Statistiques.bilan(listOf(entree(15.0, 12.0), entree(14.0, 11.0)))!!
        assertEquals("trop peu d’entrées", b.verdict)
    }

    @Test
    fun `le verdict nomme le sens de l erreur`() {
        val sousExpose = Statistiques.bilan(List(5) { entree(14.0, 12.0) })!!
        assertTrue(sousExpose.verdict.contains("sous-expose"))
        val surExpose = Statistiques.bilan(List(5) { entree(12.0, 14.0) })!!
        assertTrue(surExpose.verdict.contains("surexpose"))
    }

    @Test
    fun `une photo notee sans exercice ne fausse pas le bilan`() {
        /* On note une vue sans avoir annoncé : elle appartient au carnet,
           pas à l'entraînement. */
        val photo = EntreeJournal(
            identifiant = 2, date = "2026-09-06", pellicule = "Kodak Portra 400",
            vue = "12", sujet = "La Restonica au soleil rasant", reglage = "f/8 · 1/500"
        )
        assertEquals(null, photo.ecart)
        assertEquals(false, photo.compteAuBilan)
        val bilan = Statistiques.bilan(listOf(photo, entree(15.0, 14.0)))!!
        assertEquals(1, bilan.nombre)
        assertEquals(1.0, bilan.biais, 1e-9)
        assertEquals("vue 12 · Kodak Portra 400 · f/8 · 1/500", photo.resume)
    }

    @Test
    fun `le numero de vue suit la pellicule chargee`() {
        val entrees = listOf(
            EntreeJournal(1, "2026-09-06", pellicule = "Tri-X 400", vue = "11"),
            EntreeJournal(2, "2026-09-06", pellicule = "Tri-X 400", vue = "12"),
            EntreeJournal(3, "2026-09-06", pellicule = "Portra 400", vue = "3")
        )
        assertEquals("13", Statistiques.vueSuivante(entrees, "Tri-X 400"))
        assertEquals("4", Statistiques.vueSuivante(entrees, "Portra 400"))
        /* Une pellicule qu'on vient de charger repart à un. */
        assertEquals("1", Statistiques.vueSuivante(entrees, "HP5 Plus 400"))
        assertEquals("1", Statistiques.vueSuivante(emptyList(), "Tri-X 400"))
    }

    @Test
    fun `chaque pellicule connait sa sensibilite et sa latitude`() {
        val portra = PELLICULES.first { it.nom == "Kodak Portra 400" }
        assertEquals(400, portra.iso)
        assertEquals(TypeFilm.NEGATIF_COULEUR, portra.type)
        assertTrue(conseilLatitude(TypeFilm.NEGATIF_COULEUR).contains("surexpose"))
        assertTrue(conseilLatitude(TypeFilm.INVERSIBLE).contains("ferme"))
        /* Aucune pellicule sans sensibilité plausible. */
        PELLICULES.forEach { assertTrue(it.iso in 25..6400, it.nom) }
    }

    @Test
    fun `le type de scene suggere suit l EV`() {
        val ouvert = OBSTACLES.first { it.id == "ouvert" }
        assertEquals("Plein soleil", Statistiques.typeSuggere(15.0, ouvert))
        assertEquals("Couvert", Statistiques.typeSuggere(13.0, ouvert))
        assertEquals("Intérieur jour", Statistiques.typeSuggere(8.0, ouvert))
        assertEquals("Sous-bois", Statistiques.typeSuggere(15.0, OBSTACLES.first { it.id == "sousbois" }))
    }
}
