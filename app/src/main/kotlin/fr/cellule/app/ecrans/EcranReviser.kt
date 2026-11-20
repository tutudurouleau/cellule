package fr.cellule.app.ecrans

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.IconeChamp
import fr.cellule.app.IconeConversions
import fr.cellule.app.IconeCouleur
import fr.cellule.app.IconeElectricite
import fr.cellule.app.IconeMateriel
import fr.cellule.app.IconePlans
import fr.cellule.app.IconeProjecteur
import fr.cellule.app.IconeReviser
import fr.cellule.app.IconeScintillement
import fr.cellule.app.IconeViseur
import fr.cellule.app.Interligne
import fr.cellule.app.LocalPaysage
import fr.cellule.app.Onglet
import fr.cellule.app.RayonCarte
import fr.cellule.app.RayonControle
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.FicheFormule
import fr.cellule.core.FichesFormules
import fr.cellule.core.QuestionFormule
import fr.cellule.core.QuizFormules
import fr.cellule.core.SaisieReponse
import fr.cellule.core.ThemeFormule

/*
 * Réviser : tous les quiz de l'appli au même endroit. Chaque thème de
 * formules a deux onglets — Apprendre (les fiches, à lire ou à réciter) et
 * S'entraîner (les exercices, à choix ou à taper). Les formules ratées
 * reviennent dans « À revoir » jusqu'à deux bonnes réponses. Les autres quiz
 * (caméras, labo, focales) s'ouvrent là où ils sont rangés.
 */

/** D'où viennent les questions d'une session. */
private sealed interface Source {
    data class Theme(val theme: ThemeFormule) : Source
    data object Melange : Source
    data object Serie : Source
    data object ARevoir : Source
}

private sealed interface VueReviser {
    data object Sommaire : VueReviser
    data class Theme(val theme: ThemeFormule) : VueReviser
    data class Session(val source: Source) : VueReviser
    data object Lexique : VueReviser
    data object Marques : VueReviser
}

/** Le nombre de questions d'une série. */
private const val SERIE = 10

/** Une formule ratée revient jusqu'à ce nombre de bonnes réponses. */
private const val RAPPELS = 2

@Composable
fun EcranReviser(reglages: Reglages, naviguer: (String) -> Unit) {
    var vue by remember { mutableStateOf<VueReviser>(VueReviser.Sommaire) }
    val retour = { vue = VueReviser.Sommaire }
    BackHandler(enabled = vue != VueReviser.Sommaire, onBack = retour)
    /* Depuis le lanceur : « REVISER:LEXIQUE », « REVISER:MARQUES ». */
    androidx.compose.runtime.LaunchedEffect(fr.cellule.app.Aiguillage.cible) {
        when (fr.cellule.app.Aiguillage.prendre("REVISER")) {
            "LEXIQUE" -> vue = VueReviser.Lexique
            "MARQUES" -> vue = VueReviser.Marques
        }
    }
    key(vue) {
        when (val v = vue) {
            VueReviser.Sommaire -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
            ) {
                SommaireReviser(reglages, ouvrir = { vue = it }, naviguer = naviguer)
            }
            is VueReviser.Theme -> PageTheme(reglages, v.theme, retour)
            is VueReviser.Session -> PageSession(reglages, v.source, retour)
            VueReviser.Lexique -> PageLexique(reglages, retour)
            VueReviser.Marques -> PageMarques(retour)
        }
    }
}

/* ── Ce qu'on garde : scores par thème, formules à revoir ──────────────── */

private fun lireScores(texte: String): Map<String, Pair<Int, Int>> = texte.split(';').mapNotNull { e ->
    val morceaux = e.split('=')
    if (morceaux.size != 2) return@mapNotNull null
    val nombres = morceaux[1].split('/').mapNotNull { it.toIntOrNull() }
    if (nombres.size != 2) return@mapNotNull null
    morceaux[0] to (nombres[0] to nombres[1])
}.toMap()

private fun noterScore(reglages: Reglages, theme: ThemeFormule, juste: Boolean) {
    val scores = lireScores(reglages.quizFormules).toMutableMap()
    val (j, p) = scores[theme.name] ?: (0 to 0)
    scores[theme.name] = (j + if (juste) 1 else 0) to (p + 1)
    reglages.quizFormules = scores.entries.joinToString(";") { "${it.key}=${it.value.first}/${it.value.second}" }
}

