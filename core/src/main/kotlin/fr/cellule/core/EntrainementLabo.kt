package fr.cellule.core

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Le mode entraînement du Labo : là où il n'y a pas de geste naturel à
 * détourner — l'ordre des bains, une dilution de tête — on s'exerce comme au
 * quiz Caméras : une question, des indices du plus discret au plus explicite,
 * quatre propositions, puis l'explication et la fiche d'où elle vient.
 */
enum class ThemeLabo(val libelle: String) {
    BAINS("Ordre des bains"),
    DILUTION("Dilutions"),
    TEMPERATURE("Température"),
    DIAPHS("Tirage en diaphs"),
    PUSH("Push et pull"),
    RECIPROCITE("Réciprocité")
}

data class Exercice(
    val theme: ThemeLabo,
    val enonce: String,
    val choix: List<String>,
    val bonne: Int,
    val indices: List<String>,
    val explication: String,
    val source: Source?
)

object EntrainementLabo {

    fun exercice(theme: ThemeLabo, alea: Random): Exercice = when (theme) {
        ThemeLabo.BAINS -> bains(alea)
        ThemeLabo.DILUTION -> dilution(alea)
        ThemeLabo.TEMPERATURE -> temperature(alea)
        ThemeLabo.DIAPHS -> diaphs(alea)
        ThemeLabo.PUSH -> push(alea)
        ThemeLabo.RECIPROCITE -> reciprocite(alea)
    }

    fun auHasard(alea: Random): Exercice = exercice(ThemeLabo.entries.random(alea), alea)

    /**
     * Mélange la bonne réponse parmi des leurres tous différents d'elle et
     * entre eux ; un leurre qui se confond avec la réponse une fois arrondi
     * est écarté plutôt que de rendre la question injuste.
     */
    private fun melanger(
        theme: ThemeLabo, enonce: String, bonne: String, leurres: List<String>,
        indices: List<String>, explication: String, source: Source?, alea: Random
    ): Exercice {
        val propositions = (listOf(bonne) + leurres.filter { it != bonne }.distinct().take(3)).shuffled(alea)
        return Exercice(theme, enonce, propositions, propositions.indexOf(bonne), indices, explication, source)
    }

    /* ── Ordre des bains et durées : questions fixes, toutes sourcées ── */

    private data class Fixe(
        val enonce: String, val bonne: String, val leurres: List<String>,
        val indices: List<String>, val explication: String, val source: Source
    )

