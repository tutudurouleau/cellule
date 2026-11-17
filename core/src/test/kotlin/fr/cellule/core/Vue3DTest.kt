package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Vue3DTest {

    private val cam = CameraVirtuelle(P3(0.0, 0.0, 1.5), lacetDeg = 0.0, tangageDeg = 0.0, champH = 90.0, champV = 60.0)

    @Test
    fun droitDevantAuCentre() {
        val c = cam.versCamera(P3(0.0, -5.0, 1.5))
        assertEquals(5.0, c.z, 1e-9)
        val (x, y) = cam.ecran(c, 1000.0, 500.0)
        assertEquals(500.0, x, 1e-9)
        assertEquals(250.0, y, 1e-9)
    }

    @Test
    fun droiteEtHaut() {
        val (x, y) = cam.ecran(cam.versCamera(P3(5.0, -5.0, 1.5)), 1000.0, 500.0)
        /* 45° à droite avec un champ de 90° : le bord droit de l'image. */
        assertEquals(1000.0, x, 1e-9)
        assertEquals(250.0, y, 1e-9)
        val (_, y2) = cam.ecran(cam.versCamera(P3(0.0, -5.0, 3.0)), 1000.0, 500.0)
        assertTrue(y2 < 250.0)
    }

    @Test
    fun lacetEtTangage() {
        val versLaDroite = CameraVirtuelle(P3(0.0, 0.0, 1.5), 90.0, 0.0, 90.0, 60.0)
        assertEquals(4.0, versLaDroite.versCamera(P3(4.0, 0.0, 1.5)).z, 1e-9)
        val plongee = CameraVirtuelle(P3(0.0, 0.0, 1.5), 0.0, -45.0, 90.0, 60.0)
        val sol = plongee.versCamera(P3(0.0, -1.5, 0.0))
        assertEquals(0.0, sol.y, 1e-9)
        assertEquals(0.0, sol.x, 1e-9)
    }

    @Test
    fun coupeAvant() {
        val poly = listOf(P3(-1.0, 0.0, -1.0), P3(1.0, 0.0, -1.0), P3(1.0, 0.0, 3.0), P3(-1.0, 0.0, 3.0))
        val c = Rendu3D.couper(poly)
        assertTrue(c.all { it.z >= Rendu3D.PROCHE - 1e-9 })
        assertEquals(4, c.size)
        assertNull(Rendu3D.couperSegment(P3(0.0, 0.0, -2.0), P3(1.0, 0.0, -1.0)))
        val s = assertNotNull(Rendu3D.couperSegment(P3(0.0, 0.0, -2.0), P3(0.0, 0.0, 2.0)))
        assertEquals(Rendu3D.PROCHE, s.first.z, 1e-9)
    }

    @Test
    fun cameraDuPlan() {
        val e = ElementSol(1, TypeElement.CAMERA, 0.0, 0.0, focaleMm = 35.0, capteurLargeurMm = 24.89, capteurHauteurMm = 18.66)
        assertNotNull(CameraVirtuelle.depuis(e))
        assertNull(CameraVirtuelle.depuis(e.copy(focaleMm = null)))
    }
}
