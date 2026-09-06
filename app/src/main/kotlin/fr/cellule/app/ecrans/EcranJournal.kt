package fr.cellule.app.ecrans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.DepotJournal
import fr.cellule.app.EtatApplication
import fr.cellule.core.EntreeJournal
import fr.cellule.core.Statistiques
import fr.cellule.core.TYPES_DE_SCENE
import java.time.LocalDate

@Composable
fun EcranJournal(depot: DepotJournal, etat: EtatApplication) {

    var type by remember { mutableStateOf(TYPES_DE_SCENE.first()) }
    var annonce by remember { mutableStateOf("") }
    var mesure by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    /* Ce que les autres écrans ont préparé : une estimation à annoncer,
       une mesure à confronter. */
    LaunchedEffect(etat.annonceProposee) {
        etat.annonceProposee?.let {
            annonce = fmt(it).replace(',', '.')
            type = etat.dernierTypeScene
            note = etat.derniereChaine
            etat.annonceProposee = null
            message = "Estimation reprise. Mesure maintenant, puis enregistre."
        }
    }
    LaunchedEffect(etat.derniereMesure) {
        etat.derniereMesure?.let {
            mesure = fmt(it).replace(',', '.')
            etat.derniereMesure = null
            message = "Mesure reprise du posemètre."
        }
    }

    val entrees = depot.entrees
    val bilan = Statistiques.bilan(entrees)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)
    ) {

        Carte(
            titre = "Annoncer, puis mesurer",
            sousTitre = "Le but n'est pas d'avoir juste : c'est de découvrir ton biais. Il est presque toujours dans les ombres."
        ) {
            Deroulant("Scène", TYPES_DE_SCENE, type, { it }) { type = it }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = annonce,
                    onValueChange = { annonce = it },
                    label = { Text("EV annoncé") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = mesure,
                    onValueChange = { mesure = it },
                    label = { Text("EV mesuré") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Chaîne annoncée (facultatif)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                val a = annonce.replace(',', '.').toDoubleOrNull()
                val m = mesure.replace(',', '.').toDoubleOrNull()
                if (a == null || m == null) {
                    message = "Il faut les deux chiffres : ce que tu as annoncé, et ce que la mesure a donné."
                } else {
                    depot.ajouter(
                        EntreeJournal(
                            identifiant = System.currentTimeMillis(),
                            date = LocalDate.now().toString(),
                            typeScene = type,
                            annonce = a,
                            mesure = m,
                            note = note.trim()
                        )
                    )
                    annonce = ""; mesure = ""; note = ""
                    val ecart = a - m
                    message = when {
                        kotlin.math.abs(ecart) < 0.3 -> "Enregistré — écart ${signe(ecart)} diaph. Bien vu."
                        ecart > 0 -> "Enregistré — écart ${signe(ecart)}. Tu as cru qu'il faisait plus clair qu'il ne faisait."
                        else -> "Enregistré — écart ${signe(ecart)}. Tu as cru qu'il faisait plus sombre qu'il ne faisait."
                    }
                }
            }) { Text("Enregistrer") }
            if (message.isNotBlank()) {
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Carte(
            titre = "Ton biais",
            sousTitre = "Écart = annoncé − mesuré. Positif : tu crois qu'il fait plus clair qu'il ne fait, et tu sous-exposes."
        ) {
            if (bilan == null) {
                Text(
                    "Journal vide. Une dizaine d'entrées suffisent déjà à faire sortir un biais.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Text(signe(bilan.biais), fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "  diaph de biais",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Ligne("Entrées", "${bilan.nombre}", if (bilan.nombre < 10) "encore un peu court" else "échantillon utile")
                Ligne("Erreur absolue moyenne", fmt(bilan.erreurAbsolue) + " diaph")
                Ligne("Dans ±½ diaph", "${Math.round(bilan.partDansDemiDiaph * 100)} %", "objectif : 70 %")
                Ligne("Dans ±1 diaph", "${Math.round(bilan.partDansUnDiaph * 100)} %", "objectif : 95 %")

                val parType = Statistiques.parType(entrees)
                if (parType.size > 1) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text("Où ton œil se trompe", style = MaterialTheme.typography.titleSmall)
                    parType.forEach { (nom, b) ->
                        Ligne(nom, signe(b.biais), "${b.nombre} entrées · ${b.verdict}")
                    }
                }
            }
        }

        if (entrees.isNotEmpty()) {
            Carte(titre = "Les dernières entrées") {
                entrees.takeLast(20).reversed().forEach { e ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("${e.date} · ${e.typeScene}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "annoncé ${fmt(e.annonce)} · mesuré ${fmt(e.mesure)} · écart ${signe(e.ecart)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { depot.supprimer(e.identifiant) }) { Text("✕") }
                    }
                }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { depot.vider() }) { Text("Tout effacer") }
            }
        }

        Carte(titre = "Le protocole en trois phases") {
            Ligne(
                "Phase 1 — ancrage",
                "2 à 3 semaines",
                "Annonce un EV devant chaque scène, vérifie, note l'écart. Tu ne cherches pas la justesse, tu cherches ton biais."
            )
            Ligne(
                "Phase 2 — décomposition",
                "1 à 2 mois",
                "Annonce la chaîne, plus le chiffre : « base 15, moins 0,8 pour l'heure, moins 2 pour l'ombre, plus 1 pour l'eau ». C'est la décomposition qui se grave."
            )
            Ligne(
                "Phase 3 — reconnaissance",
                "",
                "Le chiffre arrive sans calcul. Tu ne vérifies plus que sur les scènes ambiguës : mixtes, contre-jour, lumière artificielle mêlée."
            )
        }
    }
}
