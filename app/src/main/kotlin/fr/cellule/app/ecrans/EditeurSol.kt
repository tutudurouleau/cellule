package fr.cellule.app.ecrans

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.cellule.app.CadreJson
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.DepotCinema
import fr.cellule.app.Detail
import fr.cellule.app.ExportPdf
import fr.cellule.app.FiltresJson
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.MonMateriel
import fr.cellule.app.PlansSol
import fr.cellule.app.Reglages
import fr.cellule.app.mesure.CapteurCap
import fr.cellule.app.mesure.Localisation
import fr.cellule.app.mesure.TuilesOsm
import fr.cellule.app.mesure.declinaisonMagnetique
import fr.cellule.app.sol.DessinCote
import fr.cellule.app.sol.DessinSol
import fr.cellule.app.sol.ImageSol
import fr.cellule.app.sol.OptionsDessin
import fr.cellule.app.sol.VueSol
import fr.cellule.app.sol.soleilDuPlan
import fr.cellule.core.Boussole
import fr.cellule.core.DESANAMORPHOSES
import fr.cellule.core.Eclairage
import fr.cellule.core.ElementSol
import fr.cellule.core.FORMATS_PELLICULE
import fr.cellule.core.LieuPlan
import fr.cellule.core.PlanSol
import fr.cellule.core.SaisieMateriel
import fr.cellule.core.Sol
import fr.cellule.core.Tournage
import fr.cellule.core.TypeElement
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * L'éditeur du plan au sol, en plein écran. Un doigt sur un élément le
 * déplace, sur sa pastille verte le fait tourner, ailleurs fait glisser le
 * plan ; deux doigts zooment. En bas, la palette (rien de sélectionné) ou
 * les réglages de l'élément touché.
 */

private enum class FeuilleSol { OPTIONS, LUMIERE, COTE, EXPORT }

/** L'élément sous le doigt, s'il y en a un — les gens et les machines avant le décor. */
private fun toucher(plan: PlanSol, vue: VueSol, px: Float, py: Float): ElementSol? {
    val x = vue.versPlanX(px)
    val y = vue.versPlanY(py)
    val tolerance = (26f / vue.echelle).toDouble()
    val prioritaires = listOf(TypeElement.COMEDIEN, TypeElement.CAMERA, TypeElement.PROJECTEUR, TypeElement.REPERE)
    val candidats = plan.elements.sortedBy { if (it.type in prioritaires) 0 else 1 }
    return candidats.firstOrNull { e ->
        if (e.type in prioritaires) hypot(x - e.x, y - e.y) <= maxOf(0.35, tolerance)
        else {
            val r = Math.toRadians(e.rotation)
            val dx = x - e.x
            val dy = y - e.y
            val lx = dx * cos(r) + dy * sin(r)
            val ly = -dx * sin(r) + dy * cos(r)
            abs(lx) <= e.largeur / 2 + tolerance && abs(ly) <= maxOf(e.profondeur, 0.1) / 2 + tolerance
        }
    }
}

private fun zoomer(v: VueSol, facteur: Float, centre: Offset, glisse: Offset): VueSol {
    val e = (v.echelle * facteur).coerceIn(6f, 800f)
    val f = e / v.echelle
    return VueSol(e, centre.x - (centre.x - v.origineX) * f + glisse.x, centre.y - (centre.y - v.origineY) * f + glisse.y)
}

/** Un élément neuf, au centre de la vue, avec des dimensions de départ raisonnables. */
private fun nouvelElement(type: TypeElement, plan: PlanSol, x: Double, y: Double, reglages: Reglages): ElementSol {
    val id = System.currentTimeMillis()
    val base = ElementSol(id, type, x, y)
    return when (type) {
        TypeElement.CAMERA -> {
            val cadre = CadreJson.lireTexte(reglages.cineCadre)
            val defaut = FORMATS_PELLICULE.first()
            base.copy(
                nom = Sol.prochaineCamera(plan),
                hauteur = Sol.HAUTEUR_CAMERA,
                focaleMm = cadre.focaleMm ?: 35.0,
                capteurLargeurMm = cadre.capteurLargeurMm ?: defaut.largeurMm,
                capteurHauteurMm = cadre.capteurHauteurMm ?: defaut.hauteurMm,
                desanamorphose = cadre.desanamorphose
            )
        }
        TypeElement.COMEDIEN -> {
            val n = plan.elements.filter { it.type == TypeElement.COMEDIEN }.map { it.nom }.distinct().size + 1
            base.copy(nom = "Rôle $n", hauteur = Sol.HAUTEUR_VISAGE, rotation = 180.0)
        }
        TypeElement.PROJECTEUR -> base.copy(hauteur = Sol.HAUTEUR_PROJECTEUR, rotation = 180.0)
        TypeElement.REFLECTEUR -> base.copy(largeur = 1.0, profondeur = 0.05)
        TypeElement.DIFFUSION -> base.copy(largeur = 1.2, profondeur = 0.05)
        TypeElement.DRAPEAU -> base.copy(largeur = 0.6, profondeur = 0.05)
        TypeElement.RAIL -> base.copy(largeur = 3.0, profondeur = 0.6)
        TypeElement.MUR -> base.copy(largeur = 3.0, profondeur = 0.15)
        TypeElement.FENETRE -> base.copy(largeur = 1.2, profondeur = 0.15)
        TypeElement.PORTE -> base.copy(largeur = 0.9, profondeur = 0.1)
        TypeElement.MEUBLE -> base.copy(largeur = 1.2, profondeur = 0.6, nom = "Meuble")
        TypeElement.REPERE -> base.copy(nom = "Repère", largeur = 0.3, profondeur = 0.3)
    }
}