    private val FIXES = listOf(
        Fixe(
            "Le révélateur vient d'être vidé. Quel bain ensuite ?",
            "Le bain d'arrêt", listOf("Le fixateur", "Le lavage", "L'agent mouillant"),
            listOf("Le révélateur continue d'agir tant qu'il en reste sur le film.", "Il faut d'abord stopper la réaction, puis protéger le bain suivant."),
            "Ilford : le bain d'arrêt stoppe aussitôt le développement et limite le révélateur entraîné dans le fixateur, dont il prolonge la vie. Un simple rinçage à l'eau est possible, mais Ilford recommande l'arrêt acide.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Après le bain d'arrêt, quel bain ?",
            "Le fixateur", listOf("Le lavage", "L'agent mouillant", "Un second révélateur"),
            listOf("Le film contient encore des halogénures d'argent qui n'ont pas été développés.", "Tant qu'ils sont là, le film reste sensible à la lumière."),
            "Le fixateur dissout les halogénures d'argent non développés : c'est lui qui rend le négatif insensible à la lumière. Ilford : RAPID FIXER 1+4, 2 à 5 min à 20 °C pour la HP5 Plus.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Après le fixateur ?",
            "Le lavage", listOf("L'agent mouillant", "Le bain d'arrêt", "Le séchage directement"),
            listOf("Il reste du fixateur dans la gélatine.", "Ce qui reste finit par attaquer l'image avec les années."),
            "Le lavage retire le fixateur de la gélatine : c'est la condition de la conservation. Ilford : 5 à 10 min d'eau courante, ou la méthode économique en cuve.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Dernier bain avant de suspendre le film ?",
            "L'agent mouillant", listOf("Le fixateur", "Le bain d'arrêt", "L'eau du robinet, sans rien"),
            listOf("Il ne touche pas à l'image.", "Il agit sur la façon dont l'eau quitte le film."),
            "Ilford : l'ILFOTOL dans l'eau du rinçage final aide le film à sécher vite et régulièrement — 5 mL par litre (1+200) pour commencer ; trop ou trop peu laisse des traces.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Combien de temps d'ILFOSTOP (1+19) à 20 °C, selon Ilford ?",
            "10 s", listOf("1 min", "3 min", "5 min"),
            listOf("Le bain est acide et le révélateur ne résiste pas longtemps.", "C'est un temps minimal ; plus long ne pose pas de problème s'il reste raisonnable."),
            "Ilford donne 10 s à 20 °C comme temps minimal, entre 18 et 24 °C, pour 15 films 135-36 par litre sans régénération.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Ilford lave un film en cuve sans eau courante. Comment ?",
            "Remplir et retourner 5, puis 10, puis 20 fois", listOf(
                "Remplir une fois et laisser 10 min", "Retourner 50 fois dans la même eau", "Rincer 30 s sous le robinet"
            ),
            listOf("On change l'eau plusieurs fois.", "Chaque bain dure un peu plus que le précédent."),
            "Méthode Ilford : remplir la cuve d'eau à ±5 °C des bains, retourner 5 fois, vider ; remplir, 10 retournements, vider ; remplir, 20 retournements, vider. Plus rapide, moins d'eau, négatifs aptes à la conservation.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Quel écart de température Ilford tolère-t-il entre les bains et le révélateur ?",
            "5 °C au plus", listOf("Aucun, au dixième près", "10 °C", "Peu importe après le révélateur"),
            listOf("Le révélateur est le bain critique ; les autres le sont moins.", "Un écart brutal peut marquer la gélatine."),
            "Ilford recommande que tous les bains soient à la même température, ou au moins à 5 °C du révélateur ; de même pour l'eau de lavage.",
            Sources.ILFORD_HP5
        ),
        Fixe(
            "Kodak, Tri-X : lavage à l'eau courante sans éliminateur d'hyposulfite ?",
            "20 à 30 min", listOf("2 min", "5 min", "1 heure"),
            listOf("Avec un éliminateur (Hypo Clearing Agent), Kodak descend à 5 min.", "Sans, il faut beaucoup plus longtemps."),
            "Kodak (F-4017) : 20 à 30 min d'eau courante ; ou 30 s de rinçage, 1 à 2 min d'Hypo Clearing Agent, puis 5 min d'eau courante. Ilford donne 5 à 10 min pour la HP5 Plus.",
            Sources.KODAK_TRIX
        )
    )

    private fun bains(alea: Random): Exercice {
        val f = FIXES.random(alea)
        return melanger(ThemeLabo.BAINS, f.enonce, f.bonne, f.leurres, f.indices, f.explication, f.source, alea)
    }

    /* ── Dilutions ── */

    private val VOLUMES = listOf(250.0, 300.0, 375.0, 500.0, 600.0, 1000.0)

    /** « de Rodinal », « d'ILFOSOL 3 ». */
    private fun de(nom: String): String = if (nom.first().uppercaseChar() in "AEIOUYÉ") "d'$nom" else "de $nom"

    private fun ml(x: Double): String = nombreSansZero((x * 10).roundToInt() / 10.0) + " mL"

    private fun dilution(alea: Random): Exercice {
        val d = DilutionsPubliees.LISTE.filter { it.dilution.partsEau in 1.0..60.0 }.random(alea)
        val n = d.dilution.partsEau
        val v = VOLUMES.random(alea)
        val c = d.dilution.concentre(v)
        return if (alea.nextBoolean()) {
            melanger(
                ThemeLabo.DILUTION,
                "Tu prépares ${ml(v)} ${de(d.intitule)}. Combien de concentré ?",
                ml(c), listOf(ml(v / n), ml(2 * c), ml(v - c), ml(c / 2)),
                listOf(
                    "${d.dilution.libelle} : combien de parts en tout ?",
                    "${nombreSansZero(n + 1)} parts : une de concentré, ${nombreSansZero(n)} d'eau.",
                    "Une part vaut ${ml(v)} ÷ ${nombreSansZero(n + 1)}."
                ),
                "${ml(c)} de concentré et ${ml(v - c)} d'eau. L'erreur classique divise par ${nombreSansZero(n)} au lieu de ${nombreSansZero(n + 1)}. ${d.note}".trim(),
                d.source, alea
            )
        } else {
            val reste = listOf(5.0, 10.0, 15.0, 20.0, 25.0).random(alea)
            val total = d.dilution.volumePour(reste)
            melanger(
                ThemeLabo.DILUTION,
                "Il te reste ${ml(reste)} ${de(d.produit)}. Combien de solution à ${d.dilution.libelle} peux-tu préparer ?",
                ml(total), listOf(ml(reste * n), ml(reste / (n + 1)), ml(total * 2), ml(reste * (n + 2))),
                listOf(
                    "Le concentré est une part sur ${nombreSansZero(n + 1)}.",
                    "La solution fait ${nombreSansZero(n + 1)} fois le volume de concentré."
                ),
                "${ml(reste)} × ${nombreSansZero(n + 1)} = ${ml(total)}, dont ${ml(total - reste)} d'eau.",
                d.source, alea
            )
        }
    }

