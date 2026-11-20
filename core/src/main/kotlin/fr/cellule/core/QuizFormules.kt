package fr.cellule.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

/*
 * Des exercices sur les formules de Tables › Formules, pour les revoir de
 * tête : chaque question tire ses valeurs au hasard, propose quatre réponses
 * — la bonne et des erreurs typiques (oublier un carré, confondre l'incident
 * et le réfléchi, la distance et son carré…) — et donne le calcul en entier
 * une fois qu'on a répondu.
 *
 * Les formules sont exactement celles que l'appli affiche et utilise.
 */

enum class ThemeFormule(val titre: String, val detail: String) {
    EXPOSITION("Exposition", "EV, sensibilité, cellule, diaphs"),
    TOURNAGE("Tournage", "obturateur, T-stop, densités neutres"),
    LUMIERE("Lumière", "carré de la distance, intensité, rapports"),
    COULEUR("Couleur", "kelvins, mireds, gélatines"),
    OPTIQUE("Optique", "angle de champ, hyperfocale, profondeur"),
    OPTIQUE_GEO("Optique géométrique", "conjugaison, grandissement, ouverture"),
    SENSITOMETRIE("Sensitométrie", "densité, gamma, latitude, ISO"),
    NUMERIQUE("Numérique", "bits, dynamique, débit, échantillonnage"),
    ELECTRICITE("Électricité", "amperes, watts, groupes"),
    SCINTILLEMENT("Scintillement", "secteur, vitesses et angles sûrs")
}

data class QuestionFormule(
    val theme: ThemeFormule,
    /** La formule visée : « Loi du carré de la distance ». */
    val sujet: String,
    val enonce: String,
    val options: List<String>,
    val bonne: Int,
    /** L'indice qu'on peut demander avant de répondre ; vide quand la question porte sur la formule elle-même. */
    val formule: String,
    /** Le calcul détaillé, montré après la réponse. */
    val correction: String
)

object QuizFormules {

    /** Une question sur un thème, ou sur n'importe lequel quand [theme] est nul. */
    fun question(theme: ThemeFormule?, alea: Random = Random.Default): QuestionFormule {
        val t = theme ?: ThemeFormule.entries.random(alea)
        return generateurs(t).random(alea)(alea)
    }

    /** Une question sur une formule précise (son sujet), pour revoir ses erreurs. */
    fun questionSur(theme: ThemeFormule, sujet: String, alea: Random = Random.Default): QuestionFormule {
        repeat(400) {
            val q = question(theme, alea)
            if (q.sujet == sujet) return q
        }
        return question(theme, alea)
    }

    /** Toutes les façons de poser une question sur un thème. */
    fun generateurs(theme: ThemeFormule): List<(Random) -> QuestionFormule> = when (theme) {
        ThemeFormule.EXPOSITION -> listOf(::evCouple, ::sensibilite, ::incident, ::incidentInverse, ::reflechie, ::ouverturePourVitesse, ::diaphsRapport, ::fractionDiaph, reconnaitre(theme, RECO_EXPOSITION))
        ThemeFormule.TOURNAGE -> listOf(::obturateur, ::anglePourVitesse, ::ndVersDiaphs, ::diaphsVersNd, ::transmissionNd, ::tStopJuste, ::tEtF, reconnaitre(theme, RECO_TOURNAGE))
        ThemeFormule.LUMIERE -> listOf(::intensite, ::carreDistance, ::reculer, ::distancePourEclairement, ::cosinus, ::tache, ::geometrie, ::addition, ::multiplierSources, ::rapports, ::unites, reconnaitre(theme, RECO_LUMIERE))
        ThemeFormule.COULEUR -> listOf(::mired, ::decalage, ::gelatineAChoisir, ::perteGelatine, reconnaitre(theme, RECO_COULEUR))
        ThemeFormule.OPTIQUE -> listOf(::angleDeChamp, ::focalePourAngle, ::largeurCadre, ::cercleConfusion, ::hyperfocale, ::netteteHyperfocale, ::procheNet, reconnaitre(theme, RECO_OPTIQUE))
        ThemeFormule.OPTIQUE_GEO -> listOf(::conjugaison, ::tirage, ::grandissement, ::nombreOuverture, ::vergence, reconnaitre(theme, RECO_OPTIQUE_GEO))
        ThemeFormule.SENSITOMETRIE -> listOf(::densiteOptique, ::transmissionDensite, ::gamma, ::latitude, ::isoDin, reconnaitre(theme, RECO_SENSITOMETRIE))
        ThemeFormule.NUMERIQUE -> listOf(::niveaux, ::bitsPourNiveaux, ::dynamique, ::decibels, ::debitBrut, ::stockage, ::echantillonnage, reconnaitre(theme, RECO_NUMERIQUE))
        ThemeFormule.ELECTRICITE -> listOf(::monophase, ::puissancePrise, ::combienDeProjecteurs, ::triphase, ::groupe, reconnaitre(theme, RECO_ELECTRICITE))
        ThemeFormule.SCINTILLEMENT -> listOf(::battement, ::vitesseSure, ::angleSur, ::cadenceSure, reconnaitre(theme, RECO_SCINTILLEMENT))
    }
}

/* ── Mise en forme ─────────────────────────────────────────────────────── */

/** Un nombre à la française : virgule, espace insécable des milliers, sans zéros inutiles. */
internal fun nb(x: Double, decimales: Int = 0): String {
    if (x.isNaN() || x.isInfinite()) return "—"
    val bd = BigDecimal.valueOf(x).setScale(decimales, RoundingMode.HALF_UP).stripTrailingZeros()
    if (bd.signum() == 0) return "0"
    val s = bd.abs().toPlainString()
    val entier = s.substringBefore('.')
    val reste = s.substringAfter('.', "")
    val groupe = if (entier.length >= 4) entier.reversed().chunked(3).joinToString(" ").reversed() else entier
    return (if (bd.signum() < 0) "−" else "") + groupe + (if (reste.isNotEmpty()) ",$reste" else "")
}

private fun nb(x: Int): String = nb(x.toDouble())
private fun diaphs(k: Double): String = nb(k, 1) + if (abs(k) < 2) " diaph" else " diaphs"
private fun diaphs(k: Int): String = diaphs(k.toDouble())
private fun lux(e: Double): String = nb(e, if (e < 10) 1 else 0) + " lx"
private fun metres(d: Double, decimales: Int = 1): String = nb(d, decimales) + " m"
private fun degres(a: Double, decimales: Int = 1): String = nb(a, decimales) + "°"
private fun pourcent(x: Double, decimales: Int = 0): String = nb(x, decimales) + " %"
private fun amperes(i: Double): String = nb(i, 1) + " A"
private fun fStop(n: Double): String = "f/" + nb(n, 1)
private fun tStop(t: Double): String = "T" + nb(t, 1)
private fun vitesse(denominateur: Double): String = "1/" + nb(denominateur, 1) + " s"
private fun signe(x: Double): String = (if (x > 0.5) "+" else "") + nb(x, 0)
private fun deg(rad: Double): Double = Math.toDegrees(rad)
private fun rad(deg: Double): Double = Math.toRadians(deg)

/**
 * Assemble une question à choix : la bonne réponse et trois fausses, prises
 * dans l'ordre parmi [faux] (les doublons et la bonne réponse écartés).
 */
private fun qcm(
    theme: ThemeFormule,
    sujet: String,
    formule: String,
    enonce: String,
    juste: String,
    faux: List<String>,
    correction: String,
    alea: Random
): QuestionFormule {
    val autres = faux.filter { it != juste }.distinct()
    check(autres.size >= 3) { "$sujet : pas assez de fausses réponses pour « $enonce » ($juste, $autres)" }
    val options = (autres.take(3) + juste).shuffled(alea)
    return QuestionFormule(theme, sujet, enonce, options, options.indexOf(juste), formule, correction)
}

/** Les ouvertures pleines gravées sur les bagues. */
private val OUVERTURES_QUIZ = listOf(1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0)
private val VITESSES_QUIZ = listOf(15, 30, 60, 125, 250, 500, 1000)

/** L'ouverture pleine la plus proche, en diaphs. */
private fun ouverturePleine(n: Double): Int = OUVERTURES_QUIZ.indices.minByOrNull { abs(log2(OUVERTURES_QUIZ[it]) - log2(n)) }!!

/** Les ouvertures voisines d'une ouverture pleine : les fausses réponses naturelles. */
private fun voisines(i: Int, format: (Double) -> String): List<String> =
    listOf(i + 1, i - 1, i + 2, i - 2).mapNotNull { OUVERTURES_QUIZ.getOrNull(it) }.map(format)

/* ── Exposition ────────────────────────────────────────────────────────── */

private val TH_EXPO = ThemeFormule.EXPOSITION

private fun evCouple(a: Random): QuestionFormule {
    val n = OUVERTURES_QUIZ.random(a)
    val v = VITESSES_QUIZ.random(a)
    val ev = round(log2(n * n * v)).toInt()
    val oubliCarre = round(log2(n * v)).toInt()
    return qcm(
        TH_EXPO, "Indice de lumination", "EV = log₂(N² / t)",
        "À ${fStop(n)} et 1/$v s, quel est l'EV (à 100 ISO) ?",
        "EV $ev", listOf(oubliCarre, ev + 1, ev - 1, ev + 2, ev - 2).map { "EV $it" },
        "EV = log₂(N² / t) = log₂(${nb(n, 1)}² × $v) = log₂(${nb(n * n * v)}) ≈ $ev.", a
    )
}

private fun sensibilite(a: Random): QuestionFormule {
    val depart = listOf(100, 200, 400).random(a)
    val k = (1..4).random(a)
    val r = 1 shl k
    val arrivee = depart * r
    return qcm(
        TH_EXPO, "Sensibilité", "EV(ISO) = EV₁₀₀ + log₂(ISO / 100)",
        "Tu passes de $depart à ${nb(arrivee)} ISO : combien de diaphs gagnes-tu ?",
        diaphs(k), listOf(r, k + 1, k - 1, k + 2, 2 * k).map { diaphs(it) },
        "Chaque doublement de sensibilité gagne un diaph : log₂(${nb(arrivee)} / $depart) = log₂($r) = $k.", a
    )
}

