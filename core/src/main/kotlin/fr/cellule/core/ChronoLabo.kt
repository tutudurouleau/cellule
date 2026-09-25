package fr.cellule.core

/**
 * Le chrono labo : une suite de bains, chacun avec sa durée et son rythme
 * d'agitation. Tout se déduit du temps écoulé depuis le départ : l'écran et
 * le service qui bipe écran éteint lisent la même chronologie, sans jamais
 * compter chacun de leur côté.
 */
data class Agitation(
    /** Agitation au départ du bain, en secondes. */
    val initiale: Double,
    /** Durée de chaque reprise. */
    val duree: Double,
    /** Une reprise toutes les [intervalle] secondes, comptées depuis le début du bain. */
    val intervalle: Double,
    val enContinu: Boolean = false
) {
    /** Les créneaux où l'on agite, en secondes depuis le début d'un bain de [longueur] secondes. */
    fun creneaux(longueur: Double): List<ClosedFloatingPointRange<Double>> {
        if (enContinu) return listOf(0.0..longueur)
        val liste = mutableListOf<ClosedFloatingPointRange<Double>>()
        if (initiale > 0) liste += 0.0..minOf(initiale, longueur)
        if (intervalle > 0 && duree > 0) {
            var t = intervalle
            while (t < longueur - 1e-9) {
                liste += t..minOf(t + duree, longueur)
                t += intervalle
            }
        }
        return liste
    }

    val libelle: String
        get() = when {
            enContinu -> "en continu"
            intervalle <= 0 -> "${nombreSansZero(initiale)} s au départ"
            else -> "${nombreSansZero(initiale)} s au départ, puis ${nombreSansZero(duree)} s toutes les ${nombreSansZero(intervalle)} s"
        }
}

object Agitations {
    /**
     * Ilford (fiche HP5 Plus) : retourner la cuve quatre fois dans les dix
     * premières secondes, puis quatre fois dans les dix premières secondes de
     * chaque minute.
     */
    val ILFORD = Agitation(initiale = 10.0, duree = 10.0, intervalle = 60.0)

    /**
     * Kodak (fiches J-78 et F-4017) : cinq à sept retournements en 5 s au
     * départ, repos jusqu'à 30 s, puis 5 s toutes les 30 s.
     */
    val KODAK = Agitation(initiale = 5.0, duree = 5.0, intervalle = 30.0)

    /** En cuvette, papier ou plan-film : on berce sans arrêt. */
    val CONTINUE = Agitation(0.0, 0.0, 0.0, enContinu = true)
}

data class Etape(
    val nom: String,
    val duree: Double,
    val agitation: Agitation? = null,
    val consigne: String = ""
)

data class SequenceLabo(
    val nom: String,
    val etapes: List<Etape>,
    val source: Source? = null,
    val note: String = ""
) {
    val duree: Double get() = etapes.sumOf { it.duree }

    fun debut(i: Int): Double = etapes.take(i).sumOf { it.duree }

    /** Prête à lancer : chaque bain a une durée. */
    val complete: Boolean get() = etapes.isNotEmpty() && etapes.all { it.duree > 0 }
}

enum class Signal {
    /** Un nouveau bain commence : vider, verser. */
    ETAPE,
    AGITER,
    REPOS,
    FIN
}

data class Evenement(val instant: Double, val signal: Signal, val etape: Int)

/** Où l'on en est à un instant donné. */
data class Position(
    val etape: Int,
    val dansEtape: Double,
    val resteEtape: Double,
    val agite: Boolean,
    /** Secondes avant la prochaine reprise d'agitation dans ce bain, s'il y en a une. */
    val prochaineAgitation: Double?,
    val fini: Boolean
)

object Chrono {

    /**
     * Toute la chronologie d'une séquence. Au début d'un bain, le signal
     * d'étape suffit : on verse et on agite dans le même geste, pas besoin
     * d'un second bip.
     */
    fun evenements(s: SequenceLabo): List<Evenement> {
        val liste = mutableListOf<Evenement>()
        s.etapes.forEachIndexed { i, e ->
            val debut = s.debut(i)
            liste += Evenement(debut, Signal.ETAPE, i)
            e.agitation?.takeUnless { it.enContinu }?.creneaux(e.duree)?.forEach { c ->
                if (c.start > 0) liste += Evenement(debut + c.start, Signal.AGITER, i)
                if (c.endInclusive < e.duree) liste += Evenement(debut + c.endInclusive, Signal.REPOS, i)
            }
        }
        liste += Evenement(s.duree, Signal.FIN, s.etapes.lastIndex)
        return liste.sortedBy { it.instant }
    }

