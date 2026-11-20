/* ModalBottomSheet est encore annoncé expérimental dans cette version de
   Material3 ; même choix que dans Composants.kt. */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package fr.cellule.app.ecrans

import fr.cellule.app.Aiguillage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.rememberLazyListState
import fr.cellule.app.RayonCarte
import androidx.compose.runtime.LaunchedEffect
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
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.app.TitreEcran
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CATALOGUE_CAMERAS
import fr.cellule.core.CATALOGUE_MATERIEL
import fr.cellule.core.CATALOGUE_OBJECTIFS
import fr.cellule.core.ETAGERES_CAMERAS
import fr.cellule.core.ETAGERES_OBJECTIFS
import fr.cellule.core.ETAGERES_PROJECTEURS
import fr.cellule.core.FicheProjecteur
import fr.cellule.core.GenreMateriel
import fr.cellule.core.FicheMachinerie
import fr.cellule.core.ETAGERES_MACHINERIE
import fr.cellule.core.ETAGERES_ELECTRIQUE
import fr.cellule.core.Materiel
import fr.cellule.core.Objectif
import fr.cellule.core.PROJECTEURS_CATALOGUE
import fr.cellule.core.Paire
import fr.cellule.core.Projecteur
import fr.cellule.core.Unites
import fr.cellule.core.chercherMateriel
import fr.cellule.core.nombreCourt

internal enum class Rayon(val libelle: String) {
    CAMERAS("Caméras"), OBJECTIFS("Objectifs"), TOUT("Les deux");

    fun materiel(): List<Materiel> = when (this) {
        CAMERAS -> CATALOGUE_CAMERAS
        OBJECTIFS -> CATALOGUE_OBJECTIFS
        TOUT -> CATALOGUE_MATERIEL
    }
}

/* L'état vit au niveau de l'écran : ouvrir le quiz puis revenir au catalogue
   ne doit faire perdre ni la recherche, ni la question, ni le score. */
private class EtatCatalogue {
    var genre by mutableStateOf(GenreMateriel.CAMERA)
    var recherche by mutableStateOf("")
}

/**
 * Caméras, objectifs et projecteurs de cinéma : un catalogue de fiches pour
 * apprendre à quoi sert chaque outil, en trois onglets, et un quiz à part,
 * ouvert depuis le haut du catalogue, pour vérifier qu'on reconnaît les
 * caméras et les objectifs.
 */
@Composable
fun EcranCameras(depot: DepotQuiz, reglages: Reglages) {
    var quizOuvert by remember { mutableStateOf(false) }
    var fiche by remember { mutableStateOf<Materiel?>(null) }
    val catalogue = remember { EtatCatalogue() }
    val quiz = remember { EtatQuiz(depot) }
    LaunchedEffect(Aiguillage.cible) {
        Aiguillage.prendre("CAMERAS")?.let { c ->
            if (c == "QUIZ") {
                quizOuvert = true
            } else {
                GenreMateriel.entries.firstOrNull { it.name == c }?.let {
                    quizOuvert = false
                    catalogue.genre = it
                }
            }
        }
    }
    /* Le retour du téléphone ramène du quiz au catalogue. */
    BackHandler(enabled = quizOuvert) { quizOuvert = false }

    Column(Modifier.fillMaxSize()) {
        if (quizOuvert) {
            FilAriane(
                chemin = listOf(Miette("Fiches") { quizOuvert = false }),
                titre = "Quiz",
                phrase = "Reconnaître caméras et objectifs, à ton niveau.",
                retour = { quizOuvert = false }
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Quiz(quiz, depot, surOuvrir = { fiche = it })
            }
        } else {
            Onglets(
                options = GenreMateriel.entries.toList(),
                selection = catalogue.genre,
                libelle = {
                    when (it) {
                        GenreMateriel.CAMERA -> "Caméras"
                        GenreMateriel.OBJECTIF -> "Objectifs"
                        GenreMateriel.PROJECTEUR -> "Projecteurs"
                        GenreMateriel.MACHINERIE -> "Machino"
                        GenreMateriel.ELECTRIQUE -> "Élec"
                    }
                },
                surChoix = { catalogue.genre = it }
            )
            Box(Modifier.fillMaxWidth().weight(1f)) {
                Catalogue(catalogue, ouvrirQuiz = { quizOuvert = true }, surOuvrir = { fiche = it })
            }
        }
    }

    fiche?.let { m -> FeuilleFiche(m, reglages, onFermer = { fiche = null }) }
}

