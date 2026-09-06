package fr.cellule.core

import kotlin.math.abs

/**
 * Le carnet.
 *
 * Il sert deux usages qui n'en font qu'un sur le terrain : se souvenir de ce
 * qu'on a photographié — pellicule, numéro de vue, sujet, réglage retenu — et
 * mesurer son biais d'estimation quand on a pris la peine d'annoncer avant de
 * mesurer.
 *
 * D'où [annonce] et [mesure] facultatives : on note une photo sans forcément
 * faire l'exercice, et l'exercice ne compte que là où les deux chiffres sont là.
 */
data class EntreeJournal(
    val identifiant: Long,
    val date: String,
    val heure: String = "",
    val pellicule: String = "",
    val vue: String = "",
    val sujet: String = "",
    val typeScene: String = "",
    val annonce: Double? = null,
    val mesure: Double? = null,
    val reglage: String = "",
    val note: String = ""
) {
    /** Positif : tu as cru qu'il faisait plus clair qu'il ne faisait. */
    val ecart: Double?
        get() = if (annonce != null && mesure != null) annonce - mesure else null

    /** Une entrée compte pour l'entraînement dès que les deux chiffres sont là. */
    val compteAuBilan: Boolean get() = ecart != null

    val resume: String
        get() = listOfNotNull(
            vue.takeIf { it.isNotBlank() }?.let { "vue $it" },
            pellicule.takeIf { it.isNotBlank() },
            reglage.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
}

val TYPES_DE_SCENE = listOf(
    "Plein soleil", "Ombre au soleil", "Couvert", "Sous-bois",
    "Intérieur jour", "Intérieur artificiel", "Crépuscule", "Nuit urbaine"
)

/* ── Pellicules ──────────────────────────────────────────────────────────── */

enum class TypeFilm(val libelle: String) {
    NEGATIF_COULEUR("Négatif couleur"),
    NOIR_ET_BLANC("Négatif N&B"),
    INVERSIBLE("Inversible"),
    NUMERIQUE("Numérique")
}

data class Pellicule(val nom: String, val iso: Int, val type: TypeFilm)

val PELLICULES = listOf(
    Pellicule("Kodak Portra 160", 160, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Portra 400", 400, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Portra 800", 800, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Gold 200", 200, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak ColorPlus 200", 200, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Ultramax 400", 400, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Ektar 100", 100, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Fujifilm C200", 200, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Fujifilm Superia 400", 400, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Cinestill 400D", 400, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Cinestill 800T", 800, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Lomography 400", 400, TypeFilm.NEGATIF_COULEUR),
    Pellicule("Kodak Tri-X 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Kodak T-Max 100", 100, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Kodak T-Max 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Ilford HP5 Plus 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Ilford FP4 Plus 125", 125, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Ilford Delta 100", 100, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Ilford Delta 3200", 3200, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Ilford XP2 Super 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Kentmere 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Fomapan 100", 100, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Fomapan 400", 400, TypeFilm.NOIR_ET_BLANC),
    Pellicule("Fujifilm Provia 100F", 100, TypeFilm.INVERSIBLE),
    Pellicule("Fujifilm Velvia 50", 50, TypeFilm.INVERSIBLE),
    Pellicule("Fujifilm Velvia 100", 100, TypeFilm.INVERSIBLE),
    Pellicule("Kodak Ektachrome E100", 100, TypeFilm.INVERSIBLE),
    Pellicule("Numérique", 100, TypeFilm.NUMERIQUE)
)

/**
 * En cas de doute, de quel côté se tromper. L'asymétrie du négatif est réelle
 * et large ; celle de l'inversible est inverse et impitoyable.
 */
fun conseilLatitude(type: TypeFilm): String = when (type) {
    TypeFilm.NEGATIF_COULEUR ->
        "En cas de doute, surexpose : +1 à +2 diaphs donnent souvent un meilleur négatif — grain plus fin, ombres ouvertes."
    TypeFilm.NOIR_ET_BLANC ->
        "Expose pour les ombres, développe pour les hautes lumières. Le doute se règle en ouvrant."
    TypeFilm.INVERSIBLE ->
        "N'espère rien des hautes lumières : l'écrêtage est définitif. En cas de doute, ferme."
    TypeFilm.NUMERIQUE ->
        "Comme de l'inversible : protège les hautes lumières, les ombres se relèvent."
}

/* ── Bilan d'entraînement ────────────────────────────────────────────────── */

data class Bilan(
    val nombre: Int,
    val biais: Double,
    val erreurAbsolue: Double,
    val partDansDemiDiaph: Double,
    val partDansUnDiaph: Double
) {
    val verdict: String
        get() = when {
            nombre < 4 -> "trop peu d’entrées"
            biais > 0.5 -> "tu ouvres trop peu — tu sous-exposes"
            biais < -0.5 -> "tu ouvres trop — tu surexposes"
            else -> "calé"
        }
}

object Statistiques {

    fun bilan(entrees: List<EntreeJournal>): Bilan? {
        val ecarts = entrees.mapNotNull { it.ecart }
        if (ecarts.isEmpty()) return null
        val n = ecarts.size
        return Bilan(
            nombre = n,
            biais = ecarts.sum() / n,
            erreurAbsolue = ecarts.sumOf { abs(it) } / n,
            partDansDemiDiaph = ecarts.count { abs(it) <= 0.5 }.toDouble() / n,
            partDansUnDiaph = ecarts.count { abs(it) <= 1.0 }.toDouble() / n
        )
    }

    /** Là où l'œil se trompe : un bilan par type de scène, du plus biaisé au moins. */
    fun parType(entrees: List<EntreeJournal>): List<Pair<String, Bilan>> =
        entrees.filter { it.compteAuBilan && it.typeScene.isNotBlank() }
            .groupBy { it.typeScene }
            .mapNotNull { (type, liste) -> bilan(liste)?.let { type to it } }
            .sortedByDescending { abs(it.second.biais) }

    /** Le type de scène que suggère un EV, pour préremplir le carnet. */
    fun typeSuggere(ev100: Double, obstacle: Obstacle): String = when {
        obstacle.id == "sousbois" -> "Sous-bois"
        ev100 >= 14.2 -> "Plein soleil"
        ev100 >= 12.5 -> "Couvert"
        ev100 >= 11.0 -> "Ombre au soleil"
        ev100 >= 6.0 -> "Intérieur jour"
        ev100 >= 2.0 -> "Crépuscule"
        else -> "Nuit urbaine"
    }

    /**
     * Le numéro de vue suivant sur la même pellicule. Un carnet de prise de vue
     * sans numéro de vue ne sert à rien : c'est lui qui raccroche la note au
     * négatif une fois la planche-contact sortie.
     */
    fun vueSuivante(entrees: List<EntreeJournal>, pellicule: String): String {
        val dernier = entrees
            .filter { it.pellicule == pellicule }
            .mapNotNull { it.vue.trim().takeWhile { c -> c.isDigit() }.toIntOrNull() }
            .maxOrNull()
        return ((dernier ?: 0) + 1).toString()
    }
}
