@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cellule.app.CadreJson
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.DepotCinema
import fr.cellule.app.ExportPdf
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.app.mesure.Tenue
import fr.cellule.core.Boussole
import fr.cellule.core.CadrePlan
import fr.cellule.core.Cadreur
import fr.cellule.core.ChampPhoto
import fr.cellule.core.DESANAMORPHOSES
import fr.cellule.core.LieuPlan
import fr.cellule.core.Numerotation
import fr.cellule.core.Plan
import fr.cellule.core.RATIOS_CINE
import fr.cellule.core.RATIOS_PHOTO
import fr.cellule.core.RectCadre
import fr.cellule.core.Tournage
import fr.cellule.core.TraceCadre
import fr.cellule.core.nombreCourt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/* ── Les photos des plans ──────────────────────────────────────────────── */

/**
 * Les photos des plans vivent dans la galerie : on les relit à leur adresse,
 * réduites à la taille demandée et remises dans le bon sens.
 */
object PhotosPlans {

    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun rotationExif(contexte: Context, uri: Uri): Int = try {
        contexte.contentResolver.openInputStream(uri)?.use { flux ->
            when (ExifInterface(flux).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    } catch (e: Exception) {
        0
    }

    /** La photo, son grand côté ramené à [grandCote] pixels au plus ; null si elle est introuvable. */
    fun charger(contexte: Context, adresse: String, grandCote: Int): Bitmap? {
        if (adresse.isBlank()) return null
        val cle = "$adresse@$grandCote"
        cache.get(cle)?.let { return it }
        return try {
            val uri = Uri.parse(adresse)
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contexte.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bornes) }
            if (bornes.outWidth <= 0 || bornes.outHeight <= 0) return null
            var echantillon = 1
            while (maxOf(bornes.outWidth, bornes.outHeight) / (echantillon * 2) >= grandCote) echantillon *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = echantillon }
            val brut = contexte.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return null
            val degres = rotationExif(contexte, uri)
            val droit = if (degres == 0) brut else Bitmap.createBitmap(
                brut, 0, 0, brut.width, brut.height, Matrix().apply { postRotate(degres.toFloat()) }, true
            )
            cache.put(cle, droit)
            droit
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Ce que l'EXIF d'une photo de la galerie sait d'elle : date, heure, lieu,
     * proportions, et le champ si la focale équivalente y est notée.
     */
    fun planDepuisGalerie(contexte: Context, uri: Uri, base: Plan): Plan {
        var date = LocalDate.now().toString()
        var heure = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        var lieu: LieuPlan? = null
        var champ: ChampPhoto? = null
        var rapport = 4.0 / 3.0
        var camera = "photo importée de la galerie"
        try {
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contexte.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bornes) }
            val tournee = rotationExif(contexte, uri) % 180 == 90
            if (bornes.outWidth > 0 && bornes.outHeight > 0) {
                rapport = if (tournee) bornes.outHeight.toDouble() / bornes.outWidth
                else bornes.outWidth.toDouble() / bornes.outHeight
            }
            contexte.contentResolver.openInputStream(uri)?.use { flux ->
                val exif = ExifInterface(flux)
                val quand = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                /* « 2026:11:16 14:32:05 » */
                if (quand != null && quand.length >= 16) {
                    date = quand.substring(0, 10).replace(':', '-')
                    heure = quand.substring(11, 16)
                }
                val coordonnees = FloatArray(2)
                if (exif.getLatLong(coordonnees)) {
                    val altitude = exif.getAltitude(Double.NaN)
                    lieu = LieuPlan(
                        coordonnees[0].toDouble(), coordonnees[1].toDouble(),
                        altitudeM = if (altitude.isNaN()) null else altitude
                    )
                }
                val equivalent = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
                if (equivalent > 0) {
                    champ = Cadreur.champDepuisEquivalent(equivalent.toDouble(), rapport)
                    camera = "photo importée · équivalent $equivalent mm d'après son EXIF"
                }
            }
        } catch (e: Exception) {
            /* Une photo sans EXIF lisible reste importable : on n'en sait juste rien de plus. */
        }
        return base.copy(
            date = date, heure = heure, photo = uri.toString(), rapportPhoto = rapport,
            importee = true, lieu = lieu, champ = champ, cameraTelephone = camera
        )
    }
}

