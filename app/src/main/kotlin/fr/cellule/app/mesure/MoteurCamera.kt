package fr.cellule.app.mesure

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import android.graphics.Rect
import fr.cellule.core.Cadre
import fr.cellule.core.DecodeurLuma
import fr.cellule.core.Equivalence
import fr.cellule.core.ExpositionCamera
import fr.cellule.core.Focales
import fr.cellule.core.OutilsImage
import fr.cellule.core.Reperes
import fr.cellule.core.ResultatSpot
import fr.cellule.core.SeuilsZebras
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
/** Ce qu'il faut savoir pour expliquer une lecture qui ne vient pas. */
data class Diagnostic(
    val largeur: Int,
    val hauteur: Int,
    val pasDeLigne: Int,
    val pasDePixel: Int,
    val octets: Int,
    val rotation: Int,
    val cibleCapteurX: Float,
    val cibleCapteurY: Float,
    val echantillons: Int
)

/**
 * Une caméra arrière du téléphone, telle que le système l'expose à l'appli.
 * [equivalent] est sa focale ramenée au 24×36 (diagonale), quand le téléphone
 * publie la taille de son capteur.
 */
data class CameraTelephone(
    val id: String,
    val focaleMm: Double,
    val equivalent: Double?
) {
    val libelle: String
        get() = if (equivalent != null) "${Math.round(equivalent)} mm" else "caméra $id"
}

