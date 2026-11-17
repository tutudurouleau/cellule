package fr.cellule.core

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * Les calculs du plateau : de la physique, rien d'autre — l'éclairement qui
 * décroît comme le carré de la distance, le cosinus de l'angle d'arrivée,
 * les mireds qui s'additionnent, la puissance électrique, le battement des
 * sources sur le secteur. Les seules données (gélatines) portent leur source
 * et le drapeau « à relire » tant qu'elles n'ont pas été vérifiées sur la
 * fiche du fabricant.
 */

/* ── Unités ────────────────────────────────────────────────────────────── */

object Unites {
    /** 1 foot-candle = 1 lumen par pied carré. */
    const val LUX_PAR_FC = 10.763910417
    const val METRES_PAR_PIED = 0.3048

    fun fc(lux: Double) = lux / LUX_PAR_FC
    fun luxDepuisFc(fc: Double) = fc * LUX_PAR_FC
    fun pieds(m: Double) = m / METRES_PAR_PIED
    fun metres(pieds: Double) = pieds * METRES_PAR_PIED
}

/* ── Projecteurs ───────────────────────────────────────────────────────── */

data class Geometrie(
    /** Distance réelle source–sujet, en mètres. */
    val distance: Double,
    /** Inclinaison du projecteur sous l'horizontale, en degrés (négative : il vise vers le haut). */
    val inclinaison: Double
)

object Eclairage {

    /** L'intensité (en candelas) d'après un éclairement relevé dans l'axe : I = E · d². */
    fun intensite(lux: Double, distanceM: Double): Double = lux * distanceM * distanceM

    /**
     * L'éclairement à [distanceM] : E = I · cos(θ) / d², diminué de ce que
     * retiennent gélatines et diffusion ([perteDiaphs]).
     */
    fun lux(intensite: Double, distanceM: Double, incidenceDeg: Double = 0.0, perteDiaphs: Double = 0.0): Double {
        if (distanceM <= 0) return Double.NaN
        val c = cos(Math.toRadians(incidenceDeg.coerceIn(0.0, 89.9)))
        return intensite * c / (distanceM * distanceM) / 2.0.pow(perteDiaphs)
    }

    /** La distance qui donne [luxVoulu] : d = √(I · cos θ / E). */
    fun distancePour(intensite: Double, luxVoulu: Double, incidenceDeg: Double = 0.0, perteDiaphs: Double = 0.0): Double {
        if (luxVoulu <= 0) return Double.NaN
        val c = cos(Math.toRadians(incidenceDeg.coerceIn(0.0, 89.9)))
        return sqrt(intensite * c / 2.0.pow(perteDiaphs) / luxVoulu)
    }

    /** Diamètre de la tache de lumière à [distanceM] pour un faisceau de [angleDeg] : 2 · d · tan(θ/2). */
    fun diametre(angleDeg: Double, distanceM: Double): Double = 2 * distanceM * tan(Math.toRadians(angleDeg / 2))

    /** La distance où le faisceau couvre [largeurM]. */
    fun distancePourCouvrir(angleDeg: Double, largeurM: Double): Double =
        largeurM / (2 * tan(Math.toRadians(angleDeg / 2)))

    /** Distance réelle et inclinaison, d'après les hauteurs et le recul au sol. */
    fun geometrie(hauteurSource: Double, hauteurSujet: Double, recul: Double): Geometrie {
        val dh = hauteurSource - hauteurSujet
        return Geometrie(sqrt(recul * recul + dh * dh), Math.toDegrees(atan2(dh, recul)))
    }

    /** L'EV à 100 ISO que donne cet éclairement, mesuré en incident (C = 250). */
    fun ev100(lux: Double): Double = Photometrie.evDepuisLux(lux)

    /** L'éclairement qu'il faut pour exposer à [t] avec ce réglage de caméra. */
    fun luxPour(t: Double, reglage: ReglageTournage): Double {
        val ev = log2(t * t / reglage.tempsPose) - log2(reglage.ei / 100.0) + reglage.perteFiltres
        return Photometrie.lux(ev)
    }

