package fr.cellule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

/**
 * Deux cadres emboîtés : aucune icône du jeu standard ne dit « angle de
 * champ », et une icône approximative dans une barre de navigation se voit.
 */
private val IconeCadrage: ImageVector = ImageVector.Builder(
    name = "cadrage",
    defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    addPath(
        pathData = PathData {
            moveTo(3f, 5.5f); lineTo(21f, 5.5f); lineTo(21f, 18.5f); lineTo(3f, 18.5f); close()
            moveTo(9f, 9.5f); lineTo(15f, 9.5f); lineTo(15f, 14.5f); lineTo(9f, 14.5f); close()
        },
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )
}.build()

private enum class Onglet(val titre: String, val sousTitre: String, val icone: ImageVector) {
    MESURER("Mesurer", "posemètre réfléchi et incident", Icons.Filled.Search),
    ESTIMER("Estimer", "la chaîne de facteurs, sans cellule", Icons.Filled.Star),
    FOCALES("Focales", "apprendre à voir les angles de champ", IconeCadrage),
    CARNET("Carnet", "ce que tu as photographié, et ton biais", Icons.Filled.DateRange),
    TABLES("Tables", "les repères à retenir", Icons.Filled.List)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = Gouttiere, end = Gouttiere, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        onglet.titre,
                        style = TitreEcran,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        onglet.sousTitre,
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "CELLULE",
                    style = StyleEtiquette,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Onglet.entries.forEach { o ->
                    NavigationBarItem(
                        selected = onglet == o,
                        onClick = { onglet = o },
                        icon = { Icon(o.icone, contentDescription = o.titre) },
                        label = { Text(o.titre, style = Detail, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { marges ->
        Column(Modifier.fillMaxSize().padding(marges)) {
            when (onglet) {
                Onglet.MESURER -> EcranMesurer(reglages, etat)
                Onglet.ESTIMER -> EcranEstimer(reglages, etat)
                Onglet.FOCALES -> EcranFocales()
                Onglet.CARNET -> EcranJournal(depot, reglages, etat)
                Onglet.TABLES -> EcranTables()
            }
        }
    }
}