private fun lireARevoir(texte: String): Map<Pair<ThemeFormule, String>, Int> = texte.split(';').mapNotNull { e ->
    val morceaux = e.split('=')
    if (morceaux.size != 2) return@mapNotNull null
    val n = morceaux[1].toIntOrNull() ?: return@mapNotNull null
    val cle = morceaux[0].split('|')
    if (cle.size != 2) return@mapNotNull null
    val theme = ThemeFormule.entries.firstOrNull { it.name == cle[0] } ?: return@mapNotNull null
    (theme to cle[1]) to n
}.toMap()

/** Une erreur remet la formule à revoir ; une bonne réponse la rapproche de la sortie. */
private fun suivreARevoir(reglages: Reglages, theme: ThemeFormule, sujet: String, juste: Boolean) {
    val liste = lireARevoir(reglages.quizARevoir).toMutableMap()
    val cle = theme to sujet
    if (!juste) {
        liste[cle] = RAPPELS
    } else {
        val n = liste[cle] ?: return
        if (n <= 1) liste.remove(cle) else liste[cle] = n - 1
    }
    reglages.quizARevoir = liste.entries.joinToString(";") { "${it.key.first.name}|${it.key.second}=${it.value}" }
}

private fun accorder(n: Int, mot: String) = "$n $mot" + if (n > 1) "s" else ""

/* ── Le sommaire ───────────────────────────────────────────────────────── */

private fun iconeTheme(t: ThemeFormule): ImageVector = when (t) {
    ThemeFormule.EXPOSITION -> Onglet.MESURER.icone
    ThemeFormule.TOURNAGE -> IconeViseur
    ThemeFormule.LUMIERE -> IconeProjecteur
    ThemeFormule.COULEUR -> IconeCouleur
    ThemeFormule.OPTIQUE -> IconeChamp
    ThemeFormule.OPTIQUE_GEO -> IconeMateriel
    ThemeFormule.SENSITOMETRIE -> Onglet.LABO.icone
    ThemeFormule.NUMERIQUE -> IconePlans
    ThemeFormule.ELECTRICITE -> IconeElectricite
    ThemeFormule.SCINTILLEMENT -> IconeScintillement
}

/** Un autre quiz de l'appli, rangé ailleurs : la carte y mène. */
private class AutreQuiz(val titre: String, val detail: String, val icone: ImageVector, val destination: String)

@Composable
private fun SommaireReviser(reglages: Reglages, ouvrir: (VueReviser) -> Unit, naviguer: (String) -> Unit) {
    val scores = lireScores(reglages.quizFormules)
    val aRevoir = lireARevoir(reglages.quizARevoir)
    val colonnes = if (LocalPaysage.current) 3 else 2
    val total = scores.values.fold(0 to 0) { acc, s -> (acc.first + s.first) to (acc.second + s.second) }

    Spacer(Modifier.height(8.dp))
    if (aRevoir.isNotEmpty()) {
        EntreeMiseEnAvant(
            titre = "À revoir · ${accorder(aRevoir.size, "formule")}",
            detail = "Les formules où tu t'es trompé, jusqu'à deux bonnes réponses chacune.",
            icone = IconeConversions
        ) { ouvrir(VueReviser.Session(Source.ARevoir)) }
    }
    EntreeMiseEnAvant(
        titre = "Série de $SERIE questions",
        detail = "Tous les thèmes mélangés, ton score à la fin." +
            if (total.second > 0) " Depuis le début : ${total.first} justes sur ${total.second}." else "",
        icone = IconeReviser
    ) { ouvrir(VueReviser.Session(Source.Serie)) }

    Etiquette("Le vocabulaire du métier", Modifier.padding(start = Gouttiere, end = Gouttiere, top = 16.dp, bottom = 4.dp), accent = true)
    val nbPerso = fr.cellule.core.LexiquePerso.lire(reglages.lexiquePerso).size
    GrilleSommaire(listOf(0, 1), colonnes) { i, m ->
        if (i == 0) {
            CarteSommaire(
                "Les mots du plateau", "Wind-up, 216, PAT, crabe… et tes propres mots.", IconeReviser, modifier = m,
                resume = "${fr.cellule.core.Lexique.TERMES.size} mots" + if (nbPerso > 0) " · ${accorder(nbPerso, "mot")} à toi" else ""
            ) { ouvrir(VueReviser.Lexique) }
        } else {
            CarteSommaire("Les marques", "ARRI, Cooke, Matthews, Kupo… qui fait quoi.", Onglet.CAMERAS.icone, modifier = m) { ouvrir(VueReviser.Marques) }
        }
    }

    Etiquette("Les formules, thème par thème", Modifier.padding(start = Gouttiere, end = Gouttiere, top = 16.dp, bottom = 4.dp), accent = true)
    Text(
        "Chaque thème a deux onglets : Apprendre (une fiche par formule, à lire ou à réciter) et S'entraîner (des exercices, à choix ou à taper).",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Gouttiere, vertical = 2.dp)
    )
    GrilleSommaire(ThemeFormule.entries.toList() + listOf<ThemeFormule?>(null), colonnes) { t, m ->
        if (t == null) {
            CarteSommaire("Tout mélanger", "Sans fin, tous les thèmes dans le désordre.", IconeConversions, modifier = m) {
                ouvrir(VueReviser.Session(Source.Melange))
            }
        } else {
            val score = scores[t.name]
            val revoir = aRevoir.keys.count { it.first == t }
            val resume = listOfNotNull(
                if (score == null || score.second == 0) "Pas encore commencé" else "${score.first} sur ${score.second}",
                if (revoir > 0) "$revoir à revoir" else null
            ).joinToString(" · ")
            CarteSommaire(t.titre, t.detail, iconeTheme(t), modifier = m, resume = resume) { ouvrir(VueReviser.Theme(t)) }
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

/** Une grande entrée sur fond de teinte, en tête du sommaire. */
@Composable
private fun EntreeMiseEnAvant(titre: String, detail: String, icone: ImageVector, ouvrir: () -> Unit) {
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
        Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(end = 14.dp))
        Column(Modifier.weight(1f)) {
            Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(detail, style = Detail, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
        }
        Text("›", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 10.dp))
    }
}