    /** Diaphs ↔ rapport d'éclairement : 1 diaph = ×2. */
    fun rapport(diaphs: Double): Double = 2.0.pow(diaphs)
    fun diaphs(rapport: Double): Double = log2(rapport)

    /**
     * Repère : la loi en 1/d² ne décrit bien une source qu'à partir d'environ
     * cinq fois sa plus grande dimension.
     */
    fun assezLoin(distanceM: Double, tailleSourceM: Double): Boolean = distanceM >= 5 * tailleSourceM
}

/* ── Densités ──────────────────────────────────────────────────────────── */

object Densites {
    /** Un diaph = densité log10(2) ≈ 0,301 ; les neutres s'arrondissent à 0,3. */
    fun densite(diaphs: Double): Double = diaphs * log10(2.0)
    fun diaphsDeDensite(densite: Double): Double = densite / log10(2.0)
    fun transmission(diaphs: Double): Double = 2.0.pow(-diaphs)
    fun diaphsDeTransmission(transmission: Double): Double = -log2(transmission)

    /** T = f / √(transmission de l'objectif). */
    fun tStop(fNombre: Double, transmission: Double): Double = fNombre / sqrt(transmission)
    fun fNombre(tStop: Double, transmission: Double): Double = tStop * sqrt(transmission)
}

/* ── Couleur ───────────────────────────────────────────────────────────── */

enum class FamilleGelatine(val libelle: String) {
    VERS_CHAUD("Vers le chaud (CTO)"),
    VERS_FROID("Vers le froid (CTB)"),
    PLUS_VERT("Plus de vert"),
    MOINS_VERT("Moins de vert (magenta)")
}

/**
 * Une gélatine de correction. [mireds] est son décalage publié ; [relue] dit
 * si la valeur a été vérifiée sur la fiche du fabricant.
 */
data class Gelatine(
    val marque: String,
    val reference: String,
    val nom: String,
    val famille: FamilleGelatine,
    val mireds: Double? = null,
    /** Fraction de la correction pleine : 1, 1/2, 1/4, 1/8. */
    val fraction: String = "",
    val source: String,
    val relue: Boolean = false
) {
    val libelle: String get() = "$marque $reference $nom"
}

private const val LEE = "Lee Filters, décalage en mireds publié"
private const val ROSCO = "Rosco Cinegel, décalage en mireds publié"

