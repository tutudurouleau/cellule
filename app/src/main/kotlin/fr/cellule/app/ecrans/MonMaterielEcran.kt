package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.MonMateriel
import fr.cellule.core.CameraPerso
import fr.cellule.core.DESANAMORPHOSES
import fr.cellule.core.FamilleProjecteur
import fr.cellule.core.MesureProjecteur
import fr.cellule.core.Projecteur
import fr.cellule.core.SaisieMateriel
import fr.cellule.core.SeriePerso
import fr.cellule.core.ZoneUtile
import fr.cellule.core.nombreCourt

/*
 * « Mon matériel » : ses caméras, ses séries d'objectifs et ses projecteurs,
 * quand ils ne sont pas dans le catalogue. Ils se choisissent ensuite comme
 * les autres ; leurs chiffres portent la mention « saisi par toi ».
 */

private fun texte(x: Double?, decimales: Int = 2): String = x?.let { nombreCourt(it, decimales) } ?: ""

/** Ce qu'on édite : un élément existant, ou un nouveau. */
private sealed interface Edition {
    data class Camera(val c: CameraPerso?) : Edition
    data class Serie(val s: SeriePerso?) : Edition
    data class Lumiere(val p: Projecteur?) : Edition
}

@Composable
fun CarteMonMateriel() {
    var edition by remember { mutableStateOf<Edition?>(null) }
    Carte(
        titre = "Mon matériel",
        sousTitre = "Ta caméra, tes optiques, tes projecteurs, s'ils ne sont pas dans le catalogue. Ils se choisissent ensuite comme les autres, et voyagent dans la sauvegarde du carnet."
    ) {
        SectionMateriel("Caméras", MonMateriel.cameras.map { c ->
            c.nom to (if (c.modes.isEmpty()) "aucun mode de capteur" else c.modes.joinToString(" · ") { it.nom.ifBlank { it.dimensions } })
        }, surElement = { i -> edition = Edition.Camera(MonMateriel.cameras[i]) }, ajouter = "Ajouter une caméra") {
            edition = Edition.Camera(null)
        }
        Separateur()
        SectionMateriel("Séries d'objectifs", MonMateriel.series.map { s ->
            s.nom to listOfNotNull(
                s.focales.takeIf { it.isNotEmpty() }?.joinToString(", ") { nombreCourt(it, 1) }?.let { "$it mm" },
                s.tMax?.let { "T" + nombreCourt(it, 1) },
                if (s.desanamorphose != 1.0) "×" + nombreCourt(s.desanamorphose, 1) else null
            ).joinToString(" · ")
        }, surElement = { i -> edition = Edition.Serie(MonMateriel.series[i]) }, ajouter = "Ajouter une série") {
            edition = Edition.Serie(null)
        }
        Separateur()
        SectionMateriel("Projecteurs", MonMateriel.projecteurs.map { p ->
            p.nomComplet to listOfNotNull(
                p.famille.libelle,
                p.puissanceW?.let { nombreCourt(it, 0) + " W" },
                p.temperatureK?.let { "$it K" }
            ).joinToString(" · ")
        }, surElement = { i -> edition = Edition.Lumiere(MonMateriel.projecteurs[i]) }, ajouter = "Ajouter un projecteur") {
            edition = Edition.Lumiere(null)
        }
    }
    when (val e = edition) {
        is Edition.Camera -> FeuilleCameraPerso(e.c) { edition = null }
        is Edition.Serie -> FeuilleSeriePerso(e.s) { edition = null }
        is Edition.Lumiere -> FeuilleProjecteurPerso(e.p) { edition = null }
        null -> {}
    }
}

