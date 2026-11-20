package fr.cellule.app.ecrans

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import fr.cellule.app.Reglages
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CATALOGUE_ELECTRIQUE
import fr.cellule.core.CATALOGUE_MACHINERIE
import fr.cellule.core.DomaineMarque
import fr.cellule.core.Lexique
import fr.cellule.core.LexiquePerso
import fr.cellule.core.Marques
import fr.cellule.core.Materiel
import fr.cellule.core.QuestionLexique
import fr.cellule.core.RubriqueLexique
import fr.cellule.core.TermePlateau
import kotlin.random.Random

/*
 * Les mots du plateau et les marques du cinéma, dans Réviser : chercher un
 * mot, ajouter les siens, et s'entraîner (cartes à retourner, quiz).
 */

@Composable
internal fun PageLexique(reglages: Reglages, retour: () -> Unit) {
    var onglet by remember { mutableIntStateOf(0) }
    var fiche by remember { mutableStateOf<Materiel?>(null) }
    Column(Modifier.fillMaxSize()) {
        FilAriane(
            chemin = listOf(Miette("Réviser", retour)), titre = "Les mots du plateau",
            phrase = "Ce qu'on entend en tournage, et ce que ça veut dire.", retour = retour
        )
        Onglets(listOf(0, 1, 2), onglet, { listOf("Apprendre", "S'entraîner", "Mes mots")[it] }) { onglet = it }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (onglet) {
                0 -> ApprendreLexique(reglages) { fiche = it }
                1 -> EntrainementLexique(reglages)
                else -> MesMots(reglages)
            }
        }
    }
    fiche?.let { FeuilleFiche(it, reglages) { fiche = null } }
}

private fun mesMots(reglages: Reglages) = LexiquePerso.lire(reglages.lexiquePerso)

private val FICHES_LEXIQUE: List<Materiel> by lazy { CATALOGUE_MACHINERIE + CATALOGUE_ELECTRIQUE + fr.cellule.core.CATALOGUE_MATERIEL }

@Composable
private fun ApprendreLexique(reglages: Reglages, ouvrirFiche: (Materiel) -> Unit) {
    var recherche by remember { mutableStateOf("") }
    var rubrique by remember { mutableStateOf<RubriqueLexique?>(null) }
    val tous = Lexique.tous(mesMots(reglages))
    val visibles = Lexique.chercher(recherche, tous).filter { rubrique == null || it.rubrique == rubrique }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)) {
        Column(Modifier.padding(horizontal = Gouttiere, vertical = 6.dp)) {
            Champ(recherche, "Chercher un mot", Modifier.fillMaxWidth(), indication = "wind-up, 216, PAT…") { recherche = it }
            Spacer(Modifier.height(6.dp))
            Deroulant(
                "Rubrique", listOf<RubriqueLexique?>(null) + RubriqueLexique.entries, rubrique,
                { it?.libelle ?: "Toutes (${tous.size} mots)" }, Modifier.fillMaxWidth()
            ) { rubrique = it }
        }
        if (visibles.isEmpty()) {
            Carte(sousTitre = "Aucun mot ne correspond. Tu peux l'ajouter dans « Mes mots ».") {}
        }
        visibles.groupBy { it.rubrique }.forEach { (r, termes) ->
            Carte(titre = r.libelle) {
                termes.forEach { t -> LigneTerme(t, ouvrirFiche) }
            }
        }
    }
}

