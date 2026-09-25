package fr.cellule.core

import kotlin.math.abs
import kotlin.random.Random

enum class GenreQuestion(val libelle: String) {
    QUI_SUIS_JE("Qui suis-je ?"),
    QUEL_FILM("Quel film ?"),
    CHRONOLOGIE("Chronologie"),
    INTRUS("L'intrus")
}

/**
 * Découverte : la photo tout de suite et un premier indice offert. Expert : la
 * silhouette seule, aucun indice d'office — la photo ne vient qu'après la
 * réponse, et les leurres se ressemblent davantage.
 */
enum class Niveau(val libelle: String) {
    DECOUVERTE("Découverte"),
    EXPERT("Expert")
}

/**
 * Une question : la réponse, un énoncé, des indices, des propositions qui
 * contiennent la réponse. Pour une chronologie, la « réponse » est le plus
 * ancien et c'est l'ordre des propositions qu'on demande ; pour l'intrus,
 * c'est l'intrus, et l'explication dit ce que partagent les trois autres.
 */
data class Question(
    val genre: GenreQuestion,
    val reponse: Materiel,
    val enonce: String,
    val indices: List<String>,
    val choix: List<Materiel>,
    val explication: String = ""
) {
    val bonneReponse: Int get() = choix.indexOf(reponse)

    /** Les positions des propositions, de la plus ancienne à la plus récente. */
    val ordre: List<Int> get() = choix.indices.sortedBy { choix[it].annee }
}

/**
 * Ce qui départage les propositions d'un intrus : un trait que l'on sait lire
 * dans la fiche (null quand la fiche ne permet pas de trancher).
 */
private class Critere(
    val genre: GenreMateriel,
    val indice: String,
    val trait: (Materiel) -> String?
)

object QuizMateriel {

    const val NOMBRE_DE_CHOIX = 4

    /** Une chance sur trois, quand il y en a, de revoir une fiche ratée. */
    const val PART_REVISION = 0.35

    /* Des mots trop courants dans les noms pour trahir quoi que ce soit. */
    private val MOTS_ANODINS = setOf(
        "mark", "pro", "cine", "ciné", "cinema", "cinéma", "camera", "caméra",
        "prime", "primes", "series", "série", "classic", "super", "les", "des", "et"
    )

    /**
     * Ce qui, dans un indice, donnerait la réponse : le nom lui-même, puis
     * chacun de ses mots distinctifs (« Raptor », « Monstro », « K35 »…).
     */
    fun termesRevelateurs(m: Materiel): List<String> {
        val mots = m.nom.split(Regex("[^\\p{L}\\p{N}]+")).filter { mot ->
            val distinctif = (mot.length >= 2 && mot.all { it.isLetter() }) ||
                (mot.length >= 3 && mot.any { it.isLetter() }) ||
                (mot.length >= 3 && mot.all { it.isDigit() })
            distinctif && mot.lowercase() !in MOTS_ANODINS
        }
        return (listOf(m.nom) + mots).distinct()
    }

    private fun motif(terme: String) = Regex(
        "(?<![\\p{L}\\p{N}])" + Regex.escape(terme) + "(?![\\p{L}\\p{N}])",
        RegexOption.IGNORE_CASE
    )

    fun masquer(texte: String, m: Materiel): String =
        termesRevelateurs(m).fold(texte) { t, terme -> motif(terme).replace(t, "…") }

    /** Les noms de la marque (« ARRI », « Zeiss »…), tels qu'écrits dans la fiche. */
    fun termesMarque(m: Materiel): List<String> =
        m.fabricant.split("/").map { it.trim() }.filter { it.length >= 2 }

    /**
     * Masque aussi la marque : un indice qui la nommerait ferait double emploi
     * avec « Fabricant », gardé pour la fin.
     */
    fun masquerTout(texte: String, m: Materiel): String =
        termesMarque(m).fold(masquer(texte, m)) { t, terme -> motif(terme).replace(t, "…") }

    fun nommeLaMarque(texte: String, m: Materiel): Boolean =
        termesMarque(m).any { motif(it).containsMatchIn(texte) }

    fun devoile(texte: String, m: Materiel): Boolean =
        termesRevelateurs(m).any { motif(it).containsMatchIn(texte) }

    /** Le titre sans ses précisions entre parenthèses, pour comparer deux crédits. */
    fun titreDeBase(titre: String): String = sansAccents(titre.substringBefore(" (")).trim()

    /** « ARRI / Zeiss » compte pour ARRI comme pour Zeiss. */
    fun marques(m: Materiel): Set<String> =
        m.fabricant.split("/").map { sansAccents(it).trim() }.filter { it.isNotEmpty() }.toSet()

