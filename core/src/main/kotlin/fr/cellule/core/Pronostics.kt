package fr.cellule.core

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Deviner avant de calculer.
 *
 * Chaque calculateur du Labo demande d'abord ton estimation, puis montre le
 * calcul à côté. Le but n'est pas la note : c'est de voir, pronostic après
 * pronostic, l'intuition rejoindre le calcul — jusqu'au jour où l'appli ne
 * sert plus qu'à vérifier.
 *
 * L'écart ne se mesure pas partout de la même façon : en pourcentage pour un
 * temps de développement ou un volume (une minute d'erreur sur 4 n'est pas la
 * même faute que sur 20), en diaphs pour une exposition de tirage — l'unité
 * dans laquelle le papier répond.
 */
enum class Echelle { RELATIVE, DIAPHS }

enum class Calculateur(val libelle: String, val echelle: Echelle) {
    TEMPERATURE("Temps et température", Echelle.RELATIVE),
    PUSH_PULL("Push et pull", Echelle.RELATIVE),
    RECIPROCITE("Réciprocité", Echelle.DIAPHS),
    DILUTION("Dilution", Echelle.RELATIVE),
    TIRAGE("Tirage en diaphs", Echelle.DIAPHS)
}

data class Pronostic(
    val calculateur: Calculateur,
    val date: String,
    val estime: Double,
    val calcule: Double,
    val contexte: String = ""
) {
    /** Positif : tu as vu trop grand. En fraction (0,1 = 10 %) ou en diaphs. */
    val ecart: Double
        get() = when (calculateur.echelle) {
            Echelle.RELATIVE -> (estime - calcule) / calcule
            Echelle.DIAPHS -> log2(estime / calcule)
        }

    val verdict: Verdict get() = Verdict.de(ecart, calculateur.echelle)

    /** « +12 % » ou « −⅓ diaph ». */
    val libelleEcart: String
        get() = when (calculateur.echelle) {
            Echelle.RELATIVE -> {
                val p = (ecart * 100).roundToInt()
                if (p == 0) "juste" else (if (p > 0) "+" else "−") + abs(p) + " %"
            }
            Echelle.DIAPHS -> libelleDiaphs(ecart).let { if (it == "juste") it else "$it diaph" }
        }
}

/**
 * Juste : l'écart ne se verrait pas sur le négatif ou le tirage. Proche : il
 * se verrait, mais se rattrape (Kodak conseille de corriger de 10 à 15 % un
 * temps de développement jugé trop contrasté ou trop doux, fiche J-78). Loin :
 * il faut revoir le raisonnement, pas seulement le chiffre.
 */
enum class Verdict(val libelle: String) {
    JUSTE("juste"), PROCHE("proche"), LOIN("loin");

    companion object {
        fun de(ecart: Double, echelle: Echelle): Verdict {
            val (juste, proche) = when (echelle) {
                Echelle.RELATIVE -> 0.05 to 0.15
                Echelle.DIAPHS -> 1.0 / 6 to 0.5
            }
            return when {
                abs(ecart) <= juste -> JUSTE
                abs(ecart) <= proche -> PROCHE
                else -> LOIN
            }
        }
    }
}

/**
 * Où en est l'intuition : l'erreur moyenne des premiers pronostics comparée à
 * celle des derniers. Une erreur qui fond, c'est la compétence qui s'installe.
 */
data class Progression(
    val nombre: Int,
    val erreurDebut: Double,
    val erreurRecente: Double,
    val partJustes: Double
) {
    val progresse: Boolean get() = nombre >= 2 * Progressions.FENETRE && erreurRecente < erreurDebut
}

object Progressions {

    const val FENETRE = 5

    /** [pronostics] dans l'ordre chronologique, pour un seul calculateur. */
    fun progression(pronostics: List<Pronostic>): Progression? {
        if (pronostics.isEmpty()) return null
        val erreurs = pronostics.map { abs(it.ecart) }
        return Progression(
            nombre = pronostics.size,
            erreurDebut = erreurs.take(FENETRE).average(),
            erreurRecente = erreurs.takeLast(FENETRE).average(),
            partJustes = pronostics.count { it.verdict == Verdict.JUSTE }.toDouble() / pronostics.size
        )
    }
}

/**
 * Les durées du labo s'écrivent en minutes et secondes : « 9:30 », « 9 min
 * 30 », « 9,5 » (minutes décimales) ou « 45 s ».
 */
object Duree {

    /** En secondes, ou null si le texte n'est pas une durée. */
    fun lire(texte: String): Double? {
        val t = texte.trim().lowercase().replace(',', '.')
        if (t.isEmpty()) return null
        Regex("""^(\d+)\s*(?::|min|mn|')\s*(\d{1,2})?\s*(?:s|")?$""").find(t)?.let { m ->
            return m.groupValues[1].toDouble() * 60 + (m.groupValues[2].toDoubleOrNull() ?: 0.0)
        }
        Regex("""^(\d+(?:\.\d+)?)\s*(?:s|sec|secondes?)$""").find(t)?.let { return it.groupValues[1].toDouble() }
        return t.toDoubleOrNull()?.let { it * 60 }
    }

    /** « 9 min 30 », « 45 s », « 12 min ». */
    fun libelle(secondes: Double): String {
        val total = secondes.roundToInt()
        val min = total / 60
        val s = total % 60
        return when {
            min == 0 -> "$s s"
            s == 0 -> "$min min"
            else -> "$min min ${s.toString().padStart(2, '0')}"
        }
    }

    /** « 9:30 » : la forme courte du minuteur. */
    fun chrono(secondes: Double): String {
        val total = secondes.roundToInt().coerceAtLeast(0)
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }
}
