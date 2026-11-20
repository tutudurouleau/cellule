package fr.cellule.core

import kotlin.math.abs
import kotlin.math.log2

/*
 * Le raccord lumière d'un plan : de quoi le refaire à l'identique le
 * lendemain, ou trois semaines plus tard. Le ciel et l'heure, les mesures
 * aux endroits qui comptent, les sources et leurs réglages. Le jour du
 * raccord, on remesure : l'écart s'affiche en diaphs.
 */

data class MesureRaccord(val endroit: String, val lux: Double? = null, val note: String = "")

data class SourceRaccord(val nom: String, val reglage: String = "")

data class RaccordLumiere(
    val planId: Long,
    val heure: String = "",
    val ciel: String = "",
    val mesures: List<MesureRaccord> = emptyList(),
    val sources: List<SourceRaccord> = emptyList(),
    val planSolId: Long? = null,
    val notes: String = ""
)

object Raccord {

    val CIELS = listOf("Intérieur", "Soleil", "Voilé", "Couvert", "Nuit")
    val ENDROITS = listOf("Face", "Débouchage", "Contre", "Fond")

    fun nouveau(planId: Long, heure: String = ""): RaccordLumiere =
        RaccordLumiere(planId, heure = heure, mesures = ENDROITS.map { MesureRaccord(it) })

    /** L'écart entre la mesure du jour et celle du raccord, en diaphs : positif, il y a plus de lumière aujourd'hui. */
    fun ecart(reference: Double, aujourdHui: Double): Double = log2(aujourdHui / reference)

    /** Ce que dit l'écart, à un sixième de diaph près. */
    fun conseil(ecart: Double): String = when {
        abs(ecart) < 1.0 / 6 -> "Raccord : c'est juste"
        ecart > 0 -> "+" + nombreCourt(ecart, 1) + " diaph : trop de lumière, baisse ou recule la source"
        else -> "−" + nombreCourt(-ecart, 1) + " diaph : il en manque, monte ou rapproche la source"
    }

    /** Le rapport face / débouchage, quand les deux mesures sont là. */
    fun rapport(r: RaccordLumiere): Double? {
        val face = r.mesures.firstOrNull { it.endroit.equals("Face", ignoreCase = true) }?.lux
        val deb = r.mesures.firstOrNull { it.endroit.equals("Débouchage", ignoreCase = true) }?.lux
        return if (face != null && deb != null && deb > 0) face / deb else null
    }

    fun resume(r: RaccordLumiere): String {
        val mesurees = r.mesures.count { it.lux != null }
        return listOfNotNull(
            r.heure.takeIf { it.isNotBlank() },
            r.ciel.takeIf { it.isNotBlank() },
            if (mesurees > 0) "$mesurees mesure" + (if (mesurees > 1) "s" else "") else null,
            if (r.sources.isNotEmpty()) "${r.sources.size} source" + (if (r.sources.size > 1) "s" else "") else null
        ).joinToString(" · ").ifBlank { "Rien de noté pour l'instant" }
    }

    /** Les projecteurs d'un plan au sol, repris comme sources du raccord. */
    fun sourcesDuPlan(plan: PlanSol, nomProjecteur: (ElementSol) -> String): List<SourceRaccord> =
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.map { e ->
            val modele = nomProjecteur(e)
            SourceRaccord(
                e.nom.trim().ifBlank { modele },
                listOfNotNull(
                    modele.takeIf { e.nom.isNotBlank() },
                    e.hauteur?.let { "à " + nombreCourt(it, 2) + " m de haut" },
                    if (e.pertesDiaphs > 0) "gélatine ou diffusion : −" + nombreCourt(e.pertesDiaphs, 1) + " diaph" else null,
                    e.circuit.trim().takeIf { it.isNotEmpty() }?.let { "circuit $it" }
                ).joinToString(", ")
            )
        }
}