val GELATINES: List<Gelatine> = listOf(
    Gelatine("Lee", "204", "Full CT Orange", FamilleGelatine.VERS_CHAUD, 159.0, "1", LEE),
    Gelatine("Lee", "205", "1/2 CT Orange", FamilleGelatine.VERS_CHAUD, 109.0, "1/2", LEE),
    Gelatine("Lee", "206", "1/4 CT Orange", FamilleGelatine.VERS_CHAUD, 64.0, "1/4", LEE),
    Gelatine("Lee", "223", "1/8 CT Orange", FamilleGelatine.VERS_CHAUD, 26.0, "1/8", LEE),
    Gelatine("Lee", "201", "Full CT Blue", FamilleGelatine.VERS_FROID, -137.0, "1", LEE),
    Gelatine("Lee", "202", "1/2 CT Blue", FamilleGelatine.VERS_FROID, -78.0, "1/2", LEE),
    Gelatine("Lee", "203", "1/4 CT Blue", FamilleGelatine.VERS_FROID, -35.0, "1/4", LEE),
    Gelatine("Lee", "218", "1/8 CT Blue", FamilleGelatine.VERS_FROID, -12.0, "1/8", LEE),
    Gelatine("Lee", "244", "Plus Green", FamilleGelatine.PLUS_VERT, null, "1", "Lee Filters"),
    Gelatine("Lee", "245", "1/2 Plus Green", FamilleGelatine.PLUS_VERT, null, "1/2", "Lee Filters"),
    Gelatine("Lee", "246", "1/4 Plus Green", FamilleGelatine.PLUS_VERT, null, "1/4", "Lee Filters"),
    Gelatine("Lee", "247", "Minus Green", FamilleGelatine.MOINS_VERT, null, "1", "Lee Filters"),
    Gelatine("Lee", "248", "1/2 Minus Green", FamilleGelatine.MOINS_VERT, null, "1/2", "Lee Filters"),
    Gelatine("Lee", "249", "1/4 Minus Green", FamilleGelatine.MOINS_VERT, null, "1/4", "Lee Filters"),
    Gelatine("Rosco", "3407", "Full CTO", FamilleGelatine.VERS_CHAUD, 167.0, "1", ROSCO),
    Gelatine("Rosco", "3408", "1/2 CTO", FamilleGelatine.VERS_CHAUD, 81.0, "1/2", ROSCO),
    Gelatine("Rosco", "3409", "1/4 CTO", FamilleGelatine.VERS_CHAUD, 42.0, "1/4", ROSCO),
    Gelatine("Rosco", "3410", "1/8 CTO", FamilleGelatine.VERS_CHAUD, 20.0, "1/8", ROSCO),
    Gelatine("Rosco", "3202", "Full CTB", FamilleGelatine.VERS_FROID, -131.0, "1", ROSCO),
    Gelatine("Rosco", "3204", "1/2 CTB", FamilleGelatine.VERS_FROID, -68.0, "1/2", ROSCO),
    Gelatine("Rosco", "3208", "1/4 CTB", FamilleGelatine.VERS_FROID, -30.0, "1/4", ROSCO),
    Gelatine("Rosco", "3216", "1/8 CTB", FamilleGelatine.VERS_FROID, -12.0, "1/8", ROSCO),
    Gelatine("Rosco", "3304", "Tough Plusgreen", FamilleGelatine.PLUS_VERT, null, "1", "Rosco Cinegel"),
    Gelatine("Rosco", "3315", "1/2 Tough Plusgreen", FamilleGelatine.PLUS_VERT, null, "1/2", "Rosco Cinegel"),
    Gelatine("Rosco", "3316", "1/4 Tough Plusgreen", FamilleGelatine.PLUS_VERT, null, "1/4", "Rosco Cinegel"),
    Gelatine("Rosco", "3308", "Tough Minusgreen", FamilleGelatine.MOINS_VERT, null, "1", "Rosco Cinegel"),
    Gelatine("Rosco", "3313", "1/2 Tough Minusgreen", FamilleGelatine.MOINS_VERT, null, "1/2", "Rosco Cinegel"),
    Gelatine("Rosco", "3314", "1/4 Tough Minusgreen", FamilleGelatine.MOINS_VERT, null, "1/4", "Rosco Cinegel")
)

/** Une combinaison de gélatines (une ou deux, éventuellement la même deux fois). */
data class Combinaison(val gelatines: List<Gelatine>, val mireds: Double, val residuel: Double, val kelvinsObtenus: Double)

object Mireds {
    fun depuisKelvins(k: Double): Double = 1e6 / k
    fun kelvins(mireds: Double): Double = 1e6 / mireds

    /** Le décalage à appliquer à la source pour atteindre la cible : positif, vers le chaud. */
    fun decalage(sourceK: Double, cibleK: Double): Double = depuisKelvins(cibleK) - depuisKelvins(sourceK)

    /** La température obtenue en posant ce décalage sur la source. */
    fun apres(sourceK: Double, decalage: Double): Double = kelvins(depuisKelvins(sourceK) + decalage)

