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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.PlansSol
import fr.cellule.app.RaccordsLumiere
import fr.cellule.app.TitreCarte
import fr.cellule.core.MesureRaccord
import fr.cellule.core.Raccord
import fr.cellule.core.RaccordLumiere
import fr.cellule.core.SaisieMateriel
import fr.cellule.core.SourceRaccord
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * Le raccord lumière d'un plan : le ciel et l'heure, les mesures aux
 * endroits qui comptent, les sources et leurs réglages. Le jour du raccord,
 * on tape la mesure d'aujourd'hui à côté de celle notée : l'écart s'affiche
 * en diaphs, et ce qu'il faut faire.
 */

/** Le résumé et le bouton, dans la fiche d'un plan. */
@Composable
fun BlocRaccordLumiere(planId: Long, jourId: Long?, lux: Boolean) {
    var ouvert by remember { mutableStateOf(false) }
    val raccord = RaccordsLumiere.du(planId)
    Etiquette("Raccord lumière")
    Spacer(Modifier.height(4.dp))
    Text(
        raccord?.let { Raccord.resume(it) } ?: "Pour refaire ce plan à l'identique : le ciel, les mesures, les sources.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    BoutonPlat(if (raccord == null) "Noter le raccord lumière" else "Ouvrir le raccord lumière", modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) { ouvert = true }
    if (ouvert) FeuilleRaccord(planId, jourId, lux) { ouvert = false }
}

@Composable
private fun FeuilleRaccord(planId: Long, jourId: Long?, lux: Boolean, surFermer: () -> Unit) {
    val heure = remember { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) }
    val r = RaccordsLumiere.du(planId) ?: Raccord.nouveau(planId, heure)
    val maj: (RaccordLumiere) -> Unit = { RaccordsLumiere.enregistrer(it) }
    val unite = if (lux) "lx" else "fc"
    fun versLux(v: Double) = if (lux) v else Unites.luxDepuisFc(v)
    fun depuisLux(v: Double) = if (lux) v else Unites.fc(v)
    /* Ajouter ou retirer une ligne change les clés : les champs se relisent. */
    var version by remember { mutableStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = surFermer,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = Gouttiere, end = Gouttiere, bottom = Gouttiere * 2)
        ) {
            Text("Raccord lumière", style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Note-le le jour J ; le jour du raccord, tape tes nouvelles mesures dans « Aujourd'hui » : l'écart s'affiche en diaphs.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = Interligne)
            )
            var heureTexte by remember { mutableStateOf(r.heure) }
            LigneBoutons {
                Champ(heureTexte, "Heure", Modifier.weight(1f)) { heureTexte = it; maj(r.copy(heure = it)) }
                Deroulant("Ciel", listOf("") + Raccord.CIELS, r.ciel, { it.ifBlank { "—" } }, Modifier.weight(1f)) { maj(r.copy(ciel = it)) }
            }

            Separateur()
            Etiquette("Mesures (incident, en $unite)")
            r.mesures.forEachIndexed { i, m ->
                key(version, i) { LigneMesure(m, unite, ::versLux, ::depuisLux,
                    surChangement = { nm -> maj(r.copy(mesures = r.mesures.toMutableList().also { it[i] = nm })) },
                    surRetirer = {
                        maj(r.copy(mesures = r.mesures.filterIndexed { j, _ -> j != i }))
                        version++
                    }
                ) }
            }
            BoutonPlat("+ Une mesure", modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                maj(r.copy(mesures = r.mesures + MesureRaccord("")))
                version++
            }
            Raccord.rapport(r)?.let { Ligne("Face / débouchage", nombreCourt(it, 1) + " : 1", "le contraste à retrouver", accent = true) }

            Separateur()
            Etiquette("Sources")
            r.sources.forEachIndexed { i, s ->
                key(version, "s", i) {
                    var nom by remember { mutableStateOf(s.nom) }
                    var reglage by remember { mutableStateOf(s.reglage) }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Champ(nom, "Source", Modifier.weight(1f)) {
                            nom = it
                            maj(r.copy(sources = r.sources.toMutableList().also { l -> l[i] = s.copy(nom = it) }))
                        }
                        Text(
                            "Retirer",
                            style = Detail,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable {
                                maj(r.copy(sources = r.sources.filterIndexed { j, _ -> j != i }))
                                version++
                            }.padding(horizontal = 10.dp, vertical = 12.dp)
                        )
                    }
                    Champ(reglage, "Réglage : intensité, gélatine, diffusion, hauteur…", Modifier.fillMaxWidth().padding(top = 4.dp), surUneLigne = false) {
                        reglage = it
                        maj(r.copy(sources = r.sources.toMutableList().also { l -> l[i] = s.copy(reglage = it) }))
                    }
                }
            }
            BoutonPlat("+ Une source", modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                maj(r.copy(sources = r.sources + SourceRaccord("")))
                version++
            }
            val plansSol = jourId?.let { PlansSol.duJour(it) }?.takeIf { it.isNotEmpty() } ?: PlansSol.plans
            if (plansSol.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Deroulant("Reprendre les projecteurs d'un plan au sol", listOf<Long?>(null) + plansSol.map { it.identifiant }, null, { id ->
                    if (id == null) "Choisir un plan au sol" else plansSol.first { it.identifiant == id }.nom
                }, Modifier.fillMaxWidth()) { id ->
                    val plan = plansSol.firstOrNull { it.identifiant == id } ?: return@Deroulant
                    val reprises = Raccord.sourcesDuPlan(plan, PlansSol::modele).filter { n -> r.sources.none { it.nom == n.nom } }
                    maj(r.copy(sources = r.sources + reprises, planSolId = plan.identifiant))
                    version++
                }
            }

            Separateur()
            var notes by remember { mutableStateOf(r.notes) }
            Champ(notes, "Notes : rideaux, pratiques allumées, réglage caméra…", Modifier.fillMaxWidth(), surUneLigne = false) {
                notes = it
                maj(r.copy(notes = it))
            }
            Spacer(Modifier.height(Interligne))
            BoutonPlat("OK", accent = true, modifier = Modifier.fillMaxWidth(), surClic = surFermer)
        }
    }
}

