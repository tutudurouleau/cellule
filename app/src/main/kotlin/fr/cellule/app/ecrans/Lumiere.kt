package fr.cellule.app.ecrans

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.cellule.app.CadreJson
import fr.cellule.app.Corps
import fr.cellule.app.DepotCinema
import fr.cellule.app.Detail
import fr.cellule.app.FiltresJson
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.MonMateriel
import fr.cellule.app.Reglages
import fr.cellule.core.AMBIANCES
import fr.cellule.core.CIRCUITS
import fr.cellule.core.Densites
import fr.cellule.core.Eclairage
import fr.cellule.core.Electricite
import fr.cellule.core.FamilleGelatine
import fr.cellule.core.Focales
import fr.cellule.core.GELATINES
import fr.cellule.core.Mireds
import fr.cellule.core.Optique
import fr.cellule.core.Photometrie
import fr.cellule.core.Projecteur
import fr.cellule.core.Region
import fr.cellule.core.SaisieMateriel
import fr.cellule.core.Scintillement
import fr.cellule.core.T_STOPS
import fr.cellule.core.Tournage
import fr.cellule.core.Unites
import fr.cellule.core.VerdictScintillement
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.nombreCourt
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * La vue Lumière de l'onglet Cinéma : les calculs du plateau. Tout reprend
 * le réglage de caméra du Matériel (EI, cadence, obturateur, filtres) ; les
 * unités (mètres ou pieds, lux ou foot-candles) se choisissent en tête.
 */

enum class OutilLumiere(val libelle: String) {
    PLAN_SOL("Plan au sol"),
    PROJECTEUR("Combien j'ai"),
    PLACER("Où le placer"),
    RAPPORTS("Rapports"),
    COULEUR("Couleur"),
    CHAMP("Champ"),
    ELECTRICITE("Électricité"),
    SCINTILLEMENT("Scintillement"),
    CONVERSIONS("Conversions")
}

private fun lire(texte: String): Double? = SaisieMateriel.lireNombre(texte)
private fun n(x: Double, d: Int = 1): String = nombreCourt(x, d)

/** Les unités choisies, et les conversions vers le système interne (mètres, lux). */
private class UnitesLumiere(val metres: Boolean, val lux: Boolean) {
    val uDistance get() = if (metres) "m" else "ft"
    val uEclairement get() = if (lux) "lux" else "fc"
    fun versMetres(v: Double) = if (metres) v else Unites.metres(v)
    fun versLux(v: Double) = if (lux) v else Unites.luxDepuisFc(v)
    fun distance(m: Double, d: Int = 2) = n(if (metres) m else Unites.pieds(m), d) + " " + uDistance
    fun eclairement(l: Double) = n(if (lux) l else Unites.fc(l), if (lux) 0 else 1) + " " + uEclairement
}

/** La source qu'on calcule : un projecteur à soi (et bientôt du catalogue), ou une saisie directe. */
private class SaisieSource {
    var cle by mutableStateOf("")
    var spot by mutableStateOf(false)
    var lux by mutableStateOf("")
    var distanceMesure by mutableStateOf("1")
    var angle by mutableStateOf("")
    var distance by mutableStateOf("3")
    var parHauteurs by mutableStateOf(false)
    var hauteurSource by mutableStateOf("2.5")
    var hauteurSujet by mutableStateOf("1.6")
    var recul by mutableStateOf("3")
    var incidence by mutableStateOf("0")
    var pertes by mutableStateOf("0")

    fun projecteur(): Projecteur? = MonMateriel.projecteur(cle)

    /** L'intensité de la source dans l'axe (cd), d'après son éclairement de référence. */
    fun intensite(u: UnitesLumiere): Double? {
        val p = projecteur()
        if (p == null) {
            val l = lire(lux)?.let { u.versLux(it) } ?: return null
            val d = lire(distanceMesure)?.let { u.versMetres(it) } ?: return null
            return Eclairage.intensite(l, d)
        }
        val m = p.mesure(spot) ?: p.mesures.firstOrNull() ?: return null
        return Eclairage.intensite(m.lux, m.distanceM)
    }

    fun angleFaisceau(): Double? = projecteur()?.angle(spot) ?: lire(angle)?.takeIf { it < 180 }

    /** La distance source–sujet en mètres, directe ou par les hauteurs. */
    fun distanceM(u: UnitesLumiere): Double? = if (!parHauteurs) lire(distance)?.let { u.versMetres(it) } else {
        val hs = lire(hauteurSource)?.let { u.versMetres(it) }
        val hj = lire(hauteurSujet)?.let { u.versMetres(it) }
        val r = lire(recul)?.let { u.versMetres(it) }
        if (hs != null && hj != null && r != null) Eclairage.geometrie(hs, hj, r).distance else null
    }
}