@Composable
private fun LigneTerme(t: TermePlateau, ouvrirFiche: (Materiel) -> Unit) {
    val fiche = t.fiche?.let { id -> FICHES_LEXIQUE.firstOrNull { it.identifiant == id } }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(t.mot, style = Corps, color = MaterialTheme.colorScheme.primary)
        if (t.aussi.isNotEmpty()) {
            Text("aussi : " + t.aussi.joinToString(", "), style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(t.definition.ifBlank { "(sans définition)" }, style = Corps, color = MaterialTheme.colorScheme.onSurface)
        if (fiche != null) {
            Text(
                "→ Voir la fiche « ${fiche.nomComplet} »",
                style = Detail, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { ouvrirFiche(fiche) }.padding(top = 4.dp)
            )
        }
    }
}

/* ── S'entraîner : retourner les cartes, ou le quiz ── */

private class EtatLexique {
    var mode by mutableIntStateOf(0)
    var question by mutableStateOf<QuestionLexique?>(null)
    var choisi by mutableStateOf<Int?>(null)
    var retournee by mutableStateOf(false)
    var justes by mutableIntStateOf(0)
    var posees by mutableIntStateOf(0)
    var rubrique by mutableStateOf<RubriqueLexique?>(null)
    val alea = Random(System.nanoTime())
}

@Composable
private fun EntrainementLexique(reglages: Reglages) {
    val etat = remember { EtatLexique() }
    val termes = Lexique.tous(mesMots(reglages)).filter { etat.rubrique == null || it.rubrique == etat.rubrique }
    fun suivante() {
        etat.question = Lexique.question(termes, etat.alea, etat.question?.terme?.cle)
        etat.choisi = null
        etat.retournee = false
    }
    if (etat.question == null || etat.question!!.terme !in termes) suivante()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)) {
        Column(Modifier.padding(horizontal = Gouttiere, vertical = 6.dp)) {
            ChoixSegmente(listOf(0, 1), etat.mode, { if (it == 0) "Quiz" else "Cartes" }) { etat.mode = it; suivante() }
            Spacer(Modifier.height(6.dp))
            Deroulant(
                "Rubrique", listOf<RubriqueLexique?>(null) + RubriqueLexique.entries, etat.rubrique,
                { it?.libelle ?: "Tous les mots" }, Modifier.fillMaxWidth()
            ) { etat.rubrique = it; etat.question = null }
        }
        val q = etat.question
        if (q == null) {
            Carte(sousTitre = "Il faut au moins quatre mots dans cette rubrique pour s'entraîner.") {}
            return@Column
        }
        if (etat.mode == 1) {
            /* La carte : le mot d'abord, la définition au toucher. */
            Carte(modifier = Modifier.clickable { etat.retournee = !etat.retournee }) {
                Etiquette(q.terme.rubrique.libelle, accent = true)
                Spacer(Modifier.height(6.dp))
                Text(q.terme.mot, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Interligne))
                if (etat.retournee) {
                    Text(q.terme.definition, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Text("Dis ce que ça veut dire, puis touche la carte.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Carte {
                LigneBoutons {
                    BoutonPlat("Je ne savais pas", modifier = Modifier.weight(1f)) { etat.posees++; suivante() }
                    BoutonPlat("Je savais", accent = true, modifier = Modifier.weight(1f)) { etat.posees++; etat.justes++; suivante() }
                }
            }
        } else {
            Carte {
                Etiquette("Quel mot ?", accent = true)
                Spacer(Modifier.height(6.dp))
                Text(q.terme.definition, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(Interligne))
                q.choix.forEachIndexed { i, c ->
                    val repondu = etat.choisi != null
                    BoutonPlat(
                        c,
                        actif = !repondu || i == q.bonne,
                        accent = repondu && i == q.bonne,
                        erreur = repondu && i == etat.choisi && i != q.bonne,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        if (etat.choisi == null) {
                            etat.choisi = i
                            etat.posees++
                            if (i == q.bonne) etat.justes++
                        }
                    }
                }
                if (etat.choisi != null) {
                    Spacer(Modifier.height(6.dp))
                    BoutonPlat("Suivant", accent = true, modifier = Modifier.fillMaxWidth()) { suivante() }
                }
            }
        }
        if (etat.posees > 0) Carte { Ligne("Score", "${etat.justes} / ${etat.posees}") }
    }
}

/* ── Mes mots ── */

@Composable
private fun MesMots(reglages: Reglages) {
    var mot by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    val mots = mesMots(reglages)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)) {
        Carte(titre = "Ajouter un mot", sousTitre = "Un mot entendu sur un plateau : il rejoint le lexique et les révisions.") {
            Champ(mot, "Le mot", Modifier.fillMaxWidth(), indication = "un U, une gamelle…") { mot = it }
            Spacer(Modifier.height(6.dp))
            Champ(definition, "Ce que ça veut dire", Modifier.fillMaxWidth(), surUneLigne = false) { definition = it }
            Spacer(Modifier.height(Interligne))
            BoutonPlat("Ajouter", actif = mot.isNotBlank(), accent = true, modifier = Modifier.fillMaxWidth()) {
                reglages.lexiquePerso = LexiquePerso.ecrire(mots.filter { it.mot != mot.trim() } + TermePlateau(mot.trim(), definition.trim(), RubriqueLexique.PERSO))
                mot = ""
                definition = ""
            }
        }
        if (mots.isEmpty()) {
            Carte(sousTitre = "Pas encore de mot à toi.") {}
        } else {
            Carte(titre = "Mes mots (${mots.size})") {
                mots.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(t.mot, style = Corps, color = MaterialTheme.colorScheme.primary)
                            Text(t.definition.ifBlank { "(sans définition)" }, style = Detail, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(
                            "Retirer", style = Detail, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { reglages.lexiquePerso = LexiquePerso.ecrire(mots - t) }.padding(start = 10.dp, top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/* ── Les marques ── */

@Composable
internal fun PageMarques(retour: () -> Unit) {
    var recherche by remember { mutableStateOf("") }
    var domaine by remember { mutableStateOf<DomaineMarque?>(null) }
    var petites by remember { mutableStateOf(true) }
    val visibles = Marques.chercher(recherche, domaine).filter { petites || it.grande }
    Column(Modifier.fillMaxSize()) {
        FilAriane(
            chemin = listOf(Miette("Réviser", retour)), titre = "Les marques du cinéma",
            phrase = "Celles qu'on lit sur les caisses chez le loueur.", retour = retour
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)) {
            Column(Modifier.padding(horizontal = Gouttiere, vertical = 6.dp)) {
                Champ(recherche, "Chercher", Modifier.fillMaxWidth(), indication = "une marque, un pays, un produit…") { recherche = it }
                Spacer(Modifier.height(6.dp))
                Deroulant("Domaine", listOf<DomaineMarque?>(null) + DomaineMarque.entries, domaine, { it?.libelle ?: "Tous les domaines" }, Modifier.fillMaxWidth()) { domaine = it }
                Spacer(Modifier.height(6.dp))
                ChoixSegmente(listOf(false, true), petites, { if (it) "Toutes" else "Les grandes" }) { petites = it }
            }
            visibles.forEach { m ->
                Carte(titre = m.nom, sousTitre = m.pays + " · " + m.domaines.joinToString(", ") { it.libelle.lowercase() } + if (!m.grande) " · plus récente ou spécialisée" else "") {
                    Text(m.connuePour, style = Corps, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Produits : " + m.produits, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