/* ── Apprendre ────────────────────────────────────────────────────────── */

@Composable
private fun Catalogue(etat: EtatCatalogue, ouvrirQuiz: () -> Unit, surOuvrir: (Materiel) -> Unit) {
    val etageres = when (etat.genre) {
        GenreMateriel.CAMERA -> ETAGERES_CAMERAS
        GenreMateriel.OBJECTIF -> ETAGERES_OBJECTIFS
        GenreMateriel.PROJECTEUR -> ETAGERES_PROJECTEURS
        GenreMateriel.MACHINERIE -> ETAGERES_MACHINERIE
        GenreMateriel.ELECTRIQUE -> ETAGERES_ELECTRIQUE
    }
    val visibles = remember(etat.genre, etat.recherche) {
        etageres
            .map { it.titre to chercherMateriel(etat.recherche, it.entrees) }
            .filter { it.second.isNotEmpty() }
    }

    /* Chaque onglet repart du haut de sa liste. */
    val liste = rememberLazyListState()
    LaunchedEffect(etat.genre) { liste.scrollToItem(0) }
    LazyColumn(
        Modifier.fillMaxSize(),
        state = liste,
        contentPadding = PaddingValues(top = 6.dp, bottom = ZoneNavFlottante)
    ) {
        item(key = "quiz") { EntreeQuiz(ouvrirQuiz) }
        item(key = "recherche") {
            Column(Modifier.padding(horizontal = Gouttiere)) {
                Spacer(Modifier.height(4.dp))
                Champ(
                    valeur = etat.recherche,
                    intitule = "Chercher",
                    indication = when (etat.genre) {
                        GenreMateriel.PROJECTEUR -> "un nom, une marque, une famille…"
                        GenreMateriel.MACHINERIE, GenreMateriel.ELECTRIQUE -> "un nom, un mot du plateau…"
                        else -> "un nom, une marque, un film, un chef opérateur…"
                    },
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

/** La porte du quiz, en haut du catalogue. */
@Composable
private fun EntreeQuiz(ouvrir: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 4.dp)
            .clip(RoundedCornerShape(RayonCarte))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { ouvrir() }
            .padding(horizontal = Gouttiere, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Tester tes connaissances", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "Le quiz : reconnaître caméras et objectifs, à ton niveau.",
                style = Detail,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
        Text("›", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 10.dp))
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
                listOf(m.libelleAnnee, m.categorie).filter { it.isNotBlank() }.joinToString(" · "),
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

/**
 * La grande fiche. Avec [reglages], les configurations d'un projecteur
 * ouvrent leur fiche photométrique ; sans, elles restent en lecture (c'est
 * le cas quand on vient justement de cette fiche-là).
 */
@Composable
internal fun FeuilleFiche(m: Materiel, reglages: Reglages? = null, onFermer: () -> Unit) {
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
                    Etiquette(listOf(m.genre.libelle, m.fabricant).filter { it.isNotBlank() }.joinToString(" · "), accent = true)
                    Text(m.nomComplet, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        listOf(m.libelleAnnee, m.categorie).filter { it.isNotBlank() }.joinToString(" · "),
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
            if (m is FicheMachinerie) {
                Section("Sur le plateau", m.surLePlateau)
                if (m.aussi.isNotEmpty()) Ligne("On dit aussi", "", m.aussi.joinToString(", "))
            }
            /* La vraie photo, quand l'appli n'en a pas de libre : une recherche d'images, dans le navigateur. */
            LienPhotos(m, avecPhoto = photo != null && credit != null)

            /* La fiche complète. */
            Section("Histoire", m.histoire)
            Section("Fiche technique", m.ficheTechnique)
            Section("Comment l'utiliser", m.commentLUtiliser)
            Section(if (feminin) "Ce qu'elle sait faire" else "Ce qu'il sait faire", m.atouts)
            Section("Ses limites", m.limites)

            if (m is FicheProjecteur) Photometrie(m, reglages)

            if (m.films.isNotEmpty()) {
                Separateur()
                Etiquette("Au générique")
                Spacer(Modifier.height(4.dp))
                m.films.forEach { f -> Ligne(f.titre, "", f.artisan.ifBlank { null }) }
            }

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
private fun LienPhotos(m: Materiel, avecPhoto: Boolean) {
    val liens = androidx.compose.ui.platform.LocalUriHandler.current
    val requete = java.net.URLEncoder.encode(m.nomComplet + if (m.genre == GenreMateriel.MACHINERIE || m.genre == GenreMateriel.ELECTRIQUE) " cinéma" else "", "UTF-8")
    Text(
        if (avecPhoto) "Plus de photos sur le web ↗" else "Voir des photos sur le web ↗",
        style = Detail,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RayonControle))
            .clickable { runCatching { liens.openUri("https://duckduckgo.com/?ia=images&iax=images&q=$requete") } }
            .padding(vertical = 8.dp)
    )
}

/* Les configurations publiées de la famille : une ligne par réflecteur ou
   par modèle, la première valeur du fabricant, et la fiche photométrique
   (tableau aux distances, tache, diaph) d'un toucher. */
@Composable
private fun Photometrie(f: FicheProjecteur, reglages: Reglages?) {
    val configs = f.configurations.mapNotNull { id -> PROJECTEURS_CATALOGUE.firstOrNull { it.identifiant == id } }
    if (configs.isEmpty()) return
    var ouvert by remember { mutableStateOf<Projecteur?>(null) }
    val enLux = reglages?.lumiereLux ?: true
    val metres = reglages?.lumiereMetres ?: true
    fun n(x: Double, d: Int = 0) = nombreCourt(x, d)
    Separateur()
    Etiquette("Photométrie publiée")
    Spacer(Modifier.height(4.dp))
    configs.forEach { p ->
        val m = p.mesure(true) ?: p.mesure(false)
        val valeur = m?.let {
            (if (enLux) n(it.lux) + " lux" else n(Unites.fc(it.lux), 1) + " fc") + " à " +
                (if (metres) n(it.distanceM, 1) + " m" else n(Unites.pieds(it.distanceM), 1) + " ft")
        } ?: "—"
        val detail = listOfNotNull(
            p.angle(m?.spot ?: false)?.let { "faisceau " + n(it) + "°" },
            p.temperatureK?.let { "$it K" },
            if (!p.relu) "à relire" else null
        ).joinToString(" · ").ifBlank { null }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RayonControle))
                .then(if (reglages != null) Modifier.clickable { ouvert = p } else Modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) { Ligne(p.nom, valeur, detail) }
            if (reglages != null) {
                Text(
                    "›",
                    style = Corps,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    if (reglages != null) {
        Text(
            "Touche une ligne pour le tableau aux distances, la tache et le diaph au réglage du Matériel.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (reglages != null) ouvert?.let { FeuilleFicheProjecteur(it, reglages, grandeFiche = false) { ouvert = null } }
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
    if (texte.isBlank()) return
    Separateur()
    Etiquette(titre)
    Spacer(Modifier.height(6.dp))
    Text(texte, style = Corps, color = MaterialTheme.colorScheme.onSurface)
}
