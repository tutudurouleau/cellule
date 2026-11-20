package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.ExportPdf
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.core.DemandeLocation
import fr.cellule.core.LigneLocation
import fr.cellule.core.Loueurs
import fr.cellule.core.MesTarifs
import fr.cellule.core.PlanSol
import fr.cellule.core.Pointage
import fr.cellule.core.nombreCourt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Du plan de feu au loueur : la demande de location en PDF (jamais envoyée
 * par l'appli), mes tarifs pour estimer, et la check-list d'enlèvement et de
 * retour.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeuilleLocation(titre: String, onFermer: () -> Unit, contenu: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onFermer, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Gouttiere).padding(bottom = Gouttiere)
        ) {
            Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            contenu()
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Les trois gestes, sous la liste « À louer pour ce jour ». */
@Composable
internal fun BoutonsLocation(reglages: Reglages, jourId: Long, lignes: List<LigneLocation>, plans: List<PlanSol>, metres: Boolean) {
    var feuille by remember { mutableStateOf(0) }
    val tarifs = MesTarifs.lire(reglages.mesTarifs)
    val estimation = MesTarifs.estimer(lignes, tarifs)
    if (estimation.lignesChiffrees > 0) {
        Ligne(
            "Estimation", nombreCourt(estimation.parJour, 0) + " € HT / jour",
            if (estimation.complete) "d'après tes tarifs" else "${estimation.lignesChiffrees} lignes chiffrées sur ${estimation.lignesTotal}"
        )
    }
    Spacer(Modifier.height(6.dp))
    BoutonPlat("Demande de location (PDF)", accent = true, modifier = Modifier.fillMaxWidth()) { feuille = 1 }
    Spacer(Modifier.height(6.dp))
    LigneBoutons {
        BoutonPlat("Check-list loueur", modifier = Modifier.weight(1f)) { feuille = 2 }
        BoutonPlat("Mes tarifs", modifier = Modifier.weight(1f)) { feuille = 3 }
    }
    when (feuille) {
        1 -> FeuilleLocation("Demande de location", { feuille = 0 }) { ContenuDemande(reglages, lignes, plans, metres) }
        2 -> FeuilleLocation("Check-list chez le loueur", { feuille = 0 }) { ContenuPointage(reglages, jourId, lignes) }
        3 -> FeuilleLocation("Mes tarifs", { feuille = 0 }) { ContenuTarifs(reglages, lignes) }
    }
}

@Composable
private fun ContenuDemande(reglages: Reglages, lignes: List<LigneLocation>, plans: List<PlanSol>, metres: Boolean, plansParDefaut: Boolean = true) {
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    var infos by remember { mutableStateOf(DemandeLocation.lireInfos(reglages.demandeInfos)) }
    var loueurId by remember { mutableStateOf(Loueurs.TOUS.first().id) }
    var autre by remember { mutableStateOf("") }
    var avecPlans by remember { mutableStateOf(plansParDefaut) }
    var avecPrix by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun maj(i: fr.cellule.core.InfosProduction) { infos = i; reglages.demandeInfos = DemandeLocation.ecrireInfos(i) }

    Text(
        "L'appli fabrique un PDF propre ; elle ne l'envoie à personne. Tu le relis, puis tu le transmets toi-même au loueur.",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Interligne))
    Etiquette("Loueur", accent = true)
    Deroulant("Loueur", Loueurs.TOUS.map { it.id } + "", loueurId, { id -> Loueurs.loueur(id)?.let { "${it.nom} · ${it.ville}" } ?: "Autre loueur" }, Modifier.fillMaxWidth()) { loueurId = it }
    if (loueurId.isBlank()) {
        Spacer(Modifier.height(6.dp))
        Champ(autre, "Nom du loueur", Modifier.fillMaxWidth()) { autre = it }
    }
    Spacer(Modifier.height(Interligne))
    Etiquette("Production", accent = true)
    Champ(infos.titre, "Titre du projet", Modifier.fillMaxWidth()) { maj(infos.copy(titre = it)) }
    Champ(infos.societe, "Société ou école", Modifier.fillMaxWidth()) { maj(infos.copy(societe = it)) }
    Champ(infos.contact, "Contact (nom, poste)", Modifier.fillMaxWidth()) { maj(infos.copy(contact = it)) }
    LigneBoutons {
        Champ(infos.telephone, "Téléphone", Modifier.weight(1f)) { maj(infos.copy(telephone = it)) }
        Champ(infos.email, "E-mail", Modifier.weight(1.3f)) { maj(infos.copy(email = it)) }
    }
    Champ(infos.livraison, "Adresse de livraison ou « enlèvement chez vous »", Modifier.fillMaxWidth()) { maj(infos.copy(livraison = it)) }
    LigneBoutons {
        Champ(infos.enlevement, "Enlèvement", Modifier.weight(1f), indication = "12/03, 8 h") { maj(infos.copy(enlevement = it)) }
        Champ(infos.retour, "Retour", Modifier.weight(1f), indication = "14/03, 18 h") { maj(infos.copy(retour = it)) }
    }
    Champ(infos.joursTournage.toString(), "Jours de tournage", Modifier.fillMaxWidth()) { t -> t.toIntOrNull()?.takeIf { it in 1..200 }?.let { maj(infos.copy(joursTournage = it)) } }
    Champ(infos.notes, "Précisions (accès, horaires, régie…)", Modifier.fillMaxWidth(), surUneLigne = false) { maj(infos.copy(notes = it)) }
    Spacer(Modifier.height(Interligne))
    Etiquette("Contenu", accent = true)
    Row(Modifier.fillMaxWidth().clickable { avecPlans = !avecPlans }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (avecPlans) "☑" else "☐", style = Corps, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Joindre les plans au sol (${plans.size})", style = Corps)
    }
    Row(Modifier.fillMaxWidth().clickable { avecPrix = !avecPrix }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (avecPrix) "☑" else "☐", style = Corps, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text("Afficher mes prix (estimation)", style = Corps)
    }
    Text("${lignes.size} lignes, rangées par service du loueur.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (infos.manques.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        BandeauEtat("Il manque " + infos.manques.joinToString(", ") + ".", alerte = true)
    }
    Spacer(Modifier.height(Interligne))
    BoutonPlat("Créer le PDF et le partager", accent = true, actif = lignes.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        val nom = Loueurs.loueur(loueurId)?.nom ?: autre.trim()
        message = "Préparation du PDF…"
        portee.launch {
            val f = withContext(Dispatchers.IO) {
                runCatching {
                    ExportPdf.demandeLocation(contexte, nom, infos, lignes, MesTarifs.lire(reglages.mesTarifs), avecPrix, if (avecPlans) plans else emptyList(), metres)
                }.getOrNull()
            }
            message = if (f == null) "Le PDF n'a pas pu être créé." else ""
            f?.let { runCatching { ExportPdf.partager(contexte, it) }.onFailure { message = "Aucune appli n'a pu le recevoir." } }
        }
    }
    if (message.isNotBlank()) Text(message, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

/** La demande de location ouverte directement (menu ⋯ ou Exporter du plan au sol). */
@Composable
internal fun FeuilleDemandeLocation(reglages: Reglages, lignes: List<LigneLocation>, plans: List<PlanSol>, metres: Boolean, plansParDefaut: Boolean, onFermer: () -> Unit) {
    FeuilleLocation("Demande au loueur (PDF)", onFermer) { ContenuDemande(reglages, lignes, plans, metres, plansParDefaut) }
}

@Composable
private fun ContenuTarifs(reglages: Reglages, lignes: List<LigneLocation>) {
    var tarifs by remember { mutableStateOf(MesTarifs.lire(reglages.mesTarifs)) }
    Text(
        "Les loueurs ne publient pas leurs prix. Recopie ceux de tes vrais devis (HT, par jour) : l'estimation s'affine à chaque tournage. Les remises se négocient : l'estimation reste indicative.",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Interligne))
    lignes.map { it.nom }.distinct().forEach { nom ->
        var texte by remember(nom) { mutableStateOf(tarifs[nom]?.let { nombreCourt(it, 2) }.orEmpty()) }
        Champ(texte, nom, Modifier.fillMaxWidth(), indication = "€ HT / jour") { t ->
            texte = t
            val prix = t.replace(',', '.').trim().toDoubleOrNull()
            tarifs = if (prix == null || prix < 0) tarifs - nom else tarifs + (nom to prix)
            reglages.mesTarifs = MesTarifs.ecrire(tarifs)
        }
    }
    val e = MesTarifs.estimer(lignes, tarifs)
    Spacer(Modifier.height(Interligne))
    Ligne("Par jour", nombreCourt(e.parJour, 2) + " € HT", "${e.lignesChiffrees} lignes chiffrées sur ${e.lignesTotal}", accent = e.complete)
}

@Composable
private fun ContenuPointage(reglages: Reglages, jourId: Long, lignes: List<LigneLocation>) {
    var pointage by remember { mutableStateOf(Pointage.pour(lignes, Pointage.lire(reglages.pointage(jourId)))) }
    var retour by remember { mutableStateOf(pointage.any { it.enleve > 0 } && pointage.all { it.manqueAuDepart == 0 }) }
    fun maj(p: List<fr.cellule.core.PointageLigne>) { pointage = p; reglages.noterPointage(jourId, Pointage.ecrire(p)) }
    Text(
        "Compte chaque pièce chez le loueur en la testant (allumer, monter), puis au retour. Note la casse ou le manque tout de suite, avec le loueur.",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
    ChoixSegmente(listOf(false, true), retour, { if (it) "Retour" else "Enlèvement" }) { retour = it }
    Spacer(Modifier.height(6.dp))
    BandeauEtat(Pointage.bilan(pointage), alerte = false)
    pointage.forEachIndexed { i, p ->
        val fait = if (retour) p.rendu else p.enleve
        val max = if (retour) p.enleve.coerceAtLeast(0) else p.prevu
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.nom, style = Corps, color = if (fait >= max && max > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text("$fait / $max" + if (p.note.isNotBlank()) " · ${p.note}" else "", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BoutonPlat("−", actif = fait > 0) {
                maj(pointage.toMutableList().also { it[i] = if (retour) p.copy(rendu = p.rendu - 1) else p.copy(enleve = p.enleve - 1) })
            }
            Spacer(Modifier.width(6.dp))
            BoutonPlat("+", accent = fait < max, actif = fait < max) {
                maj(pointage.toMutableList().also { it[i] = if (retour) p.copy(rendu = p.rendu + 1) else p.copy(enleve = p.enleve + 1) })
            }
        }
    }
    Spacer(Modifier.height(Interligne))
    var note by remember { mutableStateOf("") }
    var cible by remember { mutableStateOf(pointage.firstOrNull()?.nom.orEmpty()) }
    if (pointage.isNotEmpty()) {
        Etiquette("Casse ou manque", accent = true)
        Deroulant("Pièce", pointage.map { it.nom }, cible, { it }, Modifier.fillMaxWidth()) { cible = it }
        Champ(note, "Ce qui ne va pas", Modifier.fillMaxWidth()) { note = it }
        BoutonPlat("Noter", actif = note.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            maj(pointage.map { if (it.nom == cible) it.copy(note = listOf(it.note, note.trim()).filter { n -> n.isNotBlank() }.joinToString(" ; ")) else it })
            note = ""
        }
    }
}