private fun incident(a: Random): QuestionFormule {
    val ev = (6..15).random(a)
    val e = 2.5 * 2.0.pow(ev)
    return qcm(
        TH_EXPO, "Cellule incidente", "E (lux) = 2,5 × 2^EV₁₀₀",
        "EV $ev à 100 ISO : quel éclairement tombe sur le sujet ?",
        lux(e), listOf(e * 2, e / 2, 12.5 * 2.0.pow(ev), e * 4).map(::lux),
        "E = 2,5 × 2^$ev = 2,5 × ${nb(2.0.pow(ev))} = ${lux(e)}.", a
    )
}

private fun incidentInverse(a: Random): QuestionFormule {
    val ev = (6..15).random(a)
    val e = 2.5 * 2.0.pow(ev)
    return qcm(
        TH_EXPO, "Cellule incidente", "E (lux) = 2,5 × 2^EV₁₀₀",
        "Ta cellule incidente lit ${lux(e)} : quel EV à 100 ISO ?",
        "EV $ev", listOf(ev + 1, ev - 1, ev + 2, ev - 2).map { "EV $it" },
        "EV = log₂(E / 2,5) = log₂(${nb(e)} / 2,5) = log₂(${nb(2.0.pow(ev))}) = $ev.", a
    )
}

private fun reflechie(a: Random): QuestionFormule {
    val ev = (8..15).random(a)
    val l = 0.125 * 2.0.pow(ev)
    fun cd(x: Double) = nb(x, 0) + " cd/m²"
    return qcm(
        TH_EXPO, "Cellule réfléchie", "L (cd/m²) = 0,125 × 2^EV₁₀₀",
        "EV $ev à 100 ISO : quelle luminance moyenne lit la cellule réfléchie ?",
        cd(l), listOf(cd(2.5 * 2.0.pow(ev)), cd(l * 2), cd(l / 2), cd(l * 8)),
        "L = 0,125 × 2^$ev = 2^$ev / 8 = ${cd(l)}. (2,5 × 2^EV, c'est l'incident, en lux.)", a
    )
}

private fun ouverturePourVitesse(a: Random): QuestionFormule {
    val n = OUVERTURES_QUIZ[(1..7).random(a)]
    val v = VITESSES_QUIZ.random(a)
    val ev = round(log2(n * n * v)).toInt()
    val exacte = sqrt(2.0.pow(ev) / v)
    val i = ouverturePleine(exacte)
    return qcm(
        TH_EXPO, "Ouverture pour une vitesse", "N = √(2^EV × t)",
        "EV $ev, tu poses au 1/$v s : quelle ouverture ?",
        fStop(OUVERTURES_QUIZ[i]), voisines(i, ::fStop),
        "N = √(2^EV × t) = √(${nb(2.0.pow(ev))} / $v) ≈ ${nb(exacte, 1)} : ${fStop(OUVERTURES_QUIZ[i])}.", a
    )
}

private fun diaphsRapport(a: Random): QuestionFormule {
    val depart = listOf(100, 200, 250, 400, 500, 800).random(a)
    val k = (1..4).random(a)
    val r = 1 shl k
    val monte = a.nextBoolean()
    val arrivee = if (monte) depart.toDouble() * r else depart.toDouble() / r
    return qcm(
        TH_EXPO, "Diaphs", "écart (diaphs) = log₂(rapport)",
        "L'éclairement passe de $depart à ${lux(arrivee)} : de combien de diaphs ?",
        diaphs(k), listOf(r, k + 1, k - 1, k + 2).map { diaphs(it) },
        "écart = log₂(rapport) = log₂($r) = $k diaph${if (k > 1) "s" else ""} ${if (monte) "de plus" else "de moins"}. Chaque diaph double ou divise la lumière par deux.", a
    )
}

private fun fractionDiaph(a: Random): QuestionFormule {
    val cas = listOf(
        Triple("un tiers de diaph", "1/3", 2.0.pow(1.0 / 3)),
        Triple("un demi-diaph", "1/2", sqrt(2.0)),
        Triple("deux tiers de diaph", "2/3", 2.0.pow(2.0 / 3)),
        Triple("un diaph et demi", "1,5", 2.0.pow(1.5))
    )
    val (nom, exposant, r) = cas.random(a)
    fun fois(x: Double) = "×" + nb(x, 2)
    return qcm(
        TH_EXPO, "Diaphs", "rapport = 2^diaphs",
        "À quel rapport de lumière correspond $nom ?",
        fois(r), cas.map { fois(it.third) } + listOf("×1,33", "×1,5", "×1,12"),
        "rapport = 2^diaphs = 2^($exposant) ≈ ${fois(r)}.", a
    )
}

/* ── Tournage ──────────────────────────────────────────────────────────── */

private val TH_TOURNAGE = ThemeFormule.TOURNAGE

private fun obturateur(a: Random): QuestionFormule {
    val (angle, cadence) = listOf(
        180.0 to 25, 180.0 to 24, 172.8 to 24, 90.0 to 25, 90.0 to 24,
        45.0 to 25, 360.0 to 25, 270.0 to 24, 144.0 to 25, 360.0 to 24
    ).random(a)
    val c = cadence.toDouble()
    val den = 360.0 * c / angle
    return qcm(
        TH_TOURNAGE, "Obturateur", "t = angle / (360 × cadence)",
        "Obturateur à ${degres(angle)} et $cadence im/s : quel temps de pose ?",
        vitesse(den), listOf(c, 2 * c, 4 * c, den * 2, den / 2, den * 4, 3 * c).map(::vitesse),
        "t = angle / (360 × cadence) = ${nb(angle, 1)} / (360 × $cadence) = ${vitesse(den)}.", a
    )
}

private fun anglePourVitesse(a: Random): QuestionFormule {
    val (cadence, den) = listOf(25 to 50, 25 to 100, 24 to 48, 24 to 96, 25 to 200, 24 to 50, 25 to 25).random(a)
    val angle = 360.0 * cadence / den
    return qcm(
        TH_TOURNAGE, "Obturateur", "t = angle / (360 × cadence)",
        "À $cadence im/s, quel angle d'obturateur donne 1/$den s ?",
        degres(angle), listOf(angle * 2, angle / 2, 180.0, 90.0, 360.0, 172.8).map { degres(it) },
        "angle = 360 × cadence × t = 360 × $cadence / $den = ${degres(angle)}.", a
    )
}

private fun ndVersDiaphs(a: Random): QuestionFormule {
    val k = (1..6).random(a)
    val d = 0.3 * k
    return qcm(
        TH_TOURNAGE, "Densité neutre", "densité ≈ 0,3 × diaphs",
        "Un filtre ND ${nb(d, 1)} retire combien de diaphs ?",
        diaphs(k), listOf(k + 1, k - 1, 3 * k, k + 2).map { diaphs(it) },
        "densité ≈ 0,3 × diaphs, donc diaphs = ${nb(d, 1)} / 0,3 = $k.", a
    )
}

private fun diaphsVersNd(a: Random): QuestionFormule {
    val k = (1..5).random(a)
    fun nd(x: Double) = "ND " + nb(x, 1)
    return qcm(
        TH_TOURNAGE, "Densité neutre", "densité ≈ 0,3 × diaphs",
        "Tu dois retirer $k diaph${if (k > 1) "s" else ""} : quel filtre neutre ?",
        nd(0.3 * k), listOfNotNull(0.3 * (k + 1), if (k > 1) 0.3 * (k - 1) else null, 0.1 * k, 0.6 * k, 0.2 * k, 0.3 * (k + 2)).map(::nd),
        "densité ≈ 0,3 × diaphs = 0,3 × $k = ${nb(0.3 * k, 1)}.", a
    )
}

private fun transmissionNd(a: Random): QuestionFormule {
    val k = (1..4).random(a)
    val d = 0.3 * k
    fun part(n: Int) = pourcent(100.0 / 2.0.pow(n), 2)
    return qcm(
        TH_TOURNAGE, "Densité neutre", "transmission = 2^−diaphs",
        "Quelle part de la lumière laisse passer un ND ${nb(d, 1)} ?",
        part(k), listOf(part(k + 1), part(k - 1), pourcent(100 - 30.0 * k), pourcent(d * 100), part(k + 2)).filter { !it.startsWith("−") },
        "Un ND ${nb(d, 1)} retire $k diaph${if (k > 1) "s" else ""} : transmission = 2^−$k = ${part(k)}.", a
    )
}

private fun tStopJuste(a: Random): QuestionFormule {
    val t = OUVERTURES_QUIZ[(1..7).random(a)]
    val cadence = listOf(24, 25).random(a)
    val den = 2.0 * cadence
    val ei = listOf(200, 400, 800, 1600).random(a)
    val pertes = (0..2).random(a)
    val gainEi = log2(ei / 100.0)
    val ev = round(log2(t * t * den) - gainEi + pertes).toInt()
    val exacte = sqrt(2.0.pow(ev + gainEi - pertes) / den)
    val i = ouverturePleine(exacte)
    val filtre = if (pertes > 0) ", ND ${nb(0.3 * pertes, 1)} devant l'objectif" else ""
    return qcm(
        TH_TOURNAGE, "T-stop qui expose juste", "T = √(2^(EV₁₀₀ + log₂(EI/100) − pertes) × t)",
        "EV $ev à 100 ISO, EI $ei, 180° à $cadence im/s$filtre : quel T-stop ?",
        tStop(OUVERTURES_QUIZ[i]), voisines(i, ::tStop),
        "t = 1/${nb(den)} s. T = √(2^($ev + ${nb(gainEi)} − $pertes) / ${nb(den)}) = √(${nb(2.0.pow(ev + gainEi - pertes))} / ${nb(den)}) ≈ ${nb(exacte, 1)} : ${tStop(OUVERTURES_QUIZ[i])}.", a
    )
}

