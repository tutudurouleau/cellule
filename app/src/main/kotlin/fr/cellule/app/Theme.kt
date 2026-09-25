package fr.cellule.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Un instrument, pas une application décorative.
 *
 * Deux matières : en sombre, un noir chaud de chambre noire ; en clair, un
 * papier crème, lisible en plein soleil sur un écran LCD. Les surfaces
 * s'empilent par contraste de ton, bordées d'un filet à peine visible.
 *
 * Chaque onglet a sa teinte — l'ambre de l'aiguille pour Mesurer, le rouge
 * inactinique pour le Labo… — qui colore l'en-tête, la pilule de navigation
 * et les chiffres. Le reste de la palette ne bouge pas : on reconnaît où
 * l'on est sans que l'appli change de visage.
 *
 * Ni flou ni animation permanente : sur un téléphone de milieu de gamme, le
 * graphisme ne doit rien coûter à la batterie.
 */

private val Sombre = darkColorScheme(
    background = Color(0xFF0D0C0B),
    onBackground = Color(0xFFF3EEE6),
    surface = Color(0xFF151412),
    onSurface = Color(0xFFF3EEE6),
    surfaceVariant = Color(0xFF26231F),
    onSurfaceVariant = Color(0xFFA9A194),
    surfaceContainer = Color(0xFF1A1816),
    surfaceContainerHigh = Color(0xFF24211D),
    surfaceContainerHighest = Color(0xFF2E2A25),
    outline = Color(0xFF3D3831),
    outlineVariant = Color(0xFF2A2622),
    secondary = Color(0xFF8FB3E8),
    onSecondary = Color(0xFF0B1A30),
    secondaryContainer = Color(0xFF1D2E4A),
    onSecondaryContainer = Color(0xFFC9DBF7),
    error = Color(0xFFEF9068),
    onError = Color(0xFF2A1206)
)

private val Clair = lightColorScheme(
    background = Color(0xFFF4F0E8),
    onBackground = Color(0xFF1A1714),
    surface = Color(0xFFFBF8F3),
    onSurface = Color(0xFF1A1714),
    surfaceVariant = Color(0xFFEBE5DA),
    onSurfaceVariant = Color(0xFF6E665A),
    surfaceContainer = Color(0xFFFFFDF9),
    surfaceContainerHigh = Color(0xFFEEE8DE),
    surfaceContainerHighest = Color(0xFFE5DED1),
    outline = Color(0xFFD4CBBD),
    outlineVariant = Color(0xFFE6DFD3),
    secondary = Color(0xFF2F5597),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE6F8),
    onSecondaryContainer = Color(0xFF1D3E75),
    error = Color(0xFF9A3B12),
    onError = Color.White
)

/** L'accent d'un onglet : sa couleur, le texte posé dessus, son fond doux et le texte posé sur ce fond. */
class Accent(val couleur: Color, val surCouleur: Color, val fond: Color, val surFond: Color)

enum class Teinte(val sombre: Accent, val clair: Accent) {
    /** L'aiguille de la cellule. */
    AMBRE(
        Accent(Color(0xFFF0A93F), Color(0xFF241703), Color(0xFF43300F), Color(0xFFFFCE86)),
        Accent(Color(0xFF8F4A05), Color.White, Color(0xFFFBE7CD), Color(0xFF6B3703))
    ),
    /** Le soleil qu'on estime sans instrument. */
    OR(
        Accent(Color(0xFFE8C766), Color(0xFF221B04), Color(0xFF3D3310), Color(0xFFF5E3A5)),
        Accent(Color(0xFF7A5E00), Color.White, Color(0xFFF6ECC6), Color(0xFF5A4500))
    ),
    /** Le verre du viseur. */
    VERT_EAU(
        Accent(Color(0xFF7CCBB5), Color(0xFF062019), Color(0xFF173A31), Color(0xFFB8EADB)),
        Accent(Color(0xFF1F6B58), Color.White, Color(0xFFD2EFE6), Color(0xFF11503F))
    ),
    /** L'encre du carnet. */
    ENCRE(
        Accent(Color(0xFF8FB3E8), Color(0xFF0B1A30), Color(0xFF1D2E4A), Color(0xFFC9DBF7)),
        Accent(Color(0xFF2F5597), Color.White, Color(0xFFDCE6F8), Color(0xFF1D3E75))
    ),
    /** La lumière inactinique de la chambre noire. */
    INACTINIQUE(
        Accent(Color(0xFFE8705E), Color(0xFF2B0A05), Color(0xFF4A1A13), Color(0xFFFFC0B4)),
        Accent(Color(0xFFA33A28), Color.White, Color(0xFFF8DDD7), Color(0xFF7A2718))
    ),
    /** Le papier des tables de référence. */
    GRIS_CHAUD(
        Accent(Color(0xFFC9BBA5), Color(0xFF221D15), Color(0xFF38322A), Color(0xFFE8DECF)),
        Accent(Color(0xFF5E5446), Color.White, Color(0xFFECE5DA), Color(0xFF453C30))
    ),
    /** Le laiton du matériel de tournage. */
    CUIVRE(
        Accent(Color(0xFFDE9A6B), Color(0xFF2A1508), Color(0xFF45291A), Color(0xFFF8CFB2)),
        Accent(Color(0xFF94502A), Color.White, Color(0xFFF6E0D2), Color(0xFF6E3517))
    )
}

