package fr.cellule.core

import kotlin.math.atan
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.math.tan

/*
 * Le plan : une photo de repérage ou de tournage, et tout ce qu'il faut pour
 * la refaire — où, quand, vers où, sous quel soleil, à quelle exposition, avec
 * quelle caméra, quelle focale et quel cadre.
 *
 * La photo elle-même reste dans la galerie du téléphone : le carnet n'en garde
 * que l'adresse. Tout le reste est du texte, qui voyage dans la sauvegarde.
 */

/** Où le plan a été pris. */
data class LieuPlan(
    val latitude: Double,
    val longitude: Double,
    /** Précision horizontale annoncée par le téléphone, en mètres. */
    val precisionM: Double? = null,
    val altitudeM: Double? = null,
    /** Trouvée en ligne quand il y a du réseau ; vide sinon. */
    val adresse: String = ""
) {
    val coordonnees: String
        get() = formatCoordonnee(latitude, "N", "S") + "  " + formatCoordonnee(longitude, "E", "O")

    /** Un lien qui ouvre le point dans Google Maps. */
    val lienMaps: String
        get() = "https://www.google.com/maps?q=" + decimales(latitude) + "," + decimales(longitude)

    private fun decimales(x: Double): String = String.format(java.util.Locale.ROOT, "%.6f", x)

    private fun formatCoordonnee(x: Double, positif: String, negatif: String): String =
        String.format(java.util.Locale.FRANCE, "%.5f", kotlin.math.abs(x)) + "° " + (if (x >= 0) positif else negatif)
}

/** Où était le soleil au moment du plan, calculé — pas mesuré. */
data class SoleilPlan(val azimut: Double, val hauteur: Double) {
    val resume: String
        get() = if (hauteur < -0.8) "sous l'horizon (${nombreCourt(hauteur, 0)}°)"
        else "azimut ${nombreCourt(azimut, 0)}° (${Boussole.cardinal(azimut)}), hauteur ${nombreCourt(hauteur, 0)}°"
}

object Boussole {
    private val POINTS = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

    /** Ramène un angle entre 0 et 360°. */
    fun normaliser(degres: Double): Double = ((degres % 360) + 360) % 360

    /** « NE », « SO »… le point cardinal le plus proche. */
    fun cardinal(degres: Double): String =
        POINTS[(Math.round(normaliser(degres) / 45.0).toInt()) % 8]

    fun libelle(degres: Double): String = nombreCourt(normaliser(degres), 0) + "° " + cardinal(degres)
}

/** Le champ que couvre la photo du téléphone : angles le long de sa largeur et de sa hauteur. */
data class ChampPhoto(val horizontal: Double, val vertical: Double)

/** La caméra de tournage, sa focale et les cadres qu'on veut voir sur la photo. */
data class CadrePlan(
    /** Nom libre ou repris du catalogue : « ALEXA Mini — Open Gate ». */
    val capteurNom: String = "",
    val capteurLargeurMm: Double? = null,
    val capteurHauteurMm: Double? = null,
    val focaleMm: Double? = null,
    /** Facteur de désanamorphose : 1 pour une optique sphérique. */
    val desanamorphose: Double = 1.0,
    /** Noms des formats à tracer (voir [TOUS_LES_RATIOS]). */
    val formats: List<String> = emptyList()
) {
    val complet: Boolean
        get() = (capteurLargeurMm ?: 0.0) > 0 && (capteurHauteurMm ?: 0.0) > 0 && (focaleMm ?: 0.0) > 0

    val resume: String
        get() = listOfNotNull(
            capteurNom.takeIf { it.isNotBlank() },
            if (capteurLargeurMm != null && capteurHauteurMm != null)
                nombreCourt(capteurLargeurMm, 2) + " × " + nombreCourt(capteurHauteurMm, 2) + " mm" else null,
            focaleMm?.let { nombreCourt(it, 1) + " mm" },
            if (desanamorphose != 1.0) "anamorphique ×" + nombreCourt(desanamorphose, 2) else null,
            formats.takeIf { it.isNotEmpty() }?.joinToString(" + ")
        ).joinToString(" · ")
}

