package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.core.Duree
import fr.cellule.core.FicheLabo
import fr.cellule.core.FichesLabo
import fr.cellule.core.GenreFiche
import fr.cellule.core.RechercheFiches
import fr.cellule.core.TempsPublie
import fr.cellule.core.TypeFiche

/**
 * Les fiches produits : ce que chaque fabricant dit de son film, de son
 * révélateur, de ses bains — et d'où il le dit. On les range par type de
 * produit (films par sensibilité, révélateurs en poudre ou liquides, arrêt,
 * fixateurs, lavage) ; la marque reste écrite sur chaque fiche et se cherche.
 * Une fiche se déplie d'un appui ; ce qui n'a pas pu être relu à la source
 * est annoncé comme tel, jusque sur la fiche fermée.
 */
@Composable
fun EcranFiches(ouvrir: String? = null) {
    /* Venu d'un calcul ou d'une question : la recherche filtre déjà sur ce
       produit, et sa fiche s'ouvre directement. */
    var recherche by remember(ouvrir) { mutableStateOf(ouvrir.orEmpty()) }
    var genre by remember { mutableStateOf<GenreFiche?>(null) }
    var ouverte by remember(ouvrir) { mutableStateOf(ouvrir) }

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
        }) {
            genre = it
            ouverte = null
        }
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
            liste.forEach { f ->
                CarteFiche(f, ouverte == f.nom) { ouverte = if (ouverte == f.nom) null else f.nom }
            }
        }
    }
    CarteARelire()
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

@Composable
private fun CarteFiche(f: FicheLabo, ouverte: Boolean, surAppui: () -> Unit) {
    Carte(
        titre = f.nom,
        sousTitre = f.fabricant,
        modifier = Modifier.clickable { surAppui() }
    ) {
        Text(f.resume, style = Corps, color = MaterialTheme.colorScheme.onSurface)
        if (!ouverte) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Toucher pour ouvrir", style = Detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
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
            return@Carte
        }
        Spacer(Modifier.height(6.dp))
        f.faits.forEach { Ligne(it.intitule, it.valeur, it.detail.ifBlank { null }) }
        val temps = FichesLabo.temps(f)
        if (temps.isNotEmpty()) {
            Separateur()
            Etiquette("Temps publiés, cuve")
            Spacer(Modifier.height(4.dp))
            TableTemps(temps, parFilm = f.genre == GenreFiche.REVELATEUR)
        }
        if (f.lacune.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            BandeauEtat(f.lacune, alerte = false)
        }
        Spacer(Modifier.height(4.dp))
        LigneSource(f.source)
        f.autresSources.forEach { LigneSource(it) }
    }
}

/**
 * Une ligne par couple et par température ; les indices d'exposition se
 * suivent sur la ligne, du plus lent au plus poussé.
 */
@Composable
private fun TableTemps(temps: List<TempsPublie>, parFilm: Boolean) {
    temps.groupBy { Triple(if (parFilm) it.film else it.revelateur, it.dilution, it.temperature) }
        .forEach { (cle, lignes) ->
            val (nom, dilution, temperature) = cle
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    listOf(nom, if (parFilm) dilution.let { d -> if (d.isBlank()) "" else "· $d" } else dilution)
                        .filter { it.isNotBlank() }.joinToString(" ") + " · ${fmt(temperature, 0)} °C",
                    style = Corps, color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    lignes.sortedBy { it.ei }.joinToString("  ·  ") { "EI ${it.ei} : ${Duree.libelle(it.minutes * 60)}" },
                    style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
}
