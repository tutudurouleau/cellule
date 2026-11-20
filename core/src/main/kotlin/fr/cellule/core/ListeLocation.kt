package fr.cellule.core

import kotlin.math.sqrt

/*
 * Du plan au sol à la liste de location et au bilan électrique : ce qu'il
 * faut emporter (projecteurs par modèle, cadres, drapeaux, rails, caméras)
 * et ce que chaque circuit va porter.
 */

data class LigneLocation(
    val categorie: String,
    val nom: String,
    val quantite: Double,
    val unite: String = "",
    /** Ajoutée d'office (pied, sac, ballast, prolong) : la personne ne l'a pas posée sur le plan. */
    val auto: Boolean = false
) {
    /** « × 3 », ou « 6 m » pour une longueur. */
    val combien: String get() = if (unite.isNotBlank()) nombreCourt(quantite, 2) + " " + unite else "× " + nombreCourt(quantite, 0)
}

data class ChargeCircuit(
    val circuit: String,
    val puissanceW: Double,
    val sources: List<String>,
    /** Les projecteurs dont la puissance n'est pas connue : la charge est sous-estimée. */
    val sansPuissance: Int
)

object ListeLocation {

    const val SANS_CIRCUIT = "Sans circuit"

    val ORDRE = listOf("Caméra", "Projecteurs", "Accessoires", "Machinerie", "Électrique", "Consommables")

    /** Ce qu'il faut pour un plan au sol. [nomProjecteur] donne le modèle d'un projecteur. */
    fun duPlan(plan: PlanSol, nomProjecteur: (ElementSol) -> String): List<LigneLocation> {
        val lignes = mutableListOf<LigneLocation>()
        /* Une caméra qui change de position garde son nom : on la compte une fois. */
        val cameras = plan.elements.filter { it.type == TypeElement.CAMERA }.map { it.nom.trim().ifBlank { "#" + it.identifiant } }.distinct()
        if (cameras.isNotEmpty()) lignes += LigneLocation("Caméra", "Caméras", cameras.size.toDouble())
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.groupBy(nomProjecteur).toSortedMap().forEach { (nom, l) ->
            lignes += LigneLocation("Projecteurs", nom, l.size.toDouble())
        }
        listOf(TypeElement.REFLECTEUR, TypeElement.DIFFUSION, TypeElement.DRAPEAU).forEach { t ->
            plan.elements.filter { it.type == t }.groupBy { nombreCourt(it.largeur, 2) }.toSortedMap().forEach { (taille, l) ->
                lignes += LigneLocation("Accessoires", "${t.libelle} de $taille m", l.size.toDouble())
            }
        }
        val rails = plan.elements.filter { it.type == TypeElement.RAIL }
        if (rails.isNotEmpty()) lignes += LigneLocation("Machinerie", "Rail de travelling", rails.sumOf { it.largeur }, "m")
        return lignes
    }

    /** Pour un jour : chaque ligne au plus haut de ses plans, puisque le matériel passe d'un plan à l'autre. */
    fun duJour(plans: List<PlanSol>, nomProjecteur: (ElementSol) -> String): List<LigneLocation> =
        plans.flatMap { duPlan(it, nomProjecteur) }
            .groupBy { it.categorie to it.nom }
            .map { (_, l) -> l.maxByOrNull { it.quantite }!! }
            .sortedWith(compareBy({ ORDRE.indexOf(it.categorie) }, { it.nom }))

    /** La charge de chaque circuit, d'après la lettre saisie sur chaque projecteur. */
    fun charges(plan: PlanSol, puissance: (ElementSol) -> Double?, nomProjecteur: (ElementSol) -> String): List<ChargeCircuit> =
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }
            .groupBy { it.circuit.trim().uppercase().ifBlank { SANS_CIRCUIT } }
            .toList()
            .sortedBy { (c, _) -> if (c == SANS_CIRCUIT) "￿" else c }
            .map { (c, l) -> ChargeCircuit(c, l.sumOf { puissance(it) ?: 0.0 }, l.map(nomProjecteur), l.count { puissance(it) == null }) }

    /** Le courant appelé : P / U, ou P / (√3 × U) en triphasé (cos φ = 1). */
    fun courant(puissanceW: Double, circuit: Circuit): Double =
        if (circuit.triphase) puissanceW / (sqrt(3.0) * circuit.tension) else puissanceW / circuit.tension

    /** La part de la charge admise (80 % du calibre) qu'utilise cette puissance : 1 = la limite. */
    fun partDeLaMarge(puissanceW: Double, circuit: Circuit): Double = puissanceW / (0.8 * circuit.puissance)

    /** Le groupe électrogène minimal, en kVA : kW / 0,8, puis 80 % de charge au plus. */
    fun groupeKva(totalW: Double): Double = totalW / 1000 / 0.8 / 0.8
}
