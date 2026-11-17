package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogueProjecteursTest {

    @Test
    fun chaqueProjecteurEstSourceEtMesure() {
        assertEquals(PROJECTEURS_CATALOGUE.size, PROJECTEURS_CATALOGUE.map { it.identifiant }.toSet().size)
        PROJECTEURS_CATALOGUE.forEach { p ->
            assertTrue(p.source.isNotBlank(), p.nom)
            assertTrue(p.mesures.isNotEmpty(), p.nom)
            p.mesures.forEach { m -> assertTrue(m.lux > 0 && m.distanceM > 0, p.nom) }
            assertTrue(!p.perso, p.nom)
        }
    }

    @Test
    fun lesDistancesPubliesSeRepondent() {
        /* amaran 200d : 65 000 lux à 1 m doit donner ≈ 6 100 à 3 m (publié) à 10 % près. */
        val p = PROJECTEURS_CATALOGUE.first { it.identifiant == "cat-amaran-200d-hyper" }
        val i = Eclairage.intensite(p.mesures.first().lux, p.mesures.first().distanceM)
        val a3m = Eclairage.lux(i, 3.0)
        assertTrue(kotlin.math.abs(a3m - 6100) / 6100 < 0.2, "à 3 m : $a3m")
    }
}