    /* ── Température, règle Ilford ── */

    private fun quartDeMinute(min: Double): Double = (min * 4).roundToInt() / 4.0

    private fun minutes(min: Double): String = Duree.libelle(quartDeMinute(min) * 60)

    private fun temperature(alea: Random): Exercice {
        val t20 = listOf(6.0, 6.5, 7.5, 9.0, 10.5, 11.0, 13.0).random(alea)
        val t = listOf(17.0, 18.0, 22.0, 23.0, 24.0).random(alea)
        val bonne = Temperatures.temps(t20, t, RegleIlford)
        val chaud = t > 20
        return melanger(
            ThemeLabo.TEMPERATURE,
            "Ta fiche donne ${minutes(t20)} à 20 °C. Ton révélateur est à ${nombreSansZero(t)} °C. Selon la règle d'Ilford ?",
            minutes(bonne),
            listOf(
                minutes(t20),
                minutes(t20 * RegleIlford.facteur(40 - t)),
                minutes(t20 * RegleIlford.facteur(20 + 2 * (t - 20)))
            ),
            listOf(
                if (chaud) "Plus chaud, la réaction va plus vite." else "Plus froid, la réaction ralentit.",
                "Ilford : 10 % par degré, et les pourcentages se composent.",
                "${nombreSansZero(abs(t - 20))} degré(s) : ${if (chaud) "divise" else "multiplie"} par 1,1 autant de fois."
            ),
            "${minutes(t20)} × 1,1^${nombreSansZero(20 - t).replace("-", "−")} ≈ ${minutes(bonne)}. C'est l'exemple de la fiche : 6 min à 20 °C font 4½ min à 23 °C et 9 min à 16 °C.",
            Sources.ILFORD_ID11, alea
        )
    }

    /* ── Tirage en diaphs ── */

    private val PAS = listOf(1.0 / 3, 0.5, 2.0 / 3, 1.0)

    private fun fraction(d: Double): String = when {
        abs(d - 1.0 / 3) < 1e-9 -> "⅓"
        abs(d - 0.5) < 1e-9 -> "½"
        abs(d - 2.0 / 3) < 1e-9 -> "⅔"
        else -> nombreSansZero(d)
    }

    private fun s(x: Double): String = nombreSansZero(x) + " s"

    private fun diaphs(alea: Random): Exercice {
        val base = listOf(6.0, 8.0, 10.0, 12.0, 16.0, 20.0).random(alea)
        val d = PAS.random(alea)
        val libelle = fraction(d)
        return when (alea.nextInt(3)) {
            0 -> {
                val bonne = TirageDiaphs.temps(base, d)
                melanger(
                    ThemeLabo.DIAPHS,
                    "Base ${s(base)}. Tu veux $libelle diaph de plus sur tout le tirage : temps total ?",
                    s(bonne), listOf(
                        s(base * (1 + d)), s(base * 2.0.pow(d + 1)), s(TirageDiaphs.temps(base, -d)),
                        s(base + d), s(base * 2.0.pow(d / 2))
                    ),
                    listOf("Un diaph double la lumière.", "Un tiers de diaph multiplie par 1,26, un demi par 1,41."),
                    "${s(base)} × 2^$libelle = ${s(bonne)}. Le papier répond en diaphs, pas en secondes.",
                    null, alea
                )
            }
            1 -> {
                val bonne = TirageDiaphs.brulage(base, d)
                melanger(
                    ThemeLabo.DIAPHS,
                    "Base ${s(base)}. Tu veux brûler le ciel de $libelle diaph. Temps à ajouter ?",
                    s(bonne), listOf(
                        s(base * d), s(TirageDiaphs.temps(base, d)), s(TirageDiaphs.masquage(base, d)), s(base / 2), s(base),
                        s(base * 1.5), s(TirageDiaphs.brulage(base, d / 2)), s(base / 4)
                    ),
                    listOf("Le ciel doit recevoir base × 2^Δ en tout.", "Il a déjà reçu la base : ajoute seulement la différence."),
                    "Le ciel reçoit ${s(TirageDiaphs.temps(base, d))} en tout, soit ${s(bonne)} de plus que la base.",
                    null, alea
                )
            }
            else -> {
                val bonne = TirageDiaphs.masquage(base, d)
                melanger(
                    ThemeLabo.DIAPHS,
                    "Base ${s(base)}. Tu veux éclaircir un visage de $libelle diaph. Combien de temps le masques-tu ?",
                    s(bonne), listOf(
                        s(base * d), s(TirageDiaphs.brulage(base, d)), s(TirageDiaphs.temps(base, -d)), s(base * d / 2), s(base),
                        s(base * 0.75), s(TirageDiaphs.masquage(base, d / 2)), s(base / 4)
                    ),
                    listOf("Le visage ne doit plus recevoir que base × 2^−Δ.", "Tu le caches pendant la différence."),
                    "Le visage reçoit ${s(TirageDiaphs.temps(base, -d))} ; tu le masques ${s(bonne)}.",
                    null, alea
                )
            }
        }
    }

