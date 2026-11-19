package fr.cellule.app.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.IconeChamp
import fr.cellule.app.IconeConversions
import fr.cellule.app.IconeCouleur
import fr.cellule.app.IconeElectricite
import fr.cellule.app.IconeProjecteur
import fr.cellule.app.IconeReviser
import fr.cellule.app.IconeScintillement
import fr.cellule.app.IconeViseur
import fr.cellule.app.Interligne
import fr.cellule.app.LocalPaysage
import fr.cellule.app.Onglet
import fr.cellule.app.RayonCarte
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.QuestionFormule
import fr.cellule.core.QuizFormules
import fr.cellule.core.ThemeFormule

/*
 * Réviser : tous les quiz de l'appli au même endroit. Les exercices sur les
 * formules de Tables › Formules vivent ici, thème par thème ou mélangés ; les
 * autres quiz (caméras, labo, focales) s'ouvrent là où ils sont rangés.
 */

private sealed interface VueReviser {
    data object Sommaire : VueReviser
    data class Formules(val theme: ThemeFormule?, val serie: Boolean) : VueReviser
}

/** Le nombre de questions d'une série. */
private const val SERIE = 10

@Composable
fun EcranReviser(reglages: Reglages, naviguer: (String) -> Unit) {
    var vue by remember { mutableStateOf<VueReviser>(VueReviser.Sommaire) }
    BackHandler(enabled = vue != VueReviser.Sommaire) { vue = VueReviser.Sommaire }
    key(vue) {
        when (val v = vue) {
            VueReviser.Sommaire -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
            ) {
                SommaireReviser(reglages, ouvrir = { vue = it }, naviguer = naviguer)
            }
            is VueReviser.Formules -> SessionFormules(reglages, v.theme, v.serie) { vue = VueReviser.Sommaire }
        }
    }
}

/* ── Les scores, gardés par thème ──────────────────────────────────────── */

private fun lireScores(texte: String): Map<String, Pair<Int, Int>> = texte.split(';').mapNotNull { e ->
    val morceaux = e.split('=')
    if (morceaux.size != 2) return@mapNotNull null
    val nombres = morceaux[1].split('/').mapNotNull { it.toIntOrNull() }
    if (nombres.size != 2) return@mapNotNull null
    morceaux[0] to (nombres[0] to nombres[1])
}.toMap()

private fun noter(reglages: Reglages, theme: ThemeFormule, juste: Boolean) {
    val scores = lireScores(reglages.quizFormules).toMutableMap()
    val (j, p) = scores[theme.name] ?: (0 to 0)
    scores[theme.name] = (j + if (juste) 1 else 0) to (p + 1)
    reglages.quizFormules = scores.entries.joinToString(";") { "${it.key}=${it.value.first}/${it.value.second}" }
}

private fun resumeScore(score: Pair<Int, Int>?): String =
    if (score == null || score.second == 0) "Pas encore commencé"
    else "${score.first} juste${if (score.first > 1) "s" else ""} sur ${score.second}"

/* ── Le sommaire ───────────────────────────────────────────────────────── */

private fun iconeTheme(t: ThemeFormule): ImageVector = when (t) {
    ThemeFormule.EXPOSITION -> Onglet.MESURER.icone
    ThemeFormule.TOURNAGE -> IconeViseur
    ThemeFormule.LUMIERE -> IconeProjecteur
    ThemeFormule.COULEUR -> IconeCouleur
    ThemeFormule.OPTIQUE -> IconeChamp
    ThemeFormule.ELECTRICITE -> IconeElectricite
    ThemeFormule.SCINTILLEMENT -> IconeScintillement
}

/** Un autre quiz de l'appli, rangé ailleurs : la carte y mène. */
private class AutreQuiz(val titre: String, val detail: String, val icone: ImageVector, val destination: String)

