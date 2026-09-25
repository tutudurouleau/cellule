package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DilutionsTest {

    @Test
    fun `1+9 est un dixieme, pas un neuvieme`() {
        val d = Dilution.lire("1+9")!!
        assertEquals(50.0, d.concentre(500.0), 1e-9)
        assertEquals(450.0, d.eau(500.0), 1e-9)
        assertEquals(0.1, d.fraction, 1e-12)
    }

    @Test
    fun `les notations se lisent pareil`() {
        assertEquals(31.0, Dilution.lire("1:31")!!.partsEau, 1e-9)
        assertEquals(50.0, Dilution.lire(" 1 + 50 ")!!.partsEau, 1e-9)
        assertEquals(9.0, Dilution.lire("9")!!.partsEau, 1e-9)
        assertEquals(0.0, Dilution.lire("stock")!!.partsEau, 1e-9)
        assertEquals(1.5, Dilution.lire("2+3")!!.partsEau, 1e-9)
        assertNull(Dilution.lire("beaucoup"))
        assertNull(Dilution.lire("0+9"))
    }

    @Test
    fun `les libelles restent lisibles`() {
        assertEquals("1+9", Dilution(9.0).libelle)
        assertEquals("1+0", Dilution(0.0).libelle)
        assertEquals("1+1,5", Dilution(1.5).libelle)
    }

    @Test
    fun `le volume que donne un reste de concentre`() {
        assertEquals(510.0, Dilution(50.0).volumePour(10.0), 1e-9)
        val p = Preparation(Dilution(31.0), 480.0)
        assertEquals(15.0, p.concentre, 1e-9)
        assertEquals(465.0, p.eau, 1e-9)
    }
}

class TirageDiaphsTest {

    @Test
    fun `un diaph double le temps, un tiers le multiplie par 1,26`() {
        assertEquals(20.0, TirageDiaphs.temps(10.0, 1.0), 1e-9)
        assertEquals(5.0, TirageDiaphs.temps(10.0, -1.0), 1e-9)
        assertEquals(12.6, TirageDiaphs.temps(10.0, 1.0 / 3), 0.01)
        assertEquals(1.0, TirageDiaphs.ecart(8.0, 16.0), 1e-9)
    }

    @Test
    fun `bruler et masquer d un diaph`() {
        assertEquals(10.0, TirageDiaphs.brulage(10.0, 1.0), 1e-9)
        assertEquals(5.0, TirageDiaphs.masquage(10.0, 1.0), 1e-9)
        // Un demi-diaph de masquage : la zone reçoit 10/√2 ≈ 7,07 s.
        assertEquals(2.93, TirageDiaphs.masquage(10.0, 0.5), 0.01)
        assertEquals(40.0, TirageDiaphs.apresFermeture(10.0, 2.0), 1e-9)
    }

    @Test
    fun `la bande d essai en diaphs ajoute des temps croissants`() {
        val bandes = BandeEssai.diaphs(premier = 4.0, pas = 0.5, nombre = 5)
        assertEquals(listOf(4.0, 5.66, 8.0, 11.31, 16.0), bandes.map { Math.round(it.total * 100) / 100.0 })
        assertEquals(4.0, bandes[0].ajout, 1e-9)
        assertEquals(1.66, bandes[1].ajout, 0.01)
        assertEquals(bandes.last().total, bandes.sumOf { it.ajout }, 1e-9)
    }

    @Test
    fun `la bande lineaire se tasse`() {
        val bandes = BandeEssai.lineaire(premier = 5.0, increment = 5.0, nombre = 5)
        val pas = bandes.zipWithNext { a, b -> b.diaphs - a.diaphs }
        assertEquals(1.0, pas.first(), 1e-9)       // 5 → 10 s : un diaph entier
        assertEquals(0.32, pas.last(), 0.01)       // 20 → 25 s : un tiers
        assertTrue(pas.zipWithNext().all { (a, b) -> b < a })
    }
}

class PronosticsTest {

    @Test
    fun `l ecart relatif et en diaphs`() {
        val temps = Pronostic(Calculateur.TEMPERATURE, "2026-09-25", estime = 600.0, calcule = 500.0)
        assertEquals(0.2, temps.ecart, 1e-9)
        assertEquals("+20 %", temps.libelleEcart)
        assertEquals(Verdict.LOIN, temps.verdict)

        val tirage = Pronostic(Calculateur.TIRAGE, "2026-09-25", estime = 10.0, calcule = 12.6)
        assertEquals(-1.0 / 3, tirage.ecart, 0.01)
        assertEquals("−⅓ diaph", tirage.libelleEcart)
        assertEquals(Verdict.PROCHE, tirage.verdict)
    }

    @Test
    fun `la progression compare le debut et la fin`() {
        val estimes = listOf(700, 680, 400, 650, 380, 520, 505, 490, 500, 510)
        val liste = estimes.map { Pronostic(Calculateur.TEMPERATURE, "", it.toDouble(), 500.0) }
        val p = Progressions.progression(liste)!!
        assertTrue(p.erreurRecente < p.erreurDebut)
        assertTrue(p.progresse)
        assertEquals(0.5, p.partJustes, 1e-9)
    }

    @Test
    fun `les durees du labo`() {
        assertEquals(570.0, Duree.lire("9:30"))
        assertEquals(570.0, Duree.lire("9 min 30"))
        assertEquals(570.0, Duree.lire("9,5"))
        assertEquals(45.0, Duree.lire("45 s"))
        assertEquals(720.0, Duree.lire("12 min"))
        assertNull(Duree.lire("bientôt"))
        assertEquals("9 min 30", Duree.libelle(570.0))
        assertEquals("45 s", Duree.libelle(45.0))
        assertEquals("12 min", Duree.libelle(720.0))
        assertEquals("9:05", Duree.chrono(545.0))
        assertEquals(8.0, Duree.lireSecondes("8"))
        assertEquals(12.5, Duree.lireSecondes("12,5"))
        assertEquals(90.0, Duree.lireSecondes("1:30"))
        assertEquals(120.0, Duree.lireSecondes("2 min"))
    }
}
