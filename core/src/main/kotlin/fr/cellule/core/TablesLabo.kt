package fr.cellule.core

import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Les tables du Labo : l'essentiel du développement en tableaux à lire d'un
 * coup d'œil, comme la table maîtresse de l'exposition.
 *
 * Rien n'y est nouveau. Chaque ligne reprend un chiffre déjà relu dans une
 * fiche de fabricant (temps publiés, dilutions, bains) ; ces tables ne font
 * que les ranger pour qu'on comprenne ce qui change d'un bain, d'un
 * révélateur, d'un degré ou d'un diaph à l'autre.
 */

/** Un bain de la chaîne film, dans l'ordre, avec ce qu'en disent Ilford et Kodak. */
data class BainTable(val nom: String, val role: String, val ilford: String, val kodak: String)

/** Un mot du labo et ce qu'il veut dire. */
data class MotLabo(val mot: String, val sens: String)

/** Un révélateur pour un même film au même indice : son temps, sa forme, son caractère. */
data class LigneRevelateur(
    val revelateur: String,
    val dilution: String,
    val minutes: Double,
    val forme: TypeFiche?,
    val caractere: String
)

data class LigneTemperature(val temperature: Double, val facteur: Double, val minutes: Double)

/** La même table Kodak, face à la règle d'Ilford appliquée à son temps à 20 °C. */
data class LigneComparaison(val temperature: Double, val publie: Double, val regle: Double)

data class LigneDilution(val dilution: Dilution, val produits: List<String>)

/** Un indice d'exposition et, pour chaque révélateur de la table, son temps (null s'il n'est pas publié). */
data class LignePush(val ei: Int, val diaphs: Int, val minutes: List<Double?>)

object TablesLabo {

    /* ── 1. La chaîne des bains ─────────────────────────────────────────── */

    /** Fiches HP5 Plus (Ilford) et Tri-X 400 (Kodak). */
    val BAINS = listOf(
        BainTable(
            "Révélateur", "Fait apparaître l'image : l'argent noircit là où la lumière a frappé.",
            "selon le film et le révélateur", "selon le film et le révélateur"
        ),
        BainTable(
            "Arrêt", "Stoppe le développement aussitôt, et ménage le fixateur.",
            "ILFOSTOP 1+19 · 10 s au moins", "Indicator Stop Bath · 30 s"
        ),
        BainTable(
            "Fixateur", "Dissout ce qui n'a pas reçu de lumière : le film ne craint plus le jour.",
            "RAPID FIXER 1+4 · 2 à 5 min", "Rapid Fixer · 2 à 4 min"
        ),
        BainTable(
            "Lavage", "Chasse le fixateur, pour un négatif qui dure.",
            "remplir, retourner 5, 10 puis 20 fois", "eau courante · 20 à 30 min"
        ),
        BainTable(
            "Mouillant", "L'eau glisse du film sans laisser de traces au séchage.",
            "ILFOTOL · 5 mL par litre", "PHOTO-FLO · 30 s"
        )
    )

    /* ── 2. Les mots du labo ────────────────────────────────────────────── */

    val MOTS = listOf(
        MotLabo("Stock", "Le révélateur tel quel, sans eau ajoutée — une poudre, une fois dissoute selon sa notice."),
        MotLabo("1+9", "Une part de concentré pour neuf parts d'eau : dix parts en tout."),
        MotLabo("Usage unique", "La solution sert une fois, puis on la jette."),
        MotLabo("Régénérable", "La solution se garde et resert, en lui rendant ce qu'elle a perdu avec un régénérateur."),
        MotLabo("EI", "L'indice réglé sur la cellule. Pas forcément la sensibilité du film : c'est lui qui décide du temps de développement."),
        MotLabo("Pousser (push)", "Exposer à un EI plus élevé que la sensibilité du film, puis développer plus longtemps."),
        MotLabo("Retenir (pull)", "L'inverse : exposer à un EI plus bas, développer moins longtemps."),
        MotLabo("Agitation", "Retourner la cuve pour renouveler le révélateur contre le film. Ilford : 10 s par minute ; Kodak : 5 s toutes les 30 s."),
        MotLabo("Réciprocité", "Aux poses longues, le film perd de la sensibilité : il faut poser plus longtemps que ne l'annonce la cellule.")
    )

    /* ── 3. Un film, plusieurs révélateurs ──────────────────────────────── */

    const val FILM_REFERENCE = "Ilford HP5 Plus"
    const val EI_REFERENCE = 400

    /**
     * Ce que la fiche HP5 Plus d'Ilford dit de chaque couple, repris des
     * fiches produits (un test vérifie que chaque mot s'y trouve).
     */
    private val CARACTERES = mapOf(
        ("ILFOTEC DD-X" to "1+4") to "meilleure qualité, grain le plus fin, sensibilité maximale",
        ("ID-11" to "stock") to "meilleure qualité d'ensemble",
        ("ID-11" to "1+1") to "usage unique",
        ("ID-11" to "1+3") to "netteté maximale, économie",
        ("MICROPHEN" to "stock") to "sensibilité maximale",
        ("ILFOSOL 3" to "1+9") to "netteté maximale, usage unique",
        ("ILFOTEC HC" to "1+31") to "régénérable",
        ("ILFOTEC LC29" to "1+29") to "le choix économique"
    )

    fun caractere(revelateur: String, dilution: String): String = CARACTERES[revelateur to dilution].orEmpty()

    private fun forme(revelateur: String): TypeFiche? =
        FichesLabo.parNom(revelateur)?.let { RechercheFiches.type(it) }