@Composable
private fun SommaireReviser(reglages: Reglages, ouvrir: (VueReviser) -> Unit, naviguer: (String) -> Unit) {
    val scores = lireScores(reglages.quizFormules)
    val colonnes = if (LocalPaysage.current) 3 else 2
    val total = scores.values.fold(0 to 0) { acc, s -> (acc.first + s.first) to (acc.second + s.second) }

    Spacer(Modifier.height(8.dp))
    EntreeSerie(
        titre = "Série de $SERIE questions",
        detail = "Toutes les formules mélangées, ton score à la fin." +
            if (total.second > 0) " Depuis le début : ${total.first} justes sur ${total.second}." else ""
    ) { ouvrir(VueReviser.Formules(null, serie = true)) }

    Etiquette("Les formules, thème par thème", Modifier.padding(start = Gouttiere, end = Gouttiere, top = 16.dp, bottom = 4.dp), accent = true)
    Text(
        "Les formules de Tables › Formules, en petits exercices : des valeurs tirées au hasard, quatre réponses, et le calcul détaillé après.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Gouttiere, vertical = 2.dp)
    )
    GrilleSommaire(ThemeFormule.entries.toList() + listOf<ThemeFormule?>(null), colonnes) { t, m ->
        if (t == null) {
            CarteSommaire("Tout mélanger", "Sans fin, tous les thèmes dans le désordre.", IconeConversions, modifier = m) {
                ouvrir(VueReviser.Formules(null, serie = false))
            }
        } else {
            CarteSommaire(t.titre, t.detail, iconeTheme(t), modifier = m, resume = resumeScore(scores[t.name])) {
                ouvrir(VueReviser.Formules(t, serie = false))
            }
        }
    }

    Etiquette("Les autres quiz de l'appli", Modifier.padding(start = Gouttiere, end = Gouttiere, top = 16.dp, bottom = 4.dp), accent = true)
    val autres = listOf(
        AutreQuiz("Caméras et objectifs", "Reconnaître le matériel, à ton niveau.", Onglet.CAMERAS.icone, "quiz"),
        AutreQuiz("Labo", "Bains, dilutions, push, réciprocité.", Onglet.LABO.icone, "labo-quiz"),
        AutreQuiz("Focales", "Deviner la focale, cadrer à l'œil.", Onglet.FOCALES.icone, "focales")
    )
    GrilleSommaire(autres, colonnes) { q, m ->
        CarteSommaire(q.titre, q.detail, q.icone, modifier = m) { naviguer(q.destination) }
    }
}

/** La grande entrée de la série, en tête du sommaire. */
@Composable
private fun EntreeSerie(titre: String, detail: String, ouvrir: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 4.dp)
            .clip(RoundedCornerShape(RayonCarte))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { ouvrir() }
            .padding(Gouttiere),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            IconeReviser, contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(end = 14.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(detail, style = Detail, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        Text("›", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 10.dp))
    }
}

/* ── Une session d'exercices ───────────────────────────────────────────── */

private class EtatSession(val theme: ThemeFormule?, val serie: Boolean) {
    var question by mutableStateOf(QuizFormules.question(theme))
        private set
    var choisi by mutableStateOf<Int?>(null)
        private set
    var indice by mutableStateOf(false)
    var justes by mutableStateOf(0)
        private set
    var posees by mutableStateOf(0)
        private set
    var termine by mutableStateOf(false)

    val repondu get() = choisi != null
    val derniere get() = serie && posees >= SERIE

    fun repondre(i: Int): Boolean? {
        if (choisi != null) return null
        choisi = i
        posees++
        val juste = i == question.bonne
        if (juste) justes++
        return juste
    }

    fun suivante() {
        /* Jamais deux fois de suite la même question. */
        var q: QuestionFormule
        do {
            q = QuizFormules.question(theme)
        } while (q.enonce == question.enonce)
        question = q
        choisi = null
        indice = false
    }

