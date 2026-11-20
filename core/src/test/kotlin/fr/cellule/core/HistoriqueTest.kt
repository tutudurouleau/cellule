package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HistoriqueTest {

    @Test
    fun `annuler puis retablir`() {
        var h = Historique("a")
        var courant = "a"
        assertFalse(h.peutAnnuler(courant))
        courant = "b"; h = h.valider(courant)
        courant = "c"; h = h.valider(courant)
        assertTrue(h.peutAnnuler(courant))
        h.annuler(courant).let { (nh, etat) -> h = nh; courant = etat }
        assertEquals("b", courant)
        h.annuler(courant).let { (nh, etat) -> h = nh; courant = etat }
        assertEquals("a", courant)
        assertFalse(h.peutAnnuler(courant))
        h.refaire(courant).let { (nh, etat) -> h = nh; courant = etat }
        assertEquals("b", courant)
        h.refaire(courant).let { (nh, etat) -> h = nh; courant = etat }
        assertEquals("c", courant)
        assertFalse(h.peutRefaire)
    }

    @Test
    fun `un changement pas encore valide s'annule d'abord`() {
        var h = Historique("a").valider("b")
        val (h2, etat) = h.annuler("b+")
        assertEquals("b", etat)
        h = h2
        val (h3, etat2) = h.refaire(etat)
        assertEquals("b+", etat2)
        assertEquals("b+", h3.stable)
    }

    @Test
    fun `un nouveau geste efface ce qu'on pouvait retablir`() {
        var h = Historique(1).valider(2).valider(3)
        val (h2, etat) = h.annuler(3)
        h = h2.valider(etat + 10)
        assertFalse(h.peutRefaire)
        assertEquals(12, h.stable)
    }

    @Test
    fun `cinquante etats au plus`() {
        var h = Historique(0)
        (1..80).forEach { h = h.valider(it) }
        assertEquals(50, h.passe.size)
        assertEquals(30, h.passe.first())
    }

    @Test
    fun `caler sur la grille`() {
        assertEquals(1.5, Sol.caler(1.62, 0.5), 1e-9)
        assertEquals(-0.5, Sol.caler(-0.4, 0.5), 1e-9)
        assertEquals(1.62, Sol.caler(1.62, 0.0), 1e-9)
    }
}