    /**
     * Les meilleures combinaisons d'une ou deux gélatines d'une même marque
     * pour approcher [decalage] ; les gélatines sans valeur publiée sont
     * écartées. Triées par écart restant.
     */
    fun combinaisons(sourceK: Double, decalage: Double, marque: String, nombre: Int = 3): List<Combinaison> {
        val candidates = GELATINES.filter { it.marque == marque && it.mireds != null }
        val toutes = mutableListOf<List<Gelatine>>()
        candidates.forEach { toutes.add(listOf(it)) }
        for (i in candidates.indices) for (j in i until candidates.size) toutes.add(listOf(candidates[i], candidates[j]))
        return toutes
            .map { g ->
                val m = g.sumOf { it.mireds!! }
                Combinaison(g, m, decalage - m, apres(sourceK, m))
            }
            .filter { kotlin.math.sign(it.mireds) == kotlin.math.sign(decalage) || abs(decalage) < 5 }
            .sortedWith(compareBy({ abs(it.residuel) }, { it.gelatines.size }))
            .take(nombre)
    }
}

/* ── Électricité ───────────────────────────────────────────────────────── */

enum class Region(val libelle: String) { EUROPE("Europe (230 / 400 V)"), US("États-Unis (120 / 240 V)") }

/** Un calibre de circuit : tension, intensité, monophasé ou triphasé. */
data class Circuit(val nom: String, val region: Region, val tension: Double, val intensite: Double, val triphase: Boolean) {
    /** Puissance apparente du circuit, en VA : U·I, ou √3·U·I en triphasé (tension entre phases). */
    val puissance: Double get() = if (triphase) sqrt(3.0) * tension * intensite else tension * intensite
}

val CIRCUITS: List<Circuit> = listOf(
    Circuit("Prise domestique 16 A", Region.EUROPE, 230.0, 16.0, false),
    Circuit("P17 16 A mono", Region.EUROPE, 230.0, 16.0, false),
    Circuit("P17 32 A mono", Region.EUROPE, 230.0, 32.0, false),
    Circuit("P17 63 A mono", Region.EUROPE, 230.0, 63.0, false),
    Circuit("P17 16 A tri", Region.EUROPE, 400.0, 16.0, true),
    Circuit("P17 32 A tri", Region.EUROPE, 400.0, 32.0, true),
    Circuit("P17 63 A tri", Region.EUROPE, 400.0, 63.0, true),
    Circuit("P17 125 A tri", Region.EUROPE, 400.0, 125.0, true),
    Circuit("Prise 15 A", Region.US, 120.0, 15.0, false),
    Circuit("Prise 20 A", Region.US, 120.0, 20.0, false),
    Circuit("240 V 30 A", Region.US, 240.0, 30.0, false),
    Circuit("240 V 60 A", Region.US, 240.0, 60.0, false),
    Circuit("208 V tri 100 A", Region.US, 208.0, 100.0, true)
)

object Electricite {
    /** Repère de charge : au-delà de 80 % du calibre, on est trop près de la limite (règle NEC aux États-Unis pour les charges continues). */
    const val MARGE = 0.8

    /** Facteur de puissance nominal d'un groupe électrogène : sa puissance en kW vaut 0,8 × ses kVA. */
    const val COS_PHI_GROUPE = 0.8

    /** Intensité tirée par [puissanceW] sur ce circuit (réparti sur les phases en triphasé). */
    fun intensite(puissanceW: Double, circuit: Circuit, cosPhi: Double = 1.0): Double =
        if (circuit.triphase) puissanceW / (sqrt(3.0) * circuit.tension * cosPhi)
        else puissanceW / (circuit.tension * cosPhi)

    fun charge(puissanceW: Double, circuit: Circuit, cosPhi: Double = 1.0): Double =
        intensite(puissanceW, circuit, cosPhi) / circuit.intensite

    /** Les kVA minimum d'un groupe pour [puissanceW] de charge, sans marge. */
    fun kvaGroupe(puissanceW: Double): Double = puissanceW / COS_PHI_GROUPE / 1000.0
}

/* ── Scintillement ─────────────────────────────────────────────────────── */

enum class VerdictScintillement(val libelle: String) {
    SUR("Sûr"),
    SUR_SI_SYNCHRO("Sûr si la cadence est stable (quartz)"),
    RISQUE("Risque de battement")
}