    fun memeMarque(a: Materiel, b: Materiel): Boolean = marques(a).intersect(marques(b)).isNotEmpty()

    /** La marque sous laquelle ranger un résultat : la première nommée. */
    fun marquePrincipale(m: Materiel): String = m.fabricant.substringBefore("/").trim()

    /* ── Les indices ────────────────────────────────────────────────────── */

    /** Un indice, et les leurres qu'il permet d'écarter. */
    private class Piste(val texte: String, val dernier: Boolean = false, val ecarte: (Materiel) -> Boolean)

    /**
     * Les indices d'une fiche. Face à des propositions, on ne garde que ceux
     * qui écartent au moins un leurre — un indice que partagent les quatre
     * ne sert à rien —, la phrase à retenir d'abord, puis du plus subtil au
     * plus décisif ; le fabricant, quand il départage encore, vient en
     * dernier. Aucun ne nomme la réponse.
     */
    fun indices(m: Materiel, choix: List<Materiel> = emptyList()): List<String> {
        val pistes = buildList {
            add(Piste(m.aRetenir) { true })
            add(Piste("Famille : ${m.categorie}") { it.categorie != m.categorie })
            add(Piste("Apparition : ${m.libelleAnnee}") { it.annee != m.annee })
            m.chiffresCles.forEach { p ->
                add(Piste("${p.libelle} : ${p.valeur}") { autre -> autre.chiffresCles.none { it.libelle == p.libelle && it.valeur == p.valeur } })
            }
            m.films.firstOrNull()?.let { f ->
                val base = titreDeBase(f.titre)
                add(
                    Piste("Au générique de « ${f.titre} »" + if (f.artisan.isNotBlank()) " — ${f.artisan}" else "") { autre ->
                        autre.films.none { titreDeBase(it.titre) == base }
                    }
                )
            }
            add(Piste("Fabricant : ${m.fabricant}", dernier = true) { !memeMarque(it, m) })
        }
        return trier(pistes, leurres(m, choix), m)
    }

    private fun leurres(m: Materiel, choix: List<Materiel>) = choix.filter { it.nomComplet != m.nomComplet }

    /** Le fabricant se dit en toutes lettres, dans sa piste à lui ; ailleurs, il est masqué. */
    private fun trier(pistes: List<Piste>, leurres: List<Materiel>, m: Materiel): List<String> {
        val gardees = if (leurres.isEmpty()) pistes else {
            val utiles = pistes.map { it to leurres.count(it.ecarte) }.filter { it.second > 0 }
            val (derniers, autres) = utiles.partition { it.first.dernier }
            (autres.take(1) + autres.drop(1).sortedBy { it.second } + derniers).map { it.first }
        }
        return gardees.map { if (it.dernier) masquer(it.texte, m) else masquerTout(it.texte, m) }
    }

    /* ── Les questions ──────────────────────────────────────────────────── */

    fun question(
        genre: GenreQuestion,
        parmi: List<Materiel>,
        alea: Random,
        eviter: Materiel? = null,
        niveau: Niveau = Niveau.DECOUVERTE,
        aRevoir: Set<String> = emptySet()
    ): Question? = when (genre) {
        GenreQuestion.QUI_SUIS_JE, GenreQuestion.QUEL_FILM -> identifier(genre, parmi, alea, eviter, niveau, aRevoir)
        GenreQuestion.CHRONOLOGIE -> chronologie(parmi, alea, niveau)
        GenreQuestion.INTRUS -> intrus(parmi, alea)
    }

    private fun identifier(
        genre: GenreQuestion,
        parmi: List<Materiel>,
        alea: Random,
        eviter: Materiel?,
        niveau: Niveau,
        aRevoir: Set<String>
    ): Question? {
        val candidats = parmi
            .filter { genre != GenreQuestion.QUEL_FILM || it.films.isNotEmpty() }
            .let { liste -> liste.filter { it != eviter }.ifEmpty { liste } }
        if (candidats.isEmpty()) return null
        /* Les fiches ratées reviennent, sans envahir le quiz. */
        val ratees = candidats.filter { it.identifiant in aRevoir }
        val reponse =
            if (ratees.isNotEmpty() && alea.nextDouble() < PART_REVISION) ratees.random(alea)
            else candidats.random(alea)

        return when (genre) {
            GenreQuestion.QUEL_FILM -> {
                val film = reponse.films.random(alea)
                val base = titreDeBase(film.titre)
                /* Un leurre crédité sur le même film rendrait la question fausse. */
                val choix = propositions(reponse, parmi, alea, niveau) { autre ->
                    autre.films.none { titreDeBase(it.titre) == base }
                }
                val pistes = buildList {
                    if (film.artisan.isNotBlank()) {
                        add(Piste("Image ou réalisation : ${film.artisan}") { autre -> autre.films.none { it.artisan == film.artisan } })
                    }
                    add(Piste("Famille : ${reponse.categorie}") { it.categorie != reponse.categorie })
                    add(Piste("Apparition : ${reponse.libelleAnnee}") { it.annee != reponse.annee })
                    add(Piste("Fabricant : ${reponse.fabricant}", dernier = true) { !memeMarque(it, reponse) })
                }
                Question(
                    genre = genre,
                    reponse = reponse,
                    enonce = "${reponse.genre.question} a servi sur « ${film.titre} » ?",
                    indices = trier(pistes, leurres(reponse, choix), reponse),
                    choix = choix
                )
            }
            else -> {
                val choix = propositions(reponse, parmi, alea, niveau) { true }
                Question(
                    genre = genre,
                    reponse = reponse,
                    enonce = "Qui suis-je ?",
                    indices = indices(reponse, choix),
                    choix = choix
                )
            }
        }
    }

