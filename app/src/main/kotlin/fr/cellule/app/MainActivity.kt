package fr.cellule.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.style.TextOverflow
import fr.cellule.app.ecrans.Champ
import fr.cellule.app.ecrans.Pastille
import fr.cellule.core.sansAccents
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
import androidx.compose.ui.graphics.luminance
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
import fr.cellule.app.ecrans.EcranOngletCinema
import fr.cellule.app.ecrans.EcranTables
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.TirageNote
import fr.cellule.core.TYPES_DE_SCENE

/** Vrai quand l'écran est plus large que haut : les écrans s'y réorganisent. */
val LocalPaysage = androidx.compose.runtime.staticCompositionLocalOf { false }

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

    /** Un viseur en paysage, panneau replié : la barre des onglets s'efface pour laisser l'image entière. */
    var navMasquee by mutableStateOf(false)
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

/** Un clap : l'onglet Cinéma. */
private val IconeCinema = icone("cinema") {
    moveTo(4f, 10f); lineTo(20f, 10f); lineTo(20f, 20f); lineTo(4f, 20f); close()
    moveTo(4f, 10f); lineTo(3.3f, 6.3f); lineTo(18.8f, 3.6f); lineTo(19.5f, 7.4f); lineTo(4f, 10f)
    moveTo(8.4f, 5.4f); lineTo(10.2f, 8.9f)
    moveTo(13.4f, 4.5f); lineTo(15.2f, 8.1f)
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
    CAMERAS("Caméras", "fiches et quiz du matériel de tournage", IconeCameras, false, Teinte.CUIVRE),
    CINEMA("Cinéma", "tournage, plans et matériel", IconeCinema, true, Teinte.TALLY)
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
    val quiz = remember { DepotQuiz(contexte) }
    val cinema = remember { DepotCinema(contexte) }
    val etat = remember { EtatApplication() }
    /* Un chrono labo en cours ramène au Labo, par exemple depuis sa notification. */
    var onglet by remember { mutableStateOf(if (Minuteur.enCours) Onglet.LABO else Onglet.MESURER) }
    var parametres by remember { mutableStateOf(false) }
    /* Le lanceur : toutes les destinations de l'appli sur un seul écran. */
    var lanceur by remember { mutableStateOf(false) }
    var courante by remember { mutableStateOf(if (Minuteur.enCours) "labo" else "mesurer") }
    val epingles = reglages.navEpingles.split(',').mapNotNull { destination(it.trim()) }.distinct().take(4)
    val recents = reglages.navRecents.split(',').mapNotNull { destination(it.trim()) }
    /* Où l'on est : dans Cinéma, la vue et l'outil ouverts le disent ; ailleurs, la dernière destination choisie. */
    val ici: Destination? = if (onglet == Onglet.CINEMA) {
        DESTINATIONS.firstOrNull { it.onglet == Onglet.CINEMA && it.cineVue == reglages.cineVue && (it.outil == null || it.outil == reglages.lumiereOutil) }
    } else {
        destination(courante)?.takeIf { it.onglet == onglet } ?: DESTINATIONS.firstOrNull { it.onglet == onglet }
    }
    val aller: (Destination) -> Unit = { d ->
        d.cineVue?.let { reglages.cineVue = it }
        d.outil?.let { reglages.lumiereOutil = it }
        if (d.sous.isNotBlank()) Aiguillage.cible = d.sous
        onglet = d.onglet
        courante = d.id
        reglages.navRecents = (listOf(d.id) + recents.map { it.id }.filter { it != d.id }).take(5).joinToString(",")
        lanceur = false
    }
    val epingler: (Destination) -> Unit = { d ->
        val ids = epingles.map { it.id }
        reglages.navEpingles = (if (d.id in ids) ids - d.id else (ids + d.id).takeLast(4)).joinToString(",")
    }
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

    val configuration = LocalConfiguration.current
    val paysage = configuration.screenWidthDp > configuration.screenHeightDp
    androidx.compose.runtime.CompositionLocalProvider(LocalPaysage provides paysage) {
    CelluleTheme(sombre = sombre, teinte = onglet.teinte) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {

                /* En paysage, la pilule de navigation occupe le bord droit : le contenu s'arrête avant. */
                Column(Modifier.fillMaxSize().padding(end = if (paysage && !etat.navMasquee) 76.dp else 0.dp)) {
                    /* Les écrans caméra n'ont pas d'en-tête : chaque pixel du haut de
                       l'écran leur revient, comme dans un vrai viseur. */
                    if (!onglet.plein) {
                        EnTeteEcran(onglet.titre, onglet.sousTitre, onglet.icone, repli / repliMax) { parametres = true }
                    }
                    Box(Modifier.weight(1f).nestedScroll(connexion)) {
                        when (onglet) {
                            Onglet.MESURER -> EcranMesurer(reglages, etat, cinema)
                            Onglet.ESTIMER -> EcranEstimer(reglages, etat)
                            Onglet.FOCALES -> EcranFocales()
                            Onglet.CAMERAS -> EcranCameras(quiz, reglages)
                            Onglet.CARNET -> EcranJournal(depot, reglages, etat, labo, tirages, pronostics, cinema)
                            Onglet.LABO -> EcranLabo(pronostics, etat, labo, tirages)
                            Onglet.TABLES -> EcranTables()
                            Onglet.CINEMA -> EcranOngletCinema(reglages, etat, cinema, depot)
                        }
                    }
                }

                if (!(paysage && etat.navMasquee) && !lanceur) {
                    NavigationFlottante(epingles, ici, surTout = { lanceur = true }, surChoix = aller)
                }
                if (lanceur) {
                    Lanceur(
                        epingles = epingles.map { it.id }, recents = recents, ici = ici?.id,
                        surChoix = aller, surEpingle = epingler,
                        surParametres = {
                            lanceur = false
                            parametres = true
                        },
                        fermer = { lanceur = false }
                    )
                }
            }
            if (parametres) FeuilleParametres(reglages) { parametres = false }
        }
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
private fun BoxScope.NavigationFlottante(
    epingles: List<Destination>,
    ici: Destination?,
    surTout: () -> Unit,
    surChoix: (Destination) -> Unit
) {
    val paysage = LocalPaysage.current
    val forme = RoundedCornerShape(26.dp)
    val fond = Modifier
        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f), forme)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forme)
        .padding(5.dp)
    /* Hors de la barre, le bouton « Tout » dit où l'on est : on ne se perd jamais. */
    val horsBarre = ici != null && epingles.none { it.id == ici.id }
    val contenu: @Composable () -> Unit = {
        ElementBarre(
            IconeTout,
            if (horsBarre) ici?.court ?: "Tout" else "Tout",
            fond = if (horsBarre) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
            encre = if (horsBarre) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
            surClic = surTout
        )
        epingles.forEach { d ->
            val actif = d.id == ici?.id
            /* Le cinéma garde le rouge du voyant de tournage, quel que soit l'écran ouvert. */
            val tally = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Teinte.TALLY.sombre else Teinte.TALLY.clair
            val cine = d.onglet == Onglet.CINEMA
            ElementBarre(
                d.onglet.icone,
                d.court,
                fond = when {
                    actif && cine -> tally.couleur
                    actif -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                },
                encre = when {
                    actif && cine -> tally.surCouleur
                    actif -> MaterialTheme.colorScheme.onPrimary
                    cine -> tally.couleur
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                surClic = { surChoix(d) }
            )
        }
    }
    if (paysage) {
        /* En paysage, la barre se dresse sur le bord droit : la hauteur manque, pas la largeur. */
        Column(
            Modifier.align(Alignment.CenterEnd).navigationBarsPadding().padding(end = 8.dp).then(fond),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { contenu() }
    } else {
        Row(
            Modifier.align(Alignment.BottomCenter).wrapContentWidth().navigationBarsPadding().padding(bottom = 12.dp).then(fond),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { contenu() }
    }
}

/** Un élément de la barre : son icône et, toujours, son nom dessous. */
@Composable
private fun ElementBarre(icone: ImageVector, texte: String, fond: Color, encre: Color, surClic: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(21.dp))
            .background(fond)
            .clickable { surClic() }
            .widthIn(min = 56.dp)
            .padding(horizontal = 6.dp, vertical = if (LocalPaysage.current) 4.dp else 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icone, contentDescription = null, tint = encre, modifier = Modifier.size(22.dp))
        Text(texte, style = Detail.copy(fontSize = 10.sp, lineHeight = 12.sp), color = encre, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/* ── Le lanceur ────────────────────────────────────────────────────────── */

/**
 * Un écran demande à un autre d'ouvrir une de ses pages : « TABLES:FORMULES ».
 * L'écran visé la prend en s'affichant, et elle disparaît.
 */
object Aiguillage {
    var cible by mutableStateOf("")

    fun prendre(ecran: String): String? {
        val c = cible
        if (!c.startsWith("$ecran:")) return null
        cible = ""
        return c.substringAfter(':')
    }
}

private enum class Univers(val titre: String) {
    TERRAIN("Photo · sur le terrain"),
    APRES("Photo · après la prise"),
    CINEMA("Cinéma · tournage"),
    LUMIERE("Cinéma · lumière"),
    SAVOIR("Apprendre et retrouver")
}

/** Une page de l'appli où l'on peut aller d'un toucher, où qu'elle soit rangée. */
private data class Destination(
    val id: String,
    val onglet: Onglet,
    val univers: Univers,
    val titre: String,
    /** Son nom dans la barre du bas : neuf lettres au plus. */
    val court: String,
    val detail: String,
    /** Ce qu'on taperait pour la chercher. */
    val mots: String = "",
    /** La page à ouvrir dans l'écran, par l'[Aiguillage]. */
    val sous: String = "",
    val cineVue: String? = null,
    val outil: String? = null
)

private fun lumiere(id: String, outil: String, titre: String, court: String, detail: String, mots: String) =
    Destination(id, Onglet.CINEMA, Univers.LUMIERE, titre, court, detail, mots, cineVue = "LUMIERE", outil = outil)

private val DESTINATIONS: List<Destination> by lazy {
    listOf(
        Destination("mesurer", Onglet.MESURER, Univers.TERRAIN, "Mesurer", "Mesurer", "Le posemètre : réfléchi, spot, incident.", "cellule lumiere exposition ev spot incident posemetre"),
        Destination("estimer", Onglet.ESTIMER, Univers.TERRAIN, "Estimer", "Estimer", "Trouver l'exposition sans cellule, facteur par facteur.", "sunny 16 soleil f/16 exposition sans cellule"),
        Destination("focales", Onglet.FOCALES, Univers.TERRAIN, "Focales", "Focales", "Voir les angles de champ, s'entraîner à cadrer.", "angle de champ cadre objectif exercice reconnaitre"),

        Destination("carnet-vues", Onglet.CARNET, Univers.APRES, "Carnet · vues", "Carnet", "Ce que tu as photographié, et ton biais.", "journal notes photos pellicule", sous = "CARNET:VUES"),
        Destination("carnet-films", Onglet.CARNET, Univers.APRES, "Carnet · films", "Films", "Tes développements notés.", "developpement pellicule revelateur", sous = "CARNET:DEVELOPPEMENTS"),
        Destination("carnet-tirages", Onglet.CARNET, Univers.APRES, "Carnet · tirages", "Tirages", "Tes tirages notés.", "papier agrandisseur tirage", sous = "CARNET:TIRAGES"),
        Destination("labo", Onglet.LABO, Univers.APRES, "Labo", "Labo", "Développer, tirer, le chrono, et comprendre pourquoi.", "chrono minuteur revelateur fixateur dilution temperature tirage bande d'essai"),

        Destination("cine-viseur", Onglet.CINEMA, Univers.CINEMA, "Viseur", "Viseur", "Le cadre à ta focale, l'exposition, capturer un plan.", "camera cadre focale capturer reperage tournage", cineVue = "VISEUR"),
        Destination("cine-plans", Onglet.CINEMA, Univers.CINEMA, "Plans", "Plans", "Les jours de tournage, les plans, le PDF.", "jour scene plan pdf decoupage carnet de tournage", cineVue = "PLANS"),
        Destination("cine-materiel", Onglet.CINEMA, Univers.CINEMA, "Matériel", "Matériel", "Pellicule ou caméra, objectifs, et ton propre matériel.", "camera capteur pellicule objectif serie mon materiel projecteurs", cineVue = "MATERIEL"),

        lumiere("lum-plan", "PLAN_SOL", "Plan au sol", "Plan sol", "Le décor vu de dessus : caméras, comédiens, projecteurs, soleil.", "plan de feu implantation carte vue camera 3d deplacer projecteur"),
        lumiere("lum-combien", "PROJECTEUR", "Combien j'ai", "Lux", "Les lux et le diaph qu'un projecteur donne à une distance.", "lux eclairement distance diaph projecteur footcandle"),
        lumiere("lum-placer", "PLACER", "Où le placer", "Placer", "La distance pour un diaph, ou pour couvrir une largeur.", "distance reculer avancer couvrir faisceau plan au sol"),
        lumiere("lum-rapports", "RAPPORTS", "Rapports", "Rapports", "Face, débouchage, contre : contraste et ambiance.", "contraste ratio key fill ambiance high key low key"),
        lumiere("lum-couleur", "COULEUR", "Couleur", "Couleur", "Kelvins, mireds et gélatines de correction.", "mired kelvin cto ctb gelatine temperature de couleur lee rosco"),
        lumiere("lum-champ", "CHAMP", "Champ", "Champ", "Ce que couvre une focale, hyperfocale, profondeur de champ.", "profondeur de champ hyperfocale angle largeur nettete"),
        lumiere("lum-elec", "ELECTRICITE", "Électricité", "Élec", "Ampères, circuits, groupe électrogène.", "ampere watt disjoncteur prise circuit groupe kva triphase"),
        lumiere("lum-scint", "SCINTILLEMENT", "Scintillement", "Scintill.", "Les cadences et les angles sûrs sous le secteur.", "flicker obturateur 50 hz 60 hz hmi cadence ralenti"),
        lumiere("lum-conv", "CONVERSIONS", "Conversions", "Convers.", "Lux et footcandles, mètres et pieds, densités, diaphs.", "footcandle pieds nd densite transmission t-stop"),

        Destination("fiches-cameras", Onglet.CAMERAS, Univers.SAVOIR, "Fiches caméras", "Caméras", "L'histoire et l'usage de chaque caméra.", "arri alexa red sony panavision camera catalogue", sous = "CAMERAS:CAMERA"),
        Destination("fiches-objectifs", Onglet.CAMERAS, Univers.SAVOIR, "Fiches objectifs", "Objectifs", "Les grandes séries d'optiques de cinéma.", "cooke zeiss angenieux anamorphique optique serie", sous = "CAMERAS:OBJECTIF"),
        Destination("fiches-projecteurs", Onglet.CAMERAS, Univers.SAVOIR, "Fiches projecteurs", "Projos", "63 projecteurs, avec leur photométrie.", "aputure skypanel hmi fresnel led tube lumiere projo", sous = "CAMERAS:PROJECTEUR"),
        Destination("quiz", Onglet.CAMERAS, Univers.SAVOIR, "Quiz", "Quiz", "Reconnaître caméras et objectifs.", "jeu question reviser entrainement", sous = "CAMERAS:QUIZ"),
        Destination("tables-exposition", Onglet.TABLES, Univers.SAVOIR, "Tables · exposition", "Tables", "Les repères d'exposition à retenir.", "ev iso vitesse diaph reciprocite", sous = "TABLES:EXPOSITION"),
        Destination("tables-developpement", Onglet.TABLES, Univers.SAVOIR, "Tables · développement", "Dév.", "Temps, températures, dilutions.", "revelateur temps temperature dilution pousse", sous = "TABLES:DEVELOPPEMENT"),
        Destination("tables-formules", Onglet.TABLES, Univers.SAVOIR, "Tables · formules", "Formules", "Toutes les formules que l'appli utilise.", "formule calcul loi carre inverse mired gelatines", sous = "TABLES:FORMULES")
    )
}

private fun destination(id: String): Destination? = DESTINATIONS.firstOrNull { it.id == id }

/** Quatre cases : tout ce que l'appli contient. */
private val IconeTout = icone("tout") {
    moveTo(4.5f, 4.5f); lineTo(10f, 4.5f); lineTo(10f, 10f); lineTo(4.5f, 10f); close()
    moveTo(14f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10f); lineTo(14f, 10f); close()
    moveTo(4.5f, 14f); lineTo(10f, 14f); lineTo(10f, 19.5f); lineTo(4.5f, 19.5f); close()
    moveTo(14f, 14f); lineTo(19.5f, 14f); lineTo(19.5f, 19.5f); lineTo(14f, 19.5f); close()
}

/**
 * Tout ce que l'appli sait faire, sur un seul écran : on cherche, ou on
 * touche. Chaque page s'ouvre directement, même rangée trois menus plus bas,
 * et une étoile la garde dans la barre.
 */
@Composable
private fun Lanceur(
    epingles: List<String>,
    recents: List<Destination>,
    ici: String?,
    surChoix: (Destination) -> Unit,
    surEpingle: (Destination) -> Unit,
    surParametres: () -> Unit,
    fermer: () -> Unit
) {
    BackHandler(onBack = fermer)
    var recherche by remember { mutableStateOf("") }
    val mots = sansAccents(recherche).split(' ').filter { it.isNotBlank() }
    val trouvees = DESTINATIONS.filter { d ->
        val botte = sansAccents(d.titre + " " + d.detail + " " + d.mots + " " + d.univers.titre)
        mots.all { it in botte }
    }
    val colonnes = if (LocalPaysage.current) 4 else 2
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = Gouttiere)) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Où veux-tu aller ?",
                    style = TitreEcran.copy(fontSize = 26.sp, lineHeight = 30.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.clip(RoundedCornerShape(50)).clickable { surParametres() }.padding(10.dp)) {
                    Icon(IconeReglages, contentDescription = "Paramètres", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                }
                Text(
                    "✕",
                    style = TitreCarte.copy(fontSize = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { fermer() }.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Champ(recherche, "Chercher", Modifier.fillMaxWidth(), indication = "mired, ampères, plan au sol, quiz, tirage…") { recherche = it }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (mots.isEmpty() && recents.isNotEmpty()) {
                    item(key = "recents") {
                        Etiquette("Récents")
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            recents.forEach { d -> Pastille(d.titre, accent = d.id == ici) { surChoix(d) } }
                        }
                    }
                }
                Univers.entries.forEach { u ->
                    val pages = trouvees.filter { it.univers == u }
                    if (pages.isNotEmpty()) {
                        item(key = u.name) { Etiquette(u.titre, modifier = Modifier.padding(top = 8.dp), accent = true) }
                        items(pages.chunked(colonnes), key = { it.first().id }) { rangee ->
                            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rangee.forEach { d ->
                                    Tuile(
                                        d, actif = d.id == ici, epinglee = d.id in epingles,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        surClic = { surChoix(d) }, surEpingle = { surEpingle(d) }
                                    )
                                }
                                repeat(colonnes - rangee.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                if (trouvees.isEmpty()) {
                    item(key = "rien") {
                        Text("Rien ne correspond à « ${recherche.trim()} ».", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(key = "aide") {
                    Text(
                        "L'étoile garde une page dans la barre du bas (quatre au plus : la plus ancienne laisse sa place). Le bouton « Tout » te ramène ici.",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Tuile(d: Destination, actif: Boolean, epinglee: Boolean, modifier: Modifier, surClic: () -> Unit, surEpingle: () -> Unit) {
    val fond = if (actif) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val encre = if (actif) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Column(
        modifier
            .clip(RoundedCornerShape(RayonCarte))
            .background(fond)
            .clickable { surClic() }
            .padding(start = 12.dp, end = 2.dp, top = 4.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(d.onglet.icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                d.titre,
                style = TitreCarte.copy(fontSize = 15.sp),
                color = encre,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            Text(
                if (epinglee) "★" else "☆",
                style = TitreCarte.copy(fontSize = 18.sp),
                color = if (epinglee) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { surEpingle() }.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Text(
            d.detail,
            style = Detail,
            color = if (actif) encre.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}
