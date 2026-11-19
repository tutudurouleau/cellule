package fr.cellule.app.ecrans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.Detail
import fr.cellule.app.FiltresJson
import fr.cellule.app.MonMateriel
import fr.cellule.app.PlansSol
import fr.cellule.app.RayonControle
import fr.cellule.app.Reglages
import fr.cellule.app.sol.DessinSol
import fr.cellule.app.sol.OptionsDessin
import fr.cellule.app.sol.VueSol
import fr.cellule.core.Cible
import fr.cellule.core.DeplacementProjecteur
import fr.cellule.core.Deplacement
import fr.cellule.core.Eclairage
import fr.cellule.core.ElementSol
import fr.cellule.core.GardeHauteur
import fr.cellule.core.PlanSol
import fr.cellule.core.PorteeCible
import fr.cellule.core.Projecteur
import fr.cellule.core.SaisieMateriel
import fr.cellule.core.Sol
import fr.cellule.core.T_STOPS
import fr.cellule.core.TStop
import fr.cellule.core.Tournage
import fr.cellule.core.TypeElement
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt
import kotlin.math.abs

/*
 * Déplacer un projecteur du plan pour obtenir ce qu'on veut sur un sujet :
 * un recul donné, un éclairement, un diaph, ou un rapport avec un autre
 * projecteur. La nouvelle place s'affiche en fantôme ; rien ne bouge sur le
 * plan avant « Appliquer ».
 */

private enum class ActionDeplacement(val libelle: String) {
    RECUL("Recul"), LUX("Lux"), DIAPH("Diaph"), RAPPORT("Rapport")
}

private val RAPPORTS_RAPIDES = listOf(0.5, 1.0, 2.0, 3.0, 4.0, 8.0)

private fun libelleRapport(r: Double): String = if (r >= 1) nombreCourt(r, 1) + " : 1" else "1 : " + nombreCourt(1 / r, 1)

private fun libelleEcart(diaphs: Double): String {
    val signe = if (diaphs > 0.05) "+" else if (diaphs < -0.05) "−" else ""
    return signe + nombreCourt(abs(diaphs), 1) + " diaph"
}

/**
 * Le bloc « Déplacer ». [surApercu] reçoit la position proposée (null : rien
 * à montrer) ; [surAppliquer] reçoit le projecteur à mettre sur le plan.
 */
