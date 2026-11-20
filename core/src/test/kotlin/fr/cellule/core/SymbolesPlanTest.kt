package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SymbolesPlanTest {

    @Test
    fun `chaque famille a son symbole et sa source`() {
        fun p(nom: String, f: FamilleProjecteur) = Projecteur("x", nom, famille = f, puissanceW = 1200.0)
        assertEquals(SymboleProjecteur.PANNEAU, SymbolesPlan.symbole(p("SkyPanel S60-C", FamilleProjecteur.PANNEAU_LED)))
        assertEquals(SymboleProjecteur.FRESNEL, SymbolesPlan.symbole(p("LS 600d · Fresnel F10", FamilleProjecteur.LED_PONCTUELLE)))
        assertEquals(SymboleProjecteur.PAR, SymbolesPlan.symbole(p("M18", FamilleProjecteur.HMI)))
        assertEquals(SymboleProjecteur.FRESNEL, SymbolesPlan.symbole(p("True Blue D12", FamilleProjecteur.HMI)))
        assertEquals(SymboleProjecteur.BOITE, SymbolesPlan.symbole(p("Lantern 90", FamilleProjecteur.LED_PONCTUELLE)))
        assertEquals(SourceLampe.HMI, SymbolesPlan.source(p("M18", FamilleProjecteur.HMI)))
        assertEquals(SourceLampe.TUNGSTENE, SymbolesPlan.source(p("Blonde 2000", FamilleProjecteur.OUVERT_TUNGSTENE)))
        assertNull(SymbolesPlan.source(p("Ballon", FamilleProjecteur.AMBIANCE)))
        FamilleProjecteur.entries.forEach { SymbolesPlan.symbole(p("X", it)) }
    }

    @Test
    fun `le choix a la main l'emporte et l'etiquette se lit`() {
        val modele = Projecteur("m", "M18", famille = FamilleProjecteur.HMI, puissanceW = 1800.0)
        val e = ElementSol(1, TypeElement.PROJECTEUR, 0.0, 0.0, symbole = "BOITE", gelatine = "1/2 CTB", circuit = "A")
        assertEquals(SymboleProjecteur.BOITE, SymbolesPlan.symbole(e, modele))
        assertEquals(SourceLampe.HMI, SymbolesPlan.source(e, modele))
        assertEquals("1,8 kW · 1/2 CTB · circ. A", SymbolesPlan.etiquette(e, modele))
        assertEquals("450 W", SymbolesPlan.puissance(450.0))
        assertEquals(SymboleProjecteur.AUTRE, SymbolesPlan.symbole(ElementSol(2, TypeElement.PROJECTEUR, 0.0, 0.0), null))
    }
}
