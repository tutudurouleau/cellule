package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChronoLaboTest {

    @Test
    fun `l agitation Ilford, dix secondes chaque minute`() {
        val c = Agitations.ILFORD.creneaux(6.5 * 60)
        assertEquals(7, c.size)
        assertEquals(0.0..10.0, c.first())
        assertEquals(360.0..370.0, c.last())
    }

    @Test
    fun `l agitation Kodak, cinq secondes toutes les trente`() {
        val c = Agitations.KODAK.creneaux(6.75 * 60)
        assertEquals(14, c.size)
        assertEquals(30.0..35.0, c[1])
        assertEquals(390.0..395.0, c.last())
    }

    @Test
    fun `une reprise tronquee par la fin du bain`() {
        val c = Agitation(0.0, 10.0, 30.0).creneaux(35.0)
        assertEquals(listOf(30.0..35.0), c)
    }

    @Test
    fun `la chronologie d un film`() {
        val s = Sequences.FILM_ILFORD
        val e = Chrono.evenements(s)
        assertEquals(Evenement(0.0, Signal.ETAPE, 0), e.first())
        assertEquals(Evenement(s.duree, Signal.FIN, 4), e.last())
        assertTrue(e.none { it.signal == Signal.AGITER && it.instant == 0.0 })
        assertTrue(Evenement(60.0, Signal.AGITER, 0) in e)
        assertTrue(Evenement(10.0, Signal.REPOS, 0) in e)
        assertEquals(5, e.count { it.signal == Signal.ETAPE })
        assertEquals(e.sortedBy { it.instant }, e)
    }

    @Test
    fun `ou en est-on`() {
        val s = Sequences.FILM_ILFORD
        val p = Chrono.position(s, 65.0)
        assertEquals(0, p.etape)
        assertTrue(p.agite)
        assertEquals(55.0, p.prochaineAgitation!!, 1e-9)
        assertEquals(6.5 * 60 - 65, p.resteEtape, 1e-9)

        val arret = Chrono.position(s, s.debut(1) + 5)
        assertEquals(1, arret.etape)
        assertTrue(arret.agite)

        val lavage = Chrono.position(s, s.debut(3) + 60)
        assertFalse(lavage.agite)
        assertEquals(null, lavage.prochaineAgitation)

        assertTrue(Chrono.position(s, s.duree + 1).fini)
    }

    @Test
    fun `les evenements entre deux lectures`() {
        val e = Chrono.evenements(Sequences.FILM_KODAK)
        assertEquals(listOf(Signal.AGITER), Chrono.entre(e, 29.0, 30.0).map { it.signal })
        assertTrue(Chrono.entre(e, 30.0, 34.0).isEmpty())
    }

    @Test
    fun `le papier attend ses durees`() {
        assertFalse(Sequences.PAPIER.complete)
        assertTrue(Sequences.FILM_ILFORD.complete && Sequences.FILM_KODAK.complete)
    }

    @Test
    fun `les libelles d agitation`() {
        assertEquals("10 s au départ, puis 10 s toutes les 60 s", Agitations.ILFORD.libelle)
        assertEquals("en continu", Agitations.CONTINUE.libelle)
    }
}