val DESANAMORPHOSES = listOf(1.0, 1.3, 1.5, 1.8, 2.0)

/** Un rectangle dans la photo, en fractions de sa largeur et de sa hauteur. */
data class RectCadre(val gauche: Double, val haut: Double, val largeur: Double, val hauteur: Double) {
    val droite: Double get() = gauche + largeur
    val bas: Double get() = haut + hauteur
}

data class FormatTrace(val nom: String, val ratio: Double, val rect: RectCadre)

/** Ce qu'on peut dessiner du cadre de la caméra sur la photo du téléphone. */
data class TraceCadre(
    /** La zone utile du capteur, vue dans la photo. */
    val capteur: RectCadre,
    val formats: List<FormatTrace>,
    val champHorizontal: Double,
    val champVertical: Double,
    /** Le champ de la caméra déborde celui du téléphone : rien n'est tracé. */
    val tropLarge: Boolean,
    /** La plus courte focale que ce téléphone sait montrer en entier sur ce capteur. */
    val focaleMinimale: Double
)

object Cadreur {

    /**
     * Angles de champ d'une caméra : l'anamorphose élargit le champ horizontal
     * comme si le capteur était plus large d'autant.
     */
    fun champ(largeurMm: Double, hauteurMm: Double, focaleMm: Double, desanamorphose: Double = 1.0): ChampPhoto =
        ChampPhoto(
            Focales.angleDeChamp(largeurMm * desanamorphose, focaleMm),
            Focales.angleDeChamp(hauteurMm, focaleMm)
        )

    /**
     * Le champ d'une photo dont on ne connaît que la focale équivalente 24×36
     * (celle de l'EXIF, donnée sur la diagonale) et les proportions.
     */
    fun champDepuisEquivalent(equivalent35: Double, rapportImage: Double): ChampPhoto {
        val hauteur = Focales.DIAGONALE_REFERENCE / sqrt(1 + rapportImage * rapportImage)
        val largeur = hauteur * rapportImage
        return ChampPhoto(
            Focales.angleDeChamp(largeur, equivalent35),
            Focales.angleDeChamp(hauteur, equivalent35)
        )
    }

    private fun demiTangente(angle: Double): Double = tan(Math.toRadians(angle / 2))

    /**
     * Place le cadre de la caméra dans la photo du téléphone.
     *
     * Les tangentes des demi-angles se comparent directement : c'est ce qui
     * se projette à plat sur le capteur. Si la caméra voit plus large que le
     * téléphone, on le dit — on ne dessine pas un cadre faux.
     */
    fun tracer(photo: ChampPhoto, cadre: CadrePlan): TraceCadre? {
        if (!cadre.complet) return null
        val l = cadre.capteurLargeurMm!! * cadre.desanamorphose
        val h = cadre.capteurHauteurMm!!
        val f = cadre.focaleMm!!
        val tx = demiTangente(photo.horizontal)
        val ty = demiTangente(photo.vertical)
        if (tx <= 0 || ty <= 0 || tx.isNaN() || ty.isNaN()) return null
        val fractionL = (l / (2 * f)) / tx
        val fractionH = (h / (2 * f)) / ty
        val focaleMinimale = maxOf(l / (2 * tx), h / (2 * ty))
        val tropLarge = fractionL > 1.005 || fractionH > 1.005
        val fl = fractionL.coerceAtMost(1.0)
        val fh = fractionH.coerceAtMost(1.0)
        val capteur = RectCadre((1 - fl) / 2, (1 - fh) / 2, fl, fh)
        val rapportCapteur = l / h
        val formats = if (tropLarge) emptyList() else cadre.formats.mapNotNull { nom ->
            val ratio = TOUS_LES_RATIOS.firstOrNull { it.nom == nom } ?: return@mapNotNull null
            val guide = guideDeCadrage(rapportCapteur, ratio.ratio)
            val fw = fl * guide.largeur
            val fhh = fh * guide.hauteur
            FormatTrace(nom, ratio.ratio, RectCadre((1 - fw) / 2, (1 - fhh) / 2, fw, fhh))
        }
        val vu = champ(cadre.capteurLargeurMm!!, h, f, cadre.desanamorphose)
        return TraceCadre(capteur, formats, vu.horizontal, vu.vertical, tropLarge, focaleMinimale)
    }
}