/**
 * Le thème de l'appli, dans la teinte de l'onglet ouvert.
 *
 * La teinte ne s'arrête pas à l'accent : elle imprègne aussi le fond et les
 * surfaces, comme un papier légèrement coloré (en clair) ou un noir teinté
 * (en sombre). Assez pour qu'on sache où l'on est, jamais au point de gêner
 * la lecture. Le passage d'une teinte à l'autre se fait en un fondu bref :
 * une seule animation, au moment du changement, jamais en continu.
 */
@Composable
fun CelluleTheme(sombre: Boolean = isSystemInDarkTheme(), teinte: Teinte = Teinte.AMBRE, contenu: @Composable () -> Unit) {
    val base = if (sombre) Sombre else Clair
    val a = if (sombre) teinte.sombre else teinte.clair
    val duree = tween<Color>(durationMillis = 260)
    val couleur by animateColorAsState(a.couleur, duree, label = "accent")
    val surCouleur by animateColorAsState(a.surCouleur, duree, label = "surAccent")
    val fond by animateColorAsState(a.fond, duree, label = "fondAccent")
    val surFond by animateColorAsState(a.surFond, duree, label = "surFondAccent")
    /* Dosage de la teinte dans chaque couche : le fond plus que les cartes. */
    fun teinter(c: Color, part: Float) = lerp(c, couleur, part)
    val (pFond, pCarte, pVariante) = if (sombre) Triple(0.075f, 0.05f, 0.09f) else Triple(0.085f, 0.02f, 0.10f)
    MaterialTheme(
        colorScheme = base.copy(
            primary = couleur,
            onPrimary = surCouleur,
            primaryContainer = fond,
            onPrimaryContainer = surFond,
            background = teinter(base.background, pFond),
            surface = teinter(base.surface, pCarte),
            surfaceContainer = teinter(base.surfaceContainer, pCarte),
            surfaceContainerHigh = teinter(base.surfaceContainerHigh, pVariante),
            surfaceContainerHighest = teinter(base.surfaceContainerHighest, pVariante),
            surfaceVariant = teinter(base.surfaceVariant, pVariante),
            outlineVariant = teinter(base.outlineVariant, pVariante),
            /* Les rails des curseurs (Slider) prennent cette couleur : dans la
               teinte de l'onglet plutôt qu'un bleu venu d'ailleurs. */
            secondary = couleur,
            onSecondary = surCouleur,
            secondaryContainer = teinter(base.surfaceContainerHighest, 0.22f),
            onSecondaryContainer = base.onSurface
        ),
        content = contenu
    )
}

/*
 * La police de Cellule : Space Grotesk (SIL Open Font License, texte dans
 * assets/licences), pour les titres, les étiquettes et les chiffres — le
 * texte courant reste dans la police du téléphone, la plus lisible. Les
 * Paramètres permettent de revenir partout à la police du téléphone.
 */
object Typo {
    var cellule by mutableStateOf(true)
}

@OptIn(ExperimentalTextApi::class)
private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Light),
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

/** La famille des titres et des chiffres, selon le choix des Paramètres. */
private fun affiche(): FontFamily = if (Typo.cellule) SpaceGrotesk else FontFamily.Default

/*
 * Chiffres à chasse fixe partout — « tnum ».
 *
 * Sans cela, passer de 13,7 à 11,1 change la largeur du nombre et tout ce qui
 * suit se déplace. Un instrument dont l'affichage sautille à chaque image est
 * illisible à bout de bras.
 */
private const val CHASSE_FIXE = "tnum"

val ChiffreHero: TextStyle
    get() = TextStyle(
        fontFamily = affiche(),
        fontSize = 68.sp, lineHeight = 68.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-2.6).sp,
        fontFeatureSettings = CHASSE_FIXE
    )

val ChiffreEnorme: TextStyle
    get() = TextStyle(
        fontFamily = affiche(),
        fontSize = 48.sp, lineHeight = 50.sp,
        fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp,
        fontFeatureSettings = CHASSE_FIXE
    )

val ChiffreGrand: TextStyle
    get() = TextStyle(
        fontFamily = affiche(),
        fontSize = 28.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp,
        fontFeatureSettings = CHASSE_FIXE
    )

val TitreEcran: TextStyle
    get() = TextStyle(
        fontFamily = affiche(),
        fontSize = 26.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp
    )

val TitreCarte: TextStyle
    get() = TextStyle(fontFamily = affiche(), fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold)

val Corps = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFeatureSettings = CHASSE_FIXE)

val Detail = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontFeatureSettings = CHASSE_FIXE)

val StyleEtiquette: TextStyle
    get() = TextStyle(
        fontFamily = affiche(),
        fontSize = 10.5.sp, lineHeight = 14.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp
    )

/* Un seul rythme, une seule famille de rayons — poussée vers le « squircle »
   plutôt que le rectangle à coins légèrement arrondis d'il y a dix ans. */
val Gouttiere = 16.dp
val Interligne = 10.dp
val RayonCarte = 28.dp
val RayonControle = 16.dp
val RayonFlottant = 32.dp

/** Hauteur réservée en bas d'écran pour que rien ne se cache sous la pilule de
    navigation flottante — approximative mais généreuse, jamais mesurée pile. */
val ZoneNavFlottante = 100.dp
