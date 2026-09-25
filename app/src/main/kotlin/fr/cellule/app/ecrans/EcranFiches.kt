package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.core.Duree
import fr.cellule.core.FicheLabo
import fr.cellule.core.FichesLabo
import fr.cellule.core.GenreFiche
import fr.cellule.core.TempsPublie

/**
 * Les fiches produits : ce que chaque fabricant dit de son film, de son
 * révélateur, de ses bains — et d'où il le dit. Une fiche se déplie d'un
 * appui ; ce qui n'a pas pu être relu à la source est annoncé comme tel.
 */
@Composable
fun EcranFiches() {
    var genre by remember { mutableStateOf(GenreFiche.FILM) }
    var ouverte by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 2.dp)) {
        ChoixSegmente(GenreFiche.entries.toList(), genre, { if (it == GenreFiche.BAIN) "Bains" else it.libelle }) {
            genre = it
            ouverte = null
        }
    }
    FichesLabo.parGenre(genre).forEach { f ->
        CarteFiche(f, ouverte == f.nom) { ouverte = if (ouverte == f.nom) null else f.nom }
    }
    Carte(
        titre = "Encore à relire",
        sousTitre = "Ces fiches n'ont pas pu être relues chez leur fabricant. Plutôt qu'un chiffre recopié d'ailleurs, le Labo les laisse vides pour l'instant."
    ) {
        FichesLabo.A_RELIRE.forEach { Ligne(it, "") }
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
            Text("Toucher pour ouvrir la fiche", style = Detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
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
