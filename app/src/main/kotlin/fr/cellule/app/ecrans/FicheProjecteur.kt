package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.FiltresJson
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.MonMateriel
import fr.cellule.app.Reglages
import fr.cellule.core.Eclairage
import fr.cellule.core.PROJECTEURS_CATALOGUE
import fr.cellule.core.Projecteur
import fr.cellule.core.Tournage
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt

/*
 * La fiche technique d'un projecteur : ce qui est publié, puis ce qu'on en
 * tire à chaque distance — éclairement, tache, et l'ouverture que ça donne
 * avec le réglage de caméra du Matériel.
 */

private fun n(x: Double, d: Int = 0) = nombreCourt(x, d)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeuilleFicheProjecteur(p: Projecteur, reglages: Reglages, onFermer: () -> Unit) {
    val metres = reglages.lumiereMetres
    val enLux = reglages.lumiereLux
    val reglage = reglageTournage(reglages, FiltresJson.lire(reglages.cineFiltres))
    fun eclair(l: Double) = if (enLux) n(l) + " lux" else n(Unites.fc(l), 1) + " fc"
    fun dist(m: Double, d: Int = 1) = if (metres) n(m, d) + " m" else n(Unites.pieds(m), d) + " ft"
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
                .padding(horizontal = Gouttiere + 2.dp)
                .padding(bottom = Gouttiere * 2)
        ) {
            Text(p.nomComplet, style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Text(
                listOfNotNull(
                    p.famille.libelle,
                    p.puissanceW?.let { n(it) + " W" },
                    p.temperatureK?.let { "$it K" },
                    if (p.perso) "ton matériel" else null
                ).joinToString(" · "),
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!p.relu) {
                Spacer(Modifier.height(6.dp))
                BandeauEtat("À relire : les sources publiées se contredisent ou ne sont pas celles du fabricant.", alerte = true)
            }

            Spacer(Modifier.height(Interligne))
            Etiquette("Publié")
            p.mesures.forEach { m ->
                Ligne(
                    if (m.spot) "Serré (spot)" else "Ouvert (flood)",
                    eclair(m.lux) + " à " + dist(m.distanceM),
                    p.angle(m.spot)?.let { "faisceau " + n(it) + "°" }
                )
            }
            if (p.mesures.isEmpty()) Text("Aucun éclairement saisi.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            p.angleFlood?.let { if (p.mesure(false) == null) Ligne("Faisceau ouvert", n(it) + "°") }
            p.angleSpot?.let { if (p.mesure(true) == null) Ligne("Faisceau serré", n(it) + "°") }

            val distances = if (metres) listOf(1.0, 2.0, 3.0, 4.0, 5.0, 7.0, 10.0) else listOf(3.0, 6.0, 10.0, 15.0, 20.0, 30.0).map { Unites.metres(it) }
            listOf(false, true).forEach { spot ->
                val m = p.mesure(spot) ?: return@forEach
                val i = Eclairage.intensite(m.lux, m.distanceM)
                val angle = p.angle(spot)
                Spacer(Modifier.height(Interligne))
                Etiquette(if (spot) "Serré, calculé" else if (p.mesure(true) != null) "Ouvert, calculé" else "Calculé")
                Tableau(
                    listOfNotNull(
                        Colonne("Distance", 0.8f),
                        Colonne(if (enLux) "Lux" else "fc", 0.8f, aDroite = true),
                        Colonne("T", 0.5f, aDroite = true),
                        if (angle != null) Colonne("Tache", 0.7f, aDroite = true) else null
                    ),
                    distances.map { d ->
                        val lux = Eclairage.lux(i, d)
                        val t = Tournage.ouvertureExacte(Eclairage.ev100(lux), reglage)
                        RangTableau(
                            listOfNotNull(
                                dist(d, 0),
                                if (enLux) n(lux) else n(Unites.fc(lux), 1),
                                Tournage.tStopLePlusProche(t).libelle,
                                angle?.let { "⌀ " + dist(Eclairage.diametre(it, d)) }
                            ),
                            accent = kotlin.math.abs(d - m.distanceM) < 0.01
                        )
                    },
                    petit = true
                )
            }
            Text(
                "Calculé par la loi en 1/d² depuis la valeur publiée, à EI ${reglage.ei} et " + Tournage.libelleTempsPose(reglage.tempsPose) +
                    " (réglage du Matériel). Pour une source douce vue de près, l'éclairement réel décroît moins vite : vérifie à la cellule.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (p.notes.isNotBlank()) {
                Spacer(Modifier.height(Interligne))
                Etiquette("Notes")
                Text(p.notes, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(Interligne))
            Etiquette("Source")
            Text(p.source.ifBlank { "—" }, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Le catalogue des projecteurs, par fabricant, avec leurs fiches. */
@Composable
fun CarteProjecteurs(reglages: Reglages) {
    var ouvert by remember { mutableStateOf<Projecteur?>(null) }
    var deplie by remember { mutableStateOf(false) }
    Carte(
        titre = "Projecteurs",
        sousTitre = "Les fiches techniques : photométrie publiée, et ce qu'elle donne à chaque distance avec ta caméra."
    ) {
        val tous = MonMateriel.projecteurs + PROJECTEURS_CATALOGUE
        val visibles = if (deplie) tous else tous.take(6)
        visibles.groupBy { if (it.perso) "★ Mon matériel" else it.fabricant }.forEach { (fabricant, liste) ->
            Etiquette(fabricant, modifier = Modifier.padding(top = 6.dp))
            liste.forEach { p ->
                Column(Modifier.fillMaxWidth().clickable { ouvert = p }.padding(vertical = 6.dp)) {
                    Text(p.nom, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        listOfNotNull(
                            p.mesures.firstOrNull()?.let { n(it.lux) + " lux à " + n(it.distanceM) + " m" },
                            p.angle(false)?.let { n(it) + "°" },
                            if (!p.relu) "à relire" else null
                        ).joinToString(" · "),
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (tous.size > 6) {
            BoutonPlat(if (deplie) "Replier" else "Tous les projecteurs (${tous.size})", modifier = Modifier.padding(top = 6.dp)) { deplie = !deplie }
        }
    }
    ouvert?.let { FeuilleFicheProjecteur(it, reglages) { ouvert = null } }
}
