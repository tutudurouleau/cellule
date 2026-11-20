package fr.cellule.core

import kotlin.random.Random

/*
 * Le labo couleur : les quatre procédés chromogènes, bain par bain, et la
 * correction d'une dominante au tirage.
 *
 * Mêmes règles que le reste du Labo. Chaque chiffre vient d'une publication
 * Kodak, citée. Un bain dont la durée n'a pas pu être relue dans cette
 * publication garde une durée nulle et la mention « à vérifier » : le chrono
 * refuse alors de partir tant qu'on ne l'a pas reprise de la notice de son
 * kit, plutôt que de proposer un chiffre de seconde main.
 */

enum class ProcedeCouleur(val code: String, val libelle: String, val pour: String) {
    C41("C-41", "Négatif couleur", "Portra, Ektar, Gold, Ultramax…"),
    ECN2("ECN-2", "Négatif cinéma", "Vision3 500T, 250D, 50D…"),
    E6("E-6", "Diapositive", "Ektachrome E100 et les films inversibles"),
    RA4("RA-4", "Tirage couleur", "les papiers couleur, sous agrandisseur")
}

data class BainCouleur(
    val nom: String,
    /** En secondes ; 0 quand la publication citée n'a pas pu être relue sur ce point. */
    val duree: Double,
    /** Telle que la publication la donne ; vide si elle n'a pas été relue. */
    val temperature: String,
    /** Ce que fait le bain, en une phrase. */
    val role: String,
    /** Le bain qui décide de l'image : sa température se tient au plus serré. */
    val critique: Boolean = false,
    /** Durée minimale : un peu plus long ne nuit pas. */
    val minimum: Boolean = false
) {
    val aVerifier: Boolean get() = duree <= 0

    /** Ce que le chrono rappelle sous le bain : « 24 à 41 °C · durée minimale… ». */
    val consigne: String
        get() = buildString {
            if (temperature.isNotBlank()) append(temperature)
            val precision = when {
                aVerifier -> "durée à reprendre de la notice de ton kit"
                minimum -> "durée minimale, un peu plus ne nuit pas"
                else -> ""
            }
            if (precision.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(precision)
            }
            if (isNotEmpty()) append(". ")
            append(role)
        }
}

data class FicheCouleur(
    val procede: ProcedeCouleur,
    val resume: String,
    val bains: List<BainCouleur>,
    /** Ce qu'il faut savoir avant de s'y mettre. */
    val aRetenir: List<String>,
    val sources: List<Source>,
    val note: String
) {
    /** Le temps des bains connus, sans ceux à vérifier. */
    val dureeConnue: Double get() = bains.sumOf { it.duree }
    val aVerifier: Int get() = bains.count { it.aVerifier }

    /** Le révélateur qui décide de l'image (le premier, en E-6). */
    val critique: BainCouleur get() = bains.first { it.critique }

    /**
     * Les mêmes bains, prêts pour le chrono. L'agitation n'est pas imposée :
     * elle dépend de la cuve (retournements, ou rotation continue) et de la
     * notice du kit.
     */
    val sequence: SequenceLabo = SequenceLabo(
        "${procede.code} — ${procede.libelle.lowercase()}",
        bains.map { b -> Etape(b.nom, b.duree, null, b.consigne) },
        sources.first(),
        note,
        procede.code
    )
}

object LaboCouleur {

    /* Les temps des publications Kodak, en secondes. */
    private fun ms(min: Int, s: Int = 0) = min * 60.0 + s