@Composable
fun PageLumiere(reglages: Reglages, cinema: DepotCinema) {
    val outil = runCatching { OutilLumiere.valueOf(reglages.lumiereOutil) }.getOrDefault(OutilLumiere.PROJECTEUR)
    val u = UnitesLumiere(reglages.lumiereMetres, reglages.lumiereLux)
    val source = remember { SaisieSource() }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Gouttiere, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutilLumiere.entries.forEach { o ->
            Pastille(o.libelle, accent = o == outil) { reglages.lumiereOutil = o.name }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(Interligne)
    ) {
        Box(Modifier.weight(1f)) {
            ChoixSegmente(listOf(true, false), reglages.lumiereMetres, { if (it) "mètres" else "pieds" }) { reglages.lumiereMetres = it }
        }
        Box(Modifier.weight(1f)) {
            ChoixSegmente(listOf(true, false), reglages.lumiereLux, { if (it) "lux" else "fc" }) { reglages.lumiereLux = it }
        }
    }

    when (outil) {
        OutilLumiere.PLAN_SOL -> ListePlansSol(reglages, cinema, null, tous = true)
        OutilLumiere.PROJECTEUR -> OutilProjecteur(reglages, source, u)
        OutilLumiere.PLACER -> OutilPlacer(reglages, source, u)
        OutilLumiere.RAPPORTS -> OutilRapports(u)
        OutilLumiere.COULEUR -> OutilCouleur()
        OutilLumiere.CHAMP -> OutilChamp(reglages, u)
        OutilLumiere.ELECTRICITE -> OutilElectricite(reglages)
        OutilLumiere.SCINTILLEMENT -> OutilScintillement(reglages)
        OutilLumiere.CONVERSIONS -> OutilConversions()
    }
    Spacer(Modifier.height(Interligne))
}

/* ── La source ─────────────────────────────────────────────────────────── */

private const val DIRECTE = ""