private fun tEtF(a: Random): QuestionFormule {
    val n = listOf(1.4, 2.0, 2.8, 4.0).random(a)
    val tau = listOf(70, 80, 90).random(a)
    val p = tau / 100.0
    val t = n / sqrt(p)
    return qcm(
        TH_TOURNAGE, "T et f", "T = f / √(transmission)",
        "Un objectif ouvert à ${fStop(n)} transmet $tau % de la lumière : quel est son T-stop ?",
        tStop(t), listOf(n, n * p, n / p, n * sqrt(p), t * 1.4).map(::tStop),
        "T = f / √transmission = ${nb(n, 1)} / √${nb(p, 2)} ≈ ${nb(t, 2)} : ${tStop(t)}. Le T est toujours un peu plus fermé que le f.", a
    )
}

/* ── Lumière ───────────────────────────────────────────────────────────── */

private val TH_LUMIERE = ThemeFormule.LUMIERE

private fun intensite(a: Random): QuestionFormule {
    val e0 = listOf(500, 1000, 2000, 4000).random(a).toDouble()
    val d0 = (2..4).random(a).toDouble()
    val i = e0 * d0 * d0
    fun cd(x: Double) = nb(x, 0) + " cd"
    return qcm(
        TH_LUMIERE, "Intensité d'une source", "I (cd) = E₀ × d₀²",
        "Un projecteur donne ${lux(e0)} à ${nb(d0)} m dans l'axe : quelle est son intensité ?",
        cd(i), listOf(e0 * d0, e0 / (d0 * d0), i * 2, e0 * d0 * d0 * d0).map(::cd),
        "I = E₀ × d₀² = ${nb(e0)} × ${nb(d0)}² = ${cd(i)}.", a
    )
}

private fun carreDistance(a: Random): QuestionFormule {
    val e1 = listOf(400, 800, 1600, 3200).random(a).toDouble()
    val d1 = listOf(1.0, 2.0, 3.0).random(a)
    val k = listOf(2.0, 0.5, 3.0).random(a)
    val d2 = d1 * k
    val e2 = e1 / (k * k)
    return qcm(
        TH_LUMIERE, "Loi du carré de la distance", "E = I × cos θ / d²",
        "Un projecteur donne ${lux(e1)} à ${metres(d1)}. Combien à ${metres(d2)} ?",
        lux(e2), listOf(e1 / k, e1 * k * k, e1 / (k * k * k), e1).map(::lux),
        "E₂ = E₁ × (d₁ / d₂)² = ${nb(e1)} × (${nb(d1, 1)} / ${nb(d2, 1)})² = ${lux(e2)}. " + (if (k > 1) "Distance ×${nb(k, 1)}, lumière ÷${nb(k * k, 2)}." else "Distance ÷${nb(1 / k, 1)}, lumière ×${nb(1 / (k * k), 2)}."), a
    )
}

private fun reculer(a: Random): QuestionFormule {
    val (d1, d2) = listOf(2.0 to 2.8, 1.0 to 1.4, 2.0 to 4.0, 1.0 to 2.0, 1.0 to 2.8, 1.0 to 4.0, 2.0 to 8.0).random(a)
    val k = round(2 * log2(d2 / d1)).toInt()
    return qcm(
        TH_LUMIERE, "Loi du carré de la distance", "E = I × cos θ / d²",
        "Tu recules un projecteur de ${metres(d1)} à ${metres(d2)} : combien de diaphs perds-tu au sujet ?",
        diaphs(k), listOf(k / 2.0, k + 1.0, k - 1.0, k * 2.0).map(::diaphs),
        "Perte = 2 × log₂(d₂ / d₁) = 2 × log₂(${nb(d2 / d1, 2)}) ≈ $k. Reculer de ×1,4 (√2) coûte un diaph ; doubler la distance en coûte deux.", a
    )
}

private fun distancePourEclairement(a: Random): QuestionFormule {
    val d = listOf(2.0, 2.5, 4.0, 5.0).random(a)
    val e = listOf(400, 625, 1000, 1600).random(a).toDouble()
    val i = e * d * d
    return qcm(
        TH_LUMIERE, "Distance pour un éclairement", "d = √(I × cos θ / E)",
        "Un projecteur de ${nb(i)} cd : à quelle distance donne-t-il ${lux(e)} ?",
        metres(d), listOf(d * d, d / 2, d * sqrt(2.0), d * d / 2, d * 2).map { metres(it) },
        "d = √(I / E) = √(${nb(i)} / ${nb(e)}) = √${nb(d * d, 2)} = ${metres(d)}.", a
    )
}

private fun cosinus(a: Random): QuestionFormule {
    val theta = listOf(30, 45, 60).random(a)
    val c = cos(rad(theta.toDouble()))
    return qcm(
        TH_LUMIERE, "Loi du carré de la distance", "E = I × cos θ / d²",
        "La lumière arrive à $theta° de la face du sujet (0° = de face) : quelle part de l'éclairement reste ?",
        pourcent(c * 100), listOf(100.0, 87.0, 71.0, 50.0, 33.0).map { pourcent(it) },
        "E est multiplié par cos θ : cos $theta° ≈ ${nb(c, 2)}, il reste ${pourcent(c * 100)}.", a
    )
}

private fun tache(a: Random): QuestionFormule {
    val faisceau = listOf(20, 30, 40, 50, 60).random(a).toDouble()
    val d = (2..5).random(a).toDouble()
    val diametre = 2 * d * tan(rad(faisceau / 2))
    return qcm(
        TH_LUMIERE, "Tache de lumière", "D = 2 × d × tan(faisceau / 2)",
        "Faisceau de ${nb(faisceau)}° à ${nb(d)} m : quel diamètre fait la tache de lumière ?",
        metres(diametre), listOf(2 * d * tan(rad(faisceau)), diametre / 2, diametre * 2, d * tan(rad(faisceau / 2)), diametre * 1.5).map { metres(it) },
        "D = 2 × d × tan(faisceau / 2) = 2 × ${nb(d)} × tan(${nb(faisceau / 2)}°) ≈ ${metres(diametre, 2)}.", a
    )
}

private fun geometrie(a: Random): QuestionFormule {
    val (recul, dh, d) = listOf(
        Triple(2.0, 1.5, 2.5), Triple(2.4, 1.8, 3.0), Triple(1.6, 1.2, 2.0),
        Triple(3.2, 2.4, 4.0), Triple(1.2, 0.9, 1.5), Triple(4.0, 3.0, 5.0)
    ).random(a)
    val sujet = 1.6
    val hp = sujet + dh
    return qcm(
        TH_LUMIERE, "Géométrie", "d = √(recul² + (h_projecteur − h_sujet)²)",
        "Projecteur à ${metres(hp)} de haut, visage à 1,6 m, ${metres(recul)} de recul au sol : quelle distance projecteur–visage ?",
        metres(d), listOf(recul + dh, recul, sqrt(recul * recul + hp * hp), hp).map { metres(it) },
        "Δh = ${nb(hp, 1)} − 1,6 = ${nb(dh, 1)} m. d = √(${nb(recul, 1)}² + ${nb(dh, 1)}²) = ${metres(d)}.", a
    )
}

private fun addition(a: Random): QuestionFormule {
    val e1 = listOf(200, 400, 600, 800).random(a).toDouble()
    val e2 = listOf(100, 200, 400).random(a).toDouble()
    return qcm(
        TH_LUMIERE, "Plusieurs sources", "E_total = E₁ + E₂ + …",
        "Deux projecteurs donnent ${lux(e1)} et ${lux(e2)} sur le même visage : quel éclairement total ?",
        lux(e1 + e2), listOf(maxOf(e1, e2), (e1 + e2) / 2, sqrt(e1 * e1 + e2 * e2), 2 * (e1 + e2)).map(::lux),
        "Les éclairements s'additionnent : ${nb(e1)} + ${nb(e2)} = ${lux(e1 + e2)}. Les diaphs, eux, ne s'additionnent pas.", a
    )
}

private fun multiplierSources(a: Random): QuestionFormule {
    val n = listOf(2, 4, 8).random(a)
    val k = round(log2(n.toDouble())).toInt()
    return qcm(
        TH_LUMIERE, "Plusieurs sources", "écart (diaphs) = log₂(rapport)",
        "Tu remplaces un projecteur par $n projecteurs identiques, au même endroit : combien de diaphs gagnes-tu ?",
        diaphs(k), listOf(n.toDouble(), n / 2.0, k + 1.0, k * 2.0, k - 1.0, k + 2.0, 0.5).map(::diaphs),
        "Lumière ×$n : log₂($n) = $k diaph${if (k > 1) "s" else ""} de plus.", a
    )
}

private fun rapports(a: Random): QuestionFormule {
    val deb = listOf(100, 200, 250).random(a)
    val fd = listOf(1, 2, 3, 4, 7).random(a)
    val face = deb * fd
    fun r(x: Int) = "$x:1"
    return qcm(
        TH_LUMIERE, "Rapports", "côté éclairé / côté ombre = (E_face + E_déb) / E_déb",
        "Face ${nb(face)} lx, débouchage $deb lx : quel rapport entre le côté éclairé et le côté ombre ?",
        r(fd + 1), listOf(fd, fd + 2, 2 * fd, fd + 3, 2 * fd + 1).map(::r),
        "Le côté éclairé reçoit la face et le débouchage, le côté ombre le débouchage seul : (${nb(face)} + $deb) / $deb = ${r(fd + 1)}. Face / débouchage seul, c'est ${r(fd)}.", a
    )
}

