package fr.cellule.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import fr.cellule.app.ecrans.EcranEstimer
import fr.cellule.app.ecrans.EcranJournal
import fr.cellule.app.ecrans.EcranMesurer
import fr.cellule.app.ecrans.EcranTables
import fr.cellule.core.TYPES_DE_SCENE

/** Ce que les écrans se passent entre eux : une estimation, une mesure. */
class EtatApplication {
    /** Estimation à verser dans la case « annoncé » du journal. */
    var annonceProposee by mutableStateOf<Double?>(null)

    /** Mesure du posemètre à verser dans la case « mesuré ». */
    var derniereMesure by mutableStateOf<Double?>(null)

    var derniereEstimation by mutableStateOf<Double?>(null)
    var dernierTypeScene by mutableStateOf(TYPES_DE_SCENE.first())
    var derniereChaine by mutableStateOf("")
}

private enum class Onglet(val titre: String, val icone: ImageVector) {
    MESURER("Mesurer", Icons.Filled.Search),
    ESTIMER("Estimer", Icons.Filled.Star),
    JOURNAL("Journal", Icons.Filled.Edit),
    TABLES("Tables", Icons.Filled.List)
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
        bottomBar = {
            NavigationBar {
                Onglet.entries.forEach { o ->
                    NavigationBarItem(
                        selected = onglet == o,
                        onClick = { onglet = o },
                        icon = { Icon(o.icone, contentDescription = o.titre) },
                        label = { Text(o.titre) }
                    )
                }
            }
        }
    ) { marges ->
        Column(Modifier.fillMaxSize().padding(marges)) {
            when (onglet) {
                Onglet.MESURER -> EcranMesurer(reglages, etat)
                Onglet.ESTIMER -> EcranEstimer(reglages, etat)
                Onglet.JOURNAL -> EcranJournal(depot, etat)
                Onglet.TABLES -> EcranTables()
            }
        }
    }
}
