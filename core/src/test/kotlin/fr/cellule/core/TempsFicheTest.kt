package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TempsFicheTest {

    private fun fiche(nom: String) = FichesLabo.TOUTES.first { it.nom == nom }

    @Test
    fun `un film propose ses revelateurs dans l'ordre de la fiche`() {
        val choix = TempsFiche.choix(fiche("Ilford HP5 Plus"))
        assertEquals("ILFOTEC DD-X", choix.first())
        assertTrue("ID-11" in choix && "D-76" in choix)
        assertEquals(choix.distinct(), choix)
    }

    @Test
    fun `a une seule temperature, une colonne par dilution`() {
        val grilles = TempsFiche.grilles(fiche("Ilford HP5 Plus"), "ID-11")
        assertEquals(1, grilles.size)
        val g = grilles.single()
        assertEquals(listOf("stock", "1+1", "1+3"), g.colonnes)
        assertEquals(20.0, g.temperature)
        assertEquals(400, g.eiNominal)
        assertEquals(
            listOf(
                LigneGrille(400, listOf(7.5, 13.0, 20.0)),
                LigneGrille(800, listOf(10.5, 16.5, null)),
                LigneGrille(1600, listOf(14.0, null, null))
            ),
            g.lignes
        )
    }

    @Test
    fun `a plusieurs temperatures, un tableau par dilution`() {
        val grilles = TempsFiche.grilles(fiche("D-76"), "Kodak Tri-X 400")
        assertEquals(listOf("stock", "1+1"), grilles.map { it.titre })
        val stock = grilles.first()
        assertEquals(listOf("18 °C", "20 °C", "21 °C", "22 °C", "24 °C"), stock.colonnes)
        assertEquals(listOf(8.0, 6.75, 6.25, 5.5, 4.75), stock.lignes.first { it.ei == 400 }.cellules)
        assertNull(stock.temperature)
    }

    @Test
    fun `une dilution non ecrite ne donne pas de titre`() {
        val g = TempsFiche.grilles(fiche("Kodak Tri-X 400"), "T-MAX Developer")
        assertEquals(1, g.size)
        assertNull(g.single().titre)
        assertEquals(5, g.single().colonnes.size)
    }

    @Test
    fun `aucun temps publie ne se perd ni ne se double`() {
        FichesLabo.TOUTES.forEach { f ->
            val attendu = when (f.genre) {
                GenreFiche.FILM -> TempsPublies.pourFilm(f.cleTemps).size
                GenreFiche.REVELATEUR -> TempsPublies.pourRevelateur(f.cleTemps).size
                GenreFiche.BAIN -> 0
            }
            val cases = TempsFiche.choix(f).sumOf { c ->
                TempsFiche.grilles(f, c).sumOf { g -> g.lignes.sumOf { l -> l.cellules.count { it != null } } }
            }
            assertEquals(attendu, cases, f.nom)
        }
    }

    @Test
    fun `la fiche fermee annonce ce qu'elle contient`() {
        val n = TempsFiche.choix(fiche("Ilford HP5 Plus")).size
        assertEquals("Temps avec $n révélateurs", TempsFiche.contenu(fiche("Ilford HP5 Plus")))
        assertEquals("Temps pour 4 films", TempsFiche.contenu(fiche("D-76")))
        assertNull(TempsFiche.contenu(fiche("ILFOSTOP")))
        assertNull(TempsFiche.contenu(fiche("Ilford Pan F Plus")))
    }
}
