package fr.cellule.app.ecrans

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cellule.app.EtatApplication
import fr.cellule.app.Reglages
import fr.cellule.app.mesure.CapteurAmbiance
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.core.MATIERES
import fr.cellule.core.Photometrie
import fr.cellule.core.Posemetre
import fr.cellule.core.SENSIBILITES
import fr.cellule.core.Situations
import fr.cellule.core.ZONES
import fr.cellule.core.Zones
import fr.cellule.core.coupleConseille
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.libelleOuverture
import kotlin.math.min

enum class ModeMesure(val libelle: String) {
    SPOT("Spot"), MOYENNE("Moyenne"), INCIDENT("Incident")
}

private data class Memoire(val etiquette: String, val ev: Double)

@Composable
fun EcranMesurer(reglages: Reglages, etat: EtatApplication) {

    val contexte = LocalContext.current
    val proprietaire = LocalLifecycleOwner.current

    var mode by remember { mutableStateOf(ModeMesure.SPOT) }
    var autorisee by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val demandeur = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accorde -> autorisee = accorde }

    val moteur = remember { MoteurCamera(contexte) }
    val capteur = remember { CapteurAmbiance(contexte) }
    val vue = remember {
        PreviewView(contexte).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }

    SideEffect { moteur.plageVideo = reglages.plageVideo }
    val avecCamera = mode != ModeMesure.INCIDENT

    DisposableEffect(autorisee, avecCamera) {
        if (autorisee && avecCamera) moteur.demarrer(proprietaire, vue)
        onDispose { moteur.arreter() }
    }
    DisposableEffect(mode) {
        if (mode == ModeMesure.INCIDENT) capteur.demarrer()
        onDispose { capteur.arreter() }
    }

    /* ── La mesure ───────────────────────────────────────────────────── */
    val exposition = moteur.exposition
    val spot = moteur.spot
    val incidente = capteur.lecture

    val ev100: Double? = when (mode) {
        ModeMesure.SPOT ->
            if (exposition != null && spot != null)
                Posemetre.ev100Spot(exposition, spot, reglages.etalonnage).takeIf { !it.isNaN() }
            else null
        ModeMesure.MOYENNE ->
            exposition?.let { Posemetre.ev100Moyen(it, reglages.etalonnage) }
        ModeMesure.INCIDENT -> incidente?.ev100
    }

    var evRetenu by remember { mutableStateOf<Double?>(null) }
    var memoires by remember { mutableStateOf(listOf<Memoire>()) }
    var etalonnageOuvert by remember { mutableStateOf(false) }

    val ancre = evRetenu ?: ev100

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {

        Carte {
            ChoixSegmente(
                options = ModeMesure.entries.toList(),
                selection = mode,
                libelle = { it.libelle },
                surChoix = { mode = it }
            )
            Text(
                when (mode) {
                    ModeMesure.SPOT ->
                        "Mesure réfléchie du disque visé. Comme tout spotmètre elle suppose un sujet à 18 % : sur la neige elle lira 2⅓ diaphs de trop."
                    ModeMesure.MOYENNE ->
                        "Mesure réfléchie de tout le cadre, telle que l'appareil l'a décidée. Bonne sur une scène ordinaire, piégeuse dès qu'un ciel occupe le tiers de l'image."
                    ModeMesure.INCIDENT ->
                        "Capteur de luminosité ambiante : la seule mesure indépendante de la matière visée. Tourne l'écran vers la source, comme un dôme de cellule."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        /* ── Aperçu et visée ─────────────────────────────────────────── */
        if (avecCamera) {
            if (!autorisee) {
                Carte(titre = "Accès à la caméra") {
                    Text(
                        "La mesure réfléchie lit les métadonnées d'exposition de la caméra : temps de pose, sensibilité, ouverture. Rien n'est enregistré ni transmis.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { demandeur.launch(Manifest.permission.CAMERA) }) {
                        Text("Autoriser")
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .aspectRatio(3f / 4f)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                ) {
                    AndroidView(
                        factory = { vue },
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { position: Offset ->
                                    moteur.viser(
                                        position.x / size.width.toFloat(),
                                        position.y / size.height.toFloat()
                                    )
                                }
                            }
                    )
                    if (mode == ModeMesure.SPOT) {
                        val teinte = MaterialTheme.colorScheme.primary
                        Canvas(Modifier.fillMaxSize()) {
                            val centre = Offset(
                                moteur.cibleU * size.width,
                                moteur.cibleV * size.height
                            )
                            val r = moteur.rayon * min(size.width, size.height)
                            drawCircle(Color.Black.copy(alpha = 0.6f), r + 2f, centre, style = Stroke(width = 5f))
                            drawCircle(teinte, r, centre, style = Stroke(width = 2.5f))
                            drawCircle(teinte, 2.5f, centre)
                        }
                    }
                }
                if (mode == ModeMesure.SPOT) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Taille du spot", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = moteur.rayon,
                            onValueChange = { moteur.reglerRayon(it) },
                            valueRange = 0.02f..0.25f,
                            modifier = Modifier.weight(1f).padding(start = 10.dp)
                        )
                    }
                }
            }
        }

        /* ── La lecture ──────────────────────────────────────────────── */
        Carte {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (ev100 != null) fmt(ev100) else "—",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "  EV₁₀₀",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (ev100 != null) {
                Text(
                    Situations.nom(ev100),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    fmtLux(Photometrie.lux(ev100)) + " incident · " +
                        fmtCdm2(Photometrie.luminance(Photometrie.lux(ev100), Photometrie.GRIS)) +
                        " sur un gris 18 %",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            moteur.erreur?.let { Alerte(it) }
            if (avecCamera && autorisee && exposition == null && moteur.erreur == null) {
                Alerte("En attente des métadonnées d'exposition…")
            }
            if (avecCamera && exposition != null && !moteur.aeStable && !moteur.gele) {
                Alerte("L'exposition automatique cherche encore — la lecture n'est pas stable. Attends, ou fige.")
            }
            if (mode == ModeMesure.SPOT && spot != null && !spot.fiable) {
                Alerte("Le disque visé est cramé ou bouché : la mesure ne veut plus rien dire. Vise ailleurs, ou élargis le spot.")
            }
            if (mode == ModeMesure.INCIDENT) {
                if (!capteur.disponible) {
                    Alerte("Cet appareil n'a pas de capteur de luminosité ambiante.")
                } else {
                    incidente?.let {
                        if (it.sature) Alerte("Capteur saturé : il bute à ${fmtLux(it.luxMax.toDouble())}. En plein soleil, passe en mesure réfléchie.")
                        else if (it.tropFaible) Alerte("Trop sombre pour ce capteur : la quantification est plus grossière que la mesure.")
                    }
                }
            }

            exposition?.let {
                Text(
                    "L'appareil expose à ${libelleOuverture(it.ouverture)} · " +
                        fmt(it.tempsPoseSec * 1000, 2) + " ms · " + it.iso + " ISO" +
                        if (reglages.etalonnage != 0.0) "   (étalonnage ${signe(reglages.etalonnage, 2)})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (avecCamera) {
                    OutlinedButton(onClick = { moteur.basculerGel() }) {
                        Text(if (moteur.gele) "Libérer" else "Figer")
                    }
                    OutlinedButton(
                        onClick = { etalonnageOuvert = true },
                        enabled = exposition != null && spot != null
                    ) { Text("Étalonner") }
                }
                if (ev100 != null) {
                    OutlinedButton(onClick = { etat.derniereMesure = ev100 }) { Text("Au journal") }
                }
            }
        }

        /* ── Le réglage à reporter sur le boîtier ────────────────────── */
        if (ev100 != null) {
            val evExposition = evRetenu ?: ev100
            val evAppareil = evExposition + Photometrie.decalageIso(reglages.iso)
            val vitesseVoulue =
                if (reglages.vitesseReference > 0) reglages.vitesseReference else 1.0 / reglages.iso
            val couple = coupleConseille(evAppareil, vitesseVoulue)

            Carte(titre = "À reporter sur le boîtier") {
                if (couple != null) {
                    Text(
                        "${libelleOuverture(couple.ouverture)} · ${couple.vitesse.libelle}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Exact ${libelleOuverture(couple.ouvertureExacte)} — l'arrondi au diaph plein coûte ${libelleDiaphs(couple.ecartDiaphs)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (couple.vitesse.secondes >= 1.0) {
                        Text(
                            "Au-delà d'une seconde, le défaut de réciprocité du film mange encore ⅓ à 1 diaph.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text("Aucun couple standard ne couvre cette lumière à ${reglages.iso} ISO.")
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Deroulant(
                        intitule = "",
                        options = SENSIBILITES,
                        selection = reglages.iso,
                        libelle = { "$it ISO" },
                        surChoix = { reglages.iso = it }
                    )
                    Deroulant(
                        intitule = "",
                        options = listOf(0.0, 1 / 1000.0, 1 / 500.0, 1 / 250.0, 1 / 125.0, 1 / 60.0, 1 / 50.0, 1 / 30.0),
                        selection = reglages.vitesseReference,
                        libelle = {
                            when (it) {
                                0.0 -> "1/ISO"
                                1 / 50.0 -> "1/50 ciné"
                                else -> "1/" + Math.round(1 / it)
                            }
                        },
                        surChoix = { reglages.vitesseReference = it }
                    )
                }
            }

            /* ── Placement en zone ───────────────────────────────────── */
            Carte(
                titre = "Zones",
                sousTitre = "Place ton ombre la plus sombre où tu veux encore de la matière en zone III, et le reste tombe où il tombe."
            ) {
                if (ancre != null) {
                    val zoneCourante = 5 + (ev100 - ancre)
                    Ligne(
                        "Lecture en cours",
                        "zone " + Zones.zoneLaPlusProche(zoneCourante).chiffre + " (" + fmt(zoneCourante) + ")",
                        Zones.zoneLaPlusProche(zoneCourante).rendu,
                        accent = true
                    )
                }
                if (evRetenu != null) {
                    Ligne("Exposition retenue", "EV " + fmt(evRetenu!!), "le reste s'y rapporte")
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Deroulant(
                        intitule = "Placer en",
                        options = ZONES,
                        selection = ZONES[5],
                        libelle = { "zone " + it.chiffre },
                        surChoix = { evRetenu = Zones.expositionPourPlacer(ev100, it) }
                    )
                    if (evRetenu != null) {
                        OutlinedButton(onClick = { evRetenu = null }) { Text("Libérer") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        memoires = memoires + Memoire("Lecture ${memoires.size + 1}", ev100)
                    }) { Text("Mémoriser") }
                    if (memoires.isNotEmpty()) {
                        TextButton(onClick = { memoires = emptyList() }) { Text("Effacer") }
                    }
                }
                memoires.forEach { m ->
                    val z = if (ancre != null) 5 + (m.ev - ancre) else Double.NaN
                    Ligne(
                        m.etiquette,
                        "EV " + fmt(m.ev),
                        if (z.isNaN()) null else "zone " + Zones.zoneLaPlusProche(z).chiffre + " · " + fmt(z)
                    )
                }
                if (memoires.size >= 2) {
                    val amplitude = (memoires.maxOf { it.ev }) - (memoires.minOf { it.ev })
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Amplitude de la scène : ${fmt(amplitude)} diaphs. " +
                            when {
                                amplitude <= 5 -> "Tout tient, même en inversible."
                                amplitude <= 9 -> "Le négatif encaisse sans broncher."
                                else -> "Au-delà de la plage du support : tu ne rates pas l'exposition, tu choisis ce que tu sacrifies."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    /* ── Étalonnage ──────────────────────────────────────────────────── */
    if (etalonnageOuvert) {
        var matiere by remember { mutableStateOf(MATIERES.first { it.reflectance == 0.18 }) }
        var reference by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { etalonnageOuvert = false },
            title = { Text("Étalonner la caméra") },
            text = {
                Column {
                    Text(
                        "Vise une surface dont tu connais la nature, et indique ce qu'annonce une cellule en qui tu as confiance. Le décalage sera mémorisé et retiré de toutes les lectures.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Deroulant(
                        intitule = "Matière",
                        options = MATIERES,
                        selection = matiere,
                        libelle = { it.nom },
                        surChoix = { matiere = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("EV₁₀₀ de référence") },
                        singleLine = true
                    )
                    if (reglages.etalonnage != 0.0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Étalonnage actuel : ${signe(reglages.etalonnage, 2)} diaph.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = reglages.plageVideo,
                            onCheckedChange = { reglages.plageVideo = it }
                        )
                        Text(
                            "Plan de luminance en plage vidéo (16-235)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        "Si la charte grise lit toujours à côté d'un demi-diaph environ, essaie l'autre plage avant de rattraper à l'étalonnage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = exposition != null && spot != null,
                    onClick = {
                        val cible = reference.replace(',', '.').toDoubleOrNull()
                        val e = exposition
                        val s = spot
                        if (cible != null && e != null && s != null) {
                            reglages.etalonnage = Posemetre.etalonnageDepuis(e, s, cible, matiere.reflectance)
                        }
                        etalonnageOuvert = false
                    }
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { reglages.etalonnage = 0.0; etalonnageOuvert = false }) {
                        Text("Réinitialiser")
                    }
                    TextButton(onClick = { etalonnageOuvert = false }) { Text("Annuler") }
                }
            }
        )
    }
}
