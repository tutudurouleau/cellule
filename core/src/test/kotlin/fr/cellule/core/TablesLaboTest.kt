package fr.cellule.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TablesLaboTest {

    @Test
    fun `la regle d'Ilford retombe sur l'exemple de sa fiche`() {
        val parDegre = TablesLabo.REGLE_ILFORD.associate { it.temperature to it.minutes }
        assertEquals(9.0, parDegre[16.0])
        assertEquals(6.0, parDegre[20.0])
        assertEquals(4.5, parDegre[23.0])
        /* Plus chaud, toujours plus court. */
        val temps = TablesLabo.REGLE_ILFORD.map { it.facteur }
        assertEquals(temps.sortedDescending(), temps)
    }

    @Test
    fun `la table Kodak et la regle d'Ilford s'accordent au quart de minute`() {
        val lignes = TablesLabo.KODAK_FACE_A_LA_REGLE
        assertEquals(listOf(18.0, 20.0, 21.0, 22.0, 24.0), lignes.map { it.temperature })
        assertEquals(6.75, lignes.first { it.temperature == 20.0 }.publie)
        lignes.forEach { assertTrue(abs(it.publie - it.regle) <= 0.25, "${it.temperature} °C : ${it.publie} contre ${it.regle}") }
    }

    @Test
    fun `chaque caractere de revelateur est ecrit dans sa fiche`() {
        (TablesLabo.REVELATEURS_ILFORD + TablesLabo.REVELATEURS_AUTRES).filter { it.caractere.isNotBlank() }.forEach { l ->
            val f = FichesLabo.parNom(l.revelateur)!!
            val texte = (listOf(f.resume) + f.faits.flatMap { listOf(it.intitule, it.valeur, it.detail) }).joinToString(" ")
            l.caractere.split(", ").forEach { mot ->
                assertTrue(mot in texte, "« $mot » n'est pas dans la fiche ${f.nom}")
            }
        }
    }

    @Test
    fun `les revelateurs viennent tous du tableau publie`() {
        val ilford = TablesLabo.REVELATEURS_ILFORD
        val autres = TablesLabo.REVELATEURS_AUTRES
        assertTrue(ilford.isNotEmpty() && autres.isNotEmpty())
        (ilford + autres).forEach { l ->
            assertTrue(
                TempsPublies.pourFilm(TablesLabo.FILM_REFERENCE).any {
                    it.revelateur == l.revelateur && it.dilution == l.dilution && it.ei == 400 && it.temperature == 20.0 && it.minutes == l.minutes
                },
                l.toString()
            )
        }
        assertTrue(ilford.all { FichesLabo.parNom(it.revelateur)!!.fabricant.startsWith("Ilford") })
        assertTrue(autres.none { FichesLabo.parNom(it.revelateur)!!.fabricant.startsWith("Ilford") })
    }

    @Test
    fun `la table de push reprend les temps d'Ilford`() {
        val push = TablesLabo.PUSH.associateBy { it.ei }
        assertEquals(listOf(7.5, 9.0, 6.5), push.getValue(400).minutes)
        assertEquals(listOf(null, 20.0, 16.0), push.getValue(3200).minutes)
        assertEquals(listOf(0, 1, 2, 3), TablesLabo.PUSH.map { it.diaphs })
    }

    @Test
    fun `les dilutions se lisent en parts`() {
        val d = TablesLabo.DILUTIONS
        assertEquals(d.sortedBy { it.dilution.partsEau }, d)
        val neuf = d.first { it.dilution.libelle == "1+9" }
        assertTrue("ILFOSOL 3" in neuf.produits)
        assertEquals("10 %", TablesLabo.pourcentConcentre(neuf.dilution))
        assertEquals("50 + 450 mL", TablesLabo.pour(neuf.dilution))
    }

    @Test
    fun `les minutes s'ecrivent comme dans les fiches`() {
        assertEquals("7½", TablesLabo.minutes(7.5))
        assertEquals("6¾", TablesLabo.minutes(6.75))
        assertEquals("6¼", TablesLabo.minutes(6.25))
        assertEquals("13", TablesLabo.minutes(13.0))
        assertEquals("×1,46", TablesLabo.facteur(1.4641))
        assertEquals("×1", TablesLabo.facteur(1.0))
    }
}