@Composable
fun EditeurPlanSol(id: Long, reglages: Reglages, cinema: DepotCinema, onFermer: () -> Unit) {
    val enregistre = PlansSol.plan(id)
    if (enregistre == null) {
        onFermer()
        return
    }
    var plan by remember(id) { mutableStateOf(enregistre) }
    /* Enregistré un peu après le dernier geste : pas à chaque image d'un glissé. */
    LaunchedEffect(plan) {
        delay(400)
        PlansSol.enregistrer(plan)
    }
    val fermer: () -> Unit = {
        PlansSol.enregistrer(plan)
        onFermer()
    }
    var selection by remember { mutableStateOf<Long?>(null) }
    var vue by remember { mutableStateOf<VueSol?>(null) }
    var taille by remember { mutableStateOf(IntSize.Zero) }
    var feuille by remember { mutableStateOf<FeuilleSol?>(null) }
    var versionTuiles by remember { mutableIntStateOf(0) }
    val contexte = LocalContext.current
    val densite = LocalDensity.current
    val taillePolice = with(densite) { 15.sp.toPx() }
    val metres = reglages.lumiereMetres

    /* Le fond de carte : les tuiles manquantes se chargent hors du fil principal. */
    LaunchedEffect(plan.carte, plan.latitude, plan.longitude, plan.nordDeg, vue, taille) {
        val v = vue ?: return@LaunchedEffect
        if (!plan.carte || taille.width == 0) return@LaunchedEffect
        delay(250)
        val manquantes = DessinSol.tuilesNecessaires(plan, taille.width, taille.height, v)
            .filter { TuilesOsm.enMemoire(it[0], it[1], it[2]) == null }
        if (manquantes.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.IO) { manquantes.forEach { TuilesOsm.tuile(contexte, it[0], it[1], it[2]) } }
        versionTuiles++
    }

    Dialog(onDismissRequest = fermer, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                /* ── En tête ────────────────────────────────────────────── */
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "‹",
                        style = ChiffreGrand,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { fermer() }.padding(horizontal = 12.dp)
                    )
                    Text(plan.nom, style = Corps, maxLines = 1, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Pastille("Réglages du plan") { feuille = FeuilleSol.OPTIONS }
                    Pastille("Lumière au sujet") { feuille = FeuilleSol.LUMIERE }
                    Pastille("Vue de côté") { feuille = FeuilleSol.COTE }
                    Pastille("Exporter") { feuille = FeuilleSol.EXPORT }
                    Pastille("Tout voir") {
                        vue = VueSol.ajustee(plan, taille.width.toFloat(), taille.height.toFloat(), 40f)
                    }
                }
                Spacer(Modifier.height(6.dp))

                /* ── Le plan ────────────────────────────────────────────── */
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                        .onSizeChanged {
                            taille = it
                            if (vue == null && it.width > 0) {
                                vue = VueSol.ajustee(plan, it.width.toFloat(), it.height.toFloat(), 40f)
                            }
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val premier = awaitFirstDown(requireUnconsumed = false)
                                val v0 = vue ?: return@awaitEachGesture
                                /* 0 : glisser le plan ; 1 : déplacer ; 2 : tourner. */
                                var mode = 0
                                var cible: Long? = null
                                val choisi = selection?.let { plan.element(it) }
                                if (choisi != null) {
                                    val (hx, hy) = DessinSol.poigneeRotation(choisi, v0)
                                    if (hypot(premier.position.x - hx, premier.position.y - hy) < 44f) {
                                        mode = 2
                                        cible = choisi.identifiant
                                    }
                                }
                                if (mode == 0) {
                                    toucher(plan, v0, premier.position.x, premier.position.y)?.let {
                                        mode = 1
                                        cible = it.identifiant
                                        selection = it.identifiant
                                    }
                                }
                                var bouge = false
                                do {
                                    val evenement = awaitPointerEvent()
                                    val appuyes = evenement.changes.filter { it.pressed }
                                    val v = vue ?: v0
                                    if (appuyes.size >= 2) {
                                        vue = zoomer(v, evenement.calculateZoom(), evenement.calculateCentroid(), evenement.calculatePan())
                                        mode = 0
                                        cible = null
                                        bouge = true
                                    } else if (appuyes.size == 1) {
                                        val doigt = appuyes[0]
                                        val d = doigt.positionChange()
                                        if (d != Offset.Zero) {
                                            bouge = true
                                            val c = cible
                                            when {
                                                mode == 1 && c != null -> plan.element(c)?.let { e ->
                                                    plan = plan.remplacer(e.copy(x = e.x + d.x / v.echelle, y = e.y + d.y / v.echelle))
                                                }
                                                mode == 2 && c != null -> plan.element(c)?.let { e ->
                                                    var r = Sol.rotationVers(e.x, e.y, v.versPlanX(doigt.position.x), v.versPlanY(doigt.position.y))
                                                    /* Aimanté aux multiples de 15°. */
                                                    val proche = (r / 15).roundToInt() * 15.0
                                                    if (abs(r - proche) < 3) r = Boussole.normaliser(proche)
                                                    plan = plan.remplacer(e.copy(rotation = r))
                                                }
                                                else -> vue = v.copy(origineX = v.origineX + d.x, origineY = v.origineY + d.y)
                                            }
                                        }
                                    }
                                    evenement.changes.forEach { if (it.positionChanged()) it.consume() }
                                } while (evenement.changes.any { it.pressed })
                                if (!bouge && mode == 0) selection = null
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val v = vue ?: return@Canvas
                        /* Relire ce compteur : une tuile arrivée redessine le plan. */
                        if (versionTuiles < 0) return@Canvas
                        drawIntoCanvas { toile ->
                            DessinSol.dessiner(
                                toile.nativeCanvas, plan, size.width.toInt(), size.height.toInt(), v,
                                OptionsDessin(
                                    selection = selection,
                                    texte = taillePolice,
                                    metres = metres,
                                    tuile = if (plan.carte) { z, x, y -> TuilesOsm.enMemoire(z, x, y) } else null,
                                    projecteurs = { MonMateriel.projecteur(it) }
                                )
                            )
                        }
                    }
                }

                /* ── En bas : palette ou réglages de l'élément ──────────── */
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Gouttiere, vertical = 10.dp)
                ) {
                    val e = selection?.let { plan.element(it) }
                    if (e == null) {
                        Etiquette("Ajouter")
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TypeElement.entries.forEach { t ->
                                Pastille("+ " + t.libelle) {
                                    val v = vue ?: return@Pastille
                                    val x = v.versPlanX(taille.width / 2f)
                                    val y = v.versPlanY(taille.height / 2f)
                                    val n = nouvelElement(t, plan, x, y, reglages)
                                    plan = plan.copy(elements = plan.elements + n)
                                    selection = n.identifiant
                                }
                            }
                        }
                        Text(
                            "Touche un élément pour le régler, fais-le glisser pour le déplacer ; sa pastille verte le fait tourner. Un doigt ailleurs déplace le plan, deux doigts zooment.",
                            style = Detail,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        ReglagesElement(
                            e, plan, metres,
                            surChangement = { plan = plan.remplacer(it) },
                            surPositionSuivante = {
                                val (dx, dy) = Sol.direction(e.rotation)
                                val autres = plan.elements.filter { it.type == e.type && it.nom.trim() == e.nom.trim() }
                                val suivante = e.copy(
                                    identifiant = System.currentTimeMillis(),
                                    x = e.x + dx * 1.0, y = e.y + dy * 1.0,
                                    etape = (autres.maxOfOrNull { it.etape } ?: e.etape) + 1
                                )
                                plan = plan.copy(elements = plan.elements + suivante)
                                selection = suivante.identifiant
                            },
                            surDupliquer = {
                                val copie = e.copy(identifiant = System.currentTimeMillis(), x = e.x + 0.5, y = e.y + 0.5)
                                plan = plan.copy(elements = plan.elements + copie)
                                selection = copie.identifiant
                            },
                            surSupprimer = {
                                plan = plan.sans(e.identifiant)
                                selection = null
                            },
                            surFini = { selection = null }
                        )
                    }
                }
            }
        }

        when (feuille) {
            FeuilleSol.OPTIONS -> FeuilleOptionsSol(plan, cinema, { plan = it }, onSupprimer = {
                PlansSol.supprimer(plan.identifiant)
                onFermer()
            }) { feuille = null }
            FeuilleSol.LUMIERE -> FeuilleLumiereSol(plan, reglages) { feuille = null }
            FeuilleSol.COTE -> FeuilleCoteSol(plan, reglages) { feuille = null }
            FeuilleSol.EXPORT -> FeuilleExportSol(plan, cinema, metres) { feuille = null }
            null -> {}
        }
    }
}

