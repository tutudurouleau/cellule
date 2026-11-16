package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TournageTest {

    @Test
    fun `la vitesse decoule de la cadence et de l angle`() {
        assertEquals(1.0 / 48, Tournage.tempsPose(180.0, 24.0), 1e-12)
        assertEquals(1.0 / 50, Tournage.tempsPose(172.8, 24.0), 1e-12)
        assertEquals(1.0 / 50, Tournage.tempsPose(180.0, 25.0), 1e-12)
        assertEquals(1.0 / 24, Tournage.tempsPose(360.0, 24.0), 1e-12)
        assertEquals(1.0 / 960, Tournage.tempsPose(45.0, 120.0), 1e-12)
        assertTrue(Tournage.tempsPose(180.0, 0.0).isNaN())
        assertEquals(172.8, Tournage.anglePour(1.0 / 50, 24.0), 1e-9)
    }

    @Test
    fun `les listes du cahier des charges sont la`() {
        assertEquals(listOf(23.976, 24.0, 25.0, 29.97, 30.0, 48.0, 50.0, 60.0, 100.0, 120.0), CADENCES)
        assertEquals(listOf(360.0, 270.0, 180.0, 172.8, 144.0, 90.0, 45.0), ANGLES_OBTURATION)
        assertEquals(listOf(0.0, 0.3, 0.6, 0.9, 1.2, 1.5, 1.8, 2.1), DENSITES_NEUTRES.take(8).map { Math.round(it.densite * 10) / 10.0 })
    }

    @Test
    fun `les T-stops vont au tiers et les pleins tombent juste`() {
        assertEquals("T1", T_STOPS.first().libelle)
        assertEquals("T32", T_STOPS.last().libelle)
        listOf(1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0).forEachIndexed { i, plein ->
            val t = T_STOPS[i * 3]
            assertEquals(plein, t.nominal, 1e-9)
            /* La valeur gravée est un arrondi de la valeur exacte. */
            assertEquals(plein, t.exact, plein * 0.04)
        }
        /* Trois graduations par diaph : d'un rang à l'autre, un tiers. */
        T_STOPS.zipWithNext().forEach { (a, b) ->
            assertEquals(1.0 / 3, 2 * kotlin.math.log2(b.exact / a.exact), 1e-9)
            assertTrue(b.nominal > a.nominal)
        }
        assertEquals("T2,8", Tournage.tStopLePlusProche(2.83).libelle)
        assertEquals("T3,2", Tournage.tStopLePlusProche(3.1).libelle)
    }

    @Test
    fun `plein soleil a 800 EI demande de fermer tres fort`() {
        /* EV 15 à 100 ISO, 800 EI, 1/48 s : N² = 2^18 / 48 → N ≈ 73,9. */
        val r = Tournage.calculer(15.0, ReglageTournage(24.0, 180.0, 800))
        assertEquals(73.9, r.tExact, 0.1)
        assertTrue(r.horsBague)
        assertNull(r.nd)
    }

    @Test
    fun `une lecture ordinaire tombe sur la bague avec son ecart d arrondi`() {
        /* EV 9, 800 EI, 1/48 s : N² = 2^12 / 48 = 85,3 → N = 9,24. */
        val r = Tournage.calculer(9.0, ReglageTournage(24.0, 180.0, 800))
        assertEquals(9.24, r.tExact, 0.01)
        assertEquals("T9", r.tProche.libelle)
        assertFalse(r.horsBague)
        assertTrue(kotlin.math.abs(r.ecartArrondi) <= 1.0 / 6 + 1e-9)
    }

    @Test
    fun `les filtres en place ouvrent le diaphragme d autant`() {
        val nu = Tournage.ouvertureExacte(12.0, ReglageTournage(24.0, 180.0, 800))
        val nd = Tournage.ouvertureExacte(12.0, ReglageTournage(24.0, 180.0, 800, ndEnPlace = 2))
        assertEquals(2.0, nu / nd, 1e-9)
        val maison = FiltrePose("Filtre maison", FamilleFiltre.AUTRE, 1.0)
        val avec = Tournage.ouvertureExacte(12.0, ReglageTournage(24.0, 180.0, 800, 2, listOf(maison)))
        assertEquals(3.0, 2 * kotlin.math.log2(nu / avec), 1e-9)
        assertEquals("perte saisie par toi", maison.mention)
        assertEquals(3.0, ReglageTournage(ndEnPlace = 2, filtres = listOf(maison)).perteFiltres, 1e-12)
    }

    @Test
    fun `je veux T2,8 - quel neutre`() {
        /* Plein soleil, 800 EI, 180° à 24 im/s : T73,9 mesuré. De T73,9 à T2,8 :
           2·log2(73,9 / 2,83) = 9,4 diaphs → ND 2,7 (9 diaphs), reste 0,4. */
        val r = Tournage.calculer(15.0, ReglageTournage(24.0, 180.0, 800), T_STOPS.first { it.nominal == 2.8 })
        val nd = assertNotNull(r.nd)
        assertEquals(9.41, nd.aRetenir, 0.02)
        assertEquals(9, nd.nd.diaphs)
        assertEquals(2.7, nd.nd.densite, 1e-9)
        assertEquals(0.41, nd.residuel, 0.02)
        assertFalse(nd.manqueDeLumiere)
        assertFalse(nd.horsSerie)
    }

    @Test
    fun `quand il manque de la lumiere aucun neutre n est propose`() {
        val r = Tournage.calculer(5.0, ReglageTournage(24.0, 180.0, 800), T_STOPS.first { it.nominal == 5.6 })
        val nd = assertNotNull(r.nd)
        assertTrue(nd.manqueDeLumiere)
        assertEquals(0, nd.nd.diaphs)
        assertTrue(nd.residuel < 0)
    }

    @Test
    fun `au-dela de la serie on le dit`() {
        val nd = Tournage.ndPour(90.0, T_STOPS.first())
        assertTrue(nd.horsSerie)
        assertEquals(10, nd.nd.diaphs)
    }

    @Test
    fun `les libelles sont en francais`() {
        assertEquals("1/48 s", Tournage.libelleTempsPose(1.0 / 48))
        assertEquals("1/50 s", Tournage.libelleTempsPose(Tournage.tempsPose(172.8, 24.0)))
        assertEquals("1/48 s", Tournage.libelleTempsPose(Tournage.tempsPose(180.0, 23.976)))
        assertEquals("1/960 s", Tournage.libelleTempsPose(1.0 / 960))
        assertEquals("23,976 im/s", Tournage.libelleCadence(23.976))
        assertEquals("24 im/s", Tournage.libelleCadence(24.0))
        assertEquals("172,8°", Tournage.libelleAngle(172.8))
        assertEquals("ND 0,9 · 3 diaphs", DensiteNeutre(3).libelle)
        assertEquals("ND 0,3 · 1 diaph", DensiteNeutre(1).libelle)
        assertEquals(23.976, Tournage.lireNombre(" 23,976 "))
        assertNull(Tournage.lireNombre("vingt"))
        assertNull(Tournage.lireNombre("-3"))
    }
}

