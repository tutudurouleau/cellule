package fr.cellule.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Le plan au sol : le décor vu de dessus, à l'échelle, en mètres. Le haut
 * du plan est l'axe −y (comme sur un écran) ; les rotations se comptent en
 * degrés dans le sens des aiguilles d'une montre depuis ce haut. Le nord du
 * plan se règle de la même façon : 0, le nord est en haut.
 */

enum class TypeElement(val libelle: String, val pluriel: String) {
    CAMERA("Caméra", "Caméras"),
    COMEDIEN("Comédien", "Comédiens"),
    PROJECTEUR("Projecteur", "Projecteurs"),
    REFLECTEUR("Réflecteur", "Réflecteurs"),
    DIFFUSION("Cadre de diffusion", "Cadres de diffusion"),
    DRAPEAU("Drapeau", "Drapeaux"),
    RAIL("Rail de travelling", "Rails"),
    MUR("Mur", "Murs"),
    FENETRE("Fenêtre", "Fenêtres"),
    PORTE("Porte", "Portes"),
    MEUBLE("Meuble", "Meubles"),
    REPERE("Repère", "Repères");

    /** Les éléments qui se dessinent comme un trait (longueur = largeur, épaisseur = profondeur). */
    val enTrait: Boolean get() = this in setOf(REFLECTEUR, DIFFUSION, DRAPEAU, RAIL, MUR, FENETRE, PORTE)

    /** Ceux qui ont des positions numérotées reliées par des flèches. */
    val enMouvement: Boolean get() = this == CAMERA || this == COMEDIEN
}

/** Un élément posé sur le plan. Les champs sans objet pour son type restent à leur défaut. */
data class ElementSol(
    val identifiant: Long,
    val type: TypeElement,
    val x: Double,
    val y: Double,
    /** Vers où il regarde (caméra, comédien, projecteur), ou l'orientation du trait. */
    val rotation: Double = 0.0,
    val largeur: Double = 0.5,
    val profondeur: Double = 0.5,
    /** Hauteur de l'objectif, du projecteur, ou du visage du comédien, en mètres. */
    val hauteur: Double? = null,
    /** « A », « Marie », « SkyPanel face »… */
    val nom: String = "",
    /** Le numéro de la position, pour les déplacements (A1 → A2). */
    val etape: Int = 1,
    /* Caméra */
    val focaleMm: Double? = null,
    val capteurLargeurMm: Double? = null,
    val capteurHauteurMm: Double? = null,
    val desanamorphose: Double = 1.0,
    /** Inclinaison de la caméra, en degrés au-dessus de l'horizontale (négative : plongée). */
    val inclinaison: Double = 0.0,
    /** Les plans tournés depuis cette position : « 3, 4, 7 ». */
    val plans: String = "",
    /* Projecteur */
    /** L'identifiant d'un projecteur (à soi ou du catalogue) ; vide : saisie directe ci-dessous. */
    val projecteur: String = "",
    val spot: Boolean = false,
    val luxReference: Double? = null,
    val distanceReference: Double? = null,
    val faisceau: Double? = null,
    val pertesDiaphs: Double = 0.0,
    /** La lettre du circuit électrique qui l'alimente (« A », « B »…) ; vide : pas encore réparti. */
    val circuit: String = "",
    /** Sa puissance, quand le projecteur n'en a pas dans sa fiche (saisie directe). */
    val puissanceW: Double? = null,
    /** Son symbole sur le plan de feu (nom d'un [SymboleProjecteur]) ; vide : celui de son modèle. */
    val symbole: String = "",
    /** Sa source (nom d'une [SourceLampe]) ; vide : celle de son modèle. */
    val lampe: String = "",
    /** La gélatine posée devant : « 1/2 CTO », « 216 ». */
    val gelatine: String = "",
    val notes: String = ""
) {
    val libelle: String
        get() = when {
            /* « A1 », « A2 » pour une lettre ; « Marie », « Marie (2) » pour un nom. */
            type.enMouvement && nom.isNotBlank() && nom.trim().length <= 2 -> nom.trim() + etape
            type.enMouvement && nom.isNotBlank() -> if (etape <= 1) nom.trim() else nom.trim() + " (" + etape + ")"
            nom.isNotBlank() -> nom
            else -> type.libelle
        }
}