private fun unites(a: Random): QuestionFormule {
    return if (a.nextBoolean()) {
        val l = listOf(100, 500, 1000, 2000, 5000).random(a).toDouble()
        fun fc(x: Double) = nb(x, if (x < 10) 1 else 0) + " fc"
        qcm(
            TH_LUMIERE, "Unités", "1 foot-candle = 10,764 lux",
            "${lux(l)}, c'est combien de foot-candles ?",
            fc(l / 10.764), listOf(l * 10.764, l / 10, l / 3.28, l / 100).map(::fc),
            "1 fc = 10,764 lux, donc ${nb(l)} / 10,764 ≈ ${fc(l / 10.764)}.", a
        )
    } else {
        val p = listOf(5, 10, 12, 20, 25).random(a).toDouble()
        qcm(
            TH_LUMIERE, "Unités", "1 pied = 0,3048 m",
            "${nb(p)} pieds, c'est combien de mètres ?",
            metres(p * 0.3048, 2), listOf(p / 0.3048, p * 0.25, p * 0.5, p * 0.4).map { metres(it, 2) },
            "1 pied = 0,3048 m, donc ${nb(p)} × 0,3048 ≈ ${metres(p * 0.3048, 2)}.", a
        )
    }
}

/* ── Couleur ───────────────────────────────────────────────────────────── */

private val TH_COULEUR = ThemeFormule.COULEUR
private val KELVINS_QUIZ = listOf(2000, 2700, 3200, 4300, 5600, 6500, 10000)

private fun mired(a: Random): QuestionFormule {
    val k = KELVINS_QUIZ.random(a)
    fun m(x: Int) = nb(1e6 / x) + " mireds"
    return qcm(
        TH_COULEUR, "Mired", "mired = 1 000 000 / K",
        "Combien de mireds pour une source à ${nb(k)} K ?",
        m(k), KELVINS_QUIZ.filter { it != k }.shuffled(a).map(::m) + listOf(nb(k / 10.0) + " mireds"),
        "mired = 1 000 000 / ${nb(k)} ≈ ${nb(1e6 / k)}.", a
    )
}

private fun decalage(a: Random): QuestionFormule {
    val (source, cible) = listOf(5600 to 3200, 3200 to 5600, 4300 to 3200, 3200 to 4300, 5600 to 4300, 2700 to 3200, 6500 to 5600).random(a)
    val d = 1e6 / cible - 1e6 / source
    fun m(x: Double) = signe(x) + " mireds"
    val sens = if (d > 0) "vers le chaud : une gélatine orange (CTO)" else "vers le froid : une gélatine bleue (CTB)"
    return qcm(
        TH_COULEUR, "Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source",
        "Une lumière à ${nb(source)} K doit passer à ${nb(cible)} K : quel décalage en mireds ?",
        m(d), listOf(-d, (cible - source).toDouble(), d / 2, d * 2).map(::m),
        "Δ = 10⁶/${nb(cible)} − 10⁶/${nb(source)} = ${nb(1e6 / cible)} − ${nb(1e6 / source)} = ${m(d)}, $sens.", a
    )
}

private fun gelatineAChoisir(a: Random): QuestionFormule {
    val gelatines = listOf("CTO (orange)", "CTB (bleue)", "ND (neutre)", "Plus Green (verte)")
    return when ((0..2).random(a)) {
        0 -> qcm(
            TH_COULEUR, "Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source",
            "Un projecteur tungstène (3 200 K) doit se raccorder à la lumière du jour (5 600 K) : quelle gélatine sur le projecteur ?",
            "CTB (bleue)", gelatines,
            "Δ = 10⁶/5 600 − 10⁶/3 200 ≈ −134 mireds : négatif, vers le froid. Une CTB bleuit le tungstène.", a
        )
        1 -> qcm(
            TH_COULEUR, "Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source",
            "Une fenêtre (5 600 K) doit se raccorder à des projecteurs tungstène (3 200 K) : quelle gélatine sur la fenêtre ?",
            "CTO (orange)", gelatines,
            "Δ = 10⁶/3 200 − 10⁶/5 600 ≈ +134 mireds : positif, vers le chaud. Une CTO réchauffe la lumière du jour.", a
        )
        else -> qcm(
            TH_COULEUR, "Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source",
            "Un décalage positif en mireds pousse la lumière vers…",
            "le chaud (orange)", listOf("le froid (bleu)", "le vert", "le magenta", "le neutre"),
            "Positif : vers le chaud (orange, CTO). Négatif : vers le froid (bleu, CTB).", a
        )
    }
}

private fun perteGelatine(a: Random): QuestionFormule {
    val tau = listOf(50.0, 25.0, 70.0, 35.0, 12.5, 85.0).random(a)
    val k = -log2(tau / 100)
    val kArrondi = round(k * 10) / 10
    return qcm(
        TH_COULEUR, "Perte d'une gélatine", "diaphs = −log₂(transmission)",
        "Une gélatine transmet ${pourcent(tau, 1)} de la lumière : combien de diaphs perds-tu ?",
        diaphs(kArrondi), listOf(kArrondi + 0.5, kArrondi - 0.5, kArrondi * 2, kArrondi + 1, kArrondi + 1.5, kArrondi + 2).filter { it > 0.05 }.map(::diaphs),
        "diaphs = −log₂(${nb(tau / 100, 3)}) ≈ ${nb(k, 2)}.", a
    )
}

/* ── Optique ───────────────────────────────────────────────────────────── */

private val TH_OPTIQUE = ThemeFormule.OPTIQUE
private val CAPTEURS_QUIZ = listOf(36.0 to "plein format (36 mm de large)", 24.9 to "Super 35 (24,9 mm de large)")

private fun angleDeChamp(a: Random): QuestionFormule {
    val (cap, nom) = CAPTEURS_QUIZ.random(a)
    val f = listOf(18, 25, 35, 50, 85, 100).random(a).toDouble()
    val alpha = deg(2 * atan(cap / (2 * f)))
    return qcm(
        TH_OPTIQUE, "Angle de champ", "α = 2 × atan(dimension / (2 × f))",
        "Capteur $nom, objectif de ${nb(f)} mm : quel angle de champ horizontal ?",
        degres(alpha, 0), listOf(deg(2 * atan(cap / f)), alpha / 2, alpha * 1.5, alpha * 0.7).map { degres(it, 0) },
        "α = 2 × atan(${nb(cap, 1)} / (2 × ${nb(f)})) = 2 × atan(${nb(cap / (2 * f), 3)}) ≈ ${degres(alpha)}.", a
    )
}

private fun focalePourAngle(a: Random): QuestionFormule {
    val (cap, nom) = CAPTEURS_QUIZ.random(a)
    val alpha = listOf(90, 60, 40, 20).random(a).toDouble()
    val f = cap / (2 * tan(rad(alpha / 2)))
    fun mm(x: Double) = nb(x, 0) + " mm"
    return qcm(
        TH_OPTIQUE, "Focale pour un angle", "f = dimension / (2 × tan(α / 2))",
        "Quelle focale donne ${nb(alpha)}° de champ horizontal sur un capteur $nom ?",
        mm(f), listOf(f * 2, f / 2, f * 1.4, f / 1.4).map(::mm),
        "f = ${nb(cap, 1)} / (2 × tan(${nb(alpha / 2)}°)) ≈ ${nb(f, 1)} mm.", a
    )
}

private fun largeurCadre(a: Random): QuestionFormule {
    val (cap, nom) = CAPTEURS_QUIZ.random(a)
    val f = listOf(25, 35, 50, 85).random(a).toDouble()
    val d = listOf(2, 3, 4, 5, 8).random(a).toDouble()
    val l = d * cap / f
    val dec = if (l < 1) 2 else 1
    return qcm(
        TH_OPTIQUE, "Ce que couvre le cadre", "largeur = distance × capteur / f",
        "Capteur $nom, objectif de ${nb(f)} mm, sujet à ${nb(d)} m : quelle largeur de décor entre dans le cadre ?",
        metres(l, dec), listOf(l * 2, l / 2, l * 1.5, l * 3).map { metres(it, dec) },
        "largeur = distance × capteur / f = ${nb(d)} × ${nb(cap, 1)} / ${nb(f)} ≈ ${metres(l, 2)}.", a
    )
}

private fun cercleConfusion(a: Random): QuestionFormule {
    val (nom, diag) = listOf(
        "plein format (diagonale 43,3 mm)" to 43.3, "Super 35 4-perf (diagonale 31,1 mm)" to 31.1,
        "Micro 4/3 (diagonale 21,6 mm)" to 21.6, "Super 16 (diagonale 14,5 mm)" to 14.5
    ).random(a)
    fun mm(x: Double) = nb(x, 3) + " mm"
    return qcm(
        TH_OPTIQUE, "Cercle de confusion", "c = diagonale du capteur / 1 500",
        "Quel cercle de confusion retient-on, par convention, pour un capteur $nom ?",
        mm(diag / 1500), listOf(diag / 1000, diag / 3000, diag / 150, diag / 500).map(::mm),
        "c = diagonale / 1 500 = ${nb(diag, 1)} / 1 500 ≈ ${mm(diag / 1500)}.", a
    )
}

private fun hyperfocale(a: Random): QuestionFormule {
    val f = listOf(25, 35, 50).random(a).toDouble()
    val n = listOf(4.0, 5.6, 8.0, 11.0).random(a)
    val c = listOf(0.03, 0.025).random(a)
    val hMm = f * f / (n * c) + f
    val h = hMm / 1000
    return qcm(
        TH_OPTIQUE, "Hyperfocale", "H = f² / (N × c) + f",
        "Objectif de ${nb(f)} mm à ${fStop(n)}, cercle de confusion ${nb(c, 3)} mm : quelle hyperfocale ?",
        metres(h), listOf(h * 2, h / 2, f * f / c / 1000, h * 4).map { metres(it) },
        "H = f² / (N × c) + f = ${nb(f)}² / (${nb(n, 1)} × ${nb(c, 3)}) + ${nb(f)} ≈ ${nb(hMm)} mm, soit ${metres(h)}.", a
    )
}

