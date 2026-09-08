package fr.cellule.app.ecrans

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.app.mesure.InventaireCapteurs
import fr.cellule.app.mesure.ModuleCamera

/**
 * Ce que dit l'API du téléphone sur ses propres capteurs de prise de vue et
 * son capteur d'ambiance — le pendant matériel de l'écran Mesurer, qui lui
 * s'en sert plutôt que de les décrire.
 */
@Composable
fun EcranCapteurs() {
    val contexte = LocalContext.current

    var autorisee by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val demandeur = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { accordee -> autorisee = accordee }

    /* Refait l'inventaire si la permission vient d'être accordée : certains
       fabricants referment le détail des capteurs physiques sans elle. */
    val modules = remember(autorisee) { InventaireCapteurs.lister(contexte) }
    val arriere = modules.filter { it.face == "arrière" }
    val avant = modules.filter { it.face == "avant" }
    val autres = modules.filter { it.face != "arrière" && it.face != "avant" }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
    ) {
        Spacer(Modifier.height(Interligne))

        Carte(
            titre = "Inventaire matériel",
            sousTitre = "${arriere.sumOf { it.capteurs.size }} capteur(s) arrière, " +
                "${avant.sumOf { it.capteurs.size }} avant, lus directement dans l'API Camera2 " +
                "de cet appareil. Aucune image n'est capturée pour cette lecture."
        ) {
            Texte(
                "Android ne publie ni nom marketing ni rôle pour un capteur. Le rôle indiqué " +
                    "plus bas (« ultra grand-angle », « macro »…) est déduit de la focale " +
                    "équivalente 24×36 et de la distance de mise au point minimale : une " +
                    "estimation, pas une donnée constructeur. Et selon le fabricant, ces quatre " +
                    "capteurs arrière apparaissent soit comme quatre identifiants séparés, soit " +
                    "comme un seul module qui les regroupe : les deux cas sont couverts."
            )
        }

        if (!autorisee) {
            Carte(
                titre = "Détail incomplet possible",
                sousTitre = "Sans la permission caméra, certains fabricants ne détaillent pas les capteurs physiques d'un module multi-caméras."
            ) {
                BoutonPlat("Autoriser la caméra", accent = true) {
                    demandeur.launch(Manifest.permission.CAMERA)
                }
            }
        }

        if (modules.isEmpty()) {
            Carte { Texte("Aucune caméra détectée par l'API Camera2 sur cet appareil.") }
        }

        if (arriere.isNotEmpty()) BlocFace("Arrière", arriere)
        if (avant.isNotEmpty()) BlocFace("Avant", avant)
        if (autres.isNotEmpty()) BlocFace("Autre", autres)

        BlocAmbiance(contexte)
    }
}

@Composable
private fun BlocFace(titre: String, modules: List<ModuleCamera>) {
    modules.forEach { module ->
        module.capteurs.forEachIndexed { index, s ->
            val etiquette =
                if (module.capteurs.size > 1) "$titre — capteur ${index + 1}/${module.capteurs.size}" else titre
            Carte(titre = etiquette, sousTitre = s.role.replaceFirstChar { it.uppercase() }) {
                Ligne(
                    "Identifiant Camera2",
                    s.id,
                    if (s.id != s.idModule) "regroupé sous le module ${s.idModule}" else "module autonome"
                )
                Ligne(
                    "Focale",
                    s.focaleMm?.let { "${fmt(it.toDouble(), 1)} mm" } ?: "non publiée",
                    s.focaleEquivalente35mm?.let { "≈ ${Math.round(it)} mm équivalent 24×36" }
                )
                Ligne(
                    "Ouverture",
                    s.ouvertureMax?.let { "f/${fmt(it.toDouble(), 1)}" } ?: "non publiée",
                    if (s.stabilisationOptique) "stabilisation optique déclarée" else "pas de stabilisation optique déclarée"
                )
                Ligne(
                    "Capteur physique",
                    s.tailleCapteurMm?.let {
                        "${fmt(it.width.toDouble(), 2)} × ${fmt(it.height.toDouble(), 2)} mm"
                    } ?: "non publié",
                    s.resolutionPx?.let { p ->
                        s.megapixels?.let { mp -> "${p.width} × ${p.height} px · ${fmt(mp, 1)} Mpx" }
                    }
                )
                Ligne(
                    "Mise au point",
                    if (s.autofocus) "autofocus" else "fixe (hyperfocale)",
                    "flash associé : ${if (s.flash) "oui" else "non"} · niveau ${s.niveauMateriel}"
                )
            }
        }
    }
}

@Composable
private fun BlocAmbiance(contexte: Context) {
    val capteur = remember {
        val gestionnaire = contexte.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        gestionnaire?.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    Carte(
        titre = "Capteur d'ambiance",
        sousTitre = "Celui que le mode « Incident » de l'écran Mesurer utilise pour lire des lux, indépendamment du sujet visé."
    ) {
        val c = capteur
        if (c == null) {
            Texte("Aucun capteur de luminosité ambiante détecté sur cet appareil.")
        } else {
            Ligne("Nom déclaré", c.name, c.vendor)
            Ligne(
                "Portée",
                "jusqu'à ${fmt(c.maximumRange.toDouble(), 0)} lux",
                "résolution ${fmt(c.resolution.toDouble(), 2)} lux"
            )
            Ligne(
                "Fonctionnement",
                "${fmt(c.power.toDouble(), 2)} mA",
                "délai minimal entre lectures : ${c.minDelay / 1000} ms"
            )
        }
    }
}

@Composable
private fun Texte(texte: String) {
    Text(texte, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