    /* ── Push et pull, tables des fabricants ── */

    private fun push(alea: Random): Exercice {
        val table = Push.TABLES.filter { it.indices.size > 1 }.random(alea)
        val ei = table.indices.filter { it != table.iso }.random(alea)
        val bonne = table.minutes.getValue(ei)
        val d = table.diaphs(ei)
        val voisin = table.indices.filter { it != ei && it != table.iso }.randomOrNull(alea)
        val sens = when {
            bonne == table.normal -> "La latitude du film encaisse ce diaph : le fabricant garde le temps normal."
            d == 1.0 -> "Sous-exposé d'un diaph : on développe plus longtemps."
            d > 0 -> "Sous-exposé de ${fraction(d)} diaphs : on développe plus longtemps."
            else -> "Surexposé : on développe moins longtemps."
        }
        return melanger(
            ThemeLabo.PUSH,
            "${table.film} dans ${table.revelateur} : ${minutes(table.normal)} à EI ${table.iso}. Combien à EI $ei ?",
            minutes(bonne),
            listOfNotNull(
                minutes(table.normal),
                minutes(table.normal * 2.0.pow(d)),
                voisin?.let { minutes(table.minutes.getValue(it)) },
                minutes(table.normal * (1 + 0.5 * d)),
                minutes(table.normal * (1 + 0.25 * d))
            ),
            listOfNotNull(
                sens,
                "Ni proportionnel, ni doublé à chaque diaph : seule la table du fabricant fait foi.",
                voisin?.let { "Le fabricant donne ${minutes(table.minutes.getValue(it))} à EI $it." }
            ),
            "Fiche du fabricant : ${minutes(bonne)} à EI $ei, soit ×${nombreSansZero((bonne / table.normal * 100).roundToInt() / 100.0)} le temps normal. Pousser monte le contraste et le grain ; les ombres sous-exposées, elles, ne reviennent pas.",
            table.source, alea
        )
    }

    /* ── Réciprocité ── */

    private fun reciprocite(alea: Random): Exercice {
        val r = Reciprocites.LISTE.random(alea)
        val mesure = listOf(2.0, 4.0, 8.0, 15.0, 30.0, 60.0).random(alea)
        val bonne = r.corrige(mesure)!!
        fun pose(x: Double) = if (x < 60) s((x * 2).roundToInt() / 2.0) else Duree.libelle(x.roundToInt().toDouble())
        val regle = when (r) {
            is LoiIlford -> "Ilford : Tc = Tm^" + r.exposant.toString().replace('.', ',')
            else -> "Kodak : 1 s → 2 s, 10 s → 50 s, 100 s → 1200 s"
        }
        return melanger(
            ThemeLabo.RECIPROCITE,
            "${r.film} : la cellule annonce ${pose(mesure)}. Quelle pose donner ?",
            pose(bonne), listOf(pose(mesure), pose(mesure * 2), pose(bonne * 2), pose(mesure * 1.3)),
            listOf("Aux poses longues, le film perd de la sensibilité.", "La correction grandit plus vite que la pose.", regle),
            "${pose(mesure)} mesurées → ${pose(bonne)} à donner, soit ${libelleDiaphs(r.diaphs(mesure)!!)} diaph. ${r.developpement(mesure)?.let { "Kodak conseille aussi : $it." } ?: ""}".trim(),
            r.source, alea
        )
    }
}
