/* ModalBottomSheet est encore annoncé expérimental dans cette version de
   Material3 ; même choix que dans Composants.kt. */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.DepotQuiz
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonControle
import fr.cellule.app.TitreCarte
import fr.cellule.app.TitreEcran
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CATALOGUE_CAMERAS
import fr.cellule.core.CATALOGUE_MATERIEL
import fr.cellule.core.CATALOGUE_OBJECTIFS
import fr.cellule.core.ETAGERES_CAMERAS
import fr.cellule.core.ETAGERES_OBJECTIFS
import fr.cellule.core.GenreMateriel
import fr.cellule.core.Materiel
import fr.cellule.core.Objectif
import fr.cellule.core.Paire
import fr.cellule.core.chercherMateriel

private enum class ModeCameras(val libelle: String) {
    APPRENDRE("Apprendre"), QUIZ("Quiz")
}

internal enum class Rayon(val libelle: String) {
    CAMERAS("Caméras"), OBJECTIFS("Objectifs"), TOUT("Les deux");

    fun materiel(): List<Materiel> = when (this) {
        CAMERAS -> CATALOGUE_CAMERAS
        OBJECTIFS -> CATALOGUE_OBJECTIFS
        TOUT -> CATALOGUE_MATERIEL
    }
}

/* L'état vit au niveau de l'écran : passer d'« Apprendre » à « Quiz » pour
   vérifier une fiche ne doit faire perdre ni la question ni le score. */
private class EtatCatalogue {
    var genre by mutableStateOf(GenreMateriel.CAMERA)
    var recherche by mutableStateOf("")
}

/**
 * Caméras et objectifs de cinéma : un catalogue de fiches pour apprendre à
 * quoi sert chaque outil, et un quiz pour vérifier qu'on les reconnaît.
 */
@Composable
fun EcranCameras(depot: DepotQuiz) {
    var mode by remember { mutableStateOf(ModeCameras.APPRENDRE) }
    var fiche by remember { mutableStateOf<Materiel?>(null) }
    val catalogue = remember { EtatCatalogue() }
    val quiz = remember { EtatQuiz(depot) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 6.dp)) {
            ChoixSegmente(
                options = ModeCameras.entries.toList(),
                selection = mode,
                libelle = { it.libelle },
                surChoix = { mode = it }
            )
        }
        when (mode) {
            ModeCameras.APPRENDRE -> Catalogue(catalogue, surOuvrir = { fiche = it })
            ModeCameras.QUIZ -> Quiz(quiz, depot, surOuvrir = { fiche = it })
        }
    }

    fiche?.let { m -> FeuilleFiche(m, onFermer = { fiche = null }) }
}

/* ── Apprendre ────────────────────────────────────────────────────────── */

@Composable
private fun Catalogue(etat: EtatCatalogue, surOuvrir: (Materiel) -> Unit) {
    val etageres = if (etat.genre == GenreMateriel.CAMERA) ETAGERES_CAMERAS else ETAGERES_OBJECTIFS
    val visibles = remember(etat.genre, etat.recherche) {
        etageres
            .map { it.titre to chercherMateriel(etat.recherche, it.entrees) }
            .filter { it.second.isNotEmpty() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = ZoneNavFlottante)
    ) {
        item {
            Column(Modifier.padding(horizontal = Gouttiere)) {
                ChoixSegmente(
                    options = GenreMateriel.entries.toList(),
                    selection = etat.genre,
                    libelle = { if (it == GenreMateriel.CAMERA) "Caméras" else "Objectifs" },
                    surChoix = { etat.genre = it }
                )
                Spacer(Modifier.height(8.dp))
                Champ(
                    valeur = etat.recherche,
                    intitule = "Chercher",
                    indication = "un nom, une marque, un film, un chef opérateur…",
                    modifier = Modifier.fillMaxWidth()
                ) { etat.recherche = it }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (visibles.isEmpty()) {
            item {
                Carte(sousTitre = "Rien ne correspond à « ${etat.recherche.trim()} ».") {}
            }
        }
        items(visibles, key = { it.first }) { (titre, entrees) ->
            Carte(titre = titre) {
                entrees.forEach { m -> EntreeCatalogue(m) { surOuvrir(m) } }
            }
        }
    }
}

@Composable
private fun EntreeCatalogue(m: Materiel, surClic: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RayonControle))
            .clickable(onClick = surClic)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /* La liste garde les silhouettes : les photos sont pour la fiche. */
        Icon(
            m.silhouette.image(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(width = 36.dp, height = 24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                m.nomComplet,
                style = Corps,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${m.libelleAnnee} · ${m.categorie}",
                style = Detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                m.punchline,
                style = Detail,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "›",
            style = Corps,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/* ── La fiche ─────────────────────────────────────────────────────────── */

@Composable
private fun FeuilleFiche(m: Materiel, onFermer: () -> Unit) {
    val etat = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val feminin = m.genre == GenreMateriel.CAMERA
    ModalBottomSheet(
        onDismissRequest = onFermer,
        sheetState = etat,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gouttiere + 2.dp)
                .padding(bottom = Gouttiere)
        ) {
            /* La fiche récap : ce qu'on retient d'un coup d'œil. */
            val photo = photoDe(m, Photos.PLEINE)
            val credit = Photos.credit(LocalContext.current, m)
            if (photo != null && credit != null) {
                Image(
                    photo,
                    contentDescription = m.nomComplet,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(RayonControle))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.height(4.dp))
                Text(credit.mention, style = Detail, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Interligne))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    m.silhouette.image(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(width = 72.dp, height = 48.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Etiquette(m.genre.libelle + " · " + m.fabricant, accent = true)
                    Text(m.nomComplet, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${m.libelleAnnee} · ${m.categorie}",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(Interligne + 2.dp))
            Text(m.punchline, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne + 2.dp))
            GrilleChiffres(m.chiffresCles)
            if (m is Objectif) {
                Ligne("Disponibilité", "", m.disponibilite.libelle)
            }

            /* La fiche complète. */
            Section("Histoire", m.histoire)
            Section("Fiche technique", m.ficheTechnique)
            Section("Comment l'utiliser", m.commentLUtiliser)
            Section(if (feminin) "Ce qu'elle sait faire" else "Ce qu'il sait faire", m.atouts)
            Section("Ses limites", m.limites)

            Separateur()
            Etiquette("Au générique")
            Spacer(Modifier.height(4.dp))
            m.films.forEach { f -> Ligne(f.titre, "", f.artisan.ifBlank { null }) }

            Spacer(Modifier.height(Interligne))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(RayonControle))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Etiquette("À retenir", accent = true)
                Spacer(Modifier.height(4.dp))
                Text(m.aRetenir, style = Corps, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun GrilleChiffres(chiffres: List<Paire>) {
    chiffres.chunked(2).forEach { ligne ->
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ligne.forEach { p ->
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Etiquette(p.libelle)
                    Spacer(Modifier.height(3.dp))
                    Text(p.valeur, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (ligne.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Section(titre: String, texte: String) {
    Separateur()
    Etiquette(titre)
    Spacer(Modifier.height(6.dp))
    Text(texte, style = Corps, color = MaterialTheme.colorScheme.onSurface)
}