/** Choix de la source et de son éclairement de référence. */
@Composable
private fun ColumnScope.ChoixSource(source: SaisieSource, u: UnitesLumiere, reglages: Reglages) {
    val cles = listOf(DIRECTE) + MonMateriel.tousLesProjecteurs.map { it.identifiant }
    Deroulant("Source", cles, source.cle.takeIf { it in cles } ?: DIRECTE, { c ->
        if (c == DIRECTE) "Saisie directe" else MonMateriel.projecteur(c)?.let { MonMateriel.libelle(it) } ?: ""
    }, Modifier.fillMaxWidth()) { source.cle = it }
    Spacer(Modifier.height(6.dp))
    val p = source.projecteur()
    if (p == null) {
        LigneBoutons {
            Champ(source.lux, "Éclairement (${u.uEclairement})", Modifier.weight(1f)) { source.lux = it }
            Champ(source.distanceMesure, "à (${u.uDistance})", Modifier.weight(1f)) { source.distanceMesure = it }
        }
        Spacer(Modifier.height(6.dp))
        Champ(source.angle, "Angle du faisceau (°), facultatif", Modifier.fillMaxWidth()) { source.angle = it }
        Text(
            "Lu sur le tableau photométrique de la fiche, ou mesuré avec la cellule en incident, dans l'axe.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        return
    }
    if (p.mesure(true) != null && p.mesure(false) != null) {
        ChoixSegmente(listOf(false, true), source.spot, { if (it) "Serré (spot)" else "Ouvert (flood)" }) { source.spot = it }
        Spacer(Modifier.height(6.dp))
    }
    val m = p.mesure(source.spot) ?: p.mesures.firstOrNull()
    if (m == null) {
        Text(
            "Ce projecteur n'a pas d'éclairement saisi : ajoute-le dans Matériel › Mon matériel pour calculer ses lux.",
            style = Detail,
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    Ligne(
        "Référence",
        u.eclairement(m.lux) + " à " + u.distance(m.distanceM),
        listOfNotNull(
            if (m.spot) "serré" else "ouvert",
            p.angle(m.spot)?.let { "faisceau " + n(it, 0) + "°" },
            p.source,
            if (!p.relu) "à relire" else null
        ).joinToString(" · ")
    )
    var fiche by remember { mutableStateOf(false) }
    BoutonPlat("Fiche du projecteur", modifier = Modifier.padding(top = 4.dp)) { fiche = true }
    if (fiche) FeuilleFicheProjecteur(p, reglages) { fiche = false }
}

/** Distance (directe ou par les hauteurs), angle d'arrivée et pertes. */
@Composable
private fun ColumnScope.PositionSource(source: SaisieSource, u: UnitesLumiere, avecDistance: Boolean) {
    if (avecDistance) {
        ChoixSegmente(listOf(false, true), source.parHauteurs, { if (it) "Par les hauteurs" else "Distance directe" }) { source.parHauteurs = it }
        Spacer(Modifier.height(6.dp))
        if (!source.parHauteurs) {
            Champ(source.distance, "Distance source – sujet (${u.uDistance})", Modifier.fillMaxWidth()) { source.distance = it }
        } else {
            LigneBoutons {
                Champ(source.hauteurSource, "Projecteur (${u.uDistance})", Modifier.weight(1f)) { source.hauteurSource = it }
                Champ(source.hauteurSujet, "Sujet (${u.uDistance})", Modifier.weight(1f)) { source.hauteurSujet = it }
                Champ(source.recul, "Recul au sol", Modifier.weight(1f)) { source.recul = it }
            }
            val hs = lire(source.hauteurSource)?.let { u.versMetres(it) }
            val hj = lire(source.hauteurSujet)?.let { u.versMetres(it) }
            val r = lire(source.recul)?.let { u.versMetres(it) }
            if (hs != null && hj != null && r != null) {
                val g = Eclairage.geometrie(hs, hj, r)
                Ligne(
                    "Distance réelle", u.distance(g.distance),
                    "projecteur incliné de " + n(abs(g.inclinaison), 0) + "° " + if (g.inclinaison >= 0) "vers le bas" else "vers le haut"
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
    LigneBoutons {
        Champ(source.incidence, "Angle d'arrivée (°)", Modifier.weight(1f)) { source.incidence = it }
        Champ(source.pertes, "Pertes (diaphs)", Modifier.weight(1f)) { source.pertes = it }
    }
    Text(
        "Angle d'arrivée : 0° quand la lumière frappe le sujet de face ; l'éclairement suit son cosinus. Pertes : gélatines et diffusion. Repères de plateau : filet simple ≈ ½ diaph, filet double ≈ 1 diaph — à confirmer à la cellule.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

private fun ChampNul(texte: String): Double = texte.trim().replace(',', '.').toDoubleOrNull() ?: 0.0

/* ── Combien j'ai ici ? ────────────────────────────────────────────────── */

@Composable
private fun OutilProjecteur(reglages: Reglages, source: SaisieSource, u: UnitesLumiere) {
    val reglage = reglageTournage(reglages, FiltresJson.lire(reglages.cineFiltres))
    Carte(titre = "Combien j'ai ici ?", sousTitre = "L'éclairement au sujet, et l'ouverture qu'il donne avec le réglage de ta caméra.") {
        ChoixSource(source, u, reglages)
        Spacer(Modifier.height(Interligne))
        PositionSource(source, u, avecDistance = true)
    }
    val i = source.intensite(u)
    val d = source.distanceM(u)
    val angle = source.angleFaisceau()
    Carte(titre = "Résultat") {
        if (i == null || d == null) {
            Text("Il manque l'éclairement de référence ou la distance.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Carte
        }
        val lux = Eclairage.lux(i, d, ChampNul(source.incidence), ChampNul(source.pertes))
        val ev = Eclairage.ev100(lux)
        val t = Tournage.ouvertureExacte(ev, reglage)
        Ligne("Éclairement au sujet", u.eclairement(lux), "EV " + n(ev) + " à 100 ISO", accent = true)
        Ligne(
            "Ouverture",
            Tournage.tStopLePlusProche(t).libelle,
            "exacte T" + n(t, 2) + " · EI ${reglage.ei}, " + Tournage.libelleTempsPose(reglage.tempsPose) +
                (if (reglage.perteFiltres > 0) ", filtres caméra compris" else ""),
            accent = true
        )
        angle?.let { a ->
            Ligne("Tache de lumière", "⌀ " + u.distance(Eclairage.diametre(a, d), 1), "faisceau de " + n(a, 0) + "° à " + u.distance(d))
        }
        Text(
            "La loi en 1/d² vaut pour une source vue de loin : à moins d'environ cinq fois sa taille (panneau, cadre de diffusion, réflecteur), l'éclairement réel décroît moins vite. Vérifie à la cellule.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/* ── Où le placer ? ────────────────────────────────────────────────────── */

@Composable
private fun OutilPlacer(reglages: Reglages, source: SaisieSource, u: UnitesLumiere) {
    val reglage = reglageTournage(reglages, FiltresJson.lire(reglages.cineFiltres))
    var tVoulu by remember { mutableStateOf(T_STOPS.minByOrNull { abs(it.exact - 2.8) } ?: T_STOPS.first()) }
    var largeur by remember { mutableStateOf("2") }
    Carte(titre = "Où le placer ?", sousTitre = "La distance pour tenir l'ouverture voulue, ou pour couvrir une largeur.") {
        ChoixSource(source, u, reglages)
        Spacer(Modifier.height(Interligne))
        PositionSource(source, u, avecDistance = false)
        Spacer(Modifier.height(Interligne))
        LigneBoutons {
            Deroulant("Je veux", T_STOPS, tVoulu, { it.libelle }, Modifier.weight(1f)) { tVoulu = it }
            Champ(largeur, "Largeur à couvrir (${u.uDistance})", Modifier.weight(1f)) { largeur = it }
        }
    }
    val intensite = source.intensite(u)
    val angle = source.angleFaisceau()
    Carte(titre = "Résultat") {
        val i = intensite
        if (i == null) {
            Text("Il manque l'éclairement de référence de la source.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val luxVoulu = Eclairage.luxPour(tVoulu.exact, reglage)
            val d = Eclairage.distancePour(i, luxVoulu, ChampNul(source.incidence), ChampNul(source.pertes))
            Ligne(
                "Pour ${tVoulu.libelle}", "à " + u.distance(d),
                "il faut " + u.eclairement(luxVoulu) + " au sujet · EI ${reglage.ei}, " + Tournage.libelleTempsPose(reglage.tempsPose),
                accent = true
            )
        }
        val a = angle
        val l = lire(largeur)?.let { u.versMetres(it) }
        if (a != null && l != null) {
            Ligne("Pour couvrir " + u.distance(l, 1), "à " + u.distance(Eclairage.distancePourCouvrir(a, l)), "faisceau de " + n(a, 0) + "°")
        } else if (l != null) {
            Text("Donne l'angle du faisceau pour savoir où il couvre cette largeur.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/* ── Rapports et ambiances ─────────────────────────────────────────────── */

@Composable
private fun OutilRapports(u: UnitesLumiere) {
    var face by remember { mutableStateOf("800") }
    var debouchage by remember { mutableStateOf("200") }
    var contre by remember { mutableStateOf("") }
    var autres by remember { mutableStateOf("") }
    Carte(titre = "Rapports", sousTitre = "Face, débouchage, contre : l'écart en diaphs et le rapport, mesurés en incident au sujet.") {
        LigneBoutons {
            Champ(face, "Face (${u.uEclairement})", Modifier.weight(1f)) { face = it }
            Champ(debouchage, "Débouchage", Modifier.weight(1f)) { debouchage = it }
            Champ(contre, "Contre", Modifier.weight(1f)) { contre = it }
        }
        val f = lire(face)?.let { u.versLux(it) }
        val b = lire(debouchage)?.let { u.versLux(it) }
        val c = lire(contre)?.let { u.versLux(it) }
        Spacer(Modifier.height(6.dp))
        if (f != null && b != null) {
            val ecart = Eclairage.diaphs(f / b)
            Ligne("Face / débouchage", n(f / b, 1) + " : 1", "écart de " + libelleDiaphs(ecart).removePrefix("+") + " diaph", accent = true)
            Ligne(
                "Côté éclairé / côté ombre", n((f + b) / b, 1) + " : 1",
                "le côté éclairé reçoit la face et le débouchage — c'est le « rapport d'éclairage » de beaucoup de manuels"
            )
            AMBIANCES.firstOrNull { f / b >= it.rapportMin && f / b < it.rapportMax }?.let {
                Ligne("Ambiance", it.nom, it.idee)
            }
        }
        if (f != null && c != null) {
            Ligne("Contre / face", libelleDiaphs(Eclairage.diaphs(c / f)) + " diaph", "au-dessus de 0 : le contre domine la face")
        }
        Separateur()
        Champ(autres, "Autres sources sur le sujet (${u.uEclairement})", Modifier.fillMaxWidth(), indication = "300, 150") { autres = it }
        val somme = Regex("\\d+(?:[.,]\\d+)?").findAll(autres).mapNotNull { it.value.replace(',', '.').toDoubleOrNull() }.map { u.versLux(it) }.sum()
        if (f != null && somme > 0) {
            Ligne("Total au sujet", u.eclairement(f + somme), "les éclairements s'additionnent ; +" + libelleDiaphs(Eclairage.diaphs((f + somme) / f)).removePrefix("+") + " diaph sur la face seule")
        }
    }
    Carte(titre = "Ambiances", sousTitre = "Des repères d'usage, pas des normes : rapport de la face seule au débouchage seul.") {
        AMBIANCES.forEach { a ->
            Ligne(
                a.nom,
                n(a.rapportMin, 0) + ":1 à " + n(a.rapportMax, 0) + ":1",
                n(a.diaphsMin, 0) + " à " + n(a.diaphsMax, 0) + " diaphs · " + a.idee
            )
        }
        val f = lire(face)?.let { u.versLux(it) }
        if (f != null) {
            Separateur()
            Text("Avec ta face à " + u.eclairement(f) + ", le débouchage :", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AMBIANCES.forEach { a ->
                Ligne(a.nom, u.eclairement(f / a.rapportMax) + " à " + u.eclairement(f / a.rapportMin))
            }
        }
    }
}

/* ── Couleur ───────────────────────────────────────────────────────────── */

@Composable
private fun OutilCouleur() {
    var source by remember { mutableStateOf("3200") }
    var cible by remember { mutableStateOf("5600") }
    var marque by remember { mutableStateOf("Lee") }
    val reperes = listOf(2700.0, 3200.0, 4300.0, 5600.0, 6500.0)
    Carte(titre = "Mireds", sousTitre = "Un mired = un million divisé par la température en kelvins. Les décalages des gélatines s'additionnent en mireds, pas en kelvins.") {
        Champ(source, "Source (K)", Modifier.fillMaxWidth()) { source = it }
        RangeeReperes(reperes) { source = n(it, 0) }
        Spacer(Modifier.height(Interligne))
        Champ(cible, "Cible : balance de la caméra (K)", Modifier.fillMaxWidth()) { cible = it }
        RangeeReperes(reperes) { cible = n(it, 0) }
        val s = lire(source)
        val c = lire(cible)
        if (s != null && c != null) {
            val d = Mireds.decalage(s, c)
            Spacer(Modifier.height(6.dp))
            Ligne("Source", n(Mireds.depuisKelvins(s), 0) + " mireds")
            Ligne("Cible", n(Mireds.depuisKelvins(c), 0) + " mireds")
            Ligne(
                "À corriger", (if (d > 0) "+" else "") + n(d, 0) + " mireds",
                if (abs(d) < 5) "déjà raccord" else if (d > 0) "vers le chaud : gélatine orange (CTO)" else "vers le froid : gélatine bleue (CTB)",
                accent = true
            )
        }
    }
    val s = lire(source)
    val c = lire(cible)
    if (s != null && c != null && abs(Mireds.decalage(s, c)) >= 5) {
        val d = Mireds.decalage(s, c)
        Carte(titre = "Gélatines sur la source") {
            ChoixSegmente(listOf("Lee", "Rosco"), marque, { it }) { marque = it }
            Spacer(Modifier.height(6.dp))
            Mireds.combinaisons(s, d, marque).forEach { k ->
                val pertes = k.gelatines.map { it.perteDiaphs }
                val perte = if (pertes.all { it != null }) pertes.sumOf { it!! } else null
                val aRelire = k.gelatines.any { !it.relue }
                Ligne(
                    k.gelatines.joinToString(" + ") { it.reference + " " + it.nom },
                    n(k.kelvinsObtenus, 0) + " K",
                    listOfNotNull(
                        (if (k.mireds > 0) "+" else "") + n(k.mireds, 0) + " mireds · reste " + (if (k.residuel > 0) "+" else "") + n(k.residuel, 0),
                        perte?.let { "coûte " + n(it, 1) + " diaph" } ?: "perte : voir la fiche",
                        if (aRelire) "valeur à relire" else null
                    ).joinToString(" · ")
                )
            }
            Text(
                "Décalages et transmissions relevés sur les fiches Lee et Rosco et chez leurs distributeurs ; « à relire » quand la valeur n'a pas été retrouvée.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
    Carte(titre = "Vert et magenta", sousTitre = "Pour raccorder une source à des tubes fluorescents ou à des LED qui tirent vers le vert, ou l'inverse.") {
        listOf(FamilleGelatine.PLUS_VERT, FamilleGelatine.MOINS_VERT).forEach { fam ->
            Etiquette(fam.libelle)
            GELATINES.filter { it.famille == fam }.forEach { g ->
                Ligne(g.marque + " " + g.reference, g.nom, "correction " + g.fraction)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun RangeeReperes(valeurs: List<Double>, surChoix: (Double) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        valeurs.forEach { v -> Pastille(n(v, 0) + " K") { surChoix(v) } }
    }
}

/* ── Champ et profondeur de champ ──────────────────────────────────────── */

@Composable
private fun OutilChamp(reglages: Reglages, u: UnitesLumiere) {
    val cadre = remember(reglages.cineCadre) { CadreJson.lireTexte(reglages.cineCadre) }
    var largeur by remember { mutableStateOf(cadre.capteurLargeurMm?.let { n(it, 2) } ?: "24.89") }
    var hauteur by remember { mutableStateOf(cadre.capteurHauteurMm?.let { n(it, 2) } ?: "18.66") }
    var focale by remember { mutableStateOf(cadre.focaleMm?.let { n(it, 1) } ?: "35") }
    var distance by remember { mutableStateOf("3") }
    var cadrer by remember { mutableStateOf("2") }
    var ouverture by remember { mutableStateOf("2.8") }
    var cercle by remember { mutableStateOf("") }
    var faisceau by remember { mutableStateOf("") }

    val l = lire(largeur)
    val h = lire(hauteur)
    val f = lire(focale)
    val d = lire(distance)?.let { u.versMetres(it) }
    Carte(titre = "Champ", sousTitre = "Le capteur et la focale viennent du cadre de travail ; tout se modifie ici sans le toucher.") {
        LigneBoutons {
            Champ(largeur, "Capteur L (mm)", Modifier.weight(1f)) { largeur = it }
            Champ(hauteur, "H (mm)", Modifier.weight(1f)) { hauteur = it }
        }
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            Champ(focale, "Focale (mm)", Modifier.weight(1f)) { focale = it }
            Champ(distance, "Distance (${u.uDistance})", Modifier.weight(1f)) { distance = it }
        }
        if (l != null && h != null && f != null) {
            Spacer(Modifier.height(6.dp))
            val diag = sqrt(l * l + h * h)
            Ligne(
                "Angles de champ",
                n(Focales.angleDeChamp(l * cadre.desanamorphose, f), 1) + "° × " + n(Focales.angleDeChamp(h, f), 1) + "°",
                "diagonale " + n(Focales.angleDeChamp(diag, f), 1) + "°" + if (cadre.desanamorphose != 1.0) " · désanamorphosé ×" + n(cadre.desanamorphose, 1) else "",
                accent = true
            )
            if (d != null) {
                Ligne(
                    "À " + u.distance(d, 1) + ", le cadre couvre",
                    u.distance(Optique.couverture(d, f, l * cadre.desanamorphose), 2) + " × " + u.distance(Optique.couverture(d, f, h), 2)
                )
            }
        }
        Spacer(Modifier.height(Interligne))
        Champ(cadrer, "Je veux cadrer une largeur de (${u.uDistance})", Modifier.fillMaxWidth()) { cadrer = it }
        val w = lire(cadrer)?.let { u.versMetres(it) }
        if (l != null && d != null && w != null) {
            Ligne("Focale à prendre", n(Optique.focalePour(w, d, l * cadre.desanamorphose), 1) + " mm", "à " + u.distance(d, 1))
        }
    }
    Carte(titre = "Profondeur de champ", sousTitre = "Le cercle de confusion est une convention (diagonale ÷ 1500) ; le T sert de f, l'écart est faible.") {
        LigneBoutons {
            Champ(ouverture, "Ouverture (T ou f)", Modifier.weight(1f)) { ouverture = it }
            Champ(cercle, "Cercle (mm)", Modifier.weight(1f), indication = if (l != null && h != null) n(Optique.cercleConfusion(l, h), 3) else "") { cercle = it }
        }
        val nOuv = lire(ouverture)
        val c = lire(cercle) ?: if (l != null && h != null) Optique.cercleConfusion(l, h) else null
        if (f != null && nOuv != null && c != null && d != null) {
            val p = Optique.profondeur(f, nOuv, c, d)
            Ligne(
                "Net de", u.distance(p.procheM) + " à " + (p.loinM?.let { u.distance(it) } ?: "l'infini"),
                "mise au point à " + u.distance(d, 1) + (p.loinM?.let { " · profondeur " + u.distance(it - p.procheM) } ?: ""),
                accent = true
            )
            Ligne("Hyperfocale", u.distance(p.hyperfocaleM, 1), "au point là, net de la moitié à l'infini")
        }
    }
    Carte(titre = "La lumière couvre-t-elle le cadre ?", sousTitre = "Pour un projecteur posé près de la caméra, dans le même axe.") {
        Champ(faisceau, "Angle du faisceau (°)", Modifier.fillMaxWidth()) { faisceau = it }
        val b = lire(faisceau)
        if (b != null && l != null && f != null) {
            val champH = Focales.angleDeChamp(l * cadre.desanamorphose, f)
            val part = tan(Math.toRadians(b / 2)) / tan(Math.toRadians(champH / 2))
            Ligne(
                if (part >= 1) "Oui" else "Non",
                "faisceau " + n(b, 0) + "° · champ " + n(champH, 0) + "°",
                if (part >= 1) "la tache déborde le cadre" else "la tache couvre " + n(part * 100, 0) + " % de la largeur du cadre",
                accent = true
            )
        }
    }
}

/* ── Électricité ───────────────────────────────────────────────────────── */

private data class Charge(val nom: String, val watts: Double, val quantite: Int)

@Composable
private fun OutilElectricite(reglages: Reglages) {
    val region = runCatching { Region.valueOf(reglages.lumiereRegion) }.getOrDefault(Region.EUROPE)
    val circuits = CIRCUITS.filter { it.region == region }
    var circuitNom by remember(region) { mutableStateOf(circuits.first().nom) }
    val circuit = circuits.firstOrNull { it.nom == circuitNom } ?: circuits.first()
    val charges = remember { mutableStateListOf<Charge>() }
    var nom by remember { mutableStateOf("") }
    var watts by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("1") }
    var cosPhi by remember { mutableStateOf("1") }

    Carte(titre = "Électricité", sousTitre = "Un ordre de grandeur pour répartir les projecteurs. Ce n'est pas un outil de sécurité : le branchement reste l'affaire d'un électricien.") {
        ChoixSegmente(Region.entries.toList(), region, { if (it == Region.EUROPE) "Europe" else "États-Unis" }) { reglages.lumiereRegion = it.name }
        Spacer(Modifier.height(6.dp))
        Deroulant("Circuit", circuits, circuit, { it.nom + " · " + n(it.puissance / 1000, 1) + " kVA" }, Modifier.fillMaxWidth()) { circuitNom = it.nom }
        Spacer(Modifier.height(Interligne))
        val avecPuissance = MonMateriel.tousLesProjecteurs.filter { it.puissanceW != null }
        if (avecPuissance.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                avecPuissance.forEach { p ->
                    Pastille(MonMateriel.libelle(p) + " · " + n(p.puissanceW!!, 0) + " W") { charges.add(Charge(p.nomComplet, p.puissanceW!!, 1)) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        LigneBoutons {
            Champ(nom, "Appareil", Modifier.weight(1.4f)) { nom = it }
            Champ(watts, "W", Modifier.weight(1f)) { watts = it }
            Champ(quantite, "×", Modifier.weight(0.7f)) { quantite = it }
        }
        BoutonPlat("Ajouter", modifier = Modifier.padding(top = 6.dp)) {
            val w = lire(watts)
            if (w != null) {
                charges.add(Charge(nom.ifBlank { "Appareil" }, w, lire(quantite)?.toInt()?.coerceAtLeast(1) ?: 1))
                nom = ""
                watts = ""
                quantite = "1"
            }
        }
        if (charges.isNotEmpty()) Separateur()
        charges.forEachIndexed { i, c ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { Ligne(c.nom, n(c.watts * c.quantite, 0) + " W", if (c.quantite > 1) "${c.quantite} × " + n(c.watts, 0) + " W" else null) }
                BoutonPlat("✕") { charges.removeAt(i) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Champ(cosPhi, "Facteur de puissance (cos φ)", Modifier.fillMaxWidth()) { cosPhi = it }
        Text(
            "1 pour le tungstène. Pour les LED et les ballasts électroniques, et surtout les ballasts magnétiques de HMI, lis-le sur la fiche.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    val total = charges.sumOf { it.watts * it.quantite }
    if (total > 0) {
        val cp = lire(cosPhi)?.coerceIn(0.1, 1.0) ?: 1.0
        val a = Electricite.intensite(total, circuit, cp)
        val charge = Electricite.charge(total, circuit, cp)
        Carte(titre = "Bilan") {
            Ligne("Puissance totale", n(total, 0) + " W")
            Ligne(
                "Intensité", n(a, 1) + " A" + if (circuit.triphase) " par phase" else "",
                "sur " + circuit.nom + " (" + n(circuit.tension, 0) + " V" + (if (circuit.triphase) " tri, réparti sur les trois phases" else "") + ")",
                accent = true
            )
            Ligne(
                "Charge du circuit", n(charge * 100, 0) + " %",
                when {
                    charge > 1 -> "dépassé : le disjoncteur saute"
                    charge > Electricite.MARGE -> "au-delà de 80 % : trop près de la limite pour une charge continue"
                    else -> "dans la marge (moins de 80 %)"
                },
                accent = true
            )
            Ligne("Groupe électrogène", "au moins " + n(Electricite.kvaGroupe(total / cp), 1) + " kVA", "puissance d'un groupe = 0,8 × kVA ; prends de la marge pour les démarrages")
        }
    }
}

/* ── Scintillement ─────────────────────────────────────────────────────── */

@Composable
private fun OutilScintillement(reglages: Reglages) {
    var cadence by remember { mutableStateOf(n(reglages.cineCadence, 3)) }
    var angle by remember { mutableStateOf(n(reglages.cineAngle, 1)) }
    val secteur = reglages.cineSecteur
    Carte(
        titre = "Scintillement",
        sousTitre = "Sur secteur, certaines sources battent à deux fois sa fréquence : HMI et tubes à ballast magnétique, des LED à variation par impulsions. Le tungstène, chaud, bat très peu."
    ) {
        ChoixSegmente(listOf(50.0, 60.0), secteur, { if (it == 50.0) "50 Hz (Europe)" else "60 Hz (États-Unis)" }) { reglages.cineSecteur = it }
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            Champ(cadence, "Cadence (im/s)", Modifier.weight(1f)) { cadence = it }
            Champ(angle, "Obturateur (°)", Modifier.weight(1f)) { angle = it }
        }
        val c = lire(cadence)
        val a = lire(angle)
        if (c != null && a != null) {
            val v = Scintillement.verdict(c, a, secteur)
            Spacer(Modifier.height(6.dp))
            Ligne(
                v.libelle,
                Tournage.libelleTempsPose(Tournage.tempsPose(a, c)),
                when (v) {
                    VerdictScintillement.SUR -> "le temps de pose contient un nombre entier de battements"
                    VerdictScintillement.SUR_SI_SYNCHRO -> "chaque image démarre au même moment du cycle, tant que la cadence est tenue"
                    VerdictScintillement.RISQUE -> "l'exposition varie d'une image à l'autre avec ces sources"
                },
                accent = true
            )
            Etiquette("Angles sûrs à ${n(c, 3)} im/s")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Scintillement.anglesSurs(c, secteur).forEach { s ->
                    Pastille(n(s, 1) + "°") {
                        angle = n(s, 1)
                        reglages.cineAngle = Math.round(s * 10) / 10.0
                        reglages.cineCadence = c
                    }
                }
            }
            Text(
                "Toucher un angle le règle aussi sur la cellule de tournage.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Separateur()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Avertir dans le viseur", style = Corps, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Switch(checked = reglages.cineAlerteScintillement, onCheckedChange = { reglages.cineAlerteScintillement = it })
        }
    }
}

/* ── Conversions ───────────────────────────────────────────────────────── */

@Composable
private fun OutilConversions() {
    var lux by remember { mutableStateOf("1000") }
    var diaphs by remember { mutableStateOf("1") }
    var fNombre by remember { mutableStateOf("2.8") }
    var transmission by remember { mutableStateOf("85") }
    var cadence by remember { mutableStateOf("25") }
    var angle by remember { mutableStateOf("180") }
    Carte(titre = "Éclairement") {
        Champ(lux, "Lux", Modifier.fillMaxWidth()) { lux = it }
        lire(lux)?.let { l ->
            Ligne("Foot-candles", n(Unites.fc(l), 1) + " fc")
            Ligne("EV à 100 ISO", n(Photometrie.evDepuisLux(l)), "en incident (C = 250)")
        }
    }
    Carte(titre = "Diaphs, densité, transmission") {
        Champ(diaphs, "Diaphs retenus", Modifier.fillMaxWidth()) { diaphs = it }
        lire(diaphs)?.let { d ->
            Ligne("Densité", n(Densites.densite(d), 2), "les neutres s'arrondissent à 0,3 par diaph")
            Ligne("Lumière transmise", n(Densites.transmission(d) * 100, 1) + " %")
        }
    }
    Carte(titre = "f et T", sousTitre = "Le T tient compte de ce que le verre absorbe : T = f ÷ √transmission.") {
        LigneBoutons {
            Champ(fNombre, "Ouverture f", Modifier.weight(1f)) { fNombre = it }
            Champ(transmission, "Transmission (%)", Modifier.weight(1f)) { transmission = it }
        }
        val f = lire(fNombre)
        val t = lire(transmission)?.let { it / 100 }?.takeIf { it <= 1 }
        if (f != null && t != null) {
            Ligne("Ouverture T", "T" + n(Densites.tStop(f, t), 2), "écart de " + n(Densites.diaphsDeTransmission(t), 2) + " diaph")
        }
    }
    Carte(titre = "Obturateur") {
        LigneBoutons {
            Champ(cadence, "Cadence (im/s)", Modifier.weight(1f)) { cadence = it }
            Champ(angle, "Angle (°)", Modifier.weight(1f)) { angle = it }
        }
        val c = lire(cadence)
        val a = lire(angle)
        if (c != null && a != null) Ligne("Temps de pose", Tournage.libelleTempsPose(Tournage.tempsPose(a, c)), "angle ÷ (360 × cadence)")
    }
}