    val C41 = FicheCouleur(
        ProcedeCouleur.C41,
        "Le négatif couleur des appareils photo. Six bains, dont un seul vraiment critique : le révélateur, " +
            "court et chaud, à tenir au dixième de degré.",
        listOf(
            BainCouleur(
                "Révélateur", ms(3, 15), "37,8 ± 0,15 °C",
                "Développe les grains exposés ; en s'oxydant, il se lie aux coupleurs de chaque couche et forme les colorants.",
                critique = true
            ),
            BainCouleur(
                "Blanchiment", ms(6, 30), "24 à 41 °C",
                "Rend l'argent métallique soluble : sans lui, l'argent reste dans l'image et l'assombrit.",
                minimum = true
            ),
            BainCouleur("Lavage", ms(3, 15), "24 à 41 °C", "Retire le blanchiment avant le fixateur."),
            BainCouleur(
                "Fixateur", ms(6, 30), "24 à 41 °C",
                "Dissout tout l'argent qui reste : il n'y a plus que les colorants.",
                minimum = true
            ),
            BainCouleur("Lavage", ms(3, 15), "24 à 41 °C", "Retire le fixateur de la gélatine."),
            BainCouleur(
                "Rinçage final", ms(1, 30), "24 à 41 °C",
                "Stabilise les colorants et aide le film à sécher sans traces ; pas de lavage après.",
                minimum = true
            )
        ),
        listOf(
            "Le révélateur se tient à 37,8 ± 0,15 °C : bain-marie, thermomètre précis, bains préchauffés avant de verser.",
            "Après le révélateur, rien ne presse plus autant : les autres bains tolèrent de 24 à 41 °C, et leurs durées sont des minimums.",
            "Le négatif sort orange : c'est le masque intégré, qui corrige les colorants. Le tirage et le scan l'annulent.",
            "Gants et aération : les bains couleur irritent la peau. Lis la fiche de sécurité de ton kit."
        ),
        listOf(Sources.KODAK_Z131),
        "Kodak, chimie FLEXICOLOR. Les durées d'un kit d'une autre marque peuvent différer : suis sa notice."
    )

    val ECN2 = FicheCouleur(
        ProcedeCouleur.ECN2,
        "Le négatif des caméras argentiques. Comme le C-41, en plus chaud, avec une couche noire au dos " +
            "du film (le rem-jet) qu'il faut retirer avant le révélateur.",
        listOf(
            BainCouleur("Prébain", ms(0, 10), "27 ± 1 °C", "Ramollit la couche anti-halo noire (rem-jet) au dos du film."),
            BainCouleur(
                "Retrait du rem-jet", 0.0, "",
                "On rince et on essuie le dos : le rem-jet ne doit pas passer dans le révélateur."
            ),
            BainCouleur(
                "Révélateur", ms(3), "41,1 °C",
                "Forme l'argent et les colorants, comme en C-41 — plus chaud, donc plus court.",
                critique = true
            ),
            BainCouleur("Arrêt", 0.0, "", "Stoppe net le développement."),
            BainCouleur(
                "Lavage", 0.0, "",
                "Retire l'acide de l'arrêt : Kodak prévient qu'un lavage trop court tache le film en bleu dans un blanchiment au ferricyanure."
            ),
            BainCouleur("Blanchiment", 0.0, "", "Rend l'argent soluble ; les kits Kodak utilisent le blanchiment « UL »."),
            BainCouleur("Lavage", 0.0, "", "Retire le blanchiment."),
            BainCouleur("Fixateur", 0.0, "", "Dissout l'argent : il ne reste que les colorants."),
            BainCouleur("Lavage", 0.0, "", "Retire le fixateur."),
            BainCouleur("Rinçage final", 0.0, "", "Aide le séchage, sans traces.")
        ),
        listOf(
            "Le révélateur ECN-2 travaille à 41,1 °C pendant 3 min : plus chaud que le C-41.",
            "Le rem-jet, couche noire au dos du film, protège du halo et de l'électricité statique. Le prébain le ramollit, on le retire avant le révélateur : sinon il salit le bain et le film.",
            "Développer de l'ECN-2 en C-41 donne des couleurs fausses et un rem-jet qui se décolle : c'est un autre procédé, pas un réglage.",
            "Kodak publie toutes les durées dans le module 7 de ses spécifications (H-24.07) et la notice de ses kits ; seuls le prébain et le révélateur ont pu être relus ici."
        ),
        listOf(Sources.KODAK_ECN2, Sources.KODAK_KIT_CINEMA),
        "Les durées vides sont à reprendre de la notice de ton kit ECN-2 : le chrono ne part pas sans elles."
    )

