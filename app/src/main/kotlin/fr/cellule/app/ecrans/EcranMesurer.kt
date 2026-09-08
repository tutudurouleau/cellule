@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cellule.app.ChiffreEnorme
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.mesure.CapteurAmbiance
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.core.MATIERES
import fr.cellule.core.PELLICULES
import fr.cellule.core.Photometrie
import fr.cellule.core.Posemetre
import fr.cellule.core.SENSIBILITES
import fr.cellule.core.Situations
import fr.cellule.core.ZONES
import fr.cellule.core.Zones
import fr.cellule.core.conseilLatitude
import fr.cellule.core.coupleConseille
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.libelleOuverture
import kotlin.math.min

enum class ModeMesure(val libelle: String) {
    SPOT("Spot"), MOYENNE("Moyenne"), INCIDENT("Incident")
}

private data class Memoire(val etiquette: String, val ev: Double)

/** Ce qu'on affiche, et d'où ça vient — la provenance compte autant que le chiffre. */
private data class Lecture(
    val ev: Double?,
    val provenance: String,
    val etat: String = "",
    val alerte: Boolean = false
)

/**
 * Le posemètre : un viseur plein écran, un panneau d'information qui flotte
 * par-dessus et se replie. Comme dans un appareil photo, le cadre prime — les
 * détails ne s'affichent qu'à la demande.
 */
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
        PreviewView(contexte).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            /* SurfaceView (le mode par défaut) compose sur une couche matérielle à
               part : selon les appareils, elle peut passer devant les contrôles
               placés au-dessus dans l'arbre Compose dès que la hiérarchie change de
               forme. TextureView se dessine normalement, dans l'ordre habituel. */
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
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

    val exposition = moteur.exposition
    val spot = moteur.spot
    val incidente = capteur.lecture
    val pellicule = PELLICULES.firstOrNull { it.nom == reglages.pellicule } ?: PELLICULES[1]

    /* ── La lecture, et pourquoi elle vaut ce qu'elle vaut ─────────────── */
    val lecture: Lecture = when {
        moteur.erreur != null && avecCamera -> Lecture(null, "—", moteur.erreur!!, true)

        mode == ModeMesure.INCIDENT -> when {
            !capteur.disponible ->
                Lecture(null, "incident", "Cet appareil n'a pas de capteur de luminosité ambiante.", true)
            incidente == null ->
                Lecture(null, "incident", "En attente du capteur…")
            incidente.sature -> Lecture(
                incidente.ev100, "incident",
                "Capteur saturé : il bute à ${fmtLux(incidente.luxMax.toDouble())}. Passe en réfléchi.", true
            )
            incidente.tropFaible -> Lecture(
                incidente.ev100, "incident",
                "Trop sombre pour ce capteur : la quantification dépasse la mesure.", true
            )
            else -> Lecture(incidente.ev100, "incident")
        }

        exposition == null ->
            Lecture(null, mode.libelle.lowercase(), if (autorisee) "En attente de la caméra…" else "")

        mode == ModeMesure.MOYENNE -> Lecture(
            Posemetre.ev100Moyen(exposition, reglages.etalonnage), "moyenne du cadre",
            if (!moteur.aeStable && !moteur.gele) "L'exposition se cale…" else ""
        )

        else -> {
            val brut =
                if (spot != null) Posemetre.ev100Spot(exposition, spot, reglages.etalonnage)
                else Double.NaN
            when {
                brut.isNaN() -> Lecture(
                    Posemetre.ev100Moyen(exposition, reglages.etalonnage),
                    "moyenne — spot indisponible",
                    "Le disque ne rend rien d'exploitable : c'est la moyenne du cadre qui s'affiche.", true
                )
                spot != null && !spot.fiable -> Lecture(
                    brut, "spot",
                    "Disque cramé ou bouché — élargis-le ou vise ailleurs.", true
                )
                !moteur.aeStable && !moteur.gele -> Lecture(brut, "spot", "L'exposition se cale…")
                else -> Lecture(brut, "spot")
            }
        }
    }

    val ev100 = lecture.ev
    var evRetenu by remember { mutableStateOf<Double?>(null) }
    var memoires by remember { mutableStateOf(listOf<Memoire>()) }
    var etalonnageOuvert by remember { mutableStateOf(false) }
    var diagnosticVisible by remember { mutableStateOf(false) }
    var detailsOuverts by remember { mutableStateOf(false) }

    val ancre = evRetenu ?: ev100
    val evExposition = evRetenu ?: ev100
    val vitesseVoulue =
        if (reglages.vitesseReference > 0) reglages.vitesseReference else 1.0 / reglages.iso
    val couple = evExposition?.let {
        coupleConseille(it + Photometrie.decalageIso(reglages.iso), vitesseVoulue)
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {

        /* ── Mode, en haut ────────────────────────────────────────────── */
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Gouttiere, vertical = 10.dp)
        ) {
            ChoixSegmente(
                options = ModeMesure.entries.toList(),
                selection = mode,
                libelle = { it.libelle },
                surChoix = { mode = it }
            )
        }

        /* ── Le viseur ──────────────────────────────────────────────────
           Il occupe tout ce qu'il reste une fois le sélecteur du haut et
           le panneau du bas posés : jamais recouvert, jamais chevauché.
           Le cadre réellement mesuré garde exactement les proportions du
           capteur (moteur.rapportApercu), ce qui garantit que le point
           touché tombe sur le pixel visé. */
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            if (avecCamera && autorisee) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    /* Surtout pas de fillMaxSize() avant aspectRatio() : il fige la
                       hauteur autant que la largeur, aucune taille ne peut alors
                       satisfaire à la fois les contraintes et le ratio, et Compose
                       retombe sur « largeur maximale, hauteur déduite » — sans
                       plafonner cette hauteur. Tant que le viseur occupait tout
                       l'écran la hauteur déduite y tenait ; depuis qu'il le partage,
                       elle le dépassait et l'image débordait par-dessus le sélecteur.
                       Sans fillMaxSize, les contraintes restent lâches et aspectRatio
                       choisit de lui-même le côté limitant. */
                    Box(Modifier.aspectRatio(moteur.rapportApercu)) {
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
                                val centre = Offset(moteur.cibleU * size.width, moteur.cibleV * size.height)
                                val r = moteur.rayon * min(size.width, size.height)
                                drawCircle(Color.Black.copy(alpha = 0.55f), r + 1.5f, centre, style = Stroke(width = 5f))
                                drawCircle(teinte, r, centre, style = Stroke(width = 2.5f))
                                drawCircle(teinte, 2.5f, centre)
                            }
                        }
                    }
                }
                if (mode == ModeMesure.SPOT) {
                    Row(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = Gouttiere),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = moteur.rayon,
                            onValueChange = { moteur.reglerRayon(it) },
                            valueRange = 0.02f..0.25f,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            } else if (!avecCamera) {
                /* Mode incident : pas de flux caméra, un fond qui évoque le
                   capteur d'ambiance plutôt qu'un écran mort. */
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    Color.Black
                                )
                            )
                        )
                )
            }

            /* ── Demande d'accès, si nécessaire ───────────────────────── */
            if (avecCamera && !autorisee) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(Gouttiere),
                    verticalArrangement = Arrangement.Center
                ) {
                    Carte(
                        titre = "Accès à la caméra",
                        sousTitre = "La mesure réfléchie lit les métadonnées d'exposition : temps de pose, sensibilité, ouverture. Aucune image n'est enregistrée ni transmise."
                    ) {
                        BoutonPlat("Autoriser la caméra", accent = true) {
                            demandeur.launch(Manifest.permission.CAMERA)
                        }
                    }
                }
            }
        }

        /* ── Le panneau flottant du bas ───────────────────────────────── */
        PanneauFlottant(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 92.dp)
                .animateContentSize()
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 68.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (ev100 != null) fmt(ev100) else "––,–",
                            style = ChiffreEnorme,
                            color = if (ev100 != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            " EV",
                            style = Corps,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 9.dp)
                        )
                    }
                    Text(
                        lecture.provenance,
                        style = Detail,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        couple?.let { libelleOuverture(it.ouverture) } ?: "f/—",
                        style = ChiffreGrand,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        couple?.vitesse?.libelle ?: "—",
                        style = ChiffreGrand,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${reglages.iso} ISO",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BandeauEtat(lecture.etat, lecture.alerte)
            Spacer(Modifier.height(4.dp))

            LigneBoutons {
                if (avecCamera) {
                    BoutonPlat(if (moteur.gele) "Libérer" else "Figer", accent = moteur.gele) {
                        moteur.basculerGel()
                    }
                }
                if (ev100 != null) {
                    BoutonPlat("Au carnet", accent = true, modifier = Modifier.weight(1f)) {
                        etat.derniereMesure = ev100
                        etat.dernierReglage = couple?.let {
                            "${libelleOuverture(it.ouverture)} · ${it.vitesse.libelle}"
                        } ?: ""
                    }
                }
                BoutonPlat(if (detailsOuverts) "Replier" else "Détails") {
                    detailsOuverts = !detailsOuverts
                }
            }

            AnimatedVisibility(
                visible = detailsOuverts,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = Interligne)
                ) {
                    Separateur()

                    /* Ligne fixes : ce que doit dire l'appareil. */
                    Ligne(
                        "Niveau",
                        ev100?.let { fmtLux(Photometrie.lux(it)) } ?: "—",
                        ev100?.let { Situations.nom(it) } ?: " "
                    )
                    Ligne(
                        "Diaph exact",
                        couple?.let { libelleOuverture(it.ouvertureExacte) } ?: "—",
                        couple?.let { "l'arrondi au diaph plein coûte ${libelleDiaphs(it.ecartDiaphs)}" } ?: " "
                    )
                    Ligne(
                        "L'appareil expose à",
                        exposition?.let { "${libelleOuverture(it.ouverture)} · ${it.iso} ISO" } ?: "—",
                        exposition?.let {
                            fmt(it.tempsPoseSec * 1000, 2) + " ms" +
                                if (reglages.etalonnage != 0.0) "  ·  étalonnage ${signe(reglages.etalonnage, 2)}" else ""
                        } ?: " "
                    )

                    if (avecCamera) {
                        Separateur()
                        Etiquette("Pellicule")
                        Spacer(Modifier.height(8.dp))
                        LigneBoutons {
                            Deroulant("Chargée", PELLICULES, pellicule, { it.nom }, Modifier.weight(1.6f)) {
                                reglages.pellicule = it.nom
                                reglages.iso = it.iso
                            }
                            Deroulant("Exposée à", SENSIBILITES, reglages.iso, { "$it ISO" }, Modifier.weight(1f)) {
                                reglages.iso = it
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Deroulant(
                            "Vitesse de référence",
                            listOf(0.0, 1 / 1000.0, 1 / 500.0, 1 / 250.0, 1 / 125.0, 1 / 60.0, 1 / 50.0, 1 / 30.0),
                            reglages.vitesseReference,
                            {
                                when (it) {
                                    0.0 -> "1/ISO"
                                    1 / 50.0 -> "1/50 ciné"
                                    else -> "1/" + Math.round(1 / it)
                                }
                            },
                            Modifier.fillMaxWidth()
                        ) { reglages.vitesseReference = it }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            conseilLatitude(pellicule.type),
                            style = Detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BoutonPlat("Étalonner", modifier = Modifier.padding(top = 10.dp)) {
                            etalonnageOuvert = true
                        }
                    }

                    if (ev100 != null) {
                        Separateur()
                        Etiquette("Zones")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Place l'ombre la plus sombre où tu veux encore de la matière en zone III.",
                            style = Detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        if (ancre != null) {
                            val zoneCourante = 5 + (ev100 - ancre)
                            val z = Zones.zoneLaPlusProche(zoneCourante)
                            Ligne("Lecture en cours", "zone ${z.chiffre}", z.rendu, accent = true)
                        }
                        if (evRetenu != null) {
                            Ligne("Exposition retenue", "EV " + fmt(evRetenu!!), "tout s'y rapporte")
                        }
                        Spacer(Modifier.height(8.dp))
                        LigneBoutons {
                            Deroulant("Placer en", ZONES, ZONES[5], { "zone " + it.chiffre }, Modifier.weight(1f)) {
                                evRetenu = Zones.expositionPourPlacer(ev100, it)
                            }
                            if (evRetenu != null) BoutonPlat("Libérer") { evRetenu = null }
                        }
                        Spacer(Modifier.height(8.dp))
                        LigneBoutons {
                            BoutonPlat("Mémoriser", modifier = Modifier.weight(1f)) {
                                memoires = memoires + Memoire("Lecture ${memoires.size + 1}", ev100)
                            }
                            if (memoires.isNotEmpty()) BoutonPlat("Effacer") { memoires = emptyList() }
                        }
                        memoires.forEach { m ->
                            val z = if (ancre != null) 5 + (m.ev - ancre) else Double.NaN
                            Ligne(
                                m.etiquette, "EV " + fmt(m.ev),
                                if (z.isNaN()) null else "zone ${Zones.zoneLaPlusProche(z).chiffre}"
                            )
                        }
                        if (memoires.size >= 2) {
                            val amplitude = memoires.maxOf { it.ev } - memoires.minOf { it.ev }
                            Text(
                                "Amplitude : ${fmt(amplitude)} diaphs. " + when {
                                    amplitude <= 5 -> "Tout tient, même en inversible."
                                    amplitude <= 9 -> "Le négatif encaisse sans broncher."
                                    else -> "Au-delà de la plage du support : tu choisis ce que tu sacrifies."
                                },
                                style = Detail,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (avecCamera && autorisee) {
                        Separateur()
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Etiquette("Diagnostic")
                            BoutonPlat(if (diagnosticVisible) "Masquer" else "Afficher") {
                                diagnosticVisible = !diagnosticVisible
                            }
                        }
                        if (diagnosticVisible) {
                            Spacer(Modifier.height(8.dp))
                            val d = moteur.diagnostic
                            if (d == null) {
                                Text("Aucune image analysée pour l'instant.", style = Detail)
                            } else {
                                Ligne("Image analysée", "${d.largeur} × ${d.hauteur}", "rotation ${d.rotation}°")
                                Ligne("Pas de ligne / pixel", "${d.pasDeLigne} / ${d.pasDePixel}", "${d.octets} octets reçus")
                                Ligne(
                                    "Visée à l'écran",
                                    "${fmt(moteur.cibleU.toDouble(), 2)} · ${fmt(moteur.cibleV.toDouble(), 2)}",
                                    "sur le capteur : ${fmt(d.cibleCapteurX.toDouble(), 2)} · ${fmt(d.cibleCapteurY.toDouble(), 2)}"
                                )
                                Ligne(
                                    "Échantillons du disque", "${d.echantillons}",
                                    spot?.let {
                                        "moyenne linéaire ${fmt(it.moyenneLineaire, 4)} · " +
                                            "cramés ${Math.round(it.fractionCramee * 100)} % · " +
                                            "bouchés ${Math.round(it.fractionBouchee * 100)} %"
                                    }
                                )
                                Ligne("Proportions de l'aperçu", fmt(moteur.rapportApercu.toDouble(), 3))
                                Ligne("Exposition automatique", if (moteur.aeStable) "calée" else "en recherche")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    /* ── Étalonnage, en feuille plutôt qu'en boîte de dialogue ─────────── */
    if (etalonnageOuvert) {
        var matiere by remember { mutableStateOf(MATIERES.first { it.reflectance == 0.18 }) }
        var reference by remember { mutableStateOf("") }
        FeuilleEtalonnage(
            matiere = matiere,
            surMatiere = { matiere = it },
            reference = reference,
            surReference = { reference = it },
            etalonnageActuel = reglages.etalonnage,
            plageVideo = reglages.plageVideo,
            surPlageVideo = { reglages.plageVideo = it },
            peutEnregistrer = exposition != null && spot != null,
            onEnregistrer = {
                val cible = reference.replace(',', '.').toDoubleOrNull()
                val e = exposition
                val s = spot
                if (cible != null && e != null && s != null) {
                    reglages.etalonnage = Posemetre.etalonnageDepuis(e, s, cible, matiere.reflectance)
                }
                etalonnageOuvert = false
            },
            onReinitialiser = { reglages.etalonnage = 0.0; etalonnageOuvert = false },
            onFermer = { etalonnageOuvert = false }
        )
    }
}

@Composable
private fun FeuilleEtalonnage(
    matiere: fr.cellule.core.Matiere,
    surMatiere: (fr.cellule.core.Matiere) -> Unit,
    reference: String,
    surReference: (String) -> Unit,
    etalonnageActuel: Double,
    plageVideo: Boolean,
    surPlageVideo: (Boolean) -> Unit,
    peutEnregistrer: Boolean,
    onEnregistrer: () -> Unit,
    onReinitialiser: () -> Unit,
    onFermer: () -> Unit
) {
    val etat = androidx.compose.material3.rememberModalBottomSheetState()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onFermer,
        sheetState = etat,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)) {
            Text("Étalonner", style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            Text(
                "Vise une surface dont tu connais la nature et indique ce qu'annonce une cellule en qui tu as confiance. Le décalage sera retiré de toutes les lectures.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne + 2.dp))
            Deroulant("Matière visée", MATIERES, matiere, { it.nom }, Modifier.fillMaxWidth()) { surMatiere(it) }
            Spacer(Modifier.height(Interligne))
            Champ(reference, "EV₁₀₀ de référence", Modifier.fillMaxWidth()) { surReference(it) }
            if (etalonnageActuel != 0.0) {
                Spacer(Modifier.height(Interligne))
                Text(
                    "Étalonnage actuel : ${signe(etalonnageActuel, 2)} diaph.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Separateur()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = plageVideo, onCheckedChange = surPlageVideo)
                Text(
                    "Plan de luminance en plage vidéo (16-235)",
                    style = Detail,
                    modifier = Modifier.padding(start = Interligne)
                )
            }
            Text(
                "Si la charte grise lit toujours à côté d'un demi-diaph environ, essaie l'autre plage avant de rattraper à l'étalonnage.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne + 4.dp))
            LigneBoutons {
                BoutonPlat("Réinitialiser", modifier = Modifier.weight(1f), surClic = onReinitialiser)
                BoutonPlat("Enregistrer", accent = true, actif = peutEnregistrer, modifier = Modifier.weight(1f), surClic = onEnregistrer)
            }
            Spacer(Modifier.height(Gouttiere))
        }
    }
}
