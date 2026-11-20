package fr.cellule.core

import kotlin.math.ceil
import kotlin.math.hypot

/*
 * Du plan au sol à la liste complète : ce que la personne a posé (ListeLocation)
 * plus ce que chaque élément entraîne d'office — un pied et un sac pour un
 * projecteur, un ballast et un câble de tête pour un HMI, des prolongs à la
 * bonne longueur jusqu'au point d'énergie — et la répartition sur les phases.
 *
 * Ce sont des règles de préparation, pas des chiffres de fabricant : la liste
 * les marque « ajouté d'office », et chacun les ajuste.
 */

/** Un circuit calculé : un projecteur, son calibre, sa longueur de câble. */
data class Tirage(val projecteur: String, val calibreA: Int, val longueurM: Double)

data class Cablage(
    val tirages: List<Tirage>,
    /** Les prolongs à demander : (libellé, quantité). */
    val prolongs: List<LigneLocation>,
    /** Faux quand le plan n'a pas de point d'énergie : on ne peut pas mesurer. */
    val mesure: Boolean
)

data class Phases(val charges: List<Double>, val repartition: List<List<String>>) {
    val ecartW: Double get() = (charges.maxOrNull() ?: 0.0) - (charges.minOrNull() ?: 0.0)
}

object ListeTournage {

    /** Les longueurs de prolong qu'on trouve chez les loueurs, de la plus longue à la plus courte. */
    val LONGUEURS = listOf(50.0, 25.0, 10.0, 5.0)

    /** Le détour d'un câble qui longe murs et passages, plutôt que la ligne droite. */
    const val DETOUR = 1.3

    /** La marge au pied du projecteur et au départ : on ne tend jamais un câble. */
    const val MARGE_M = 3.0

    /** Le calibre d'une ligne monophasée qui porte cette puissance en 230 V. */
    fun calibre(puissanceW: Double): Int {
        val i = puissanceW / 230.0
        return when {
            i <= 16 -> 16
            i <= 32 -> 32
            else -> 63
        }
    }

    /** Le moins de prolongs standard possible qui couvrent [m] mètres, puis le moins de longueur. */
    fun decouper(m: Double): List<Double> {
        var meilleur: List<Double>? = null
        val max50 = (m / 50).toInt() + 1
        for (a in 0..max50) for (b in 0..2) for (c in 0..2) for (d in 0..1) {
            val total = a * 50.0 + b * 25.0 + c * 10.0 + d * 5.0
            if (total < m || total == 0.0) continue
            val liste = List(a) { 50.0 } + List(b) { 25.0 } + List(c) { 10.0 } + List(d) { 5.0 }
            val m0 = meilleur
            if (m0 == null || liste.size < m0.size || liste.size == m0.size && total < m0.sum()) meilleur = liste
        }
        return meilleur ?: listOf(LONGUEURS.last())
    }

    /** Ce qu'un projecteur entraîne : pied, sac, ballast, câble de tête. */
    fun accessoiresProjecteur(modele: Projecteur?, puissanceW: Double?): List<Pair<String, String>> {
        val w = puissanceW ?: 0.0
        val famille = modele?.famille
        val l = mutableListOf<Pair<String, String>>()
        val pied = when {
            famille == FamilleProjecteur.TUBE_LED -> "C-stand et pince de tube"
            famille == FamilleProjecteur.AMBIANCE -> "Accroche ou perche (ballon, spacelight)"
            famille == FamilleProjecteur.HMI && w >= 4000 || w >= 5000 -> "Wind-up (pied à manivelle)"
            w >= 1500 || famille == FamilleProjecteur.HMI -> "Pied junior"
            else -> "Pied baby"
        }
        l += "Machinerie" to pied
        if (famille != FamilleProjecteur.AMBIANCE) l += "Machinerie" to "Sac de lest"
        if (famille == FamilleProjecteur.HMI) {
            l += "Électrique" to "Ballast HMI"
            l += "Électrique" to "Câble de tête HMI"
        }
        return l
    }