/** La photo d'un plan, décodée hors du fil principal ; null tant qu'elle charge ou si elle manque. */
@Composable
fun photoDuPlan(adresse: String, grandCote: Int = 1280): ImageBitmap? {
    val contexte = LocalContext.current.applicationContext
    val photo by produceState<ImageBitmap?>(null, adresse, grandCote) {
        value = withContext(Dispatchers.IO) { PhotosPlans.charger(contexte, adresse, grandCote)?.asImageBitmap() }
    }
    return photo
}

/* ── Le dessin des cadres ──────────────────────────────────────────────── */

val COULEURS_FORMATS = listOf(
    Color(0xFFF0A93F), Color(0xFF6FD3F2), Color(0xFFF07FCF), Color(0xFF9BE06B), Color(0xFFF2E26F)
)

/**
 * Trace la zone utile du capteur et les formats par-dessus une image.
 *
 * [rotation] dit comment l'image est posée sur la surface de dessin : 0 quand
 * elle y est droite (une photo affichée), 90 ou 270 quand le téléphone est
 * couché et que l'aperçu, lui, reste dans le sens de l'écran.
 */
fun DrawScope.dessinerCadres(trace: TraceCadre, rotation: Int = 0, taillePolice: Float = 28f) {
    val l = size.width
    val h = size.height
    fun surface(r: RectCadre): Rect = when (rotation) {
        90 -> Rect(((1 - r.bas) * l).toFloat(), (r.gauche * h).toFloat(), ((1 - r.haut) * l).toFloat(), (r.droite * h).toFloat())
        270 -> Rect((r.haut * l).toFloat(), ((1 - r.droite) * h).toFloat(), (r.bas * l).toFloat(), ((1 - r.gauche) * h).toFloat())
        else -> Rect((r.gauche * l).toFloat(), (r.haut * h).toFloat(), (r.droite * l).toFloat(), (r.bas * h).toFloat())
    }

    val capteur = surface(trace.capteur)
    /* Hors du capteur : assombri, pour que l'œil aille au cadre. */
    val cache = Color.Black.copy(alpha = 0.5f)
    if (capteur.top > 0f) drawRect(cache, Offset(0f, 0f), Size(l, capteur.top))
    if (capteur.bottom < h) drawRect(cache, Offset(0f, capteur.bottom), Size(l, h - capteur.bottom))
    if (capteur.left > 0f) drawRect(cache, Offset(0f, capteur.top), Size(capteur.left, capteur.height))
    if (capteur.right < l) drawRect(cache, Offset(capteur.right, capteur.top), Size(l - capteur.right, capteur.height))
    drawRect(Color.White, capteur.topLeft, capteur.size, style = Stroke(width = 2f))

    val pinceau = Paint().apply {
        isAntiAlias = true
        textSize = taillePolice
        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
    }
    trace.formats.forEachIndexed { i, format ->
        val couleur = COULEURS_FORMATS[i % COULEURS_FORMATS.size]
        val r = surface(format.rect)
        drawRect(couleur, r.topLeft, r.size, style = Stroke(width = 3f))
        /* L'étiquette part du coin haut-gauche du format, dans le sens de l'image. */
        val (x, y, angle) = when (rotation) {
            90 -> Triple(r.right, r.top, 90f)
            270 -> Triple(r.left, r.bottom, -90f)
            else -> Triple(r.left, r.top, 0f)
        }
        pinceau.color = couleur.toArgb()
        val toile = drawContext.canvas.nativeCanvas
        toile.save()
        toile.translate(x, y)
        toile.rotate(angle)
        toile.drawText(format.nom, 8f, taillePolice + 4f + i * (taillePolice + 2f), pinceau)
        toile.restore()
    }
}