/* ── Un thème : Apprendre · S'entraîner ────────────────────────────────── */

@Composable
private fun PageTheme(reglages: Reglages, theme: ThemeFormule, retour: () -> Unit) {
    /* Un thème jamais travaillé s'ouvre sur ses fiches ; sinon, sur les exercices. */
    val dejaCommence = lireScores(reglages.quizFormules)[theme.name] != null
    var onglet by remember { mutableStateOf(if (dejaCommence) 1 else 0) }
    val session = rememberSession(reglages, Source.Theme(theme))
    Column(Modifier.fillMaxSize()) {
        FilAriane(chemin = listOf(Miette("Réviser", retour)), titre = theme.titre, phrase = theme.detail, retour = retour)
        Onglets(listOf(0, 1), onglet, { if (it == 0) "Apprendre" else "S'entraîner" }) { onglet = it }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (onglet == 0) Apprendre(reglages, theme) else SessionFormules(reglages, session, Source.Theme(theme), retour)
        }
    }
}

@Composable
private fun Apprendre(reglages: Reglages, theme: ThemeFormule) {
    var reciter by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)) {
        Box(Modifier.fillMaxWidth().padding(start = Gouttiere, end = Gouttiere, top = 10.dp, bottom = 4.dp)) {
            ChoixSegmente(listOf(false, true), reciter, { if (it) "Réciter" else "Lire" }) { reciter = it }
        }
        Text(
            if (reciter) "La formule est cachée : dis-la de tête, puis touche la carte pour vérifier."
            else "Pour chaque formule : ce que veut dire chaque lettre, un exemple pas à pas, le repère à retenir et le piège classique.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Gouttiere, vertical = 4.dp)
        )
        FichesFormules.du(theme).forEach { f ->
            key(f.sujet, reciter) { CarteFicheFormule(reglages, f, reciter) }
        }
    }
}