    val E6 = FicheCouleur(
        ProcedeCouleur.E6,
        "La diapositive : un procédé inversible. Deux révélateurs — le premier, en noir et blanc, " +
            "décide de tout ; le second forme l'image positive en couleur.",
        listOf(
            BainCouleur(
                "Premier révélateur", ms(6), "38,0 ± 0,2 °C",
                "Développe une image négative en noir et blanc. C'est lui qui fixe la densité : on pousse en allongeant ce bain.",
                critique = true
            ),
            BainCouleur("Premier lavage", ms(2), "", "Arrête le premier révélateur."),
            BainCouleur(
                "Bain d'inversion", ms(2), "",
                "Voile chimiquement les grains restés intacts : ce sont eux qui feront l'image positive."
            ),
            BainCouleur(
                "Révélateur chromogène", ms(6), "38,0 ± 0,3 °C",
                "Développe ces grains et forme les colorants : l'image positive, en couleur."
            ),
            BainCouleur("Pré-blanchiment", ms(2), "35 à 40 °C", "Prépare le blanchiment ; sans agitation."),
            BainCouleur("Blanchiment", ms(6), "", "Rend tout l'argent soluble."),
            BainCouleur("Fixateur", ms(4), "", "Dissout l'argent : il ne reste que les colorants."),
            BainCouleur("Lavage final", 0.0, "", "Retire le fixateur."),
            BainCouleur("Rinçage final", ms(1), "température ambiante", "Aide le séchage sans traces ; Kodak donne de 30 s à 1 min.")
        ),
        listOf(
            "Le premier révélateur est le bain critique : 38 °C au plus serré. Une erreur ici se voit sur toute la diapo, et rien ne la rattrape au tirage.",
            "On pousse ou retient une diapo en jouant sur le seul premier révélateur : la durée vient de la notice de ton kit.",
            "Une fois la température choisie, Kodak la tient à ± 0,2 °C pour le premier révélateur et ± 0,3 °C pour le chromogène.",
            "Une diapo n'a pas de latitude de rattrapage : la pose doit être juste à la prise de vue.",
            "Les temps de Kodak comprennent 10 à 20 s d'égouttage entre deux bains."
        ),
        listOf(Sources.KODAK_E6_KIT, Sources.KODAK_Z119),
        "Kodak, kit E-6 à usage unique et manuel Z-119. Le lavage final est à reprendre de la notice de ton kit."
    )

    val RA4 = FicheCouleur(
        ProcedeCouleur.RA4,
        "Le tirage d'un négatif couleur sur papier. Trois bains très courts, à température élevée : " +
            "tout se joue en moins de trois minutes.",
        listOf(
            BainCouleur(
                "Révélateur", 45.0, "35,0 ± 0,3 °C",
                "Forme l'argent et les colorants dans les trois couches du papier.",
                critique = true
            ),
            BainCouleur(
                "Blanchiment-fixage", 45.0, "30 à 36 °C",
                "Blanchit et fixe en un seul bain : il ne reste que les colorants."
            ),
            BainCouleur("Lavage", 90.0, "30 à 40 °C", "Retire le blanchiment-fixage du papier.")
        ),
        listOf(
            "Le papier couleur est sensible à toutes les couleurs : il se manipule dans le noir complet, sauf éclairage inactinique prévu par sa fiche.",
            "Une dominante se corrige à la filtration de l'agrandisseur : on ajoute la couleur de la dominante (voir l'outil Dominante).",
            "Ne juge les couleurs qu'une fois le tirage sec, sous une lumière proche du jour.",
            "En cuvette ou en tambour, la notice de ton kit peut ajouter un bain d'arrêt entre le révélateur et le blanchiment-fixage."
        ),
        listOf(Sources.KODAK_RA4),
        "Kodak, Process RA-4 à 35 °C. En tambour ou en cuvette, suis la notice de ton kit."
    )