/** Les outils d'analyse d'image : chacun ne tourne que si on l'a allumé. */
enum class OutilImage(val libelle: String) {
    FAUX_CONTRASTE("Faux contraste"),
    ZEBRAS("Zébras"),
    HISTOGRAMME("Histogramme"),
    FORME_ONDE("Forme d'onde")
}

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

    /**
     * Proportions de l'image telle qu'elle s'affiche, largeur sur hauteur.
     *
     * Le cadre de l'aperçu doit les adopter exactement : si l'image y était
     * mise en boîte aux lettres, la position touchée ne correspondrait plus au
     * pixel visé, et le spot mesurerait ailleurs que là où on le voit.
     */
    var rapportApercu by mutableStateOf(3f / 4f)
        private set

    /** Dernier diagnostic d'analyse, pour comprendre une lecture qui refuse. */
    var diagnostic by mutableStateOf<Diagnostic?>(null)
        private set

    /**
     * Angle de champ courant et son équivalence 24×36.
     *
     * Elle se recalcule à chaque capture parce que rien n'y est fixe : le zoom
     * numérique recadre le capteur, et sur un téléphone à plusieurs modules
     * l'objectif change physiquement en cours de route.
     */
    var equivalence by mutableStateOf<Equivalence?>(null)
        private set

    var zoomDemande by mutableStateOf(1f)
        private set
    var zoomMinimal by mutableStateOf(1f)
        private set
    var zoomMaximal by mutableStateOf(1f)
        private set

    /**
     * Ratio de sortie choisi (largeur/hauteur), pour recadrer le capteur comme
     * le ferait un cadrage photo ou ciné. Null = capteur plein, sans recadrage —
     * c'est tout ce que l'appareil peut demander nativement (4:3 ou 16:9) ; tout
     * le reste est un cadre logiciel superposé, jamais un mode caméra réel.
     */
    var ratioSortie by mutableStateOf<Double?>(null)
        private set

    fun choisirRatio(ratio: Double?) {
        ratioSortie = ratio
    }

    /** Rapport largeur sur hauteur du flux, dans l'orientation du capteur. */
    private var rapportCapteur = 4f / 3f
    private var capteurLargeurMm = 0.0
    private var capteurHauteurMm = 0.0
    private var zoneActive: Rect? = null

    fun reglerZoom(ratio: Float) {
        val borne = ratio.coerceIn(zoomMinimal, zoomMaximal)
        zoomDemande = borne
        try {
            camera?.cameraControl?.setZoomRatio(borne)
        } catch (e: Exception) {
            /* Certains appareils refusent une valeur pourtant annoncée. */
        }
    }

    fun pincer(facteur: Float) = reglerZoom(zoomDemande * facteur)

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

    /* ── Les caméras du téléphone ──────────────────────────────────────── */

    /** Les caméras arrière que le téléphone expose, de la plus large à la plus longue. */
    var cameras by mutableStateOf<List<CameraTelephone>>(emptyList())
        private set

    /** Celle qui tourne en ce moment. */
    var cameraActive by mutableStateOf<String?>(null)
        private set

    /** Dit pourquoi une caméra choisie n'a pas pu s'ouvrir. */
    var avisCamera by mutableStateOf("")
        private set

    private var cameraChoisie: String? = null
    private var dernierProprietaire: LifecycleOwner? = null
    private var derniereVue: PreviewView? = null

    /** Passe sur une autre caméra du téléphone (ultra grand-angle, téléobjectif…). */
    fun choisirCamera(id: String) {
        if (id == cameraActive) return
        cameraChoisie = id
        avisCamera = ""
        relancer()
    }

    private fun relancer() {
        val p = dernierProprietaire ?: return
        val v = derniereVue ?: return
        exposition = null
        spot = null
        equivalence = null
        aeStable = false
        if (gele) gele = false
        demarrer(p, v)
    }

    /* ── Les outils d'image ────────────────────────────────────────────── */

    /** Les outils allumés. Vide : aucune analyse supplémentaire n'a lieu. */
    var outils by mutableStateOf<Set<OutilImage>>(emptySet())
        private set

    @Volatile
    private var outilsActifs: Set<OutilImage> = emptySet()

    @Volatile
    var seuilsZebras: SeuilsZebras = SeuilsZebras()

    /** Faux contraste et zébras, à dessiner par-dessus l'aperçu. */
    var calque by mutableStateOf<ImageBitmap?>(null)
        private set

    var histogramme by mutableStateOf<IntArray?>(null)
        private set

    /** La forme d'onde, déjà rendue en image. */
    var onde by mutableStateOf<ImageBitmap?>(null)
        private set

    private var dernierOutil = 0L

    fun basculerOutil(outil: OutilImage) {
        regleOutils(if (outil in outils) outils - outil else outils + outil)
    }

    fun eteindreOutils() = regleOutils(emptySet())

    private fun regleOutils(nouveaux: Set<OutilImage>) {
        outils = nouveaux
        outilsActifs = nouveaux
        if (OutilImage.FAUX_CONTRASTE !in nouveaux && OutilImage.ZEBRAS !in nouveaux) calque = null
        if (OutilImage.HISTOGRAMME !in nouveaux) histogramme = null
        if (OutilImage.FORME_ONDE !in nouveaux) onde = null
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
        appliquerOptions()
    }

    /**
     * L'aperçu tourne aussi vite que le téléphone le permet (60 images par
     * seconde au plus) : la cadence de tournage choisie ne sert qu'aux
     * calculs. Imposée à l'aperçu, elle le faisait saccader dès qu'on bougeait.
     */
    private var plageImages: android.util.Range<Int>? = null

    /** La cadence réelle de l'aperçu, quand le téléphone l'a annoncée. */
    var imagesParSeconde by mutableStateOf<Int?>(null)
        private set

    private fun choisirPlageImages(cam: Camera) {
        plageImages = try {
            Camera2CameraInfo.from(cam.cameraInfo)
                .getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                ?.filter { it.upper <= 60 }
                /* La plus rapide, puis la plus stable : [60, 60] avant [15, 60]. */
                ?.maxWithOrNull(compareBy<android.util.Range<Int>>({ it.upper }, { it.lower }))
        } catch (e: Exception) {
            null
        }
        imagesParSeconde = plageImages?.upper
    }

    /** Verrou d'exposition et cadence de l'aperçu, posés ensemble : chaque envoi remplace le précédent. */
    private fun appliquerOptions() {
        try {
            camera?.cameraControl?.let { controle ->
                val options = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, gele)
                plageImages?.let { options.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it) }
                Camera2CameraControl.from(controle).setCaptureRequestOptions(options.build())
            }
        } catch (e: Exception) {
            /* Le gel logiciel suffit, et le téléphone garde sa cadence : ce ne sont que des conforts. */
        }
    }

    fun demarrer(proprietaire: LifecycleOwner, vue: PreviewView) {
        erreur = null
        dernierProprietaire = proprietaire
        derniereVue = vue
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

                if (cameras.isEmpty()) cameras = recenser(provider)
                val voulue = cameraChoisie
                val choix = if (voulue != null && cameras.any { it.id == voulue }) {
                    CameraSelector.Builder()
                        .addCameraFilter { infos ->
                            infos.filter { info ->
                                try {
                                    Camera2CameraInfo.from(info).cameraId == voulue
                                } catch (e: Exception) {
                                    false
                                }
                            }
                        }
                        .build()
                } else CameraSelector.DEFAULT_BACK_CAMERA

                /* La photo pleine définition du plan. Certains téléphones
                   anciens ne tiennent pas trois flux à la fois : on garde alors
                   la mesure, et la capture dira qu'elle n'est pas possible. */
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(selecteur)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                provider.unbindAll()
                val cam = try {
                    provider.bindToLifecycle(proprietaire, choix, apercu, analyse, capture)
                        .also { captureImage = capture }
                } catch (e: IllegalArgumentException) {
                    captureImage = null
                    provider.unbindAll()
                    provider.bindToLifecycle(proprietaire, choix, apercu, analyse)
                }
                camera = cam
                cameraActive = try {
                    Camera2CameraInfo.from(cam.cameraInfo).cameraId
                } catch (e: Exception) {
                    null
                }
                ouvertureParDefaut = ouvertureDeLOptique(cam) ?: 1.8
                relevePlanFocal(cam)
                choisirPlageImages(cam)
                appliquerOptions()
            } catch (e: Exception) {
                if (cameraChoisie != null) {
                    /* Certains modules annoncés refusent de s'ouvrir seuls : on
                       le dit, et on revient à la caméra principale. */
                    cameraChoisie = null
                    avisCamera = "Cette caméra refuse de s'ouvrir seule sur ce téléphone : retour à la principale."
                    demarrer(proprietaire, vue)
                } else {
                    erreur = "Caméra indisponible : ${e.message ?: e.javaClass.simpleName}"
                }
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
        captureImage = null
        executeur?.shutdown()
        executeur = null
        dernierProprietaire = null
        derniereVue = null
    }

    private var captureImage: ImageCapture? = null

    /** Le champ de la photo qu'on prendrait maintenant, selon que le téléphone est couché ou debout. */
    fun champPhoto(couche: Boolean): fr.cellule.core.ChampPhoto? {
        val e = equivalence ?: return null
        /* L'équivalence est donnée dans le sens du capteur : son grand côté
           est la largeur d'une photo couchée, la hauteur d'une photo debout. */
        return if (couche) fr.cellule.core.ChampPhoto(e.angleHorizontal, e.angleVertical)
        else fr.cellule.core.ChampPhoto(e.angleVertical, e.angleHorizontal)
    }

    /** Libellé de la caméra qui tourne, pour le noter avec le plan. */
    fun libelleCameraActive(): String {
        val c = cameras.firstOrNull { it.id == cameraActive }
        val eq = equivalence?.equivalentDiagonal ?: c?.equivalent
        return listOfNotNull(
            eq?.let { "équivalent ${Math.round(it)} mm" },
            equivalence?.let { "focale réelle ${String.format(Locale.FRANCE, "%.2f", it.focaleReelleMm)} mm" },
            if (zoomDemande != 1f) "zoom ×" + String.format(Locale.FRANCE, "%.1f", zoomDemande) else null
        ).joinToString(" · ")
    }

    /**
     * Prend la photo du plan en pleine définition et la range dans la galerie,
     * album « Cellule ». [rotationCible] est la tenue du téléphone
     * (Surface.ROTATION_0, _90 ou _270) : la photo sort dans le bon sens.
     * Rend l'adresse de la photo, ou la raison de l'échec.
     */
    fun capturer(rotationCible: Int, surResultat: (Uri?, String?) -> Unit) {
        val capture = captureImage
        if (capture == null) {
            surResultat(null, "Ce téléphone ne laisse pas prendre la photo pendant la mesure.")
            return
        }
        try {
            capture.targetRotation = rotationCible
            val nom = "Cellule_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
            val valeurs = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nom)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Cellule")
                } else {
                    @Suppress("DEPRECATION")
                    val dossier = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Cellule"
                    )
                    dossier.mkdirs()
                    put(MediaStore.MediaColumns.DATA, File(dossier, nom).absolutePath)
                }
            }
            val options = ImageCapture.OutputFileOptions.Builder(
                contexte.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valeurs
            ).build()
            capture.takePicture(
                options,
                ContextCompat.getMainExecutor(contexte),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(resultat: ImageCapture.OutputFileResults) {
                        surResultat(resultat.savedUri, if (resultat.savedUri == null) "La photo est enregistrée, mais la galerie n'a pas rendu son adresse." else null)
                    }

                    override fun onError(e: ImageCaptureException) {
                        surResultat(null, "La photo n'a pas pu être enregistrée dans la galerie.")
                    }
                }
            )
        } catch (e: Exception) {
            surResultat(null, "La photo n'a pas pu être prise.")
        }
    }

    /**
     * Les caméras arrière visibles par l'appli. Beaucoup de téléphones cachent
     * leurs modules secondaires aux applications tierces : on ne montre que ce
     * que le système veut bien exposer.
     */
    private fun recenser(provider: ProcessCameraProvider): List<CameraTelephone> =
        provider.availableCameraInfos.mapNotNull { info ->
            try {
                val c2 = Camera2CameraInfo.from(info)
                val face = c2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                if (face != CameraMetadata.LENS_FACING_BACK) return@mapNotNull null
                val focale = c2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    ?.firstOrNull()?.toDouble() ?: return@mapNotNull null
                val taille = c2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val equivalent = if (taille != null && taille.width > 0f && taille.height > 0f && focale > 0.0) {
                    Focales.equivalence(Cadre(taille.width.toDouble(), taille.height.toDouble()), focale)
                        .equivalentDiagonal
                } else null
                CameraTelephone(c2.cameraId, focale, equivalent)
            } catch (e: Exception) {
                null
            }
        }.sortedBy { it.equivalent ?: it.focaleMm }

    /** Taille physique du capteur et débattement de zoom : invariants de l'appareil. */
    private fun relevePlanFocal(cam: Camera) {
        try {
            val info = Camera2CameraInfo.from(cam.cameraInfo)
            info.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let {
                capteurLargeurMm = it.width.toDouble()
                capteurHauteurMm = it.height.toDouble()
            }
            zoneActive = info.getCameraCharacteristic(
                CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE
            )
        } catch (e: Exception) {
            /* Sans ces caractéristiques, l'onglet des focales le dira. */
        }
        try {
            cam.cameraInfo.zoomState.value?.let {
                zoomMinimal = it.minZoomRatio
                zoomMaximal = it.maxZoomRatio
                zoomDemande = it.zoomRatio
            }
        } catch (e: Exception) {
            zoomMinimal = 1f
            zoomMaximal = 1f
        }
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

    @Volatile
    private var derniereLecture = 0L

    private val rappelCapture = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            if (gele) return
            /* L'aperçu peut tourner à 60 images par seconde : les chiffres,
               eux, n'ont pas besoin de changer plus de dix fois par seconde,
               et chaque mise à jour redessine l'écran. */
            val maintenant = SystemClock.elapsedRealtime()
            if (maintenant - derniereLecture < 100) return
            derniereLecture = maintenant
            val temps = result.get(CaptureResult.SENSOR_EXPOSURE_TIME) ?: return
            val sensibilite = result.get(CaptureResult.SENSOR_SENSITIVITY) ?: return
            val ouverture = result.get(CaptureResult.LENS_APERTURE)?.toDouble() ?: ouvertureParDefaut

            /* Certains appareils appliquent un gain numérique supplémentaire
               après dématriçage : il compte comme de la sensibilité. */
            val amplification = result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST) ?: 100
            val isoEffectif = (sensibilite * amplification / 100).coerceAtLeast(1)

            /* Seuls deux états signifient que l'exposition bouge encore. Tous
               les autres sont calés — y compris FLASH_REQUIRED, qui veut dire
               « convergé, mais un flash aiderait » et qui est l'état permanent
               en intérieur. Énumérer les états instables plutôt que les stables
               évite de rejeter à tort tout ce qu'on n'avait pas prévu. */
            val etatAe = result.get(CaptureResult.CONTROL_AE_STATE)
            val stable = etatAe != CaptureResult.CONTROL_AE_STATE_SEARCHING &&
                etatAe != CaptureResult.CONTROL_AE_STATE_PRECAPTURE

            val lecture = ExpositionCamera(ouverture, temps, isoEffectif, compensation())
            val equiv = calculeEquivalence(result)
            principal.post {
                if (!gele) {
                    exposition = lecture
                    aeStable = stable
                }
                if (equiv != null) equivalence = equiv
            }
        }
    }

    /**
     * La focale déclarée et la zone de capteur réellement lue donnent l'angle
     * de champ ; le reste n'est que géométrie.
     */
    private fun calculeEquivalence(result: TotalCaptureResult): Equivalence? {
        val focale = result.get(CaptureResult.LENS_FOCAL_LENGTH)?.toDouble() ?: return null
        if (focale <= 0.0 || capteurLargeurMm <= 0.0) return null
        val active = zoneActive ?: return null
        val recadrage = result.get(CaptureResult.SCALER_CROP_REGION)
        val fx = if (recadrage != null && active.width() > 0)
            recadrage.width().toDouble() / active.width() else 1.0
        val fy = if (recadrage != null && active.height() > 0)
            recadrage.height().toDouble() / active.height() else 1.0
        val cadre = Focales.cadreUtile(
            capteurLargeurMm, capteurHauteurMm,
            fx.coerceIn(0.001, 1.0), fy.coerceIn(0.001, 1.0),
            ratioSortie ?: rapportCapteur.toDouble()
        )
        return Focales.equivalence(cadre, focale)
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
            rapportCapteur = image.width.toFloat() / image.height.toFloat()
            val rapport = if (rotation % 180 == 90) {
                image.height.toFloat() / image.width.toFloat()
            } else {
                image.width.toFloat() / image.height.toFloat()
            }
            val diag = Diagnostic(
                largeur = image.width,
                hauteur = image.height,
                pasDeLigne = plan.rowStride,
                pasDePixel = plan.pixelStride,
                octets = octets.size,
                rotation = rotation,
                cibleCapteurX = x,
                cibleCapteurY = y,
                echantillons = resultat.echantillons
            )
            principal.post {
                if (!gele) {
                    rotationAnalyse = rotation
                    rapportApercu = rapport
                    spot = resultat
                    diagnostic = diag
                }
            }
            analyserOutils(octets, image.width, image.height, plan.rowStride, plan.pixelStride, rotation)
        } catch (e: Exception) {
            /* Une image ratée n'est pas une panne : la suivante arrive. */
        } finally {
            image.close()
        }
    }

    /**
     * Faux contraste, zébras, histogramme, forme d'onde. Rien ne se calcule
     * tant qu'aucun outil n'est allumé, et jamais plus de huit fois par
     * seconde : c'est assez pour l'œil, et ça ménage la batterie.
     */
    private fun analyserOutils(
        octets: ByteArray, largeur: Int, hauteur: Int, pasDeLigne: Int, pasDePixel: Int, rotation: Int
    ) {
        val actifs = outilsActifs
        if (actifs.isEmpty()) return
        val maintenant = SystemClock.elapsedRealtime()
        if (maintenant - dernierOutil < 125) return
        dernierOutil = maintenant

        val grille = OutilsImage.grille(
            octets, largeur, hauteur, pasDeLigne, pasDePixel, rotation, plageVideo, grandCote = 160
        )
        var nouveauCalque: ImageBitmap? = null
        if (OutilImage.FAUX_CONTRASTE in actifs || OutilImage.ZEBRAS in actifs) {
            val couleurs = if (OutilImage.FAUX_CONTRASTE in actifs) OutilsImage.fauxContraste(grille)
            else IntArray(grille.largeur * grille.hauteur)
            if (OutilImage.ZEBRAS in actifs) {
                val rayures = OutilsImage.zebras(grille, seuilsZebras)
                for (i in rayures.indices) if (rayures[i] != OutilsImage.TRANSPARENT) couleurs[i] = rayures[i]
            }
            nouveauCalque = Bitmap.createBitmap(couleurs, grille.largeur, grille.hauteur, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }
        val nouvelHistogramme =
            if (OutilImage.HISTOGRAMME in actifs) OutilsImage.histogramme(grille, CLASSES_HISTOGRAMME) else null
        var nouvelleOnde: ImageBitmap? = null
        if (OutilImage.FORME_ONDE in actifs) {
            val comptes = OutilsImage.formeDOnde(grille, COLONNES_ONDE, NIVEAUX_ONDE)
            /* Une colonne de l'image tombe sur quelques niveaux : dès qu'un
               niveau rassemble un huitième de la colonne, il est pleinement lumineux. */
            val plein = maxOf(1, grille.hauteur * grille.largeur / COLONNES_ONDE / 8)
            val pixels = IntArray(comptes.size) { i ->
                val alpha = (comptes[i] * 255 / plein).coerceIn(0, 255)
                (alpha shl 24) or 0x00F3EEE6
            }
            nouvelleOnde = Bitmap.createBitmap(pixels, COLONNES_ONDE, NIVEAUX_ONDE, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }
        principal.post {
            val encore = outils
            if (OutilImage.FAUX_CONTRASTE in encore || OutilImage.ZEBRAS in encore) calque = nouveauCalque
            if (OutilImage.HISTOGRAMME in encore) histogramme = nouvelHistogramme
            if (OutilImage.FORME_ONDE in encore) onde = nouvelleOnde
        }
    }

    companion object {
        const val CLASSES_HISTOGRAMME = 64
        const val COLONNES_ONDE = 96
        const val NIVEAUX_ONDE = 51
    }
}
