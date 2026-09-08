package fr.cellule.app.ecrans

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
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
import fr.cellule.app.RayonCarte
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.core.Cadrage
import fr.cellule.core.FOCALES_CLASSIQUES
import fr.cellule.core.FocaleClassique
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class Exercice(val libelle: String) {
    LIBRE("Libre"), CADRER("Cadrer"), RECONNAITRE("Reconnaître")
}

/**
 * Apprendre à voir les focales.
 *
 * Une focale ne se retient pas comme un nombre, elle se retient comme un
 * cadrage. L'aperçu montre donc, emboîtés dans l'image, les rectangles que
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
            /* « Cadrer » : on annonce la focale, l'appareil revient au plus large. */
            Exercice.CADRER -> moteur.reglerZoom(moteur.zoomMinimal)
            /* « Reconnaître » : l'appareil se règle, à l'utilisateur de nommer. */
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

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
    ) {

        Spacer(Modifier.height(Interligne))
        Box(Modifier.padding(horizontal = Gouttiere)) {
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

        if (!autorisee) {
            Carte(
                titre = "Accès à la caméra",
                sousTitre = "L'angle de champ se lit dans les caractéristiques de l'optique et la zone de capteur réellement exposée. Aucune image n'est enregistrée ni transmise."
            ) {
                BoutonPlat("Autoriser la caméra", accent = true) {
                    demandeur.launch(Manifest.permission.CAMERA)
                }
            }
            return@Column
        }

        /* ── L'aperçu et les cadres emboîtés ──────────────────────────── */
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Gouttiere, vertical = 5.dp)
                .aspectRatio(moteur.rapportApercu)
                .clip(RoundedCornerShape(RayonCarte))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { vue },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ -> moteur.pincer(zoom) }
                    }
            )
            val teinte = MaterialTheme.colorScheme.primary
            if (focaleActuelle != null && !enExercice) {
                Canvas(Modifier.fillMaxSize()) {
                    /* Les quatre focales suivantes, emboîtées : c'est cette image
                       qu'on veut garder en tête, pas le nombre. */
                    FOCALES_CLASSIQUES
                        .mapNotNull { f ->
                            Cadrage.fractionDuCadre(focaleActuelle, f.mm.toDouble())
                                ?.let { fr -> f to fr }
                        }
                        .filter { it.second < 0.94 }
                        .take(4)
                        .forEach { (_, fraction) ->
                            /* La fraction vient du calcul d'angle, en Double ;
                               la géométrie de dessin, elle, est en Float. */
                            val l = (size.width * fraction).toFloat()
                            val h = (size.height * fraction).toFloat()
                            drawRect(
                                color = teinte.copy(alpha = 0.75f),
                                topLeft = Offset((size.width - l) / 2, (size.height - h) / 2),
                                size = Size(l, h),
                                style = Stroke(width = 2f)
                            )
                        }
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

        /* ── Le chiffre ───────────────────────────────────────────────── */
        CarteInstrument {
            Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    Etiquette("Équivalent 24×36", accent = true)
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

            Spacer(Modifier.height(Interligne))
            BandeauEtat(
                when {
                    focaleActuelle == null ->
                        "Cet appareil ne publie pas la focale ou la taille de son capteur : l'équivalence est incalculable ici."
                    else -> ""
                },
                alerte = focaleActuelle == null
            )

            Spacer(Modifier.height(Interligne))
            Ligne(
                "Angle de champ",
                if (chiffreVisible && equivalence != null)
                    fmtDegres(equivalence.angleHorizontal) else "—",
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
        }

        /* ── Aller directement à une focale ───────────────────────────── */
        if (!enExercice) {
            Carte(
                titre = "Se caler sur une focale",
                sousTitre = "Touche-en une, regarde ce qu'elle cadre, puis reviens en arrière. Le pincement fonctionne aussi."
            ) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Interligne)
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
                Text(
                    "Zoom ×" + fmt(moteur.zoomMinimal.toDouble(), 1) +
                        " à ×" + fmt(moteur.zoomMaximal.toDouble(), 1),
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        /* ── Les exercices ────────────────────────────────────────────── */
        if (enExercice) {
            Carte(
                titre = if (exercice == Exercice.CADRER) "Cadrer une focale" else "Reconnaître une focale",
                sousTitre = if (exercice == Exercice.CADRER)
                    "On te donne une focale, tu zoomes jusqu'à croire l'avoir. Le chiffre reste caché tant que tu n'as pas répondu."
                else
                    "L'appareil s'est réglé sur une focale. Nomme-la sans regarder — puis vérifie."
            ) {
                if (cible == null) {
                    BoutonPlat("Commencer", accent = true, modifier = Modifier.fillMaxWidth()) {
                        nouvelleConsigne()
                    }
                } else {
                    if (exercice == Exercice.RECONNAITRE) {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Interligne)
                        ) {
                            atteignables.forEach { f ->
                                Pastille("${f.mm}", accent = reponse?.mm == f.mm) {
                                    if (!revele) reponse = f
                                }
                            }
                        }
                        Spacer(Modifier.height(Interligne))
                    }

                    if (revele) {
                        val vraie = focaleActuelle
                        val visee = cible!!
                        val ecart = if (vraie != null)
                            Cadrage.ecartRelatif(vraie, visee.mm.toDouble()) else Double.NaN
                        val juste = when (exercice) {
                            Exercice.CADRER -> abs(ecart) <= 0.15
                            else -> reponse?.mm == visee.mm
                        }
                        Ligne(
                            if (juste) "Juste" else "À revoir",
                            "${visee.mm} mm",
                            visee.usage,
                            accent = juste
                        )
                        if (exercice == Exercice.CADRER && vraie != null) {
                            Ligne(
                                "Tu as cadré",
                                "${vraie.roundToInt()} mm",
                                if (ecart > 0) "trop serré de ${Math.round(abs(ecart) * 100)} %"
                                else "trop large de ${Math.round(abs(ecart) * 100)} %"
                            )
                        }
                        if (exercice == Exercice.RECONNAITRE && reponse != null) {
                            Ligne("Tu as dit", "${reponse!!.mm} mm")
                        }
                        Spacer(Modifier.height(Interligne))
                        BoutonPlat("Suivante", accent = true, modifier = Modifier.fillMaxWidth()) {
                            nouvelleConsigne()
                        }
                    } else {
                        BoutonPlat(
                            if (exercice == Exercice.CADRER) "C'est ça" else "Vérifier",
                            accent = true,
                            actif = exercice == Exercice.CADRER || reponse != null,
                            modifier = Modifier.fillMaxWidth()
                        ) { verifier() }
                    }
                }

                if (posees > 0) {
                    Separateur()
                    Ligne(
                        "Score",
                        "$justes / $posees",
                        if (posees < 5) "encore un peu court"
                        else "${Math.round(justes * 100.0 / posees)} % de réussite"
                    )
                }
            }
        }

        /* ── La table des focales ─────────────────────────────────────── */
        Carte(
            titre = "Les focales et ce qu'elles font",
            sousTitre = "Angles de champ sur 24×36. Une focale ne dit rien sur la perspective : c'est la distance au sujet qui la fait. La focale ne décide que du cadrage — mais comme elle oblige à s'approcher ou à reculer, tout se tient."
        ) {
            FOCALES_CLASSIQUES.forEach { f ->
                val proche = focaleActuelle != null && chiffreVisible &&
                    Cadrage.laPlusProche(focaleActuelle).mm == f.mm
                Ligne(
                    "${f.mm} mm — ${f.surnom}",
                    fmtDegres(f.angleHorizontal),
                    f.usage,
                    accent = proche
                )
            }
        }
    }
}
