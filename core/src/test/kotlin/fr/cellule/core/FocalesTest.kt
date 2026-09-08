package fr.cellule.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FocalesTest {

    private val plein24x36 = Cadre(36.0, 24.0)

    @Test
    fun `les angles de champ du 24x36`() {
        /* Valeurs de catalogue, angle horizontal. */
        listOf(
            14 to 104.3, 20 to 83.9, 24 to 73.7, 28 to 65.5, 35 to 54.4,
            50 to 39.6, 85 to 23.9, 135 to 15.2, 200 to 10.3
        ).forEach { (mm, attendu) ->
            assertEquals(attendu, Focales.angleDeChamp(36.0, mm.toDouble()), 0.15, "$mm mm")
        }
    }

    @Test
    fun `le 50 mm et sa diagonale de 47 degres`() {
        assertEquals(46.8, Focales.angleDeChamp(Focales.DIAGONALE_REFERENCE, 50.0), 0.1)
        assertEquals(43.267, Focales.DIAGONALE_REFERENCE, 0.001)
    }

    @Test
    fun `angle et focale sont reciproques`() {
        listOf(14.0, 24.0, 35.0, 50.0, 85.0, 200.0).forEach { f ->
            val angle = Focales.angleDeChamp(36.0, f)
            assertEquals(f, Focales.focalePourAngle(angle, 36.0), 1e-9, "$f mm")
        }
    }

    @Test
    fun `sur son propre format une focale est sa propre equivalente`() {
        val e = Focales.equivalence(plein24x36, 50.0)
        assertEquals(50.0, e.equivalentHorizontal, 1e-9)
        assertEquals(50.0, e.equivalentDiagonal, 1e-9)
        assertEquals(1.0, e.facteurDeConversion, 1e-9)
    }

    @Test
    fun `un capteur de telephone donne un grand-angle`() {
        /* Capteur d'environ 1/1,7 pouce, 7,4 × 5,6 mm, objectif de 5,6 mm :
           l'ordre de grandeur d'un module principal de téléphone. */
        val cadre = Focales.cadreUtile(7.4, 5.6, 1.0, 1.0, rapportImage = 4.0 / 3.0)
        assertEquals(7.4, cadre.largeurMm, 0.01)
        val e = Focales.equivalence(cadre, 5.6)
        assertEquals(27.2, e.equivalentHorizontal, 0.5)
        assertEquals(4.86, e.facteurDeConversion, 0.05)
    }

    @Test
    fun `doubler le zoom double la focale equivalente`() {
        val plein = Focales.cadreUtile(7.4, 5.6, 1.0, 1.0, 4.0 / 3.0)
        val zoomDeux = Focales.cadreUtile(7.4, 5.6, 0.5, 0.5, 4.0 / 3.0)
        val a = Focales.equivalence(plein, 5.6).equivalentHorizontal
        val b = Focales.equivalence(zoomDeux, 5.6).equivalentHorizontal
        assertEquals(2.0, b / a, 0.001)
    }

    @Test
    fun `le format de sortie retaille le capteur`() {
        /* Un capteur 4:3 filmé en 16:9 perd de la hauteur, pas de la largeur. */
        val en43 = Focales.cadreUtile(7.4, 5.55, 1.0, 1.0, 4.0 / 3.0)
        val en169 = Focales.cadreUtile(7.4, 5.55, 1.0, 1.0, 16.0 / 9.0)
        assertEquals(en43.largeurMm, en169.largeurMm, 0.01)
        assertTrue(en169.hauteurMm < en43.hauteurMm)
        /* Donc même angle horizontal, angle vertical plus étroit. */
        val a = Focales.equivalence(en43, 5.6)
        val b = Focales.equivalence(en169, 5.6)
        assertEquals(a.angleHorizontal, b.angleHorizontal, 1e-9)
        assertTrue(b.angleVertical < a.angleVertical)
    }

    @Test
    fun `un capteur plus etroit que le format demande perd de la largeur`() {
        /* Cadre carré, sortie 16:9 : c'est la largeur qui commande. */
        val c = Focales.cadreUtile(6.0, 6.0, 1.0, 1.0, 16.0 / 9.0)
        assertEquals(6.0, c.largeurMm, 1e-9)
        assertEquals(6.0 * 9 / 16, c.hauteurMm, 1e-9)
    }
}

class CadrageTest {

    @Test
    fun `une focale plus longue occupe une fraction du cadre`() {
        /* À 28 mm, un 85 mm ne tient que sur un tiers de la largeur. */
        assertEquals(28.0 / 85.0, Cadrage.fractionDuCadre(28.0, 85.0)!!, 1e-9)
        assertEquals(0.5, Cadrage.fractionDuCadre(25.0, 50.0)!!, 1e-9)
    }

    @Test
    fun `une focale plus courte que la notre ne se montre pas`() {
        assertNull(Cadrage.fractionDuCadre(50.0, 35.0))
        /* La focale courante occupe tout le cadre, et c'est la limite. */
        assertEquals(1.0, Cadrage.fractionDuCadre(50.0, 50.0)!!, 1e-9)
    }

    @Test
    fun `la classique la plus proche se juge en proportion, pas en millimetres`() {
        assertEquals(50, Cadrage.laPlusProche(48.0).mm)
        assertEquals(35, Cadrage.laPlusProche(37.0).mm)
        assertEquals(28, Cadrage.laPlusProche(26.0).mm)
        assertEquals(200, Cadrage.laPlusProche(190.0).mm)
        /* Entre 135 et 200, l'écart relatif départage — pas la différence brute. */
        assertEquals(135, Cadrage.laPlusProche(150.0).mm)
    }