private fun netteteHyperfocale(a: Random): QuestionFormule {
    val h = listOf(4.0, 6.0, 8.0, 10.0, 12.0).random(a)
    fun de(x: Double) = "de ${metres(x)} à l'infini"
    return qcm(
        TH_OPTIQUE, "Hyperfocale", "H = f² / (N × c) + f",
        "Tu fais le point sur l'hyperfocale (${metres(h)}) : à partir d'où l'image est-elle nette ?",
        de(h / 2), listOf(h / 3, h, h / 4, h * 2 / 3).map(::de),
        "Au point sur H, la netteté va de H / 2 à l'infini : ${metres(h / 2)} → ∞.", a
    )
}

private fun procheNet(a: Random): QuestionFormule {
    val (h, s) = listOf(6.0 to 2.0, 6.0 to 3.0, 10.0 to 2.0, 10.0 to 5.0, 12.0 to 4.0, 20.0 to 2.0, 20.0 to 5.0).random(a)
    val proche = s * h / (h + s)
    val loin = s * h / (h - s)
    return qcm(
        TH_OPTIQUE, "Profondeur de champ", "proche = s(H − f) / (H + s − 2f)",
        "Hyperfocale ${metres(h)}, point à ${metres(s)} : où commence la zone nette ? (f négligeable devant s et H)",
        metres(proche), listOf(loin, s / 2, h / 2, s * 0.9).map { metres(it) },
        "proche ≈ s × H / (H + s) = ${nb(s)} × ${nb(h)} / (${nb(h)} + ${nb(s)}) ≈ ${metres(proche, 2)}. La limite lointaine : s × H / (H − s) ≈ ${metres(loin)}.", a
    )
}

/* ── Électricité ───────────────────────────────────────────────────────── */

private val TH_ELEC = ThemeFormule.ELECTRICITE

private fun monophase(a: Random): QuestionFormule {
    val p = listOf(650, 1000, 1200, 2000, 2500, 3000).random(a).toDouble()
    val u = listOf(230, 230, 120).random(a).toDouble()
    val i = p / u
    val ou = if (u == 120.0) " (aux États-Unis)" else ""
    return qcm(
        TH_ELEC, "Monophasé", "I = P / (U × cos φ)",
        "Un projecteur de ${nb(p)} W sur une prise ${nb(u)} V$ou, cos φ = 1 : quel courant ?",
        amperes(i), listOf(p / 400, p / (sqrt(3.0) * u), i * 2, i / 2).map(::amperes),
        "I = P / (U × cos φ) = ${nb(p)} / ${nb(u)} ≈ ${nb(i, 2)} A.", a
    )
}

private fun puissancePrise(a: Random): QuestionFormule {
    val calibre = listOf(10, 16, 20, 32).random(a)
    val marge = a.nextBoolean()
    val max = calibre * 230.0
    fun w(x: Double) = nb(x) + " W"
    val juste = if (marge) max * 0.8 else max
    val autre = if (marge) max else max * 0.8
    return qcm(
        TH_ELEC, "Marge", "charge ≤ 80 % du calibre",
        "Circuit de $calibre A en 230 V${if (marge) ", en gardant la marge de 80 % pour une charge continue" else ""} : quelle puissance au maximum ?",
        w(juste), listOf(autre, calibre * 400.0, calibre * 120.0, max * 1.25).map(::w),
        "P = U × I = 230 × $calibre = ${w(max)}" + (if (marge) " ; avec la marge : 0,8 × ${nb(max)} = ${w(juste)}." else "."), a
    )
}

private fun combienDeProjecteurs(a: Random): QuestionFormule {
    val calibre = listOf(16, 20, 32).random(a)
    val p = listOf(300, 650, 1000, 2000).random(a)
    val dispo = 0.8 * calibre * 230
    val n = floor(dispo / p).toInt()
    val sansMarge = floor(calibre * 230.0 / p).toInt()
    fun projos(x: Int) = "$x projecteur${if (x > 1) "s" else ""}"
    return qcm(
        TH_ELEC, "Marge", "charge ≤ 80 % du calibre",
        "Circuit de $calibre A en 230 V, marge de 80 % : combien de projecteurs de ${nb(p)} W au maximum ?",
        projos(n), listOf(sansMarge, n + 1, n - 1, n + 2).filter { it >= 0 }.map(::projos),
        "0,8 × $calibre × 230 = ${nb(dispo)} W disponibles ; ${nb(dispo)} / ${nb(p)} ≈ ${nb(dispo / p, 1)}, donc ${projos(n)}.", a
    )
}

private fun triphase(a: Random): QuestionFormule {
    val kw = listOf(6, 9, 12, 18, 24, 36).random(a)
    val p = kw * 1000.0
    val i = p / (sqrt(3.0) * 400)
    return qcm(
        TH_ELEC, "Triphasé", "I = P / (√3 × U × cos φ)",
        "$kw kW répartis en triphasé 400 V, cos φ = 1 : quel courant par phase ?",
        amperes(i), listOf(p / 400, p / (3 * 400), p / 230, i * 2).map(::amperes),
        "I = P / (√3 × U × cos φ) = ${nb(p)} / (1,732 × 400) ≈ ${nb(i, 1)} A.", a
    )
}

private fun groupe(a: Random): QuestionFormule {
    val kva = listOf(2.0, 3.5, 6.0, 10.0, 20.0, 60.0).random(a)
    fun kw(x: Double) = nb(x, 1) + " kW"
    return qcm(
        TH_ELEC, "Groupe électrogène", "kW = 0,8 × kVA",
        "Un groupe électrogène de ${nb(kva, 1)} kVA : combien de kW utiles ?",
        kw(0.8 * kva), listOf(kva, kva / 0.8, kva * 0.5, kva * 0.9).map(::kw),
        "kW = 0,8 × kVA = 0,8 × ${nb(kva, 1)} = ${kw(0.8 * kva)}.", a
    )
}

/* ── Scintillement ─────────────────────────────────────────────────────── */

private val TH_SCINT = ThemeFormule.SCINTILLEMENT

private fun battement(a: Random): QuestionFormule {
    val secteur = listOf(50, 60).random(a)
    fun hz(x: Int) = "$x Hz"
    return qcm(
        TH_SCINT, "Battement des sources", "f_battement = 2 × f_secteur",
        "Sur un secteur à $secteur Hz, à quelle fréquence bat la lumière d'un HMI à ballast magnétique ou d'un tube fluo ?",
        hz(2 * secteur), listOf(secteur, secteur / 2, 4 * secteur, 3 * secteur).map(::hz),
        "f_battement = 2 × f_secteur = ${2 * secteur} Hz : la lumière faiblit deux fois par période du courant.", a
    )
}

private fun vitesseSure(a: Random): QuestionFormule {
    val secteur = listOf(50, 60).random(a)
    val battement = 2 * secteur
    val sures = if (secteur == 50) listOf(50, 100, 25) else listOf(60, 120, 30, 40)
    val pieges = if (secteur == 50) listOf(48, 60, 120, 96, 200) else listOf(50, 100, 48, 25, 96, 200)
    val den = sures.random(a)
    return qcm(
        TH_SCINT, "Vitesse sûre", "t × f_battement = nombre entier",
        "Sur un secteur à $secteur Hz, quelle vitesse d'obturation évite le scintillement ?",
        "1/$den s", pieges.shuffled(a).map { "1/$it s" },
        "Sûr si t × $battement est un nombre entier : 1/$den × $battement = ${nb(battement.toDouble() / den, 2)}. Les autres donnent un nombre à virgule.", a
    )
}

/** Les angles d'obturateur sûrs, jusqu'à 360°. */
internal fun anglesSurs(cadence: Int, secteur: Int): List<Double> {
    val pas = 360.0 * cadence / (2 * secteur)
    return (1..8).map { it * pas }.filter { it <= 360.0 + 1e-9 }
}

private fun angleSur(a: Random): QuestionFormule {
    val (cadence, secteur) = listOf(24 to 50, 25 to 60, 30 to 50, 24 to 60, 25 to 50).random(a)
    val sures = anglesSurs(cadence, secteur)
    val pas = sures.first()
    val pieges = listOf(180.0, 172.8, 90.0, 144.0, 150.0, 270.0, 45.0, 216.0, 108.0, 75.0, 86.4, 120.0)
        .filter { x -> abs(x / pas - round(x / pas)) > 0.01 }
    val juste = sures.filter { it <= 345.6 + 1e-9 }.random(a)
    return qcm(
        TH_SCINT, "Angles sûrs", "angle = k × 360 × cadence / f_battement",
        "À $cadence im/s sur un secteur à $secteur Hz, quel angle d'obturateur évite le scintillement ?",
        degres(juste), pieges.shuffled(a).map { degres(it) },
        "angle = k × 360 × $cadence / ${2 * secteur} = k × ${degres(pas)} : ${sures.joinToString(", ") { degres(it) }}.", a
    )
}

private fun cadenceSure(a: Random): QuestionFormule {
    val secteur = listOf(50, 60).random(a)
    val battement = 2 * secteur
    val sures = if (secteur == 50) listOf(25, 50, 20) else listOf(24, 30, 60)
    val pieges = if (secteur == 50) listOf(24, 30, 48, 60) else listOf(25, 50, 48, 100)
    val c = sures.random(a)
    fun ims(x: Int) = "$x im/s"
    return qcm(
        TH_SCINT, "Cadence sûre", "f_battement / cadence = nombre entier",
        "Sur un secteur à $secteur Hz, quelle cadence tombe juste avec le battement de la lumière ?",
        ims(c), pieges.shuffled(a).map(::ims),
        "f_battement / cadence = $battement / $c = ${nb(battement.toDouble() / c, 2)} : un nombre entier, chaque image reçoit la même lumière.", a
    )
}

/* ── Reconnaître la formule ────────────────────────────────────────────── */

private class Reco(val sujet: String, val juste: String, val faux: List<String>, val note: String = "")

private fun reconnaitre(theme: ThemeFormule, liste: List<Reco>): (Random) -> QuestionFormule = { a ->
    val r = liste.random(a)
    qcm(
        theme, r.sujet, "",
        "${r.sujet} : quelle est la bonne formule ?",
        r.juste, r.faux,
        "La bonne écriture : ${r.juste}." + if (r.note.isNotBlank()) " ${r.note}" else "", a
    )
}

