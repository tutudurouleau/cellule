package fr.cellule.core

import kotlin.math.abs

/**
 * Le journal d'entraînement.
 *
 * Ce qu'on cherche n'est pas la justesse moyenne mais le biais : l'erreur est
 * presque toujours systématique, et presque toujours dans les ombres. D'où le
 * découpage par type de scène, qui est la seule statistique qui serve.
 */
data class EntreeJournal(
    val identifiant: Long,
    val date: String,
    val typeScene: String,
    val annonce: Double,
    val mesure: Double,
    val note: String = ""
) {
    /** Positif : tu as cru qu'il faisait plus clair qu'il ne faisait. */
    val ecart: Double get() = annonce - mesure
}

val TYPES_DE_SCENE = listOf(
    "Plein soleil", "Ombre au soleil", "Couvert", "Sous-bois",
    "Intérieur jour", "Intérieur artificiel", "Crépuscule", "Nuit urbaine"
)

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
        if (entrees.isEmpty()) return null
        val ecarts = entrees.map { it.ecart }
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
        entrees.groupBy { it.typeScene }
            .mapNotNull { (type, liste) -> bilan(liste)?.let { type to it } }
            .sortedByDescending { abs(it.second.biais) }

    /** Le type de scène que suggère un EV, pour préremplir le journal. */
    fun typeSuggere(ev100: Double, obstacle: Obstacle): String = when {
        obstacle.id == "sousbois" -> "Sous-bois"
        ev100 >= 14.2 -> "Plein soleil"
        ev100 >= 12.5 -> "Couvert"
        ev100 >= 11.0 -> "Ombre au soleil"
        ev100 >= 6.0 -> "Intérieur jour"
        ev100 >= 2.0 -> "Crépuscule"
        else -> "Nuit urbaine"
    }
}
