package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarteTest {

    @Test
    fun `le centre du monde est au centre de la grille`() {
        assertEquals(0.5, Tuiles.x(0.0, 0), 1e-12)
        assertEquals(0.5, Tuiles.y(0.0, 0), 1e-12)
        assertEquals(0.0, Tuiles.x(-180.0, 5), 1e-12)
        assertEquals(32.0, Tuiles.x(180.0, 5), 1e-12)
        assertEquals(0.0, Tuiles.y(Tuiles.LATITUDE_MAX, 3), 1e-3)
    }

    @Test
    fun `l aller-retour rend le point de depart`() {
        listOf(48.8584 to 2.2945, 43.6045 to 1.4440, -33.8568 to 151.2153, 64.1466 to -21.9426).forEach { (lat, lon) ->
            for (z in listOf(3, 10, 16)) {
                assertEquals(lon, Tuiles.longitude(Tuiles.x(lon, z), z), 1e-9)
                assertEquals(lat, Tuiles.latitude(Tuiles.y(lat, z), z), 1e-9)
            }
        }
    }

    @Test
    fun `la tour Eiffel tombe dans la tuile connue`() {
        /* Exemple classique de la documentation d'OpenStreetMap : zoom 16, autour de 48,858 N 2,294 E. */
        val z = 16
        assertEquals(33_185, Tuiles.colonne(Tuiles.x(2.2945, z), z))
        assertEquals(22_545, Tuiles.ligne(Tuiles.y(48.8584, z), z))
        assertEquals("https://tile.openstreetmap.org/16/33185/22545.png", Tuiles.adresse(16, 33_185, 22_545))
    }

    @Test
    fun `la carte reboucle d est en ouest mais pas par les poles`() {
        assertEquals(7, Tuiles.colonne(-1.0, 3))
        assertEquals(0, Tuiles.colonne(8.0, 3))
        assertNull(Tuiles.ligne(-0.5, 3))
        assertNull(Tuiles.ligne(8.0, 3))
        assertEquals(7, Tuiles.ligne(7.9, 3))
    }

    @Test
    fun `l echelle se resserre avec la latitude et le zoom`() {
        assertEquals(156_543.0, Tuiles.metresParPixel(0.0, 0), 1.0)
        assertEquals(Tuiles.metresParPixel(0.0, 10) / 2, Tuiles.metresParPixel(60.0, 10), 1e-6)
        assertEquals(Tuiles.metresParPixel(45.0, 10) / 2, Tuiles.metresParPixel(45.0, 11), 1e-9)
    }

    @Test
    fun `les coordonnees se saisissent comme on les ecrit`() {
        assertEquals(43.6045 to 1.444, Lieux.lireCoordonnees("43.6045, 1.4440"))
        assertEquals(43.6045 to 1.444, Lieux.lireCoordonnees("43,6045 1,4440"))
        assertEquals(43.6045 to 1.444, Lieux.lireCoordonnees(" 43.6045;1.444 "))
        assertEquals(43.6045 to 1.444, Lieux.lireCoordonnees("43.6045,1.444"))
        assertEquals(-33.0 to -70.5, Lieux.lireCoordonnees("-33 -70,5"))
        assertNull(Lieux.lireCoordonnees(""))
        assertNull(Lieux.lireCoordonnees("Toulouse"))
        assertNull(Lieux.lireCoordonnees("43.6"))
        assertNull(Lieux.lireCoordonnees("95 2"))
        assertNull(Lieux.lireCoordonnees("45 200"))
    }

    @Test
    fun `les lieux recents ne se repetent pas`() {
        val toulouse = LieuRecent("Toulouse", 43.6045, 1.4440)
        val voisin = LieuRecent("Capitole", 43.6046, 1.4441)
        val auch = LieuRecent("Auch", 43.65, 0.59)
        val liste = Lieux.ajouter(Lieux.ajouter(emptyList(), toulouse), auch)
        assertEquals(listOf(auch, toulouse), liste)
        /* À moins de 200 m, le nouveau remplace l'ancien et passe en tête. */
        assertEquals(listOf(voisin, auch), Lieux.ajouter(liste, voisin))
        val beaucoup = (1..20).fold(emptyList<LieuRecent>()) { l, i -> Lieux.ajouter(l, LieuRecent("L$i", i.toDouble(), 0.0)) }
        assertEquals(Lieux.MAXIMUM, beaucoup.size)
        assertEquals("L20", beaucoup.first().nom)
        assertTrue(toulouse.coordonnees.startsWith("43,60450° N"))
    }

    @Test
    fun `le fuseau geographique suit la longitude`() {
        assertEquals(0.0, Lieux.fuseauGeographique(1.44))
        assertEquals(1.0, Lieux.fuseauGeographique(9.15))
        assertEquals(-5.0, Lieux.fuseauGeographique(-74.0))
        assertEquals(9.0, Lieux.fuseauGeographique(139.7))
    }
}