    @Test
    fun `l ecart s exprime en proportion`() {
        assertEquals(0.0, Cadrage.ecartRelatif(50.0, 50.0), 1e-9)
        assertEquals(0.7, Cadrage.ecartRelatif(85.0, 50.0), 1e-9)
        assertEquals(-0.3, Cadrage.ecartRelatif(35.0, 50.0), 1e-9)
    }

    @Test
    fun `le zoom a viser se deduit de la focale voulue`() {
        /* À zoom 1 on est à 26 mm ; pour un 50 il faut zoomer d'un peu moins de deux. */
        val z = Cadrage.zoomPour(equivalentVise = 50.0, equivalentCourant = 26.0, zoomCourant = 1.0)
        assertEquals(50.0 / 26.0, z, 1e-9)
        /* Et le calcul reste juste si l'on part d'un zoom déjà appliqué. */
        assertEquals(4.0, Cadrage.zoomPour(100.0, 50.0, 2.0), 1e-9)
    }

    @Test
    fun `chaque classique porte un angle plausible`() {
        FOCALES_CLASSIQUES.forEach {
            assertTrue(it.angleHorizontal in 5.0..120.0, "${it.mm} mm")
            assertTrue(it.angleDiagonal > it.angleHorizontal, "${it.mm} mm")
        }
        /* Et la liste va bien du plus large au plus long. */
        assertEquals(FOCALES_CLASSIQUES.sortedBy { it.mm }, FOCALES_CLASSIQUES)
    }
}

class RatiosTest {

    @Test
    fun `les catalogues de ratios sont plausibles`() {
        (RATIOS_PHOTO + RATIOS_CINE).forEach {
            assertTrue(it.ratio in 0.5..3.0, "${it.nom} : ratio ${it.ratio}")
            assertTrue(it.nom.isNotBlank())
            assertTrue(it.detail.isNotBlank())
        }
        assertEquals(FamilleRatio.PHOTO, RATIOS_PHOTO.first().famille)
        assertEquals(FamilleRatio.CINE, RATIOS_CINE.first().famille)
        assertTrue(TOUS_LES_RATIOS.size == RATIOS_PHOTO.size + RATIOS_CINE.size)
    }

    @Test
    fun `3 sur 2 est bien le plein format argentique`() {
        assertEquals(1.5, RATIOS_PHOTO.first { it.nom == "3:2" }.ratio, 1e-9)
    }

    @Test
    fun `2,39 scope est plus large que 1,85 flat`() {
        val flat = RATIOS_CINE.first { it.nom == "1.85:1" }.ratio
        val scope = RATIOS_CINE.first { it.nom == "2.39:1" }.ratio
        assertTrue(scope > flat)
    }

    @Test
    fun `un ratio de sortie plus large mange de la hauteur`() {
        /* Scope 2.39 tiré d'un capteur 4:3 : la largeur reste entière,
           la hauteur se réduit d'autant que l'exige le nouveau rapport. */
        val g = guideDeCadrage(rapportCapteur = 4.0 / 3.0, rapportSortie = 2.39)
        assertEquals(1.0, g.largeur, 1e-9)
        assertEquals((4.0 / 3.0) / 2.39, g.hauteur, 1e-9)
        assertTrue(g.hauteur < 1.0)
    }

    @Test
    fun `un ratio de sortie plus etroit mange de la largeur`() {
        /* Portrait 4:5 tiré du même capteur 4:3 : la hauteur reste entière. */
        val g = guideDeCadrage(rapportCapteur = 4.0 / 3.0, rapportSortie = 4.0 / 5.0)
        assertEquals(1.0, g.hauteur, 1e-9)
        assertEquals((4.0 / 5.0) / (4.0 / 3.0), g.largeur, 1e-9)
        assertTrue(g.largeur < 1.0)
    }

    @Test
    fun `le capteur natif ne recadre rien`() {
        val g = guideDeCadrage(rapportCapteur = 4.0 / 3.0, rapportSortie = 4.0 / 3.0)
        assertEquals(1.0, g.largeur, 1e-9)
        assertEquals(1.0, g.hauteur, 1e-9)
    }

    @Test
    fun `le guide de cadrage et cadreUtile s accordent sur la meme geometrie`() {
        /* Les deux fonctions décrivent le même recadrage par deux chemins
           différents : leurs surfaces relatives doivent coïncider. */
        val capteurL = 7.4; val capteurH = 5.55
        val rapportCapteur = capteurL / capteurH
        listOf(1.0, 4.0 / 3.0, 3.0 / 2.0, 1.85, 2.39, 4.0 / 5.0).forEach { sortie ->
            val guide = guideDeCadrage(rapportCapteur, sortie)
            val cadre = Focales.cadreUtile(capteurL, capteurH, 1.0, 1.0, sortie)
            assertEquals(guide.largeur, cadre.largeurMm / capteurL, 1e-9, "sortie $sortie")
            assertEquals(guide.hauteur, cadre.hauteurMm / capteurH, 1e-9, "sortie $sortie")
        }
    }

    @Test
    fun `passer d un ratio a un autre ne peut qu ajouter du recadrage, jamais l inverse`() {
        val g = guideDeCadrage(rapportCapteur = 4.0 / 3.0, rapportSortie = 2.39)
        assertTrue(g.largeur <= 1.0 && g.hauteur <= 1.0)
    }
}
