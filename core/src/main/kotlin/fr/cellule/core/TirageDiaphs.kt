package fr.cellule.core

import kotlin.math.log2
import kotlin.math.pow

/**
 * Le tirage en diaphs (« f-stop timing »).
 *
 * Le papier ne voit pas des secondes : sa densité suit le logarithme de
 * l'exposition, comme le négatif. Une bande d'essai à 5, 10, 15, 20, 25 s
 * paraît régulière sur le minuteur mais ne l'est pas sur le papier : de 5 à
 * 10 s on double la lumière (1 diaph), de 20 à 25 s on ne l'augmente que d'un
 * tiers de diaph. Les premières bandes sautent, les dernières se confondent.
 *
 * En comptant en diaphs, chaque bande ajoute la même quantité de gris, et une
 * correction s'exprime de la même façon quel que soit le temps de base :
 * « +⅓ dans le ciel » vaut sur un tirage de 8 s comme sur un de 40 s. C'est
 * exactement la logique EV du posemètre : t = t₀ · 2^Δ.
 */
object TirageDiaphs {

    /** Le temps qui donne Δ diaphs de plus (ou de moins) que [base]. */
    fun temps(base: Double, diaphs: Double): Double = base * 2.0.pow(diaphs)

    /** L'écart en diaphs entre deux temps : log₂(t₂/t₁). */
    fun ecart(t1: Double, t2: Double): Double = log2(t2 / t1)

    /**
     * Temps supplémentaire pour brûler une zone de [diaphs] : la zone reçoit
     * alors base · 2^Δ en tout. Brûler de 1 diaph, c'est redonner tout le
     * temps de base, pas « un peu plus ».
     */
    fun brulage(base: Double, diaphs: Double): Double = temps(base, diaphs) - base

    /**
     * Durée pendant laquelle on masque une zone pour l'éclaircir de [diaphs] :
     * elle ne reçoit plus que base · 2^−Δ. Masquer de 1 diaph, c'est cacher
     * la zone pendant la moitié du temps de base.
     */
    fun masquage(base: Double, diaphs: Double): Double = base - temps(base, -diaphs)

    /**
     * Fermer l'objectif de l'agrandisseur de N diaphs demande 2^N fois plus de
     * temps : même règle que sur l'appareil de prise de vue.
     */
    fun apresFermeture(temps: Double, diaphsFermes: Double): Double = temps * 2.0.pow(diaphsFermes)
}

/**
 * « +1 ½ », « −⅓ », « +⅔ » : au tirage on compte en demis ou en tiers de
 * diaph ; un demi arrondi au tiers le plus proche deviendrait faux.
 */
fun libelleCorrection(diaphs: Double): String {
    val demis = diaphs * 2
    val enDemis = kotlin.math.abs(demis - Math.round(demis)) < 1e-6 && Math.round(demis) % 2 != 0L
    if (!enDemis) return libelleDiaphs(diaphs)
    val entier = kotlin.math.abs(diaphs).toInt()
    return (if (diaphs < 0) "−" else "+") + (if (entier > 0) "$entier ½" else "½")
}

/**
 * Une bande d'une bande d'essai : son temps total et ce qu'il faut ajouter
 * quand on découvre la bande suivante (méthode du cache qu'on déplace).
 */
data class Bande(val rang: Int, val diaphs: Double, val total: Double, val ajout: Double)

object BandeEssai {

    /**
     * [nombre] bandes à pas réguliers de [pas] diaphs, la première exposée
     * [premier] secondes. Les ajouts sont ce qu'on programme au minuteur,
     * bande après bande, en découvrant progressivement le papier : la bande 0
     * reçoit tout, la dernière seulement le premier temps — ou l'inverse selon
     * le sens du cache, le total par bande est le même.
     */
    fun diaphs(premier: Double, pas: Double, nombre: Int): List<Bande> {
        require(nombre >= 1 && premier > 0 && pas > 0)
        var precedent = 0.0
        return (0 until nombre).map { i ->
            val total = TirageDiaphs.temps(premier, i * pas)
            Bande(i, i * pas, total, total - precedent).also { precedent = total }
        }
    }

    /**
     * La bande d'essai « classique » à incréments égaux en secondes, pour
     * montrer ce qu'elle fait réellement : l'écart en diaphs entre deux bandes
     * voisines fond à mesure que le temps grandit.
     */
    fun lineaire(premier: Double, increment: Double, nombre: Int): List<Bande> {
        require(nombre >= 1 && premier > 0 && increment > 0)
        return (0 until nombre).map { i ->
            val total = premier + i * increment
            Bande(i, TirageDiaphs.ecart(premier, total), total, if (i == 0) premier else increment)
        }
    }
}