/** Un plan au sol, rangé (ou non) dans un jour de tournage ou de repérage. */
data class PlanSol(
    val identifiant: Long,
    val nom: String,
    val jourId: Long? = null,
    val elements: List<ElementSol> = emptyList(),
    val grille: Boolean = true,
    /** Pas de la grille, en mètres. */
    val pasGrille: Double = 0.5,
    /** Vrai : les éléments déplacés se calent sur la grille. */
    val aimanter: Boolean = false,
    /** Le calibre des circuits du bilan électrique (un nom de CIRCUITS) ; vide : le premier de la région. */
    val circuitNom: String = "",
    val legende: Boolean = true,
    /** Direction du nord sur le plan, en degrés depuis le haut, sens horaire. */
    val nordDeg: Double = 0.0,
    val soleil: Boolean = true,
    /** « 2026-11-17 » et « 15:30 » : le moment du tournage, pour le soleil. */
    val date: String = "",
    val heure: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Le fond de carte OpenStreetMap (extérieur), quand il y a du réseau. */
    val carte: Boolean = false,
    /** La hauteur sous plafond (ou la plus haute accroche possible), en mètres ; null : pas de limite. */
    val plafond: Double? = null,
    val notes: String = ""
) {
    fun element(id: Long): ElementSol? = elements.firstOrNull { it.identifiant == id }

    fun remplacer(e: ElementSol): PlanSol =
        copy(elements = if (elements.any { it.identifiant == e.identifiant }) elements.map { if (it.identifiant == e.identifiant) e else it } else elements + e)

    fun sans(id: Long): PlanSol = copy(elements = elements.filterNot { it.identifiant == id })
}

/** Ce qu'un projecteur donne au sujet, d'après le plan. */
data class ApportLumiere(
    val projecteur: ElementSol,
    val distance: Double,
    /** Le sujet est dans le faisceau (ou le faisceau est inconnu). */
    val dansFaisceau: Boolean,
    /** Écart à l'axe du projecteur, en degrés. */
    val horsAxe: Double,
    /** Angle entre le regard du sujet et la direction d'où vient la lumière : 0 = de face, 180 = de dos. */
    val angleAuRegard: Double,
    /** Éclairement dans l'axe de la lumière (cellule incidente tournée vers la source), ou null si l'intensité est inconnue. */
    val lux: Double?
) {
    val position: String
        get() = when {
            angleAuRegard <= 60 -> "face"
            angleAuRegard >= 120 -> "contre"
            else -> "côté"
        }
}

data class Creneau(val debut: Double, val fin: Double)

/** Heures dorées et bleues d'un jour, en heures légales décimales. */
data class HeuresDuJour(val bleueMatin: Creneau?, val doreeMatin: Creneau?, val doreeSoir: Creneau?, val bleueSoir: Creneau?)

object Sol {
    /** Une position calée sur la grille de pas [pas] (en mètres). */
    fun caler(v: Double, pas: Double): Double = if (pas <= 0.0) v else Math.round(v / pas) * pas


    const val HAUTEUR_PROJECTEUR = 2.5
    const val HAUTEUR_VISAGE = 1.6
    const val HAUTEUR_CAMERA = 1.5

    /** Le vecteur unitaire d'une rotation (0 = vers le haut du plan). */
    fun direction(rotationDeg: Double): Pair<Double, Double> {
        val r = Math.toRadians(rotationDeg)
        return sin(r) to -cos(r)
    }

    /** La rotation qui regarde de [a] vers [b]. */
    fun rotationVers(ax: Double, ay: Double, bx: Double, by: Double): Double =
        Boussole.normaliser(Math.toDegrees(atan2(bx - ax, -(by - ay))))

    /** Angle entre deux directions, de 0 à 180°. */
    fun ecartAngulaire(a: Double, b: Double): Double {
        val d = abs(Boussole.normaliser(a - b))
        return if (d > 180) 360 - d else d
    }

    fun distanceSol(a: ElementSol, b: ElementSol): Double = hypot(b.x - a.x, b.y - a.y)

    /** Champ horizontal d'une caméra du plan, en degrés ; null si sa focale ou son capteur manque. */
    fun champHorizontal(c: ElementSol): Double? {
        val f = c.focaleMm ?: return null
        val l = c.capteurLargeurMm ?: return null
        return Focales.angleDeChamp(l * c.desanamorphose, f)
    }

    fun champVertical(c: ElementSol): Double? {
        val f = c.focaleMm ?: return null
        val h = c.capteurHauteurMm ?: return null
        return Focales.angleDeChamp(h, f)
    }

    /** Les déplacements : chaque position reliée à la suivante, par type et par nom. */
    fun fleches(plan: PlanSol): List<Pair<ElementSol, ElementSol>> =
        plan.elements.filter { it.type.enMouvement && it.nom.isNotBlank() }
            .groupBy { it.type to it.nom.trim().lowercase() }
            .values
            .flatMap { groupe -> groupe.sortedBy { it.etape }.zipWithNext() }

    /** Le prochain nom de caméra libre : A, B, C… */
    fun prochaineCamera(plan: PlanSol): String {
        val pris = plan.elements.filter { it.type == TypeElement.CAMERA }.map { it.nom.trim().uppercase() }.toSet()
        return ('A'..'Z').map { it.toString() }.firstOrNull { it !in pris } ?: "Z"
    }

    /** L'intensité (cd) d'un projecteur du plan : celle de son projecteur choisi, sinon sa saisie directe. */
    fun intensite(e: ElementSol, choisi: Projecteur?): Double? {
        if (choisi != null) {
            val m = choisi.mesure(e.spot) ?: choisi.mesures.firstOrNull() ?: return null
            return Eclairage.intensite(m.lux, m.distanceM)
        }
        val l = e.luxReference ?: return null
        val d = e.distanceReference ?: return null
        return Eclairage.intensite(l, d)
    }

