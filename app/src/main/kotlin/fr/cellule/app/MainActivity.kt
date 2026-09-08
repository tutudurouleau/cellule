package fr.cellule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.cellule.app.ecrans.EcranEstimer
import fr.cellule.app.ecrans.EcranFocales
import fr.cellule.app.ecrans.EcranJournal
import fr.cellule.app.ecrans.EcranMesurer
import fr.cellule.app.ecrans.EcranTables
import fr.cellule.core.TYPES_DE_SCENE

/** Ce que les écrans se passent entre eux. */
class EtatApplication {
    /** Estimation à verser dans la case « annoncé » du carnet. */
    var annonceProposee by mutableStateOf<Double?>(null)

    /** Mesure du posemètre à verser dans la case « mesuré ». */
    var derniereMesure by mutableStateOf<Double?>(null)

    /** Le couple retenu au moment de la mesure, pour le noter avec la vue. */
    var dernierReglage by mutableStateOf("")

    var derniereEstimation by mutableStateOf<Double?>(null)
    var dernierTypeScene by mutableStateOf(TYPES_DE_SCENE.first())
    var derniereChaine by mutableStateOf("")
}

/*
 * Une iconographie dessinée plutôt que le jeu Material par défaut : c'est ce
 * dernier, plus que n'importe quelle couleur, qui donne à une application son
 * air d'avoir dix ans. Cinq glyphes au trait, même épaisseur, même esprit.
 */
private fun icone(nom: String, trace: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit): ImageVector =
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

/** Un carnet : reliure et lignes. */
private val IconeCarnet = icone("carnet") {
    moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(5f, 20f); close()
    moveTo(8f, 4f); lineTo(8f, 20f)
    moveTo(11.3f, 9f); lineTo(16f, 9f)
    moveTo(11.3f, 13f); lineTo(16f, 13f)
    moveTo(11.3f, 17f); lineTo(14f, 17f)
}

/** Une grille : les tables de référence. */
private val IconeTables = icone("tables") {
    moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
    moveTo(4f, 10.3f); lineTo(20f, 10.3f)
    moveTo(10f, 5f); lineTo(10f, 19f)
    moveTo(15f, 5f); lineTo(15f, 19f)
}

private enum class Onglet(val titre: String, val sousTitre: String, val icone: ImageVector, val plein: Boolean) {
    MESURER("Mesurer", "posemètre réfléchi et incident", IconeMesurer, true),
    ESTIMER("Estimer", "la chaîne de facteurs, sans cellule", IconeEstimer, false),
    FOCALES("Focales", "apprendre à voir les angles de champ", IconeFocales, true),
    CARNET("Carnet", "ce que tu as photographié, et ton biais", IconeCarnet, false),
    TABLES("Tables", "les repères à retenir", IconeTables, false)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /* Le contenu dessine sous les barres système : les écrans caméra en
           profitent pour un viseur réellement plein écran, les autres gèrent
           leur propre respiration en haut via statusBarsPadding(). */
        enableEdgeToEdge()
        setContent {
            CelluleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Application()
                }
            }
        }
    }
}

@Composable
private fun Application() {
    val contexte = LocalContext.current
    val reglages = remember { Reglages(contexte) }
    val depot = remember { DepotJournal(contexte) }
    val etat = remember { EtatApplication() }
    var onglet by remember { mutableStateOf(Onglet.MESURER) }

    Box(Modifier.fillMaxSize()) {

        Column(Modifier.fillMaxSize()) {
            /* Les écrans caméra n'ont pas d'en-tête : chaque pixel du haut de
               l'écran leur revient, comme dans un vrai viseur. */
            if (!onglet.plein) {
                EnTeteEcran(onglet.titre, onglet.sousTitre)
            }
            Box(Modifier.weight(1f)) {
                when (onglet) {
                    Onglet.MESURER -> EcranMesurer(reglages, etat)
                    Onglet.ESTIMER -> EcranEstimer(reglages, etat)
                    Onglet.FOCALES -> EcranFocales()
                    Onglet.CARNET -> EcranJournal(depot, reglages, etat)
                    Onglet.TABLES -> EcranTables()
                }
            }
        }

        NavigationFlottante(onglet = onglet, surChoix = { onglet = it })
    }
}

/** L'en-tête des écrans qui ne sont pas plein cadre. */
@Composable
private fun EnTeteEcran(titre: String, sousTitre: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = Gouttiere, end = Gouttiere, top = 14.dp, bottom = 8.dp)
    ) {
        Text(titre, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
        Text(sousTitre, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                RoundedCornerShape(50)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Onglet.entries.forEach { o -> OngletFlottant(o, o == onglet) { surChoix(o) } }
    }
}

@Composable
private fun RowScope.OngletFlottant(onglet: Onglet, actif: Boolean, surClic: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (actif) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { surClic() }
            .padding(horizontal = if (actif) 16.dp else 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            onglet.icone,
            contentDescription = onglet.titre,
            tint = if (actif) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        AnimatedVisibility(visible = actif, enter = fadeIn(), exit = fadeOut()) {
            Text(
                onglet.titre,
                style = Corps,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