    /**
     * La réponse et des leurres du même genre, de la même marque d'abord :
     * un logo sur la photo ne doit pas suffire à trancher. Viennent ensuite
     * la même silhouette, puis la même époque ; en Expert, la silhouette pèse
     * autant que la marque.
     */
    private fun propositions(
        reponse: Materiel,
        parmi: List<Materiel>,
        alea: Random,
        niveau: Niveau,
        admissible: (Materiel) -> Boolean
    ): List<Materiel> {
        val autres = parmi
            .filter { it.genre == reponse.genre && it.nomComplet != reponse.nomComplet && admissible(it) }
            .shuffled(alea)
        val poidsSilhouette = if (niveau == Niveau.EXPERT) 4 else 2
        fun proximite(m: Materiel): Int =
            (if (memeMarque(m, reponse)) 4 else 0) +
                (if (m.silhouette == reponse.silhouette) poidsSilhouette else 0) +
                (if (abs(m.annee - reponse.annee) <= 10) 1 else 0)
        val leurres = autres.sortedByDescending(::proximite).take(NOMBRE_DE_CHOIX - 1)
        return (leurres + reponse).shuffled(alea)
    }

    /* ── Chronologie ────────────────────────────────────────────────────── */

    /** Deux dates « vers » ne se départagent qu'à cinq ans d'écart. */
    fun departageables(a: Materiel, b: Materiel): Boolean =
        abs(a.annee - b.annee) >= if (a.anneeApprox || b.anneeApprox) 5 else 1

    /**
     * Quatre fiches du même genre à remettre dans l'ordre d'apparition. En
     * Expert, on les prend de préférence dans la même marque : quatre ARRI,
     * c'est une vraie question.
     */
    private fun chronologie(parmi: List<Materiel>, alea: Random, niveau: Niveau): Question? {
        repeat(12) {
            val melange = parmi.shuffled(alea)
            val depart = melange.firstOrNull() ?: return null
            val memeGenre = melange.filter { it.genre == depart.genre }
            val candidats =
                if (niveau == Niveau.EXPERT) memeGenre.sortedByDescending { memeMarque(it, depart) }
                else memeGenre
            val retenus = mutableListOf<Materiel>()
            for (c in candidats) {
                if (retenus.size == NOMBRE_DE_CHOIX) break
                if (retenus.all { departageables(it, c) }) retenus += c
            }
            if (retenus.size == NOMBRE_DE_CHOIX) {
                val choix = retenus.shuffled(alea)
                val plusAncien = choix.minBy { it.annee }
                val repere = choix.filter { it != plusAncien && it != choix.maxBy { m -> m.annee } }.random(alea)
                return Question(
                    genre = GenreQuestion.CHRONOLOGIE,
                    reponse = plusAncien,
                    enonce = "Du plus ancien au plus récent : touche-les dans l'ordre.",
                    indices = listOf("Un repère : ${repere.nomComplet} date de ${repere.libelleAnnee}."),
                    choix = choix,
                    explication = choix.sortedBy { it.annee }.joinToString(" → ") { "${it.nomComplet} (${it.libelleAnnee})" }
                )
            }
        }
        return null
    }

    /* ── L'intrus ───────────────────────────────────────────────────────── */

    private fun categorie(m: Materiel) = m.categorie.lowercase()

