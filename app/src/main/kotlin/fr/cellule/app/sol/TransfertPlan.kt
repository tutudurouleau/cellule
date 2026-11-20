package fr.cellule.app.sol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.app.PlansSol
import fr.cellule.app.ecrans.BoutonPlat
import fr.cellule.app.ecrans.Champ
import fr.cellule.app.ecrans.LigneBoutons
import fr.cellule.core.PlanCodec
import fr.cellule.core.PlanSol
import java.util.concurrent.Executors

/*
 * Passer un plan au sol d'un appareil à l'autre, sans compte ni serveur :
 * des QR codes à scanner, un texte à coller, ou un fichier. C'est aussi le
 * chemin qu'empruntera l'éditeur de plans sur PC.
 */

/** Un QR code net, noir sur blanc, de [taille] pixels de côté. */
private fun qr(texte: String, taille: Int = 900): Bitmap {
    val matrice = QRCodeWriter().encode(
        texte, BarcodeFormat.QR_CODE, taille, taille,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 2)
    )
    val pixels = IntArray(taille * taille) { i -> if (matrice.get(i % taille, i / taille)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, taille, taille, Bitmap.Config.ARGB_8888)
}

/** Les QR codes d'un plan, un par un, et le partage en texte. */
@Composable
internal fun ContenuEnvoiPlan(plan: PlanSol) {
    val contexte = LocalContext.current
    val morceaux = remember(plan) { PlanCodec.morceaux(PlansSol.versTexte(plan)) }
    var i by remember(plan) { mutableIntStateOf(0) }
    val image = remember(morceaux, i) { qr(morceaux[i]).asImageBitmap() }
    Text(
        "Sur l'autre téléphone : Plans au sol › Recevoir un plan › Scanner. " +
            if (morceaux.size > 1) "Montre les ${morceaux.size} codes l'un après l'autre." else "",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Interligne))
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)) {
        Image(image, contentDescription = "QR code ${i + 1} sur ${morceaux.size}", modifier = Modifier.fillMaxWidth().aspectRatio(1f))
    }
    if (morceaux.size > 1) {
        Spacer(Modifier.height(6.dp))
        Text("Code ${i + 1} sur ${morceaux.size}", style = Corps, color = MaterialTheme.colorScheme.onSurface)
        LigneBoutons {
            BoutonPlat("‹ Précédent", actif = i > 0, modifier = Modifier.weight(1f)) { i-- }
            BoutonPlat("Suivant ›", actif = i < morceaux.size - 1, accent = true, modifier = Modifier.weight(1f)) { i++ }
        }
    }
    Spacer(Modifier.height(Interligne))
    BoutonPlat("Partager en texte", modifier = Modifier.fillMaxWidth()) {
        val envoi = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Plan au sol : ${plan.nom}")
            putExtra(Intent.EXTRA_TEXT, "Plan au sol « ${plan.nom} » pour Cellule (Plans au sol › Recevoir un plan › Coller) :\n\n" + morceaux.joinToString("\n"))
        }
        contexte.startActivity(Intent.createChooser(envoi, "Partager le plan"))
    }
    Text(
        "Le texte s'envoie par message ou par mail, à toi-même ou à l'équipe : c'est toi qui choisis où il part.",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
    )
}

