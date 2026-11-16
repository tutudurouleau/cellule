package fr.cellule.app.ecrans

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.app.mesure.Localisation
import fr.cellule.app.mesure.TuilesOsm
import fr.cellule.app.mesure.reseauDisponible
import fr.cellule.core.LIEUX
import fr.cellule.core.LieuPlan
import fr.cellule.core.LieuRecent
import fr.cellule.core.Lieux
import fr.cellule.core.Tuiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

/*
 * Choisir le lieu du calcul solaire : une ville de la liste, un lieu récent,
 * sa position, des coordonnées tapées — et, s'il y a du réseau, un point sur
 * une carte OpenStreetMap. Hors connexion, la carte se contente des tuiles
 * déjà vues.
 */

private fun lireRecents(texte: String): List<LieuRecent> = try {
    val a = JSONArray(texte)
    (0 until a.length()).mapNotNull { i ->
        a.optJSONObject(i)?.let { LieuRecent(it.optString("nom"), it.optDouble("lat"), it.optDouble("lon")) }
    }
} catch (e: Exception) {
    emptyList()
}

private fun ecrireRecents(lieux: List<LieuRecent>): String =
    JSONArray(lieux.map { JSONObject().put("nom", it.nom).put("lat", it.latitude).put("lon", it.longitude) }).toString()

/** Retient le lieu choisi pour le calcul, et l'ajoute aux récents. */
fun choisirLieu(reglages: Reglages, nom: String, latitude: Double, longitude: Double, recent: Boolean = true) {
    reglages.latitude = latitude
    reglages.longitude = longitude
    reglages.lieuNom = nom
    if (recent) reglages.lieuxRecents = ecrireRecents(Lieux.ajouter(lireRecents(reglages.lieuxRecents), LieuRecent(nom, latitude, longitude)))
}

/** Le nom du lieu en cours : celui retenu, sinon ses coordonnées. */
fun nomLieu(reglages: Reglages): String = reglages.lieuNom.ifBlank {
    LIEUX.firstOrNull { kotlin.math.abs(it.latitude - reglages.latitude) < 0.01 && kotlin.math.abs(it.longitude - reglages.longitude) < 0.01 }?.nom
        ?: LieuPlan(reglages.latitude, reglages.longitude).coordonnees
}

/** La ligne « lieu » d'Estimer et ses quatre façons d'en changer. */
@Composable
fun SelecteurLieu(reglages: Reglages) {
    val contexte = LocalContext.current
    val localisation = remember { Localisation(contexte) }
    var carteOuverte by remember { mutableStateOf(false) }
    var saisie by remember { mutableStateOf(false) }
    var texte by remember { mutableStateOf("") }
    var avis by remember { mutableStateOf("") }

    val recents = remember(reglages.lieuxRecents) { lireRecents(reglages.lieuxRecents) }
    val options = LIEUX.map { LieuRecent(it.nom, it.latitude, it.longitude) } +
        recents.filter { r -> LIEUX.none { it.nom == r.nom } }
    val courant = LieuRecent(nomLieu(reglages), reglages.latitude, reglages.longitude)

    val chercherPosition: () -> Unit = {
        avis = "Recherche de ta position…"
        localisation.obtenir { position ->
            if (position == null) avis = "Position introuvable : essaie dehors, ou tape des coordonnées."
            else {
                avis = ""
                choisirLieu(reglages, "Ma position", position.latitude, position.longitude)
            }
        }
    }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) chercherPosition() else avis = "Sans la localisation, choisis sur la carte ou tape des coordonnées."
    }

    Deroulant("Lieu", (listOf(courant) + options).distinctBy { it.nom }, courant, { it.nom }, Modifier.fillMaxWidth()) {
        choisirLieu(reglages, it.nom, it.latitude, it.longitude, recent = LIEUX.none { l -> l.nom == it.nom })
    }
    Spacer(Modifier.height(6.dp))
    LigneBoutons {
        BoutonPlat("Ma position", modifier = Modifier.weight(1f)) {
            if (localisation.autorisee()) chercherPosition()
            else demande.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        BoutonPlat("Carte", modifier = Modifier.weight(1f)) { carteOuverte = true }
        BoutonPlat("Coordonnées", accent = saisie, modifier = Modifier.weight(1f)) { saisie = !saisie }
    }
    if (saisie) {
        Spacer(Modifier.height(6.dp))
        Champ(texte, "Latitude, longitude", Modifier.fillMaxWidth(), indication = "43.6045, 1.4440") {
            texte = it
            Lieux.lireCoordonnees(it)?.let { (lat, lon) ->
                choisirLieu(reglages, LieuPlan(lat, lon).coordonnees, lat, lon)
            }
        }
    }
    Text(
        avis.ifBlank { LieuPlan(reglages.latitude, reglages.longitude).coordonnees },
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )

    if (carteOuverte) {
        FenetreCarte(reglages.latitude, reglages.longitude, onFermer = { carteOuverte = false }) { lat, lon ->
            choisirLieu(reglages, "Point de la carte", lat, lon)
            carteOuverte = false
        }
    }
}

/**
 * Une carte en tuiles, qu'on fait glisser du doigt ; le point visé est au
 * centre. Les tuiles se chargent hors du fil principal, une fois, et restent
 * en cache sur le téléphone.
 */
