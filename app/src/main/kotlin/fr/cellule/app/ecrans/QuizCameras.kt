package fr.cellule.app.ecrans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.Corps
import fr.cellule.app.DepotQuiz
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonControle
import fr.cellule.app.StyleEtiquette
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.GenreMateriel
import fr.cellule.core.GenreQuestion
import fr.cellule.core.Materiel
import fr.cellule.core.Niveau
import fr.cellule.core.Question
import fr.cellule.core.QuizMateriel
import kotlin.random.Random

/* L'état vit au niveau de l'écran : passer d'« Apprendre » à « Quiz » pour
   vérifier une fiche ne doit faire perdre ni la question ni le score. */
internal class EtatQuiz(private val depot: DepotQuiz) {
    var rayon by mutableStateOf(Rayon.TOUT)
    var genre by mutableStateOf(GenreQuestion.QUI_SUIS_JE)
    var niveau by mutableStateOf(runCatching { Niveau.valueOf(depot.niveau) }.getOrDefault(Niveau.DECOUVERTE))
        private set
    var question by mutableStateOf<Question?>(null)
    var devoiles by mutableStateOf(0)
    var choisi by mutableStateOf<Int?>(null)

    /** Chronologie : les propositions touchées, dans l'ordre où on les a touchées. */
    val ordreDonne = mutableStateListOf<Int>()
    var justes by mutableStateOf(0)
    var posees by mutableStateOf(0)
    var serie by mutableStateOf(0)

    /** Les dernières réponses, pour la barre de série. */
    val derniers = mutableStateListOf<Boolean>()
    private val alea = Random(System.nanoTime())

    init { suivante() }

    fun changerNiveau(n: Niveau) {
        niveau = n
        depot.niveau = n.name
        suivante()
    }

    fun suivante() {
        question = QuizMateriel.question(
            genre, rayon.materiel(), alea,
            eviter = question?.reponse, niveau = niveau, aRevoir = depot.aRevoir.identifiants
        )
        /* En Découverte, « Qui suis-je ? » offre son premier indice ; « Quel
           film ? » a déjà le sien, le titre. */
        devoiles = if (genre == GenreQuestion.QUI_SUIS_JE && niveau == Niveau.DECOUVERTE) 1 else 0
        choisi = null
        ordreDonne.clear()
    }

    val repondu: Boolean
        get() = when (question?.genre) {
            null -> false
            GenreQuestion.CHRONOLOGIE -> ordreDonne.size == question!!.choix.size
            else -> choisi != null
        }

    val juste: Boolean
        get() {
            val q = question ?: return false
            return if (q.genre == GenreQuestion.CHRONOLOGIE) ordreDonne.toList() == q.ordre
            else choisi == q.bonneReponse
        }

    fun toucher(i: Int) {
        val q = question ?: return
        if (repondu) return
        if (q.genre == GenreQuestion.CHRONOLOGIE) {
            if (i in ordreDonne) return
            ordreDonne.add(i)
            if (ordreDonne.size == q.choix.size) conclure(q)
        } else {
            choisi = i
            conclure(q)
        }
    }

    fun annulerDernier() {
        if (!repondu && ordreDonne.isNotEmpty()) ordreDonne.removeAt(ordreDonne.lastIndex)
    }

    private fun conclure(q: Question) {
        val j = juste
        posees += 1
        if (j) justes += 1
        serie = if (j) serie + 1 else 0
        if (serie > depot.record) depot.record = serie
        derniers.add(j)
        if (derniers.size > 12) derniers.removeAt(0)
        /* Seules les questions sur une fiche nourrissent les révisions et le bilan par marque. */
        if (q.genre == GenreQuestion.QUI_SUIS_JE || q.genre == GenreQuestion.QUEL_FILM) depot.noter(q.reponse, j)
    }
}

private val GenreQuestion.court: String
    get() = when (this) {
        GenreQuestion.QUI_SUIS_JE -> "Qui suis-je ?"
        GenreQuestion.QUEL_FILM -> "Quel film ?"
        GenreQuestion.CHRONOLOGIE -> "Dans l'ordre"
        GenreQuestion.INTRUS -> "L'intrus"
    }

