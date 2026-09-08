package fr.cellule.app.mesure

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Size
import android.util.SizeF
import kotlin.math.sqrt

/**
 * Inventaire du matériel de prise de vue, via Camera2.
 *
 * `CameraManager.getCameraIdList()` ne rend qu'un identifiant par module tel
 * qu'exposé aux applications : sur beaucoup de téléphones récents, plusieurs
 * capteurs physiques du dos (grand-angle, ultra grand-angle, téléobjectif,
 * macro…) sont regroupés derrière un seul identifiant « logique ». Quand ce
 * module déclare la capacité LOGICAL_MULTI_CAMERA, ses capteurs physiques se
 * retrouvent via [CameraCharacteristics.getPhysicalCameraIds] et peuvent être
 * requêtés un par un — c'est ce qui permet de voir quatre capteurs arrière
 * plutôt qu'un seul. D'autres constructeurs exposent au contraire chaque
 * capteur comme son propre identifiant de premier niveau ; les deux cas sont
 * couverts puisqu'on parcourt aussi tous les identifiants racine.
 *
 * Lire ces caractéristiques ne demande pas la permission CAMERA et ne
 * capture aucune image — seule l'ouverture d'une caméra le ferait.
 */
data class InfoCapteurPhoto(
    val id: String,
    val idModule: String,
    val face: String,
    val focaleMm: Float?,
    val focaleEquivalente35mm: Double?,
    val ouvertureMax: Float?,
    val tailleCapteurMm: SizeF?,
    val resolutionPx: Size?,
    val megapixels: Double?,
    val autofocus: Boolean,
    val macroProbable: Boolean,
    val stabilisationOptique: Boolean,
    val flash: Boolean,
    val niveauMateriel: String,
    val profondeurSeule: Boolean,
    val role: String
)

data class ModuleCamera(
    val idModule: String,
    val face: String,
    val estMultiCapteurs: Boolean,
    val capteurs: List<InfoCapteurPhoto>
)

object InventaireCapteurs {

    /** Diagonale d'un capteur 24×36, référence usuelle des focales « équivalentes ». */
    private const val DIAGONALE_24X36_MM = 43.267

    fun lister(contexte: Context): List<ModuleCamera> {
        val gestionnaire =
            contexte.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return emptyList()
        val ids = try {
            gestionnaire.cameraIdList
        } catch (e: Exception) {
            return emptyList()
        }
        return ids.mapNotNull { id -> decrireModule(gestionnaire, id) }
    }

    private fun decrireModule(gestionnaire: CameraManager, id: String): ModuleCamera? {
        val racine = try {
            gestionnaire.getCameraCharacteristics(id)
        } catch (e: Exception) {
            return null
        }
        val face = libelleFace(racine.get(CameraCharacteristics.LENS_FACING))
        val capacites = racine.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList().orEmpty()
        val estLogique = capacites.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)

        /* getPhysicalCameraIds() n'existe qu'à partir de l'API 28 : sous ce
           niveau, ou si le module n'est pas multi-capteurs, on le décrit
           avec ses propres caractéristiques plutôt qu'avec un sous-capteur. */
        val idsPhysiques = if (estLogique && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try { racine.physicalCameraIds.toList() } catch (e: Exception) { emptyList() }
        } else {
            emptyList()
        }

        val capteurs = idsPhysiques.mapNotNull { pid ->
            try {
                decrireCapteur(gestionnaire.getCameraCharacteristics(pid), pid, id, face)
            } catch (e: Exception) {
                /* Certains fabricants referment l'accès aux caractéristiques
                   physiques individuelles ; ce sous-capteur reste simplement
                   absent du détail plutôt que de faire échouer tout le module. */
                null
            }
        }.ifEmpty { listOf(decrireCapteur(racine, id, id, face)) }

        return ModuleCamera(id, face, capteurs.size > 1, capteurs)
    }

    private fun decrireCapteur(
        c: CameraCharacteristics,
        id: String,
        idModule: String,
        face: String
    ): InfoCapteurPhoto {
        val focale = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull()
        val ouverture = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.firstOrNull()
        val taille = c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val pixels = c.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val distanceMini = c.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val ois = c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
        val flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        val niveau = libelleNiveau(c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL))
        val capacites = c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList().orEmpty()
        val profondeurSeule = capacites.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) &&
            !capacites.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)

        val equivalente = if (focale != null && taille != null) {
            val diagonale = sqrt((taille.width * taille.width + taille.height * taille.height).toDouble())
            if (diagonale > 0) focale * (DIAGONALE_24X36_MM / diagonale) else null
        } else null

        val megapixels = pixels?.let { it.width.toLong() * it.height.toLong() / 1_000_000.0 }
        /* En dioptries : 0 signifie mise au point fixe à l'infini, une valeur
           haute signifie une distance mini de mise au point très courte. */
        val autofocus = distanceMini != null && distanceMini > 0f
        val macroProbable = distanceMini != null && distanceMini >= 8f

        return InfoCapteurPhoto(
            id = id,
            idModule = idModule,
            face = face,
            focaleMm = focale,
            focaleEquivalente35mm = equivalente,
            ouvertureMax = ouverture,
            tailleCapteurMm = taille,
            resolutionPx = pixels,
            megapixels = megapixels,
            autofocus = autofocus,
            macroProbable = macroProbable,
            stabilisationOptique = ois != null && ois.any { it != 0 },
            flash = flash,
            niveauMateriel = niveau,
            profondeurSeule = profondeurSeule,
            role = role(equivalente, macroProbable, profondeurSeule, megapixels)
        )
    }

    /**
     * Android ne publie aucun champ « rôle » : ni « ultra grand-angle », ni
     * « macro », ni « téléobjectif ». On l'approche depuis la focale
     * équivalente 24×36 et la distance de mise au point la plus proche —
     * une estimation, pas une donnée constructeur.
     */
    private fun role(equivMm: Double?, macroProbable: Boolean, profondeurSeule: Boolean, mpx: Double?): String {
        if (profondeurSeule) return "capteur de profondeur seul (ToF) — n'expose pas d'image exploitable directement"
        val base = when {
            equivMm == null -> "indéterminé (focale non publiée)"
            equivMm <= 20 -> "ultra grand-angle"
            equivMm <= 34 -> "principal (grand-angle standard)"
            equivMm <= 48 -> "principal"
            equivMm <= 90 -> "téléobjectif court (~2-3×)"
            else -> "téléobjectif long"
        }
        return when {
            macroProbable && (mpx == null || mpx < 5.0) -> "$base, mise au point très proche — probablement le macro"
            macroProbable -> "$base, mise au point très proche possible"
            else -> base
        }
    }

    private fun libelleFace(f: Int?): String = when (f) {
        CameraCharacteristics.LENS_FACING_FRONT -> "avant"
        CameraCharacteristics.LENS_FACING_BACK -> "arrière"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "externe"
        else -> "inconnue"
    }

    private fun libelleNiveau(n: Int?): String = when (n) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "legacy"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "limited"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "full"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "level_3"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "external"
        else -> "inconnu"
    }
}