object Scintillement {
    /**
     * Les sources sur secteur battent à deux fois sa fréquence (100 Hz en
     * Europe, 120 Hz aux États-Unis). Le temps de pose est sûr s'il contient
     * un nombre entier de battements ; sinon la cadence l'est encore si elle
     * divise exactement la fréquence de battement (chaque image commence au
     * même moment du cycle).
     */
    fun verdict(cadence: Double, angle: Double, secteurHz: Double): VerdictScintillement {
        val battement = 2 * secteurHz
        val k = Tournage.tempsPose(angle, cadence) * battement
        if (k >= 0.999 && abs(k - k.roundToInt()) < 0.01) return VerdictScintillement.SUR
        val r = battement / cadence
        if (abs(r - r.roundToInt()) < 0.001) return VerdictScintillement.SUR_SI_SYNCHRO
        return VerdictScintillement.RISQUE
    }

    /** Les angles d'obturateur qui contiennent un nombre entier de battements, à cette cadence. */
    fun anglesSurs(cadence: Double, secteurHz: Double): List<Double> {
        val pas = 360.0 * cadence / (2 * secteurHz)
        return generateSequence(pas) { it + pas }.takeWhile { it <= 360.0001 }.toList()
    }
}

/* ── Optique ───────────────────────────────────────────────────────────── */

data class ProfondeurDeChamp(val procheM: Double, val loinM: Double?, val hyperfocaleM: Double)

object Optique {
    /** Ce que couvre le cadre à [distanceM], le long d'une dimension du capteur : d · capteur / f. */
    fun couverture(distanceM: Double, focaleMm: Double, capteurMm: Double): Double = distanceM * capteurMm / focaleMm

    /** La focale qui cadre [largeurM] à [distanceM] sur cette dimension de capteur. */
    fun focalePour(largeurM: Double, distanceM: Double, capteurMm: Double): Double = capteurMm * distanceM / largeurM

    /** Cercle de confusion par convention : la diagonale du capteur divisée par 1500. */
    fun cercleConfusion(largeurMm: Double, hauteurMm: Double): Double = sqrt(largeurMm * largeurMm + hauteurMm * hauteurMm) / 1500.0

    fun hyperfocale(focaleMm: Double, ouverture: Double, cercleMm: Double): Double =
        (focaleMm * focaleMm / (ouverture * cercleMm) + focaleMm) / 1000.0

    /** Profondeur de champ, distances en mètres depuis le plan du capteur. */
    fun profondeur(focaleMm: Double, ouverture: Double, cercleMm: Double, distanceM: Double): ProfondeurDeChamp {
        val f = focaleMm
        val s = distanceM * 1000
        val h = f * f / (ouverture * cercleMm) + f
        val proche = s * (h - f) / (h + s - 2 * f)
        val loin = if (s < h) s * (h - f) / (h - s) else null
        return ProfondeurDeChamp(proche / 1000, loin?.let { it / 1000 }, h / 1000)
    }
}

/* ── Ambiances ─────────────────────────────────────────────────────────── */

/** Un repère de contraste face / débouchage (rapport de la face seule au débouchage seul). */
data class Ambiance(val nom: String, val rapportMin: Double, val rapportMax: Double, val idee: String) {
    val diaphsMin: Double get() = log2(rapportMin)
    val diaphsMax: Double get() = log2(rapportMax)
}

/** Des repères d'usage, pas des normes : chaque chef opérateur les déplace. */
val AMBIANCES: List<Ambiance> = listOf(
    Ambiance("High key", 1.0, 2.0, "presque pas d'ombre : comédie, publicité, plateau télé"),
    Ambiance("Classique", 2.0, 4.0, "le visage modelé sans être coupé en deux"),
    Ambiance("Dramatique", 4.0, 8.0, "une moitié du visage glisse dans l'ombre"),
    Ambiance("Low key", 8.0, 16.0, "film noir, clair-obscur : l'ombre domine"),
    Ambiance("Nuit", 16.0, 64.0, "presque tout dans le noir, quelques accents")
)
