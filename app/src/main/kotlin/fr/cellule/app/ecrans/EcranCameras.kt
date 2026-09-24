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
import fr.cellule.core.GenreQuestion
import fr.cellule.core.Materiel
import fr.cellule.core.Objectif
import fr.cellule.core.Paire
import fr.cellule.core.Question
import fr.cellule.core.QuizMateriel
import fr.cellule.core.chercherMateriel
import kotlin.random.Random

private enum class ModeCameras(val libelle: String) {
    APPRENDRE("Apprendre"), QUIZ("Quiz")
}

private enum class Rayon(val libelle: String) {
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

private class EtatQuiz {
    var rayon by mutableStateOf(Rayon.TOUT)
    var genre by mutableStateOf(GenreQuestion.QUI_SUIS_JE)
    var question by mutableStateOf<Question?>(null)
    var devoiles by mutableStateOf(0)
    var choisi by mutableStateOf<Int?>(null)
    var justes by mutableStateOf(0)
    var posees by mutableStateOf(0)
    private val alea = Random(System.nanoTime())

    init { suivante() }

    fun suivante() {
        question = QuizMateriel.question(genre, rayon.materiel(), alea, eviter = question?.reponse)
        /* « Qui suis-je ? » part d'un premier indice ; « Quel film ? » a déjà le sien, le titre. */
        devoiles = if (genre == GenreQuestion.QUI_SUIS_JE) 1 else 0
        choisi = null
    }

    fun repondre(i: Int) {
        val q = question ?: return
        if (choisi != null) return
        choisi = i
        posees += 1
        if (i == q.bonneReponse) justes += 1
    }
}

/**
 * Caméras et objectifs de cinéma : un catalogue de fiches pour apprendre à
 * quoi sert chaque outil, et un quiz pour vérifier qu'on les reconnaît.
 */
@Composable
fun EcranCameras() {
    var mode by remember { mutableStateOf(ModeCameras.APPRENDRE) }
    var fiche by remember { mutableStateOf<Materiel?>(null) }
    val catalogue = remember { EtatCatalogue() }
    val quiz = remember { EtatQuiz() }

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
            ModeCameras.QUIZ -> Quiz(quiz, surOuvrir = { fiche = it })
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

/* ── Quiz ─────────────────────────────────────────────────────────────── */

@Composable
private fun Quiz(etat: EtatQuiz, surOuvrir: (Materiel) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = ZoneNavFlottante)
    ) {
        Column(Modifier.padding(horizontal = Gouttiere)) {
            ChoixSegmente(
                options = Rayon.entries.toList(),
                selection = etat.rayon,
                libelle = { it.libelle },
                surChoix = { etat.rayon = it; etat.suivante() }
            )
            Spacer(Modifier.height(8.dp))
            ChoixSegmente(
                options = GenreQuestion.entries.toList(),
                selection = etat.genre,
                libelle = { it.libelle },
                surChoix = { etat.genre = it; etat.suivante() }
            )
        }
        Spacer(Modifier.height(6.dp))

        val q = etat.question
        if (q == null) {
            Carte(sousTitre = "Pas assez de fiches pour poser une question ici.") {}
        } else {
            CarteQuestion(q, etat, surOuvrir)
        }

        if (etat.posees > 0) {
            Carte {
                Ligne(
                    "Score", "${etat.justes} / ${etat.posees}",
                    if (etat.posees < 5) "encore un peu court"
                    else "${Math.round(etat.justes * 100.0 / etat.posees)} % de réussite"
                )
            }
        }
    }
}

@Composable
private fun CarteQuestion(q: Question, etat: EtatQuiz, surOuvrir: (Materiel) -> Unit) {
    val choisi = etat.choisi
    val repondu = choisi != null
    /* « Qui suis-je ? » se joue sur la photo ; « Quel film ? » la garde pour
       après la réponse, sinon l'image répondrait à la place de la mémoire. */
    val photo = photoDe(q.reponse, Photos.PLEINE)
    val credit = Photos.credit(LocalContext.current, q.reponse)
    val montrerPhoto = photo != null && (q.genre == GenreQuestion.QUI_SUIS_JE || repondu)
    Carte {
        if (montrerPhoto && photo != null) {
            Image(
                photo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(RayonControle))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            /* Le crédit n'apparaît qu'une fois répondu : l'auteur est parfois
               le fabricant lui-même, ce qui soufflerait la réponse. */
            if (repondu && credit != null) {
                Spacer(Modifier.height(4.dp))
                Text(credit.mention, style = Detail, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    q.reponse.silhouette.image(),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(width = 144.dp, height = 96.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Etiquette(
            if (q.reponse.genre == GenreMateriel.CAMERA) "Une caméra" else "Un objectif",
            accent = true
        )
        Text(q.enonce, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))

        /* Une fois la réponse donnée, tous les indices se découvrent : ils
           deviennent une petite fiche de révision. */
        val visibles = if (repondu) q.indices else q.indices.take(etat.devoiles)
        visibles.forEachIndexed { i, indice ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${i + 1}",
                    style = Detail,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(20.dp)
                )
                Text(indice, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        val reste = q.indices.size - etat.devoiles
        if (!repondu && reste > 0) {
            BoutonPlat(
                if (etat.devoiles == 0) "Un indice" else "Encore un indice ($reste)",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { etat.devoiles += 1 }
        }

        Spacer(Modifier.height(Interligne))
        q.choix.forEachIndexed { i, m ->
            BoutonPlat(
                m.nomComplet,
                actif = !repondu || i == q.bonneReponse,
                accent = repondu && i == q.bonneReponse,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { etat.repondre(i) }
        }

        if (choisi != null) {
            Spacer(Modifier.height(6.dp))
            val juste = choisi == q.bonneReponse
            BandeauEtat(
                if (juste) {
                    val n = etat.devoiles
                    "Juste" + if (n <= 1) " !" else ", avec $n indices."
                } else {
                    "Non : tu as répondu ${q.choix[choisi].nomComplet}."
                },
                alerte = !juste
            )
            Spacer(Modifier.height(8.dp))
            LigneBoutons {
                BoutonPlat("Voir la fiche", modifier = Modifier.weight(1f)) { surOuvrir(q.reponse) }
                BoutonPlat("Suivante", accent = true, modifier = Modifier.weight(1f)) { etat.suivante() }
            }
        }
    }
}