    val FICHES = listOf(C41, ECN2, E6, RA4)

    val SEQUENCES: List<SequenceLabo> = FICHES.map { it.sequence }

    fun fiche(p: ProcedeCouleur): FicheCouleur = FICHES.first { it.procede == p }

    fun estCouleur(s: SequenceLabo): Boolean = SEQUENCES.any { it.nom == s.nom }
}

/* ── La dominante au tirage ───────────────────────────────────────────────── */

/**
 * Une dominante sur un tirage couleur, et le geste qui la corrige quand on
 * tire un négatif à travers une tête soustractive (filtres jaune, magenta,
 * cyan) : on ajoute un filtre de la couleur de la dominante — un filtre
 * absorbe sa complémentaire, donc la couche du papier qui forme ce colorant
 * reçoit moins de lumière.
 */
enum class Dominante(
    val libelle: String,
    val jaune: Int,
    val magenta: Int,
    val geste: String,
    val pourquoi: String
) {
    JAUNE(
        "jaune", +1, 0, "Ajouter du jaune",
        "Trop de jaune, c'est trop de lumière bleue reçue par le papier. Le filtre jaune absorbe le bleu."
    ),
    MAGENTA(
        "magenta", 0, +1, "Ajouter du magenta",
        "Trop de magenta, c'est trop de lumière verte reçue. Le filtre magenta absorbe le vert."
    ),
    ROUGE(
        "rouge", +1, +1, "Ajouter jaune et magenta",
        "Le rouge, c'est du jaune plus du magenta : trop des deux. On ajoute les deux filtres, autant l'un que l'autre."
    ),
    BLEU(
        "bleu", -1, 0, "Retirer du jaune",
        "Le bleu, c'est un manque de jaune : le papier a reçu trop peu de lumière bleue. On retire du filtre jaune."
    ),
    VERT(
        "vert", 0, -1, "Retirer du magenta",
        "Le vert, c'est un manque de magenta : trop peu de lumière verte. On retire du filtre magenta."
    ),
    CYAN(
        "cyan", -1, -1, "Retirer jaune et magenta",
        "Le cyan, c'est un manque de rouge, donc de jaune et de magenta. On retire les deux — ce qui revient à ajouter du cyan."
    )
}

/** Une filtration de tête couleur, en unités CC. */
data class Filtration(val jaune: Int, val magenta: Int, val cyan: Int = 0) {
    val libelle: String
        get() = buildString {
            append("Y $jaune · M $magenta")
            if (cyan != 0) append(" · C $cyan")
        }

    /** La part neutre : quand les trois filtres sont là, leur minimum ne fait qu'allonger la pose. */
    val neutre: Int get() = minOf(jaune, magenta, cyan).coerceAtLeast(0)
}

data class CorrectionCouleur(
    val avant: Filtration,
    val apres: Filtration,
    /** « Ajoute 10 de jaune » */
    val geste: String,
    /** Densité neutre ajoutée (> 0) pour éviter un filtre négatif, ou retirée (< 0). */
    val neutreAjoute: Int
)

object Dominantes {

    /** Les pas proposés, en unités CC : à l'œil, du plus discret au plus franc. */
    val PAS = listOf(5, 10, 20, 30)

