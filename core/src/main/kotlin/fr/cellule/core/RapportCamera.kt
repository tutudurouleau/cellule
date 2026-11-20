package fr.cellule.core

/*
 * Le rapport caméra : une ligne par prise, ce qu'on y a mis (objectif, T,
 * filtres, EI, cadence, obturateur) et si elle est bonne. Il part le soir à
 * la production et au labo.
 */

enum class VerdictPrise(val libelle: String, val court: String) {
    A_VOIR("À voir", "—"),
    BONNE("Bonne", "Bonne"),
    A_REFAIRE("À refaire", "NG")
}

data class PriseCamera(
    val identifiant: Long,
    val jourId: Long,
    val sequence: String = "",
    val plan: String = "",
    val prise: Int = 1,
    val camera: String = "A",
    val objectif: String = "",
    val tStop: String = "",
    val filtres: String = "",
    val ei: String = "",
    val cadence: String = "",
    val obturateur: String = "",
    /** La carte ou le magasin. */
    val carte: String = "",
    val heure: String = "",
    val verdict: VerdictPrise = VerdictPrise.A_VOIR,
    val remarques: String = ""
) {
    /** « Séq. 12 · Pl. 3 · Pr. 2 ». */
    val reference: String
        get() = listOfNotNull(
            sequence.trim().takeIf { it.isNotEmpty() }?.let { "Séq. $it" },
            plan.trim().takeIf { it.isNotEmpty() }?.let { "Pl. $it" },
            "Pr. $prise"
        ).joinToString(" · ")

    /** Ce qu'on a mis sur la caméra, en une ligne. */
    val reglage: String
        get() = listOfNotNull(
            objectif.trim().takeIf { it.isNotEmpty() },
            tStop.trim().takeIf { it.isNotEmpty() }?.let { if (it.startsWith("T", ignoreCase = true)) it else "T$it" },
            filtres.trim().takeIf { it.isNotEmpty() },
            ei.trim().takeIf { it.isNotEmpty() }?.let { "EI $it" },
            cadence.trim().takeIf { it.isNotEmpty() }?.let { "$it im/s" },
            obturateur.trim().takeIf { it.isNotEmpty() }?.let { "$it°" }
        ).joinToString(" · ")
}

object RapportCamera {

    fun duJour(prises: List<PriseCamera>, jourId: Long): List<PriseCamera> =
        prises.filter { it.jourId == jourId }.sortedBy { it.identifiant }

    /** La prise suivante : les mêmes réglages, le numéro de prise suivant. */
    fun priseSuivante(derniere: PriseCamera, identifiant: Long, heure: String = ""): PriseCamera =
        derniere.copy(identifiant = identifiant, prise = derniere.prise + 1, verdict = VerdictPrise.A_VOIR, remarques = "", heure = heure)

    /** Un nouveau plan : les réglages gardés, le plan suivant (12 → 13, 3A → 3B), prise 1. */
    fun planSuivant(derniere: PriseCamera, identifiant: Long, heure: String = ""): PriseCamera =
        derniere.copy(identifiant = identifiant, plan = suivant(derniere.plan), prise = 1, verdict = VerdictPrise.A_VOIR, remarques = "", heure = heure)

    /** Le numéro qui suit : la lettre suivante (3A → 3B), sinon le dernier nombre augmenté (12 → 13). */
    fun suivant(texte: String): String {
        val t = texte.trim()
        if (t.isEmpty()) return "1"
        val lettre = Regex("^(.*\\d)([A-Ya-y])$").find(t)
        if (lettre != null) return lettre.groupValues[1] + (lettre.groupValues[2][0] + 1)
        /* « 12 bis » est suivi de 13 : le suffixe ne se reporte pas. */
        val nombre = Regex("^(.*?)(\\d+)\\D*$").find(t) ?: return t + " 2"
        val (avant, chiffres) = nombre.destructured
        return avant + (chiffres.toLong() + 1)
    }

    /** « 12 prises · 5 bonnes · 2 à refaire ». */
    fun bilan(prises: List<PriseCamera>): String {
        if (prises.isEmpty()) return "Aucune prise"
        val bonnes = prises.count { it.verdict == VerdictPrise.BONNE }
        val ng = prises.count { it.verdict == VerdictPrise.A_REFAIRE }
        return listOfNotNull(
            "${prises.size} prise" + if (prises.size > 1) "s" else "",
            if (bonnes > 0) "$bonnes bonne" + if (bonnes > 1) "s" else "" else null,
            if (ng > 0) "$ng à refaire" else null
        ).joinToString(" · ")
    }
}