    fun faisceau(e: ElementSol, choisi: Projecteur?): Double? = choisi?.angle(e.spot) ?: e.faisceau

    /** Ce que chaque projecteur du plan apporte au [sujet]. */
    fun apports(plan: PlanSol, sujet: ElementSol, projecteurs: (String) -> Projecteur?): List<ApportLumiere> =
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.map { p ->
            val choisi = if (p.projecteur.isBlank()) null else projecteurs(p.projecteur)
            val dh = (p.hauteur ?: HAUTEUR_PROJECTEUR) - (sujet.hauteur ?: HAUTEUR_VISAGE)
            val sol = distanceSol(p, sujet)
            val d = sqrt(sol * sol + dh * dh)
            val versSujet = rotationVers(p.x, p.y, sujet.x, sujet.y)
            val horsAxe = ecartAngulaire(p.rotation, versSujet)
            val ouverture = faisceau(p, choisi)
            val dedans = ouverture == null || horsAxe <= ouverture / 2
            val versSource = rotationVers(sujet.x, sujet.y, p.x, p.y)
            val i = intensite(p, choisi)
            ApportLumiere(
                projecteur = p,
                distance = d,
                dansFaisceau = dedans,
                horsAxe = horsAxe,
                angleAuRegard = ecartAngulaire(sujet.rotation, versSource),
                lux = if (i != null && dedans) Eclairage.lux(i, d, perteDiaphs = p.pertesDiaphs) else if (i != null) 0.0 else null
            )
        }

    /** Où pointe le soleil sur le plan : son azimut, tourné du nord du plan. */
    fun soleilSurPlan(nordDeg: Double, azimut: Double): Double = Boussole.normaliser(nordDeg + azimut)

    /**
     * Heures bleues et dorées, par convention : heure dorée tant que le soleil
     * est entre −4° et +6°, heure bleue entre −6° et −4°. Calculées minute par
     * minute.
     */
    fun heuresDuJour(latitude: Double, longitude: Double, jourDeLAnnee: Int, fuseau: Double): HeuresDuJour {
        val midi = Soleil.midiSolaire(jourDeLAnnee, longitude, fuseau)
        fun hauteur(h: Double) = Soleil.hauteur(latitude, jourDeLAnnee, Soleil.tempsSolaire(h, jourDeLAnnee, longitude, fuseau))
        fun creneau(de: Double, a: Double, bas: Double, haut: Double): Creneau? {
            var debut: Double? = null
            var fin: Double? = null
            var t = de
            val pas = 1.0 / 60
            while (if (a > de) t <= a else t >= a) {
                val e = hauteur(t)
                if (e >= bas && e <= haut) {
                    if (debut == null) debut = t
                    fin = t
                }
                t += if (a > de) pas else -pas
            }
            val d = debut ?: return null
            val f = fin ?: return null
            return Creneau(minOf(d, f), maxOf(d, f))
        }
        return HeuresDuJour(
            bleueMatin = creneau(midi - 12, midi, -6.0, -4.0),
            doreeMatin = creneau(midi - 12, midi, -4.0, 6.0),
            doreeSoir = creneau(midi, midi + 12, -4.0, 6.0),
            bleueSoir = creneau(midi, midi + 12, -6.0, -4.0)
        )
    }

    /** « 18h42 », depuis des heures décimales. */
    fun libelleHeure(h: Double): String {
        val m = Math.round(((h % 24) + 24) % 24 * 60).toInt()
        return String.format(java.util.Locale.FRANCE, "%dh%02d", (m / 60) % 24, m % 60)
    }

    /** Les bornes du contenu, en mètres : gauche, haut, droite, bas — avec une marge. */
    fun bornes(plan: PlanSol, marge: Double = 1.0): DoubleArray {
        if (plan.elements.isEmpty()) return doubleArrayOf(-3.0, -3.0, 3.0, 3.0)
        var g = Double.MAX_VALUE
        var h = Double.MAX_VALUE
        var d = -Double.MAX_VALUE
        var b = -Double.MAX_VALUE
        plan.elements.forEach { e ->
            val r = maxOf(e.largeur, e.profondeur) / 2 + 0.3
            g = minOf(g, e.x - r); d = maxOf(d, e.x + r)
            h = minOf(h, e.y - r); b = maxOf(b, e.y + r)
        }
        return doubleArrayOf(g - marge, h - marge, d + marge, b + marge)
    }

    /** Angle d'inclinaison qui vise depuis [a] (à sa hauteur) le point de [b] à sa hauteur. */
    fun inclinaisonVers(a: ElementSol, hauteurA: Double, b: ElementSol, hauteurB: Double): Double =
        Math.toDegrees(atan2(hauteurB - hauteurA, distanceSol(a, b)))

    /** Cosinus d'un angle en degrés, pour les vues de côté. */
    fun cosd(d: Double) = cos(Math.toRadians(d))
    fun acosd(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
}
