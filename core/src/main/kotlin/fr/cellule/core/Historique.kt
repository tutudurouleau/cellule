package fr.cellule.core

/**
 * Annuler et rétablir, pour n'importe quel état immuable (un plan au sol…).
 *
 * L'état affiché vit à côté ; l'historique ne garde que des points d'arrêt :
 * on « valide » quand le doigt se lève ou qu'une saisie se pose, jamais à
 * chaque image d'un glissé. Annuler revient d'abord au dernier point validé
 * si l'on a changé quelque chose depuis, puis aux précédents.
 */
data class Historique<T>(
    /** Le dernier point validé. */
    val stable: T,
    val passe: List<T> = emptyList(),
    val futur: List<T> = emptyList(),
    val max: Int = 50
) {
    /** Fige l'état courant s'il a changé depuis le dernier point. */
    fun valider(courant: T): Historique<T> =
        if (courant == stable) this else copy(stable = courant, passe = (passe + stable).takeLast(max), futur = emptyList())

    fun peutAnnuler(courant: T): Boolean = courant != stable || passe.isNotEmpty()

    val peutRefaire: Boolean get() = futur.isNotEmpty()

    /** Rend le nouvel historique et l'état à afficher. */
    fun annuler(courant: T): Pair<Historique<T>, T> = when {
        courant != stable -> copy(futur = futur + courant) to stable
        passe.isNotEmpty() -> copy(stable = passe.last(), passe = passe.dropLast(1), futur = futur + stable) to passe.last()
        else -> this to courant
    }

    fun refaire(courant: T): Pair<Historique<T>, T> {
        val h = valider(courant)
        if (h.futur.isEmpty()) return h to courant
        val suivant = h.futur.last()
        return h.copy(stable = suivant, passe = (h.passe + h.stable).takeLast(max), futur = h.futur.dropLast(1)) to suivant
    }
}