    /**
     * Tout ce qu'il faut pour un plan : la liste posée, plus les accessoires
     * d'office. [modele] et [puissance] décrivent chaque projecteur.
     */
    fun complete(
        plan: PlanSol,
        nomProjecteur: (ElementSol) -> String,
        modele: (ElementSol) -> Projecteur?,
        puissance: (ElementSol) -> Double?
    ): List<LigneLocation> {
        val lignes = ListeLocation.duPlan(plan, nomProjecteur).toMutableList()
        val auto = mutableMapOf<Pair<String, String>, Double>()
        fun ajouter(categorie: String, nom: String, n: Double = 1.0) {
            auto[categorie to nom] = (auto[categorie to nom] ?: 0.0) + n
        }
        plan.elements.forEach { e ->
            when (e.type) {
                TypeElement.PROJECTEUR -> {
                    accessoiresProjecteur(modele(e), puissance(e)).forEach { (c, n) -> ajouter(c, n) }
                    e.gelatine.split("+").map { it.trim() }.filter { it.isNotBlank() }.forEach { ajouter("Consommables", "Gélatine $it (feuille)") }
                }
                TypeElement.DIFFUSION, TypeElement.REFLECTEUR -> {
                    if (e.largeur >= 1.8) {
                        ajouter("Machinerie", "Pied combo ou wind-up", 2.0); ajouter("Machinerie", "Sac de lest", 2.0)
                    } else {
                        ajouter("Machinerie", "C-stand", 1.0); ajouter("Machinerie", "Sac de lest", 1.0)
                    }
                }
                TypeElement.DRAPEAU -> { ajouter("Machinerie", "C-stand", 1.0); ajouter("Machinerie", "Sac de lest", 1.0) }
                TypeElement.CAMERA -> {}
                else -> {}
            }
        }
        val cameras = plan.elements.filter { it.type == TypeElement.CAMERA }.map { it.nom.trim().ifBlank { "#" + it.identifiant } }.distinct().size
        if (cameras > 0) {
            ajouter("Caméra", "Trépied et tête", cameras.toDouble())
            ajouter("Caméra", "Jeu de batteries et chargeur", cameras.toDouble())
            ajouter("Caméra", "Cartes ou magasins (jeu)", cameras.toDouble())
            ajouter("Caméra", "Moniteur de retour", 1.0)
        }
        if (plan.elements.any { it.type == TypeElement.RAIL }) ajouter("Machinerie", "Dolly et jeu de cales", 1.0)
        val projecteurs = plan.elements.count { it.type == TypeElement.PROJECTEUR }
        if (projecteurs > 0) {
            ajouter("Consommables", "Gaffer (rouleau)", maxOf(1.0, ceil(projecteurs / 6.0)))
            ajouter("Consommables", "Pinces à linge C-47 (sachet)", 1.0)
            ajouter("Machinerie", "Pinces à ressort (jeu)", 1.0)
        }
        cablage(plan, nomProjecteur, puissance).prolongs.forEach { auto[it.categorie to it.nom] = (auto[it.categorie to it.nom] ?: 0.0) + it.quantite }
        auto.forEach { (cle, n) -> lignes += LigneLocation(cle.first, cle.second, n, auto = true) }
        return lignes.sortedWith(compareBy({ ListeLocation.ORDRE.indexOf(it.categorie).let { i -> if (i < 0) 99 else i } }, { it.auto }, { it.nom }))
    }

    /** Pour un jour : chaque ligne au plus haut de ses plans. */
    fun duJour(
        plans: List<PlanSol>,
        nomProjecteur: (ElementSol) -> String,
        modele: (ElementSol) -> Projecteur?,
        puissance: (ElementSol) -> Double?
    ): List<LigneLocation> =
        plans.flatMap { complete(it, nomProjecteur, modele, puissance) }
            .groupBy { Triple(it.categorie, it.nom, it.auto) }
            .map { (_, l) -> l.maxByOrNull { it.quantite }!! }
            .sortedWith(compareBy({ ListeLocation.ORDRE.indexOf(it.categorie).let { i -> if (i < 0) 99 else i } }, { it.auto }, { it.nom }))

    /**
     * Les prolongs, depuis le point d'énergie le plus proche de chaque
     * projecteur : la distance au sol, plus le détour, plus la hauteur du
     * projecteur, plus une marge, découpée en longueurs standard.
     */
    fun cablage(plan: PlanSol, nomProjecteur: (ElementSol) -> String, puissance: (ElementSol) -> Double?): Cablage {
        val sources = plan.elements.filter { it.type == TypeElement.ENERGIE }
        val projos = plan.elements.filter { it.type == TypeElement.PROJECTEUR }
        if (sources.isEmpty() || projos.isEmpty()) return Cablage(emptyList(), emptyList(), sources.isNotEmpty())
        val tirages = projos.map { p ->
            val d = sources.minOf { hypot(it.x - p.x, it.y - p.y) }
            val longueur = d * DETOUR + (p.hauteur ?: 2.0) + MARGE_M
            Tirage(nomProjecteur(p), calibre(puissance(p) ?: 0.0), longueur)
        }
        val compte = mutableMapOf<Pair<Int, Double>, Int>()
        tirages.forEach { t -> decouper(t.longueurM).forEach { l -> compte[t.calibreA to l] = (compte[t.calibreA to l] ?: 0) + 1 } }
        val prolongs = compte.entries.sortedWith(compareBy({ it.key.first }, { -it.key.second })).map { (k, n) ->
            LigneLocation("Électrique", "Prolong ${k.first} A mono de ${nombreCourt(k.second, 0)} m", n.toDouble(), auto = true)
        }
        return Cablage(tirages, prolongs, true)
    }

    /**
     * La répartition sur les trois phases d'un groupe ou d'une armoire : le
     * plus gros projecteur d'abord, sur la phase la moins chargée.
     */
    fun phases(elements: List<ElementSol>, nomProjecteur: (ElementSol) -> String, puissance: (ElementSol) -> Double?): Phases {
        val charges = DoubleArray(3)
        val noms = List(3) { mutableListOf<String>() }
        elements.filter { it.type == TypeElement.PROJECTEUR }
            .sortedByDescending { puissance(it) ?: 0.0 }
            .forEach { e ->
                val i = charges.indices.minByOrNull { charges[it] }!!
                charges[i] += puissance(e) ?: 0.0
                noms[i] += nomProjecteur(e)
            }
        return Phases(charges.toList(), noms)
    }
}
