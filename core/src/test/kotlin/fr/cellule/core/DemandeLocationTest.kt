package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemandeLocationTest {

    @Test
    fun `les infos de production se relisent`() {
        val i = InfosProduction("Court « Nuit »", "Asso", "Gaspard", "06", "", "École\n2e étage", "12/03", "14/03", 2, "")
        assertEquals(i, DemandeLocation.lireInfos(DemandeLocation.ecrireInfos(i)))
        assertTrue(InfosProduction().manques.size == 4)
        assertTrue(i.manques.isEmpty())
    }

    @Test
    fun `la liste se range par service`() {
        val lignes = listOf(
            LigneLocation("Projecteurs", "M18", 1.0),
            LigneLocation("Électrique", "Ballast HMI", 1.0, auto = true),
            LigneLocation("Machinerie", "Pied junior", 1.0, auto = true),
            LigneLocation("Consommables", "Gaffer (rouleau)", 1.0, auto = true),
            LigneLocation("Caméra", "Caméras", 1.0)
        )
        assertEquals(listOf("Caméra et optiques", "Lumière", "Machinerie", "Électrique et énergie", "Consommables"), DemandeLocation.parService(lignes).map { it.first })
    }

    @Test
    fun `l'estimation ne compte que les prix connus`() {
        val t = MesTarifs.lire("M18\t120\nPied junior\t8,5\nmauvais")
        assertEquals(2, t.size)
        val e = MesTarifs.estimer(listOf(LigneLocation("Projecteurs", "M18", 2.0), LigneLocation("Machinerie", "Pied junior", 2.0), LigneLocation("Machinerie", "Rail", 6.0, "m")), t)
        assertEquals(257.0, e.parJour)
        assertEquals(2, e.lignesChiffrees)
        assertEquals(t, MesTarifs.lire(MesTarifs.ecrire(t)))
    }

    @Test
    fun `le pointage suit l'enlevement et le retour`() {
        val p = Pointage.pour(listOf(LigneLocation("Projecteurs", "M18", 2.0)), emptyList())
        assertEquals("Rien de pointé pour l'instant.", Pointage.bilan(p))
        val enleve = p.map { it.copy(enleve = 2) }
        assertTrue(Pointage.bilan(enleve).startsWith("Tout est enlevé"))
        val rendu = enleve.map { it.copy(rendu = 1, note = "un volet tordu") }
        assertEquals("1 pièce(s) pas encore rendue(s).", Pointage.bilan(rendu))
        assertEquals(rendu, Pointage.lire(Pointage.ecrire(rendu)))
    }
}