/** Une mesure : l'endroit, la valeur notée, celle d'aujourd'hui et l'écart. */
@Composable
private fun LigneMesure(
    m: MesureRaccord,
    unite: String,
    versLux: (Double) -> Double,
    depuisLux: (Double) -> Double,
    surChangement: (MesureRaccord) -> Unit,
    surRetirer: () -> Unit
) {
    var endroit by remember { mutableStateOf(m.endroit) }
    var valeur by remember { mutableStateOf(m.lux?.let { nombreCourt(depuisLux(it), 1) } ?: "") }
    var aujourdHui by remember { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Champ(endroit, "Endroit", Modifier.weight(1f)) {
            endroit = it
            surChangement(m.copy(endroit = it))
        }
        Text(
            "Retirer",
            style = Detail,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable { surRetirer() }.padding(horizontal = 10.dp, vertical = 12.dp)
        )
    }
    LigneBoutons {
        Champ(valeur, "Noté ($unite)", Modifier.weight(1f).padding(top = 4.dp)) {
            valeur = it
            surChangement(m.copy(lux = SaisieMateriel.lireNombre(it)?.takeIf { v -> v > 0 }?.let(versLux)))
        }
        Champ(aujourdHui, "Aujourd'hui ($unite)", Modifier.weight(1f).padding(top = 4.dp)) { aujourdHui = it }
    }
    val reference = m.lux
    val actuel = SaisieMateriel.lireNombre(aujourdHui)?.takeIf { it > 0 }?.let(versLux)
    if (reference != null && actuel != null) {
        val ecart = Raccord.ecart(reference, actuel)
        Text(
            Raccord.conseil(ecart),
            style = Corps,
            color = if (kotlin.math.abs(ecart) < 1.0 / 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
