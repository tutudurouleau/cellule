package fr.cellule.core

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/*
 * Déplacer un projecteur du plan au sol pour obtenir ce qu'on veut sur un
 * sujet. Le projecteur glisse dans l'axe où il est orienté : reculer, c'est
 * aller à l'opposé de là où il regarde. En reculant, il garde l'angle sous
 * lequel il voit le sujet (il monte), sauf si on préfère garder sa hauteur,
 * et jamais au-dessus du plafond du plan.
 */

enum class GardeHauteur(val libelle: String) {
    ANGLE("Garder l'angle"),
    HAUTEUR("Garder la hauteur")
}

enum class PorteeCible(val libelle: String) {
    SEUL("Ce projecteur seul"),
    TOTAL("Total au sujet")
}

/** Ce que reçoit le sujet d'un projecteur, à un moment donné. */
data class EtatLumiere(
    /** Distance réelle projecteur–sujet, hauteurs comprises. */
    val distance: Double,
    val hauteur: Double,
    /** Apport de ce projecteur ; null si son intensité est inconnue. */
    val lux: Double?,
    /** Tout ce que reçoit le sujet, ce projecteur compris. */
    val total: Double,
    val dansFaisceau: Boolean,
    /** Angle sous lequel le projecteur voit le sujet, en degrés sous l'horizontale. */
    val angleVertical: Double
)

data class Deplacement(
    /** Le projecteur à sa nouvelle place. */
    val element: ElementSol,
    /** En mètres dans l'axe : positif, il recule ; négatif, il avance. */
    val recul: Double,
    val avant: EtatLumiere,
    val apres: EtatLumiere,
    /** Il aurait dû monter plus haut que le plafond : l'angle n'est pas tout à fait gardé. */
    val plafondAtteint: Boolean,
    val solAtteint: Boolean
)

/** Une cible atteinte ([deplacement]) ou la raison pour laquelle elle ne l'est pas. */
data class Cible(val deplacement: Deplacement?, val probleme: String?, val apportVoulu: Double? = null)

object DeplacementProjecteur {

    const val HAUTEUR_MIN = 0.2
    const val RECUL_MAX = 150.0
    /** Le projecteur ne dépasse pas le sujet : il s'arrête à cette distance avant lui, dans son axe. */
    const val GARDE_AVANT = 0.3

    fun etat(plan: PlanSol, p: ElementSol, sujet: ElementSol, projecteurs: (String) -> Projecteur?): EtatLumiere {
        val apports = Sol.apports(plan, sujet, projecteurs)
        val a = apports.firstOrNull { it.projecteur.identifiant == p.identifiant }
        val hp = p.hauteur ?: Sol.HAUTEUR_PROJECTEUR
        val hs = sujet.hauteur ?: Sol.HAUTEUR_VISAGE
        val sol = Sol.distanceSol(p, sujet)
        return EtatLumiere(
            distance = a?.distance ?: sqrt(sol * sol + (hp - hs) * (hp - hs)),
            hauteur = hp,
            lux = a?.lux,
            total = apports.mapNotNull { it.lux }.sum(),
            dansFaisceau = a?.dansFaisceau ?: true,
            angleVertical = Math.toDegrees(atan2(hp - hs, sol))
        )
    }

    private data class Place(val element: ElementSol, val plafondAtteint: Boolean, val solAtteint: Boolean)

    /** Le projecteur reculé de [recul] mètres dans son axe (négatif : avancé), sa hauteur ajustée. */
    private fun placer(p: ElementSol, sujet: ElementSol, recul: Double, garde: GardeHauteur, plafond: Double?): Place {
        val (ux, uy) = Sol.direction(p.rotation)
        val nx = p.x - ux * recul
        val ny = p.y - uy * recul
        val hp = p.hauteur ?: Sol.HAUTEUR_PROJECTEUR
        val hs = sujet.hauteur ?: Sol.HAUTEUR_VISAGE
        val s0 = hypot(p.x - sujet.x, p.y - sujet.y)
        if (garde == GardeHauteur.HAUTEUR || s0 < 0.05) return Place(p.copy(x = nx, y = ny), false, false)
        val s1 = hypot(nx - sujet.x, ny - sujet.y)
        var h = hs + (hp - hs) / s0 * s1
        var haut = false
        var bas = false
        val limite = plafond?.takeIf { it > HAUTEUR_MIN }
        if (limite != null && h > limite) {
            /* S'il était déjà au-dessus du plafond, il n'y monte pas plus. */
            h = maxOf(limite, minOf(h, hp))
            haut = true
        }
        if (h < HAUTEUR_MIN) {
            h = HAUTEUR_MIN
            bas = true
        }
        return Place(p.copy(x = nx, y = ny, hauteur = Math.round(h * 100) / 100.0), haut, bas)
    }

