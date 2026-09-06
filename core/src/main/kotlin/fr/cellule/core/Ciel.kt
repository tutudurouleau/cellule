package fr.cellule.core

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sin

/**
 * Le modèle d'éclairement.
 *
 * Faisceau direct : E₀·sin(h)·τ^(AM^0,678), avec la masse d'air de
 * Kasten-Young. C'est ce terme qui s'effondre sous 15° — bien plus vite que le
 * simple sin(h) — et qui rend la golden hour si rapide à basculer.
 *
 * Ciel diffus : en sin(h)^0,8, beaucoup plus plat. Il devient l'essentiel de la
 * lumière dès que le soleil est bas ou caché, et c'est pourquoi la hauteur du
 * soleil compte beaucoup moins par temps couvert.
 *
 * Étalonnage : ciel pur, soleil à 52° → EV 15,0, c'est-à-dire Sunny 16.
 */
object Ciel {

    private const val E0 = 128000.0
    private const val PART_DIFFUS = 0.155

    /** Part directe d'un plein soleil franc, qui sert de référence à 1. */
    private const val PART_DIRECTE_REF = 0.80

    private fun rad(d: Double) = d * PI / 180

    /** Masse d'air relative, formule de Kasten-Young. */
    fun masseAir(hauteurDeg: Double): Double {
        val h = maxOf(hauteurDeg, -0.5)
        return 1.0 / (sin(rad(h)) + 0.50572 * (h + 6.07995).pow(-1.6364))
    }

    /* Crépuscule : interpolation logarithmique sur des points d'ancrage mesurés. */
    private val CREPUSCULE = listOf(
        0.0 to 400.0, -2.0 to 100.0, -4.0 to 16.0, -6.0 to 3.4, -8.0 to 0.6,
        -10.0 to 0.09, -12.0 to 0.008, -14.0 to 0.002, -16.0 to 0.0016, -18.0 to 0.0015
    )

    fun luxCrepuscule(hauteurDeg: Double): Double {
        if (hauteurDeg <= -18) return 0.0015
        for (i in 0 until CREPUSCULE.size - 1) {
            val (ha, la) = CREPUSCULE[i]
            val (hb, lb) = CREPUSCULE[i + 1]
            if (hauteurDeg <= ha && hauteurDeg >= hb) {
                val f = (ha - hauteurDeg) / (ha - hb)
                return 10.0.pow(log10(la) * (1 - f) + log10(lb) * f)
            }
        }
        return 400.0
    }

    fun eclairement(hauteurDeg: Double, ciel: EtatDuCiel): Eclairement {
        var direct = 0.0
        var diffus: Double
        if (hauteurDeg > 0) {
            val s = sin(rad(hauteurDeg))
            val tau = 0.7.pow(masseAir(hauteurDeg).pow(0.678))
            direct = E0 * s * tau
            diffus = E0 * PART_DIFFUS * s.pow(0.8) + 350
        } else {
            diffus = luxCrepuscule(hauteurDeg)
        }
        direct *= ciel.partDirecte
        diffus *= ciel.partDiffuse
        val total = (direct + diffus).coerceAtLeast(1e-6)
        return Eclairement(
            direct = direct,
            diffus = diffus,
            total = total,
            ev100 = Photometrie.evDepuisLux(total),
            partSoleil = ((direct / total) / PART_DIRECTE_REF).coerceIn(0.0, 1.0)
        )
    }

    /**
     * Table du document source : perte en diaphs = log₂(sin h). Sert au mode
     * « grille mentale », celui qu'on peut tenir de tête.
     */
    private val TABLE_PLATE = listOf(
        90.0 to 0.0, 60.0 to -0.2, 45.0 to -0.5, 30.0 to -1.0, 20.0 to -1.5,
        15.0 to -2.0, 10.0 to -2.5, 5.0 to -3.5, 0.0 to -6.0,
        -3.0 to -9.0, -6.0 to -12.0, -9.0 to -15.0, -18.0 to -24.0
    )

