/* ModalBottomSheet est encore annoncé expérimental dans cette version de
   Material3 ; même choix que pour les fiches. */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package fr.cellule.app.ecrans

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreHero
import fr.cellule.app.Corps
import fr.cellule.app.DepotLabo
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonControle
import fr.cellule.app.TitreCarte
import fr.cellule.app.TitreEcran
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.Duree
import fr.cellule.core.FormeCarbonate
import fr.cellule.core.NiveauRecette
import fr.cellule.core.RecetteMaison
import fr.cellule.core.RecettesMaison
import fr.cellule.core.RoleRecette
import fr.cellule.core.SequenceLabo
import kotlinx.coroutines.delay

private val VOLUMES = listOf(300.0, 500.0, 1000.0)

/**
 * Les recettes maison : Caffenol, herbes, thiosulfate, récupération de
 * l'argent. Aucun fabricant ne les publie ; chacune dit donc d'où elle vient
 * et à quel point on peut s'y fier. Les quantités suivent la cuve et la forme
 * de carbonate qu'on a sous la main.
 */
@Composable
fun EcranRecettesMaison(
    labo: DepotLabo,
    versCarnet: (DeveloppementNote) -> Unit,
    versSequence: (SequenceLabo) -> Unit
) {
    var volume by remember { mutableStateOf(300.0) }
    var forme by remember { mutableStateOf(FormeCarbonate.ANHYDRE) }
    var ouverte by remember { mutableStateOf<RecetteMaison?>(null) }

    Carte(
        titre = "Recettes maison",
        sousTitre = "Aucun fabricant derrière ces recettes. Chacune dit d'où elle vient : documentée, essayée par des amateurs, " +
            "ou à tester. Tous les temps sont des points de départ : un essai, puis une note au carnet."
    ) {
        Etiquette("Ta cuve")
        Spacer(Modifier.height(6.dp))
        ChoixSegmente(VOLUMES, volume, { "${it.toInt()} mL" }) { volume = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Ton carbonate de sodium")
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormeCarbonate.entries.forEach { f -> Pastille(f.libelle, accent = f == forme) { forme = f } }
        }
        Spacer(Modifier.height(6.dp))
        Text(forme.conseil, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    NiveauRecette.entries.forEach { n ->
        val liste = RecettesMaison.TOUTES.filter { it.niveau == n }
        Column(Modifier.padding(start = Gouttiere + 6.dp, end = Gouttiere, top = 14.dp, bottom = 2.dp)) {
            Etiquette("${n.libelle} · ${liste.size}", accent = true)
            Text(n.detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        liste.forEach { r -> CarteRecette(r) { ouverte = r } }
    }

    TestAmorce(labo)
    CarteEnchainement()
    CarteAEviter()

    ouverte?.let { r ->
        FeuilleRecette(
            r, volume, forme,
            surFermer = { ouverte = null },
            versCarnet = versCarnet,
            versSequence = { s -> ouverte = null; versSequence(s) }
        )
    }
}

@Composable
private fun CarteRecette(r: RecetteMaison, surAppui: () -> Unit) {
    Carte(modifier = Modifier.clickable { surAppui() }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(r.nom, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Text(r.role.libelle, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = TitreEcran, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(r.resume, style = Corps, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        Text(
            r.temps, style = Detail, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** La recette entière, dans un tiroir, aux quantités de la cuve choisie. */
@Composable
private fun FeuilleRecette(
    r: RecetteMaison,
    volume: Double,
    forme: FormeCarbonate,
    surFermer: () -> Unit,
    versCarnet: (DeveloppementNote) -> Unit,
    versSequence: (SequenceLabo) -> Unit
) {
    val etat = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var message by remember(r) { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = surFermer,
        sheetState = etat,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gouttiere + 2.dp)
                .padding(bottom = Gouttiere)
        ) {
            Etiquette("${r.role.libelle} · ${r.niveau.libelle}", accent = true)
            Text(r.nom, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(r.resume, style = Corps, color = MaterialTheme.colorScheme.onSurface)

            /* Le niveau de confiance, avant tout le reste. */
            Spacer(Modifier.height(Interligne))
            Encadre(if (r.sources.isEmpty()) "À tester" else "D'où elle vient") {
                Text(r.niveau.detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (r.ingredients.isNotEmpty()) {
                Separateur()
                Etiquette(listOf("Pour ${volume.toInt()} mL", r.volumeDe).filter { it.isNotBlank() }.joinToString(" "))
                Spacer(Modifier.height(4.dp))
                Tableau(
                    listOf(Colonne("Ingrédient", 1.6f), Colonne("Quantité", 1f, aDroite = true)),
                    r.ingredients.map { i ->
                        RangTableau(
                            listOf(RecettesMaison.nomIngredient(i, forme), RecettesMaison.libelleQuantite(i, volume, forme)),
                            detail = listOfNotNull(
                                i.note.ifBlank { null },
                                forme.conseil.takeIf { i.carbonate && forme != FormeCarbonate.ANHYDRE }
                            ).joinToString(" · ").ifBlank { null }
                        )
                    }
                )
            }

            Separateur()
            Ligne("Temps", "", r.temps)
            Ligne("Température", r.temperature)
            Ligne("Agitation", "", r.agitationTexte)
            Ligne("Films", "", r.films)

            Separateur()
            Etiquette("Préparation")
            r.preparation.forEachIndexed { i, etape ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text("${i + 1}", style = Detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                    Text(etape, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (r.precautions.isNotEmpty()) {
                Spacer(Modifier.height(Interligne))
                Encadre("Précautions") {
                    r.precautions.forEach { Text("• $it", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            Spacer(Modifier.height(4.dp))
            Depliable("Ce qui change par rapport au guide") {
                r.corrections.forEach {
                    Text("• $it", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 3.dp))
                }
            }

            if (r.role == RoleRecette.REVELATEUR) {
                Spacer(Modifier.height(6.dp))
                LigneBoutons {
                    BoutonPlat("Lancer au chrono", accent = true, modifier = Modifier.weight(1f)) {
                        versSequence(RecettesMaison.sequence(r))
                    }
                    BoutonPlat("Noter au carnet", modifier = Modifier.weight(1f)) {
                        versCarnet(
                            DeveloppementNote(
                                identifiant = 0, date = "",
                                revelateur = r.nom,
                                temperature = 20.0.takeIf { r.temperature == "20 °C" },
                                tempsDonne = r.tempsDepart?.let { it * 60 },
                                agitation = r.agitationTexte,
                                notes = "Recette maison — ${r.niveau.libelle.lowercase()}"
                            )
                        )
                        message = "Préparé : ouvre le Carnet, onglet Films, pour compléter et enregistrer."
                    }
                }
                if (message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    BandeauEtat(message, alerte = false)
                }
            }

            Separateur()
            if (r.sources.isEmpty()) {
                Text(
                    "Aucune source : c'est une piste du guide d'origine, corrigée. Ton carnet d'essais sera sa première source.",
                    style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                r.sources.forEach { LigneSource(it) }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun Encadre(titre: String, contenu: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Etiquette(titre)
        Spacer(Modifier.height(2.dp))
        contenu()
    }
}

/**
 * Le test de l'amorce, chronométré : combien de temps fixer, et quand le
 * fixateur est usé. Le temps du fixateur neuf est gardé d'une séance à l'autre.
 */
@Composable
private fun TestAmorce(labo: DepotLabo) {
    var depart by remember { mutableStateOf<Long?>(null) }
    var mesure by remember { mutableStateOf<Double?>(null) }
    var maintenant by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    /* L'horloge ne tourne que pendant la mesure. */
    LaunchedEffect(depart) {
        while (depart != null) {
            maintenant = SystemClock.elapsedRealtime()
            delay(200)
        }
    }
    val reference = labo.clarificationNeuf.takeIf { it > 0 }

    Carte(
        titre = "Test de l'amorce",
        sousTitre = "Un bout d'amorce dans le fixateur, à la lumière : le temps qu'il met à devenir transparent dit combien fixer, et quand le fixateur est usé."
    ) {
        val d = depart
        if (d != null) {
            Text(Duree.chrono((maintenant - d) / 1000.0), style = ChiffreHero, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            BoutonPlat("Elle est transparente", accent = true, modifier = Modifier.fillMaxWidth()) {
                mesure = (SystemClock.elapsedRealtime() - d) / 1000.0
                depart = null
            }
            return@Carte
        }
        val t = mesure
        if (t != null) {
            Ligne("Temps de clarification", Duree.libelle(t), accent = true)
            Ligne("Fixer", Duree.libelle(2 * t), "deux fois ce temps")
            Ligne("Grain tabulaire", "2 × ${Duree.libelle(t)}", "T-MAX, Delta, Acros, Fomapan 200 : deux bains")
            if (reference != null) {
                val use = t >= 2 * reference
                Spacer(Modifier.height(6.dp))
                BandeauEtat(
                    if (use) "Deux fois plus lent que neuf (${Duree.libelle(reference)}) : ce fixateur est usé."
                    else "Neuf, il mettait ${Duree.libelle(reference)} : encore bon.",
                    alerte = use
                )
            }
            Spacer(Modifier.height(Interligne))
        } else if (reference != null) {
            Ligne("Fixateur neuf", Duree.libelle(reference), "référence gardée d'une séance à l'autre")
            Spacer(Modifier.height(6.dp))
        }
        LigneBoutons {
            BoutonPlat("Plonger l'amorce", accent = t == null, modifier = Modifier.weight(1f)) {
                depart = SystemClock.elapsedRealtime()
                maintenant = depart!!
                mesure = null
            }
            if (t != null) {
                BoutonPlat("C'est un fixateur neuf", modifier = Modifier.weight(1f)) { labo.clarificationNeuf = t }
            }
        }
    }
}

@Composable
private fun CarteEnchainement() {
    Carte(titre = "Le bon enchaînement", sousTitre = "Une séance maison, de la pesée à la note au carnet.") {
        Tableau(
            listOf(Colonne("Étape", 1f)),
            RecettesMaison.ENCHAINEMENT.mapIndexed { i, (etape, detail) -> RangTableau(listOf("${i + 1}. $etape"), detail = detail) }
        )
    }
}

@Composable
private fun CarteAEviter() {
    Carte(titre = "À éviter", sousTitre = "Ce que le guide d'origine proposait, et pourquoi il vaut mieux s'en passer.") {
        RecettesMaison.A_EVITER.forEach { (quoi, pourquoi) -> Ligne(quoi, "", pourquoi) }
    }
}