    /**
     * La nouvelle filtration. Un filtre ne peut pas descendre sous zéro : si
     * la correction l'y mène, on ajoute à tous la même densité (neutre,
     * elle ne change pas la couleur) ; et quand les trois filtres sont
     * présents, on retire leur part commune, qui n'allonge que la pose.
     */
    fun corriger(f: Filtration, d: Dominante, pas: Int): CorrectionCouleur {
        var y = f.jaune + d.jaune * pas
        var m = f.magenta + d.magenta * pas
        var c = f.cyan
        val manque = -minOf(y, m, c)
        var neutre = 0
        if (manque > 0) {
            y += manque; m += manque; c += manque
            neutre = manque
        }
        val commun = minOf(y, m, c)
        if (commun > 0) {
            y -= commun; m -= commun; c -= commun
            neutre -= commun
        }
        return CorrectionCouleur(f, Filtration(y, m, c), geste(d, pas), neutre)
    }

    fun geste(d: Dominante, pas: Int): String = when (d) {
        Dominante.JAUNE -> "Ajoute $pas de jaune"
        Dominante.MAGENTA -> "Ajoute $pas de magenta"
        Dominante.ROUGE -> "Ajoute $pas de jaune et $pas de magenta"
        Dominante.BLEU -> "Retire $pas de jaune"
        Dominante.VERT -> "Retire $pas de magenta"
        Dominante.CYAN -> "Retire $pas de jaune et $pas de magenta"
    }

    /** La dominante qui demande le geste inverse : l'erreur la plus courante. */
    fun inverse(d: Dominante): Dominante = when (d) {
        Dominante.JAUNE -> Dominante.BLEU
        Dominante.BLEU -> Dominante.JAUNE
        Dominante.MAGENTA -> Dominante.VERT
        Dominante.VERT -> Dominante.MAGENTA
        Dominante.ROUGE -> Dominante.CYAN
        Dominante.CYAN -> Dominante.ROUGE
    }
}

/* ── Le quiz couleur ──────────────────────────────────────────────────────── */

object QuizCouleur {

    private data class Question(
        val enonce: String,
        val bonne: String,
        val leurres: List<String>,
        val indices: List<String>,
        val explication: String,
        val source: Source?
    )