/** Comment on numérote les plans. */
enum class Numerotation(val libelle: String) {
    SEQUENCE("Séq. / Plan / Prise"),
    SIMPLE("Numéro simple")
}

/** L'exposition mesurée et les réglages au moment du plan. */
data class ExpositionPlan(
    val ev100: Double? = null,
    /** Vrai : réglages de tournage (cadence, obturateur, EI, T). Faux : photo. */
    val tournage: Boolean = true,
    val cadence: Double? = null,
    val angle: Double? = null,
    val ei: Int? = null,
    /** « T4 » ou « f/8 », tel qu'affiché. */
    val ouverture: String = "",
    /** « 1/48 s ». */
    val tempsPose: String = "",
    /** Les filtres en place, en clair. */
    val filtres: String = ""
) {
    val resume: String
        get() = listOfNotNull(
            ouverture.takeIf { it.isNotBlank() },
            tempsPose.takeIf { it.isNotBlank() },
            ei?.let { (if (tournage) "EI " else "ISO ") + it },
            cadence?.let { nombreCourt(it) + " im/s" },
            angle?.let { nombreCourt(it, 1) + "°" }
        ).joinToString(" · ")
}

data class Plan(
    val identifiant: Long,
    val date: String,
    val heure: String = "",
    /** L'adresse de la photo dans la galerie (URI) ; vide si la photo n'a pas pu être enregistrée. */
    val photo: String = "",
    /** Proportions de la photo, largeur sur hauteur. */
    val rapportPhoto: Double = 4.0 / 3.0,
    /** Vrai si la photo vient de la galerie et non d'une capture de Cellule. */
    val importee: Boolean = false,
    /** Le jour (de tournage ou de repérage) où le plan est rangé ; null : pas encore rangé. */
    val jourId: Long? = null,
    val numerotation: Numerotation = Numerotation.SEQUENCE,
    val sequence: String = "",
    val plan: String = "",
    val prise: String = "",
    val numero: String = "",
    val titre: String = "",
    val exposition: ExpositionPlan = ExpositionPlan(),
    val lieu: LieuPlan? = null,
    /** Vers où pointait la caméra du téléphone, en degrés depuis le nord géographique. */
    val cap: Double? = null,
    val soleil: SoleilPlan? = null,
    val cadre: CadrePlan = CadrePlan(),
    /** Le champ de la photo ; null quand on ne le connaît pas (photo importée sans EXIF). */
    val champ: ChampPhoto? = null,
    val cameraTelephone: String = "",
    val notes: String = ""
) {
    /** « Séq. 3 · Plan 2 · Prise 1 » ou « Plan 12 ». */
    val reference: String
        get() = when (numerotation) {
            Numerotation.SIMPLE -> if (numero.isBlank()) "Plan sans numéro" else "Plan $numero"
            Numerotation.SEQUENCE -> listOfNotNull(
                sequence.takeIf { it.isNotBlank() }?.let { "Séq. $it" },
                plan.takeIf { it.isNotBlank() }?.let { "Plan $it" },
                prise.takeIf { it.isNotBlank() }?.let { "Prise $it" }
            ).joinToString(" · ").ifBlank { "Plan sans numéro" }
        }

    val intitule: String get() = if (titre.isBlank()) reference else "$reference — $titre"
}

