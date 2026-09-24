package fr.cellule.app.ecrans

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreEnorme
import fr.cellule.app.Corps
import fr.cellule.app.DepotJournal
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.EntreeJournal
import fr.cellule.core.PELLICULES
import fr.cellule.core.Statistiques
import fr.cellule.core.TYPES_DE_SCENE
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun EcranJournal(depot: DepotJournal, reglages: Reglages, etat: EtatApplication) {

    var pellicule by remember { mutableStateOf(reglages.pellicule) }
    var vue by remember { mutableStateOf("") }
    var sujet by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TYPES_DE_SCENE.first()) }
    var annonce by remember { mutableStateOf("") }
    var mesure by remember { mutableStateOf("") }
    var reglage by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var toutVoir by remember { mutableStateOf(false) }
    var effacementDemande by remember { mutableStateOf(false) }
    var messageSauvegarde by remember { mutableStateOf("") }
    var sauvegardeEchouee by remember { mutableStateOf(false) }

    /* Le fichier est écrit ou lu là où l'utilisateur le choisit, par le
       sélecteur de fichiers d'Android : aucune permission de stockage. */
    val contexte = LocalContext.current
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            contexte.contentResolver.openOutputStream(uri)?.use {
                it.write(depot.versSauvegarde().toByteArray(Charsets.UTF_8))
            } ?: error("fichier inaccessible")
            messageSauvegarde = "Carnet sauvegardé : ${depot.entrees.size} vue(s)."
            sauvegardeEchouee = false
        } catch (e: Exception) {
            messageSauvegarde = "La sauvegarde n'a pas pu être écrite."
            sauvegardeEchouee = true
        }
    }
    val restaurer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val texte = contexte.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("fichier inaccessible")
            val ajoutees = depot.importer(texte)
            messageSauvegarde = if (ajoutees == 0) "Rien de nouveau : ces vues sont déjà dans le carnet."
            else "$ajoutees vue(s) restaurée(s)."
            sauvegardeEchouee = false
            vue = Statistiques.vueSuivante(depot.entrees, pellicule)
        } catch (e: Exception) {
            messageSauvegarde = "Ce fichier n'est pas une sauvegarde du carnet."
            sauvegardeEchouee = true
        }
    }

    val entrees = depot.entrees

    /* Le numéro de vue suit la pellicule chargée, sans qu'on ait à y penser. */
    LaunchedEffect(pellicule, entrees.size) {
        if (vue.isBlank()) vue = Statistiques.vueSuivante(entrees, pellicule)
    }

    /* Ce que les autres écrans ont préparé. */
    LaunchedEffect(etat.annonceProposee) {
        etat.annonceProposee?.let {
            annonce = fmt(it).replace(',', '.')
            type = etat.dernierTypeScene
            if (note.isBlank()) note = etat.derniereChaine
            etat.annonceProposee = null
            message = "Estimation reprise. Mesure maintenant, puis enregistre."
        }
    }
    LaunchedEffect(etat.derniereMesure) {
        etat.derniereMesure?.let {
            mesure = fmt(it).replace(',', '.')
            if (etat.dernierReglage.isNotBlank()) reglage = etat.dernierReglage
            etat.derniereMesure = null
            message = "Mesure reprise du posemètre."
        }
    }

    val bilan = Statistiques.bilan(entrees)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
    ) {

        Spacer(Modifier.height(Interligne))

        /* ── Noter une vue ────────────────────────────────────────────── */
        Carte(
            titre = "Noter une vue",
            sousTitre = "Le sujet et le numéro de vue sont ce qui raccroche la note au négatif, une fois la planche-contact sortie. L'EV annoncé n'est utile que si tu fais l'exercice."
        ) {
            LigneBoutons {
                Deroulant(
                    "Pellicule", PELLICULES.map { it.nom }, pellicule, { it },
                    modifier = Modifier.weight(2f)
                ) {
                    pellicule = it
                    reglages.pellicule = it
                    PELLICULES.firstOrNull { p -> p.nom == it }?.let { p -> reglages.iso = p.iso }
                    vue = Statistiques.vueSuivante(entrees, it)
                }
                Champ(vue, "Vue", Modifier.weight(1f)) { vue = it }
            }
            Spacer(Modifier.height(Interligne))
            Champ(sujet, "Sujet", Modifier.fillMaxWidth(), indication = "la Restonica au soleil rasant") {
                sujet = it
            }
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                Deroulant("Scène", TYPES_DE_SCENE, type, { it }, Modifier.weight(1.4f)) { type = it }
                Champ(reglage, "Réglage", Modifier.weight(1f), indication = "f/8 · 1/500") { reglage = it }
            }
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                Champ(annonce, "EV annoncé", Modifier.weight(1f)) { annonce = it }
                Champ(mesure, "EV mesuré", Modifier.weight(1f)) { mesure = it }
            }
            Spacer(Modifier.height(Interligne))
            Champ(note, "Notes", Modifier.fillMaxWidth(), surUneLigne = false,
                indication = "filtre jaune, développement +1, à retirer") { note = it }

            Spacer(Modifier.height(Interligne + 2.dp))
            LigneBoutons {
                BoutonPlat("Enregistrer la vue", accent = true, modifier = Modifier.weight(1f)) {
                    val a = annonce.replace(',', '.').toDoubleOrNull()
                    val m = mesure.replace(',', '.').toDoubleOrNull()
                    if (sujet.isBlank() && a == null && m == null && note.isBlank()) {
                        message = "Rien à enregistrer : donne au moins un sujet, une note ou une mesure."
                    } else {
                        depot.ajouter(
                            EntreeJournal(
                                identifiant = System.currentTimeMillis(),
                                date = LocalDate.now().toString(),
                                heure = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                                pellicule = pellicule,
                                vue = vue.trim(),
                                sujet = sujet.trim(),
                                typeScene = type,
                                annonce = a,
                                mesure = m,
                                reglage = reglage.trim(),
                                note = note.trim()
                            )
                        )
                        val ecart = if (a != null && m != null) a - m else null
                        message = when {
                            ecart == null -> "Vue $vue enregistrée."
                            kotlin.math.abs(ecart) < 0.3 -> "Vue $vue — écart ${signe(ecart)} diaph. Bien vu."
                            ecart > 0 -> "Vue $vue — écart ${signe(ecart)}. Tu as cru qu'il faisait plus clair qu'il ne faisait."
                            else -> "Vue $vue — écart ${signe(ecart)}. Tu as cru qu'il faisait plus sombre qu'il ne faisait."
                        }
                        sujet = ""; annonce = ""; mesure = ""; note = ""; reglage = ""
                        vue = Statistiques.vueSuivante(depot.entrees, pellicule)
                    }
                }
            }
            Spacer(Modifier.height(Interligne))
            BandeauEtat(message, alerte = message.startsWith("Rien"))
        }

        /* ── Le biais ─────────────────────────────────────────────────── */
        Carte(
            titre = "Ton biais",
            sousTitre = "Écart = annoncé − mesuré. Positif : tu crois qu'il fait plus clair qu'il ne fait, et tu sous-exposes."
        ) {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        bilan?.let { signe(it.biais) } ?: "—",
                        style = ChiffreEnorme,
                        color = if (bilan == null) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(Modifier.weight(1.5f)) {
                    Text(
                        bilan?.verdict ?: "aucune vue annoncée puis mesurée",
                        style = Corps,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (bilan != null) {
                Separateur()
                Ligne("Vues comptées", "${bilan.nombre}", if (bilan.nombre < 10) "encore un peu court" else "échantillon utile")
                Ligne("Erreur absolue moyenne", fmt(bilan.erreurAbsolue) + " diaph")
                Ligne("Dans ±½ diaph", "${Math.round(bilan.partDansDemiDiaph * 100)} %", "objectif : 70 %")
                Ligne("Dans ±1 diaph", "${Math.round(bilan.partDansUnDiaph * 100)} %", "objectif : 95 %")

                val parType = Statistiques.parType(entrees)
                if (parType.size > 1) {
                    Separateur()
                    Etiquette("Où ton œil se trompe")
                    Spacer(Modifier.height(4.dp))
                    parType.forEach { (nom, b) ->
                        Ligne(nom, signe(b.biais), "${b.nombre} vues · ${b.verdict}")
                    }
                }
            }
        }

        /* ── Le carnet ────────────────────────────────────────────────── */
        Carte(titre = "Le carnet", sousTitre = "${entrees.size} vue" + (if (entrees.size > 1) "s" else "") + " notée" + (if (entrees.size > 1) "s" else "")) {
            if (entrees.isEmpty()) {
                Text(
                    "Rien encore. Note une vue et elle apparaîtra ici, la plus récente en haut.",
                    style = Corps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val visibles = if (toutVoir) entrees.reversed() else entrees.reversed().take(8)
                visibles.forEach { e -> LigneCarnet(e) { depot.supprimer(e.identifiant) } }
                Spacer(Modifier.height(Interligne))
                LigneBoutons {
                    if (entrees.size > 8) {
                        BoutonPlat(if (toutVoir) "Réduire" else "Tout voir (${entrees.size})") {
                            toutVoir = !toutVoir
                        }
                    }
                    /* Deux temps, comme pour une vue seule : un effacement
                       complet ne doit jamais tenir à un effleurement. */
                    BoutonPlat(if (effacementDemande) "Confirmer l'effacement" else "Tout effacer") {
                        if (effacementDemande) {
                            depot.vider()
                            effacementDemande = false
                        } else effacementDemande = true
                    }
                }
            }
        }

        /* ── La sauvegarde ────────────────────────────────────────────── */
        Carte(
            titre = "Sauvegarde",
            sousTitre = "Un fichier à ranger où tu veux — Drive, Téléchargements, un mail à toi-même. Restaurer ajoute les vues qui manquent, sans rien écraser ni dédoubler."
        ) {
            LigneBoutons {
                BoutonPlat(
                    "Sauvegarder",
                    accent = true,
                    actif = entrees.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { exporter.launch("carnet-cellule-${LocalDate.now()}.json") }
                BoutonPlat("Restaurer", modifier = Modifier.weight(1f)) {
                    restaurer.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
                }
            }
            Spacer(Modifier.height(Interligne))
            BandeauEtat(messageSauvegarde, alerte = sauvegardeEchouee)
        }
    }
}

/** Une vue du carnet : ce qu'on a photographié, et à quoi on l'a exposé. */
@Composable
private fun LigneCarnet(e: EntreeJournal, surSuppression: () -> Unit) {
    var confirme by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    e.sujet.ifBlank { "Vue sans sujet" },
                    style = Corps,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val entete = listOfNotNull(
                    e.vue.takeIf { it.isNotBlank() }?.let { "vue $it" },
                    e.pellicule.takeIf { it.isNotBlank() },
                    e.date + (if (e.heure.isNotBlank()) " · " + e.heure else "")
                ).joinToString(" · ")
                Text(entete, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .clickable { if (confirme) surSuppression() else confirme = true }
                    .padding(start = 10.dp, top = 2.dp)
            ) {
                Text(
                    if (confirme) "confirmer" else "✕",
                    style = Detail,
                    color = if (confirme) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val lignes = listOfNotNull(
            e.reglage.takeIf { it.isNotBlank() },
            e.ecart?.let { "annoncé ${fmt(e.annonce!!)} · mesuré ${fmt(e.mesure!!)} · écart ${signe(it)}" }
                ?: e.mesure?.let { "mesuré ${fmt(it)}" },
            e.typeScene.takeIf { it.isNotBlank() }
        )
        if (lignes.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                lignes.joinToString("  ·  "),
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (e.note.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(e.note, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
