package fr.cellule.core

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemperatureTest {

    @Test
    fun `la regle Ilford redonne l exemple de sa fiche`() {
        // « si 6 min à 20 °C, alors 4½ min à 23 °C et 9 min à 16 °C »
        assertEquals(4.5, Temperatures.temps(6.0, 23.0, RegleIlford), 0.1)
        assertEquals(9.0, Temperatures.temps(6.0, 16.0, RegleIlford), 0.25)
        RegleIlford.reperes.forEach { (t, f) -> assertEquals(f, RegleIlford.facteur(t), 0.05) }
    }

    @Test
    fun `additionner les 10 pour cent s ecarte de la fiche`() {
        assertEquals(4.2, 6.0 * RegleIlford.lineaire(23.0), 1e-9)
        assertEquals(8.4, 6.0 * RegleIlford.lineaire(16.0), 1e-9)
        assertTrue(9.0 - 6.0 * RegleIlford.lineaire(16.0) > 9.0 - Temperatures.temps(6.0, 16.0, RegleIlford))
    }

    @Test
    fun `une table Kodak se relit a l identique`() {
        val trix = Temperatures.TABLES.first { it.film == "Kodak Tri-X 400" && it.revelateur == "D-76" }
        trix.minutes.forEach { (t, m) -> assertEquals(m, Temperatures.temps(trix.t20, t, trix), 1e-9) }
        // Entre deux points, l'exponentielle qui les relie : moyenne géométrique au milieu.
        assertEquals(sqrt(5.5 * 4.75), Temperatures.temps(trix.t20, 23.0, trix), 1e-9)
    }

    @Test
    fun `dilue, le revelateur craint moins la temperature`() {
        fun taux(dev: String) = Temperatures.TABLES.first { it.film == "Kodak Tri-X 400" && it.revelateur == dev }.tauxParDegre
        assertEquals(0.092, taux("D-76"), 0.005)
        assertEquals(0.058, taux("D-76 1+1"), 0.005)
        assertTrue(taux("XTOL 1+1") < taux("XTOL"))
    }

    @Test
    fun `hors de la table, on prolonge la tendance`() {
        val trix = Temperatures.TABLES.first { it.film == "Kodak Tri-X 400" && it.revelateur == "D-76" }
        assertTrue(26.0 !in trix.plage)
        assertTrue(Temperatures.temps(trix.t20, 26.0, trix) < 4.75)
        assertTrue(Temperatures.temps(trix.t20, 16.0, trix) > 8.0)
    }

    @Test
    fun `la Delta 3200 rappelle que la table prime sur la regle`() {
        val d = Temperatures.DELTA_3200_ID11
        assertEquals(9.0, Temperatures.temps(d.t20, 24.0, d), 1e-9)
        assertEquals(7.17, Temperatures.temps(d.t20, 24.0, RegleIlford), 0.01)
    }

    @Test
    fun `chaque table commence a 20 degres`() {
        Temperatures.TABLES.forEach { assertTrue(it.minutes.any { m -> m.first == 20.0 }, it.intitule) }
    }
}

class PushTest {

    @Test
    fun `HP5 Plus en ID-11 selon Ilford`() {
        val t = Push.TABLES.first { it.film == "Ilford HP5 Plus" && it.revelateur == "ID-11" }
        assertEquals(7.5, t.normal, 1e-9)
        assertEquals(14.0, t.minutes[1600])
        assertEquals(2.0, t.diaphs(1600), 1e-9)
        assertEquals(1.87, t.facteur(1600)!!, 0.01)
        assertNull(t.facteur(3200))
    }

    @Test
    fun `Tri-X sous-expose d un diaph se developpe au temps normal`() {
        Push.TABLES.filter { it.film == "Kodak Tri-X 400" }.forEach {
            assertEquals(it.normal, it.minutes[800], it.intitule)
        }
    }