/** Le cadre de travail, dessiné en direct sur l'aperçu de la caméra. */
@Composable
fun BoxScope.CadreSurApercu(moteur: MoteurCamera, cadre: CadrePlan, tenue: Tenue) {
    if (!cadre.complet) return
    val champ = moteur.champPhoto(tenue.couche) ?: return
    val trace = Cadreur.tracer(champ, cadre) ?: return
    if (trace.tropLarge) {
        Text(
            "Trop large pour ce téléphone : sur ce capteur, il ne montre rien de plus large que " +
                nombreCourt(trace.focaleMinimale, 1) + " mm. Le cadre n'est pas tracé.",
            style = Detail,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        )
    } else {
        val rotation = when (tenue) {
            Tenue.COUCHE_GAUCHE -> 90
            Tenue.COUCHE_DROITE -> 270
            Tenue.DEBOUT -> 0
        }
        Canvas(Modifier.fillMaxSize()) { dessinerCadres(trace, rotation, 11.dp.toPx()) }
    }
}

/** Le déclencheur, posé sur l'aperçu. */
@Composable
fun BoxScope.BoutonCapture(enCours: Boolean, surClic: () -> Unit) {
    Row(
        Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(50))
            .clickable(enabled = !enCours) { surClic() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)).padding(6.dp))
        Text(if (enCours) "Capture…" else "Capturer le plan", style = Corps, color = Color.White)
    }
}