    fun perteHauteurPlate(hauteurDeg: Double): Double {
        if (hauteurDeg >= 90) return 0.0
        for (i in 0 until TABLE_PLATE.size - 1) {
            val (ha, va) = TABLE_PLATE[i]
            val (hb, vb) = TABLE_PLATE[i + 1]
            if (hauteurDeg <= ha && hauteurDeg >= hb) {
                val f = (ha - hauteurDeg) / (ha - hb)
                return va * (1 - f) + vb * f
            }
        }
        return -24.0
    }
}

data class Eclairement(
    val direct: Double,
    val diffus: Double,
    val total: Double,
    val ev100: Double,
    /** Part de soleil direct, ramenée à 1 pour un plein soleil franc. */
    val partSoleil: Double
)

data class EtatDuCiel(
    val id: String,
    val nom: String,
    val detail: String,
    val partDirecte: Double,
    val partDiffuse: Double,
    /** Valeur plate du document source, pour le mode grille mentale. */
    val plat: Double
)

val CIELS = listOf(
    EtatDuCiel("soleil", "Plein soleil", "ombre franche, bord net", 1.00, 1.00, 0.0),
    EtatDuCiel("voile", "Voile léger", "ombre visible mais bord flou", 0.30, 1.45, -1.0),
    EtatDuCiel("blanc", "Couvert clair", "ciel blanc lumineux, sans ombre", 0.0, 1.60, -1.5),
    EtatDuCiel("couvert", "Ciel couvert", "gris uniforme, aucune ombre", 0.0, 1.00, -2.5),
    EtatDuCiel("lourd", "Couvert lourd", "orage, il fait sombre à midi", 0.0, 0.30, -4.5)
)

/**
 * Un obstacle ne coupe que ce qui existe. Sans soleil direct, « ombre portée »
 * ne veut plus rien dire ; « sous-bois » garde tout son sens, puisque la
 * canopée bouche le ciel lui-même. D'où les deux valeurs.
 */
data class Obstacle(
    val id: String,
    val nom: String,
    val detail: String,
    val auSoleil: Double,
    val sousDiffus: Double
) {
    fun diaphs(partSoleil: Double): Double = sousDiffus + (auSoleil - sousDiffus) * partSoleil
}

val OBSTACLES = listOf(
    Obstacle("ouvert", "Terrain ouvert", "sujet directement éclairé", 0.0, 0.0),
    Obstacle("ombre", "Ombre portée", "ciel dégagé au-dessus", -2.5, 0.0),
    Obstacle("cj", "Contre-jour", "soleil derrière le sujet", -1.5, 0.0),
    Obstacle("arbre", "Sous un arbre isolé", "feuillage clair, taches de jour", -1.5, -1.0),
    Obstacle("sousbois", "Sous-bois dense", "canopée fermée", -3.5, -3.0),
    Obstacle("versant", "Ombre de versant", "vallée encaissée, relief", -2.5, -0.5),
    Obstacle("rue", "Rue étroite, cour", "ciel réduit à une bande", -3.5, -2.5),
    Obstacle("vitre", "Près de la fenêtre", "intérieur, à un mètre de la vitre", -4.5, -4.0),
    Obstacle("interieur", "Intérieur, fond de pièce", "lumière du jour seule", -8.0, -7.5)
)

/** Surfaces qui renvoient de la lumière sur le sujet — un gain d'éclairement réel. */
data class Renvoi(val id: String, val nom: String, val diaphs: Double)

val RENVOIS = listOf(
    Renvoi("neige", "Neige fraîche plein cadre", 1.5),
    Renvoi("sable", "Sable clair, plage", 1.0),
    Renvoi("eau", "Eau au soleil, galets clairs", 1.0),
    Renvoi("granite", "Granite clair, calcaire", 0.75),
    Renvoi("mur", "Mur blanc proche", 0.75),
    Renvoi("altitude", "Altitude > 2 000 m", 0.75),
    Renvoi("sec", "Air très sec, méditerranéen", 0.4)
)

/**
 * Une surface réverbérante rend surtout du soleil direct. Par temps couvert
 * elle rend encore, mais nettement moins.
 */
fun facteurRenvoi(partSoleil: Double): Double = 0.4 + 0.6 * partSoleil
