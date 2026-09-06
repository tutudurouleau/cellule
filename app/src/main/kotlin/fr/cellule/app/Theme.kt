package fr.cellule.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Ambre = Color(0xFFE8A13C)
private val AmbreSombre = Color(0xFFA8560A)

private val Sombre = darkColorScheme(
    primary = Ambre,
    onPrimary = Color(0xFF17140D),
    primaryContainer = Color(0xFF33281A),
    onPrimaryContainer = Ambre,
    secondary = Color(0xFFA9A396),
    background = Color(0xFF111110),
    onBackground = Color(0xFFF1EEE6),
    surface = Color(0xFF1A1A17),
    onSurface = Color(0xFFF1EEE6),
    surfaceVariant = Color(0xFF232320),
    onSurfaceVariant = Color(0xFFA9A396),
    outline = Color(0xFF3A3933),
    error = Color(0xFFE0794E)
)

private val Clair = lightColorScheme(
    primary = AmbreSombre,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E6D2),
    onPrimaryContainer = AmbreSombre,
    secondary = Color(0xFF57534A),
    background = Color(0xFFF4F2EE),
    onBackground = Color(0xFF16150F),
    surface = Color(0xFFFFFDF9),
    onSurface = Color(0xFF16150F),
    surfaceVariant = Color(0xFFEBE7DF),
    onSurfaceVariant = Color(0xFF57534A),
    outline = Color(0xFFDDD8CF),
    error = Color(0xFFA33A12)
)

/* Chiffres à chasse fixe : un posemètre dont les valeurs sautillent est
   illisible à bout de bras. */
private val typographie = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun CelluleTheme(sombre: Boolean = isSystemInDarkTheme(), contenu: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (sombre) Sombre else Clair,
        typography = typographie,
        content = contenu
    )
}

val StyleChiffre = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 46.sp)
