package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FichesLaboTest {

    @Test
    fun `les temps publies recoupent les tables des calculateurs`() {
        // Deux transcriptions des mêmes tableaux : la moindre coquille se verrait ici.
        Push.TABLES.forEach { t ->
            t.minutes.forEach { (ei, m) ->
                val lignes = TempsPublies.LISTE.filter {
                    it.film == t.film && it.ei == ei && it.temperature == 20.0 &&
                        "${it.revelateur} ${it.dilution}".contains(t.revelateur.removePrefix("Kodak ").substringBefore(" 1+").substringBefore(" dilution"))
                }
                if (lignes.isNotEmpty() && !(t.film == "Kodak Tri-X 400" && ei == 800)) {
                    assertTrue(lignes.any { it.minutes == m }, "${t.intitule} EI $ei : $m absent de $lignes")
                }
            }
        }
        Temperatures.TABLES.forEach { t ->
            t.minutes.forEach { (temp, m) ->
                assertTrue(
                    TempsPublies.LISTE.any { it.film == t.film && it.temperature == temp && it.minutes == m },
                    "${t.intitule} à $temp °C"
                )
            }
        }
    }

    @Test
    fun `la Delta 3200 en ID-11 est la meme partout`() {
        val d = Temperatures.DELTA_3200_ID11
        d.minutes.forEach { (temp, m) ->
            assertTrue(TempsPublies.LISTE.any { it.film == "Ilford Delta 3200" && it.revelateur == "ID-11" && it.ei == 3200 && it.temperature == temp && it.minutes == m })
        }
    }

    @Test
    fun `chaque film et chaque revelateur a ses temps, sauf lacune avouee`() {
        FichesLabo.TOUTES.filter { it.genre != GenreFiche.BAIN }.forEach { f ->
            assertTrue(FichesLabo.temps(f).isNotEmpty() || f.lacune.isNotBlank(), f.nom)
        }
    }

    @Test
    fun `aucun doublon dans les temps publies`() {
        val cles = TempsPublies.LISTE.map { listOf(it.film, it.revelateur, it.dilution, it.ei, it.temperature) }
        assertEquals(cles.size, cles.distinct().size)
    }

    @Test
    fun `les temps chauds sont plus courts que les froids`() {
        TempsPublies.LISTE.groupBy { listOf(it.film, it.revelateur, it.dilution, it.ei) }.values.forEach { lignes ->
            val tries = lignes.sortedBy { it.temperature }
            assertTrue(tries.zipWithNext().all { (a, b) -> b.minutes <= a.minutes }, tries.toString())
        }
    }

    @Test
    fun `pousser allonge toujours`() {
        TempsPublies.LISTE.groupBy { listOf(it.film, it.revelateur, it.dilution, it.temperature) }.values.forEach { lignes ->
            val tries = lignes.sortedBy { it.ei }
            assertTrue(tries.zipWithNext().all { (a, b) -> b.minutes >= a.minutes }, tries.toString())
        }
    }
}
