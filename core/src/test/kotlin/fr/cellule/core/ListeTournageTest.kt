package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeTournageTest {

    private val hmi = Projecteur("h", "M18", "ARRI", FamilleProjecteur.HMI, puissanceW = 1800.0)
    private val led = Projecteur("l", "SkyPanel S60", "ARRI", FamilleProjecteur.PANNEAU_LED, puissanceW = 450.0)

    private fun plan(vararg e: ElementSol) = PlanSol(1, "Test", null, elements = e.toList())

    @Test
    fun `les accessoires suivent le projecteur`() {
        val p = plan(
            ElementSol(1, TypeElement.PROJECTEUR, 0.0, 0.0, projecteur = "h", gelatine = "1/2 CTB"),
            ElementSol(2, TypeElement.PROJECTEUR, 2.0, 0.0, projecteur = "l"),
            ElementSol(3, TypeElement.DRAPEAU, 1.0, 1.0, largeur = 0.6)
        )
        val modeles = mapOf("h" to hmi, "l" to led)
        val liste = ListeTournage.complete(p, { modeles[it.projecteur]!!.nom }, { modeles[it.projecteur] }, { modeles[it.projecteur]?.puissanceW })
        fun n(nom: String) = liste.filter { it.nom == nom }.sumOf { it.quantite }
        assertEquals(1.0, n("Ballast HMI"))
        assertEquals(1.0, n("Câble de tête HMI"))
        assertEquals(1.0, n("Pied junior"))
        assertEquals(1.0, n("Pied baby"))
        assertEquals(3.0, n("Sac de lest"))
        assertEquals(1.0, n("C-stand"))
        assertEquals(1.0, n("Gélatine 1/2 CTB (feuille)"))
        assertTrue(liste.filter { it.nom == "M18" }.none { it.auto })
        assertTrue(liste.filter { it.nom == "Ballast HMI" }.all { it.auto })
    }

    @Test
    fun `les prolongs se mesurent depuis le point d'energie`() {
        val p = plan(
            ElementSol(1, TypeElement.ENERGIE, 0.0, 0.0),
            ElementSol(2, TypeElement.PROJECTEUR, 10.0, 0.0, hauteur = 2.0, puissanceW = 2000.0),
            ElementSol(3, TypeElement.PROJECTEUR, 0.0, 30.0, hauteur = 3.0, puissanceW = 5000.0)
        )
        val c = ListeTournage.cablage(p, { "P" + it.identifiant }, { it.puissanceW })
        assertTrue(c.mesure)
        assertEquals(listOf(16, 32), c.tirages.map { it.calibreA })
        /* 10 m × 1,3 + 2 + 3 = 18 m → un 25 m ; 30 × 1,3 + 3 + 3 = 45 m → un 50 m. */
        assertEquals(1.0, c.prolongs.first { it.nom == "Prolong 16 A mono de 25 m" }.quantite)
        assertEquals(1.0, c.prolongs.first { it.nom == "Prolong 32 A mono de 50 m" }.quantite)
        assertFalse(ListeTournage.cablage(plan(ElementSol(2, TypeElement.PROJECTEUR, 1.0, 1.0)), { "" }, { null }).mesure)
    }

    @Test
    fun `decouper prend le moins de prolongs`() {
        assertEquals(listOf(10.0), ListeTournage.decouper(7.0))
        assertEquals(listOf(25.0), ListeTournage.decouper(23.0))
        assertEquals(listOf(50.0, 10.0), ListeTournage.decouper(57.0))
        assertEquals(63, ListeTournage.calibre(9000.0))
    }

    @Test
    fun `les phases s'equilibrent`() {
        val els = listOf(4000.0, 2000.0, 2000.0, 1000.0, 1000.0).mapIndexed { i, w ->
            ElementSol(i.toLong(), TypeElement.PROJECTEUR, 0.0, 0.0, puissanceW = w)
        }
        val ph = ListeTournage.phases(els, { it.identifiant.toString() }, { it.puissanceW })
        assertEquals(10000.0, ph.charges.sum())
        assertTrue(ph.ecartW <= 1000.0)
    }

    @Test
    fun `un plan passe par des morceaux et revient`() {
        val json = "{\"nom\":\"Cuisine\",\"elements\":[" + (1..80).joinToString(",") { "{\"id\":$it,\"type\":\"PROJECTEUR\",\"x\":$it.5}" } + "]}"
        val m = PlanCodec.morceaux(json, 200)
        assertTrue(m.size > 1)
        assertEquals(json, PlanCodec.assembler(m.shuffled()))
        assertNull(PlanCodec.assembler(m.drop(1)))
        assertEquals(json, PlanCodec.depuisTexte("bla " + m.joinToString("\n") + " fin"))
    }
}
