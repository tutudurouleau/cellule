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

/* Un seul accent, ambre, comme l'aiguille d'une cellule ou la lampe d'un
   labo. Le reste est neutre : on lit des chiffres, pas des couleurs. */

private val Sombre = darkColorScheme(
    primary = Color(0xFFE8A13C),
    onPrimary = Color(0xFF17140D),
    primaryContainer = Color(0xFF3A2C17),
    onPrimaryContainer = Color(0xFFF3C075),
    secondary = Color(0xFFA9A396),
    onSecondary = Color(0xFF17140D),
    background = Color(0xFF0E0E0D),
    onBackground = Color(0xFFF2EFE7),
    surface = Color(0xFF181816),
    onSurface = Color(0xFFF2EFE7),
    surfaceVariant = Color(0xFF232320),
    onSurfaceVariant = Color(0xFF9E9889),
    outline = Color(0xFF35342E),
    outlineVariant = Color(0xFF26251F),
    error = Color(0xFFE58B5C),
    onError = Color(0xFF17140D),
    surfaceContainer = Color(0xFF1D1D1A),
    surfaceContainerHigh = Color(0xFF232320)
)

private val Clair = lightColorScheme(
    primary = Color(0xFF9C5108),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7E7D3),
    onPrimaryContainer = Color(0xFF7A3F06),
    secondary = Color(0xFF5C584E),
    onSecondary = Color.White,
    background = Color(0xFFF5F3EF),
    onBackground = Color(0xFF17160F),
    surface = Color(0xFFFFFDFA),
    onSurface = Color(0xFF17160F),
    surfaceVariant = Color(0xFFEDE9E1),
    onSurfaceVariant = Color(0xFF6B665C),
    outline = Color(0xFFDCD7CD),
    outlineVariant = Color(0xFFE8E4DB),
    error = Color(0xFF9A3B12),
    onError = Color.White,
    surfaceContainer = Color(0xFFFAF8F4),
    surfaceContainerHigh = Color(0xFFF0EDE7)
)

@Composable
fun CelluleTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (sombre) Sombre else Clair, content = contenu)
}

/*
 * Chiffres à chasse fixe partout — « tnum ».
 *
 * Sans cela, passer de 13,7 à 11,1 change la largeur du nombre, et tout ce qui
 * suit se déplace. Un posemètre dont l'affichage sautille à chaque image est
 * illisible à bout de bras.
 */
private const val CHASSE_FIXE = "tnum"

val ChiffreEnorme = TextStyle(
    fontSize = 54.sp, lineHeight = 56.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = (-1.6).sp,
    fontFeatureSettings = CHASSE_FIXE
)

val ChiffreGrand = TextStyle(
    fontSize = 26.sp, lineHeight = 30.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp,
    fontFeatureSettings = CHASSE_FIXE
)

val ChiffreMoyen = TextStyle(
    fontSize = 17.sp, lineHeight = 22.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = CHASSE_FIXE
)

val Corps = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontFeatureSettings = CHASSE_FIXE)

val Detail = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontFeatureSettings = CHASSE_FIXE)

val StyleEtiquette = TextStyle(
    fontSize = 11.sp, lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp
)

val TitreCarte = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)

/** Rythme vertical unique, pour que rien ne flotte au hasard. */
val Gouttiere = 14.dp
val Interligne = 8.dp
