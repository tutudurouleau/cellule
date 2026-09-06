package fr.cellule.core

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

data class Matiere(val nom: String, val reflectance: Double) {
    /** Écart au gris 18 %, en diaphs. */
    val ecartAuGris: Double get() = log2(reflectance / Photometrie.GRIS)
}

val MATIERES = listOf(
    Matiere("Neige fraîche", 0.85),
    Matiere("Peinture blanche, mur chaulé", 0.75),
    Matiere("Béton neuf", 0.475),
    Matiere("Sable de désert, plage claire", 0.40),
    Matiere("Peau claire", 0.35),
    Matiere("Granite clair (Restonica)", 0.30),
    Matiere("Herbe verte vive", 0.25),
    Matiere("Béton vieilli, pierre grise", 0.25),
    Matiere("Gris moyen (charte)", 0.18),
    Matiere("Sol nu, terre sèche", 0.17),
    Matiere("Feuillage caduc", 0.165),
    Matiere("Herbe sèche, maquis grillé", 0.15),
    Matiere("Peau foncée", 0.125),
    Matiere("Asphalte usé", 0.12),
    Matiere("Forêt de conifères", 0.11),
    Matiere("Océan, eau profonde (diffus)", 0.06),
    Matiere("Asphalte neuf", 0.045),
    Matiere("Velours noir, charbon", 0.015)
)

data class Zone(val chiffre: String, val ecart: Int, val rendu: String) {
    val reflectance: Double get() = Photometrie.GRIS * 2.0.pow(ecart)
}

val ZONES = listOf(
    Zone("0", -5, "Noir absolu, aucune densité"),
    Zone("I", -4, "Noir, aucune texture"),
    Zone("II", -3, "Premier noir texturé"),
    Zone("III", -2, "Ombre texturée"),
    Zone("IV", -1, "Ombre ouverte, feuillage sombre, peau foncée"),
    Zone("V", 0, "Gris moyen — ce que donne toute cellule"),
    Zone("VI", 1, "Peau claire, ciel bleu profond"),
    Zone("VII", 2, "Blanc texturé, neige à l’ombre"),
    Zone("VIII", 3, "Dernier blanc texturé"),
    Zone("IX", 4, "Blanc sans texture")
)

object Zones {

    /** La zone où tombe naturellement une matière. */
    fun zoneNaturelle(reflectance: Double): Double = 5 + log2(reflectance / Photometrie.GRIS)

    fun zoneLaPlusProche(valeur: Double): Zone =
        ZONES[(valeur.roundToInt()).coerceIn(0, ZONES.size - 1)]

    /**
     * Une lecture réfléchie rend toujours la matière visée en zone V. Pour la
     * placer ailleurs, on décale l'exposition — et comme un EV plus haut veut
     * dire moins de lumière, monter d'une zone fait descendre l'EV d'autant.
     */
    fun expositionPourPlacer(ev100Lu: Double, zone: Zone): Double = ev100Lu - zone.ecart

    /**
     * La correction à appliquer à une lecture réfléchie pour rendre une matière
     * à sa clarté vraie. Sur la neige : +2⅓ diaphs, sans quoi elle sort grise.
     */
    fun correctionReflectance(reflectance: Double): Double = log2(reflectance / Photometrie.GRIS)
}