    private val CRITERES = listOf(
        Critere(GenreMateriel.CAMERA, "Pense au support : pellicule ou numérique.") { m ->
            val c = categorie(m)
            when {
                c.startsWith("argentique") -> "sur pellicule"
                c.startsWith("numérique") || c.startsWith("caméscope") || c.startsWith("appareil photo") -> "en numérique"
                else -> null
            }
        },
        Critere(GenreMateriel.CAMERA, "Pense à la taille du capteur.") { m ->
            val c = categorie(m)
            when {
                !c.startsWith("numérique") || "micro 4/3" in c -> null
                "grand format" in c || "plein format" in c || "65 mm" in c -> "à grand capteur (plein format ou plus)"
                "super 35" in c -> "en Super 35"
                else -> null
            }
        },
        Critere(GenreMateriel.OBJECTIF, "Pense à la forme de l'image : anamorphique ou sphérique.") { m ->
            if (m.silhouette == Silhouette.ANAMORPHIQUE || "anamorph" in categorie(m)) "anamorphique" else "sphérique"
        },
        Critere(GenreMateriel.OBJECTIF, "Pense à la focale : variable ou fixe.") { m ->
            val c = categorie(m)
            when {
                "zoom" in c && "fixe" in c -> null
                m.silhouette == Silhouette.ZOOM || c.startsWith("zoom") -> "un zoom"
                "fixe" in c -> "une focale fixe"
                else -> null
            }
        }
    )

    /**
     * Trois fiches qui partagent un trait, une qui ne le partage pas. Le trait
     * se lit dans la fiche (support, capteur, optique, focale), jamais dans le
     * nom de la marque.
     */
    private fun intrus(parmi: List<Materiel>, alea: Random): Question? {
        val genres = parmi.map { it.genre }.toSet()
        for (critere in CRITERES.filter { it.genre in genres }.shuffled(alea)) {
            val groupes = parmi.filter { it.genre == critere.genre }
                .mapNotNull { m -> critere.trait(m)?.let { it to m } }
                .groupBy({ it.first }, { it.second })
            if (groupes.size < 2) continue
            val majorites = groupes.filter { it.value.size >= NOMBRE_DE_CHOIX - 1 }.keys.toList()
            if (majorites.isEmpty()) continue
            val majorite = majorites.random(alea)
            val minorite = groupes.keys.filter { it != majorite }.random(alea)
            val trois = groupes.getValue(majorite).shuffled(alea).take(NOMBRE_DE_CHOIX - 1)
            val lIntrus = groupes.getValue(minorite).random(alea)
            val feminin = critere.genre == GenreMateriel.CAMERA
            return Question(
                genre = GenreQuestion.INTRUS,
                reponse = lIntrus,
                enonce = if (feminin) "Trois se ressemblent sur un point. Laquelle est l'intruse ?"
                else "Trois se ressemblent sur un point. Lequel est l'intrus ?",
                indices = listOf(critere.indice),
                choix = (trois + lIntrus).shuffled(alea),
                explication = "${lIntrus.nomComplet} : $minorite. Les trois autres : $majorite."
            )
        }
        return null
    }
}

/**
 * Les fiches ratées, à revoir : chacune revient jusqu'à ce qu'on l'ait
 * retrouvée deux fois ; une nouvelle erreur remet son compte à zéro.
 */
data class ARevoir(val compte: Map<String, Int> = emptyMap()) {

    val identifiants: Set<String> get() = compte.keys

    fun apres(identifiant: String, juste: Boolean): ARevoir = when {
        !juste -> ARevoir(compte + (identifiant to 0))
        identifiant !in compte -> this
        compte.getValue(identifiant) + 1 >= REUSSITES -> ARevoir(compte - identifiant)
        else -> ARevoir(compte + (identifiant to compte.getValue(identifiant) + 1))
    }

    companion object {
        const val REUSSITES = 2
    }
}

/** Réussite par marque : où l'œil reconnaît, où il confond. */
data class ScoreMarque(val marque: String, val justes: Int, val posees: Int) {
    val part: Double get() = if (posees == 0) 0.0 else justes.toDouble() / posees
}

data class BilanQuiz(val parMarque: Map<String, ScoreMarque> = emptyMap()) {

    fun apres(m: Materiel, juste: Boolean): BilanQuiz {
        val marque = QuizMateriel.marquePrincipale(m)
        val avant = parMarque[marque] ?: ScoreMarque(marque, 0, 0)
        return BilanQuiz(parMarque + (marque to avant.copy(justes = avant.justes + (if (juste) 1 else 0), posees = avant.posees + 1)))
    }

    /** Les marques assez jouées pour dire quelque chose, de la plus ratée à la mieux reconnue. */
    fun classement(minimum: Int = 3): List<ScoreMarque> =
        parMarque.values.filter { it.posees >= minimum }.sortedWith(compareBy({ it.part }, { -it.posees }))
}
