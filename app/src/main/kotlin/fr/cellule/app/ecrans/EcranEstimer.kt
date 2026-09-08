package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.EtatApplication
import fr.cellule.app.Reglages
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CIELS
import fr.cellule.core.Estimateur
import fr.cellule.core.LIEUX
import fr.cellule.core.Modele
import fr.cellule.core.OBSTACLES
import fr.cellule.core.Photometrie
import fr.cellule.core.RENVOIS
import fr.cellule.core.SENSIBILITES
import fr.cellule.core.Scene
import fr.cellule.core.Soleil
import fr.cellule.core.Statistiques
import fr.cellule.core.coupleConseille
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.libelleOuverture
import java.time.LocalDate
import java.time.LocalTime

/** Une ligne d'option sélectionnable, avec sa valeur en diaphs à droite. */
@Composable
private fun Option(nom: String, detail: String, valeur: String, actif: Boolean, surClic: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (actif) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(9.dp)
            )
            .clickable { surClic() }
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                nom,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (actif) FontWeight.Medium else FontWeight.Normal
            )
            if (detail.isNotBlank()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            valeur,
            style = MaterialTheme.typography.bodySmall,
            color = if (actif) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EcranEstimer(reglages: Reglages, etat: EtatApplication) {

    var ciel by remember { mutableStateOf(CIELS[0]) }
    var obstacle by remember { mutableStateOf(OBSTACLES[0]) }
    var renvois by remember { mutableStateOf(setOf<String>()) }
    var modele by remember { mutableStateOf(Modele.PHYSIQUE) }
    var parCalcul by remember { mutableStateOf(true) }

    var lieu by remember {
        mutableStateOf(LIEUX.minByOrNull { kotlin.math.abs(it.latitude - reglages.latitude) } ?: LIEUX[0])
    }
    var mois by remember { mutableStateOf(LocalDate.now().monthValue) }
    val maintenant = remember { LocalTime.now() }
    var heure by remember { mutableStateOf(maintenant.hour + maintenant.minute / 60f) }
    var hauteurAVue by remember { mutableStateOf(45.0) }

    val jour = LocalDate.of(LocalDate.now().year, mois, 15).dayOfYear
    val hauteur = if (parCalcul) {
        Soleil.hauteur(
            lieu.latitude, jour,
            Soleil.tempsSolaire(heure.toDouble(), jour, lieu.longitude, reglages.fuseau)
        )
    } else hauteurAVue

    val scene = Scene(ciel, hauteur, obstacle, renvois, reglages.iso, modele)
    val estimation = Estimateur.calculer(scene)
    etat.derniereEstimation = estimation.ev100Scene
    etat.dernierTypeScene = Statistiques.typeSuggere(estimation.ev100Scene, obstacle)
    etat.derniereChaine = estimation.termes
        .filter { it.diaphs != null }
        .joinToString(", ") { signe(it.diaphs!!) + " " + it.nom.lowercase() }
        .let { "ciel " + fmt(estimation.ev100Ciel) + (if (it.isBlank()) "" else ", $it") +
            " → " + fmt(estimation.ev100Scene) }

    val vitesseVoulue =
        if (reglages.vitesseReference > 0) reglages.vitesseReference else 1.0 / reglages.iso
    val couple = coupleConseille(estimation.evAppareil, vitesseVoulue)

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
    ) {

        Carte {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    fmt(estimation.ev100Scene),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "  EV₁₀₀",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (couple != null) {
                Text(
                    "${libelleOuverture(couple.ouverture)} · ${couple.vitesse.libelle}   à ${reglages.iso} ISO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Exact ${libelleOuverture(couple.ouvertureExacte)} — l'arrondi coûte ${libelleDiaphs(couple.ecartDiaphs)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            estimation.termes.forEach { terme ->
                Ligne(
                    intitule = terme.nom,
                    valeur = terme.ev?.let { "EV " + fmt(it) } ?: signe(terme.diaphs ?: 0.0),
                    detail = terme.detail,
                    accent = terme.ev != null
                )
            }
            Text(
                if (modele == Modele.PHYSIQUE)
                    "Les termes s'additionnent en diaphs, jamais en pourcentages — c'est tout l'intérêt du log₂."
                else
                    "Grille mentale : facteurs bruts, exactement ce que tu calcules de tête.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Carte(titre = "1 · La lumière du ciel", sousTitre = "Le repère infaillible, c'est l'ombre portée.") {
            CIELS.forEach { c ->
                val ev = if (modele == Modele.PHYSIQUE)
                    fr.cellule.core.Ciel.eclairement(hauteur, c).ev100
                else 15 + c.plat + fr.cellule.core.Ciel.perteHauteurPlate(hauteur)
                Option(c.nom, c.detail, "EV " + fmt(ev), c.id == ciel.id) { ciel = c }
            }
        }

        Carte(
            titre = "2 · La hauteur du soleil",
            sousTitre = "L'éclairement suit le sinus de la hauteur, puis chute plus vite encore sous 15°, l'atmosphère traversée s'épaississant."
        ) {
            ChoixSegmente(
                options = listOf(true, false),
                selection = parCalcul,
                libelle = { if (it) "Calculer" else "À vue" },
                surChoix = { parCalcul = it }
            )
            Spacer(Modifier.height(6.dp))
            if (parCalcul) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Deroulant("", LIEUX, lieu, { it.nom }) {
                        lieu = it
                        reglages.latitude = it.latitude
                        reglages.longitude = it.longitude
                    }
                    Deroulant("", (1..12).toList(), mois, { moisNom(it) }) { mois = it }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Heure légale : " + String.format("%02dh%02d", heure.toInt(), ((heure % 1) * 60).toInt()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(value = heure, onValueChange = { heure = it }, valueRange = 0f..23.98f)
                val midi = Soleil.midiSolaire(jour, lieu.longitude, reglages.fuseau)
                Text(
                    "Midi solaire à " + String.format("%dh%02d", midi.toInt(), Math.round((midi % 1) * 60)) +
                        " · le soleil culmine à " + fmt(Soleil.hauteur(lieu.latitude, jour, 12.0)) +
                        "° · à l'heure indiquée il est à " + fmt(hauteur) + "°.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                listOf(
                    75.0 to "Ombre très courte, été, midi",
                    60.0 to "Ombre = ⅗ de ta taille",
                    45.0 to "Ombre = ta taille exactement",
                    30.0 to "Ombre = 2 × ta taille",
                    20.0 to "Ombre = 3 × ta taille",
                    10.0 to "Ombre = 6 × ta taille, lumière rasante",
                    4.0 to "Soleil sur l'horizon",
                    -3.0 to "Crépuscule civil"
                ).forEach { (h, detail) ->
                    val ev = fr.cellule.core.Ciel.eclairement(h, ciel).ev100
                    Option("${h.toInt()}°", detail, signe(ev - 15), hauteurAVue == h) { hauteurAVue = h }
                }
            }
        }

        Carte(titre = "3 · Ce qui bouche la lumière", sousTitre = "L'obstacle dominant entre le ciel et ton sujet.") {
            OBSTACLES.forEach { o ->
                val v = if (modele == Modele.PHYSIQUE) o.diaphs(estimation.partSoleil) else o.auSoleil
                Option(
                    o.nom, o.detail,
                    if (kotlin.math.abs(v) < 0.05) "—" else signe(v),
                    o.id == obstacle.id
                ) { obstacle = o }
            }
        }

        Carte(
            titre = "4 · Ce qui la renvoie",
            sousTitre = "Cumulable. Des gains d'éclairement réel, pas une correction de mesure."
        ) {
            val facteur = if (modele == Modele.PHYSIQUE)
                fr.cellule.core.facteurRenvoi(estimation.partSoleil) else 1.0
            RENVOIS.forEach { r ->
                Option(r.nom, "", signe(r.diaphs * facteur), r.id in renvois) {
                    renvois = if (r.id in renvois) renvois - r.id else renvois + r.id
                }
            }
        }

        Carte(titre = "5 · Le film dans le dos") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Deroulant("", SENSIBILITES, reglages.iso, { "$it ISO" }) { reglages.iso = it }
                Deroulant(
                    "", listOf(Modele.PHYSIQUE, Modele.GRILLE_MENTALE), modele,
                    { if (it == Modele.PHYSIQUE) "Physique" else "Grille mentale" }
                ) { modele = it }
            }
            Text(
                if (modele == Modele.PHYSIQUE)
                    "Le modèle physique tient compte de la masse d'air et du ciel diffus : par temps couvert la hauteur du soleil compte beaucoup moins, et une ombre ne coupe que du soleil qui existe."
                else
                    "La grille mentale reprend les facteurs plats du document de référence. L'écart avec le modèle physique te dit où l'approximation lâche.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
            BoutonPlat("Verser cette estimation au carnet", accent = true) {
                etat.annonceProposee = estimation.ev100Scene
            }
        }
    }
}

fun moisNom(m: Int): String = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre"
)[(m - 1).coerceIn(0, 11)]
