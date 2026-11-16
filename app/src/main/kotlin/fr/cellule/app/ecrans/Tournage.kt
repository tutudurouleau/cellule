@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.FiltresJson
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.mesure.MoteurCamera
import fr.cellule.app.mesure.OutilImage
import fr.cellule.core.ANGLES_OBTURATION
import fr.cellule.core.CADENCES
import fr.cellule.core.DENSITES_NEUTRES
import fr.cellule.core.FamilleFiltre
import fr.cellule.core.FiltrePose
import fr.cellule.core.INDICES_EXPOSITION
import fr.cellule.core.OriginePerte
import fr.cellule.core.OutilsImage
import fr.cellule.core.Photometrie
import fr.cellule.core.ReglageTournage
import fr.cellule.core.ResultatTournage
import fr.cellule.core.SeuilsZebras
import fr.cellule.core.Situations
import fr.cellule.core.TStop
import fr.cellule.core.T_STOPS
import fr.cellule.core.Tournage
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.nombreCourt
import kotlin.math.pow

/** Les préférences gardent des flottants : on retrouve la valeur choisie au millième. */
private fun auMillieme(x: Double): Double = Math.round(x * 1000) / 1000.0

/** Les réglages de tournage tels que les préférences les ont gardés. */
fun reglageTournage(reglages: Reglages, filtres: List<FiltrePose>): ReglageTournage = ReglageTournage(
    cadence = auMillieme(reglages.cineCadence),
    angle = auMillieme(reglages.cineAngle),
    ei = reglages.cineEi,
    ndEnPlace = reglages.cineNd.coerceIn(0, DENSITES_NEUTRES.last().diaphs),
    filtres = filtres
)

/** « T4 · 1/48 s · EI 800 » : ce qu'on note au carnet. */
fun resumeTournage(resultat: ResultatTournage, reglage: ReglageTournage): String =
    resultat.tProche.libelle + " · " + Tournage.libelleTempsPose(resultat.tempsPose) + " · EI " + reglage.ei

/* ── Par-dessus l'aperçu ───────────────────────────────────────────────── */

/**
 * Les outils d'image, dessinés sur l'aperçu : le calque (faux contraste,
 * zébras) à la taille de l'image, l'histogramme et la forme d'onde en
 * vignettes dans le coin. Rien n'est dessiné tant qu'aucun outil n'est allumé.
 */
