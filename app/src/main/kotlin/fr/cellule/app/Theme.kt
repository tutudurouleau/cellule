package fr.cellule.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Un instrument, pas une application décorative.
 *
 * Fonds profonds et neutres, surfaces empilées plutôt que bordées, un seul
 * accent ambre — celui d'une aiguille de cellule ou d'une lampe inactinique.
 * La couleur ne sert qu'à dire ce qui est vivant : la mesure, l'état actif.
 */

private val Sombre = darkColorScheme(
    primary = Color(0xFFF0A93F),
    onPrimary = Color(0xFF241703),
    primaryContainer = Color(0xFF43300F),
    onPrimaryContainer = Color(0xFFFFCE86),
    secondary = Color(0xFF6FA8C8),
    onSecondary = Color(0xFF0B1B24),
    secondaryContainer = Color(0xFF1B3140),
    onSecondaryContainer = Color(0xFFB6DCF0),
    background = Color(0xFF0B0B0A),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF141413),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF1F1F1D),
    onSurfaceVariant = Color(0xFF9C968A),
    surfaceContainer = Color(0xFF1A1A18),
    surfaceContainerHigh = Color(0xFF232320),
    surfaceContainerHighest = Color(0xFF2B2B27),
    outline = Color(0xFF3A3934),
    outlineVariant = Color(0xFF232320),
    error = Color(0xFFEF9068),
    onError = Color(0xFF2A1206)
)

private val Clair = lightColorScheme(
    primary = Color(0xFF8F4A05),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBE7CD),
    onPrimaryContainer = Color(0xFF6B3703),
    secondary = Color(0xFF2E6483),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8ECF7),
    onSecondaryContainer = Color(0xFF1B4A64),
    background = Color(0xFFF7F5F1),
    onBackground = Color(0xFF15140F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15140F),
    surfaceVariant = Color(0xFFEFEBE3),
    onSurfaceVariant = Color(0xFF635E55),
    surfaceContainer = Color(0xFFFBF9F6),
    surfaceContainerHigh = Color(0xFFF1EDE6),
    surfaceContainerHighest = Color(0xFFE9E4DA),
    outline = Color(0xFFD5CFC4),
    outlineVariant = Color(0xFFE9E4DA),
    error = Color(0xFF9A3B12),
    onError = Color.White
)

@Composable
fun CelluleTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (sombre) Sombre else Clair, content = contenu)
}

/*
 * Chiffres à chasse fixe partout — « tnum ».
 *
 * Sans cela, passer de 13,7 à 11,1 change la largeur du nombre et tout ce qui
 * suit se déplace. Un instrument dont l'affichage sautille à chaque image est
 * illisible à bout de bras.
 */
private const val CHASSE_FIXE = "tnum"

val ChiffreHero = TextStyle(
    fontSize = 64.sp, lineHeight = 64.sp,
    fontWeight = FontWeight.Medium, letterSpacing = (-2.4).sp,
    fontFeatureSettings = CHASSE_FIXE
)

val ChiffreEnorme = TextStyle(
    fontSize = 46.sp, lineHeight = 48.sp,
    fontWeight = FontWeight.Medium, letterSpacing = (-1.4).sp,
    fontFeatureSettings = CHASSE_FIXE
)

val ChiffreGrand = TextStyle(
    fontSize = 24.sp, lineHeight = 28.sp,
    fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp,
    fontFeatureSettings = CHASSE_FIXE
)

val TitreEcran = TextStyle(
    fontSize = 22.sp, lineHeight = 26.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp
)

val TitreCarte = TextStyle(fontSize = 15.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)

val Corps = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = CHASSE_FIXE)

val Detail = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontFeatureSettings = CHASSE_FIXE)

val StyleEtiquette = TextStyle(
    fontSize = 10.5.sp, lineHeight = 14.sp,
    fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp
)

/* Un seul rythme, une seule famille de rayons. */
val Gouttiere = 16.dp
val Interligne = 10.dp
val RayonCarte = 22.dp
val RayonControle = 14.dp