    fun position(s: SequenceLabo, t: Double): Position {
        if (t >= s.duree) return Position(s.etapes.lastIndex, s.etapes.last().duree, 0.0, false, null, true)
        var i = 0
        while (i < s.etapes.lastIndex && t >= s.debut(i + 1)) i++
        val e = s.etapes[i]
        val dans = (t - s.debut(i)).coerceAtLeast(0.0)
        val creneaux = e.agitation?.creneaux(e.duree).orEmpty()
        val agite = creneaux.any { dans >= it.start && dans < it.endInclusive }
        val prochaine = creneaux.map { it.start }.firstOrNull { it > dans }?.let { it - dans }
        return Position(i, dans, e.duree - dans, agite, prochaine, false)
    }

    /** Les événements survenus entre deux lectures de l'horloge, bornes (avant, apres]. */
    fun entre(evenements: List<Evenement>, avant: Double, apres: Double): List<Evenement> =
        evenements.filter { it.instant > avant && it.instant <= apres }
}

/*
 * Les enchaînements de départ. Durées et consignes viennent des fiches ; ce
 * qu'une fiche ne précise pas est dit comme tel, et tout reste modifiable.
 */
object Sequences {

    val FILM_KODAK = SequenceLabo(
        "Film — chaîne Kodak",
        listOf(
            Etape("Révélateur", 6.75 * 60, Agitations.KODAK, "Tri-X 400 en D-76 à 20 °C : 6 min 45. Reprends le temps de ton couple film-révélateur."),
            Etape("Arrêt", 30.0, Agitations.CONTINUE, "Bain d'arrêt, avec agitation."),
            Etape("Fixateur", 4 * 60.0, Agitations.KODAK, "KODAK Rapid Fixer : 2 à 4 min, agitation fréquente."),
            Etape("Lavage", 20 * 60.0, null, "Eau courante, 20 à 30 min — ou 5 min après un éliminateur d'hyposulfite."),
            Etape("Agent mouillant", 30.0, null, "KODAK PHOTO-FLO : 30 s, puis sécher à l'abri de la poussière.")
        ),
        Sources.KODAK_TRIX,
        "Entre 18 et 24 °C pour tous les bains."
    )

    val FILM_ILFORD = SequenceLabo(
        "Film — chaîne Ilford",
        listOf(
            Etape("Révélateur", 6.5 * 60, Agitations.ILFORD, "HP5 Plus en ILFOTEC HC 1+31 à 20 °C : 6 min 30. Reprends le temps de ton couple film-révélateur."),
            Etape("Arrêt", 10.0, Agitations.CONTINUE, "ILFOSTOP 1+19 : 10 s au minimum ; plus long ne gêne pas."),
            Etape("Fixateur", 5 * 60.0, Agitations.ILFORD, "RAPID FIXER 1+4 : 2 à 5 min."),
            Etape("Lavage", 5 * 60.0, null, "Eau courante, 5 à 10 min — ou la méthode Ilford : remplir et retourner 5, 10 puis 20 fois."),
            Etape("Agent mouillant", 30.0, null, "ILFOTOL, 5 mL par litre. Ilford ne donne pas de durée ; Kodak compte 30 s pour son PHOTO-FLO.")
        ),
        Sources.ILFORD_HP5,
        "Tous les bains à la même température, ou à 5 °C au plus du révélateur."
    )

    /**
     * Le papier : l'ordre des bains est sûr, les durées dépendent du papier et
     * des produits — le Labo ne les invente pas, elles sont à reprendre de
     * leurs fiches.
     */
    val PAPIER = SequenceLabo(
        "Tirage papier",
        listOf(
            Etape("Révélateur", 0.0, Agitations.CONTINUE, "Durée de la fiche de ton papier et de ton révélateur."),
            Etape("Arrêt", 0.0, Agitations.CONTINUE, "Durée de la fiche du bain d'arrêt."),
            Etape("Fixateur", 0.0, Agitations.CONTINUE, "Durée de la fiche du fixateur, pour ce papier (RC ou baryté)."),
            Etape("Lavage", 0.0, null, "Durée de la fiche du papier : un baryté se lave bien plus longtemps qu'un RC.")
        ),
        null,
        "Règle chaque durée d'après les fiches de ton papier et de tes bains."
    )

    val TOUTES = listOf(FILM_ILFORD, FILM_KODAK, PAPIER)
}
