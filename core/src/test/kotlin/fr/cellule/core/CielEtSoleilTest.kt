package fr.cellule.core

import java.time.LocalDate
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoleilTest {

    private fun jour(annee: Int, mois: Int, jour: Int) =
        LocalDate.of(annee, mois, jour).dayOfYear

    /* Éphéméride du 6 septembre 2026 à Corte (42,30° N — 9,15° E, UTC+2). */
    private val sept6 = jour(2026, 9, 6)

    @Test
    fun `position du soleil a Corte`() {
        assertEquals(249, sept6)
        assertEquals(6.5, Soleil.declinaison(sept6), 0.4)
        assertEquals(1.4, Soleil.equationDuTemps(sept6), 0.4)
        assertEquals(13.37, Soleil.midiSolaire(sept6, 9.15, 2.0), 0.06)
        assertEquals(54.2, Soleil.hauteur(42.30, sept6, 12.0), 0.4)
    }

    @Test
    fun `a 17 heures le soleil est a 30 degres, pas a 40`() {
        val tsv = Soleil.tempsSolaire(17.0, sept6, 9.15, 2.0)
        assertEquals(30.2, Soleil.hauteur(42.30, sept6, tsv), 0.5)
    }

    @Test
    fun `solstices et equinoxes`() {
        val ete = jour(2026, 6, 21)
        val hiver = jour(2026, 12, 21)
        val equinoxe = jour(2026, 3, 20)
        assertEquals(23.44, Soleil.declinaison(ete), 0.1)
        assertEquals(-23.44, Soleil.declinaison(hiver), 0.1)
        assertEquals(0.0, Soleil.declinaison(equinoxe), 0.6)
        assertEquals(71.1, Soleil.hauteur(42.30, ete, 12.0), 0.5)
        assertEquals(24.3, Soleil.hauteur(42.30, hiver, 12.0), 0.5)
        assertEquals(69.8, Soleil.hauteur(43.65, ete, 12.0), 0.5)
        assertEquals(22.9, Soleil.hauteur(43.65, hiver, 12.0), 0.5)
    }

    @Test
    fun `l ombre portee sert de goniometre`() {
        assertEquals(45.0, Soleil.hauteurDepuisOmbre(1.0), 0.01)
        assertEquals(26.6, Soleil.hauteurDepuisOmbre(2.0), 0.1)
        assertEquals(9.5, Soleil.hauteurDepuisOmbre(6.0), 0.1)
    }
}

class CielTest {

    @Test
    fun `l etalonnage tombe sur Sunny 16`() {
        assertEquals(15.0, Ciel.eclairement(52.0, CIELS[0]).ev100, 0.06)
        assertEquals(82000.0, Ciel.eclairement(52.0, CIELS[0]).total, 3000.0)
    }

    @Test
    fun `les etats du ciel valent ce que dit le document`() {
        val ref = Ciel.eclairement(52.0, CIELS[0]).ev100
        assertEquals(-1.0, Ciel.eclairement(52.0, CIELS[1]).ev100 - ref, 0.2)   // voile : −1
        assertEquals(-2.4, Ciel.eclairement(52.0, CIELS[3]).ev100 - ref, 0.45)  // couvert : −2 à −3
        assertEquals(-4.3, Ciel.eclairement(52.0, CIELS[4]).ev100 - ref, 0.6)   // orage : −4 à −5
    }

    @Test
    fun `sous quinze degres le sinus devient trop optimiste`() {
        listOf(90.0 to 0.42, 60.0 to 0.18, 45.0 to -0.17, 30.0 to -0.77,
               15.0 to -1.98, 10.0 to -2.73, 5.0 to -4.0).forEach { (h, attendu) ->
            assertEquals(attendu, Ciel.eclairement(h, CIELS[0]).ev100 - 15, 0.12, "à $h°")
        }
        /* À 5°, la table plate du document annonce −3,5 : un demi-diaph de trop. */
        val ecart = (Ciel.eclairement(5.0, CIELS[0]).ev100 - 15) - Ciel.perteHauteurPlate(5.0)
        assertEquals(-0.5, ecart, 0.3)
    }

    @Test
    fun `la lumiere decroit toujours quand le soleil descend`() {
        var precedent = Double.MAX_VALUE
        var h = 90.0
        while (h >= -18.0) {
            val ev = Ciel.eclairement(h, CIELS[0]).ev100
            assertTrue(ev < precedent, "remontée à $h°")
            precedent = ev
            h -= 0.5
        }
    }

    @Test
    fun `un obstacle ne coupe que le soleil qui existe`() {
        val plein = Ciel.eclairement(52.0, CIELS[0])
        val couvert = Ciel.eclairement(52.0, CIELS[3])
        assertEquals(1.0, plein.partSoleil, 0.05)
        assertEquals(0.0, couvert.partSoleil, 1e-9)

        val ombre = OBSTACLES.first { it.id == "ombre" }
        val sousbois = OBSTACLES.first { it.id == "sousbois" }
        assertEquals(-2.5, ombre.diaphs(plein.partSoleil), 0.05)
        assertEquals(0.0, ombre.diaphs(couvert.partSoleil), 1e-9)
        /* La canopée, elle, bouche le ciel lui-même. */
        assertEquals(-3.0, sousbois.diaphs(couvert.partSoleil), 0.05)
    }

    @Test
    fun `le crepuscule suit les reperes du document`() {
        assertEquals(3.4, Ciel.luxCrepuscule(-6.0), 0.2)          // civil
        assertEquals(0.44, Photometrie.evDepuisLux(Ciel.luxCrepuscule(-6.0)), 0.2)
        assertEquals(0.008, Ciel.luxCrepuscule(-12.0), 0.002)     // nautique
    }

    @Test
    fun `la masse d air vaut un au zenith`() {
        assertEquals(1.0, Ciel.masseAir(90.0), 0.001)
        assertEquals(1.41, Ciel.masseAir(45.0), 0.02)
        assertTrue(Ciel.masseAir(5.0) > 10)
    }
}