private val RECO_EXPOSITION = listOf(
    Reco("Indice de lumination", "EV = log₂(N² / t)", listOf("EV = log₂(N / t)", "EV = log₂(N² × t)", "EV = N² / t"), "N : ouverture, t : temps de pose en secondes."),
    Reco("Cellule incidente", "E = 2,5 × 2^EV", listOf("E = 12,5 × 2^EV", "E = 2^EV / 2,5", "E = 250 × EV"), "Constante d'étalonnage C = 250."),
    Reco("Cellule réfléchie", "L = 0,125 × 2^EV", listOf("L = 2,5 × 2^EV", "L = 12,5 × 2^EV", "L = 2^EV / 0,125"), "Constante K = 12,5."),
    Reco("Ouverture pour une vitesse", "N = √(2^EV × t)", listOf("N = 2^EV × t", "N = √(2^EV / t)", "N = log₂(EV × t)")),
    Reco("Sensibilité", "EV(ISO) = EV₁₀₀ + log₂(ISO / 100)", listOf("EV(ISO) = EV₁₀₀ × ISO / 100", "EV(ISO) = EV₁₀₀ − log₂(ISO / 100)", "EV(ISO) = EV₁₀₀ + ISO / 100"))
)

private val RECO_TOURNAGE = listOf(
    Reco("Obturateur", "t = angle / (360 × cadence)", listOf("t = 360 / (angle × cadence)", "t = angle × cadence / 360", "t = cadence / angle")),
    Reco("Densité neutre", "densité ≈ 0,3 × diaphs", listOf("densité ≈ 0,1 × diaphs", "densité ≈ 0,5 × diaphs", "densité ≈ diaphs / 0,3")),
    Reco("T et f", "T = f / √(transmission)", listOf("T = f × transmission", "T = √(f × transmission)", "T = f × √(transmission)")),
    Reco("T-stop qui expose juste", "T = √(2^(EV₁₀₀ + log₂(EI/100) − pertes) × t)", listOf("T = √(2^(EV₁₀₀ − log₂(EI/100) + pertes) × t)", "T = 2^(EV₁₀₀ + log₂(EI/100) − pertes) × t", "T = √(2^(EV₁₀₀ + EI/100 − pertes) / t)"))
)

private val RECO_LUMIERE = listOf(
    Reco("Loi du carré de la distance", "E = I × cos θ / d²", listOf("E = I / d", "E = I × d²", "E = I × cos θ / d"), "Deux fois plus loin, quatre fois moins de lumière."),
    Reco("Intensité d'une source", "I = E₀ × d₀²", listOf("I = E₀ / d₀²", "I = E₀ × d₀", "I = √(E₀ × d₀)")),
    Reco("Distance pour un éclairement", "d = √(I × cos θ / E)", listOf("d = I / E", "d = √(E / I)", "d = (I / E)²")),
    Reco("Tache de lumière", "D = 2 × d × tan(faisceau / 2)", listOf("D = d × tan(faisceau)", "D = 2 × d × sin(faisceau)", "D = d / tan(faisceau / 2)")),
    Reco("Rapports", "côté éclairé / côté ombre = (E_face + E_déb) / E_déb", listOf("côté éclairé / côté ombre = E_face / E_déb", "côté éclairé / côté ombre = E_déb / E_face", "côté éclairé / côté ombre = (E_face − E_déb) / E_déb")),
    Reco("Géométrie", "d = √(recul² + Δh²)", listOf("d = recul + Δh", "d = √(recul + Δh)", "d = recul² + Δh²"))
)

private val RECO_COULEUR = listOf(
    Reco("Mired", "mired = 1 000 000 / K", listOf("mired = K / 1 000", "mired = 1 000 / K", "mired = K × 1 000 000")),
    Reco("Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source", listOf("Δ = K_cible − K_source", "Δ = 10⁶/K_source − 10⁶/K_cible", "Δ = 10⁶ / (K_cible − K_source)"), "Positif : vers le chaud. Négatif : vers le froid."),
    Reco("Perte d'une gélatine", "diaphs = −log₂(transmission)", listOf("diaphs = −log₁₀(transmission)", "diaphs = 1 − transmission", "diaphs = 1 / transmission"))
)

private val RECO_OPTIQUE = listOf(
    Reco("Angle de champ", "α = 2 × atan(dimension / (2 × f))", listOf("α = atan(dimension / f)", "α = 2 × atan(f / dimension)", "α = dimension / (2 × f)")),
    Reco("Focale pour un angle", "f = dimension / (2 × tan(α / 2))", listOf("f = dimension × tan(α / 2)", "f = 2 × dimension / tan(α)", "f = tan(α / 2) / dimension")),
    Reco("Hyperfocale", "H = f² / (N × c) + f", listOf("H = f / (N × c)", "H = N × c / f²", "H = f² × N / c")),
    Reco("Ce que couvre le cadre", "largeur = distance × capteur / f", listOf("largeur = distance × f / capteur", "largeur = capteur × f / distance", "largeur = distance / (capteur × f)")),
    Reco("Cercle de confusion", "c = diagonale / 1 500", listOf("c = diagonale / 150", "c = diagonale / 15 000", "c = diagonale × 1 500")),
    Reco("Profondeur de champ", "proche = s(H − f) / (H + s − 2f)", listOf("proche = s(H − f) / (H − s)", "proche = s × H / (H − s)", "proche = (H + s) / s"))
)

private val RECO_ELECTRICITE = listOf(
    Reco("Monophasé", "I = P / (U × cos φ)", listOf("I = P × U", "I = U / P", "I = P / (√3 × U)")),
    Reco("Triphasé", "I = P / (√3 × U × cos φ)", listOf("I = P / (3 × U)", "I = √3 × P / U", "I = P / (U × cos φ)")),
    Reco("Groupe électrogène", "kW = 0,8 × kVA", listOf("kW = kVA", "kW = kVA / 0,8", "kW = 0,5 × kVA")),
    Reco("Marge", "charge ≤ 80 % du calibre", listOf("charge ≤ 100 % du calibre", "charge ≤ 50 % du calibre", "charge ≤ 120 % du calibre"))
)

private val RECO_SCINTILLEMENT = listOf(
    Reco("Battement des sources", "f_battement = 2 × f_secteur", listOf("f_battement = f_secteur", "f_battement = f_secteur / 2", "f_battement = 4 × f_secteur")),
    Reco("Vitesse sûre", "t × f_battement = nombre entier", listOf("t / f_battement = nombre entier", "t × f_secteur = 1", "t = f_battement / cadence")),
    Reco("Angles sûrs", "angle = k × 360 × cadence / f_battement", listOf("angle = k × 360 / (cadence × f_battement)", "angle = k × f_battement / (360 × cadence)", "angle = 360 × cadence × f_battement"))
)

/* ── Optique géométrique ───────────────────────────────────────────────── */

private val TH_OPTGEO = ThemeFormule.OPTIQUE_GEO
private fun millimetres(x: Double, decimales: Int = 1): String = nb(x, decimales) + " mm"

private fun conjugaison(a: Random): QuestionFormule {
    val f = listOf(25, 35, 50, 85, 100).random(a).toDouble()
    val d = listOf(0.5, 1.0, 2.0, 3.0, 5.0).random(a)
    val dmm = d * 1000
    val image = f * dmm / (dmm - f)
    val t = image - f
    return qcm(
        TH_OPTGEO, "Conjugaison", "1/OA' − 1/OA = 1/f'",
        "Objectif de ${nb(f)} mm, sujet à ${metres(d)} : à quelle distance derrière l'objectif se forme l'image nette ?",
        millimetres(image), listOf(f, f - t, f + 2 * t, f * dmm / (dmm + f), f + 3 * t).map { millimetres(it) },
        "OA = −${nb(dmm)} mm. 1/OA' = 1/f' + 1/OA = 1/${nb(f)} − 1/${nb(dmm)}, donc OA' = f' × d / (d − f') = ${nb(f)} × ${nb(dmm)} / ${nb(dmm - f)} ≈ ${millimetres(image, 2)}. L'image est ${millimetres(t, 2)} derrière le foyer.", a
    )
}

private fun tirage(a: Random): QuestionFormule {
    val f = listOf(25, 35, 50, 85, 100).random(a).toDouble()
    val d = listOf(0.5, 1.0, 2.0, 3.0).random(a)
    val dmm = d * 1000
    val t = f * f / (dmm - f)
    return qcm(
        TH_OPTGEO, "Conjugaison", "1/OA' − 1/OA = 1/f'",
        "Un objectif de ${nb(f)} mm est réglé sur l'infini. De combien doit-il avancer pour faire le point à ${metres(d)} ?",
        millimetres(t, 2), listOf(t * 2, t / 2, t * 10, t * 4).map { millimetres(it, 2) },
        "OA' = f' × d / (d − f') = ${nb(f)} × ${nb(dmm)} / ${nb(dmm - f)} ≈ ${millimetres(f + t, 2)}. Le tirage vaut OA' − f' = f'² / (d − f') ≈ ${millimetres(t, 2)}.", a
    )
}

private fun grandissement(a: Random): QuestionFormule {
    val f = listOf(25, 50, 85, 135).random(a).toDouble()
    val d = listOf(2.0, 3.0, 5.0, 10.0).random(a)
    val h = listOf(1.7, 1.8).random(a)
    val dmm = d * 1000
    val g = f / (dmm - f)
    val image = h * 1000 * g
    return qcm(
        TH_OPTGEO, "Grandissement", "γ = OA' / OA",
        "Une personne de ${metres(h)} se tient à ${metres(d)} d'un objectif de ${nb(f)} mm : quelle hauteur fait son image ?",
        millimetres(image), listOf(image * 2, image / 2, image * 1.5, image / 3).map { millimetres(it) },
        "γ = OA' / OA = −f' / (d − f') = −${nb(f)} / ${nb(dmm - f)} ≈ −${nb(g, 4)} : image renversée, ${nb(1 / g)} fois plus petite. ${nb(h * 1000)} mm × ${nb(g, 4)} ≈ ${millimetres(image)}.", a
    )
}

