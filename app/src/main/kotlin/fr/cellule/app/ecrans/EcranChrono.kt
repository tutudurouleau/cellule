package fr.cellule.app.ecrans

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import fr.cellule.app.ChiffreHero
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonCarte
import fr.cellule.app.RayonControle
import fr.cellule.app.TitreEcran
import fr.cellule.app.chrono.Minuteur
import fr.cellule.app.chrono.ServiceChrono
import fr.cellule.core.Agitation
import fr.cellule.core.Agitations
import fr.cellule.core.Chrono
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.Duree
import fr.cellule.core.Position
import fr.cellule.core.SequenceLabo
import fr.cellule.core.Sequences
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Ce qu'on prépare avant de lancer le chrono : les bains d'un enchaînement,
 * leurs durées (en texte, pour pouvoir les taper librement) et leur agitation.
 */
class EtatChrono {
    var modele by mutableStateOf(Sequences.FILM_ILFORD)
        private set
    val saisies = mutableStateListOf<String>()
    val agitations = mutableStateListOf<Agitation?>()

    init { charger(Sequences.FILM_ILFORD) }

    fun charger(s: SequenceLabo) {
        modele = s
        saisies.clear()
        saisies.addAll(s.etapes.map { if (it.duree > 0) Duree.chrono(it.duree) else "" })
        agitations.clear()
        agitations.addAll(s.etapes.map { it.agitation })
    }

    /** Le révélateur est toujours le premier bain : un calculateur peut y verser son résultat. */
    fun reglerRevelateur(secondes: Double) {
        if (saisies.isNotEmpty()) saisies[0] = Duree.chrono(secondes)
    }

    fun sequence(): SequenceLabo = modele.copy(
        etapes = modele.etapes.mapIndexed { i, e ->
            e.copy(duree = Duree.lire(saisies[i]) ?: 0.0, agitation = agitations[i])
        }
    )
}

private val AGITATIONS = listOf<Pair<String, Agitation?>>(
    "Aucune" to null,
    "Ilford : 10 s par minute" to Agitations.ILFORD,
    "Kodak : 5 s toutes les 30 s" to Agitations.KODAK,
    "En continu" to Agitations.CONTINUE
)

private fun nomAgitation(a: Agitation?): String =
    AGITATIONS.firstOrNull { it.second == a }?.first ?: "Personnalisée"

@Composable
fun EcranChrono(etat: EtatChrono, versCarnet: (DeveloppementNote) -> Unit) {
    if (Minuteur.enCours) ChronoEnCours(versCarnet) else Preparation(etat)
}

/* ── Préparer ─────────────────────────────────────────────────────────────── */

