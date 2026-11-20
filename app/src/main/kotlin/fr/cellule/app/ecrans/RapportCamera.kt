/* ModalBottomSheet est encore annoncé expérimental dans cette version de Material3. */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cellule.app.CadreJson
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.ExportPdf
import fr.cellule.app.FiltresJson
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RapportsCamera
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.core.JourCinema
import fr.cellule.core.PriseCamera
import fr.cellule.core.Projet
import fr.cellule.core.RapportCamera
import fr.cellule.core.VerdictPrise
import fr.cellule.core.nombreCourt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Le rapport caméra d'un jour de tournage : une ligne par prise. « Prise
 * suivante » et « Nouveau plan » reprennent les réglages de la précédente :
 * sur le plateau, un toucher suffit, et le verdict se change d'un toucher
 * sur sa pastille. Le PDF part le soir à la production et au labo.
 */

private fun heureCourante(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

/** La première prise d'un jour, préremplie avec le matériel et le réglage de la caméra. */
private fun premierePrise(reglages: Reglages, jourId: Long): PriseCamera {
    val cadre = CadreJson.lireTexte(reglages.cineCadre)
    return PriseCamera(
        identifiant = System.currentTimeMillis(),
        jourId = jourId,
        plan = "1",
        objectif = cadre.focaleMm?.let { nombreCourt(it, 1) + " mm" } ?: "",
        filtres = FiltresJson.lire(reglages.cineFiltres).joinToString(", ") { it.nom },
        ei = reglages.cineEi.toString(),
        cadence = nombreCourt(reglages.cineCadence, 3),
        obturateur = nombreCourt(reglages.cineAngle, 1),
        heure = heureCourante()
    )
}

private fun verdictSuivant(v: VerdictPrise): VerdictPrise = when (v) {
    VerdictPrise.A_VOIR -> VerdictPrise.BONNE
    VerdictPrise.BONNE -> VerdictPrise.A_REFAIRE
    VerdictPrise.A_REFAIRE -> VerdictPrise.A_VOIR
}

/** Le nombre de prises montrées d'abord : les dernières. */
private const val VISIBLES = 12

@Composable
fun CarteRapportCamera(reglages: Reglages, projet: Projet, jour: JourCinema) {
    val prises = RapportCamera.duJour(RapportsCamera.prises, jour.identifiant)
    var ouverte by remember { mutableStateOf<Long?>(null) }
    var tout by remember { mutableStateOf(false) }
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var message by remember { mutableStateOf("") }
    var enCours by remember { mutableStateOf(false) }

    Carte(
        titre = "Rapport caméra",
        sousTitre = if (prises.isEmpty()) "Une ligne par prise : objectif, T, filtres, et si elle est bonne. Le PDF part le soir à la production et au labo."
        else RapportCamera.bilan(prises)
    ) {
        val visibles = if (tout || prises.size <= VISIBLES) prises else prises.takeLast(VISIBLES)
        if (visibles.size < prises.size) {
            Text(
                "Voir les ${prises.size} prises",
                style = Detail,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { tout = true }.padding(vertical = 6.dp)
            )
        }
        visibles.forEach { p ->
            LignePrise(
                p,
                surClic = { ouverte = p.identifiant },
                surVerdict = { RapportsCamera.enregistrer(p.copy(verdict = verdictSuivant(p.verdict))) }
            )
        }
        Spacer(Modifier.height(Interligne))
        if (prises.isEmpty()) {
            BoutonPlat("Première prise", accent = true, modifier = Modifier.fillMaxWidth()) {
                val p = premierePrise(reglages, jour.identifiant)
                RapportsCamera.enregistrer(p)
                ouverte = p.identifiant
            }
        } else {
            LigneBoutons {
                BoutonPlat("Prise suivante", accent = true, modifier = Modifier.weight(1f)) {
                    RapportsCamera.enregistrer(RapportCamera.priseSuivante(prises.last(), System.currentTimeMillis(), heureCourante()))
                }
                BoutonPlat("Nouveau plan", modifier = Modifier.weight(1f)) {
                    RapportsCamera.enregistrer(RapportCamera.planSuivant(prises.last(), System.currentTimeMillis(), heureCourante()))
                }
            }
            Spacer(Modifier.height(6.dp))
            BoutonPlat("Rapport en PDF", actif = !enCours, modifier = Modifier.fillMaxWidth()) {
                if (!enCours) {
                    enCours = true
                    message = "Fabrication du PDF…"
                    portee.launch {
                        val f = withContext(Dispatchers.IO) {
                            runCatching { ExportPdf.rapportCamera(contexte, prises, projet.nom, jour.intitule) }.getOrNull()
                        }
                        enCours = false
                        message = if (f == null) "Le PDF n'a pas pu être fabriqué." else ""
                        f?.let { runCatching { ExportPdf.partager(contexte, it) }.onFailure { message = "Aucune appli n'a pu le recevoir." } }
                    }
                }
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                BandeauEtat(message, alerte = !enCours)
            }
            Text(
                "« Prise suivante » et « Nouveau plan » reprennent les réglages de la dernière prise. Touche une prise pour la corriger, sa pastille pour dire si elle est bonne.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    ouverte?.let { id -> RapportsCamera.prise(id)?.let { p -> FeuillePrise(p) { ouverte = null } } }
}

@Composable
private fun LignePrise(p: PriseCamera, surClic: () -> Unit, surVerdict: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { surClic() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(p.reference + " · cam. " + p.camera, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            Text(p.reglage.ifBlank { "Réglages à remplir" }, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (p.remarques.isNotBlank()) {
                Text(p.remarques, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
        Pastille(
            if (p.verdict == VerdictPrise.A_VOIR) "À voir" else p.verdict.court,
            accent = p.verdict == VerdictPrise.BONNE,
            modifier = Modifier.padding(start = 8.dp)
        ) { surVerdict() }
    }
}

/** La fiche d'une prise : tout se corrige, et s'enregistre à chaque frappe. */
@Composable
private fun FeuillePrise(p: PriseCamera, surFermer: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = surFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        val id = p.identifiant
        var sequence by remember(id) { mutableStateOf(p.sequence) }
        var plan by remember(id) { mutableStateOf(p.plan) }
        var prise by remember(id) { mutableStateOf(p.prise.toString()) }
        var camera by remember(id) { mutableStateOf(p.camera) }
        var objectif by remember(id) { mutableStateOf(p.objectif) }
        var tStop by remember(id) { mutableStateOf(p.tStop) }
        var filtres by remember(id) { mutableStateOf(p.filtres) }
        var ei by remember(id) { mutableStateOf(p.ei) }
        var cadence by remember(id) { mutableStateOf(p.cadence) }
        var obturateur by remember(id) { mutableStateOf(p.obturateur) }
        var carte by remember(id) { mutableStateOf(p.carte) }
        var heure by remember(id) { mutableStateOf(p.heure) }
        var remarques by remember(id) { mutableStateOf(p.remarques) }
        var supprimer by remember(id) { mutableStateOf(false) }
        val maj: (PriseCamera) -> Unit = { RapportsCamera.enregistrer(it) }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Gouttiere, end = Gouttiere, bottom = Gouttiere * 2)
        ) {
            Text(p.reference, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                Champ(sequence, "Séquence", Modifier.weight(1f)) { sequence = it; maj(p.copy(sequence = it)) }
                Champ(plan, "Plan", Modifier.weight(1f)) { plan = it; maj(p.copy(plan = it)) }
                Champ(prise, "Prise", Modifier.weight(1f)) {
                    prise = it
                    it.trim().toIntOrNull()?.let { n -> maj(p.copy(prise = n.coerceAtLeast(1))) }
                }
            }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                Champ(camera, "Caméra", Modifier.weight(0.7f)) { camera = it; maj(p.copy(camera = it)) }
                Champ(objectif, "Objectif", Modifier.weight(1.2f)) { objectif = it; maj(p.copy(objectif = it)) }
                Champ(tStop, "T", Modifier.weight(0.8f)) { tStop = it; maj(p.copy(tStop = it)) }
            }
            Spacer(Modifier.height(6.dp))
            Champ(filtres, "Filtres (ND, diffusion…)", Modifier.fillMaxWidth()) { filtres = it; maj(p.copy(filtres = it)) }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                Champ(ei, "EI", Modifier.weight(1f)) { ei = it; maj(p.copy(ei = it)) }
                Champ(cadence, "Cadence (im/s)", Modifier.weight(1f)) { cadence = it; maj(p.copy(cadence = it)) }
                Champ(obturateur, "Obturateur (°)", Modifier.weight(1f)) { obturateur = it; maj(p.copy(obturateur = it)) }
            }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                Champ(carte, "Carte ou magasin", Modifier.weight(1f)) { carte = it; maj(p.copy(carte = it)) }
                Champ(heure, "Heure", Modifier.weight(1f)) { heure = it; maj(p.copy(heure = it)) }
            }
            Spacer(Modifier.height(Interligne))
            Etiquette("La prise")
            Spacer(Modifier.height(6.dp))
            ChoixSegmente(listOf(VerdictPrise.A_VOIR, VerdictPrise.BONNE, VerdictPrise.A_REFAIRE), p.verdict, { it.libelle }) { maj(p.copy(verdict = it)) }
            Spacer(Modifier.height(6.dp))
            Champ(remarques, "Remarques (flou, perche dans le champ…)", Modifier.fillMaxWidth(), surUneLigne = false) {
                remarques = it
                maj(p.copy(remarques = it))
            }
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                BoutonPlat("OK", accent = true, modifier = Modifier.weight(1f), surClic = surFermer)
                BoutonPlat(if (supprimer) "Confirmer" else "Supprimer", erreur = supprimer, modifier = Modifier.weight(1f)) {
                    if (supprimer) {
                        RapportsCamera.supprimer(id)
                        surFermer()
                    } else {
                        supprimer = true
                    }
                }
            }
        }
    }
}
