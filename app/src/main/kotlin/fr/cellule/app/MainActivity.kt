package fr.cellule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.LaunchedEffect
import fr.cellule.app.ecrans.ChoixSegmente
import fr.cellule.app.ecrans.Etiquette
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cellule.app.chrono.Minuteur
import fr.cellule.app.ecrans.EcranCameras
import fr.cellule.app.ecrans.EcranEstimer
import fr.cellule.app.ecrans.EcranFocales
import fr.cellule.app.ecrans.EcranJournal
import fr.cellule.app.ecrans.EcranLabo
import fr.cellule.app.ecrans.EcranMesurer
import fr.cellule.app.ecrans.EcranTables
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.TirageNote
import fr.cellule.core.TYPES_DE_SCENE

/** Ce que les écrans se passent entre eux. */
class EtatApplication {
    /** Estimation à verser dans la case « annoncé » du carnet. */
    var annonceProposee by mutableStateOf<Double?>(null)

    /** Mesure du posemètre à verser dans la case « mesuré ». */
    var derniereMesure by mutableStateOf<Double?>(null)

    /** Le couple retenu au moment de la mesure, pour le noter avec la vue. */
    var dernierReglage by mutableStateOf("")

    /** Un développement préparé par le Labo, à reprendre dans le carnet. */
    var developpementPropose by mutableStateOf<DeveloppementNote?>(null)

    /** Un tirage préparé par le calcul en diaphs, à reprendre dans le carnet. */
    var tiragePropose by mutableStateOf<TirageNote?>(null)

    var derniereEstimation by mutableStateOf<Double?>(null)
    var dernierTypeScene by mutableStateOf(TYPES_DE_SCENE.first())
    var derniereChaine by mutableStateOf("")
}

/*
 * Une iconographie dessinée plutôt que le jeu Material par défaut : c'est ce
 * dernier, plus que n'importe quelle couleur, qui donne à une application son
 * air d'avoir dix ans. Des glyphes au trait, même épaisseur, même esprit.
 */