@Composable
internal fun Quiz(etat: EtatQuiz, depot: DepotQuiz, surOuvrir: (Materiel) -> Unit) {
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenreQuestion.entries.forEach { g ->
                    Pastille(g.court, accent = g == etat.genre) { etat.genre = g; etat.suivante() }
                }
            }
            Spacer(Modifier.height(8.dp))
            ChoixSegmente(
                options = Niveau.entries.toList(),
                selection = etat.niveau,
                libelle = { it.libelle },
                surChoix = { etat.changerNiveau(it) }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (etat.niveau == Niveau.EXPERT) "Expert : la silhouette seule, aucun indice d'office ; la photo vient après la réponse."
                else "Découverte : la photo tout de suite, un premier indice offert.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))

        BarreSerie(etat, depot.record)

        val q = etat.question
        if (q == null) {
            Carte(sousTitre = "Pas assez de fiches pour poser cette question ici.") {}
        } else {
            CarteQuestion(q, etat, surOuvrir)
        }

        BilanMarques(depot)
    }
}

/** La série en cours, en cases : on voit d'un coup d'œil si ça s'installe. */
@Composable
private fun BarreSerie(etat: EtatQuiz, record: Int) {
    if (etat.posees == 0 && record == 0) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(12) { i ->
                val r = etat.derniers.getOrNull(i)
                Box(
                    Modifier
                        .size(width = 12.dp, height = 18.dp)
                        .background(
                            when (r) {
                                true -> MaterialTheme.colorScheme.primary
                                false -> MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                                null -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "série ${etat.serie}",
                style = TitreCarte,
                color = if (etat.serie > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "record $record · ${etat.justes}/${etat.posees}",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CarteQuestion(q: Question, etat: EtatQuiz, surOuvrir: (Materiel) -> Unit) {
    val repondu = etat.repondu
    val surFiche = q.genre == GenreQuestion.QUI_SUIS_JE || q.genre == GenreQuestion.QUEL_FILM

    Carte {
        if (surFiche) {
            /* Découverte : « Qui suis-je ? » se joue sur la photo. Expert, et
               toujours pour « Quel film ? » : la silhouette seule, la photo
               après la réponse — sinon l'image répondrait à la place de la mémoire. */
            val photo = photoDe(q.reponse, Photos.PLEINE)
            val credit = Photos.credit(LocalContext.current, q.reponse)
            val montrerPhoto = photo != null &&
                (repondu || (q.genre == GenreQuestion.QUI_SUIS_JE && etat.niveau == Niveau.DECOUVERTE))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(RayonControle))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (montrerPhoto && photo != null) {
                    Image(photo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        q.reponse.silhouette.image(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(width = 180.dp, height = 120.dp)
                    )
                }
                Text(
                    (if (q.reponse.genre == GenreMateriel.CAMERA) "Une caméra" else "Un objectif").uppercase(),
                    style = StyleEtiquette,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            /* Le crédit n'apparaît qu'une fois répondu : l'auteur est parfois
               le fabricant lui-même, ce qui soufflerait la réponse. */
            if (repondu && credit != null && montrerPhoto) {
                Spacer(Modifier.height(4.dp))
                Text(credit.mention, style = Detail, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(Interligne))
        } else {
            Etiquette(q.genre.court, accent = true)
            Spacer(Modifier.height(4.dp))
        }

        Text(q.enonce, style = TitreCarte.copy(fontSize = 19.sp, lineHeight = 24.sp), color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))

        /* Une fois la réponse donnée, tous les indices se découvrent : ils
           deviennent une petite fiche de révision. */
        val visibles = if (repondu) q.indices else q.indices.take(etat.devoiles)
        visibles.forEachIndexed { i, indice ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.padding(top = 1.dp).size(20.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", style = Detail, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(10.dp))
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
        GrilleReponses(q, etat)

        if (q.genre == GenreQuestion.CHRONOLOGIE && !repondu && etat.ordreDonne.isNotEmpty()) {
            BoutonPlat("Annuler le dernier", modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { etat.annulerDernier() }
        }

        if (repondu) {
            Spacer(Modifier.height(Interligne))
            val juste = etat.juste
            BandeauEtat(
                when {
                    juste && q.genre == GenreQuestion.CHRONOLOGIE -> "Juste : le bon ordre."
                    juste -> "Juste" + if (etat.devoiles <= 1) " !" else ", avec ${etat.devoiles} indices."
                    q.genre == GenreQuestion.CHRONOLOGIE -> {
                        val places = etat.ordreDonne.zip(q.ordre).count { (a, b) -> a == b }
                        "Pas tout à fait : $places sur ${q.choix.size} à la bonne place."
                    }
                    else -> "Non : c'était ${q.reponse.nomComplet}."
                },
                alerte = !juste
            )
            if (q.explication.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(q.explication, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(Interligne))
            LigneBoutons {
                if (q.genre != GenreQuestion.CHRONOLOGIE) {
                    BoutonPlat("Voir la fiche", modifier = Modifier.weight(1f)) { surOuvrir(q.reponse) }
                }
                BoutonPlat("Suivante", accent = true, modifier = Modifier.weight(1f)) { etat.suivante() }
            }
        }
    }
}

/**
 * Les réponses en tuiles, deux par rang : de grandes cibles, lisibles au
 * soleil. Pour une chronologie, chaque tuile touchée prend son numéro.
 */
@Composable
private fun GrilleReponses(q: Question, etat: EtatQuiz) {
    val repondu = etat.repondu
    q.choix.indices.chunked(2).forEach { rang ->
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rang.forEach { i ->
                val m = q.choix[i]
                val rangDonne = etat.ordreDonne.indexOf(i).takeIf { it >= 0 }
                val chrono = q.genre == GenreQuestion.CHRONOLOGIE
                val bienPlace = chrono && repondu && q.ordre.getOrNull(rangDonne ?: -1) == i
                val bonne = !chrono && i == q.bonneReponse
                val choisie = !chrono && i == etat.choisi
                val (fond, texte) = when {
                    !repondu && rangDonne != null -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    !repondu -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
                    bonne || bienPlace -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                    choisie || chrono -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .heightIn(min = 76.dp)
                        .alpha(if (repondu && !bonne && !choisie && !chrono) 0.55f else 1f)
                        .clip(RoundedCornerShape(RayonControle))
                        .background(fond)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RayonControle))
                        .clickable(enabled = !repondu) { etat.toucher(i) }
                        .padding(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().align(Alignment.CenterStart)) {
                        Text(m.nomComplet, style = Corps.copy(fontSize = 15.sp), color = texte, maxLines = 3)
                        if (repondu && (chrono || q.genre == GenreQuestion.INTRUS)) {
                            Text(
                                if (chrono) m.libelleAnnee else m.categorie,
                                style = Detail,
                                color = texte.copy(alpha = 0.8f),
                                maxLines = 2
                            )
                        }
                    }
                    if (chrono && (rangDonne != null || repondu)) {
                        val numero = if (repondu) q.ordre.indexOf(i) + 1 else rangDonne!! + 1
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    if (repondu) texte.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$numero",
                                style = Detail,
                                textAlign = TextAlign.Center,
                                color = if (repondu) texte else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Où l'œil reconnaît, où il confond : la réussite par marque, et ce qui reste à revoir. */
@Composable
private fun BilanMarques(depot: DepotQuiz) {
    val classement = depot.bilan.classement()
    val aRevoir = depot.aRevoir.identifiants.size
    if (classement.isEmpty() && aRevoir == 0) return
    Carte(
        titre = "Ton bilan",
        sousTitre = if (aRevoir > 0) "$aRevoir fiche" + (if (aRevoir > 1) "s" else "") +
            " à revoir : elles reviennent jusqu'à ce que tu les aies retrouvées deux fois."
        else "Aucune fiche en attente de révision."
    ) {
        if (classement.isNotEmpty()) {
            Tableau(
                listOf(Colonne("Marque", 1.4f), Colonne("Réussite", 0.9f, aDroite = true), Colonne("Questions", 0.9f, aDroite = true)),
                classement.map {
                    RangTableau(listOf(it.marque, "${Math.round(it.part * 100)} %", "${it.posees}"))
                }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "De la marque la plus confondue à la mieux reconnue — à partir de trois questions.",
                style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