private fun nombreOuverture(a: Random): QuestionFormule {
    val f = listOf(35, 50, 85, 100, 135).random(a).toDouble()
    val i = (1..4).random(a)
    val n = OUVERTURES_QUIZ[i]
    val d = f / n
    return if (a.nextBoolean()) {
        qcm(
            TH_OPTGEO, "Nombre d'ouverture", "N = f / D",
            "Un ${nb(f)} mm ouvert à ${fStop(n)} : quel diamètre de pupille d'entrée ?",
            millimetres(d, 0), listOf(f * n, f / (2 * n), f / (n * n), d * 2, d / 2).map { millimetres(it, 0) },
            "D = f / N = ${nb(f)} / ${nb(n, 1)} ≈ ${millimetres(d)}.", a
        )
    } else {
        qcm(
            TH_OPTGEO, "Nombre d'ouverture", "N = f / D",
            "Un ${nb(f)} mm dont la pupille d'entrée mesure ${millimetres(d)} : quelle ouverture ?",
            fStop(n), voisines(i, ::fStop),
            "N = f / D = ${nb(f)} / ${nb(d, 1)} ≈ ${nb(n, 1)} : ${fStop(n)}.", a
        )
    }
}

private fun vergence(a: Random): QuestionFormule {
    return if (a.nextBoolean()) {
        val f = listOf(25, 50, 100, 200, 500).random(a).toDouble()
        val v = 1000 / f
        fun dioptries(x: Double) = nb(x, 1) + " δ"
        qcm(
            TH_OPTGEO, "Vergence", "V = 1 / f' (f' en mètres)",
            "Quelle est la vergence d'un objectif de ${nb(f)} mm ?",
            dioptries(v), listOf(v * 2, v / 2, f / 10, v * 10).map(::dioptries),
            "V = 1 / f' avec f' en mètres : 1 / ${nb(f / 1000, 3)} = ${dioptries(v)}.", a
        )
    } else {
        val v = listOf(1, 2, 3, 4).random(a).toDouble()
        qcm(
            TH_OPTGEO, "Vergence", "V = 1 / f' (f' en mètres)",
            "Une bonnette de +${nb(v)} δ sur un objectif réglé à l'infini : à quelle distance se fait le point ?",
            metres(1 / v, 2), listOf(1 / (2 * v), 2 / v, 1 / v + 1, 1 / (4 * v), v).map { metres(it, 2) },
            "La bonnette ramène l'infini à son propre foyer : 1 / ${nb(v)} = ${metres(1 / v, 2)}.", a
        )
    }
}

/* ── Sensitométrie ─────────────────────────────────────────────────────── */

private val TH_SENSITO = ThemeFormule.SENSITOMETRIE

private fun densiteOptique(a: Random): QuestionFormule {
    val (t, d) = listOf(50.0 to log10(2.0), 25.0 to log10(4.0), 10.0 to 1.0, 1.0 to 2.0, 0.1 to 3.0).random(a)
    fun dens(x: Double) = "D = " + nb(x, 1)
    return qcm(
        TH_SENSITO, "Densité optique", "D = log₁₀(1 / T)",
        "Une plage du négatif laisse passer ${pourcent(t, 1)} de la lumière : quelle densité ?",
        dens(d), listOf(d + 0.3, d - 0.3, d * 2, d + 1, 1 - t / 100, log2(100 / t)).filter { it > 0.05 }.map(::dens),
        "D = log₁₀(1 / T) = log₁₀(1 / ${nb(t / 100, 3)}) = log₁₀(${nb(100 / t)}) ≈ ${nb(d, 2)}.", a
    )
}

private fun transmissionDensite(a: Random): QuestionFormule {
    val d = listOf(0.3, 0.6, 0.9, 1.0, 2.0).random(a)
    val t = 100 / 10.0.pow(d)
    fun part(x: Double) = pourcent(x, if (x < 1) 2 else if (x < 20) 1 else 0)
    return qcm(
        TH_SENSITO, "Densité optique", "D = log₁₀(1 / T)",
        "Une densité de ${nb(d, 1)} : quelle part de la lumière traverse ?",
        part(t), listOf(t * 2, t / 2, 100 - d * 100, t * 10, t / 4).filter { it > 0.01 && it <= 100.0 }.map(::part),
        "T = 10^−D = 10^−${nb(d, 1)} ≈ ${part(t)}. D = 0,3, c'est la moitié de la lumière : un diaph, comme un ND 0,3.", a
    )
}

private fun gamma(a: Random): QuestionFormule {
    val g = listOf(0.5, 0.6, 0.7, 0.8, 1.0).random(a)
    val dlog = listOf(0.5, 1.0, 1.5, 2.0).random(a)
    val d1 = listOf(0.3, 0.5, 0.8).random(a)
    val d2 = d1 + g * dlog
    fun gm(x: Double) = "γ = " + nb(x, 2)
    return qcm(
        TH_SENSITO, "Gamma", "γ = ΔD / Δlog H",
        "Sur la partie droite de la courbe, la densité passe de ${nb(d1, 1)} à ${nb(d2, 2)} quand log H augmente de ${nb(dlog, 1)} : quel gamma ?",
        gm(g), listOf(1 / g, g * dlog, g * 2, g / 2, d2 / dlog, g * 1.5, g + 0.25).map(::gm),
        "γ = ΔD / Δlog H = (${nb(d2, 2)} − ${nb(d1, 1)}) / ${nb(dlog, 1)} = ${nb(g * dlog, 2)} / ${nb(dlog, 1)} = ${nb(g, 2)}.", a
    )
}

private fun latitude(a: Random): QuestionFormule {
    return if (a.nextBoolean()) {
        val k = listOf(3, 5, 7, 8, 10).random(a)
        val x = 0.3 * k
        qcm(
            TH_SENSITO, "Latitude", "1 diaph = 0,3 en log H",
            "Une latitude de Δlog H = ${nb(x, 1)} représente combien de diaphs ?",
            diaphs(k), listOf(x * 10, k + 1.0, k - 1.0, 2.0 * k, x).map(::diaphs),
            "1 diaph = log₁₀ 2 ≈ 0,3 en log H : ${nb(x, 1)} / 0,3 = $k diaphs.", a
        )
    } else {
        val k = listOf(5, 7, 10, 12, 14).random(a)
        fun dl(x: Double) = "Δlog H = " + nb(x, 1)
        qcm(
            TH_SENSITO, "Latitude", "1 diaph = 0,3 en log H",
            "Un film tient $k diaphs entre le pied et l'épaule : quel écart en log H ?",
            dl(0.3 * k), listOf(0.1 * k, 0.5 * k, k.toDouble(), 0.3 * (k + 1)).map(::dl),
            "1 diaph ≈ 0,3 en log H : $k × 0,3 = ${nb(0.3 * k, 1)}.", a
        )
    }
}

private fun isoDin(a: Random): QuestionFormule {
    val isos = listOf(25, 50, 100, 200, 400, 800, 1600, 3200)
    val i = (1..6).random(a)
    val iso = isos[i]
    val log = log10(iso.toDouble())
    val din = round(10 * log + 1).toInt()
    return if (a.nextBoolean()) {
        qcm(
            TH_SENSITO, "ISO et DIN", "DIN = 10 × log₁₀(ISO) + 1",
            "Une pellicule de ${nb(iso)} ISO : combien de degrés DIN ?",
            "$din°", listOf(din + 3, din - 3, din + 1, iso / 10).map { "$it°" },
            "DIN = 10 × log₁₀(${nb(iso)}) + 1 = 10 × ${nb(log, 2)} + 1 ≈ $din°. +3° DIN = sensibilité ×2 = un diaph.", a
        )
    } else {
        qcm(
            TH_SENSITO, "ISO et DIN", "DIN = 10 × log₁₀(ISO) + 1",
            "$din° DIN : quelle sensibilité ISO ?",
            "${nb(iso)} ISO", listOf(isos[i - 1], isos[i + 1], din * 10, isos[(i + 2) % isos.size]).map { "${nb(it)} ISO" },
            "ISO = 10^((DIN − 1) / 10) = 10^(${nb((din - 1) / 10.0, 1)}) ≈ ${nb(iso)}. 21° = 100 ISO, et chaque +3° double.", a
        )
    }
}

/* ── Numérique ─────────────────────────────────────────────────────────── */

private val TH_NUM = ThemeFormule.NUMERIQUE

private fun niveaux(a: Random): QuestionFormule {
    val b = listOf(8, 10, 12, 14, 16).random(a)
    val n = 2.0.pow(b)
    fun nv(x: Double) = nb(x) + " niveaux"
    return qcm(
        TH_NUM, "Niveaux de quantification", "niveaux = 2^bits",
        "Un signal codé sur $b bits par composante : combien de niveaux ?",
        nv(n), listOf(2.0.pow(b - 1), 2.0.pow(b + 1), (b * b).toDouble(), b * 256.0, 2.0.pow(b + 2)).map(::nv),
        "niveaux = 2^$b = ${nb(n)}. Chaque bit de plus double le nombre de nuances.", a
    )
}

private fun bitsPourNiveaux(a: Random): QuestionFormule {
    val b = listOf(8, 10, 12, 14).random(a)
    val n = 2.0.pow(b)
    return qcm(
        TH_NUM, "Niveaux de quantification", "niveaux = 2^bits",
        "Il faut ${nb(n)} niveaux par composante : combien de bits ?",
        "$b bits", listOf(b + 1, b - 1, b + 2, (n / 256).toInt(), b * 2).map { "$it bits" },
        "bits = log₂(${nb(n)}) = $b, car 2^$b = ${nb(n)}.", a
    )
}

