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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.cellule.app.ChiffreHero
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.core.Cadrage
import fr.cellule.core.FOCALES_CLASSIQUES
import fr.cellule.core.FocaleClassique
import fr.cellule.core.GuideCadrage
import fr.cellule.core.RATIOS_CINE
import fr.cellule.core.RATIOS_PHOTO
import fr.cellule.core.RatioCadre
import fr.cellule.core.guideDeCadrage
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Exercice(val libelle: String) {
    LIBRE("Libre"), CADRER("Cadrer"), RECONNAITRE("Reconnaître")
}

private data class OptionRatio(val libelle: String, val valeur: Double?)

private val OPTIONS_RATIO: List<OptionRatio> = buildList {
    add(OptionRatio("Capteur plein (natif)", null))
    RATIOS_PHOTO.forEach { add(OptionRatio("Photo — ${it.nom}", it.ratio)) }
    RATIOS_CINE.forEach { add(OptionRatio("Ciné — ${it.nom}", it.ratio)) }
}

/**
 * Apprendre à voir les focales — même viseur plein écran que « Mesurer ».
 *
 * Une focale ne se retient pas comme un nombre, elle se retient comme un
 * cadrage. Le viseur montre donc, emboîtés dans l'image, les rectangles que
 * prendraient les focales plus longues — et pendant les exercices, le chiffre
 * disparaît : c'est l'œil qu'on entraîne, pas la lecture.
 */