@Composable
fun BoxScope.CalquesTournage(moteur: MoteurCamera) {
    val calque = moteur.calque
    if (calque != null) {
        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = calque,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(calque.width, calque.height),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.None
            )
        }
    }
    val histogramme = moteur.histogramme
    val onde = moteur.onde
    if (histogramme != null || onde != null) {
        Column(
            Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (histogramme != null) {
                Canvas(
                    Modifier
                        .size(width = 132.dp, height = 60.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    val max = (histogramme.maxOrNull() ?: 1).coerceAtLeast(1)
                    val pas = size.width / histogramme.size
                    for (i in histogramme.indices) {
                        /* Racine carrée : sans elle un grand aplat écrase tout le reste. */
                        val h = Math.sqrt(histogramme[i].toDouble() / max).toFloat() * size.height
                        if (h > 0f) {
                            drawRect(
                                Color(0xFFF3EEE6),
                                topLeft = Offset(i * pas, size.height - h),
                                size = Size(pas, h)
                            )
                        }
                    }
                }
            }
            if (onde != null) {
                Canvas(
                    Modifier
                        .size(width = 132.dp, height = 76.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    /* Repères à 0, 50 et 100 % du signal. */
                    listOf(0f, 0.5f, 1f).forEach { f ->
                        val y = f * (size.height - 1f)
                        drawLine(Color.White.copy(alpha = 0.28f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }
                    drawImage(
                        image = onde,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(onde.width, onde.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        filterQuality = FilterQuality.Low
                    )
                }
            }
        }
    }
}

/* ── Le coin droit du panneau ──────────────────────────────────────────── */

@Composable
fun LectureTournage(resultat: ResultatTournage?, reglage: ReglageTournage) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            when {
                resultat == null -> "T—"
                resultat.horsBague -> "T" + nombreCourt(resultat.tExact, 1)
                else -> resultat.tProche.libelle
            },
            style = ChiffreGrand,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            Tournage.libelleTempsPose(reglage.tempsPose),
            style = ChiffreGrand,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "EI ${reglage.ei} · ${nombreCourt(reglage.cadence)} im/s · ${Tournage.libelleAngle(reglage.angle)}",
            style = Detail,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* ── Les détails ───────────────────────────────────────────────────────── */

private const val AUTRE = -1.0

/** Un choix dans une liste de nombres, avec « Autre… » pour saisir le sien. */
@Composable
private fun ChoixOuLibre(
    intitule: String,
    valeurs: List<Double>,
    valeur: Double,
    libelle: (Double) -> String,
    modifier: Modifier = Modifier,
    surChoix: (Double) -> Unit
) {
    var saisie by remember { mutableStateOf(false) }
    val options = (if (valeurs.any { it == valeur }) valeurs else valeurs + valeur) + AUTRE
    Deroulant(intitule, options, valeur, { if (it == AUTRE) "Autre…" else libelle(it) }, modifier) {
        if (it == AUTRE) saisie = true else surChoix(it)
    }
    if (saisie) {
        FeuilleSaisieNombre(intitule, onFermer = { saisie = false }) {
            surChoix(it)
            saisie = false
        }
    }
}

@Composable
private fun FeuilleSaisieNombre(titre: String, onFermer: () -> Unit, surValeur: (Double) -> Unit) {
    var texte by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)) {
            Text(titre, style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            Champ(texte, "Valeur", Modifier.fillMaxWidth(), indication = "un nombre, virgule ou point") { texte = it }
            Spacer(Modifier.height(Interligne + 4.dp))
            val valeur = Tournage.lireNombre(texte)
            BoutonPlat("Valider", accent = valeur != null, actif = valeur != null, modifier = Modifier.fillMaxWidth()) {
                valeur?.let(surValeur)
            }
            Spacer(Modifier.height(Gouttiere))
        }
    }
}

@Composable
private fun FeuilleFiltre(onFermer: () -> Unit, surAjout: (FiltrePose) -> Unit) {
    var famille by remember { mutableStateOf(FamilleFiltre.POLARISANT) }
    var nom by remember { mutableStateOf("") }
    var perte by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)) {
            Text("Ajouter un filtre", style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            Text(
                "Cellule ne connaît la perte d'aucun polarisant ni d'aucune diffusion : elle change d'une marque et d'une force à l'autre. Lis-la sur la fiche du fabricant, ou mesure-la — une lecture sans le filtre, une lecture à travers — et note-la ici.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne + 2.dp))
            Deroulant("Famille", FamilleFiltre.entries.toList(), famille, { it.libelle }, Modifier.fillMaxWidth()) {
                famille = it
            }
            Spacer(Modifier.height(Interligne))
            Champ(nom, "Nom (facultatif)", Modifier.fillMaxWidth(), indication = "marque, référence, force") { nom = it }
            Spacer(Modifier.height(Interligne))
            Champ(perte, "Perte en diaphs", Modifier.fillMaxWidth(), indication = "1,5") { perte = it }
            Spacer(Modifier.height(Interligne + 4.dp))
            val diaphs = perte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 && it <= 12 }
            BoutonPlat("Ajouter", accent = diaphs != null, actif = diaphs != null, modifier = Modifier.fillMaxWidth()) {
                if (diaphs != null) {
                    surAjout(
                        FiltrePose(
                            nom = nom.trim().ifBlank { famille.libelle },
                            famille = famille,
                            perteDiaphs = diaphs,
                            origine = OriginePerte.SAISIE
                        )
                    )
                }
            }
            Spacer(Modifier.height(Gouttiere))
        }
    }
}

