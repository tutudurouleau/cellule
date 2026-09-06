package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.StyleEtiquette
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.TitreCarte
import java.util.Locale
import kotlin.math.abs

/* ── Mise en forme des nombres ─────────────────────────────────────────── */

fun fmt(x: Double, decimales: Int = 1): String =
    if (x.isNaN() || x.isInfinite()) "—"
    else String.format(Locale.FRANCE, "%.${decimales}f", x)

fun signe(x: Double, decimales: Int = 1): String = when {
    x.isNaN() -> "—"
    abs(x) < 0.05 -> "0"
    x > 0 -> "+" + fmt(x, decimales)
    else -> "−" + fmt(abs(x), decimales)
}

/** Trois chiffres significatifs : au-delà on ferait croire à une précision
    que ni l'atmosphère ni l'œil ne possèdent. */
fun fmtLux(lux: Double): String = when {
    lux.isNaN() || lux.isInfinite() -> "—"
    lux >= 1000 -> {
        val puissance = Math.pow(10.0, 2 - Math.floor(Math.log10(lux)))
        val arrondi = Math.round(lux * puissance) / puissance
        String.format(Locale.FRANCE, "%,.0f", arrondi)
            .replace(' ', ' ').replace(' ', ' ') + " lx"
    }
    lux >= 100 -> "${Math.round(lux)} lx"
    lux >= 10 -> fmt(lux, 1) + " lx"
    lux >= 1 -> fmt(lux, 2) + " lx"
    lux >= 0.01 -> fmt(lux, 3) + " lx"
    else -> String.format(Locale.FRANCE, "%.4f", lux) + " lx"
}

fun fmtCdm2(l: Double): String = when {
    l.isNaN() || l.isInfinite() -> "—"
    l >= 100 -> "${Math.round(l)} cd/m²"
    l >= 1 -> fmt(l, 1) + " cd/m²"
    else -> fmt(l, 3) + " cd/m²"
}

/* ── Blocs ─────────────────────────────────────────────────────────────── */

@Composable
fun Etiquette(texte: String, modifier: Modifier = Modifier) {
    Text(
        texte.uppercase(Locale.FRANCE),
        style = StyleEtiquette,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Une carte : bordure fine plutôt qu'une ombre, pour ne pas alourdir. */
@Composable
fun Carte(
    titre: String? = null,
    sousTitre: String? = null,
    modifier: Modifier = Modifier,
    contenu: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 5.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(Gouttiere)
    ) {
        if (titre != null) {
            Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        }
        if (sousTitre != null) {
            Spacer(Modifier.height(3.dp))
            Text(sousTitre, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (titre != null || sousTitre != null) Spacer(Modifier.height(Interligne))
        contenu()
    }
}

/** Une ligne « intitulé · valeur », le détail sous l'intitulé. */
@Composable
fun Ligne(
    intitule: String,
    valeur: String,
    detail: String? = null,
    accent: Boolean = false
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(intitule, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            if (!detail.isNullOrBlank()) {
                Text(detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (valeur.isNotBlank()) {
            Text(
                valeur,
                style = Corps,
                color = if (accent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun Separateur() {
    HorizontalDivider(
        Modifier.padding(vertical = Interligne),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** Un choix parmi quelques-uns, en bandeau. */
@Composable
fun <T> ChoixSegmente(
    options: List<T>,
    selection: T,
    libelle: (T) -> String,
    surChoix: (T) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { option ->
            val actif = option == selection
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (actif) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable { surChoix(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    libelle(option),
                    style = Corps,
                    maxLines = 1,
                    color = if (actif) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Liste déroulante compacte, sans API expérimentale. */
@Composable
fun <T> Deroulant(
    intitule: String,
    options: List<T>,
    selection: T,
    libelle: (T) -> String,
    modifier: Modifier = Modifier,
    surChoix: (T) -> Unit
) {
    var ouvert by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(11.dp))
                .clickable { ouvert = true }
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            if (intitule.isNotBlank()) {
                Etiquette(intitule)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                libelle(selection) + "  ▾",
                style = Corps,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(libelle(option), style = Corps) },
                    onClick = { surChoix(option); ouvert = false }
                )
            }
        }
    }
}

/** Champ de saisie, à la même hauteur que les déroulants. */
@Composable
fun Champ(
    valeur: String,
    intitule: String,
    modifier: Modifier = Modifier,
    surUneLigne: Boolean = true,
    indication: String = "",
    surChangement: (String) -> Unit
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = surChangement,
        label = { Text(intitule, style = Detail) },
        placeholder = if (indication.isBlank()) null else {
            { Text(indication, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        singleLine = surUneLigne,
        textStyle = Corps,
        shape = RoundedCornerShape(11.dp),
        modifier = modifier
    )
}

/**
 * Bandeau d'état, de hauteur constante.
 *
 * Il occupe la même place qu'il ait quelque chose à dire ou non : c'est ce qui
 * empêche l'apparition d'un avertissement de faire sauter tout ce qui suit.
 */
@Composable
fun BandeauEtat(texte: String, alerte: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .background(
                if (texte.isBlank()) androidx.compose.ui.graphics.Color.Transparent
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            texte,
            style = Detail,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (alerte) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Une pastille de valeur, pour les états courts. */
@Composable
fun Pastille(texte: String, accent: Boolean = false) {
    Text(
        texte,
        style = Detail,
        color = if (accent) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                if (accent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

/** Bouton discret, aligné sur le reste. */
@Composable
fun BoutonPlat(
    texte: String,
    actif: Boolean = true,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    surClic: () -> Unit
) {
    Box(
        modifier
            .background(
                when {
                    !actif -> MaterialTheme.colorScheme.surfaceVariant
                    accent -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(11.dp)
            )
            .clickable(enabled = actif) { surClic() }
            .padding(horizontal = 15.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texte,
            style = Corps,
            maxLines = 1,
            color = when {
                !actif -> MaterialTheme.colorScheme.outline
                accent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
fun LigneBoutons(contenu: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Interligne),
        content = contenu
    )
}