@Composable
private fun ColumnScope.SectionMateriel(
    titre: String,
    elements: List<Pair<String, String>>,
    surElement: (Int) -> Unit,
    ajouter: String,
    surAjout: () -> Unit
) {
    Etiquette(titre)
    Spacer(Modifier.height(4.dp))
    if (elements.isEmpty()) {
        Text("Rien pour l'instant.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    elements.forEachIndexed { i, (nom, detail) ->
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { surElement(i) }
                .padding(vertical = 6.dp)
        ) {
            Text(nom.ifBlank { "Sans nom" }, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            if (detail.isNotBlank()) Text(detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    BoutonPlat(ajouter, modifier = Modifier.padding(top = 4.dp), surClic = surAjout)
}

/* ── Les feuilles d'édition ────────────────────────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeuilleEdition(titre: String, onFermer: () -> Unit, contenu: @Composable ColumnScope.() -> Unit) {
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
                .padding(horizontal = Gouttiere + 2.dp, vertical = 6.dp)
        ) {
            Text(titre, style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne))
            contenu()
            Spacer(Modifier.height(Gouttiere * 2))
        }
    }
}

/** Enregistrer, et supprimer en deux temps pour un élément existant. */
@Composable
private fun BoutonsEdition(existant: Boolean, valide: Boolean, enregistrer: () -> Unit, supprimer: () -> Unit) {
    var confirmer by remember { mutableStateOf(false) }
    Spacer(Modifier.height(Interligne + 4.dp))
    LigneBoutons {
        BoutonPlat("Enregistrer", actif = valide, accent = valide, modifier = Modifier.weight(1f)) { if (valide) enregistrer() }
        if (existant) {
            BoutonPlat(if (confirmer) "Confirmer" else "Supprimer", erreur = confirmer) {
                if (confirmer) supprimer() else confirmer = true
            }
        }
    }
}

private class ModeSaisi(nom: String, largeur: String, hauteur: String) {
    var nom by mutableStateOf(nom)
    var largeur by mutableStateOf(largeur)
    var hauteur by mutableStateOf(hauteur)
}

@Composable
private fun FeuilleCameraPerso(existante: CameraPerso?, onFermer: () -> Unit) {
    var nom by remember { mutableStateOf(existante?.nom ?: "") }
    var notes by remember { mutableStateOf(existante?.notes ?: "") }
    val modes = remember {
        mutableStateListOf<ModeSaisi>().apply {
            existante?.modes?.forEach { add(ModeSaisi(it.nom, texte(it.largeurMm), texte(it.hauteurMm))) }
            if (isEmpty()) add(ModeSaisi("", "", ""))
        }
    }
    FeuilleEdition(if (existante == null) "Nouvelle caméra" else "Ma caméra", onFermer) {
        Champ(nom, "Nom", Modifier.fillMaxWidth(), indication = "ex. Bolex H16, ma FX3") { nom = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Modes de capteur")
        Text(
            "La zone réellement enregistrée, en millimètres, telle que la donne la fiche de ta caméra. Pour une caméra argentique, la fenêtre de prise de vues.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        modes.forEachIndexed { i, m ->
            Spacer(Modifier.height(8.dp))
            Champ(m.nom, "Mode ${i + 1}", Modifier.fillMaxWidth(), indication = "ex. Open Gate, 16:9 4K") { m.nom = it }
            Spacer(Modifier.height(6.dp))
            LigneBoutons {
                Champ(m.largeur, "Largeur (mm)", Modifier.weight(1f)) { m.largeur = it }
                Champ(m.hauteur, "Hauteur (mm)", Modifier.weight(1f)) { m.hauteur = it }
                if (modes.size > 1) {
                    Box(
                        Modifier
                            .align(Alignment.CenterVertically)
                            .clickable { modes.removeAt(i) }
                            .padding(10.dp)
                    ) { Text("✕", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        BoutonPlat("Ajouter un mode", modifier = Modifier.padding(top = 8.dp)) { modes.add(ModeSaisi("", "", "")) }
        Spacer(Modifier.height(Interligne))
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false) { notes = it }

        val lus = modes.mapNotNull { m ->
            val l = SaisieMateriel.lireNombre(m.largeur) ?: return@mapNotNull null
            val h = SaisieMateriel.lireNombre(m.hauteur) ?: return@mapNotNull null
            ZoneUtile(m.nom.trim().ifBlank { "Mode" }, maxOf(l, h), minOf(l, h), MonMateriel.SAISI)
        }
        BoutonsEdition(
            existant = existante != null,
            valide = nom.isNotBlank(),
            enregistrer = {
                MonMateriel.enregistrer(
                    CameraPerso(existante?.identifiant ?: System.currentTimeMillis(), nom.trim(), lus, notes.trim())
                )
                onFermer()
            },
            supprimer = {
                existante?.let { MonMateriel.supprimerCamera(it.identifiant) }
                onFermer()
            }
        )
    }
}

@Composable
private fun FeuilleSeriePerso(existante: SeriePerso?, onFermer: () -> Unit) {
    var nom by remember { mutableStateOf(existante?.nom ?: "") }
    var focales by remember { mutableStateOf(existante?.focales?.joinToString(", ") { nombreCourt(it, 1) } ?: "") }
    var tMax by remember { mutableStateOf(texte(existante?.tMax, 1)) }
    var desanamorphose by remember { mutableStateOf(existante?.desanamorphose ?: 1.0) }
    var notes by remember { mutableStateOf(existante?.notes ?: "") }
    val lues = SaisieMateriel.lireFocales(focales)
    FeuilleEdition(if (existante == null) "Nouvelle série" else "Ma série", onFermer) {
        Champ(nom, "Nom", Modifier.fillMaxWidth(), indication = "ex. Samyang VDSLR, Helios 44") { nom = it }
        Spacer(Modifier.height(Interligne))
        Champ(focales, "Focales (mm)", Modifier.fillMaxWidth(), indication = "18, 25, 35, 50, 85") { focales = it }
        if (lues.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                lues.forEach { Pastille(nombreCourt(it, 1) + " mm") }
            }
        }
        Text(
            "Elles deviennent les pastilles de focale du viseur.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(tMax, "T max", Modifier.weight(1f), indication = "1,5") { tMax = it }
            Deroulant(
                "Désanamorphose", DESANAMORPHOSES, desanamorphose,
                { if (it == 1.0) "×1 (sphérique)" else "×" + nombreCourt(it, 1) }, Modifier.weight(1f)
            ) { desanamorphose = it }
        }
        Spacer(Modifier.height(Interligne))
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false) { notes = it }
        BoutonsEdition(
            existant = existante != null,
            valide = nom.isNotBlank(),
            enregistrer = {
                MonMateriel.enregistrer(
                    SeriePerso(
                        existante?.identifiant ?: System.currentTimeMillis(), nom.trim(), lues,
                        SaisieMateriel.lireNombre(tMax), desanamorphose, notes.trim()
                    )
                )
                onFermer()
            },
            supprimer = {
                existante?.let { MonMateriel.supprimerSerie(it.identifiant) }
                onFermer()
            }
        )
    }
}

@Composable
private fun FeuilleProjecteurPerso(existant: Projecteur?, onFermer: () -> Unit) {
    var nom by remember { mutableStateOf(existant?.nom ?: "") }
    var fabricant by remember { mutableStateOf(existant?.fabricant ?: "") }
    var famille by remember { mutableStateOf(existant?.famille ?: FamilleProjecteur.PANNEAU_LED) }
    var puissance by remember { mutableStateOf(texte(existant?.puissanceW, 0)) }
    var kelvins by remember { mutableStateOf(existant?.temperatureK?.toString() ?: "") }
    var angleFlood by remember { mutableStateOf(texte(existant?.angleFlood, 1)) }
    var angleSpot by remember { mutableStateOf(texte(existant?.angleSpot, 1)) }
    var luxFlood by remember { mutableStateOf(texte(existant?.mesure(false)?.lux, 0)) }
    var distFlood by remember { mutableStateOf(texte(existant?.mesure(false)?.distanceM, 2)) }
    var luxSpot by remember { mutableStateOf(texte(existant?.mesure(true)?.lux, 0)) }
    var distSpot by remember { mutableStateOf(texte(existant?.mesure(true)?.distanceM, 2)) }
    var notes by remember { mutableStateOf(existant?.notes ?: "") }

    FeuilleEdition(if (existant == null) "Nouveau projecteur" else "Mon projecteur", onFermer) {
        Champ(nom, "Nom", Modifier.fillMaxWidth(), indication = "ex. mon panneau bicolore") { nom = it }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(fabricant, "Fabricant", Modifier.weight(1f)) { fabricant = it }
            Deroulant("Famille", FamilleProjecteur.entries.toList(), famille, { it.libelle }, Modifier.weight(1f)) { famille = it }
        }
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Champ(puissance, "Puissance (W)", Modifier.weight(1f)) { puissance = it }
            Champ(kelvins, "Couleur (K)", Modifier.weight(1f), indication = "5600") { kelvins = it }
        }
        Spacer(Modifier.height(Interligne))
        Etiquette("Faisceau")
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            Champ(angleFlood, "Ouvert (°)", Modifier.weight(1f)) { angleFlood = it }
            Champ(angleSpot, "Serré (°)", Modifier.weight(1f)) { angleSpot = it }
        }
        Spacer(Modifier.height(Interligne))
        Etiquette("Éclairement")
        Text(
            "Lu sur le tableau photométrique de la fiche, ou mesuré avec la cellule en incident à une distance connue, dans l'axe. Sans lui, le projecteur se range mais ne sert pas aux calculs de lux.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            Champ(luxFlood, "Ouvert : lux", Modifier.weight(1f)) { luxFlood = it }
            Champ(distFlood, "à (m)", Modifier.weight(1f)) { distFlood = it }
        }
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            Champ(luxSpot, "Serré : lux", Modifier.weight(1f)) { luxSpot = it }
            Champ(distSpot, "à (m)", Modifier.weight(1f)) { distSpot = it }
        }
        Spacer(Modifier.height(Interligne))
        Champ(notes, "Notes", Modifier.fillMaxWidth(), surUneLigne = false) { notes = it }

        val mesures = listOfNotNull(
            SaisieMateriel.lireNombre(luxFlood)?.let { l -> SaisieMateriel.lireNombre(distFlood)?.let { d -> MesureProjecteur(false, d, l) } },
            SaisieMateriel.lireNombre(luxSpot)?.let { l -> SaisieMateriel.lireNombre(distSpot)?.let { d -> MesureProjecteur(true, d, l) } }
        )
        BoutonsEdition(
            existant = existant != null,
            valide = nom.isNotBlank(),
            enregistrer = {
                MonMateriel.enregistrer(
                    Projecteur(
                        identifiant = existant?.identifiant ?: ("perso-" + System.currentTimeMillis()),
                        nom = nom.trim(),
                        fabricant = fabricant.trim(),
                        famille = famille,
                        puissanceW = SaisieMateriel.lireNombre(puissance),
                        temperatureK = SaisieMateriel.lireNombre(kelvins)?.let { Math.round(it).toInt() },
                        angleFlood = SaisieMateriel.lireNombre(angleFlood)?.takeIf { it < 180 },
                        angleSpot = SaisieMateriel.lireNombre(angleSpot)?.takeIf { it < 180 },
                        mesures = mesures,
                        notes = notes.trim()
                    )
                )
                onFermer()
            },
            supprimer = {
                existant?.let { MonMateriel.supprimerProjecteur(it.identifiant) }
                onFermer()
            }
        )
    }
}
