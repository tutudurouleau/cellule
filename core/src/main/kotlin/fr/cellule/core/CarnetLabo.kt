package fr.cellule.core

import kotlin.math.abs

/**
 * Le carnet de développement : une ligne par film développé.
 *
 * Il garde trois temps côte à côte — ce que tu avais estimé avant de
 * calculer, ce que le calcul donnait, ce que tu as réellement donné — puis ce
 * que le négatif a montré. Relu au fil des films, il dit deux choses : si ton
 * intuition rejoint le calcul, et si ta chaîne à toi (ta cuve, ton eau, ton
 * agitation) demande de s'écarter des fiches.
 */
enum class Resultat(val libelle: String) {
    CORRECT("Correct"),
    DENSE("Trop dense"),
    CLAIR("Trop clair"),
    DUR("Trop contrasté"),
    DOUX("Trop doux"),
    GRAIN("Grain gênant"),
    DEFAUT("Défaut : taches, voile, bulles…")
}

data class DeveloppementNote(
    val identifiant: Long,
    val date: String,
    val film: String = "",
    val ei: Int? = null,
    val revelateur: String = "",
    val dilution: String = "",
    val temperature: Double? = null,
    /** En secondes, comme tous les temps de ce carnet. */
    val tempsCalcule: Double? = null,
    val estimation: Double? = null,
    val tempsDonne: Double? = null,
    val cuve: String = "",
    val agitation: String = "",
    val resultat: Resultat? = null,
    val notes: String = ""
) {
    /** Positif : tu avais vu plus long que le calcul. En fraction (0,1 = 10 %). */
    val ecartEstimation: Double?
        get() = if (estimation != null && tempsCalcule != null && tempsCalcule > 0)
            (estimation - tempsCalcule) / tempsCalcule else null

    /** Ce que tu as donné par rapport au calcul — ta correction à toi. */
    val ecartDonne: Double?
        get() = if (tempsDonne != null && tempsCalcule != null && tempsCalcule > 0)
            (tempsDonne - tempsCalcule) / tempsCalcule else null

    val titre: String
        get() = listOfNotNull(
            film.ifBlank { null }?.let { if (ei != null) "$it à EI $ei" else it },
            listOf(revelateur, dilution).filter { it.isNotBlank() }.joinToString(" ").ifBlank { null }
        ).joinToString(" · ").ifBlank { "Développement" }
}

data class BilanDeveloppements(
    val nombre: Int,
    val resultats: Map<Resultat, Int>,
    /** L'intuition, comme pour les calculateurs : erreur moyenne des premiers et des derniers. */
    val erreurDebut: Double?,
    val erreurRecente: Double?,
    val nombreEstimes: Int
)

object CarnetLabo {

    fun bilan(notes: List<DeveloppementNote>): BilanDeveloppements? {
        if (notes.isEmpty()) return null
        val ecarts = notes.mapNotNull { it.ecartEstimation }.map { abs(it) }
        val fenetre = Progressions.FENETRE
        return BilanDeveloppements(
            nombre = notes.size,
            resultats = notes.mapNotNull { it.resultat }.groupingBy { it }.eachCount(),
            erreurDebut = ecarts.take(fenetre).takeIf { it.isNotEmpty() }?.average(),
            erreurRecente = ecarts.takeLast(fenetre).takeIf { it.isNotEmpty() }?.average(),
            nombreEstimes = ecarts.size
        )
    }

    /**
     * Ce que disent les fiches quand le négatif déçoit. Le contraste se règle
     * au développement ; la densité des ombres, à l'exposition.
     */
    fun conseil(r: Resultat): Pair<String, Source?>? = when (r) {
        Resultat.CORRECT -> null
        Resultat.DUR -> "Kodak : des négatifs régulièrement trop contrastés demandent de raccourcir un peu le développement, de 10 à 15 %." to Sources.KODAK_D76
        Resultat.DOUX -> "Kodak : des négatifs régulièrement trop doux demandent d'allonger un peu le développement, de 10 à 15 %." to Sources.KODAK_D76
        Resultat.DENSE, Resultat.CLAIR ->
            "Regarde d'abord les ombres : vides, c'est l'exposition ; détaillées mais hautes lumières bouchées ou éteintes, c'est le développement. On expose pour les ombres, on développe pour les hautes lumières." to null
        Resultat.GRAIN -> "Pousser augmente le grain (Kodak). Pour le grain le plus fin, Ilford conseille ILFOTEC DD-X ou PERCEPTOL avec la HP5 Plus." to Sources.ILFORD_HP5
        Resultat.DEFAUT -> "Kodak : des bulles d'air laissent des cercles moins denses — tape la cuve après l'avoir remplie. Ilford (fiche HP5 Plus) : tous les bains à 5 °C au plus du révélateur." to Sources.KODAK_D76
    }

    /** Ajoute ce qui manque, sans écraser ni dédoubler (même logique que le carnet de vues). */
    fun fusionner(existant: List<DeveloppementNote>, importees: List<DeveloppementNote>): List<DeveloppementNote> {
        val connus = existant.map { it.identifiant }.toMutableSet()
        return (existant + importees.filter { connus.add(it.identifiant) }).sortedBy { it.identifiant }
    }
}
