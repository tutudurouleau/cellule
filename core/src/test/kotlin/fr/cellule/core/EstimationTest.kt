package fr.cellule.core

import java.time.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EstimationTest {

    @Test
    fun `la scene par defaut est Sunny 16`() {
        val e = Estimateur.calculer(Scene())
        assertEquals(15.0, e.ev100Scene, 0.06)
        val c = coupleConseille(e.evAppareil, 1.0 / 100)!!
        assertEquals(16.0, c.ouverture, 1e-9)
        assertEquals("1/125", c.vitesse.libelle)
    }

    @Test
    fun `l exemple de la Restonica`() {
        /* Corte, 6 septembre, 17 h, sous un arbre isolé au bord de l'eau. */
        val jour = LocalDate.of(2026, 9, 6).dayOfYear
        val hauteur = Soleil.hauteur(42.30, jour, Soleil.tempsSolaire(17.0, jour, 9.15, 2.0))
        val e = Estimateur.calculer(
            Scene(
                ciel = CIELS[0],
                hauteurSoleil = hauteur,
                obstacle = OBSTACLES.first { it.id == "arbre" },
                renvois = setOf("eau", "sec"),
                iso = 400
            )
        )
        assertEquals(14.1, e.ev100Scene, 0.25)
        val c = coupleConseille(e.evAppareil, 1.0 / 400)!!
        assertTrue(c.ouverture in listOf(8.0, 11.0), "attendu f/8 ou f/11, obtenu f/${c.ouverture}")
    }

    @Test
    fun `la grille mentale et le modele physique restent proches en plein soleil`() {
        val base = Scene(hauteurSoleil = 52.0)
        val physique = Estimateur.calculer(base.copy(modele = Modele.PHYSIQUE)).ev100Scene
        val plate = Estimateur.calculer(base.copy(modele = Modele.GRILLE_MENTALE)).ev100Scene
        assertTrue(abs(physique - plate) < 0.5, "physique $physique contre plate $plate")
    }

    @Test
    fun `par temps couvert la hauteur du soleil compte beaucoup moins`() {
        val couvert = CIELS.first { it.id == "couvert" }
        val hautCouvert = Estimateur.calculer(Scene(ciel = couvert, hauteurSoleil = 52.0)).ev100Scene
        val basCouvert = Estimateur.calculer(Scene(ciel = couvert, hauteurSoleil = 20.0)).ev100Scene
        val hautSoleil = Estimateur.calculer(Scene(hauteurSoleil = 52.0)).ev100Scene
        val basSoleil = Estimateur.calculer(Scene(hauteurSoleil = 20.0)).ev100Scene
        /* Le même écart de hauteur coûte bien plus cher sous un ciel pur. */
        assertTrue((hautSoleil - basSoleil) > (hautCouvert - basCouvert) + 0.4)
    }

    @Test
    fun `la chaine s affiche dans l ordre et se termine sur l appareil`() {
        val e = Estimateur.calculer(
            Scene(obstacle = OBSTACLES.first { it.id == "sousbois" }, renvois = setOf("neige"), iso = 400)
        )
        assertEquals("Lumière du ciel", e.termes.first().nom)
        assertEquals("Appareil", e.termes.last().nom)
        assertTrue(e.termes.any { it.nom == "Sous-bois dense" })
        assertTrue(e.termes.any { it.nom == "Neige fraîche plein cadre" })
        assertTrue(e.termes.any { it.nom == "400 ISO" })
        /* La somme des termes doit redonner le total affiché. */
        val diaphs = e.termes.mapNotNull { it.diaphs }.filter { it != Photometrie.decalageIso(400) }.sum()
        assertEquals(e.ev100Scene, e.ev100Ciel + diaphs, 1e-9)
    }
}

class MatieresTest {

    @Test
    fun `les ecarts au gris`() {
        assertEquals(2.24, MATIERES.first { it.nom == "Neige fraîche" }.ecartAuGris, 0.02)
        assertEquals(0.96, MATIERES.first { it.nom == "Peau claire" }.ecartAuGris, 0.06)
        assertEquals(0.0, MATIERES.first { it.reflectance == 0.18 }.ecartAuGris, 1e-9)
    }

    @Test
    fun `la zone I vaut 1,1 pour cent et non 0,6`() {
        assertEquals(1.125, ZONES.first { it.chiffre == "I" }.reflectance * 100, 0.01)
        assertEquals(18.0, ZONES.first { it.chiffre == "V" }.reflectance * 100, 0.01)
        assertEquals(72.0, ZONES.first { it.chiffre == "VII" }.reflectance * 100, 0.5)
    }

    @Test
    fun `placer une lecture en zone III revient a fermer de deux diaphs`() {
        val zoneIII = ZONES.first { it.chiffre == "III" }
        assertEquals(14.0, Zones.expositionPourPlacer(12.0, zoneIII), 1e-9)
    }

    @Test
    fun `le piege de la neige`() {
        /* Le spot lit 2⅓ diaphs de trop ; suivre la lecture rend la neige grise. */
        val neige = MATIERES.first { it.nom == "Neige fraîche" }
        assertEquals(2.24, Zones.correctionReflectance(neige.reflectance), 0.02)
        assertEquals(7.2, Zones.zoneNaturelle(neige.reflectance), 0.05)
        assertEquals("VII", Zones.zoneLaPlusProche(Zones.zoneNaturelle(neige.reflectance)).chiffre)
    }
}
