package fr.cellule.app.mesure

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Handler
import android.os.Looper
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import fr.cellule.core.DecodeurLuma
import fr.cellule.core.ExpositionCamera
import fr.cellule.core.Reperes
import fr.cellule.core.ResultatSpot
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * La caméra en posemètre réfléchi.
 *
 * Deux flux séparés y concourent. L'aperçu porte les métadonnées d'exposition —
 * temps de pose, sensibilité, ouverture — que l'algorithme d'exposition a
 * choisies : c'est de là que sort la mesure. L'analyse porte le plan de
 * luminance, dont on tire la valeur de la zone visée.
 *
 * Comme les deux flux ne livrent pas la même image, ils ne se correspondent que
 * lorsque l'exposition automatique a convergé. D'où [aeStable], qu'il faut
 * respecter : tant qu'elle est fausse, la lecture est en train de bouger et ne
 * vaut rien.
 */
@OptIn(ExperimentalCamera2Interop::class)
class MoteurCamera(private val contexte: Context) {

    /** Métadonnées de la dernière capture aboutie. */
    var exposition by mutableStateOf<ExpositionCamera?>(null)
        private set

    /** Analyse du disque visé. */
    var spot by mutableStateOf<ResultatSpot?>(null)
        private set

    var rotationAnalyse by mutableStateOf(0)
        private set

    /** L'exposition automatique a convergé : les deux flux se correspondent. */
    var aeStable by mutableStateOf(false)
        private set

    var erreur by mutableStateOf<String?>(null)
        private set

    /** Position visée, en coordonnées d'écran normalisées. */
    var cibleU by mutableStateOf(0.5f)
        private set
    var cibleV by mutableStateOf(0.5f)
        private set

    var rayon by mutableStateOf(0.055f)
        private set

    /** Lecture figée : ni les métadonnées ni le spot ne bougent plus. */
    var gele by mutableStateOf(false)
        private set

    var plageVideo: Boolean = true
        set(valeur) {
            field = valeur
            decodeur = DecodeurLuma(plageVideo = valeur)
        }

    private var decodeur = DecodeurLuma(plageVideo = true)
    private val principal = Handler(Looper.getMainLooper())
    private var executeur: ExecutorService? = null
    private var fournisseur: ProcessCameraProvider? = null
    private var camera: Camera? = null

    /** Repli quand l'appareil ne publie pas LENS_APERTURE, cas des optiques fixes. */
    private var ouvertureParDefaut = 1.8

    fun viser(u: Float, v: Float) {
        cibleU = u.coerceIn(0f, 1f)
        cibleV = v.coerceIn(0f, 1f)
    }

    fun reglerRayon(r: Float) {
        rayon = r.coerceIn(0.02f, 0.30f)
    }

    /**
     * Fige la lecture. On verrouille aussi l'exposition côté matériel quand
     * c'est possible : sans cela l'appareil continue de se réajuster et les
     * métadonnées cessent de correspondre à l'image analysée.
     */
    fun basculerGel() {
        gele = !gele
        try {
            camera?.cameraControl?.let { controle ->
                Camera2CameraControl.from(controle).setCaptureRequestOptions(
                    CaptureRequestOptions.Builder()
                        .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, gele)
                        .build()
                )
            }
        } catch (e: Exception) {
            /* Le gel logiciel suffit ; le verrou matériel n'est qu'un confort. */
        }
    }

    fun demarrer(proprietaire: LifecycleOwner, vue: PreviewView) {
        erreur = null
        val executeurLocal = executeur ?: Executors.newSingleThreadExecutor().also { executeur = it }
        val futur = ProcessCameraProvider.getInstance(contexte)
        futur.addListener({
            try {
                val provider = futur.get()
                fournisseur = provider

                val selecteur = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .build()

                val constructeurApercu = Preview.Builder().setResolutionSelector(selecteur)
                Camera2Interop.Extender(constructeurApercu).setSessionCaptureCallback(rappelCapture)
                val apercu = constructeurApercu.build()
                apercu.setSurfaceProvider(vue.surfaceProvider)

                val analyse = ImageAnalysis.Builder()
                    .setResolutionSelector(selecteur)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                analyse.setAnalyzer(executeurLocal) { image -> analyser(image) }

                provider.unbindAll()
                val cam = provider.bindToLifecycle(
                    proprietaire, CameraSelector.DEFAULT_BACK_CAMERA, apercu, analyse
                )
                camera = cam
                ouvertureParDefaut = ouvertureDeLOptique(cam) ?: ouvertureParDefaut
            } catch (e: Exception) {
                erreur = "Caméra indisponible : ${e.message ?: e.javaClass.simpleName}"
            }
        }, ContextCompat.getMainExecutor(contexte))
    }

    fun arreter() {
        try {
            fournisseur?.unbindAll()
        } catch (e: Exception) {
            /* rien à faire : on s'arrête de toute façon */
        }
        camera = null
        executeur?.shutdown()
        executeur = null
    }

    private fun ouvertureDeLOptique(cam: Camera): Double? = try {
        Camera2CameraInfo.from(cam.cameraInfo)
            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            ?.firstOrNull()?.toDouble()
    } catch (e: Exception) {
        null
    }

    /** Correction d'exposition actuellement demandée à l'appareil, en diaphs. */
    private fun compensation(): Double = try {
        val etat = camera?.cameraInfo?.exposureState
        if (etat != null && etat.isExposureCompensationSupported) {
            etat.exposureCompensationIndex * etat.exposureCompensationStep.toDouble()
        } else 0.0
    } catch (e: Exception) {
        0.0
    }

    private val rappelCapture = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            if (gele) return
            val temps = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return
            val sensibilite = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return
            val ouverture = result.get(CaptureResult.LENS_APERTURE)?.toDouble() ?: ouvertureParDefaut

            /* Certains appareils appliquent un gain numérique supplémentaire
               après dématriçage : il compte comme de la sensibilité. */
            val amplification = result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST) ?: 100
            val isoEffectif = (sensibilite * amplification / 100).coerceAtLeast(1)

            val etatAe = result.get(CaptureResult.CONTROL_AE_STATE)
            val stable = etatAe == null ||
                etatAe == CaptureResult.CONTROL_AE_STATE_CONVERGED ||
                etatAe == CaptureResult.CONTROL_AE_STATE_LOCKED

            val lecture = ExpositionCamera(ouverture, temps, isoEffectif, compensation())
            principal.post {
                if (!gele) {
                    exposition = lecture
                    aeStable = stable
                }
            }
        }
    }

    private fun analyser(image: ImageProxy) {
        try {
            if (gele) return
            val plan = image.planes[0]
            val tampon = plan.buffer
            val octets = ByteArray(tampon.remaining())
            tampon.get(octets)

            val rotation = image.imageInfo.rotationDegrees
            val (x, y) = Reperes.ecranVersCapteur(cibleU, cibleV, rotation)
            val resultat = decodeur.analyser(
                luma = octets,
                largeur = image.width,
                hauteur = image.height,
                rowStride = plan.rowStride,
                pixelStride = plan.pixelStride,
                centreX = x,
                centreY = y,
                rayonRelatif = rayon
            )
            principal.post {
                if (!gele) {
                    rotationAnalyse = rotation
                    spot = resultat
                }
            }
        } catch (e: Exception) {
            /* Une image ratée n'est pas une panne : la suivante arrive. */
        } finally {
            image.close()
        }
    }
}