private fun dynamique(a: Random): QuestionFormule {
    return if (a.nextBoolean()) {
        val k = listOf(10, 12, 14, 16).random(a)
        val r = 2.0.pow(k)
        qcm(
            TH_NUM, "Dynamique", "diaphs = log₂(rapport)",
            "Une caméra distingue un rapport de ${nb(r)}:1 entre la plus forte et la plus faible lumière : combien de diaphs de dynamique ?",
            diaphs(k), listOf(k + 1.0, k - 1.0, round(log10(r)), k * 2.0, r / 1000).map(::diaphs),
            "diaphs = log₂(${nb(r)}) = $k.", a
        )
    } else {
        val k = listOf(10, 12, 13, 14, 15).random(a)
        fun rap(x: Double) = nb(x) + ":1"
        qcm(
            TH_NUM, "Dynamique", "diaphs = log₂(rapport)",
            "Un capteur annonce $k diaphs de dynamique : quel rapport de contraste peut-il enregistrer ?",
            rap(2.0.pow(k)), listOf(2.0.pow(k - 1), 2.0.pow(k + 1), k * 1000.0, 2.0.pow(k + 2)).map(::rap),
            "rapport = 2^$k = ${rap(2.0.pow(k))}.", a
        )
    }
}

private fun decibels(a: Random): QuestionFormule {
    val k = listOf(8, 10, 12, 14).random(a)
    val r = 2.0.pow(k)
    val db = 20 * log10(r)
    fun decibel(x: Double) = nb(x, 0) + " dB"
    return qcm(
        TH_NUM, "Dynamique en décibels", "dB = 20 × log₁₀(rapport)",
        "Une dynamique de $k diaphs (${nb(r)}:1), c'est combien de décibels ?",
        decibel(db), listOf(db / 2, k * 10.0, db + 6, k.toDouble()).map(::decibel),
        "dB = 20 × log₁₀(${nb(r)}) ≈ ${nb(db, 1)} dB : environ 6 dB par diaph.", a
    )
}

private fun debitBrut(a: Random): QuestionFormule {
    val (w, h) = listOf(1920 to 1080, 3840 to 2160, 4096 to 2160).random(a)
    val bits = listOf(10, 12).random(a)
    val fps = listOf(24, 25).random(a)
    val bitsParSeconde = w.toDouble() * h * bits * fps
    val mo = bitsParSeconde / 8 / 1e6
    fun mos(x: Double) = nb(x, 0) + " Mo/s"
    return qcm(
        TH_NUM, "Débit brut", "débit = largeur × hauteur × bits × cadence / 8",
        "Image RAW de $w × $h photosites, $bits bits chacun, $fps im/s, sans compression : quel débit ?",
        mos(mo), listOf(mo * 3, mo * 8, mo / 2, mo * 2).map(::mos),
        "$w × $h × $bits × $fps = ${nb(bitsParSeconde)} bits/s ; ÷ 8 = ${nb(bitsParSeconde / 8)} octets/s ≈ ${mos(mo)}. En RVB 4:4:4, trois valeurs par pixel : ×3.", a
    )
}

private fun stockage(a: Random): QuestionFormule {
    val debit = listOf(100, 220, 440, 880).random(a)
    val (duree, libelle) = listOf(1800 to "30 min", 3600 to "1 h", 7200 to "2 h").random(a)
    val go = debit / 8.0 * duree / 1000
    fun gos(x: Double) = nb(x, if (x < 100) 1 else 0) + " Go"
    return qcm(
        TH_NUM, "Stockage", "taille = débit × durée / 8",
        "Un codec à $debit Mbit/s, $libelle de rushes : quelle place sur la carte ?",
        gos(go), listOf(go * 8, go / 8, go * 2, go / 2).map(::gos),
        "$debit Mbit/s ÷ 8 = ${nb(debit / 8.0, 1)} Mo/s ; × ${nb(duree)} s = ${nb(debit / 8.0 * duree)} Mo ≈ ${gos(go)}.", a
    )
}

private fun echantillonnage(a: Random): QuestionFormule {
    return if (a.nextBoolean()) {
        val f = listOf(15, 20, 22, 24).random(a)
        qcm(
            TH_NUM, "Échantillonnage", "f_éch ≥ 2 × f_max",
            "Pour enregistrer des sons jusqu'à $f kHz, quelle fréquence d'échantillonnage au minimum ?",
            "${2 * f} kHz", listOf(f, 4 * f, f + 10, 3 * f).map { "$it kHz" },
            "Shannon : f_éch ≥ 2 × f_max = 2 × $f = ${2 * f} kHz.", a
        )
    } else {
        val fe = listOf(32.0, 44.1, 48.0, 96.0).random(a)
        fun khz(x: Double) = nb(x, 2) + " kHz"
        qcm(
            TH_NUM, "Échantillonnage", "f_éch ≥ 2 × f_max",
            "Un son échantillonné à ${khz(fe)} : jusqu'à quelle fréquence peut-il restituer ?",
            khz(fe / 2), listOf(fe, fe * 2, fe / 4, fe - 4).map(::khz),
            "f_max = f_éch / 2 = ${khz(fe / 2)}. C'est pourquoi on enregistre en 48 kHz : au-delà des 20 kHz que l'oreille entend.", a
        )
    }
}

private val RECO_OPTIQUE_GEO = listOf(
    Reco("Conjugaison", "1/OA' − 1/OA = 1/f'", listOf("1/OA' + 1/OA = 1/f'", "OA' − OA = f'", "1/OA − 1/OA' = 1/f'"), "Distances algébriques : OA est négatif, le sujet est devant l'objectif."),
    Reco("Grandissement", "γ = OA' / OA", listOf("γ = OA / OA'", "γ = OA' × OA", "γ = f' / OA'")),
    Reco("Nombre d'ouverture", "N = f / D", listOf("N = D / f", "N = f × D", "N = √(f / D)")),
    Reco("Vergence", "V = 1 / f'", listOf("V = f'", "V = 1 / f'²", "V = 2 / f'"), "f' en mètres, V en dioptries.")
)

private val RECO_SENSITOMETRIE = listOf(
    Reco("Densité optique", "D = log₁₀(1 / T)", listOf("D = log₂(1 / T)", "D = 1 / T", "D = log₁₀(T)")),
    Reco("Gamma", "γ = ΔD / Δlog H", listOf("γ = Δlog H / ΔD", "γ = D / log H", "γ = ΔD × Δlog H")),
    Reco("Latitude", "1 diaph = 0,3 en log H", listOf("1 diaph = 1 en log H", "1 diaph = 0,5 en log H", "1 diaph = 0,1 en log H")),
    Reco("ISO et DIN", "DIN = 10 × log₁₀(ISO) + 1", listOf("DIN = ISO / 10", "DIN = log₂(ISO) + 1", "DIN = 10 × log₂(ISO)"))
)

private val RECO_NUMERIQUE = listOf(
    Reco("Niveaux de quantification", "niveaux = 2^bits", listOf("niveaux = bits²", "niveaux = 10^bits", "niveaux = 2 × bits")),
    Reco("Dynamique", "diaphs = log₂(rapport)", listOf("diaphs = log₁₀(rapport)", "diaphs = rapport / 2", "diaphs = √rapport")),
    Reco("Dynamique en décibels", "dB = 20 × log₁₀(rapport)", listOf("dB = 10 × log₂(rapport)", "dB = 20 × rapport", "dB = log₁₀(rapport) / 20")),
    Reco("Débit brut", "débit = largeur × hauteur × bits × cadence / 8", listOf("débit = largeur × hauteur × bits × cadence × 8", "débit = largeur × hauteur × cadence / bits", "débit = (largeur + hauteur) × bits × cadence")),
    Reco("Stockage", "taille = débit × durée / 8", listOf("taille = débit × durée × 8", "taille = débit / durée", "taille = durée / (8 × débit)")),
    Reco("Échantillonnage", "f_éch ≥ 2 × f_max", listOf("f_éch ≥ f_max", "f_éch ≥ f_max / 2", "f_éch ≥ 4 × f_max"), "Théorème de Shannon-Nyquist.")
)

/* ── Répondre en tapant ────────────────────────────────────────────────── */

/**
 * Pour s'entraîner sans les quatre propositions : le gabarit de la réponse
 * (« 1/… s », « … lx »), et la vérification du nombre tapé, à 3 % près ou au
 * dernier chiffre affiché.
 */
object SaisieReponse {
    private val NOMBRE = Regex("[+−-]?\\d{1,3}(?: \\d{3})+(?:,\\d+)?|[+−-]?\\d+(?:,\\d+)?")

    private fun dernier(texte: String): MatchResult? = NOMBRE.findAll(texte).lastOrNull()

    /** Le gabarit de la réponse, ou null quand la question ne se prête pas à une réponse tapée. */
    fun gabarit(q: QuestionFormule): String? {
        if (q.formule.isBlank()) return null
        val juste = q.options[q.bonne]
        if (':' in juste) return null
        val m = dernier(juste) ?: return null
        val signe = m.value.first() in "+−-"
        val trou = juste.substring(0, m.range.first) + "…" + juste.substring(m.range.last + 1)
        return trou.replace(Regex("diaphs?\\b"), "diaph(s)").replace(Regex("projecteurs?\\b"), "projecteur(s)") +
            if (signe) " (avec le signe)" else ""
    }

    /** Le nombre attendu. */
    fun attendu(q: QuestionFormule): Double? = dernier(q.options[q.bonne])?.value?.let(::lire)

    internal fun lire(texte: String): Double? =
        texte.replace(" ", "").replace(" ", "").replace('−', '-').replace(',', '.').toDoubleOrNull()

    fun accepte(q: QuestionFormule, saisie: String): Boolean {
        val m = dernier(q.options[q.bonne]) ?: return false
        val attendu = lire(m.value) ?: return false
        /* « 1/50 » vaut « 50 » quand le gabarit est « 1/… s ». */
        val tape = lire(saisie.trim().substringAfterLast('/').replace("+", "")) ?: return false
        val decimales = m.value.substringAfter(',', "").length
        val tolerance = maxOf(0.5 * 10.0.pow(-decimales), abs(attendu) * 0.03)
        return abs(tape - attendu) <= tolerance + 1e-9
    }
}
