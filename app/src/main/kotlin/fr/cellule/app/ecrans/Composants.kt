package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs

/* ── Mise en forme ─────────────────────────────────────────────────────── */

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
            .replace('\u00A0', ' ').replace('\u202F', ' ') + " lx"
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

/* ── Blocs d'interface ─────────────────────────────────────────────────── */

@Composable
fun Carte(
    titre: String? = null,
    sousTitre: String? = null,
    contenu: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            if (titre != null) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
            }
            if (sousTitre != null) {
                Text(
                    sousTitre,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
            }
            contenu()
        }
    }
}

/** Une ligne « intitulé · valeur », avec un détail optionnel sous l'intitulé. */
@Composable
fun Ligne(intitule: String, valeur: String, detail: String? = null, accent: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                intitule,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            valeur,
            style = MaterialTheme.typography.bodyMedium,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Un choix parmi quelques-uns, présenté en bandeau. */
@Composable
fun <T> ChoixSegmente(
    options: List<T>,
    selection: T,
    libelle: (T) -> String,
    surChoix: (T) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val actif = option == selection
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (actif) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable { surChoix(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    libelle(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (actif) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Une liste déroulante, sans API expérimentale. */
@Composable
fun <T> Deroulant(
    intitule: String,
    options: List<T>,
    selection: T,
    libelle: (T) -> String,
    largeur: Int = 0,
    surChoix: (T) -> Unit
) {
    var ouvert by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { ouvert = true },
            modifier = if (largeur > 0) Modifier.width(largeur.dp) else Modifier
        ) {
            Text(
                if (intitule.isEmpty()) libelle(selection) else "$intitule : ${libelle(selection)}",
                style = MaterialTheme.typography.labelLarge
            )
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(libelle(option)) },
                    onClick = { surChoix(option); ouvert = false }
                )
            }
        }
    }
}

/** Un avertissement discret mais lisible : la lecture n'est pas fiable. */
@Composable
fun Alerte(texte: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            texte,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
