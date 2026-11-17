package fr.cellule.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LumiereTest {

    @Test
    fun loiDuCarre() {
        val i = Eclairage.intensite(1000.0, 3.0)
        assertEquals(9000.0, i, 1e-9)
        assertEquals(250.0, Eclairage.lux(i, 6.0), 1e-9)
        assertEquals(125.0, Eclairage.lux(i, 6.0, perteDiaphs = 1.0), 1e-9)
        assertEquals(6.0, Eclairage.distancePour(i, 250.0), 1e-9)
        assertEquals(125.0, Eclairage.lux(i, 6.0, incidenceDeg = 60.0), 1e-6)
    }

    @Test
    fun faisceauEtGeometrie() {
        assertEquals(2.0, Eclairage.diametre(90.0, 1.0), 1e-9)
        assertEquals(1.0, Eclairage.distancePourCouvrir(90.0, 2.0), 1e-9)
        val g = Eclairage.geometrie(3.0, 1.5, 1.5)
        assertEquals(Math.sqrt(4.5), g.distance, 1e-9)
        assertEquals(45.0, g.inclinaison, 1e-9)
    }

    @Test
    fun luxEtTStopSeRepondent() {
        val r = ReglageTournage(cadence = 25.0, angle = 180.0, ei = 800)
        val lux = Eclairage.luxPour(2.8, r)
        val t = Tournage.ouvertureExacte(Eclairage.ev100(lux), r)
        assertEquals(2.8, t, 1e-9)
    }

    @Test
    fun densitesEtConversions() {
        assertEquals(0.30103, Densites.densite(1.0), 1e-5)
        assertEquals(0.25, Densites.transmission(2.0), 1e-12)
        assertEquals(2.0, Densites.diaphsDeTransmission(0.25), 1e-12)
        assertEquals(10.7639, Unites.luxDepuisFc(1.0), 1e-4)
        assertEquals(3.0, Densites.tStop(Densites.fNombre(3.0, 0.8), 0.8), 1e-12)
    }

    @Test
    fun mireds() {
        val d = Mireds.decalage(5600.0, 3200.0)
        assertEquals(1e6 / 3200 - 1e6 / 5600, d, 1e-9)
        assertEquals(3200.0, Mireds.apres(5600.0, d), 1e-6)
        val c = Mireds.combinaisons(3200.0, Mireds.decalage(3200.0, 5600.0), "Lee")
        assertTrue(c.isNotEmpty())
        assertTrue(c.first().mireds < 0)
        assertTrue(abs(c.first().residuel) <= abs(c.last().residuel))
        GELATINES.forEach { assertTrue(it.source.isNotBlank(), it.libelle) }
    }

    @Test
    fun electricite() {
        val prise = CIRCUITS.first { it.nom == "Prise domestique 16 A" }
        assertEquals(10.0, Electricite.intensite(2300.0, prise), 1e-9)
        val tri = CIRCUITS.first { it.nom == "P17 32 A tri" }
        assertEquals(1.0, Electricite.charge(tri.puissance, tri), 1e-9)
        assertEquals(10.0, Electricite.kvaGroupe(8000.0), 1e-9)
    }

    @Test
    fun scintillement() {
        assertEquals(VerdictScintillement.SUR, Scintillement.verdict(24.0, 172.8, 50.0))
        assertEquals(VerdictScintillement.RISQUE, Scintillement.verdict(24.0, 180.0, 50.0))
        assertEquals(VerdictScintillement.SUR, Scintillement.verdict(25.0, 180.0, 50.0))
        assertEquals(VerdictScintillement.SUR_SI_SYNCHRO, Scintillement.verdict(24.0, 180.0, 60.0))
        assertEquals(VerdictScintillement.SUR_SI_SYNCHRO, Scintillement.verdict(25.0, 45.0, 50.0))
        val a = Scintillement.anglesSurs(24.0, 50.0)
        assertEquals(listOf(86.4, 172.8, 259.2, 345.6), a.map { Math.round(it * 10) / 10.0 })
    }

    @Test
    fun optique() {
        assertEquals(2.4, Optique.couverture(4.0, 50.0, 30.0), 1e-9)
        assertEquals(50.0, Optique.focalePour(2.4, 4.0, 30.0), 1e-9)
        val h = Optique.hyperfocale(50.0, 4.0, 0.025)
        assertEquals((2500 / 0.1 + 50) / 1000, h, 1e-9)
        val p = Optique.profondeur(50.0, 4.0, 0.025, h)
        assertNull(p.loinM)
        assertEquals(h / 2, p.procheM, 0.02)
    }
}
