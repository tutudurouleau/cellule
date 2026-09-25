package fr.cellule.core

/**
 * Le carnet de tirage : de quoi refaire une épreuve des mois plus tard.
 *
 * Tout ce qui se corrige se note en diaphs — « ciel +⅓, visage −½ » — parce
 * qu'une correction en diaphs survit à un changement de temps de base : si le
 * papier ou l'agrandissement change, la base change, les corrections restent.
 * Les secondes se recalculent au moment de tirer.
 */
data class Correction(val zone: String, val diaphs: Double) {
    val brulage: Boolean get() = diaphs > 0
}

data class TirageNote(
    val identifiant: Long,
    val date: String,
    val negatif: String = "",
    val papier: String = "",
    val filtre: String = "",
    val ouverture: String = "",
    val format: String = "",
    /** Temps de base, en secondes. */
    val base: Double? = null,
    val corrections: List<Correction> = emptyList(),
    val notes: String = ""
) {
    val titre: String get() = negatif.ifBlank { "Tirage" }

    /** Le déroulé au minuteur : la base, puis chaque masquage et chaque brûlage en secondes. */
    fun deroule(): List<String> {
        val b = base ?: return emptyList()
        return listOf("Base : ${nombreSansZero(b)} s") + corrections.map { c ->
            val ecart = libelleCorrection(c.diaphs)
            if (c.brulage) "${c.zone} $ecart : brûler ${nombreSansZero(TirageDiaphs.brulage(b, c.diaphs))} s de plus"
            else "${c.zone} $ecart : masquer ${nombreSansZero(TirageDiaphs.masquage(b, -c.diaphs))} s pendant la base"
        }
    }
}

object CarnetTirage {

    /** Même règle que les autres carnets : restaurer n'écrase ni ne dédouble rien. */
    fun fusionner(existant: List<TirageNote>, importees: List<TirageNote>): List<TirageNote> {
        val connus = existant.map { it.identifiant }.toMutableSet()
        return (existant + importees.filter { connus.add(it.identifiant) }).sortedBy { it.identifiant }
    }

    /**
     * Refaire le tirage avec un autre temps de base : les corrections en
     * diaphs ne bougent pas, seules les secondes se recalculent.
     */
    fun rebaser(t: TirageNote, nouvelleBase: Double): TirageNote = t.copy(base = nouvelleBase)
}