/** Une ligne de boutons à bascule pour les outils d'image. */
@Composable
private fun BasculesOutils(moteur: MoteurCamera) {
    OutilImage.entries.chunked(2).forEach { paire ->
        LigneBoutons {
            paire.forEach { outil ->
                BoutonPlat(outil.libelle, accent = outil in moteur.outils, modifier = Modifier.weight(1f)) {
                    moteur.basculerOutil(outil)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CurseurSeuil(intitule: String, valeur: Int, plage: ClosedFloatingPointRange<Float>, surValeur: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(intitule, style = Detail, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.width(118.dp))
        Slider(
            value = valeur.toFloat(),
            onValueChange = { surValeur(Math.round(it)) },
            valueRange = plage,
            modifier = Modifier.weight(1f).height(36.dp)
        )
        Text("$valeur %", style = Detail, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
    }
}

/**
 * Tout ce qui se règle en tournage : cadence, obturateur, EI, ouverture
 * voulue et neutre, autres filtres, outils d'image, caméra du téléphone.
 */
@Composable
fun DetailsTournage(
    reglages: Reglages,
    moteur: MoteurCamera,
    ev100: Double?,
    reglage: ReglageTournage,
    resultat: ResultatTournage?,
    avecCamera: Boolean
) {
    var ajoutFiltre by remember { mutableStateOf(false) }
    val voulu = T_STOPS.getOrNull(reglages.cineTVoulu)

    Ligne(
        "Obturation",
        Tournage.libelleTempsPose(reglage.tempsPose),
        "${Tournage.libelleCadence(reglage.cadence)} · ${Tournage.libelleAngle(reglage.angle)} — angle ÷ (360 × cadence)"
    )
    Ligne(
        "Ouverture exacte",
        resultat?.let { "T" + nombreCourt(it.tExact, 2) } ?: "—",
        when {
            resultat == null -> " "
            resultat.horsBague -> "hors de la bague (T1 à T32) : change l'EI, l'obturation ou les filtres"
            else -> "au tiers : ${resultat.tProche.libelle} — l'arrondi coûte ${libelleDiaphs(resultat.ecartArrondi)}"
        }
    )
    Ligne(
        "Niveau",
        ev100?.let { fmtLux(Photometrie.lux(it)) } ?: "—",
        ev100?.let { Situations.nom(it) } ?: " "
    )

    Separateur()
    Etiquette("Caméra")
    Spacer(Modifier.height(8.dp))
    LigneBoutons {
        ChoixOuLibre("Cadence", CADENCES, reglage.cadence, { Tournage.libelleCadence(it) }, Modifier.weight(1f)) {
            reglages.cineCadence = it
        }
        ChoixOuLibre("Obturateur", ANGLES_OBTURATION, reglage.angle, { Tournage.libelleAngle(it) }, Modifier.weight(1f)) {
            reglages.cineAngle = it.coerceAtMost(360.0)
        }
    }
    Spacer(Modifier.height(8.dp))
    LigneBoutons {
        ChoixOuLibre(
            "Indice d'exposition", INDICES_EXPOSITION.map { it.toDouble() }, reglage.ei.toDouble(),
            { "EI " + nombreCourt(it) }, Modifier.weight(1f)
        ) { reglages.cineEi = Math.round(it).toInt().coerceIn(1, 409600) }
        Deroulant(
            "Je veux", listOf<TStop?>(null) + T_STOPS, voulu,
            { it?.libelle ?: "le T mesuré" }, Modifier.weight(1f)
        ) { reglages.cineTVoulu = it?.rang ?: -1 }
    }

    /* ── Le neutre ─────────────────────────────────────────────────────── */
    Separateur()
    Etiquette("Densité neutre")
    Spacer(Modifier.height(4.dp))
    val nd = resultat?.nd
    if (voulu == null) {
        Text(
            "Choisis une ouverture dans « Je veux » : Cellule dira quel neutre ajouter pour la tenir.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else if (nd == null) {
        Text("En attente d'une mesure…", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (nd.manqueDeLumiere) {
        Ligne(
            "Pour ${voulu.libelle}", "aucun ND",
            "il manque ${libelleDiaphs(-nd.aRetenir).removePrefix("+")} diaph de lumière : ouvre davantage, ou monte l'EI",
            accent = true
        )
    } else {
        val apres = Tournage.tStopLePlusProche(voulu.exact * 2.0.pow(nd.residuel / 2))
        Ligne(
            "Pour ${voulu.libelle}, ajoute",
            nd.nd.court,
            when {
                nd.horsSerie -> "il faudrait retenir ${nombreCourt(nd.aRetenir, 1)} diaphs : au-delà du ND 3,0"
                nd.nd.diaphs == 0 && kotlin.math.abs(nd.residuel) < 0.17 -> "la lumière donne déjà cette ouverture"
                kotlin.math.abs(nd.residuel) < 0.17 -> "${nd.nd.diaphs} diaph${if (nd.nd.diaphs > 1) "s" else ""} retenu${if (nd.nd.diaphs > 1) "s" else ""} — tombe juste"
                else -> "écart restant ${libelleDiaphs(nd.residuel)} : règle à ${apres.libelle}, ou garde ${voulu.libelle} et accepte l'écart"
            },
            accent = true
        )
    }
    Spacer(Modifier.height(8.dp))
    Deroulant(
        "ND déjà en place", DENSITES_NEUTRES, DENSITES_NEUTRES[reglage.ndEnPlace], { it.libelle },
        Modifier.fillMaxWidth()
    ) { reglages.cineNd = it.diaphs }
    Text(
        "Une densité de 0,3 retient un diaph, par définition. Le neutre proposé s'ajoute à ce qui est déjà en place.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )

    /* ── Les autres filtres ────────────────────────────────────────────── */
    Separateur()
    Etiquette("Autres filtres")
    Spacer(Modifier.height(4.dp))
    if (reglage.filtres.isEmpty()) {
        Text(
            "Polarisant, diffusion, conversion de couleur : ce qu'ils retiennent entre dans l'exposition.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    reglage.filtres.forEachIndexed { i, f ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.nom, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    f.famille.libelle + " · " + f.mention,
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "−" + nombreCourt(f.perteDiaphs, 2) + " diaph",
                style = Corps,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                Modifier
                    .clickable {
                        reglages.cineFiltres = FiltresJson.ecrire(reglage.filtres.filterIndexed { j, _ -> j != i })
                    }
                    .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 2.dp)
            ) {
                Text("✕", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (reglage.perteFiltres > 0) {
        Ligne("Perte totale, neutre compris", "−" + nombreCourt(reglage.perteFiltres, 2) + " diaph")
    }
    BoutonPlat("Ajouter un filtre", modifier = Modifier.padding(top = 6.dp)) { ajoutFiltre = true }

    /* ── Les outils d'image ────────────────────────────────────────────── */
    if (avecCamera) {
        Separateur()
        Etiquette("Outils d'image")
        Spacer(Modifier.height(4.dp))
        Text(
            "Ils lisent l'image du téléphone, pas le capteur de ta caméra : ils montrent où la scène est claire ou sombre, pas ce que la caméra enregistrera. Éteints tant que tu ne les allumes pas.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        BasculesOutils(moteur)
        if (OutilImage.ZEBRAS in moteur.outils) {
            CurseurSeuil("Zébras hauts, dès", reglages.zebraHaut, 80f..100f) {
                reglages.zebraHaut = it
                moteur.seuilsZebras = SeuilsZebras(it, reglages.zebraMoyen)
            }
            CurseurSeuil("Zébras moyens, ± 5", reglages.zebraMoyen, 30f..75f) {
                reglages.zebraMoyen = it
                moteur.seuilsZebras = SeuilsZebras(reglages.zebraHaut, it)
            }
            Text(
                "Rayures rouges : tout ce qui dépasse le seuil haut. Rayures vertes : ce qui tombe à ± 5 % du seuil moyen.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
        }
        if (OutilImage.FAUX_CONTRASTE in moteur.outils) {
            OutilsImage.PALETTE.filter { it.argb != OutilsImage.TRANSPARENT }.forEach { plage ->
                Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).background(Color(plage.argb or 0xFF000000.toInt()), RoundedCornerShape(3.dp)))
                    Text(
                        "  ${plage.de} à ${minOf(plage.a, 100)} % — ${plage.nom}",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "Palette propre à Cellule, en pourcentage du signal du téléphone ; le reste de l'image reste visible.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
        }

        /* ── Les caméras du téléphone ──────────────────────────────────── */
        Separateur()
        Etiquette("Caméra du téléphone")
        Spacer(Modifier.height(6.dp))
        val cameras = moteur.cameras
        if (cameras.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cameras.take(5).forEach { c ->
                    Pastille(c.libelle, accent = c.id == moteur.cameraActive) { moteur.choisirCamera(c.id) }
                }
            }
            Text(
                "Focales équivalentes 24×36, calculées d'après ce que le téléphone déclare de ses capteurs.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            Text(
                "Ce téléphone n'expose qu'une caméra arrière aux applications" +
                    (cameras.firstOrNull()?.equivalent?.let { " (équivalent ${Math.round(it)} mm)" } ?: "") +
                    ". Si ses autres modules existent, le système les garde pour son appli photo.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (moteur.avisCamera.isNotBlank()) {
            Text(
                moteur.avisCamera,
                style = Detail,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    if (ajoutFiltre) {
        FeuilleFiltre(onFermer = { ajoutFiltre = false }) {
            reglages.cineFiltres = FiltresJson.ecrire(reglage.filtres + it)
            ajoutFiltre = false
        }
    }
}