@Composable
fun BlocDeplacerProjecteur(
    plan: PlanSol,
    projecteur: ElementSol,
    reglages: Reglages,
    sujetInitial: Long? = null,
    fermable: Boolean = true,
    surPlafond: (Double?) -> Unit,
    surApercu: (ElementSol?) -> Unit,
    surAppliquer: (ElementSol) -> Unit,
    surFermer: () -> Unit
) {
    val metres = reglages.lumiereMetres
    val enLux = reglages.lumiereLux
    fun eclair(l: Double) = if (enLux) nombreCourt(l, 0) + " lux" else nombreCourt(Unites.fc(l), 1) + " fc"
    fun dist(m: Double) = if (metres) nombreCourt(m, 2) + " m" else nombreCourt(Unites.pieds(m), 1) + " ft"
    val projos: (String) -> Projecteur? = { MonMateriel.projecteur(it) }
    val choisi = projos(projecteur.projecteur)
    val nomProjo = projecteur.nom.ifBlank { choisi?.nomComplet ?: "Projecteur" }
    val reglage = reglageTournage(reglages, FiltresJson.lire(reglages.cineFiltres))
    fun diaph(l: Double) = Tournage.tStopLePlusProche(Tournage.ouvertureExacte(Eclairage.ev100(l), reglage)).libelle
    val cle = projecteur.identifiant

    Row(verticalAlignment = Alignment.CenterVertically) {
        Etiquette("Déplacer · $nomProjo", modifier = Modifier.weight(1f), accent = true)
        if (fermable) BoutonPlat("Fermer") { surApercu(null); surFermer() }
    }
    Spacer(Modifier.height(6.dp))

    val sujets = plan.elements.filter { it.type == TypeElement.COMEDIEN }
    if (sujets.isEmpty()) {
        LaunchedEffect(cle) { surApercu(null) }
        Text(
            "Pose d'abord un comédien sur le plan : c'est par rapport à lui qu'on règle la lumière.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    /* Par défaut, le sujet que ce projecteur éclaire le plus. */
    var sujetId by remember(cle, sujetInitial) {
        mutableStateOf(
            sujetInitial?.takeIf { id -> sujets.any { it.identifiant == id } }
                ?: sujets.maxByOrNull { s -> DeplacementProjecteur.etat(plan, projecteur, s, projos).lux ?: -1.0 }?.identifiant
        )
    }
    val sujet = sujets.firstOrNull { it.identifiant == sujetId } ?: sujets.first()
    var action by remember(cle) { mutableStateOf(ActionDeplacement.LUX) }
    var portee by remember(cle) { mutableStateOf(PorteeCible.SEUL) }
    var garde by remember(cle) { mutableStateOf(GardeHauteur.ANGLE) }
    var reculer by remember(cle) { mutableStateOf(true) }
    var reculTexte by remember(cle, metres) { mutableStateOf(if (metres) "1" else "3") }
    var luxTexte by remember(cle, enLux) { mutableStateOf("") }
    var tVoulu by remember(cle) { mutableStateOf<TStop?>(null) }
    var autreId by remember(cle) { mutableStateOf<Long?>(null) }
    var rapportTexte by remember(cle) { mutableStateOf("2") }

    Deroulant("Sujet", sujets.map { it.identifiant }, sujet.identifiant, { id -> sujets.first { it.identifiant == id }.libelle }, Modifier.fillMaxWidth()) {
        sujetId = it
    }
    val avant = DeplacementProjecteur.etat(plan, projecteur, sujet, projos)
    Ligne(
        "Maintenant",
        avant.lux?.let { eclair(it) } ?: "lux inconnus",
        listOfNotNull(
            "à " + dist(avant.distance),
            if (avant.total > 0) "total au sujet " + eclair(avant.total) + " · " + diaph(avant.total) else null,
            if (!avant.dansFaisceau) "hors du faisceau" else null
        ).joinToString(" · ")
    )
    Spacer(Modifier.height(4.dp))
    ChoixSegmente(ActionDeplacement.entries.toList(), action, { it.libelle }) { action = it }
    Spacer(Modifier.height(6.dp))

    var deplacement: Deplacement? = null
    var probleme: String? = null
    var apportVoulu: Double? = null
    fun retenir(c: Cible) {
        deplacement = c.deplacement
        probleme = c.probleme
        apportVoulu = c.apportVoulu
    }

    when (action) {
        ActionDeplacement.RECUL -> {
            LigneBoutons {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    ChoixSegmente(listOf(true, false), reculer, { if (it) "Reculer" else "Avancer" }) { reculer = it }
                }
                Champ(reculTexte, "De" + if (metres) " (m)" else " (ft)", Modifier.weight(0.7f)) { reculTexte = it }
            }
            val v = SaisieMateriel.lireNombre(reculTexte)?.let { if (metres) it else Unites.metres(it) }
            if (v != null) {
                val max = DeplacementProjecteur.avanceMax(projecteur, sujet)
                if (!reculer && v > max) {
                    probleme = if (max <= 0) "Il est déjà au niveau du sujet dans son axe : il ne peut pas avancer plus."
                    else "Il dépasserait le sujet : dans son axe, il peut avancer de " + dist(max) + " au plus."
                } else {
                    deplacement = DeplacementProjecteur.deplacer(plan, projecteur, sujet, if (reculer) v else -v, garde, plan.plafond, projos)
                }
            }
        }
        ActionDeplacement.LUX -> {
            Champ(luxTexte, if (enLux) "Lux voulus" else "Footcandles voulus", Modifier.fillMaxWidth(), indication = avant.lux?.let { nombreCourt(if (enLux) it / 2 else Unites.fc(it) / 2, 0) } ?: "") {
                luxTexte = it
            }
            Spacer(Modifier.height(4.dp))
            ChoixSegmente(PorteeCible.entries.toList(), portee, { it.libelle }) { portee = it }
            SaisieMateriel.lireNombre(luxTexte)?.let { v ->
                retenir(DeplacementProjecteur.pourLux(plan, projecteur, sujet, if (enLux) v else Unites.luxDepuisFc(v), portee, garde, plan.plafond, projos))
            }
        }
        ActionDeplacement.DIAPH -> {
            val reference = if (portee == PorteeCible.TOTAL) avant.total else avant.lux ?: 0.0
            val parDefaut = if (reference > 0) Tournage.tStopLePlusProche(Tournage.ouvertureExacte(Eclairage.ev100(reference), reglage)) else T_STOPS[9]
            val t = tVoulu ?: parDefaut
            LigneBoutons {
                Deroulant("Je veux", T_STOPS.take(28), t, { it.libelle }, Modifier.weight(1f)) { tVoulu = it }
                androidx.compose.foundation.layout.Box(Modifier.weight(1.4f)) {
                    ChoixSegmente(PorteeCible.entries.toList(), portee, { if (it == PorteeCible.SEUL) "Seul" else "Total" }) { portee = it }
                }
            }
            Text(
                "À EI ${reglage.ei}, " + Tournage.libelleTempsPose(reglage.tempsPose) + " (réglage du Matériel) : il faut " + eclair(Eclairage.luxPour(t.exact, reglage)) + ".",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (tVoulu != null) {
                retenir(DeplacementProjecteur.pourLux(plan, projecteur, sujet, Eclairage.luxPour(t.exact, reglage), portee, garde, plan.plafond, projos))
            }
        }
        ActionDeplacement.RAPPORT -> {
            val autres = plan.elements.filter { it.type == TypeElement.PROJECTEUR && it.identifiant != projecteur.identifiant }
            if (autres.isEmpty()) {
                Text("Il faut un autre projecteur sur le plan pour viser un rapport.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val autre = autres.firstOrNull { it.identifiant == autreId } ?: autres.first()
                fun nomDe(e: ElementSol) = e.nom.ifBlank { projos(e.projecteur)?.nomComplet ?: "Projecteur" }
                LigneBoutons {
                    Deroulant("Par rapport à", autres.map { it.identifiant }, autre.identifiant, { id -> nomDe(autres.first { it.identifiant == id }) }, Modifier.weight(1.4f)) {
                        autreId = it
                    }
                    Champ(rapportTexte, "Rapport (× l'autre)", Modifier.weight(1f)) { rapportTexte = it }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RAPPORTS_RAPIDES.forEach { r ->
                        Pastille(libelleRapport(r), accent = SaisieMateriel.lireNombre(rapportTexte) == r) { rapportTexte = nombreCourt(r, 2) }
                    }
                }
                val luxAutre = DeplacementProjecteur.etat(plan, autre, sujet, projos).lux
                val r = SaisieMateriel.lireNombre(rapportTexte)
                if (luxAutre == null || luxAutre <= 0) {
                    probleme = nomDe(autre) + " ne donne rien de connu à ce sujet (intensité inconnue ou sujet hors de son faisceau)."
                } else if (r != null) {
                    Text(
                        nomDe(autre) + " donne " + eclair(luxAutre) + " : " + nomProjo + " doit en donner " + eclair(luxAutre * r) +
                            " (" + libelleRapport(r) + ", " + libelleEcart(Eclairage.diaphs(r)) + ").",
                        style = Detail,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    retenir(DeplacementProjecteur.pourLux(plan, projecteur, sujet, luxAutre * r, PorteeCible.SEUL, garde, plan.plafond, projos))
                }
            }
        }
    }

    /* La hauteur en reculant, et le plafond du lieu. */
    Spacer(Modifier.height(6.dp))
    LigneBoutons {
        androidx.compose.foundation.layout.Box(Modifier.weight(1.4f)) {
            ChoixSegmente(GardeHauteur.entries.toList(), garde, { if (it == GardeHauteur.ANGLE) "Garder l'angle" else "Garder la hauteur" }) { garde = it }
        }
        ChampLongueur(plan.plafond, "Plafond", metres, "plafond-" + plan.identifiant, Modifier.weight(1f)) { surPlafond(it?.takeIf { h -> h > 0.3 }) }
    }

    /* Le résultat. */
    val d = deplacement
    val p = probleme
    Spacer(Modifier.height(6.dp))
    if (p != null) BandeauEtat(p, alerte = true)
    if (d != null) {
        Ligne(
            if (d.recul >= 0) "Reculer de " + dist(d.recul) else "Avancer de " + dist(-d.recul),
            d.apres.lux?.let { eclair(it) } ?: "—",
            listOfNotNull(
                "à " + dist(d.apres.distance) + " du sujet",
                if (d.apres.total > 0) "total " + eclair(d.apres.total) + " · " + diaph(d.apres.total) else null,
                d.avant.lux?.let { a -> d.apres.lux?.takeIf { it > 0 && a > 0 }?.let { libelleEcart(Eclairage.diaphs(it / a)) } }
            ).joinToString(" · "),
            accent = true
        )
        if (abs(d.apres.hauteur - d.avant.hauteur) >= 0.01) {
            Ligne("Hauteur", dist(d.avant.hauteur) + " → " + dist(d.apres.hauteur), "angle " + nombreCourt(d.apres.angleVertical, 0) + "° sous l'horizontale")
        }
        if (d.plafondAtteint) {
            BandeauEtat(
                "Plafond atteint : il ne monte pas plus haut, l'angle passe de " + nombreCourt(d.avant.angleVertical, 0) + "° à " + nombreCourt(d.apres.angleVertical, 0) + "°.",
                alerte = true
            )
        }
        if (d.solAtteint) BandeauEtat("Il touche presque le sol : l'angle n'est plus tout à fait gardé.", alerte = true)
        if (!d.apres.dansFaisceau) BandeauEtat("À cette place, le sujet sort du faisceau.", alerte = true)
    }

    /* L'alternative : ne pas bouger, graduer. */
    val voulu = apportVoulu
    val actuel = avant.lux
    if (voulu != null && actuel != null && actuel > 0 && action != ActionDeplacement.RECUL) {
        val part = DeplacementProjecteur.gradation(actuel, voulu)
        Ligne(
            "Ou sans bouger",
            if (part <= 1.0) "à " + nombreCourt(part * 100, 0) + " %" else "il faudrait " + nombreCourt(part * 100, 0) + " %",
            listOfNotNull(
                libelleEcart(Eclairage.diaphs(part)),
                if (part > 1.0) "plus que sa pleine puissance : il faut le rapprocher" else "en lumière, pas en position de curseur",
                if (part < 1.0 && DeplacementProjecteur.virageAuGradateur(choisi)) "tungstène : la gradation réchauffe la couleur" else null,
                if (part < 1.0 && choisi == null) "si c'est du tungstène, la gradation réchauffe la couleur" else null
            ).joinToString(" · ")
        )
    }

    LaunchedEffect(d?.element) { surApercu(d?.element) }

    Spacer(Modifier.height(6.dp))
    LigneBoutons {
        if (!avant.dansFaisceau) {
            BoutonPlat("Orienter vers le sujet", modifier = Modifier.weight(1f)) {
                surAppliquer(projecteur.copy(rotation = Sol.rotationVers(projecteur.x, projecteur.y, sujet.x, sujet.y)))
            }
        }
        BoutonPlat("Appliquer sur le plan", actif = d != null, accent = true, modifier = Modifier.weight(1f)) {
            d?.let {
                surApercu(null)
                surAppliquer(it.element)
                surFermer()
            }
        }
    }
    Text(
        "Il glisse dans l'axe où il est orienté. En reculant, il garde l'angle sous lequel il voit le sujet (il monte), jamais plus haut que le plafond. Calcul en 1/d² : pour une source douce et proche, vérifie à la cellule.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * Depuis l'onglet Lumière : choisir un projecteur d'un plan au sol, le
 * régler par le calcul, et enregistrer sa nouvelle place dans le plan.
 */
@Composable
fun CarteDeplacerSurPlan(reglages: Reglages) {
    val plans = PlansSol.plans.filter { p -> p.elements.any { it.type == TypeElement.PROJECTEUR } }
    Carte(
        titre = "Sur un plan au sol",
        sousTitre = "Choisis un projecteur d'un de tes plans : reculer, viser des lux, un diaph ou un rapport — la nouvelle place s'enregistre dans le plan."
    ) {
        if (plans.isEmpty()) {
            Text(
                "Aucun plan au sol avec un projecteur pour l'instant : crée-en un dans « Plan au sol ».",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Carte
        }
        var planId by remember { mutableStateOf(plans.first().identifiant) }
        val plan = plans.firstOrNull { it.identifiant == planId } ?: plans.first()
        val projos = plan.elements.filter { it.type == TypeElement.PROJECTEUR }
        var projoId by remember(plan.identifiant) { mutableStateOf(projos.first().identifiant) }
        val projo = projos.firstOrNull { it.identifiant == projoId } ?: projos.first()
        var fantome by remember(plan.identifiant, projo.identifiant) { mutableStateOf<ElementSol?>(null) }
        fun nomDe(e: ElementSol) = e.nom.ifBlank { MonMateriel.projecteur(e.projecteur)?.nomComplet ?: "Projecteur" }
        LigneBoutons {
            Deroulant("Plan", plans.map { it.identifiant }, plan.identifiant, { id -> plans.first { it.identifiant == id }.nom }, Modifier.weight(1f)) {
                planId = it
            }
            Deroulant("Projecteur", projos.map { it.identifiant }, projo.identifiant, { id -> nomDe(projos.first { it.identifiant == id }) }, Modifier.weight(1f)) {
                projoId = it
            }
        }
        Spacer(Modifier.height(8.dp))
        ApercuPlan(plan, projo.identifiant, fantome, reglages.lumiereMetres)
        Spacer(Modifier.height(8.dp))
        BlocDeplacerProjecteur(
            plan, projo, reglages,
            fermable = false,
            surPlafond = { PlansSol.enregistrer(plan.copy(plafond = it)) },
            surApercu = { fantome = it },
            surAppliquer = { PlansSol.enregistrer(plan.remplacer(it)) },
            surFermer = {}
        )
    }
}

/** Le plan en petit, cadré pour montrer aussi la place proposée. */
@Composable
private fun ApercuPlan(plan: PlanSol, selection: Long, fantome: ElementSol?, metres: Boolean) {
    val taillePolice = with(LocalDensity.current) { 12.sp.toPx() }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(RayonControle))
    ) {
        val cadre = if (fantome != null) plan.copy(elements = plan.elements + fantome.copy(identifiant = Long.MIN_VALUE)) else plan
        val v = VueSol.ajustee(cadre, size.width, size.height, 24f)
        drawIntoCanvas { toile ->
            DessinSol.dessiner(
                toile.nativeCanvas, plan, size.width.toInt(), size.height.toInt(), v,
                OptionsDessin(
                    selection = selection,
                    texte = taillePolice,
                    metres = metres,
                    projecteurs = { MonMateriel.projecteur(it) },
                    legende = false,
                    boussole = false,
                    fantome = fantome
                )
            )
        }
    }
}