    @Test
    fun `pas de facteur universel`() {
        val facteurs = Push.TABLES.filter { it.film == "Ilford HP5 Plus" }.mapNotNull { it.facteur(800) }
        assertTrue(facteurs.max() - facteurs.min() > 0.5)
    }

    @Test
    fun `les tables de push et de temperature se recoupent`() {
        Push.TABLES.forEach { p ->
            Temperatures.TABLES.firstOrNull { it.film == p.film && it.revelateur == p.revelateur }?.let {
                assertEquals(it.t20, p.normal, 1e-9, p.intitule)
            }
        }
    }

    @Test
    fun `un pull aussi est publie`() {
        val t = Push.TABLES.first { it.revelateur == "ILFOSOL 3 1+9" }
        assertEquals(-1.0, t.diaphs(200), 1e-9)
        assertTrue(t.facteur(200)!! < 1)
    }
}

class ReciprociteTest {

    private fun film(nom: String) = Reciprocites.LISTE.first { it.film == nom }

    @Test
    fun `la formule Ilford de la HP5 Plus`() {
        val hp5 = film("Ilford HP5 Plus")
        assertEquals(1.0, hp5.corrige(1.0)!!, 1e-9)
        assertEquals(20.4, hp5.corrige(10.0)!!, 0.1)
        assertEquals(417.0, hp5.corrige(100.0)!!, 1.0)
        assertEquals(0.4, hp5.corrige(0.4)!!, 1e-9)
    }

    @Test
    fun `le tableau Kodak de la Tri-X`() {
        val trix = film("Kodak Tri-X")
        assertEquals(0.01, trix.corrige(0.01)!!, 1e-12)
        assertEquals(2.0, trix.corrige(1.0)!!, 1e-9)
        assertEquals(50.0, trix.corrige(10.0)!!, 1e-9)
        assertEquals(1200.0, trix.corrige(100.0)!!, 1e-6)
        assertEquals(228.0, trix.corrige(30.0)!!, 1.0)
        assertEquals("développement −20 %", trix.developpement(12.0))
        assertNull(trix.corrige(200.0))
    }

    @Test
    fun `la correction grandit plus vite que la pose`() {
        Reciprocites.LISTE.forEach { r ->
            val a = assertNotNull(r.diaphs(10.0))
            val b = assertNotNull(r.diaphs(100.0))
            assertTrue(b > a, r.film)
        }
    }
}

class DilutionsPublieesTest {

    @Test
    fun `l Ilfotol a 1+200, c est 5 mL par litre`() {
        val ilfotol = DilutionsPubliees.LISTE.first { it.produit == "ILFOTOL" }
        assertEquals(5.0, ilfotol.dilution.concentre(1000.0), 0.05)
    }

    @Test
    fun `chaque dilution a sa source`() {
        DilutionsPubliees.LISTE.forEach { assertTrue(it.source.adresse.startsWith("https://"), it.intitule) }
    }
}

class EntrainementLaboTest {

    @Test
    fun `chaque exercice tient debout`() {
        val alea = kotlin.random.Random(7)
        ThemeLabo.entries.forEach { theme ->
            repeat(300) {
                val e = EntrainementLabo.exercice(theme, alea)
                assertTrue(e.choix.size >= 3, "${theme}: ${e.enonce} ${e.choix}")
                assertEquals(e.choix.size, e.choix.distinct().size, e.enonce)
                assertTrue(e.bonne in e.choix.indices)
                assertTrue(e.indices.isNotEmpty())
                listOf(e.enonce, e.explication).plus(e.choix).forEach {
                    assertTrue("NaN" !in it && "Infinity" !in it && "null" !in it, it)
                }
            }
        }
    }

    @Test
    fun `la dilution de tete se verifie`() {
        val alea = kotlin.random.Random(3)
        repeat(100) {
            val e = EntrainementLabo.exercice(ThemeLabo.DILUTION, alea)
            assertTrue(e.choix[e.bonne].endsWith(" mL"))
        }
    }
}
