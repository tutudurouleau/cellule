package fr.cellule.core

import kotlin.math.abs
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosemetreTest {

    /** Un plan Y uniforme, tel que la caméra le livrerait. */
    private fun planUniforme(valeur: Int, cote: Int = 160) =
        ByteArray(cote * cote) { valeur.toByte() }

    /** Encodage sRGB d'une luminance relative, pour fabriquer des cas de test. */
    private fun lumaPour(lineaire: Double, plageVideo: Boolean): Int {
        val e = if (lineaire <= 0.0031308) lineaire * 12.92
        else 1.055 * lineaire.pow(1 / 2.4) - 0.055
        return if (plageVideo) Math.round(16 + e * 219).toInt() else Math.round(e * 255).toInt()
    }

    @Test
    fun `l EV se deduit du triplet d exposition`() {
        /* f/1,8 au 1/120 à 100 ISO : ce que donne un téléphone en intérieur. */
        val e = ExpositionCamera(ouverture = 1.8, tempsPoseNs = 8_333_333, iso = 100)
        assertEquals(8.6, e.evAppareil, 0.05)
        assertEquals(8.6, e.ev100, 0.05)
    }

    @Test
    fun `la sensibilite est bien retiree de la mesure`() {
        /* Mêmes réglages, sensibilité multipliée par huit : la scène est
           trois diaphs plus sombre. */
        val cent = ExpositionCamera(1.8, 8_333_333, 100)
        val huitCents = ExpositionCamera(1.8, 8_333_333, 800)
        assertEquals(3.0, cent.ev100 - huitCents.ev100, 1e-6)
    }

    @Test
    fun `la compensation d exposition s ajoute dans le bon sens`() {
        /* Demander un diaph de plus fait choisir à l'appareil une exposition
           plus généreuse d'un diaph — la scène mesurée, elle, n'a pas bougé. */
        val sansCorrection = ExpositionCamera(16.0, 8_000_000, 100)
        /* Un diaph exact vaut 16/√2, pas f/11 : l'échelle des diaphragmes est
           arrondie, et c'est le calcul qu'on teste ici, pas l'échelle. */
        val avecPlusUn = ExpositionCamera(16.0 / kotlin.math.sqrt(2.0), 8_000_000, 100, compensationEv = 1.0)
        assertEquals(sansCorrection.ev100, avecPlusUn.ev100, 1e-9)

        /* Et une correction négative fait bien descendre la mesure. */
        val avecMoinsUn = ExpositionCamera(16.0 * kotlin.math.sqrt(2.0), 8_000_000, 100, compensationEv = -1.0)
        assertEquals(sansCorrection.ev100, avecMoinsUn.ev100, 1e-9)
    }

    @Test
    fun `le gris moyen se decode a 18 pour cent`() {
        listOf(true, false).forEach { video ->
            val d = DecodeurLuma(plageVideo = video)
            val luma = lumaPour(0.18, video)
            assertEquals(0.18, d.lineaire(luma), 0.005, "plage vidéo = $video")
        }
        /* Le fameux 118 sur 255 en pleine plage. */
        assertEquals(118, lumaPour(0.18, plageVideo = false))
    }

    @Test
    fun `le decodage est monotone et borne`() {
        val d = DecodeurLuma()
        assertEquals(0.0, d.lineaire(0), 1e-9)
        assertEquals(1.0, d.lineaire(255), 0.01)
        var precedent = -1.0
        for (y in 0..255) {
            val v = d.lineaire(y)
            assertTrue(v >= precedent, "décroissance à y = $y")
            precedent = v
        }
    }

    @Test
    fun `un spot sur le gris moyen lit la meme chose que la mesure globale`() {
        val d = DecodeurLuma(plageVideo = true)
        val plan = planUniforme(lumaPour(0.18, true))
        val spot = d.analyser(plan, 160, 160, rowStride = 160)
        val expo = ExpositionCamera(1.8, 8_333_333, 100)
        assertEquals(Posemetre.ev100Moyen(expo), Posemetre.ev100Spot(expo, spot), 0.05)
    }

    @Test
    fun `un spot deux diaphs plus clair lit deux diaphs de plus`() {
        val d = DecodeurLuma(plageVideo = true)
        val expo = ExpositionCamera(1.8, 8_333_333, 100)
        listOf(-2.0, -1.0, 0.0, 1.0, 2.0).forEach { diaphs ->
            val plan = planUniforme(lumaPour(0.18 * 2.0.pow(diaphs), true))
            val spot = d.analyser(plan, 160, 160, rowStride = 160)
            assertEquals(
                Posemetre.ev100Moyen(expo) + diaphs,
                Posemetre.ev100Spot(expo, spot),
                0.06,
                "à $diaphs diaph du gris"
            )
        }
    }

    @Test
    fun `la moyenne se fait en lineaire, pas en gamma`() {
        /* Moitié noir, moitié blanc. En linéaire la moyenne vaut ~0,5 ;
           moyenner les valeurs gamma d'abord donnerait ~0,21 — plus de un
           diaph d'erreur, systématiquement dans les hautes lumières. */
        val d = DecodeurLuma(plageVideo = false)
        val cote = 160
        val plan = ByteArray(cote * cote) { i -> if ((i % cote) < cote / 2) 0 else 255.toByte().toInt().toByte() }
        val spot = d.analyser(plan, cote, cote, rowStride = cote, rayonRelatif = 0.45f)
        assertEquals(0.5, spot.moyenneLineaire, 0.05)
    }

    @Test
    fun `un spot crame ou bouche est signale comme non fiable`() {
        val d = DecodeurLuma()
        assertFalse(d.analyser(planUniforme(255), 160, 160, 160).fiable)
        assertFalse(d.analyser(planUniforme(0), 160, 160, 160).fiable)
        assertTrue(d.analyser(planUniforme(120), 160, 160, 160).fiable)
    }

    @Test
    fun `le spot ne lit que le disque vise`() {
        /* Fond blanc, pastille sombre au centre : le spot doit lire la pastille. */
        val cote = 200
        val d = DecodeurLuma(plageVideo = false)
        val sombre = lumaPour(0.045, false)
        val plan = ByteArray(cote * cote) { i ->
            val x = i % cote; val y = i / cote
            val dx = x - cote / 2.0; val dy = y - cote / 2.0
            if (dx * dx + dy * dy < 20.0 * 20.0) sombre.toByte() else 235.toByte()
        }
        val spot = d.analyser(plan, cote, cote, rowStride = cote, rayonRelatif = 0.06f)
        assertEquals(0.045, spot.moyenneLineaire, 0.01)
    }

    @Test
    fun `le pas de ligne decale les lignes correctement`() {
        /* Un plan avec du remplissage en fin de ligne ne doit pas fausser la lecture. */
        val largeur = 160; val hauteur = 160; val rowStride = 192
        val d = DecodeurLuma(plageVideo = false)
        val cible = lumaPour(0.18, false)
        val plan = ByteArray(rowStride * hauteur) { 0 }
        for (y in 0 until hauteur) for (x in 0 until largeur) plan[y * rowStride + x] = cible.toByte()
        val spot = d.analyser(plan, largeur, hauteur, rowStride = rowStride)
        assertEquals(0.18, spot.moyenneLineaire, 0.005)
    }

    @Test
    fun `l etalonnage sur charte grise retrouve le decalage`() {
        val d = DecodeurLuma(plageVideo = true)
        val expo = ExpositionCamera(1.8, 8_333_333, 100)
        val spot = d.analyser(planUniforme(lumaPour(0.18, true)), 160, 160, 160)
        /* On sait par ailleurs que la scène vaut 9,3 EV : l'écart mesuré doit
           être exactement le décalage à mémoriser. */
        val vrai = 9.3
        val offset = Posemetre.etalonnageDepuis(expo, spot, ev100Vrai = vrai)
        assertEquals(vrai, Posemetre.ev100Spot(expo, spot, etalonnage = offset), 0.02)
    }

    @Test
    fun `le capteur d ambiance lit en incident`() {
        val plein = LectureIncidente(lux = 82000f, luxMax = 100000f, resolution = 1f)
        assertEquals(15.0, plein.ev100, 0.02)
        assertFalse(plein.sature)

        /* La plupart des capteurs butent bien avant le plein soleil. */
        val bute = LectureIncidente(lux = 29800f, luxMax = 30000f, resolution = 1f)
        assertTrue(bute.sature)

        /* Et quantifient trop grossièrement dans le bas. */
        val faible = LectureIncidente(lux = 2f, luxMax = 30000f, resolution = 1f)
        assertTrue(faible.tropFaible)
    }

    @Test
    fun `camera et capteur d ambiance concordent sur une charte grise`() {
        /* Une charte grise sous 82 000 lx : la voie incidente donne EV 15,0 ;
           la voie réfléchie, calibrée, doit retomber dessus. */
        val incident = LectureIncidente(82000f, 200000f, 1f)
        val d = DecodeurLuma(plageVideo = true)
        val spot = d.analyser(planUniforme(lumaPour(0.18, true)), 160, 160, 160)
        /* f/1,8 au 1/22000 à 50 ISO, ce qu'exige EV 15 sur un téléphone. */
        val t = Photometrie.tempsPosePour(15.0 + Photometrie.decalageIso(50), 1.8)
        val expo = ExpositionCamera(1.8, (t * 1e9).toLong(), 50)
        assertEquals(incident.ev100, Posemetre.ev100Spot(expo, spot), 0.06)
    }
}