internal fun icone(nom: String, trace: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name = nom, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            addPath(
                pathData = PathData(trace),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()

/** Cadre de mise au point : le langage visuel du viseur, pour « Mesurer ». */
private val IconeMesurer = icone("mesurer") {
    moveTo(4f, 8f); lineTo(4f, 4f); lineTo(8f, 4f)
    moveTo(16f, 4f); lineTo(20f, 4f); lineTo(20f, 8f)
    moveTo(20f, 16f); lineTo(20f, 20f); lineTo(16f, 20f)
    moveTo(8f, 20f); lineTo(4f, 20f); lineTo(4f, 16f)
    moveTo(12f, 10.6f); lineTo(13.4f, 12f); lineTo(12f, 13.4f); lineTo(10.6f, 12f); close()
}

/** Un petit soleil : estimer sans instrument. */
private val IconeEstimer = icone("estimer") {
    moveTo(12f, 9.3f); lineTo(14.7f, 12f); lineTo(12f, 14.7f); lineTo(9.3f, 12f); close()
    moveTo(12f, 3f); lineTo(12f, 6f)
    moveTo(12f, 21f); lineTo(12f, 18f)
    moveTo(3f, 12f); lineTo(6f, 12f)
    moveTo(21f, 12f); lineTo(18f, 12f)
    moveTo(5.6f, 5.6f); lineTo(7.7f, 7.7f)
    moveTo(18.4f, 18.4f); lineTo(16.3f, 16.3f)
    moveTo(18.4f, 5.6f); lineTo(16.3f, 7.7f)
    moveTo(5.6f, 18.4f); lineTo(7.7f, 16.3f)
}

/** Deux cadres emboîtés : ce qu'une focale plus longue retient du cadre large. */
private val IconeFocales = icone("focales") {
    moveTo(3f, 5.5f); lineTo(21f, 5.5f); lineTo(21f, 18.5f); lineTo(3f, 18.5f); close()
    moveTo(9f, 9.5f); lineTo(15f, 9.5f); lineTo(15f, 14.5f); lineTo(9f, 14.5f); close()
}

/** Une caméra de cinéma et ses deux bobines : le matériel de tournage. */
private val IconeCameras = icone("cameras") {
    moveTo(4f, 7.5f); arcTo(2.8f, 2.8f, 0f, true, true, 9.6f, 7.5f); arcTo(2.8f, 2.8f, 0f, true, true, 4f, 7.5f); close()
    moveTo(10.6f, 7.5f); arcTo(2.8f, 2.8f, 0f, true, true, 16.2f, 7.5f); arcTo(2.8f, 2.8f, 0f, true, true, 10.6f, 7.5f); close()
    moveTo(3f, 12f); lineTo(16f, 12f); lineTo(16f, 19.5f); lineTo(3f, 19.5f); close()
    moveTo(16f, 14f); lineTo(21f, 11.5f); lineTo(21f, 20f); lineTo(16f, 17.5f); close()
}

/** Un carnet : reliure et lignes. */
private val IconeCarnet = icone("carnet") {
    moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(5f, 20f); close()
    moveTo(8f, 4f); lineTo(8f, 20f)
    moveTo(11.3f, 9f); lineTo(16f, 9f)
    moveTo(11.3f, 13f); lineTo(16f, 13f)
    moveTo(11.3f, 17f); lineTo(14f, 17f)
}

/** Une fiole d'Erlenmeyer et son niveau de liquide : le labo. */
private val IconeLabo = icone("labo") {
    moveTo(9f, 3.5f); lineTo(15f, 3.5f)
    moveTo(10f, 3.5f); lineTo(10f, 9.5f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(14f, 9.5f); lineTo(14f, 3.5f)
    moveTo(7.4f, 15f); lineTo(16.6f, 15f)
}

/** Une grille : les tables de référence. */
private val IconeTables = icone("tables") {
    moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
    moveTo(4f, 10.3f); lineTo(20f, 10.3f)
    moveTo(10f, 5f); lineTo(10f, 19f)
    moveTo(15f, 5f); lineTo(15f, 19f)
}

/** Trois réglettes : les paramètres. */
private val IconeReglages = icone("reglages") {
    moveTo(4f, 6f); lineTo(20f, 6f)
    moveTo(4f, 12f); lineTo(20f, 12f)
    moveTo(4f, 18f); lineTo(20f, 18f)
    moveTo(9f, 4f); lineTo(9f, 8f)
    moveTo(15f, 10f); lineTo(15f, 14f)
    moveTo(7f, 16f); lineTo(7f, 20f)
}

/*
 * Dans l'ordre du geste : on mesure, on estime, on cadre, on note, on
 * développe, on vérifie — et le matériel de cinéma ferme la marche.
 */
private enum class Onglet(
    val titre: String,
    val sousTitre: String,
    val icone: ImageVector,
    val plein: Boolean,
    val teinte: Teinte
) {
    MESURER("Mesurer", "posemètre réfléchi et incident", IconeMesurer, true, Teinte.AMBRE),
    ESTIMER("Estimer", "la chaîne de facteurs, sans cellule", IconeEstimer, false, Teinte.OR),
    FOCALES("Focales", "apprendre à voir les angles de champ", IconeFocales, true, Teinte.VERT_EAU),
    CARNET("Carnet", "ce que tu as photographié, et ton biais", IconeCarnet, false, Teinte.ENCRE),
    LABO("Labo", "développer, tirer, et comprendre pourquoi", IconeLabo, false, Teinte.INACTINIQUE),
    TABLES("Tables", "les repères à retenir", IconeTables, false, Teinte.GRIS_CHAUD),
    CAMERAS("Caméras", "fiches et quiz du matériel de tournage", IconeCameras, false, Teinte.CUIVRE)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* Le contenu dessine sous les barres système : les écrans caméra en
           profitent pour un viseur réellement plein écran, les autres gèrent
           leur propre respiration en haut via statusBarsPadding(). */
        enableEdgeToEdge()
        setContent { Application() }
    }
}

@Composable
private fun Application() {
    val contexte = LocalContext.current
    val reglages = remember { Reglages(contexte).also { Typo.cellule = it.police != "systeme" } }
    val depot = remember { DepotJournal(contexte) }
    val pronostics = remember { DepotPronostics(contexte) }
    val labo = remember { DepotLabo(contexte) }
    val tirages = remember { DepotTirages(contexte) }
    val etat = remember { EtatApplication() }
    /* Un chrono labo en cours ramène au Labo, par exemple depuis sa notification. */
    var onglet by remember { mutableStateOf(if (Minuteur.enCours) Onglet.LABO else Onglet.MESURER) }
    var parametres by remember { mutableStateOf(false) }
    LaunchedEffect(reglages.police) { Typo.cellule = reglages.police != "systeme" }

    /* L'en-tête se replie quand on fait défiler vers le bas, se déplie quand
       on remonte : il prend d'abord ce que le défilement lui cède. */
    val densite = LocalDensity.current
    val repliMax = with(densite) { ReplieEnTete.toPx() }
    var repli by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(onglet) { repli = 0f }
    val connexion = remember(repliMax) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f) return Offset.Zero
                val avant = repli
                repli = (repli - available.y).coerceAtMost(repliMax)
                return Offset(0f, avant - repli)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f) return Offset.Zero
                val avant = repli
                repli = (repli - available.y).coerceAtLeast(0f)
                return Offset(0f, avant - repli)
            }
        }
    }

    /* Par défaut l'appli suit le téléphone ; les paramètres peuvent l'imposer. */
    val sombre = when (reglages.apparence) {
        "clair" -> false
        "sombre" -> true
        else -> isSystemInDarkTheme()
    }
    /* Les icônes de la barre d'état suivent le thème de l'appli, pas celui du téléphone. */
    val activite = contexte as? ComponentActivity
    LaunchedEffect(sombre) {
        val style = if (sombre) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        activite?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    CelluleTheme(sombre = sombre, teinte = onglet.teinte) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {

                Column(Modifier.fillMaxSize()) {
                    /* Les écrans caméra n'ont pas d'en-tête : chaque pixel du haut de
                       l'écran leur revient, comme dans un vrai viseur. */
                    if (!onglet.plein) {
                        EnTeteEcran(onglet.titre, onglet.sousTitre, onglet.icone, repli / repliMax) { parametres = true }
                    }
                    Box(Modifier.weight(1f).nestedScroll(connexion)) {
                        when (onglet) {
                            Onglet.MESURER -> EcranMesurer(reglages, etat)
                            Onglet.ESTIMER -> EcranEstimer(reglages, etat)
                            Onglet.FOCALES -> EcranFocales()
                            Onglet.CAMERAS -> EcranCameras()
                            Onglet.CARNET -> EcranJournal(depot, reglages, etat, labo, tirages, pronostics)
                            Onglet.LABO -> EcranLabo(pronostics, etat, labo, tirages)
                            Onglet.TABLES -> EcranTables()
                        }
                    }
                }

                NavigationFlottante(onglet = onglet, surChoix = { onglet = it })
            }
            if (parametres) FeuilleParametres(reglages) { parametres = false }
        }
    }
}

