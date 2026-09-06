package fr.cellule.core

import kotlin.math.log2
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotometrieTest {

    @Test
    fun `les constantes du systeme`() {
        assertEquals(81920.0, Photometrie.lux(15.0), 1.0)
        assertEquals(4096.0, Photometrie.cdm2(15.0), 1.0)
        assertEquals(15.0, Photometrie.evDepuisLux(82000.0), 0.02)
        assertEquals(4698.0, Photometrie.luminance(82000.0, Photometrie.GRIS), 2.0)
    }

    @Test
    fun `l ecart entre C et K est structurel, pas un arrondi`() {
        val parIncident = Photometrie.evDepuisLux(82000.0)
        val parReflechi = Photometrie.evDepuisCdm2(Photometrie.luminance(82000.0, Photometrie.GRIS))
        assertEquals(-0.2, parIncident - parReflechi, 0.03)
    }

    @Test
    fun `f sur 16 au 125e donne bien EV 15`() {
        assertEquals(14.97, Photometrie.evDepuisReglages(16.0, 1.0 / 125), 0.02)
    }

    @Test
    fun `la compensation ISO`() {
        assertEquals(2.0, Photometrie.decalageIso(400), 1e-9)
        assertEquals(-1.0, Photometrie.decalageIso(50), 1e-9)
        assertEquals(3.0, Photometrie.decalageIso(800), 1e-9)
    }

    @Test
    fun `la table maitresse reproduit celle du document`() {
        val attendu = listOf(
            16 to "f/22 · 1/125", 15 to "f/16 · 1/125", 14 to "f/11 · 1/125",
            13 to "f/8 · 1/125", 12 to "f/5,6 · 1/125", 11 to "f/4 · 1/125",
            10 to "f/2,8 · 1/125", 9 to "f/2 · 1/125", 8 to "f/2 · 1/60",
            7 to "f/2 · 1/30", 6 to "f/2 · 1/15", 5 to "f/2 · 1/8",
            3 to "f/2 · 1/2", 0 to "f/2 · 4 s", -3 to "f/2 · 30 s", -6 to "f/2 · 4 min"
        )
        attendu.forEach { (ev, libelle) ->
            val c = coupleDeTable(ev.toDouble())!!
            assertEquals(libelle, "${libelleOuverture(c.ouverture)} · ${c.vitesse.libelle}", "à EV $ev")
        }
    }

    @Test
    fun `le cas cine demande f sur 64, pas f sur 45`() {
        val evAppareil = 15.0 + Photometrie.decalageIso(800)
        assertEquals(18.0, evAppareil, 0.01)
        val exacte = Photometrie.ouverturePour(evAppareil, 1.0 / 50)
        assertEquals(72.4, exacte, 0.5)
        assertEquals(64.0, ouvertureLaPlusProche(exacte), 1e-9)
        assertEquals(18.1, Photometrie.ouverturePour(evAppareil - 4, 1.0 / 50), 0.3)
        assertEquals(9.05, Photometrie.ouverturePour(evAppareil - 6, 1.0 / 50), 0.2)
    }

    @Test
    fun `les couples sont coherents avec leur EV`() {
        couples(15.0).forEach { c ->
            assertEquals(15.0, Photometrie.evDepuisReglages(c.ouvertureExacte, c.vitesse.secondes), 1e-9)
            /* L'écart annoncé doit correspondre à l'arrondi réellement fait. */
            val evArrondi = Photometrie.evDepuisReglages(c.ouverture, c.vitesse.secondes)
            assertEquals(15.0 - evArrondi, c.ecartDiaphs, 1e-9, "sur ${c.vitesse.libelle}")
        }
    }

    @Test
    fun `l arrondi au diaph plein ne depasse jamais un sixieme de diaph`() {
        listOf(6.0, 10.0, 15.0, 18.0).forEach { ev ->
            couples(ev).forEach {
                assertTrue(kotlin.math.abs(it.ecartDiaphs) <= 0.35, "${it.vitesse.libelle} à EV $ev")
            }
        }
    }

    @Test
    fun `le formatage des ouvertures`() {
        assertEquals("f/22", libelleOuverture(22.0))
        assertEquals("f/5,6", libelleOuverture(5.6))
        assertEquals("f/1,4", libelleOuverture(1.4))
        assertEquals("f/10,3", libelleOuverture(10.34))
    }

    @Test
    fun `les fractions de diaph`() {
        assertEquals("juste", libelleDiaphs(0.0))
        assertEquals("−⅓", libelleDiaphs(-1.0 / 3))
        assertEquals("+⅔", libelleDiaphs(2.0 / 3))
        assertEquals("−1 ⅓", libelleDiaphs(-4.0 / 3))
        assertEquals("+2", libelleDiaphs(2.0))
        assertEquals("+2 ⅓", libelleDiaphs(2.24))
    }

    @Test
    fun `les niveaux de lumiere sont nommes au bon EV`() {
        assertEquals("Plein soleil franc", Situations.nom(15.0))
        assertEquals("Plein jour vif", Situations.nom(14.0))
        assertEquals("Intérieur très éclairé, vitrine", Situations.nom(8.0))
        assertEquals("Crépuscule avancé", Situations.nom(0.0))
    }
}