    fun recommencer() {
        justes = 0
        posees = 0
        termine = false
        suivante()
    }
}

@Composable
private fun SessionFormules(reglages: Reglages, theme: ThemeFormule?, serie: Boolean, retour: () -> Unit) {
    val etat = remember { EtatSession(theme, serie) }
    val defilement = rememberScrollState()
    /* Chaque question repart du haut. */
    LaunchedEffect(etat.question, etat.termine) { defilement.scrollTo(0) }
    val titre = theme?.titre ?: if (serie) "Série de $SERIE" else "Tout mélanger"

    Column(Modifier.fillMaxSize().verticalScroll(defilement).padding(bottom = ZoneNavFlottante)) {
        FilAriane(
            chemin = listOf(Miette("Réviser", retour)),
            titre = titre,
            phrase = when {
                etat.termine -> "Série terminée."
                serie -> "Question ${minOf(etat.posees + if (etat.repondu) 0 else 1, SERIE)} sur $SERIE · ${etat.justes} juste${if (etat.justes > 1) "s" else ""}"
                etat.posees > 0 -> "${etat.justes} juste${if (etat.justes > 1) "s" else ""} sur ${etat.posees}"
                else -> theme?.detail ?: "Toutes les formules, dans le désordre."
            },
            retour = retour
        )
        if (etat.termine) {
            FinDeSerie(etat.justes, recommencer = { etat.recommencer() }, retour = retour)
        } else {
            Question(etat) { juste -> noter(reglages, etat.question.theme, juste) }
        }
    }
}

@Composable
private fun Question(etat: EtatSession, surReponse: (Boolean) -> Unit) {
    val q = etat.question
    Carte {
        Etiquette("${q.theme.titre} · ${q.sujet}", accent = true)
        Spacer(Modifier.height(6.dp))
        Text(q.enonce, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        if (q.formule.isNotBlank()) {
            Spacer(Modifier.height(Interligne))
            if (etat.indice || etat.repondu) {
                Text("Formule : ${q.formule}", style = Detail, color = MaterialTheme.colorScheme.primary)
            } else {
                Text(
                    "Voir la formule (indice)",
                    style = Detail,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { etat.indice = true }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 4.dp)) {
        q.options.forEachIndexed { i, option ->
            BoutonPlat(
                option,
                actif = !etat.repondu,
                accent = etat.repondu && i == q.bonne,
                erreur = etat.choisi == i && i != q.bonne,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { etat.repondre(i)?.let(surReponse) }
        }
    }
    if (etat.repondu) {
        val juste = etat.choisi == q.bonne
        CarteResultat(titre = if (juste) "Juste !" else "Pas tout à fait") {
            if (!juste) {
                Text("La bonne réponse : ${q.options[q.bonne]}", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(6.dp))
            }
            Text(q.correction, style = Corps, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        BoutonPlat(
            if (etat.derniere) "Voir mon score" else "Question suivante",
            accent = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 8.dp)
        ) {
            if (etat.derniere) etat.termine = true else etat.suivante()
        }
    }
}

@Composable
private fun FinDeSerie(justes: Int, recommencer: () -> Unit, retour: () -> Unit) {
    val mot = when {
        justes == SERIE -> "Sans faute."
        justes >= 8 -> "Très bien : relis seulement les corrections de tes erreurs."
        justes >= 5 -> "Pas mal : refais une série sur les thèmes qui t'ont piégé."
        else -> "Reprends thème par thème, avec l'indice de la formule."
    }
    CarteResultat(titre = "Série terminée") {
        GrandeValeur("Ton score", "$justes / $SERIE", mot)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 8.dp)) {
        BoutonPlat("Une autre série", accent = true, modifier = Modifier.fillMaxWidth()) { recommencer() }
        Spacer(Modifier.height(8.dp))
        BoutonPlat("Retour aux quiz", modifier = Modifier.fillMaxWidth()) { retour() }
    }
}