    private val QUESTIONS = listOf(
        Question(
            "C-41 : à quelle température Kodak tient-il le révélateur ?",
            "37,8 ± 0,15 °C", listOf("20 ± 0,5 °C", "24 à 41 °C", "41,1 °C"),
            listOf("Bien plus chaud que le noir et blanc.", "La tolérance tient au dixième de degré."),
            "Kodak : révélateur FLEXICOLOR 3 min 15 à 37,8 ± 0,15 °C. Les bains suivants tolèrent de 24 à 41 °C.",
            Sources.KODAK_Z131
        ),
        Question(
            "C-41 : combien de temps de révélateur, selon Kodak ?",
            "3 min 15", listOf("45 s", "6 min", "10 min"),
            listOf("Le bain est chaud, donc court.", "C'est le plus court des bains du film C-41."),
            "Kodak : 3 min 15 à 37,8 °C. Le blanchiment et le fixateur durent, eux, au moins 6 min 30 chacun.",
            Sources.KODAK_Z131
        ),
        Question(
            "C-41 : quel bain suit le révélateur ?",
            "Le blanchiment", listOf("Le fixateur", "Le rinçage final", "Le bain d'inversion"),
            listOf("Après le révélateur, l'image contient à la fois de l'argent et des colorants.", "Avant de dissoudre l'argent, il faut le rendre soluble."),
            "Kodak : révélateur, blanchiment, lavage, fixateur, lavage, rinçage final. Le blanchiment rend l'argent soluble pour que le fixateur l'emporte.",
            Sources.KODAK_Z131
        ),
        Question(
            "À quoi sert le blanchiment d'un film couleur ?",
            "Rendre l'argent soluble pour que le fixateur l'enlève",
            listOf("Éclaircir les colorants trop denses", "Supprimer le masque orange", "Arrêter le révélateur"),
            listOf("Un film couleur fini ne contient plus d'argent.", "Le fixateur seul ne dissout pas l'argent métallique."),
            "Le révélateur laisse de l'argent métallique à côté des colorants. Le blanchiment le reconvertit en sel, que le fixateur dissout. Sans blanchiment, l'image reste grise et dense.",
            null
        ),
        Question(
            "RA-4 à 35 °C : combien de temps de révélateur, selon Kodak ?",
            "45 s", listOf("3 min 15", "6 min", "2 min 30"),
            listOf("Le papier est fin : tout va très vite.", "Le blanchiment-fixage dure autant que le révélateur."),
            "Kodak : révélateur 45 s à 35,0 ± 0,3 °C, blanchiment-fixage 45 s, lavage 1 min 30.",
            Sources.KODAK_RA4
        ),
        Question(
            "E-6 : que forme le premier révélateur ?",
            "Une image négative en noir et blanc",
            listOf("Les colorants jaune, magenta et cyan", "Une image positive en couleur", "Le masque orange"),
            listOf("Le procédé est inversible.", "Les colorants n'apparaissent qu'au second révélateur."),
            "Le premier révélateur E-6 est un révélateur noir et blanc : il développe les grains exposés. Le bain d'inversion voile les autres, que le révélateur chromogène transforme en image positive colorée.",
            Sources.KODAK_Z119
        ),
        Question(
            "E-6 : sur quel bain joue-t-on pour pousser une diapo ?",
            "Le premier révélateur", listOf("Le révélateur chromogène", "Le blanchiment", "Le bain d'inversion"),
            listOf("Le bain qui décide de la densité.", "C'est celui qui travaille en noir et blanc."),
            "Push et pull se règlent sur le seul premier révélateur : c'est lui qui décide de la densité de la diapo. Le chromogène va toujours au bout.",
            Sources.KODAK_Z119
        ),
        Question(
            "ECN-2 : que faut-il retirer avant le révélateur ?",
            "Le rem-jet, la couche noire au dos du film",
            listOf("Le masque orange", "Les coupleurs", "La couche sensible au bleu"),
            listOf("Les films cinéma ont une couche de plus que les films photo.", "Elle protège du halo et de l'électricité statique."),
            "Kodak : le prébain ramollit le rem-jet, qu'on retire avant le révélateur. Sinon il salit le bain et se redépose sur l'image.",
            Sources.KODAK_KIT_CINEMA
        ),
        Question(
            "ECN-2 : à quelle température travaille le révélateur ?",
            "41,1 °C", listOf("37,8 °C", "20 °C", "35 °C"),
            listOf("Plus chaud que le C-41.", "Le développement dure 3 min."),
            "Le révélateur ECN-2 travaille à 41,1 °C pendant 3 min ; le C-41 à 37,8 °C pendant 3 min 15.",
            Sources.KODAK_ECN2
        ),
        Question(
            "Pourquoi la couleur tient-elle sa température plus serré que le noir et blanc ?",
            "Trois couches doivent garder le même contraste : un écart décale les couleurs",
            listOf("Les colorants fondent au-delà de 38 °C", "Le révélateur couleur gèle sous 30 °C", "Pour finir plus vite"),
            listOf("Un film couleur, ce sont trois émulsions superposées.", "Une erreur de densité se corrige au tirage ; une erreur d'équilibre, non."),
            "Une température fausse ne change pas seulement la densité : les trois couches ne réagissent pas pareil. Les couleurs dérivent des ombres aux hautes lumières, et aucun filtre ne rattrape une dominante qui change avec la densité.",
            null
        ),
        Question(
            "Pourquoi un négatif couleur est-il orange ?",
            "Un masque intégré corrige les défauts des colorants",
            listOf("Le fixateur a mal travaillé", "Le film a pris la lumière", "C'est le rem-jet resté au dos"),
            listOf("Tous les négatifs C-41 sont orange, même parfaits.", "Les colorants réels absorbent un peu plus que leur couleur."),
            "Les colorants magenta et cyan absorbent aussi un peu de lumière qu'ils devraient laisser passer. Des coupleurs colorés compensent ce défaut : c'est le masque orange, que le papier et le scan annulent.",
            null
        )
    )