/** Ce que l'en-tête perd en se repliant. */
private val ReplieEnTete = 46.dp

/**
 * Le grand en-tête des écrans qui ne sont pas plein cadre : un bandeau dans
 * la teinte de l'onglet, son icône en filigrane, le titre en grand. Il se
 * replie en une ligne quand on descend dans la page ([repli] de 0 à 1), sans
 * animation propre : il suit le doigt.
 */
@Composable
private fun EnTeteEcran(titre: String, sousTitre: String, icone: ImageVector, repli: Float, ouvrirParametres: () -> Unit) {
    val f = repli.coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = RayonCarte, bottomEnd = RayonCarte))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        /* L'icône de l'onglet, en très grand et presque effacée. */
        Icon(
            icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f * (1f - f)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = 10.dp)
                .size(150.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = Gouttiere, end = 4.dp, top = 6.dp, bottom = lerp(16.dp, 8.dp, f))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Text(
                    titre,
                    style = TitreEcran.copy(fontSize = lerp(32.sp, 22.sp, f), lineHeight = lerp(36.sp, 26.sp, f)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { ouvrirParametres() }
                        .padding(12.dp)
                ) {
                    Icon(IconeReglages, contentDescription = "Paramètres", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                }
            }
            /* Le sous-titre s'efface et laisse sa place en se repliant. */
            Box(Modifier.height(lerp(24.dp, 0.dp, f)).padding(start = 50.dp).alpha(1f - f)) {
                Text(sousTitre, style = Detail, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f), maxLines = 1)
            }
        }
    }
}

