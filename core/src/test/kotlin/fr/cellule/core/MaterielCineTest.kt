package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MaterielCineTest {

    @Test
    fun chaqueCameraAModesExisteDansLeCatalogue() {
        val noms = CATALOGUE_CAMERAS.map { it.nom }.toSet()
        MODES_CAPTEUR.forEach { assertTrue(it.camera in noms, "${it.camera} absente du catalogue") }
    }

    @Test
    fun chaqueTailleEstSourceeEtPlausible() {
        (FORMATS_PELLICULE + MODES_CAPTEUR.flatMap { it.modes }).forEach {
            assertTrue(it.source.isNotBlank(), it.nom)
            assertTrue(it.largeurMm in 4.0..60.0 && it.hauteurMm in 3.0..30.0, it.nom)
            assertTrue(it.largeurMm >= it.hauteurMm, it.nom)
        }
        PELLICULES_CINE.forEach { assertTrue(it.source.isNotBlank() && it.eiJour > 0 && it.eiTungstene > 0, it.nom) }
    }

    @Test
    fun angleEtFocaleSontReciproques() {
        val f = MaterielCine.focalePourAngle(24.89, 40.0)
        assertEquals(40.0, Focales.angleDeChamp(24.89, f), 1e-9)
    }

    @Test
    fun lectureDesFocales() {
        val plage = MaterielCine.lireFocales("16 à 135 mm")
        assertEquals(16.0, plage.min!!, 0.0)
        assertEquals(135.0, plage.max!!, 0.0)
        assertFalse(plage.exactes)
        assertTrue(plage.choix.all { it in 16.0..135.0 })

        val liste = MaterielCine.lireFocales("18, 24, 35, 55 et 85 mm")
        assertTrue(liste.exactes)
        assertEquals(listOf(18.0, 24.0, 35.0, 55.0, 85.0), liste.choix)

        val zooms = MaterielCine.lireFocales("30-90 et 15-40 mm (S35), 45-135 et 22-60 mm (plein format)")
        assertEquals(15.0, zooms.min!!, 0.0)
        assertEquals(135.0, zooms.max!!, 0.0)

        assertEquals(14.5, MaterielCine.lireFocales("14,5-45, 18-85 mm").min!!, 0.0)
        assertFalse(MaterielCine.lireFocales("Zoom 10×").exactes)
        assertEquals(listOf(50.0), MaterielCine.lireFocales("50 mm").choix)
    }
}