@Composable
fun FenetreCarte(latitude: Double, longitude: Double, onFermer: () -> Unit, surChoix: (Double, Double) -> Unit) {
    val contexte = LocalContext.current
    val enLigne = remember { reseauDisponible(contexte) }
    var zoom by remember { mutableIntStateOf(12) }
    /* Le centre, en pixels du monde au zoom courant. */
    var cx by remember { mutableStateOf(Tuiles.x(longitude, 12) * Tuiles.COTE) }
    var cy by remember { mutableStateOf(Tuiles.y(latitude, 12) * Tuiles.COTE) }
    var chargees by remember { mutableIntStateOf(0) }
    var taille by remember { mutableStateOf(IntOffset(0, 0)) }

    fun zoomer(delta: Int) {
        val nouveau = (zoom + delta).coerceIn(Tuiles.ZOOM_MIN, Tuiles.ZOOM_MAX)
        val f = Math.pow(2.0, (nouveau - zoom).toDouble())
        cx *= f
        cy *= f
        zoom = nouveau
    }

    /* Les tuiles visibles, demandées quand le cadre change. */
    val colonnes = if (taille.x == 0) IntRange.EMPTY
    else floor((cx - taille.x / 2.0) / Tuiles.COTE).toInt()..floor((cx + taille.x / 2.0) / Tuiles.COTE).toInt()
    val lignes = if (taille.y == 0) IntRange.EMPTY
    else floor((cy - taille.y / 2.0) / Tuiles.COTE).toInt()..floor((cy + taille.y / 2.0) / Tuiles.COTE).toInt()
    LaunchedEffect(zoom, colonnes, lignes) {
        for (tx in colonnes) for (ty in lignes) {
            val ligne = Tuiles.ligne(ty.toDouble(), zoom) ?: continue
            val colonne = Tuiles.colonne(tx.toDouble(), zoom)
            if (TuilesOsm.enMemoire(zoom, colonne, ligne) == null) {
                launch {
                    val image = withContext(Dispatchers.IO) { TuilesOsm.tuile(contexte, zoom, colonne, ligne) }
                    if (image != null) chargees++
                }
            }
        }
    }

    val teinte = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onFermer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().padding(Gouttiere),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(Modifier.padding(Gouttiere)) {
                Text("Choisir sur la carte", style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (enLigne) "Fais glisser la carte : le lieu retenu est la croix du centre."
                    else "Pas de réseau : seules les tuiles déjà vues s'affichent. Tu peux aussi taper des coordonnées.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Interligne))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clipToBounds()
                        .onSizeChanged { taille = IntOffset(it.width, it.height) }
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { changement, d ->
                                changement.consume()
                                cx -= d.x
                                cy -= d.y
                            }
                        }
                        .pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoomer(1) }) }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        /* Relire le compteur : une tuile arrivée redessine la carte. */
                        if (chargees < 0) return@Canvas
                        val gauche = cx - size.width / 2.0
                        val haut = cy - size.height / 2.0
                        for (tx in colonnes) for (ty in lignes) {
                            val ligne = Tuiles.ligne(ty.toDouble(), zoom) ?: continue
                            val image = TuilesOsm.enMemoire(zoom, Tuiles.colonne(tx.toDouble(), zoom), ligne) ?: continue
                            drawImage(
                                image.asImageBitmap(),
                                topLeft = Offset((tx * Tuiles.COTE - gauche).toFloat(), (ty * Tuiles.COTE - haut).toFloat())
                            )
                        }
                        val c = Offset(size.width / 2, size.height / 2)
                        drawCircle(Color.White, 11f, c, style = Stroke(5f))
                        drawCircle(teinte, 11f, c, style = Stroke(2.5f))
                        drawLine(teinte, c.copy(x = c.x - 22f), c.copy(x = c.x - 13f), 2.5f)
                        drawLine(teinte, c.copy(x = c.x + 13f), c.copy(x = c.x + 22f), 2.5f)
                        drawLine(teinte, c.copy(y = c.y - 22f), c.copy(y = c.y - 13f), 2.5f)
                        drawLine(teinte, c.copy(y = c.y + 13f), c.copy(y = c.y + 22f), 2.5f)
                    }
                    Text(
                        "© contributeurs OpenStreetMap",
                        style = Detail,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color.White.copy(alpha = 0.75f))
                            .padding(horizontal = 4.dp)
                    )
                }
                val lat = Tuiles.latitude(cy / Tuiles.COTE, zoom)
                val lon = Tuiles.longitude(cx / Tuiles.COTE, zoom)
                Text(
                    LieuPlan(lat, lon).coordonnees,
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Interligne)) {
                    BoutonPlat("−", modifier = Modifier.weight(1f)) { zoomer(-1) }
                    BoutonPlat("+", modifier = Modifier.weight(1f)) { zoomer(1) }
                }
                Spacer(Modifier.height(Interligne))
                Row(horizontalArrangement = Arrangement.spacedBy(Interligne)) {
                    BoutonPlat("Annuler", modifier = Modifier.weight(1f), surClic = onFermer)
                    BoutonPlat("Choisir ce point", accent = true, modifier = Modifier.weight(1.4f)) {
                        surChoix(lat, lon.let { ((it + 540) % 360) - 180 })
                    }
                }
            }
        }
    }
}
