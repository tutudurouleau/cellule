package fr.cellule.app.ecrans

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.DepotTirages
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Interligne
import fr.cellule.core.Correction
import fr.cellule.core.Duree
import fr.cellule.core.TirageNote
import fr.cellule.core.libelleCorrection
import java.time.LocalDate

/**
 * Le carnet de tirage : papier, filtre, ouverture, temps de base, et les
 * corrections zone par zone, en diaphs. De quoi refaire l'épreuve des mois
 * plus tard — même avec un autre papier ou un autre agrandissement.
 */
@Composable
fun CarnetTirages(depot: DepotTirages, etat: EtatApplication) {
    var negatif by remember { mutableStateOf("") }
    var papier by remember { mutableStateOf("") }
    var filtre by remember { mutableStateOf("") }
    var ouverture by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("") }
    var base by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val corrections = remember { mutableStateListOf<Correction>() }
    var zone by remember { mutableStateOf("") }
    var pas by remember { mutableStateOf(1.0 / 3) }
    var crans by remember { mutableStateOf(1) }

    LaunchedEffect(etat.tiragePropose) {
        etat.tiragePropose?.let { p ->
            p.base?.let { base = fmt(it, 1).removeSuffix(",0") }
            corrections.clear()
            corrections.addAll(p.corrections)
            etat.tiragePropose = null
            message = "Repris du calcul en diaphs. Nomme la zone, complète, puis enregistre."
        }
    }

    val secondes = Duree.lireSecondes(base)?.takeIf { it > 0 }
    val brouillon = TirageNote(0, "", base = secondes, corrections = corrections.toList())

    Carte(
        titre = "Noter un tirage",
        sousTitre = "Les corrections se notent en diaphs : elles survivent à un changement de papier ou d'agrandissement ; seules les secondes se recalculent."
    ) {
        Champ(negatif, "Négatif", Modifier.fillMaxWidth(), indication = "HP5 · film 12 · vue 7") { negatif = it }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(papier, "Papier", Modifier.weight(1.6f), indication = "RC brillant") { papier = it }
            Champ(filtre, "Filtre / grade", Modifier.weight(1f), indication = "3½") { filtre = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(ouverture, "Ouverture", Modifier.weight(1f), indication = "f/8") { ouverture = it }
            Champ(format, "Format", Modifier.weight(1f), indication = "18×24") { format = it }
            Champ(base, "Base (s)", Modifier.weight(1f), indication = "12") { base = it }
        }

        Separateur()
        Etiquette("Corrections")
        corrections.forEachIndexed { i, c ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Ligne(
                        "${c.zone.ifBlank { "Zone" }} ${libelleCorrection(c.diaphs)}",
                        secondes?.let { brouillon.deroule().getOrNull(i + 1)?.substringAfter(": ") } ?: ""
                    )
                }
                Text(
                    "✕", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { corrections.removeAt(i) }.padding(start = 10.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        ChoixSegmente(listOf(1.0 / 3, 0.5), pas, { if (it == 0.5) "par ½ diaph" else "par ⅓ diaph" }) { pas = it }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Champ(zone, "Zone", Modifier.weight(1f), indication = "ciel") { zone = it }
            Spacer(Modifier.width(6.dp))
            BoutonPlat("−") { crans -= 1 }
            Text(
                libelleCorrection(crans * pas).let { if (it == "juste") "0" else it },
                style = Corps, color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            BoutonPlat("+") { crans += 1 }
        }
        Spacer(Modifier.height(6.dp))
        BoutonPlat("Ajouter la correction", actif = crans != 0, modifier = Modifier.fillMaxWidth()) {
            corrections.add(Correction(zone.trim(), crans * pas))
            zone = ""
            crans = 1
        }

        if (secondes != null && corrections.isNotEmpty()) {
            Separateur()
            Etiquette("Au minuteur")
            brouillon.deroule().forEach { Text(it, style = Corps, color = MaterialTheme.colorScheme.onSurface) }
        }
        Spacer(Modifier.height(Interligne))
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false, indication = "virage, séchage, à refaire plus doux…") { notes = it }
        Spacer(Modifier.height(Interligne + 2.dp))
        BoutonPlat("Enregistrer le tirage", accent = true, modifier = Modifier.fillMaxWidth()) {
            if (negatif.isBlank() && secondes == null && corrections.isEmpty()) {
                message = "Rien à enregistrer : donne au moins le négatif, un temps ou une correction."
            } else {
                depot.ajouter(
                    TirageNote(
                        identifiant = System.currentTimeMillis(),
                        date = LocalDate.now().toString(),
                        negatif = negatif.trim(), papier = papier.trim(), filtre = filtre.trim(),
                        ouverture = ouverture.trim(), format = format.trim(), base = secondes,
                        corrections = corrections.toList(), notes = notes.trim()
                    )
                )
                message = "Tirage enregistré."
                corrections.clear()
                notes = ""
            }
        }
        Spacer(Modifier.height(Interligne))
        BandeauEtat(message, alerte = message.startsWith("Rien"))
    }

    Carte(titre = "Les tirages") {
        val liste = depot.tirages
        if (liste.isEmpty()) {
            Text(
                "Rien encore. Depuis Labo › Calculer › Tirage en diaphs, « Verser au carnet » prépare la fiche.",
                style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            liste.reversed().forEach { t -> LigneTirage(t) { depot.supprimer(t.identifiant) } }
        }
    }
}

@Composable
private fun LigneTirage(t: TirageNote, surSuppression: () -> Unit) {
    var confirme by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(t.titre, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    listOf(t.date, t.papier, t.filtre.let { if (it.isBlank()) "" else "filtre $it" }, t.ouverture, t.format)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(Modifier.clickable { if (confirme) surSuppression() else confirme = true }.padding(start = 10.dp, top = 2.dp)) {
                Text(
                    if (confirme) "confirmer" else "✕",
                    style = Detail,
                    color = if (confirme) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val deroule = t.deroule()
        if (deroule.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(deroule.joinToString("\n"), style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (t.corrections.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            Text(
                t.corrections.joinToString(" · ") { "${it.zone} ${libelleCorrection(it.diaphs)}" },
                style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (t.notes.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(t.notes, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