class OutilsImageTest {

    /** Une image 8 × 4 en plage pleine : un dégradé de gauche à droite. */
    private fun degrade(): ByteArray = ByteArray(8 * 4) { i -> ((i % 8) * 255 / 7).toByte() }

    @Test
    fun `le pourcentage suit la plage du flux`() {
        assertEquals(0, OutilsImage.pourcent(16, plageVideo = true))
        assertEquals(100, OutilsImage.pourcent(235, plageVideo = true))
        assertEquals(100, OutilsImage.pourcent(255, plageVideo = true))
        assertEquals(0, OutilsImage.pourcent(0, plageVideo = true))
        assertEquals(50, OutilsImage.pourcent(128, plageVideo = false))
    }

    @Test
    fun `la grille garde le sens de l ecran`() {
        val g = OutilsImage.grille(degrade(), 8, 4, 8, plageVideo = false, grandCote = 8)
        assertEquals(8, g.largeur)
        assertEquals(4, g.hauteur)
        assertEquals(0, g.niveau(0, 0))
        assertEquals(100, g.niveau(7, 3))
        /* Tournée d'un quart de tour, l'image devient 4 × 8 et le dégradé vertical. */
        val t = OutilsImage.grille(degrade(), 8, 4, 8, rotation = 90, plageVideo = false, grandCote = 8)
        assertEquals(4, t.largeur)
        assertEquals(8, t.hauteur)
        assertEquals(t.niveau(0, 2), t.niveau(3, 2))
        assertTrue(t.niveau(0, 0) != t.niveau(0, 7))
    }

    @Test
    fun `l histogramme compte chaque point une fois`() {
        val g = OutilsImage.grille(degrade(), 8, 4, 8, plageVideo = false, grandCote = 8)
        val h = OutilsImage.histogramme(g, 10)
        assertEquals(32, h.sum())
        assertEquals(4, h.first())
        assertEquals(4, h.last())
    }

    @Test
    fun `la forme d onde range les niveaux par colonne, le clair en haut`() {
        val g = OutilsImage.grille(degrade(), 8, 4, 8, plageVideo = false, grandCote = 8)
        val onde = OutilsImage.formeDOnde(g, colonnes = 8, niveaux = 11)
        assertEquals(32, onde.sum())
        /* Colonne de gauche : noire, donc tout en bas (dernière ligne). */
        assertEquals(4, onde[10 * 8 + 0])
        /* Colonne de droite : blanche, donc tout en haut (première ligne). */
        assertEquals(4, onde[0 * 8 + 7])
    }

    @Test
    fun `le faux contraste colore les extremes et laisse voir le reste`() {
        val g = OutilsImage.grille(degrade(), 8, 4, 8, plageVideo = false, grandCote = 8)
        val calque = OutilsImage.fauxContraste(g)
        assertTrue(calque[0] != OutilsImage.TRANSPARENT)
        assertTrue(calque[7] != OutilsImage.TRANSPARENT)
        assertTrue(calque[0] != calque[7])
        /* La palette couvre 0 à 100 sans trou ni chevauchement. */
        assertEquals(0, OutilsImage.PALETTE.first().de)
        assertEquals(101, OutilsImage.PALETTE.last().a)
        OutilsImage.PALETTE.zipWithNext().forEach { (a, b) -> assertEquals(a.a, b.de) }
    }

    @Test
    fun `les zebras ne rayent que ce qui depasse`() {
        val g = OutilsImage.grille(degrade(), 8, 4, 8, plageVideo = false, grandCote = 8)
        val z = OutilsImage.zebras(g, SeuilsZebras(haut = 95, moyen = 70))
        /* Colonne 7 (100 %) : rayée du seuil haut, une case sur deux. */
        val haut = (0 until 4).count { z[it * 8 + 7] == OutilsImage.ZEBRA_HAUT }
        assertEquals(2, haut)
        /* Colonne 5 (71 %) : dans la bande du seuil moyen. */
        assertTrue((0 until 4).any { z[it * 8 + 5] == OutilsImage.ZEBRA_MOYEN })
        /* Colonnes sombres : rien. */
        assertTrue((0 until 4).all { z[it * 8 + 1] == OutilsImage.TRANSPARENT })
        assertEquals(0.125, OutilsImage.partAuDessus(g, 95), 1e-9)
    }
}