/* ── Les réglages d'un élément ─────────────────────────────────────────── */

/** Un champ de longueur, affiché en mètres ou en pieds, rendu en mètres. */
@Composable
private fun ChampLongueur(valeur: Double?, intitule: String, metres: Boolean, cle: Any, modifier: Modifier, surValeur: (Double?) -> Unit) {
    var t by remember(cle, metres) { mutableStateOf(valeur?.let { nombreCourt(if (metres) it else Unites.pieds(it), 2) } ?: "") }
    Champ(t, intitule + if (metres) " (m)" else " (ft)", modifier) {
        t = it
        surValeur(SaisieMateriel.lireNombre(it)?.let { v -> if (metres) v else Unites.metres(v) })
    }
}

@Composable
private fun ChampReel(valeur: Double?, intitule: String, cle: Any, modifier: Modifier, surValeur: (Double?) -> Unit) {
    var t by remember(cle) { mutableStateOf(valeur?.let { nombreCourt(it, 2) } ?: "") }
    Champ(t, intitule, modifier) {
        t = it
        surValeur(it.trim().replace(',', '.').toDoubleOrNull())
    }
}

@Composable
private fun ColumnScope.ReglagesElement(
    e: ElementSol,
    plan: PlanSol,
    metres: Boolean,
    surChangement: (ElementSol) -> Unit,
    surPositionSuivante: () -> Unit,
    surDupliquer: () -> Unit,
    surSupprimer: () -> Unit,
    surFini: () -> Unit
) {
    val cle = e.identifiant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Etiquette(e.type.libelle, modifier = Modifier.weight(1f))
        BoutonPlat("OK", accent = true, surClic = surFini)
    }
    Spacer(Modifier.height(6.dp))
    var nom by remember(cle) { mutableStateOf(e.nom) }
    LigneBoutons {
        Champ(nom, if (e.type == TypeElement.CAMERA) "Caméra (lettre)" else "Nom", Modifier.weight(1.4f)) {
            nom = it
            surChangement(e.copy(nom = it))
        }
        if (e.type.enMouvement) {
            ChampReel(e.etape.toDouble(), "Position n°", cle, Modifier.weight(1f)) { v -> v?.let { surChangement(e.copy(etape = it.roundToInt().coerceAtLeast(1))) } }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text("Orientation " + nombreCourt(e.rotation, 0) + "°", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(value = e.rotation.toFloat(), onValueChange = { surChangement(e.copy(rotation = (it / 5).roundToInt() * 5.0)) }, valueRange = 0f..355f)

    when (e.type) {
        TypeElement.CAMERA -> {
            LigneBoutons {
                ChampReel(e.focaleMm, "Focale (mm)", cle, Modifier.weight(1f)) { surChangement(e.copy(focaleMm = it?.takeIf { f -> f > 0 })) }
                ChampLongueur(e.hauteur, "Hauteur", metres, cle, Modifier.weight(1f)) { surChangement(e.copy(hauteur = it)) }
            }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                ChampReel(e.capteurLargeurMm, "Capteur L (mm)", cle, Modifier.weight(1f)) { surChangement(e.copy(capteurLargeurMm = it?.takeIf { f -> f > 0 })) }
                ChampReel(e.capteurHauteurMm, "H (mm)", cle, Modifier.weight(1f)) { surChangement(e.copy(capteurHauteurMm = it?.takeIf { f -> f > 0 })) }
            }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                ChampReel(e.inclinaison, "Inclinaison (°)", cle, Modifier.weight(1f)) { surChangement(e.copy(inclinaison = it ?: 0.0)) }
                Deroulant("Anamorphose", DESANAMORPHOSES, e.desanamorphose, { if (it == 1.0) "×1" else "×" + nombreCourt(it, 1) }, Modifier.weight(1f)) {
                    surChangement(e.copy(desanamorphose = it))
                }
            }
            Spacer(Modifier.height(6.dp))
            var plans by remember(cle) { mutableStateOf(e.plans) }
            Champ(plans, "Plans tournés d'ici", Modifier.fillMaxWidth(), indication = "3, 4, 7") { plans = it; surChangement(e.copy(plans = it)) }
            Sol.champHorizontal(e)?.let {
                Text("Champ " + nombreCourt(it, 0) + "° × " + nombreCourt(Sol.champVertical(e) ?: 0.0, 0) + "°", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
        }
        TypeElement.COMEDIEN -> {
            ChampLongueur(e.hauteur, "Hauteur du visage", metres, cle, Modifier.fillMaxWidth()) { surChangement(e.copy(hauteur = it)) }
        }
        TypeElement.PROJECTEUR -> {
            val cles = listOf("") + MonMateriel.projecteurs.map { it.identifiant }
            Deroulant("Projecteur", cles, e.projecteur.takeIf { it in cles } ?: "", { c ->
                if (c.isBlank()) "Saisie directe" else "★ " + (MonMateriel.projecteur(c)?.nomComplet ?: "")
            }, Modifier.fillMaxWidth()) { surChangement(e.copy(projecteur = it)) }
            Spacer(Modifier.height(6.dp))
            val choisi = MonMateriel.projecteur(e.projecteur)
            if (choisi == null) {
                LigneBoutons {
                    ChampReel(e.luxReference, "Lux", cle, Modifier.weight(1f)) { surChangement(e.copy(luxReference = it)) }
                    ChampLongueur(e.distanceReference, "à", metres, cle, Modifier.weight(1f)) { surChangement(e.copy(distanceReference = it)) }
                    ChampReel(e.faisceau, "Faisceau (°)", cle, Modifier.weight(1f)) { surChangement(e.copy(faisceau = it?.takeIf { a -> a in 1.0..179.0 })) }
                }
            } else if (choisi.mesure(true) != null && choisi.mesure(false) != null) {
                ChoixSegmente(listOf(false, true), e.spot, { if (it) "Serré" else "Ouvert" }) { surChangement(e.copy(spot = it)) }
            }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                ChampLongueur(e.hauteur, "Hauteur", metres, cle, Modifier.weight(1f)) { surChangement(e.copy(hauteur = it)) }
                ChampReel(e.pertesDiaphs, "Pertes (diaphs)", cle, Modifier.weight(1f)) { surChangement(e.copy(pertesDiaphs = it ?: 0.0)) }
            }
        }
        TypeElement.REPERE -> {}
        else -> {
            LigneBoutons {
                ChampLongueur(e.largeur, "Longueur", metres, cle, Modifier.weight(1f)) { v -> v?.let { surChangement(e.copy(largeur = it.coerceIn(0.05, 100.0))) } }
                ChampLongueur(e.profondeur, if (e.type == TypeElement.MEUBLE) "Profondeur" else "Épaisseur", metres, cle, Modifier.weight(1f)) { v ->
                    v?.let { surChangement(e.copy(profondeur = it.coerceIn(0.01, 100.0))) }
                }
            }
        }
    }
    Spacer(Modifier.height(Interligne))
    LigneBoutons {
        if (e.type.enMouvement) BoutonPlat("Position suivante", accent = true, modifier = Modifier.weight(1.3f), surClic = surPositionSuivante)
        BoutonPlat("Dupliquer", modifier = Modifier.weight(1f), surClic = surDupliquer)
        BoutonPlat("Supprimer", erreur = true, surClic = surSupprimer)
    }
}

/* ── Les feuilles ──────────────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeuilleSolBase(titre: String, onFermer: () -> Unit, contenu: @Composable ColumnScope.() -> Unit) {
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
            Text(titre, style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            contenu()
            Spacer(Modifier.height(Gouttiere * 2))
        }
    }
}

@Composable
private fun LigneInterrupteur(texte: String, valeur: Boolean, surChangement: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texte, style = Corps, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = valeur, onCheckedChange = surChangement)
    }
}

@Composable
private fun FeuilleOptionsSol(
    plan: PlanSol,
    cinema: DepotCinema,
    surChangement: (PlanSol) -> Unit,
    onSupprimer: () -> Unit,
    onFermer: () -> Unit
) {
    val contexte = LocalContext.current
    val localisation = remember { Localisation(contexte) }
    val boussole = remember { CapteurCap(contexte) }
    var cap by remember { mutableStateOf<Double?>(null) }
    var avis by remember { mutableStateOf("") }
    var supprimer by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        boussole.demarrer()
        onDispose { boussole.arreter() }
    }
    LaunchedEffect(Unit) {
        while (true) {
            cap = boussole.capMagnetique
            delay(300)
        }
    }
    val chercher: () -> Unit = {
        avis = "Recherche de ta position…"
        localisation.obtenir { p ->
            if (p == null) avis = "Position introuvable : essaie dehors."
            else {
                avis = ""
                surChangement(plan.copy(latitude = p.latitude, longitude = p.longitude))
            }
        }
    }
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) chercher() else avis = "Sans la localisation, tape les coordonnées."
    }

    FeuilleSolBase("Réglages du plan", onFermer) {
        var nom by remember { mutableStateOf(plan.nom) }
        Champ(nom, "Nom du plan", Modifier.fillMaxWidth()) { nom = it; surChangement(plan.copy(nom = it)) }
        Spacer(Modifier.height(Interligne))
        val jours = listOf<Long?>(null) + cinema.donnees.jours.map { it.identifiant }
        Deroulant("Rangé dans", jours, plan.jourId.takeIf { it in jours }, { j ->
            if (j == null) "Aucun jour" else cinema.donnees.jours.first { it.identifiant == j }.let { jr ->
                (cinema.donnees.projets.firstOrNull { it.identifiant == jr.projetId }?.nom?.let { "$it › " } ?: "") + jr.intitule
            }
        }, Modifier.fillMaxWidth()) { surChangement(plan.copy(jourId = it)) }

        Separateur()
        Etiquette("Affichage")
        LigneInterrupteur("Grille", plan.grille) { surChangement(plan.copy(grille = it)) }
        if (plan.grille) {
            ChoixSegmente(listOf(0.25, 0.5, 1.0, 2.0), plan.pasGrille, { nombreCourt(it, 2) + " m" }) { surChangement(plan.copy(pasGrille = it)) }
        }
        LigneInterrupteur("Légende", plan.legende) { surChangement(plan.copy(legende = it)) }

        Separateur()
        Etiquette("Lieu et nord")
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            BoutonPlat("Ma position", modifier = Modifier.weight(1f)) {
                if (localisation.autorisee()) chercher()
                else demande.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
        var coord by remember { mutableStateOf(listOfNotNull(plan.latitude, plan.longitude).joinToString(", ") { nombreCourt(it, 5).replace(',', '.') }) }
        Spacer(Modifier.height(6.dp))
        Champ(coord, "Latitude, longitude", Modifier.fillMaxWidth(), indication = "43.6045, 1.4440") {
            coord = it
            fr.cellule.core.Lieux.lireCoordonnees(it)?.let { (la, lo) -> surChangement(plan.copy(latitude = la, longitude = lo)) }
        }
        Text(
            avis.ifBlank { plan.latitude?.let { LieuPlan(it, plan.longitude ?: 0.0).coordonnees } ?: "Aucun lieu : le soleil et la carte en ont besoin." },
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(Interligne))
        val declinaison = plan.latitude?.let { declinaisonMagnetique(LieuPlan(it, plan.longitude ?: 0.0)) } ?: 0.0
        val capVrai = cap?.let { Boussole.normaliser(it + declinaison) }
        Text(
            "Nord du plan : " + nombreCourt(plan.nordDeg, 0) + "° depuis le haut. Pour le caler, vise avec le dos du téléphone dans la direction du haut de ton plan, puis touche « Prendre ».",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            BoutonPlat(
                capVrai?.let { "Prendre (visée " + nombreCourt(it, 0) + "°)" } ?: "Boussole indisponible",
                actif = capVrai != null,
                accent = capVrai != null,
                modifier = Modifier.weight(1f)
            ) { capVrai?.let { surChangement(plan.copy(nordDeg = Boussole.normaliser(360 - it))) } }
        }
        Slider(value = plan.nordDeg.toFloat(), onValueChange = { surChangement(plan.copy(nordDeg = (it / 5).roundToInt() * 5.0)) }, valueRange = 0f..355f)
        if (plan.latitude == null) {
            Text("Sans lieu, la boussole donne le nord magnétique.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LigneInterrupteur("Fond de carte (extérieur)", plan.carte) { surChangement(plan.copy(carte = it)) }
        if (plan.carte) {
            Text(
                "Carte OpenStreetMap à l'échelle, centrée sur le lieu (au point 0 du plan), tournée selon le nord. Il faut du réseau la première fois ; ensuite les tuiles restent en cache.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Separateur()
        Etiquette("Soleil")
        LigneInterrupteur("Afficher le soleil", plan.soleil) { surChangement(plan.copy(soleil = it)) }
        var date by remember { mutableStateOf(plan.date.ifBlank { LocalDate.now().toString() }) }
        var heure by remember { mutableStateOf(plan.heure.ifBlank { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }) }
        LigneBoutons {
            Champ(date, "Date (AAAA-MM-JJ)", Modifier.weight(1.3f)) { date = it; surChangement(plan.copy(date = it.trim())) }
            Champ(heure, "Heure (HH:MM)", Modifier.weight(1f)) { heure = it; surChangement(plan.copy(heure = it.trim())) }
        }
        soleilDuPlan(plan)?.let { s ->
            Ligne(
                "Soleil",
                if (s.hauteur < -0.8) "sous l'horizon" else "hauteur " + nombreCourt(s.hauteur, 0) + "°",
                "azimut " + nombreCourt(s.azimut, 0) + "° (" + Boussole.cardinal(s.azimut) + ") — calculé"
            )
        }
        ImageSol.infos(plan).filter { it.first.startsWith("Heure") }.forEach { (i, v) -> Ligne(i, "", v) }

        Separateur()
        var notes by remember { mutableStateOf(plan.notes) }
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false) { notes = it; surChangement(plan.copy(notes = it)) }
        Spacer(Modifier.height(Interligne))
        BoutonPlat(if (supprimer) "Confirmer : supprimer ce plan au sol" else "Supprimer ce plan au sol", erreur = supprimer, modifier = Modifier.fillMaxWidth()) {
            if (supprimer) onSupprimer() else supprimer = true
        }
    }
}

@Composable
private fun FeuilleLumiereSol(plan: PlanSol, reglages: Reglages, onFermer: () -> Unit) {
    val sujets = plan.elements.filter { it.type == TypeElement.COMEDIEN }
    var sujetId by remember { mutableStateOf(sujets.firstOrNull()?.identifiant) }
    val metres = reglages.lumiereMetres
    val enLux = reglages.lumiereLux
    fun eclair(l: Double) = if (enLux) nombreCourt(l, 0) + " lux" else nombreCourt(Unites.fc(l), 1) + " fc"
    fun dist(m: Double) = if (metres) nombreCourt(m, 2) + " m" else nombreCourt(Unites.pieds(m), 1) + " ft"
    FeuilleSolBase("Lumière au sujet", onFermer) {
        if (sujets.isEmpty()) {
            Text("Pose d'abord un comédien sur le plan.", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FeuilleSolBase
        }
        Deroulant("Sujet", sujets.map { it.identifiant }, sujetId ?: sujets.first().identifiant, { id ->
            sujets.first { it.identifiant == id }.libelle
        }, Modifier.fillMaxWidth()) { sujetId = it }
        val sujet = sujets.firstOrNull { it.identifiant == sujetId } ?: sujets.first()
        val apports = Sol.apports(plan, sujet) { MonMateriel.projecteur(it) }
        if (apports.isEmpty()) {
            Text("Aucun projecteur sur le plan.", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FeuilleSolBase
        }
        val reglage = reglageTournage(reglages, FiltresJson.lire(reglages.cineFiltres))
        Spacer(Modifier.height(6.dp))
        apports.forEach { a ->
            val nom = a.projecteur.nom.ifBlank { MonMateriel.projecteur(a.projecteur.projecteur)?.nomComplet ?: "Projecteur" }
            Ligne(
                nom,
                a.lux?.let { eclair(it) } ?: "lux inconnus",
                listOfNotNull(
                    dist(a.distance),
                    a.position,
                    if (!a.dansFaisceau) "hors du faisceau (" + nombreCourt(a.horsAxe, 0) + "° de l'axe)" else null,
                    a.lux?.takeIf { it > 0 }?.let { "seul : " + Tournage.tStopLePlusProche(Tournage.ouvertureExacte(Eclairage.ev100(it), reglage)).libelle }
                ).joinToString(" · ")
            )
        }
        val total = apports.mapNotNull { it.lux }.sum()
        if (total > 0) {
            Separateur()
            val t = Tournage.ouvertureExacte(Eclairage.ev100(total), reglage)
            Ligne("Total au visage", eclair(total), "les sources du faisceau s'additionnent", accent = true)
            Ligne("Ouverture", Tournage.tStopLePlusProche(t).libelle, "exacte T" + nombreCourt(t, 2) + " · EI ${reglage.ei}, " + Tournage.libelleTempsPose(reglage.tempsPose), accent = true)
            val face = apports.filter { it.position == "face" }.mapNotNull { it.lux }.sortedDescending()
            if (face.size >= 2 && face[1] > 0) {
                Ligne("Face / débouchage", nombreCourt(face[0] / face[1], 1) + " : 1", "les deux plus fortes sources de face")
            }
        }
        Text(
            "Éclairement dans l'axe de chaque source (cellule incidente tournée vers elle), en supposant le faisceau uniforme. Les distances tiennent compte des hauteurs. Pour les sources douces et proches, vérifie à la cellule.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun FeuilleCoteSol(plan: PlanSol, reglages: Reglages, onFermer: () -> Unit) {
    val cameras = plan.elements.filter { it.type == TypeElement.CAMERA }
    val sujets = plan.elements.filter { it.type == TypeElement.COMEDIEN }
    var cameraId by remember { mutableStateOf(cameras.firstOrNull()?.identifiant) }
    var sujetId by remember { mutableStateOf(sujets.firstOrNull()?.identifiant) }
    val densite = LocalDensity.current
    val taillePolice = with(densite) { 15.sp.toPx() }
    FeuilleSolBase("Vue de côté", onFermer) {
        if (cameras.isEmpty()) {
            Text("Pose d'abord une caméra sur le plan.", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@FeuilleSolBase
        }
        LigneBoutons {
            Deroulant("Caméra", cameras.map { it.identifiant }, cameraId ?: cameras.first().identifiant, { id ->
                cameras.first { it.identifiant == id }.libelle
            }, Modifier.weight(1f)) { cameraId = it }
            if (sujets.isNotEmpty()) {
                Deroulant("Vers", sujets.map { it.identifiant }, sujetId ?: sujets.first().identifiant, { id ->
                    sujets.first { it.identifiant == id }.libelle
                }, Modifier.weight(1f)) { sujetId = it }
            }
        }
        Spacer(Modifier.height(Interligne))
        val camera = cameras.firstOrNull { it.identifiant == cameraId } ?: cameras.first()
        val sujet = sujets.firstOrNull { it.identifiant == sujetId }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        ) {
            drawIntoCanvas { toile ->
                DessinCote.dessiner(
                    toile.nativeCanvas, plan, camera, sujet, size.width.toInt(), size.height.toInt(),
                    OptionsDessin(texte = taillePolice, metres = reglages.lumiereMetres, projecteurs = { MonMateriel.projecteur(it) })
                )
            }
        }
        Text(
            "Coupe le long de l'axe caméra → sujet : les éléments sont ramenés sur cet axe. Le champ vertical suit la focale, le capteur et l'inclinaison de la caméra.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (sujet != null) {
            val hc = camera.hauteur ?: Sol.HAUTEUR_CAMERA
            val hs = sujet.hauteur ?: Sol.HAUTEUR_VISAGE
            Ligne("Pour viser le visage", nombreCourt(Sol.inclinaisonVers(camera, hc, sujet, hs), 1) + "°", "inclinaison de la caméra")
        }
    }
}

@Composable
private fun FeuilleExportSol(plan: PlanSol, cinema: DepotCinema, metres: Boolean, onFermer: () -> Unit) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var enCours by remember { mutableStateOf(false) }
    val jour = cinema.donnees.jours.firstOrNull { it.identifiant == plan.jourId }
    val projet = jour?.let { j -> cinema.donnees.projets.firstOrNull { it.identifiant == j.projetId } }
    val enTete = listOfNotNull(projet?.nom?.takeIf { it.isNotBlank() }, jour?.intitule).joinToString(" › ")
    FeuilleSolBase("Exporter", onFermer) {
        Text(
            "L'image se partage d'un geste (message, mail) ; le PDF ajoute ce qu'on lit sur le plan : caméras, projecteurs, soleil, heures dorées. Les dossiers PDF d'un jour contiennent aussi ses plans au sol.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            BoutonPlat("Image", accent = true, actif = !enCours, modifier = Modifier.weight(1f)) {
                if (!enCours) {
                    enCours = true
                    message = "Fabrication de l'image…"
                    portee.launch {
                        val f = withContext(Dispatchers.IO) { runCatching { ImageSol.fichierImage(contexte, plan, metres, enTete) }.getOrNull() }
                        enCours = false
                        message = if (f == null) "L'image n'a pas pu être fabriquée." else ""
                        f?.let { runCatching { ImageSol.partager(contexte, it) }.onFailure { message = "Aucune appli n'a pu la recevoir." } }
                    }
                }
            }
            BoutonPlat("PDF", accent = true, actif = !enCours, modifier = Modifier.weight(1f)) {
                if (!enCours) {
                    enCours = true
                    message = "Fabrication du PDF…"
                    portee.launch {
                        val f = withContext(Dispatchers.IO) { runCatching { ExportPdf.planSol(contexte, plan, enTete, metres) }.getOrNull() }
                        enCours = false
                        message = if (f == null) "Le PDF n'a pas pu être fabriqué." else ""
                        f?.let { runCatching { ExportPdf.partager(contexte, it) }.onFailure { message = "Aucune appli n'a pu le recevoir." } }
                    }
                }
            }
        }
        if (message.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            BandeauEtat(message, alerte = !enCours)
        }
    }
}

/** La liste des plans au sol d'un jour (ou de tous), avec création. */
@Composable
fun ListePlansSol(reglages: Reglages, cinema: DepotCinema, jourId: Long?, tous: Boolean) {
    var ouvert by remember { mutableStateOf<Long?>(null) }
    val liste = if (tous) PlansSol.plans else PlansSol.plans.filter { it.jourId == jourId }
    Carte(
        titre = "Plans au sol",
        sousTitre = "Le décor vu de dessus, à l'échelle : caméras et leur champ, comédiens et leurs déplacements, projecteurs et leur faisceau, soleil."
    ) {
        if (liste.isEmpty()) Text("Aucun plan au sol pour l'instant.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        liste.forEach { p ->
            val jour = cinema.donnees.jours.firstOrNull { it.identifiant == p.jourId }
            Column(Modifier.fillMaxWidth().clickable { ouvert = p.identifiant }.padding(vertical = 6.dp)) {
                Text(p.nom, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOfNotNull(
                        if (tous) (jour?.intitule ?: "non rangé") else null,
                        p.elements.size.toString() + " élément" + if (p.elements.size > 1) "s" else ""
                    ).joinToString(" · "),
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        BoutonPlat("Nouveau plan au sol", accent = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            val n = PlansSol.nouveau("Plan au sol " + (PlansSol.plans.size + 1), jourId)
            ouvert = n.identifiant
        }
    }
    ouvert?.let { id -> EditeurPlanSol(id, reglages, cinema) { ouvert = null } }
}