@Composable
fun EcranFocales() {

    val contexte = LocalContext.current
    val proprietaire = LocalLifecycleOwner.current

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
    val vue = remember {
        PreviewView(contexte).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }

    DisposableEffect(autorisee) {
        if (autorisee) moteur.demarrer(proprietaire, vue)
        onDispose { moteur.arreter() }
    }

    var exercice by remember { mutableStateOf(Exercice.LIBRE) }
    var cible by remember { mutableStateOf<FocaleClassique?>(null) }
    var reponse by remember { mutableStateOf<FocaleClassique?>(null) }
    var revele by remember { mutableStateOf(false) }
    var justes by remember { mutableStateOf(0) }
    var posees by remember { mutableStateOf(0) }
    var detailsOuverts by remember { mutableStateOf(false) }
    /* Source unique de la sélection affichée. Une recherche inverse par valeur
       serait ambiguë : « Photo — 16:9 » et « Ciné — 1.78:1 » partagent le même
       nombre, et la première correspondance du catalogue gagnerait toujours —
       jamais forcément celle que l'utilisateur vient de choisir. */
    var optionRatio by remember { mutableStateOf(OPTIONS_RATIO.first()) }

    val equivalence = moteur.equivalence
    val focaleActuelle = equivalence?.equivalentHorizontal
    val enExercice = exercice != Exercice.LIBRE
    /* Pendant l'exercice le chiffre se tait : sinon il n'y a rien à apprendre. */
    val chiffreVisible = !enExercice || revele

    /* Ce que le zoom de cet appareil permet réellement d'atteindre. */
    val auZoomUn = if (focaleActuelle != null && moteur.zoomDemande > 0)
        focaleActuelle / moteur.zoomDemande else null
    val atteignables = remember(auZoomUn, moteur.zoomMinimal, moteur.zoomMaximal) {
        if (auZoomUn == null) FOCALES_CLASSIQUES
        else FOCALES_CLASSIQUES.filter {
            it.mm >= auZoomUn * moteur.zoomMinimal * 0.92 &&
                it.mm <= auZoomUn * moteur.zoomMaximal * 1.08
        }.ifEmpty { FOCALES_CLASSIQUES }
    }

    fun viser(mm: Double) {
        val actuelle = focaleActuelle ?: return
        moteur.reglerZoom(
            Cadrage.zoomPour(mm, actuelle, moteur.zoomDemande.toDouble()).toFloat()
        )
    }

    fun nouvelleConsigne() {
        val tiree = atteignables.random()
        cible = tiree
        reponse = null
        revele = false
        posees += 1
        when (exercice) {
            Exercice.CADRER -> moteur.reglerZoom(moteur.zoomMinimal)
            Exercice.RECONNAITRE -> viser(tiree.mm.toDouble())
            Exercice.LIBRE -> {}
        }
    }

    fun verifier() {
        val vraie = focaleActuelle ?: return
        val visee = cible ?: return
        revele = true
        val juge = when (exercice) {
            Exercice.CADRER -> abs(Cadrage.ecartRelatif(vraie, visee.mm.toDouble())) <= 0.15
            Exercice.RECONNAITRE -> reponse?.mm == visee.mm
            Exercice.LIBRE -> false
        }
        if (juge) justes += 1
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (!autorisee) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(Gouttiere),
                verticalArrangement = Arrangement.Center
            ) {
                Carte(
                    titre = "Accès à la caméra",
                    sousTitre = "L'angle de champ se lit dans les caractéristiques de l'optique et la zone de capteur réellement exposée. Aucune image n'est enregistrée ni transmise."
                ) {
                    BoutonPlat("Autoriser la caméra", accent = true) {
                        demandeur.launch(Manifest.permission.CAMERA)
                    }
                }
            }
            return@Box
        }

        /* ── Le viseur ────────────────────────────────────────────────────
           Même précaution que sur l'écran Mesurer : le cadre garde exactement
           les proportions du capteur, seul le fond noir va bord à bord. Le
           ratio choisi, lui, se dessine par-dessus comme un cache — jamais un
           mode caméra réel, l'appareil ne peut demander que du 4:3 ou du 16:9. */
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().aspectRatio(moteur.rapportApercu)) {
                AndroidView(
                    factory = { vue },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ -> moteur.pincer(zoom) }
                        }
                )
                val teinte = MaterialTheme.colorScheme.primary
                val ratioSortie = moteur.ratioSortie
                if (focaleActuelle != null && !enExercice) {
                    Canvas(Modifier.fillMaxSize()) {
                        /* Le rectangle du ratio choisi, à l'intérieur du capteur
                           plein. Sans sélection, il couvre tout le cadre. */
                        val guide = if (ratioSortie != null)
                            guideDeCadrage(size.width / size.height.toDouble(), ratioSortie)
                        else GuideCadrage(1.0, 1.0)
                        val cadreL = (size.width * guide.largeur).toFloat()
                        val cadreH = (size.height * guide.hauteur).toFloat()
                        val cadreX = (size.width - cadreL) / 2
                        val cadreY = (size.height - cadreH) / 2

                        if (ratioSortie != null) {
                            val cache = Color.Black.copy(alpha = 0.72f)
                            if (cadreY > 0f) {
                                drawRect(cache, Offset(0f, 0f), Size(size.width, cadreY))
                                drawRect(cache, Offset(0f, cadreY + cadreH), Size(size.width, size.height - cadreY - cadreH))
                            }
                            if (cadreX > 0f) {
                                drawRect(cache, Offset(0f, 0f), Size(cadreX, size.height))
                                drawRect(cache, Offset(cadreX + cadreL, 0f), Size(size.width - cadreX - cadreL, size.height))
                            }
                            drawRect(teinte.copy(alpha = 0.9f), Offset(cadreX, cadreY), Size(cadreL, cadreH), style = Stroke(width = 1.5f))
                        }

                        /* Les quatre focales suivantes, emboîtées à l'intérieur
                           du cadre choisi : c'est cette image qu'on veut garder
                           en tête, pas le nombre. */
                        FOCALES_CLASSIQUES
                            .mapNotNull { f ->
                                Cadrage.fractionDuCadre(focaleActuelle, f.mm.toDouble())
                                    ?.let { fr -> f to fr }
                            }
                            .filter { it.second < 0.94 }
                            .take(4)
                            .forEach { (_, fraction) ->
                                val l = cadreL * fraction
                                val h = cadreH * fraction
                                drawRect(
                                    color = teinte.copy(alpha = 0.75f),
                                    topLeft = Offset(cadreX + (cadreL - l) / 2, cadreY + (cadreH - h) / 2),
                                    size = Size(l, h),
                                    style = Stroke(width = 2f)
                                )
                            }
                    }
                } else if (ratioSortie != null && enExercice) {
                    /* Pendant les exercices, le cache reste visible même sans
                       les rectangles emboîtés : le cadre qu'on entraîne ne
                       doit pas changer sous les pieds selon le mode. */
                    Canvas(Modifier.fillMaxSize()) {
                        val guide = guideDeCadrage(size.width / size.height.toDouble(), ratioSortie)
                        val cadreL = (size.width * guide.largeur).toFloat()
                        val cadreH = (size.height * guide.hauteur).toFloat()
                        val cadreX = (size.width - cadreL) / 2
                        val cadreY = (size.height - cadreH) / 2
                        val cache = Color.Black.copy(alpha = 0.72f)
                        if (cadreY > 0f) {
                            drawRect(cache, Offset(0f, 0f), Size(size.width, cadreY))
                            drawRect(cache, Offset(0f, cadreY + cadreH), Size(size.width, size.height - cadreY - cadreH))
                        }
                        if (cadreX > 0f) {
                            drawRect(cache, Offset(0f, 0f), Size(cadreX, size.height))
                            drawRect(cache, Offset(cadreX + cadreL, 0f), Size(size.width - cadreX - cadreL, size.height))
                        }
                        drawRect(teinte.copy(alpha = 0.9f), Offset(cadreX, cadreY), Size(cadreL, cadreH), style = Stroke(width = 1.5f))
                    }
                }
                if (enExercice && cible != null && !revele) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (exercice == Exercice.CADRER) "Cadre un ${cible!!.mm} mm"
                            else "Quelle focale ?",
                            style = Corps,
                            color = Color.White
                        )
                    }
                }
            }
        }

        /* ── Exercice, en haut ────────────────────────────────────────── */
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Gouttiere, vertical = 10.dp)
        ) {
            ChoixSegmente(
                options = Exercice.entries.toList(),
                selection = exercice,
                libelle = { it.libelle },
                surChoix = {
                    exercice = it
                    cible = null; reponse = null; revele = false
                    justes = 0; posees = 0
                }
            )
        }

        /* ── Le panneau flottant du bas ───────────────────────────────── */
        PanneauFlottant(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 92.dp)
                .animateContentSize()
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            when {
                                focaleActuelle == null -> "––"
                                !chiffreVisible -> "??"
                                else -> focaleActuelle.roundToInt().toString()
                            },
                            style = ChiffreHero,
                            color = if (chiffreVisible && focaleActuelle != null)
                                MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                        Text(
                            " mm",
                            style = Corps,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Etiquette("Équiv. 24×36", accent = true)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (chiffreVisible && focaleActuelle != null)
                            Cadrage.laPlusProche(focaleActuelle).surnom
                        else " ",
                        style = Detail,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (chiffreVisible && equivalence != null)
                            "×" + fmt(moteur.zoomDemande.toDouble(), 1) + " de zoom"
                        else " ",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            BandeauEtat(
                if (focaleActuelle == null)
                    "Cet appareil ne publie pas la focale ou la taille de son capteur : l'équivalence est incalculable ici."
                else "",
                alerte = focaleActuelle == null
            )
            Spacer(Modifier.height(4.dp))

            Deroulant(
                "Cadre",
                OPTIONS_RATIO,
                optionRatio,
                { it.libelle },
                Modifier.fillMaxWidth()
            ) { optionRatio = it; moteur.choisirRatio(it.valeur) }
            Spacer(Modifier.height(Interligne))

            if (!enExercice) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    atteignables.forEach { f ->
                        val proche = focaleActuelle != null &&
                            abs(Cadrage.ecartRelatif(focaleActuelle, f.mm.toDouble())) < 0.06
                        Pastille("${f.mm}", accent = proche) { viser(f.mm.toDouble()) }
                    }
                }
                Spacer(Modifier.height(Interligne))
                Slider(
                    value = moteur.zoomDemande,
                    onValueChange = { moteur.reglerZoom(it) },
                    valueRange = moteur.zoomMinimal..maxOf(moteur.zoomMaximal, moteur.zoomMinimal + 0.01f)
                )
            } else if (cible == null) {
                BoutonPlat("Commencer", accent = true, modifier = Modifier.fillMaxWidth()) {
                    nouvelleConsigne()
                }
            } else {
                if (exercice == Exercice.RECONNAITRE && !revele) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        atteignables.forEach { f ->
                            Pastille("${f.mm}", accent = reponse?.mm == f.mm) { reponse = f }
                        }
                    }
                    Spacer(Modifier.height(Interligne))
                }
                if (revele) {
                    val vraie = focaleActuelle
                    val visee = cible!!
                    val ecart = if (vraie != null) Cadrage.ecartRelatif(vraie, visee.mm.toDouble()) else Double.NaN
                    val juste = when (exercice) {
                        Exercice.CADRER -> abs(ecart) <= 0.15
                        else -> reponse?.mm == visee.mm
                    }
                    Ligne(if (juste) "Juste" else "À revoir", "${visee.mm} mm", visee.usage, accent = juste)
                    if (exercice == Exercice.CADRER && vraie != null) {
                        Ligne(
                            "Tu as cadré", "${vraie.roundToInt()} mm",
                            if (ecart > 0) "trop serré de ${Math.round(abs(ecart) * 100)} %"
                            else "trop large de ${Math.round(abs(ecart) * 100)} %"
                        )
                    }
                    if (exercice == Exercice.RECONNAITRE && reponse != null) Ligne("Tu as dit", "${reponse!!.mm} mm")
                    Spacer(Modifier.height(8.dp))
                    BoutonPlat("Suivante", accent = true, modifier = Modifier.fillMaxWidth()) { nouvelleConsigne() }
                } else {
                    BoutonPlat(
                        if (exercice == Exercice.CADRER) "C'est ça" else "Vérifier",
                        accent = true,
                        actif = exercice == Exercice.CADRER || reponse != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { verifier() }
                }
                if (posees > 0) {
                    Spacer(Modifier.height(6.dp))
                    Ligne(
                        "Score", "$justes / $posees",
                        if (posees < 5) "encore un peu court"
                        else "${Math.round(justes * 100.0 / posees)} % de réussite"
                    )
                }
            }

            LigneBoutons {
                BoutonPlat(if (detailsOuverts) "Replier" else "Détails", modifier = Modifier.weight(1f)) {
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
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = Interligne)
                ) {
                    Separateur()
                    Ligne(
                        "Angle de champ",
                        if (chiffreVisible && equivalence != null) fmtDegres(equivalence.angleHorizontal) else "—",
                        if (chiffreVisible && equivalence != null)
                            "vertical ${fmtDegres(equivalence.angleVertical)} · diagonal ${fmtDegres(equivalence.angleDiagonal)}"
                        else " "
                    )
                    Ligne(
                        "Optique réelle",
                        if (equivalence != null) fmt(equivalence.focaleReelleMm, 1) + " mm" else "—",
                        if (chiffreVisible && equivalence != null)
                            "facteur de conversion ×" + fmt(equivalence.facteurDeConversion, 1) else " "
                    )
                    Ligne(
                        "Convention des fabricants",
                        if (chiffreVisible && equivalence != null)
                            equivalence.equivalentDiagonal.roundToInt().toString() + " mm" else "—",
                        "calculée sur la diagonale ; le grand chiffre, lui, cadre la même largeur"
                    )
                    Separateur()
                    Etiquette("Les ratios de cadrage")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Un ratio ne s'obtient jamais en élargissant le capteur, seulement en soustrayant — le cache qui se dessine sur le viseur en est la preuve visuelle.",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    (RATIOS_PHOTO + RATIOS_CINE).forEach { r: RatioCadre ->
                        Ligne(
                            "${r.famille.libelle} — ${r.nom}",
                            "",
                            r.detail,
                            accent = optionRatio.valeur == r.ratio && optionRatio.libelle.endsWith(r.nom)
                        )
                    }
                    Separateur()
                    Etiquette("Les focales et ce qu'elles font")
                    Spacer(Modifier.height(8.dp))
                    FOCALES_CLASSIQUES.forEach { f ->
                        val proche = focaleActuelle != null && chiffreVisible &&
                            Cadrage.laPlusProche(focaleActuelle).mm == f.mm
                        Ligne("${f.mm} mm — ${f.surnom}", fmtDegres(f.angleHorizontal), f.usage, accent = proche)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
