package fr.cellule.app.ecrans

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonCarte
import fr.cellule.app.RayonControle
import fr.cellule.app.RayonFlottant
import fr.cellule.app.StyleEtiquette
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
            .replace(' ', ' ').replace(' ', ' ') + " lx"
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

fun fmtDegres(a: Double): String = if (a.isNaN()) "—" else fmt(a, 1) + "°"

/* ── Blocs ─────────────────────────────────────────────────────────────── */

@Composable
fun Etiquette(texte: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Text(
        texte.uppercase(Locale.FRANCE),
        style = StyleEtiquette,
        color = if (accent) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Une carte : surface empilée par contraste de ton, jamais de bordure. */
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
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(RayonCarte))
            .padding(horizontal = Gouttiere, vertical = Gouttiere + 2.dp)
    ) {
        if (titre != null) {
            Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        }
        if (sousTitre != null) {
            Spacer(Modifier.height(4.dp))
            Text(sousTitre, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (titre != null || sousTitre != null) Spacer(Modifier.height(Interligne + 2.dp))
        contenu()
    }
}

/** La carte de tête : celle qui porte la mesure, plus contrastée que les autres. */
@Composable
fun CarteInstrument(modifier: Modifier = Modifier, contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(RayonCarte))
            .padding(horizontal = Gouttiere + 2.dp, vertical = Gouttiere + 4.dp),
        content = contenu
    )
}

/**
 * Poignée décorative en tête d'un panneau flottant ou d'un tiroir — l'affordance
 * qui dit « ceci se manipule », même quand on n'implémente pas le glissé lui-même.
 */
@Composable
fun Poignee(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(vertical = 10.dp)
            .width(34.dp)
            .height(4.dp)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(50))
    )
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
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
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

/** Un choix parmi quelques-uns, en gélule. */
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
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val actif = option == selection
            val fond by animateColorAsState(
                if (actif) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                label = "fond"
            )
            Box(
                Modifier
                    .weight(1f)
                    .background(fond, RoundedCornerShape(50))
                    .clickable { surChoix(option) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    libelle(option),
                    style = Corps,
                    maxLines = 1,
                    color = if (actif) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Sélecteur : déclencheur compact, réponse en tiroir qui remonte du bas.
 *
 * Un menu déroulant qui s'ouvre sous le doigt est illisible dès qu'on tient le
 * téléphone à bout de bras ; une feuille qui remonte du bas, elle, se lit et se
 * touche sans viser.
 */
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
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(RayonControle))
            .clickable { ouvert = true }
            .padding(horizontal = 13.dp, vertical = 10.dp)
    ) {
        if (intitule.isNotBlank()) {
            Etiquette(intitule)
            Spacer(Modifier.height(3.dp))
        }
        Text(
            libelle(selection) + "  ›",
            style = Corps,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    if (ouvert) {
        FeuilleChoix(
            titre = intitule,
            options = options,
            libelle = libelle,
            selection = selection,
            onDismiss = { ouvert = false },
            onChoix = { surChoix(it); ouvert = false }
        )
    }
}

/** La feuille elle-même, réutilisable pour un choix qui n'a pas de déclencheur
    dédié (par exemple depuis une autre feuille, ou un bouton). */
@Composable
fun <T> FeuilleChoix(
    titre: String,
    options: List<T>,
    libelle: (T) -> String,
    selection: T?,
    onDismiss: () -> Unit,
    onChoix: (T) -> Unit
) {
    val etat: SheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = etat,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { Poignee() }
    ) {
        Column(Modifier.padding(bottom = Gouttiere + 8.dp)) {
            if (titre.isNotBlank()) {
                Text(
                    titre,
                    style = TitreCarte,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Gouttiere * 1.25f, vertical = Interligne)
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    val actif = option == selection
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onChoix(option) }
                            .padding(horizontal = Gouttiere * 1.25f, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            libelle(option),
                            style = Corps,
                            color = if (actif) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (actif) {
                            Box(
                                Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
                                    .width(7.dp).height(7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Champ de saisie minimal : fond plein, aucun contour. */
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
        shape = RoundedCornerShape(RayonControle),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
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
            .heightIn(min = 36.dp)
            .background(
                if (texte.isBlank()) Color.Transparent
                else if (alerte) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(RayonControle)
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
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

/** Une pastille de valeur, cliquable ou non. */
@Composable
fun Pastille(
    texte: String,
    accent: Boolean = false,
    modifier: Modifier = Modifier,
    surClic: (() -> Unit)? = null
) {
    val fond by animateColorAsState(
        if (accent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "pastille"
    )
    Box(
        modifier
            .background(fond, RoundedCornerShape(50))
            .then(if (surClic != null) Modifier.clickable { surClic() } else Modifier)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texte,
            style = Corps,
            maxLines = 1,
            color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Bouton plat, aligné sur le reste. */
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
                    accent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(RayonControle)
            )
            .clickable(enabled = actif) { surClic() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texte,
            style = Corps,
            maxLines = 1,
            color = when {
                !actif -> MaterialTheme.colorScheme.outline
                accent -> MaterialTheme.colorScheme.onPrimary
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

/**
 * Panneau flottant : le conteneur des écrans caméra, ancré en bas, coins
 * arrondis seulement en haut, fond quasi opaque plutôt qu'une vraie
 * transparence dépoli — le flou d'arrière-plan coûterait une dépendance et un
 * risque de compilation pour un gain qu'une teinte à haute opacité obtient déjà.
 */
@Composable
fun PanneauFlottant(modifier: Modifier = Modifier, contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                RoundedCornerShape(topStart = RayonFlottant, topEnd = RayonFlottant)
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Poignee()
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 2.dp), content = contenu)
    }
}