@Composable
private fun CarteFicheFormule(reglages: Reglages, f: FicheFormule, reciter: Boolean) {
    var visible by remember { mutableStateOf(!reciter) }
    var note by remember { mutableStateOf<String?>(null) }
    Carte(modifier = if (reciter && !visible) Modifier.clickable { visible = true } else Modifier) {
        Text(f.sujet, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        if (!visible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
                    .padding(14.dp)
            ) {
                Text("Dis-la de tête, puis touche pour vérifier", style = Detail, color = MaterialTheme.colorScheme.primary)
            }
        } else {
            ContenuFiche(f)
            if (reciter) {
                Spacer(Modifier.height(Interligne))
                LigneBoutons {
                    BoutonPlat("Je savais", modifier = Modifier.weight(1f)) {
                        suivreARevoir(reglages, f.theme, f.sujet, juste = true)
                        note = "Bien. Elle sort de « À revoir » après deux réussites."
                    }
                    BoutonPlat("À revoir", modifier = Modifier.weight(1f)) {
                        suivreARevoir(reglages, f.theme, f.sujet, juste = false)
                        note = "Ajoutée à « À revoir », en tête de Réviser."
                    }
                }
                note?.let { Text(it, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
    }
}

/** La fiche elle-même : formule, lettres, exemple, repère, piège. */
@Composable
private fun ContenuFiche(f: FicheFormule) {
    Text(
        f.formule,
        style = Corps.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
            .padding(12.dp)
    )
    Spacer(Modifier.height(8.dp))
    f.lettres.forEach { Text("• $it", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Spacer(Modifier.height(10.dp))
    Etiquette("Exemple", accent = true)
    Text(f.exemple, style = Corps, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Etiquette("À retenir", accent = true)
    Text(f.repere, style = Corps, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(8.dp))
    Etiquette("Le piège", accent = true)
    Text(f.piege, style = Corps, color = MaterialTheme.colorScheme.onSurface)
}

/* ── Une session d'exercices ───────────────────────────────────────────── */

private class EtatSession(val serie: Boolean, taperAuDepart: Boolean, private val tirer: () -> QuestionFormule) {
    var taper by mutableStateOf(taperAuDepart)
    var question by mutableStateOf(tirage())
        private set
    var choisi by mutableStateOf<Int?>(null)
        private set
    var saisie by mutableStateOf("")
    var verdict by mutableStateOf<Boolean?>(null)
        private set
    var indice by mutableStateOf(false)
    var justes by mutableStateOf(0)
        private set
    var posees by mutableStateOf(0)
        private set
    var termine by mutableStateOf(false)

    val repondu get() = verdict != null
    val derniere get() = serie && posees >= SERIE

    /** En mode « taper », on écarte les questions qui ne se tapent pas (les formules à reconnaître). */
    private fun tirage(): QuestionFormule {
        var q = tirer()
        if (taper) repeat(40) {
            if (SaisieReponse.gabarit(q) != null) return q
            q = tirer()
        }
        return q
    }

    fun repondreChoix(i: Int): Boolean? {
        if (repondu) return null
        choisi = i
        return conclure(i == question.bonne)
    }

    fun repondreSaisie(): Boolean? {
        if (repondu || saisie.isBlank()) return null
        return conclure(SaisieReponse.accepte(question, saisie))
    }

    private fun conclure(juste: Boolean): Boolean {
        verdict = juste
        posees++
        if (juste) justes++
        return juste
    }

    fun suivante() {
        /* Jamais deux fois de suite la même question. */
        var q = tirage()
        var essais = 0
        while (q.enonce == question.enonce && essais < 20) {
            q = tirage()
            essais++
        }
        question = q
        choisi = null
        saisie = ""
        verdict = null
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
private fun rememberSession(reglages: Reglages, source: Source): EtatSession = remember(source) {
    val tirer: () -> QuestionFormule = {
        when (source) {
            is Source.Theme -> QuizFormules.question(source.theme)
            Source.ARevoir -> {
                val liste = lireARevoir(reglages.quizARevoir).keys.toList()
                if (liste.isEmpty()) QuizFormules.question(null)
                else liste.random().let { (theme, sujet) -> QuizFormules.questionSur(theme, sujet) }
            }
            else -> QuizFormules.question(null)
        }
    }
    EtatSession(serie = source == Source.Serie, taperAuDepart = reglages.quizTaper, tirer = tirer)
}

/** Une session à part entière : série, mélange, ou formules à revoir. */
@Composable
private fun PageSession(reglages: Reglages, source: Source, retour: () -> Unit) {
    val session = rememberSession(reglages, source)
    val titre = when (source) {
        Source.Serie -> "Série de $SERIE"
        Source.ARevoir -> "À revoir"
        else -> "Tout mélanger"
    }
    Column(Modifier.fillMaxSize()) {
        FilAriane(chemin = listOf(Miette("Réviser", retour)), titre = titre, retour = retour)
        Box(Modifier.fillMaxWidth().weight(1f)) { SessionFormules(reglages, session, source, retour) }
    }
}

@Composable
private fun SessionFormules(reglages: Reglages, etat: EtatSession, source: Source, retour: () -> Unit) {
    val defilement = rememberScrollState()
    /* Chaque question repart du haut. */
    LaunchedEffect(etat.question, etat.termine) { defilement.scrollTo(0) }
    val plusRienARevoir = source == Source.ARevoir && !etat.repondu && reglages.quizARevoir.isBlank()

    Column(Modifier.fillMaxSize().verticalScroll(defilement).padding(bottom = ZoneNavFlottante)) {
        Box(Modifier.fillMaxWidth().padding(start = Gouttiere, end = Gouttiere, top = 10.dp)) {
            ChoixSegmente(listOf(false, true), etat.taper, { if (it) "Taper" else "4 choix" }) {
                etat.taper = it
                reglages.quizTaper = it
            }
        }
        val progression = when {
            etat.termine -> "Série terminée."
            etat.serie -> "Question ${minOf(etat.posees + if (etat.repondu) 0 else 1, SERIE)} sur $SERIE · ${accorder(etat.justes, "juste")}"
            etat.posees > 0 -> "${accorder(etat.justes, "juste")} sur ${etat.posees}"
            else -> ""
        }
        if (progression.isNotBlank()) {
            Text(progression, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Gouttiere, vertical = 6.dp))
        }
        when {
            plusRienARevoir -> {
                CarteResultat(titre = "Plus rien à revoir") {
                    Text("Toutes tes erreurs ont été rattrapées. Elles reviendront ici si tu te trompes à nouveau.", style = Corps, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                BoutonPlat("Retour aux quiz", accent = true, modifier = Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 8.dp)) { retour() }
            }
            etat.termine -> FinDeSerie(etat.justes, recommencer = { etat.recommencer() }, retour = retour)
            else -> Question(etat) { juste ->
                val q = etat.question
                noterScore(reglages, q.theme, juste)
                suivreARevoir(reglages, q.theme, q.sujet, juste)
            }
        }
    }
}

@Composable
private fun Question(etat: EtatSession, surReponse: (Boolean) -> Unit) {
    val q = etat.question
    val gabarit = SaisieReponse.gabarit(q)
    val modeTaper = etat.taper && gabarit != null
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

    if (modeTaper) {
        val valider = { etat.repondreSaisie()?.let(surReponse); Unit }
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 4.dp)) {
            Text("Réponse attendue : $gabarit", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = etat.saisie,
                onValueChange = { etat.saisie = it },
                enabled = !etat.repondu,
                singleLine = true,
                label = { Text("Ta réponse", style = Detail) },
                textStyle = Corps,
                shape = RoundedCornerShape(RayonControle),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { valider() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (!etat.repondu) {
                BoutonPlat(
                    "Valider",
                    actif = etat.saisie.isNotBlank(),
                    accent = etat.saisie.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { valider() }
            }
        }
    } else {
        if (etat.taper && gabarit == null && !etat.repondu) {
            Text(
                "Celle-ci se reconnaît, elle ne se tape pas : choisis parmi les quatre.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Gouttiere, vertical = 2.dp)
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 4.dp)) {
            q.options.forEachIndexed { i, option ->
                BoutonPlat(
                    option,
                    actif = !etat.repondu,
                    accent = etat.repondu && i == q.bonne,
                    erreur = etat.choisi == i && i != q.bonne,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { etat.repondreChoix(i)?.let(surReponse) }
            }
        }
    }

    if (etat.repondu) {
        val juste = etat.verdict == true
        CarteResultat(titre = if (juste) "Juste !" else "Pas tout à fait") {
            if (!juste) {
                Text("La bonne réponse : ${q.options[q.bonne]}", style = TitreCarte, color = MaterialTheme.colorScheme.onPrimaryContainer)
                if (modeTaper) {
                    Text("Tu as répondu : ${etat.saisie}", style = Detail, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                }
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
        /* Revoir la fiche de la formule, sans quitter l'exercice. */
        FichesFormules.pour(q.theme, q.sujet)?.let { fiche ->
            Carte {
                Depliable("Revoir la fiche « ${fiche.sujet} »", ouvertAuDepart = false) {
                    Spacer(Modifier.height(4.dp))
                    ContenuFiche(fiche)
                }
            }
        }
    }
}

@Composable
private fun FinDeSerie(justes: Int, recommencer: () -> Unit, retour: () -> Unit) {
    val mot = when {
        justes == SERIE -> "Sans faute."
        justes >= 8 -> "Très bien : relis seulement les corrections de tes erreurs."
        justes >= 5 -> "Pas mal : tes erreurs t'attendent dans « À revoir »."
        else -> "Reprends thème par thème : lis les fiches, puis entraîne-toi avec l'indice."
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