/** Les paramètres : l'apparence, et la version installée. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeuilleParametres(reglages: Reglages, fermer: () -> Unit) {
    val contexte = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = fermer,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(Modifier.fillMaxWidth().padding(start = Gouttiere, end = Gouttiere, bottom = Gouttiere * 2)) {
            Text("Paramètres", style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(Interligne * 2))
            Etiquette("Apparence")
            Spacer(Modifier.height(6.dp))
            ChoixSegmente(
                options = listOf("systeme", "clair", "sombre"),
                selection = reglages.apparence,
                libelle = {
                    when (it) {
                        "clair" -> "Clair"
                        "sombre" -> "Sombre"
                        else -> "Système"
                    }
                }
            ) { reglages.apparence = it }
            Spacer(Modifier.height(6.dp))
            Text(
                "« Système » suit le réglage du téléphone. Au soleil, le clair se lit mieux ; en chambre noire, le sombre éblouit moins.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne * 2))
            Etiquette("Police")
            Spacer(Modifier.height(6.dp))
            ChoixSegmente(
                options = listOf("cellule", "systeme"),
                selection = reglages.police,
                libelle = { if (it == "systeme") "Du téléphone" else "Cellule" }
            ) { reglages.police = it }
            Spacer(Modifier.height(6.dp))
            Text(
                "« Cellule » dessine titres et chiffres en Space Grotesk ; le texte courant garde la police du téléphone.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Interligne * 2))
            val version = remember {
                runCatching { contexte.packageManager.getPackageInfo(contexte.packageName, 0).versionName }.getOrNull() ?: "?"
            }
            Text("Cellule $version", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Barre de navigation en pilule flottante.
 *
 * L'étiquette ne s'affiche que sous l'onglet actif : le reste se lit à
 * l'icône seule, ce qui laisse la pilule courte et légère au lieu d'occuper
 * toute la largeur comme une barre Material classique.
 *
 * La pilule repose sur une surface fixe du thème, jamais sur le flux caméra
 * lui-même : son contraste ne dépend donc jamais de ce qui est filmé derrière.
 *
 * Extension de BoxScope — c'est de là que vient align(), utilisable
 * uniquement à l'intérieur d'un Box.
 */
@Composable
private fun BoxScope.NavigationFlottante(onglet: Onglet, surChoix: (Onglet) -> Unit) {
    Row(
        Modifier
            .align(Alignment.BottomCenter)
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                RoundedCornerShape(50)
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        /* Sept onglets : sur un écran de moins de 420 dp, l'étiquette de
           l'onglet actif ferait déborder la pilule ; l'icône en couleur
           suffit alors. */
        val avecEtiquette = LocalConfiguration.current.screenWidthDp >= 420
        Onglet.entries.forEach { o -> OngletFlottant(o, o == onglet, avecEtiquette) { surChoix(o) } }
    }
}

@Composable
private fun RowScope.OngletFlottant(onglet: Onglet, actif: Boolean, avecEtiquette: Boolean, surClic: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (actif) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { surClic() }
            .padding(horizontal = if (actif && avecEtiquette) 14.dp else 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            onglet.icone,
            contentDescription = onglet.titre,
            tint = if (actif) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        AnimatedVisibility(visible = actif && avecEtiquette, enter = fadeIn(), exit = fadeOut()) {
            Text(
                onglet.titre,
                style = TitreCarte.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