@Composable
private fun Preparation(etat: EtatChrono) {
    val contexte = LocalContext.current
    val lancer = { Minuteur.lancer(contexte, etat.sequence()) }
    /* Sans la permission, le chrono bipe quand même ; seule la notification manque. */
    val demande = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { lancer() }

    Carte(
        titre = "Chrono labo",
        sousTitre = "Un enchaînement de bains, avec bips et vibrations à chaque changement et à chaque agitation — écran éteint, appli en arrière-plan."
    ) {
        ChoixSegmente(
            options = Sequences.TOUTES,
            selection = etat.modele,
            libelle = {
                when (it) {
                    Sequences.FILM_ILFORD -> "Film Ilford"
                    Sequences.FILM_KODAK -> "Film Kodak"
                    else -> "Papier"
                }
            }
        ) { etat.charger(it) }
        if (etat.modele.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(etat.modele.note, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    etat.modele.etapes.forEachIndexed { i, e ->
        Carte(titre = "${i + 1}. ${e.nom}", sousTitre = e.consigne) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Champ(etat.saisies[i], "Durée", Modifier.weight(1f), indication = "m:ss") { etat.saisies[i] = it }
                Spacer(Modifier.width(Interligne))
                Deroulant(
                    intitule = "Agitation",
                    options = AGITATIONS.map { it.second },
                    selection = etat.agitations[i],
                    libelle = ::nomAgitation,
                    modifier = Modifier.weight(1.3f)
                ) { etat.agitations[i] = it }
            }
            val a = etat.agitations[i]
            if (a != null && !a.enContinu) {
                Spacer(Modifier.height(Interligne))
                Row {
                    ChampSecondes("Au départ", a.initiale, Modifier.weight(1f)) { etat.agitations[i] = a.copy(initiale = it) }
                    Spacer(Modifier.width(6.dp))
                    ChampSecondes("Reprise", a.duree, Modifier.weight(1f)) { etat.agitations[i] = a.copy(duree = it) }
                    Spacer(Modifier.width(6.dp))
                    ChampSecondes("Toutes les", a.intervalle, Modifier.weight(1f)) { etat.agitations[i] = a.copy(intervalle = it) }
                }
            }
        }
    }

    val s = etat.sequence()
    Carte {
        if (!s.complete) {
            BandeauEtat("Chaque bain doit avoir une durée — reprends-les des fiches de tes produits.", alerte = true)
            Spacer(Modifier.height(Interligne))
        } else {
            Ligne("Durée totale", Duree.libelle(s.duree))
        }
        BoutonPlat("Lancer", actif = s.complete, accent = true, modifier = Modifier.fillMaxWidth()) {
            val manque = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (manque) demande.launch(Manifest.permission.POST_NOTIFICATIONS) else lancer()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Film : Ilford demande de manipuler la HP5 Plus dans le noir complet — écran éteint, les bips suffisent. " +
                "Au tirage, l'affichage rouge limite la lumière ; garde tout de même l'écran loin du papier.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        etat.modele.source?.let { LigneSource(it) }
    }
}

/** Un nombre de secondes, tapé librement ; la valeur n'est prise que si elle se lit. */
@Composable
private fun ChampSecondes(intitule: String, valeur: Double, modifier: Modifier, surValeur: (Double) -> Unit) {
    var texte by remember(valeur) { mutableStateOf(fmt(valeur, 0)) }
    Champ(texte, "$intitule (s)", modifier) {
        texte = it
        it.replace(',', '.').toDoubleOrNull()?.takeIf { v -> v >= 0 }?.let(surValeur)
    }
}

/* ── En cours ─────────────────────────────────────────────────────────────── */

@Composable
private fun ChronoEnCours(versCarnet: (DeveloppementNote) -> Unit) {
    val s = Minuteur.sequence ?: return
    val contexte = LocalContext.current
    var maintenant by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var rouge by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            maintenant = SystemClock.elapsedRealtime()
            delay(200)
        }
    }
    val p = Chrono.position(s, Minuteur.ecoule(maintenant))
    val etape = s.etapes[p.etape]
    val etatTexte = when {
        p.fini -> "Séquence terminée."
        Minuteur.pause != null -> "En pause."
        p.agite -> "Agite !"
        p.prochaineAgitation != null -> "Repos · agitation dans ${ceil(p.prochaineAgitation!!).toInt()} s"
        else -> "Repos"
    }
    val pause = {
        Minuteur.basculerPause()
        ServiceChrono.rafraichir(contexte)
    }
    val suivant = {
        Minuteur.etapeSuivante()
        ServiceChrono.rafraichir(contexte)
    }

    if (rouge) {
        AffichageRouge(etape.nom, if (p.fini) "fini" else Duree.chrono(p.resteEtape), etatTexte, p, pause, suivant) { rouge = false }
        return
    }

    Carte {
        Etiquette("Bain ${p.etape + 1} sur ${s.etapes.size} · ${s.nom}")
        Text(etape.nom, style = TitreEcran, color = MaterialTheme.colorScheme.onSurface)
        Text(
            if (p.fini) "Terminé" else Duree.chrono(p.resteEtape),
            style = ChiffreHero,
            color = if (p.agite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        BandeauEtat(etatTexte, alerte = p.agite)
        if (etape.consigne.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(etape.consigne, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Interligne))
        if (!p.fini) {
            LigneBoutons {
                BoutonPlat(if (Minuteur.pause == null) "Pause" else "Reprendre", modifier = Modifier.weight(1f)) { pause() }
                BoutonPlat("Bain suivant", modifier = Modifier.weight(1f)) { suivant() }
            }
            Spacer(Modifier.height(Interligne))
        }
        if (p.fini && s.nom != Sequences.PAPIER.nom) {
            BoutonPlat("Verser ce développement au carnet", modifier = Modifier.fillMaxWidth()) {
                val revelateur = s.etapes.first()
                versCarnet(
                    DeveloppementNote(
                        identifiant = 0, date = "",
                        tempsDonne = revelateur.duree,
                        agitation = revelateur.agitation?.libelle.orEmpty()
                    )
                )
            }
            Spacer(Modifier.height(Interligne))
        }
        LigneBoutons {
            BoutonPlat("Affichage rouge", modifier = Modifier.weight(1f)) { rouge = true }
            BoutonPlat(if (p.fini) "Nouvelle séquence" else "Arrêter", accent = p.fini, modifier = Modifier.weight(1f)) {
                Minuteur.arreter(contexte)
            }
        }
    }

    Carte(titre = "La séquence") {
        s.etapes.forEachIndexed { i, e ->
            Ligne(
                (if (i < p.etape || p.fini) "✓ " else "") + e.nom,
                Duree.libelle(e.duree),
                e.agitation?.let { "agiter ${it.libelle}" },
                accent = i == p.etape && !p.fini
            )
        }
    }
}

/**
 * Pour la chambre noire : rouge sombre sur noir, grands chiffres, rien d'autre.
 * Un appui n'importe où hors des boutons revient à l'affichage normal.
 */
@Composable
private fun AffichageRouge(
    bain: String,
    reste: String,
    etat: String,
    p: Position,
    pause: () -> Unit,
    suivant: () -> Unit,
    quitter: () -> Unit
) {
    val rouge = Color(0xFF9E1B1B)
    val rougeVif = Color(0xFFD02A2A)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 6.dp)
            .background(Color.Black, RoundedCornerShape(RayonCarte))
            .clickable { quitter() }
            .padding(Gouttiere + 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(bain, style = TitreEcran, color = rouge)
        Text(reste, style = ChiffreHero, color = if (p.agite) rougeVif else rouge)
        Text(etat, style = Corps, color = rouge)
        Spacer(Modifier.height(Gouttiere))
        if (!p.fini) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Interligne)) {
                BoutonRouge(if (Minuteur.pause == null) "Pause" else "Reprendre", rouge, Modifier.weight(1f), pause)
                BoutonRouge("Bain suivant", rouge, Modifier.weight(1f), suivant)
            }
        }
        Spacer(Modifier.height(Interligne))
        Text("Toucher ailleurs pour revenir", style = Detail, color = rouge.copy(alpha = 0.7f))
    }
}

@Composable
private fun BoutonRouge(texte: String, couleur: Color, modifier: Modifier, surClic: () -> Unit) {
    Box(
        modifier
            .border(1.dp, couleur, RoundedCornerShape(RayonControle))
            .clickable { surClic() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(texte, style = Corps, color = couleur)
    }
}