    fun deplacer(
        plan: PlanSol, p: ElementSol, sujet: ElementSol, recul: Double,
        garde: GardeHauteur, plafond: Double?, projecteurs: (String) -> Projecteur?
    ): Deplacement {
        val place = placer(p, sujet, recul, garde, plafond)
        return Deplacement(
            element = place.element,
            recul = recul,
            avant = etat(plan, p, sujet, projecteurs),
            apres = etat(plan.remplacer(place.element), place.element, sujet, projecteurs),
            plafondAtteint = place.plafondAtteint,
            solAtteint = place.solAtteint
        )
    }

    /**
     * Le déplacement qui donne [luxVoulu] au sujet : l'apport de ce projecteur
     * seul, ou le total (les autres projecteurs restant où ils sont).
     */
    fun pourLux(
        plan: PlanSol, p: ElementSol, sujet: ElementSol, luxVoulu: Double, portee: PorteeCible,
        garde: GardeHauteur, plafond: Double?, projecteurs: (String) -> Projecteur?
    ): Cible {
        val choisi = if (p.projecteur.isBlank()) null else projecteurs(p.projecteur)
        val i = Sol.intensite(p, choisi)
            ?: return Cible(null, "L'intensité de ce projecteur est inconnue : choisis-le dans le catalogue ou saisis ses lux à une distance.")
        val avant = etat(plan, p, sujet, projecteurs)
        if (!avant.dansFaisceau) return Cible(null, "Le sujet est hors du faisceau : oriente d'abord le projecteur vers lui.")
        if (luxVoulu <= 0) return Cible(null, "Donne un éclairement plus grand que zéro.")
        val autres = avant.total - (avant.lux ?: 0.0)
        val seul = if (portee == PorteeCible.TOTAL) luxVoulu - autres else luxVoulu
        if (seul <= 0) {
            return Cible(
                null,
                "Les autres projecteurs donnent déjà " + nombreCourt(autres, 0) + " lux à ce sujet : même éteint, celui-ci ne descend pas le total à " +
                    nombreCourt(luxVoulu, 0) + ". Baisse ou recule un autre projecteur."
            )
        }
        val dVoulue = Eclairage.distancePour(i, seul, perteDiaphs = p.pertesDiaphs)
        val (ux, uy) = Sol.direction(p.rotation)
        val a0 = (sujet.x - p.x) * ux + (sujet.y - p.y) * uy
        val rMin = GARDE_AVANT - a0
        fun d(r: Double) = etatGeometrique(placer(p, sujet, r, garde, plafond).element, sujet)
        val dMin = d(rMin)
        if (dVoulue < dMin) {
            return Cible(
                null,
                "Il faudrait l'approcher à " + nombreCourt(dVoulue, 2) + " m du sujet : dans son axe, il ne peut pas venir à moins de " +
                    nombreCourt(dMin, 2) + " m. Prends un projecteur plus puissant, ou retire de la diffusion.",
                apportVoulu = seul
            )
        }
        if (dVoulue > d(RECUL_MAX)) {
            return Cible(null, "Il faudrait le reculer de plus de " + nombreCourt(RECUL_MAX, 0) + " m : baisse-le plutôt, ou ajoute de la diffusion.", apportVoulu = seul)
        }
        var bas = rMin
        var haut = RECUL_MAX
        repeat(80) {
            val m = (bas + haut) / 2
            if (d(m) < dVoulue) bas = m else haut = m
        }
        val r = Math.round((bas + haut) / 2 * 100) / 100.0
        val dep = deplacer(plan, p, sujet, r, garde, plafond, projecteurs)
        if (!dep.apres.dansFaisceau) {
            return Cible(null, "Dans cet axe, le sujet sortirait du faisceau : oriente d'abord le projecteur vers lui.", apportVoulu = seul)
        }
        return Cible(dep, null, apportVoulu = seul)
    }

    /** Jusqu'où il peut avancer dans son axe sans dépasser le sujet (négatif : il est déjà trop près). */
    fun avanceMax(p: ElementSol, sujet: ElementSol): Double {
        val (ux, uy) = Sol.direction(p.rotation)
        return (sujet.x - p.x) * ux + (sujet.y - p.y) * uy - GARDE_AVANT
    }

    /** Distance réelle, hauteurs comprises. */
    private fun etatGeometrique(p: ElementSol, sujet: ElementSol): Double {
        val sol = Sol.distanceSol(p, sujet)
        val dh = (p.hauteur ?: Sol.HAUTEUR_PROJECTEUR) - (sujet.hauteur ?: Sol.HAUTEUR_VISAGE)
        return sqrt(sol * sol + dh * dh)
    }

    /**
     * Sans bouger : la part de lumière à garder (1 = pleine puissance) pour
     * passer de [luxActuel] à [luxVoulu] — en intensité lumineuse, pas en
     * position de curseur (la courbe du gradateur varie selon le projecteur).
     */
    fun gradation(luxActuel: Double, luxVoulu: Double): Double = luxVoulu / luxActuel

    /** Les projecteurs à filament changent de couleur quand on les gradue. */
    fun virageAuGradateur(choisi: Projecteur?): Boolean =
        choisi?.famille == FamilleProjecteur.FRESNEL_TUNGSTENE || choisi?.famille == FamilleProjecteur.OUVERT_TUNGSTENE
}