    private fun estIlford(revelateur: String): Boolean =
        FichesLabo.parNom(revelateur)?.fabricant?.startsWith("Ilford") == true

    private fun revelateurs(ilford: Boolean): List<LigneRevelateur> =
        TempsPublies.pourFilm(FILM_REFERENCE)
            .filter { it.ei == EI_REFERENCE && it.temperature == 20.0 && estIlford(it.revelateur) == ilford }
            .map { LigneRevelateur(it.revelateur, it.dilution, it.minutes, forme(it.revelateur), caractere(it.revelateur, it.dilution)) }

    /** Les révélateurs d'Ilford pour la HP5 Plus à EI 400 et 20 °C. */
    val REVELATEURS_ILFORD: List<LigneRevelateur> get() = revelateurs(ilford = true)

    /** Ceux d'autres marques, dont Ilford donne aussi les temps dans la même fiche. */
    val REVELATEURS_AUTRES: List<LigneRevelateur> get() = revelateurs(ilford = false)

    /* ── 4. La température ──────────────────────────────────────────────── */

    /** L'exemple chiffré d'Ilford : 6 min à 20 °C. */
    const val TEMPS_EXEMPLE = 6.0

    /** Ilford arrondit ses temps à la demi-minute. */
    fun demiMinute(m: Double): Double = (m * 2).roundToInt() / 2.0

    /** Kodak, au quart de minute. */
    fun quartDeMinute(m: Double): Double = (m * 4).roundToInt() / 4.0

    /** La règle d'Ilford de 16 à 24 °C, appliquée à l'exemple de la fiche. */
    val REGLE_ILFORD: List<LigneTemperature>
        get() = (16..24).map { t ->
            val f = RegleIlford.facteur(t.toDouble())
            LigneTemperature(t.toDouble(), f, demiMinute(TEMPS_EXEMPLE * f))
        }

    /** Les températures de l'exemple publié : 9 min à 16 °C, 6 à 20 °C, 4½ à 23 °C. */
    val TEMPERATURES_EXEMPLE = setOf(16.0, 20.0, 23.0)

    /** Tri-X 400 en D-76 : la table Kodak, et la règle d'Ilford partie de son temps à 20 °C. */
    val KODAK_FACE_A_LA_REGLE: List<LigneComparaison>
        get() {
            val lignes = TempsPublies.pourFilm("Kodak Tri-X 400")
                .filter { it.revelateur == "D-76" && it.dilution == "stock" && it.ei == 400 }
                .sortedBy { it.temperature }
            val t20 = lignes.first { it.temperature == 20.0 }.minutes
            return lignes.map { LigneComparaison(it.temperature, it.minutes, quartDeMinute(t20 * RegleIlford.facteur(it.temperature))) }
        }

    /* ── 5. Les dilutions ───────────────────────────────────────────────── */

    const val VOLUME_EXEMPLE = 500.0

    val DILUTIONS: List<LigneDilution>
        get() = DilutionsPubliees.LISTE
            .groupBy { it.dilution }
            .map { (d, liste) -> LigneDilution(d, liste.map { it.produit }.distinct()) }
            .sortedBy { it.dilution.partsEau }

    /* ── 6. Pousser ─────────────────────────────────────────────────────── */

    /** Les trois révélateurs Ilford publiés jusqu'à EI 1600 au moins pour la HP5 Plus. */
    val REVELATEURS_PUSH = listOf("ID-11" to "stock", "ILFOTEC DD-X" to "1+4", "MICROPHEN" to "stock")

    val PUSH: List<LignePush>
        get() {
            val hp5 = TempsPublies.pourFilm(FILM_REFERENCE).filter { it.temperature == 20.0 }
            return listOf(400, 800, 1600, 3200).map { ei ->
                LignePush(
                    ei,
                    log2Entier(ei / EI_REFERENCE),
                    REVELATEURS_PUSH.map { (r, d) -> hp5.firstOrNull { it.revelateur == r && it.dilution == d && it.ei == ei }?.minutes }
                )
            }
        }

    private fun log2Entier(n: Int): Int = 31 - Integer.numberOfLeadingZeros(n)

    /* ── Écriture ───────────────────────────────────────────────────────── */

    /** « 7½ », « 6¾ », « 13 » : les minutes comme les fiches les écrivent. */
    fun minutes(m: Double): String {
        val entier = m.toInt()
        val reste = m - entier
        val fraction = when {
            abs(reste) < 1e-6 -> ""
            abs(reste - 0.25) < 1e-6 -> "¼"
            abs(reste - 0.5) < 1e-6 -> "½"
            abs(reste - 0.75) < 1e-6 -> "¾"
            else -> return nombreSansZero(m)
        }
        return if (entier == 0 && fraction.isNotEmpty()) fraction else "$entier$fraction"
    }

    /** « ×1,46 », « ×1 », « ×0,75 ». */
    fun facteur(f: Double): String {
        val arrondi = (f * 100).roundToInt() / 100.0
        return "×" + if (arrondi == arrondi.toInt().toDouble()) arrondi.toInt().toString()
        else arrondi.toString().replace('.', ',')
    }

    /** « 1 part sur 10 » → « 10 % » ; « 3,8 % » en dessous de 10. */
    fun pourcentConcentre(d: Dilution): String {
        val p = d.fraction * 100
        return if (p >= 10) "${p.roundToInt()} %" else nombreSansZero(p) + " %"
    }

    /** Le concentré et l'eau pour [volume] mL : « 50 + 450 mL ». */
    fun pour(d: Dilution, volume: Double = VOLUME_EXEMPLE): String =
        "${nombreSansZero(d.concentre(volume))} + ${nombreSansZero(d.eau(volume))} mL"
}