/** La photo d'un plan avec ses cadres, telle qu'on la voit dans le carnet. */
@Composable
fun ImagePlan(plan: Plan, modifier: Modifier = Modifier) {
    val photo = photoDuPlan(plan.photo)
    val rapport = photo?.let { it.width.toFloat() / it.height.toFloat() } ?: plan.rapportPhoto.toFloat()
    val trace = plan.champ?.let { Cadreur.tracer(it, plan.cadre) }
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().heightIn(max = 380.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .aspectRatio(rapport.coerceIn(0.4f, 3f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = plan.intitule,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    if (trace != null && !trace.tropLarge) {
                        Canvas(Modifier.fillMaxSize()) { dessinerCadres(trace, 0, 11.dp.toPx()) }
                    }
                } else {
                    Text(
                        if (plan.photo.isBlank()) "Pas de photo pour ce plan."
                        else "Photo introuvable dans la galerie de ce téléphone.",
                        style = Detail,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }
        }
        val avis = when {
            !plan.cadre.complet -> ""
            plan.champ == null -> "Le champ de cette photo est inconnu : le cadre ne peut pas y être tracé."
            trace != null && trace.tropLarge ->
                "Trop large pour ce téléphone : sur ce capteur, la photo ne montre rien de plus large que " +
                    nombreCourt(trace.focaleMinimale, 1) + " mm. Le cadre n'est pas tracé."
            trace != null ->
                "Champ de la caméra : " + nombreCourt(trace.champHorizontal, 1) + "° × " +
                    nombreCourt(trace.champVertical, 1) + "°."
            else -> ""
        }
        if (avis.isNotBlank()) {
            Text(
                avis,
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/* ── Régler un cadre ───────────────────────────────────────────────────── */

@Composable
private fun BasculesFormats(noms: List<String>, choisis: List<String>, surBascule: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        noms.forEach { nom ->
            Pastille(nom, accent = nom in choisis) { surBascule(nom) }
        }
    }
}

/**
 * Capteur (ou zone utile), focale, désanamorphose et formats. Les tailles se
 * saisissent en millimètres, telles que le fabricant les publie.
 */
@Composable
fun EditeurCadre(cadre: CadrePlan, surChangement: (CadrePlan) -> Unit) {
    var nom by remember { mutableStateOf(cadre.capteurNom) }
    var largeur by remember { mutableStateOf(cadre.capteurLargeurMm?.let { nombreCourt(it, 2) } ?: "") }
    var hauteur by remember { mutableStateOf(cadre.capteurHauteurMm?.let { nombreCourt(it, 2) } ?: "") }
    var focale by remember { mutableStateOf(cadre.focaleMm?.let { nombreCourt(it, 1) } ?: "") }

    fun pousser(desanamorphose: Double = cadre.desanamorphose, formats: List<String> = cadre.formats) {
        surChangement(
            CadrePlan(
                capteurNom = nom.trim(),
                capteurLargeurMm = Tournage.lireNombre(largeur),
                capteurHauteurMm = Tournage.lireNombre(hauteur),
                focaleMm = Tournage.lireNombre(focale),
                desanamorphose = desanamorphose,
                formats = formats,
                objectif = cadre.objectif
            )
        )
    }

    Champ(nom, "Caméra, mode de capteur", Modifier.fillMaxWidth(), indication = "nom libre") { nom = it; pousser() }
    Spacer(Modifier.height(Interligne))
    LigneBoutons {
        Champ(largeur, "Largeur utile (mm)", Modifier.weight(1f)) { largeur = it; pousser() }
        Champ(hauteur, "Hauteur utile (mm)", Modifier.weight(1f)) { hauteur = it; pousser() }
    }
    Text(
        "La taille de la zone du capteur réellement enregistrée, lue sur la fiche technique de la caméra. Cellule n'en invente aucune.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
    Spacer(Modifier.height(Interligne))
    LigneBoutons {
        Champ(focale, "Focale (mm)", Modifier.weight(1f)) { focale = it; pousser() }
        Deroulant(
            "Désanamorphose", DESANAMORPHOSES, cadre.desanamorphose,
            { if (it == 1.0) "×1 (sphérique)" else "×" + nombreCourt(it, 1) }, Modifier.weight(1f)
        ) { pousser(desanamorphose = it) }
    }
    Spacer(Modifier.height(Interligne))
    Etiquette("Formats à tracer")
    Spacer(Modifier.height(6.dp))
    val bascule: (String) -> Unit = { n ->
        pousser(formats = if (n in cadre.formats) cadre.formats - n else cadre.formats + n)
    }
    BasculesFormats(RATIOS_CINE.map { it.nom }, cadre.formats, bascule)
    Spacer(Modifier.height(6.dp))
    BasculesFormats(RATIOS_PHOTO.map { it.nom }, cadre.formats, bascule)
    Text(
        "Plusieurs formats se superposent, chacun dans sa couleur, à l'intérieur de la zone utile (désanamorphosée).",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/** Le cadre de travail : celui qu'on voit sur l'aperçu et que reçoivent les nouveaux plans. */
@Composable
fun FeuilleCadreDeTravail(reglages: Reglages, onFermer: () -> Unit) {
    val cadre = remember(reglages.cineCadre) { CadreJson.lireTexte(reglages.cineCadre) }
    ModalBottomSheet(
        onDismissRequest = onFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)
        ) {
            Text("Cadre de travail", style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            Text(
                "Le cadre de ta caméra, tracé sur l'aperçu du téléphone et noté avec chaque plan capturé. Il reste modifiable plan par plan.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne + 2.dp))
            EditeurCadre(cadre) { reglages.cineCadre = CadreJson.ecrire(it).toString() }
            Spacer(Modifier.height(Interligne + 4.dp))
            Etiquette("Numérotation des nouveaux plans")
            Spacer(Modifier.height(6.dp))
            ChoixSegmente(
                Numerotation.entries.toList(),
                runCatching { Numerotation.valueOf(reglages.cineNumerotation) }.getOrDefault(Numerotation.SEQUENCE),
                { it.libelle }
            ) { reglages.cineNumerotation = it.name }
            Spacer(Modifier.height(Interligne + 4.dp))
            BoutonPlat("Fermer", accent = true, modifier = Modifier.fillMaxWidth(), surClic = onFermer)
            Spacer(Modifier.height(Gouttiere))
        }
    }
}

/* ── L'export en PDF ───────────────────────────────────────────────────── */

/**
 * Fabrique un PDF hors du fil principal, puis ouvre le partage d'Android.
 * Tout se passe sur le téléphone ; le réseau n'ajoute que la mini-carte.
 */
class ExporteurPdf(
    private val contexte: Context,
    private val portee: CoroutineScope,
    private val cinema: DepotCinema
) {
    var enCours by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var echec by mutableStateOf(false)
        private set

    fun plan(plan: Plan) = lancer { ExportPdf.planSeul(contexte, plan, cinema.donnees) }

    fun dossier(
        titre: String, sousTitre: String, plans: List<Plan>,
        jours: List<fr.cellule.core.JourCinema>, vues: List<String> = emptyList()
    ) = lancer { ExportPdf.dossier(contexte, titre, sousTitre, plans, jours, vues) }

    private fun lancer(fabrique: () -> java.io.File) {
        if (enCours) return
        enCours = true
        echec = false
        message = "Fabrication du PDF…"
        portee.launch {
            val pdf = withContext(Dispatchers.IO) { runCatching(fabrique).getOrNull() }
            enCours = false
            if (pdf == null) {
                message = "Le PDF n'a pas pu être fabriqué."
                echec = true
            } else {
                message = ""
                try {
                    ExportPdf.partager(contexte, pdf)
                } catch (e: Exception) {
                    message = "Aucune appli n'a pu recevoir le PDF."
                    echec = true
                }
            }
        }
    }
}

@Composable
fun rememberExporteurPdf(cinema: DepotCinema): ExporteurPdf {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    return remember(cinema) { ExporteurPdf(contexte, portee, cinema) }
}

/* ── La fiche d'un plan ────────────────────────────────────────────────── */

/**
 * Tout ce qu'on sait d'un plan, et ce qu'on peut en corriger après coup :
 * numérotation, titre, cadre et focale, notes. Le lieu, le cap, le soleil et
 * l'exposition sont des relevés : ils se lisent, ils ne se retouchent pas.
 */
@Composable
fun FeuillePlan(cinema: DepotCinema, identifiant: Long, avis: String = "", onFermer: () -> Unit) {
    val enregistre = cinema.plan(identifiant)
    if (enregistre == null) {
        onFermer()
        return
    }
    var brouillon by remember(identifiant) { mutableStateOf(enregistre) }
    var suppression by remember { mutableStateOf(false) }
    val exporteur = rememberExporteurPdf(cinema)

    /* Les relevés arrivent parfois après l'ouverture de la fiche (la position
       met quelques secondes) : on les reprend toujours de la version enregistrée. */
    fun ranger() {
        cinema.modifier(identifiant) { actuel ->
            brouillon.copy(lieu = actuel.lieu, soleil = actuel.soleil, cap = actuel.cap, jourId = actuel.jourId)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { ranger(); onFermer() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)
        ) {
            Text(brouillon.intitule, style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Text(
                listOf(enregistre.date, enregistre.heure).filter { it.isNotBlank() }.joinToString(" · "),
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (avis.isNotBlank()) {
                Spacer(Modifier.height(Interligne))
                BandeauEtat(avis, alerte = true)
            }
            Spacer(Modifier.height(Interligne))
            ImagePlan(brouillon.copy(photo = enregistre.photo))

            Separateur()
            Etiquette("Numérotation")
            Spacer(Modifier.height(6.dp))
            ChoixSegmente(Numerotation.entries.toList(), brouillon.numerotation, { it.libelle }) {
                brouillon = brouillon.copy(numerotation = it)
            }
            Spacer(Modifier.height(Interligne))
            if (brouillon.numerotation == Numerotation.SEQUENCE) {
                LigneBoutons {
                    Champ(brouillon.sequence, "Séq.", Modifier.weight(1f)) { brouillon = brouillon.copy(sequence = it) }
                    Champ(brouillon.plan, "Plan", Modifier.weight(1f)) { brouillon = brouillon.copy(plan = it) }
                    Champ(brouillon.prise, "Prise", Modifier.weight(1f)) { brouillon = brouillon.copy(prise = it) }
                }
            } else {
                Champ(brouillon.numero, "Numéro", Modifier.fillMaxWidth()) { brouillon = brouillon.copy(numero = it) }
            }
            Spacer(Modifier.height(Interligne))
            Champ(brouillon.titre, "Titre", Modifier.fillMaxWidth(), indication = "la cuisine, contre-jour") {
                brouillon = brouillon.copy(titre = it)
            }

            Separateur()
            Etiquette("Rangement")
            Spacer(Modifier.height(6.dp))
            val dossiers = cinema.donnees
            val jours: List<Long?> = listOf<Long?>(null) +
                dossiers.projets.flatMap { p -> dossiers.joursDe(p.identifiant).map { it.identifiant } }
            if (jours.size == 1) {
                Text(
                    "Crée un projet et un jour dans Carnet › Cinéma pour y ranger ce plan.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Deroulant(
                    "Rangé dans", jours, enregistre.jourId?.takeIf { it in jours },
                    { id ->
                        val jour = dossiers.jours.firstOrNull { it.identifiant == id }
                        if (jour == null) "Non rangé"
                        else (dossiers.projets.firstOrNull { it.identifiant == jour.projetId }?.nom ?: "Projet") +
                            " › " + jour.intitule
                    },
                    Modifier.fillMaxWidth()
                ) { cinema.deplacer(identifiant, it) }
            }

            Separateur()
            Etiquette("Cadre")
            Spacer(Modifier.height(6.dp))
            EditeurCadre(brouillon.cadre) { brouillon = brouillon.copy(cadre = it) }

            Separateur()
            Etiquette("Relevés")
            Spacer(Modifier.height(4.dp))
            val e = enregistre.exposition
            Ligne(
                "Exposition",
                e.ev100?.let { "EV " + fmt(it) } ?: "—",
                listOf(e.resume, e.filtres).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { null }
            )
            val lieu = enregistre.lieu
            if (lieu != null) {
                Ligne(
                    "Lieu",
                    "",
                    listOfNotNull(
                        lieu.adresse.takeIf { it.isNotBlank() },
                        lieu.coordonnees,
                        listOfNotNull(
                            lieu.precisionM?.let { "à ± " + nombreCourt(it, 0) + " m" },
                            lieu.altitudeM?.let { "altitude " + nombreCourt(it, 0) + " m" }
                        ).joinToString(" · ").takeIf { it.isNotBlank() },
                        lieu.lienMaps
                    ).joinToString("\n")
                )
            } else {
                Ligne(
                    "Lieu", "—",
                    if (enregistre.importee) "aucune position dans cette photo"
                    else "pas de position : localisation refusée, coupée ou encore en recherche"
                )
            }
            Ligne(
                "Boussole",
                enregistre.cap?.let { Boussole.libelle(it) } ?: "—",
                when {
                    enregistre.cap == null -> "pas de cap relevé"
                    lieu == null -> "direction de visée, nord magnétique (sans position, pas de correction)"
                    else -> "direction de visée, nord géographique"
                }
            )
            Ligne(
                "Soleil",
                "",
                enregistre.soleil?.resume?.let { "$it — calculé pour ce lieu et cette heure" }
                    ?: "se calcule dès que le lieu est connu"
            )
            if (enregistre.cameraTelephone.isNotBlank()) {
                Ligne("Caméra du téléphone", "", enregistre.cameraTelephone)
            }

            Separateur()
            Champ(
                brouillon.notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false,
                indication = "lumière, décor, accès, ce qu'il faudra apporter"
            ) { brouillon = brouillon.copy(notes = it) }

            Spacer(Modifier.height(Interligne + 4.dp))
            BoutonPlat(
                if (exporteur.enCours) "Fabrication du PDF…" else "PDF du plan",
                actif = !exporteur.enCours,
                modifier = Modifier.fillMaxWidth()
            ) {
                /* Le PDF montre le plan tel qu'on le voit ici : on range d'abord les retouches. */
                ranger()
                cinema.plan(identifiant)?.let { exporteur.plan(it) }
            }
            if (exporteur.message.isNotBlank() && !exporteur.enCours) {
                Spacer(Modifier.height(6.dp))
                BandeauEtat(exporteur.message, alerte = exporteur.echec)
            }
            Text(
                "Fabriqué sur le téléphone, sans réseau. La mini-carte n'y figure que s'il y a du réseau à ce moment-là.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                BoutonPlat(if (suppression) "Confirmer" else "Supprimer", erreur = suppression) {
                    if (suppression) {
                        cinema.supprimerPlan(identifiant)
                        onFermer()
                    } else suppression = true
                }
                BoutonPlat("Enregistrer", accent = true, modifier = Modifier.weight(1f)) {
                    ranger()
                    onFermer()
                }
            }
            if (suppression) {
                Text(
                    "Le plan sort du carnet ; la photo, elle, reste dans la galerie.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.height(Gouttiere))
        }
    }
}