/** Recevoir un plan : scanner ses QR codes, coller son texte, ou ouvrir un fichier. */
@Composable
internal fun ContenuReceptionPlan(jourId: Long?, surRecu: (PlanSol) -> Unit) {
    val contexte = LocalContext.current
    var mode by remember { mutableIntStateOf(0) }
    var colle by remember { mutableStateOf("") }
    var erreur by remember { mutableStateOf("") }
    val lus = remember { mutableStateListOf<String>() }
    fun essayer(json: String?) {
        val p = json?.let { PlansSol.depuisTexte(it, jourId) }
        if (p != null) surRecu(p) else erreur = "Ce n'est pas un plan au sol de Cellule, ou il manque des morceaux."
    }
    val fichier = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val texte = runCatching { contexte.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull().orEmpty()
            essayer(PlanCodec.depuisTexte(texte) ?: texte.takeIf { it.trimStart().startsWith("[") })
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) mode = 1 }

    LigneBoutons {
        BoutonPlat("Scanner", accent = mode == 1, modifier = Modifier.weight(1f)) {
            if (ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) mode = 1
            else permission.launch(Manifest.permission.CAMERA)
        }
        BoutonPlat("Coller", accent = mode == 2, modifier = Modifier.weight(1f)) { mode = 2 }
        BoutonPlat("Fichier", modifier = Modifier.weight(1f)) { fichier.launch(arrayOf("text/*", "application/json", "application/octet-stream")) }
    }
    Spacer(Modifier.height(Interligne))
    when (mode) {
        1 -> {
            ScannerQr { texte ->
                if (PlanCodec.lire(texte) != null && texte !in lus) {
                    lus += texte
                    PlanCodec.assembler(lus)?.let { essayer(it) }
                }
            }
            val total = lus.firstNotNullOfOrNull { PlanCodec.lire(it)?.second }
            Text(
                if (total == null) "Vise le QR code affiché sur l'autre téléphone." else "${lus.size} code(s) lu(s) sur $total : passe au suivant.",
                style = Corps, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp)
            )
        }
        2 -> {
            Champ(colle, "Le texte reçu", Modifier.fillMaxWidth().height(160.dp), surUneLigne = false) { colle = it }
            Spacer(Modifier.height(6.dp))
            BoutonPlat("Ajouter ce plan", actif = colle.isNotBlank(), accent = true, modifier = Modifier.fillMaxWidth()) {
                essayer(PlanCodec.depuisTexte(colle) ?: colle.takeIf { it.trimStart().startsWith("[") })
            }
        }
        else -> Text(
            "Le plan reçu s'ajoute à tes plans, sans jamais en remplacer un.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (erreur.isNotBlank()) Text(erreur, style = Detail, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
}

/** L'aperçu de la caméra, qui lit les QR codes au passage. */
@Composable
private fun ScannerQr(surLu: (String) -> Unit) {
    val contexte = LocalContext.current
    val cycle = LocalLifecycleOwner.current
    val executeur = remember { Executors.newSingleThreadExecutor() }
    val lecteur = remember {
        MultiFormatReader().apply { setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true)) }
    }
    val vue = remember { PreviewView(contexte) }
    DisposableEffect(Unit) {
        val futur = ProcessCameraProvider.getInstance(contexte)
        futur.addListener({
            val fournisseur = futur.get()
            val apercu = Preview.Builder().build().also { it.setSurfaceProvider(vue.surfaceProvider) }
            @Suppress("DEPRECATION")
            val analyse = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 960))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analyse.setAnalyzer(executeur) { image ->
                try {
                    val texte = runCatching {
                        val plan = image.planes[0]
                        val tampon = plan.buffer
                        val octets = ByteArray(tampon.remaining()).also { tampon.get(it) }
                        val source = PlanarYUVLuminanceSource(octets, plan.rowStride, image.height, 0, 0, image.width, image.height, false)
                        lecteur.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
                    }.getOrNull()
                    lecteur.reset()
                    if (texte != null) ContextCompat.getMainExecutor(contexte).execute { surLu(texte) }
                } finally {
                    image.close()
                }
            }
            runCatching {
                fournisseur.unbindAll()
                fournisseur.bindToLifecycle(cycle, CameraSelector.DEFAULT_BACK_CAMERA, apercu, analyse)
            }
        }, ContextCompat.getMainExecutor(contexte))
        onDispose {
            runCatching { futur.get().unbindAll() }
            executeur.shutdown()
        }
    }
    Column {
        AndroidView({ vue }, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)))
    }
}
