package fr.cellule.app.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.DepotCinema
import fr.cellule.app.DepotJournal
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.core.EntreeJournal
import fr.cellule.core.JourCinema
import fr.cellule.core.Plan
import fr.cellule.core.Projet
import fr.cellule.core.TypeJour
import java.time.LocalDate

private fun pluriel(n: Int, mot: String): String = "$n $mot" + if (n > 1) "s" else ""

/** Une ligne cliquable d'une liste de dossiers : intitulé, détail, chevron. */
@Composable
private fun LigneDossier(titre: String, detail: String, surClic: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { surClic() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titre, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            if (detail.isNotBlank()) {
                Text(detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("›", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Un plan dans une liste : vignette, référence, l'essentiel de ses relevés. */
@Composable
fun LignePlan(plan: Plan, surClic: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { surClic() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val vignette = photoDuPlan(plan.photo, 192)
        Box(
            Modifier.size(58.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (vignette != null) {
                Image(vignette, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(plan.intitule, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            val detail = listOfNotNull(
                listOf(plan.date, plan.heure).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() },
                plan.cadre.focaleMm?.let { fr.cellule.core.nombreCourt(it, 1) + " mm" },
                plan.exposition.ouverture.takeIf { it.isNotBlank() },
                plan.lieu?.adresse?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(detail, style = Detail, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("›", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Le retour vers le niveau au-dessus : « ‹ Projets ». */
@Composable
private fun Retour(vers: String, surClic: () -> Unit) {
    Text(
        "‹ $vers",
        style = Corps,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(horizontal = fr.cellule.app.Gouttiere, vertical = 2.dp)
            .clip(RoundedCornerShape(50))
            .clickable { surClic() }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

/** Un bouton qui demande confirmation : un effleurement ne supprime rien. */
@Composable
private fun SuppressionEnDeuxTemps(libelle: String, surSuppression: () -> Unit) {
    var demande by remember { mutableStateOf(false) }
    BoutonPlat(if (demande) "Confirmer la suppression" else libelle, erreur = demande) {
        if (demande) surSuppression() else demande = true
    }
}

/** Le carnet « Cinéma » : Projet › Jour de tournage ou de repérage › Plans. */
@Composable
fun CarnetCinemaEcran(cinema: DepotCinema, journal: DepotJournal, reglages: Reglages) {
    var projetOuvert by remember { mutableStateOf<Long?>(null) }
    var jourOuvert by remember { mutableStateOf<Long?>(null) }
    var planOuvert by remember { mutableStateOf<Long?>(null) }

    val donnees = cinema.donnees
    val projet = donnees.projets.firstOrNull { it.identifiant == projetOuvert }
    val jour = donnees.jours.firstOrNull { it.identifiant == jourOuvert && it.projetId == projet?.identifiant }

    /* Le dossier PDF d'un jour ou d'un projet ; celui du projet cite aussi ses vues photo. */
    val exporteur = rememberExporteurPdf(cinema)
    val exporter: (String, String, List<Plan>, List<JourCinema>) -> Unit = { titre, sousTitre, plans, jours ->
        val vues = if (jour == null && projet != null) {
            val liees = donnees.vuesDe(projet.identifiant).toSet()
            journal.entrees.filter { it.identifiant in liees }.map { e ->
                listOf(e.sujet.ifBlank { "Vue sans sujet" }, e.resume, e.date).filter { it.isNotBlank() }.joinToString(" · ")
            }
        } else emptyList()
        exporteur.dossier(titre, sousTitre, plans, jours, vues)
    }
    if (exporteur.message.isNotBlank()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = fr.cellule.app.Gouttiere, vertical = 4.dp)) {
            BandeauEtat(exporteur.message, alerte = exporteur.echec)
        }
    }

    when {
        projet == null -> ListeProjets(cinema, surProjet = { projetOuvert = it; jourOuvert = null }, surPlan = { planOuvert = it })
        jour == null -> PageProjet(
            cinema, journal, projet,
            retour = { projetOuvert = null },
            surJour = { jourOuvert = it },
            exporter = exporter
        )
        else -> PageJour(
            cinema, reglages, projet, jour,
            retour = { jourOuvert = null },
            surPlan = { planOuvert = it },
            exporter = exporter
        )
    }

    planOuvert?.let { id -> FeuillePlan(cinema, id) { planOuvert = null } }
}

@Composable
private fun ListeProjets(cinema: DepotCinema, surProjet: (Long) -> Unit, surPlan: (Long) -> Unit) {
    val donnees = cinema.donnees
    var nom by remember { mutableStateOf("") }

    Carte(
        titre = "Projets",
        sousTitre = "Un projet rassemble ses jours de tournage et de repérage ; chaque jour, ses plans. Les plans se capturent depuis Mesurer."
    ) {
        if (donnees.projets.isEmpty()) {
            Text(
                "Aucun projet encore. Donne-lui un nom ci-dessous.",
                style = Corps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        donnees.projets.sortedByDescending { it.identifiant }.forEach { p ->
            val jours = donnees.joursDe(p.identifiant)
            LigneDossier(
                p.nom.ifBlank { "Projet sans nom" },
                pluriel(jours.size, "jour") + " · " + pluriel(donnees.plansDuProjet(p.identifiant).size, "plan")
            ) { surProjet(p.identifiant) }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(nom, "Nouveau projet", Modifier.weight(1f), indication = "le titre du film") { nom = it }
            BoutonPlat("Créer", accent = nom.isNotBlank(), actif = nom.isNotBlank()) {
                val p = cinema.ajouterProjet(nom)
                nom = ""
                surProjet(p.identifiant)
            }
        }
    }

    val libres = donnees.plansNonRanges
    if (libres.isNotEmpty()) {
        Carte(
            titre = "Plans non rangés",
            sousTitre = "Ouvre un plan pour le ranger dans un jour."
        ) {
            libres.reversed().forEach { p -> LignePlan(p) { surPlan(p.identifiant) } }
        }
    }
}

@Composable
private fun PageProjet(
    cinema: DepotCinema,
    journal: DepotJournal,
    projet: Projet,
    retour: () -> Unit,
    surJour: (Long) -> Unit,
    exporter: ((String, String, List<Plan>, List<JourCinema>) -> Unit)?
) {
    val donnees = cinema.donnees
    val jours = donnees.joursDe(projet.identifiant)
    val plans = donnees.plansDuProjet(projet.identifiant)
    var nom by remember(projet.identifiant) { mutableStateOf(projet.nom) }
    var nomJour by remember { mutableStateOf("") }
    var dateJour by remember { mutableStateOf(LocalDate.now().toString()) }
    var typeJour by remember { mutableStateOf(TypeJour.TOURNAGE) }

    Retour("Projets", retour)

    Carte(titre = projet.nom.ifBlank { "Projet sans nom" }, sousTitre = pluriel(jours.size, "jour") + " · " + pluriel(plans.size, "plan")) {
        if (jours.isEmpty()) {
            Text("Aucun jour encore.", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        jours.forEach { j ->
            LigneDossier(j.intitule, j.type.libelle + " · " + pluriel(donnees.plansDe(j.identifiant).size, "plan")) {
                surJour(j.identifiant)
            }
        }
        if (exporter != null && plans.isNotEmpty()) {
            Spacer(Modifier.height(Interligne))
            BoutonPlat("Dossier PDF du projet", accent = true, modifier = Modifier.fillMaxWidth()) {
                exporter(projet.nom.ifBlank { "Projet" }, "Projet entier", plans, jours)
            }
        }
    }

    Carte(titre = "Ajouter un jour") {
        ChoixSegmente(TypeJour.entries.toList(), typeJour, { it.libelle }) { typeJour = it }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(nomJour, "Nom", Modifier.weight(1.3f), indication = "Jour 1, la ferme") { nomJour = it }
            Champ(dateJour, "Date", Modifier.weight(1f), indication = "2026-11-16") { dateJour = it }
        }
        Spacer(Modifier.height(Interligne))
        BoutonPlat("Ajouter ce jour", accent = true) {
            val j = cinema.ajouterJour(
                projet.identifiant,
                nomJour.ifBlank { typeJour.libelle + " " + (jours.count { it.type == typeJour } + 1) },
                dateJour, typeJour
            )
            nomJour = ""
            surJour(j.identifiant)
        }
    }

    VuesLiees(cinema, journal, projet)

    Carte(titre = "Le projet") {
        LigneBoutons {
            Champ(nom, "Nom du projet", Modifier.weight(1f)) { nom = it }
            BoutonPlat("Renommer", actif = nom.isNotBlank() && nom.trim() != projet.nom) {
                cinema.renommerProjet(projet.identifiant, nom)
            }
        }
        Spacer(Modifier.height(Interligne))
        SuppressionEnDeuxTemps("Supprimer le projet") {
            cinema.supprimerProjet(projet.identifiant)
            retour()
        }
        Text(
            "Supprimer un projet emporte ses jours. Ses plans, eux, ne sont pas perdus : ils redeviennent « non rangés ».",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Les vues du carnet photo rangées dans le projet : un lien, pas une copie. */
@Composable
private fun VuesLiees(cinema: DepotCinema, journal: DepotJournal, projet: Projet) {
    val donnees = cinema.donnees
    val liees = donnees.vuesDe(projet.identifiant).toSet()
    val vues = journal.entrees.filter { it.identifiant in liees }
    val libres = journal.entrees.filter { donnees.projetDeLaVue(it.identifiant) == null }.reversed().take(60)
    var choix by remember { mutableStateOf(false) }

    fun libelle(e: EntreeJournal): String = listOfNotNull(
        e.sujet.ifBlank { "Vue sans sujet" },
        e.vue.takeIf { it.isNotBlank() }?.let { "vue $it" },
        e.pellicule.takeIf { it.isNotBlank() },
        e.date.takeIf { it.isNotBlank() }
    ).joinToString(" · ")

    Carte(
        titre = "Vues photo du projet",
        sousTitre = "Les vues notées dans le carnet photo peuvent être rangées ici. C'est un lien : la vue reste dans « Vues »."
    ) {
        vues.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(libelle(e), style = Corps, color = MaterialTheme.colorScheme.onSurface)
                    if (e.reglage.isNotBlank()) {
                        Text(e.reglage, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    "retirer",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { cinema.lierVue(e.identifiant, null) }.padding(8.dp)
                )
            }
        }
        if (libres.isEmpty()) {
            Text(
                if (journal.entrees.isEmpty()) "Le carnet photo est vide." else "Toutes les vues sont déjà rangées.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            BoutonPlat("Ranger une vue ici", modifier = Modifier.padding(top = 4.dp)) { choix = true }
        }
    }
    if (choix) {
        FeuilleChoix(
            titre = "Ranger une vue dans ce projet",
            options = libres,
            libelle = { libelle(it) },
            selection = null,
            onDismiss = { choix = false },
            onChoix = {
                cinema.lierVue(it.identifiant, projet.identifiant)
                choix = false
            }
        )
    }
}

@Composable
private fun PageJour(
    cinema: DepotCinema,
    reglages: Reglages,
    projet: Projet,
    jour: JourCinema,
    retour: () -> Unit,
    surPlan: (Long) -> Unit,
    exporter: ((String, String, List<Plan>, List<JourCinema>) -> Unit)?
) {
    val plans = cinema.donnees.plansDe(jour.identifiant)
    var nom by remember(jour.identifiant) { mutableStateOf(jour.nom) }
    var date by remember(jour.identifiant) { mutableStateOf(jour.date) }
    val courant = reglages.cineJourCourant == jour.identifiant.toString()

    Retour(projet.nom.ifBlank { "Projet" }, retour)

    Carte(titre = jour.intitule, sousTitre = jour.type.libelle + " · " + pluriel(plans.size, "plan")) {
        BoutonPlat(
            if (courant) "Les nouveaux plans se rangent ici" else "Ranger ici les nouveaux plans",
            accent = courant,
            modifier = Modifier.fillMaxWidth()
        ) { reglages.cineJourCourant = if (courant) "" else jour.identifiant.toString() }
        Spacer(Modifier.height(Interligne))
        if (plans.isEmpty()) {
            Text(
                "Aucun plan encore. Capture-les depuis Mesurer, ou range ici un plan non rangé.",
                style = Corps,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        plans.forEach { p -> LignePlan(p) { surPlan(p.identifiant) } }
        if (exporter != null && plans.isNotEmpty()) {
            Spacer(Modifier.height(Interligne))
            BoutonPlat("Dossier PDF du jour", accent = true, modifier = Modifier.fillMaxWidth()) {
                exporter(projet.nom.ifBlank { "Projet" }, jour.intitule, plans, listOf(jour))
            }
        }
    }

    ListePlansSol(reglages, cinema, jour.identifiant, tous = false)

    Carte(titre = "Le jour") {
        ChoixSegmente(TypeJour.entries.toList(), jour.type, { it.libelle }) { cinema.modifierJour(jour.copy(type = it)) }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(nom, "Nom", Modifier.weight(1.3f)) { nom = it }
            Champ(date, "Date", Modifier.weight(1f)) { date = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            BoutonPlat("Renommer", actif = nom.trim() != jour.nom || date.trim() != jour.date) {
                cinema.modifierJour(jour.copy(nom = nom.trim(), date = date.trim()))
            }
            SuppressionEnDeuxTemps("Supprimer le jour") {
                if (courant) reglages.cineJourCourant = ""
                cinema.supprimerJour(jour.identifiant)
                retour()
            }
        }
        Text(
            "Supprimer un jour ne supprime pas ses plans : ils redeviennent « non rangés ».",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