    private val COUCHES = listOf(
        "bleu" to "le jaune",
        "vert" to "le magenta",
        "rouge" to "le cyan"
    )

    private fun melanger(enonce: String, bonne: String, leurres: List<String>, indices: List<String>, explication: String, source: Source?, alea: Random): Exercice {
        val choix = (listOf(bonne) + leurres.filter { it != bonne }.distinct().take(3)).shuffled(alea)
        return Exercice(ThemeLabo.COULEUR, enonce, choix, choix.indexOf(bonne), indices, explication, source)
    }

    fun exercice(alea: Random): Exercice = when (alea.nextInt(10)) {
        in 0..4 -> fixe(alea)
        in 5..6 -> geste(alea)
        7 -> couche(alea)
        else -> filtration(alea)
    }

    private fun fixe(alea: Random): Exercice {
        val q = QUESTIONS.random(alea)
        return melanger(q.enonce, q.bonne, q.leurres, q.indices, q.explication, q.source, alea)
    }

    private fun geste(alea: Random): Exercice {
        val d = Dominante.entries.random(alea)
        val autres = Dominante.entries.filter { it != d && it != Dominantes.inverse(d) }.shuffled(alea).take(2)
        return melanger(
            "Ton tirage RA-4 sort trop ${d.libelle}. Que changes-tu à la filtration ?",
            d.geste,
            listOf(Dominantes.inverse(d).geste) + autres.map { it.geste },
            listOf(
                "En tirant un négatif, on ajoute un filtre de la couleur de la dominante.",
                "Le bleu est un manque de jaune, le vert un manque de magenta, le cyan un manque de jaune et de magenta."
            ),
            d.pourquoi,
            null,
            alea
        )
    }

    private fun couche(alea: Random): Exercice {
        val (lumiere, colorant) = COUCHES.random(alea)
        return melanger(
            "La couche sensible au $lumiere forme quel colorant ?",
            colorant.replaceFirstChar { it.uppercase() },
            COUCHES.map { it.second.replaceFirstChar { c -> c.uppercase() } } + "Le $lumiere",
            listOf("Chaque couche forme la couleur complémentaire de la lumière qu'elle a reçue.", "Bleu ↔ jaune, vert ↔ magenta, rouge ↔ cyan."),
            "Un film couleur superpose trois couches, sensibles au bleu, au vert et au rouge. Au développement, chacune forme la complémentaire : jaune, magenta, cyan. C'est ce qui inverse les couleurs sur le négatif.",
            null,
            alea
        )
    }

    private fun filtration(alea: Random): Exercice {
        val f = Filtration((3..8).random(alea) * 10, (3..8).random(alea) * 10)
        val d = Dominante.entries.random(alea)
        val pas = listOf(5, 10, 20).random(alea)
        val bonne = Dominantes.corriger(f, d, pas).apres
        val inverse = Dominantes.inverse(d)
        /* Les leurres : le geste inverse d'abord (l'erreur classique), puis les autres gestes. */
        val leurres = (listOf(inverse) + Dominante.entries.filter { it != d && it != inverse }.shuffled(alea))
            .map { Dominantes.corriger(f, it, pas).apres.libelle }
        return melanger(
            "Filtration ${f.libelle}. Le tirage est trop ${d.libelle} : tu corriges de $pas. Nouvelle filtration ?",
            bonne.libelle,
            leurres,
            listOf("On ajoute la couleur de la dominante, ou on retire sa complémentaire.", "${d.geste} : de $pas, pas davantage."),
            "${Dominantes.geste(d, pas)} : ${f.libelle} devient ${bonne.libelle}. ${d.pourquoi} Puis refais une bande d'essai : la pose change avec la filtration.",
            null,
            alea
        )
    }
}