/** Un projet : un film, un clip, un repérage au long cours. */
data class Projet(val identifiant: Long, val nom: String, val notes: String = "")

enum class TypeJour(val libelle: String) { TOURNAGE("Tournage"), REPERAGE("Repérage") }

/** Un jour de tournage ou de repérage, dans un projet. */
data class JourCinema(
    val identifiant: Long,
    val projetId: Long,
    val nom: String,
    val date: String = "",
    val type: TypeJour = TypeJour.TOURNAGE
) {
    val intitule: String
        get() = listOf(nom.ifBlank { type.libelle }, date).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Une vue du carnet photo rangée dans un projet : un lien, pas une copie. */
data class LienVue(val vueId: Long, val projetId: Long)

/** Le carnet « Cinéma » tout entier : Projet › Jour › Plans, et les vues photo liées. */
data class CarnetCinemaDonnees(
    val projets: List<Projet> = emptyList(),
    val jours: List<JourCinema> = emptyList(),
    val plans: List<Plan> = emptyList(),
    val liens: List<LienVue> = emptyList()
) {
    fun joursDe(projetId: Long): List<JourCinema> =
        jours.filter { it.projetId == projetId }.sortedWith(compareBy({ it.date }, { it.identifiant }))

    fun plansDe(jourId: Long): List<Plan> = plans.filter { it.jourId == jourId }.sortedBy { it.identifiant }

    fun plansDuProjet(projetId: Long): List<Plan> = joursDe(projetId).flatMap { plansDe(it.identifiant) }

    /** Les plans qui ne sont rangés dans aucun jour existant. */
    val plansNonRanges: List<Plan>
        get() {
            val connus = jours.map { it.identifiant }.toSet()
            return plans.filter { it.jourId == null || it.jourId !in connus }.sortedBy { it.identifiant }
        }

    fun vuesDe(projetId: Long): List<Long> = liens.filter { it.projetId == projetId }.map { it.vueId }

    fun projetDeLaVue(vueId: Long): Long? = liens.firstOrNull { it.vueId == vueId }?.projetId
}

object CarnetCinema {

    /** Supprimer un projet emporte ses jours ; les plans, eux, redeviennent « non rangés ». */
    fun sansProjet(d: CarnetCinemaDonnees, projetId: Long): CarnetCinemaDonnees {
        val joursOtes = d.jours.filter { it.projetId == projetId }.map { it.identifiant }.toSet()
        return d.copy(
            projets = d.projets.filterNot { it.identifiant == projetId },
            jours = d.jours.filterNot { it.projetId == projetId },
            plans = d.plans.map { if (it.jourId in joursOtes) it.copy(jourId = null) else it },
            liens = d.liens.filterNot { it.projetId == projetId }
        )
    }

    /** Supprimer un jour rend ses plans « non rangés » : on ne perd jamais un plan par ricochet. */
    fun sansJour(d: CarnetCinemaDonnees, jourId: Long): CarnetCinemaDonnees = d.copy(
        jours = d.jours.filterNot { it.identifiant == jourId },
        plans = d.plans.map { if (it.jourId == jourId) it.copy(jourId = null) else it }
    )

    fun deplacer(d: CarnetCinemaDonnees, planId: Long, jourId: Long?): CarnetCinemaDonnees =
        d.copy(plans = d.plans.map { if (it.identifiant == planId) it.copy(jourId = jourId) else it })

    fun remplacer(d: CarnetCinemaDonnees, plan: Plan): CarnetCinemaDonnees =
        if (d.plans.any { it.identifiant == plan.identifiant })
            d.copy(plans = d.plans.map { if (it.identifiant == plan.identifiant) plan else it })
        else d.copy(plans = d.plans + plan)

    /** Range une vue photo dans un projet, ou la sort de tout projet ([projetId] null). */
    fun lier(d: CarnetCinemaDonnees, vueId: Long, projetId: Long?): CarnetCinemaDonnees {
        val autres = d.liens.filterNot { it.vueId == vueId }
        return d.copy(liens = if (projetId == null) autres else autres + LienVue(vueId, projetId))
    }

    /**
     * Ajoute ce qui manque, sans rien écraser ni dédoubler : même identifiant,
     * on garde ce qu'on a. Restaurer deux fois la même sauvegarde ne change rien.
     */
    fun fusionner(existant: CarnetCinemaDonnees, importe: CarnetCinemaDonnees): CarnetCinemaDonnees {
        val projets = existant.projets.map { it.identifiant }.toMutableSet()
        val jours = existant.jours.map { it.identifiant }.toMutableSet()
        val plans = existant.plans.map { it.identifiant }.toMutableSet()
        val liens = existant.liens.map { it.vueId }.toMutableSet()
        return CarnetCinemaDonnees(
            projets = (existant.projets + importe.projets.filter { projets.add(it.identifiant) }).sortedBy { it.identifiant },
            jours = (existant.jours + importe.jours.filter { jours.add(it.identifiant) }).sortedBy { it.identifiant },
            plans = (existant.plans + importe.plans.filter { plans.add(it.identifiant) }).sortedBy { it.identifiant },
            liens = existant.liens + importe.liens.filter { liens.add(it.vueId) }
        )
    }

    /**
     * La numérotation proposée pour le plan suivant : même séquence et même
     * plan que le précédent, prise suivante ; ou le numéro suivant.
     */
    fun suivant(precedent: Plan?, numerotation: Numerotation): Plan {
        val base = Plan(identifiant = 0, date = "", numerotation = numerotation)
        if (precedent == null) {
            return if (numerotation == Numerotation.SIMPLE) base.copy(numero = "1")
            else base.copy(sequence = "1", plan = "1", prise = "1")
        }
        return when (numerotation) {
            Numerotation.SIMPLE -> base.copy(numero = incrementer(precedent.numero.ifBlank { precedent.plan }))
            Numerotation.SEQUENCE -> base.copy(
                sequence = precedent.sequence.ifBlank { "1" },
                plan = precedent.plan.ifBlank { precedent.numero.ifBlank { "1" } },
                prise = incrementer(precedent.prise)
            )
        }
    }

    /** « 12 » → « 13 », « 4B » → « 5 », vide → « 1 ». */
    fun incrementer(texte: String): String =
        ((texte.trim().takeWhile { it.isDigit() }.toIntOrNull() ?: 0) + 1).toString()

    /** Les lieux distincts d'une série de plans, pour la page de garde d'un dossier. */
    fun lieux(plans: List<Plan>): List<String> =
        plans.mapNotNull { p -> p.lieu?.let { it.adresse.ifBlank { it.coordonnees } } }.distinct()

    /** « 12 novembre 2026 » ou « du 12 au 14 novembre 2026 » — ici, simplement les dates extrêmes. */
    fun periode(plans: List<Plan>, jours: List<JourCinema>): String {
        val dates = (plans.map { it.date } + jours.map { it.date }).filter { it.isNotBlank() }.sorted()
        return when {
            dates.isEmpty() -> ""
            dates.first() == dates.last() -> dates.first()
            else -> dates.first() + " → " + dates.last()
        }
    }
}

/** Distance entre deux points du globe, en mètres (haversine, sphère de 6 371 km). */
fun distanceMetres(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dp = p2 - p1
    val dl = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dp / 2).let { it * it } +
        kotlin.math.cos(p1) * kotlin.math.cos(p2) * kotlin.math.sin(dl / 2).let { it * it }
    return 2 * 6_371_000.0 * atan(sqrt(a) / sqrt(1 - a).coerceAtLeast(1e-12))
}

/** La diagonale d'un capteur, pour le situer d'un coup d'œil. */
fun diagonaleMm(largeurMm: Double, hauteurMm: Double): Double = hypot(largeurMm, hauteurMm)
