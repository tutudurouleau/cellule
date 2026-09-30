/* ModalBottomSheet est encore annoncé expérimental dans cette version de
   Material3 ; même choix que pour les fiches Caméras. */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonControle
import fr.cellule.app.TitreCarte
import fr.cellule.app.TitreEcran
import fr.cellule.core.FicheLabo
import fr.cellule.core.FichesLabo
import fr.cellule.core.GenreFiche
import fr.cellule.core.GrilleTemps
import fr.cellule.core.RechercheFiches
import fr.cellule.core.TablesLabo
import fr.cellule.core.TempsFiche
import fr.cellule.core.TypeFiche

/**
 * Les fiches produits : ce que chaque fabricant dit de son film, de son
 * révélateur, de ses bains — et d'où il le dit.
 *
 * La liste reste courte et lisible : une carte par produit, rangée par type.
 * Toucher une carte ouvre la fiche entière dans un tiroir qui remonte du bas,
 * comme les fiches Caméras : on y lit tranquillement, on le referme en le
 * glissant vers le bas — jamais en touchant son contenu par mégarde.
 */
@Composable
fun EcranFiches(ouvrir: String? = null) {
    var recherche by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf<GenreFiche?>(null) }
    /* Venue d'un calcul ou d'une question, la fiche s'ouvre d'emblée. */
    var ouverte by remember(ouvrir) {
        mutableStateOf(ouvrir?.let { nom -> FichesLabo.TOUTES.firstOrNull { it.nom == nom } ?: FichesLabo.parNom(nom) })
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 2.dp)) {
        Champ(recherche, "Chercher", Modifier.fillMaxWidth(), indication = "HP5, liquide, fixateur, Kodak…") { recherche = it }
        Spacer(Modifier.height(8.dp))
        ChoixSegmente(listOf<GenreFiche?>(null) + GenreFiche.entries, genre, {
            when (it) {
                null -> "Tout"
                GenreFiche.FILM -> "Films"
                GenreFiche.REVELATEUR -> "Révélateurs"
                GenreFiche.BAIN -> "Bains"
            }
        }) { genre = it }
    }
    val trouvees = RechercheFiches.chercher(recherche, genre)
    if (trouvees.isEmpty()) {
        Carte(sousTitre = "Aucune fiche ne correspond. Peut-être n'a-t-elle pas encore été relue : voir la liste plus bas.") {}
    }
    TypeFiche.entries.forEach { t ->
        val liste = trouvees.filter { RechercheFiches.type(it) == t }
        if (liste.isNotEmpty()) {
            Column(Modifier.padding(start = Gouttiere + 6.dp, end = Gouttiere, top = 14.dp, bottom = 2.dp)) {
                Etiquette("${t.libelle} · ${liste.size}", accent = true)
                if (t.detail.isNotBlank()) {
                    Text(t.detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            liste.forEach { f -> CarteFiche(f) { ouverte = f } }
        }
    }
    CarteARelire()

    ouverte?.let { f -> FeuilleFicheLabo(f) { ouverte = null } }
}

/**
 * Ce que le Labo refuse d'inventer : chaque fiche manquante, cochée le jour
 * où sa source officielle aura été relue.
 */
@Composable
private fun CarteARelire() {
    Carte(
        titre = "Encore à relire · ${FichesLabo.A_RELIRE.size}",
        sousTitre = "Ces fiches n'ont pas pu être relues chez leur fabricant. Plutôt qu'un chiffre recopié d'ailleurs, le Labo les laisse vides pour l'instant."
    ) {
        FichesLabo.A_RELIRE.forEach { ligne ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text("○", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(22.dp))
                Text(ligne, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Chacune rejoindra le Labo une fois sa fiche officielle relue — avec sa source, comme les autres.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** La carte de la liste : de quoi reconnaître le produit, et ce que contient sa fiche. */
@Composable
private fun CarteFiche(f: FicheLabo, surAppui: () -> Unit) {
    Carte(modifier = Modifier.clickable { surAppui() }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.nom, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Text(f.fabricant, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = TitreEcran, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(f.resume, style = Corps, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
        val contenu = TempsFiche.contenu(f)
        if (contenu != null || f.lacune.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(contenu.orEmpty(), style = Detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                /* Une fiche incomplète le dit avant même d'être ouverte. */
                if (f.lacune.isNotBlank()) {
                    Text(
                        "fiche partielle",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * La fiche entière, dans un tiroir : l'identité du produit, ce qui manque,
 * ce que dit le fabricant, puis les temps publiés en tableaux — un révélateur
 * (ou un film) à la fois — et les sources.
 */
@Composable
private fun FeuilleFicheLabo(f: FicheLabo, surFermer: () -> Unit) {
    val etat = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = surFermer,
        sheetState = etat,
        /* Un ton sous celui des lignes paires des tableaux, pour qu'elles se voient. */
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        dragHandle = { Poignee() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gouttiere + 2.dp)
                .padding(bottom = Gouttiere)
        ) {
            Etiquette(RechercheFiches.type(f).libelle, accent = true)
            Text(f.nom, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
            Text(f.fabricant, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Interligne))
            Text(f.resume, style = Corps, color = MaterialTheme.colorScheme.onSurface)

            /* Ce qui manque se dit avant le reste, en entier. */
            if (f.lacune.isNotBlank()) {
                Spacer(Modifier.height(Interligne))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Etiquette("Fiche partielle")
                    Spacer(Modifier.height(2.dp))
                    Text(f.lacune, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (f.faits.isNotEmpty()) {
                Separateur()
                Etiquette("Ce que dit la fiche")
                Spacer(Modifier.height(2.dp))
                f.faits.forEach { Ligne(it.intitule, it.valeur, it.detail.ifBlank { null }) }
            }

            SectionTemps(f)

            Separateur()
            LigneSource(f.source)
            f.autresSources.forEach { LigneSource(it) }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Les temps publiés : on choisit d'abord le révélateur (sur une fiche de
 * film) ou le film (sur une fiche de révélateur), puis on lit son tableau.
 */
@Composable
private fun SectionTemps(f: FicheLabo) {
    val choix = remember(f) { TempsFiche.choix(f) }
    if (choix.isEmpty()) return
    var choisi by remember(f) { mutableStateOf(choix.first()) }
    val film = f.genre == GenreFiche.FILM

    Separateur()
    Etiquette("Temps de développement publiés, en cuve")
    Spacer(Modifier.height(4.dp))
    if (choix.size > 1) {
        Text(
            if (film) "Choisis le révélateur :" else "Choisis le film :",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            choix.forEach { c -> Pastille(c, accent = c == choisi) { choisi = c } }
        }
    } else {
        Text(
            (if (film) "Avec " else "Pour ") + choisi,
            style = TitreCarte, color = MaterialTheme.colorScheme.onSurface
        )
    }

    val grilles = TempsFiche.grilles(f, choisi)
    grilles.forEach { g -> TableauTemps(g) }
    if (grilles.any { g -> g.lignes.any { l -> l.cellules.any { it == null } } }) {
        Text(
            "— : le fabricant ne publie pas ce temps.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    /* Les temps d'un révélateur viennent parfois de la fiche du film, pas de la sienne : on le dit. */
    grilles.map { it.source }.distinct().filter { it != f.source && it !in f.autresSources }.forEach {
        LigneSource(it)
    }
}

/** Un tableau : EI en lignes ; dilutions ou températures en colonnes ; minutes dans les cases. */
@Composable
private fun TableauTemps(g: GrilleTemps) {
    Spacer(Modifier.height(Interligne))
    if (g.titre != null) {
        Text("Dilution ${g.titre}", style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
    }
    Text(
        g.temperature?.let { "À ${fmt(it, 0)} °C · temps en minutes" } ?: "Temps en minutes, selon la température du révélateur",
        style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(4.dp))
    Tableau(
        listOf(Colonne("EI", 0.9f)) + g.colonnes.map { Colonne(it, 1f, aDroite = true) },
        g.lignes.map { l ->
            RangTableau(
                listOf("${l.ei}") + l.cellules.map { m -> m?.let { TablesLabo.minutes(it) } ?: "—" },
                accent = l.ei == g.eiNominal
            )
        },
        petit = g.colonnes.size > 3
    )
    if (g.eiNominal != null && g.lignes.any { it.ei == g.eiNominal }) {
        Text(
            "En couleur : la sensibilité nominale du film, EI ${g.eiNominal}.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
