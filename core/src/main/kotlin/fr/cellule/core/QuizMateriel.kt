package fr.cellule.core

import kotlin.random.Random

enum class GenreQuestion(val libelle: String) {
    QUI_SUIS_JE("Qui suis-je ?"),
    QUEL_FILM("Quel film ?")
}

/**
 * Une question : la silhouette de la réponse, un énoncé, des indices du plus
 * difficile au plus facile, et des propositions qui contiennent la réponse.
 */
data class Question(
    val genre: GenreQuestion,
    val reponse: Materiel,
    val enonce: String,
    val indices: List<String>,
    val choix: List<Materiel>
) {
    val bonneReponse: Int get() = choix.indexOf(reponse)
}

object QuizMateriel {

    const val NOMBRE_DE_CHOIX = 4

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

    fun devoile(texte: String, m: Materiel): Boolean =
        termesRevelateurs(m).any { motif(it).containsMatchIn(texte) }

    /** Le titre sans ses précisions entre parenthèses, pour comparer deux crédits. */
    fun titreDeBase(titre: String): String = sansAccents(titre.substringBefore(" (")).trim()

    /** Du plus difficile au plus facile ; aucun ne nomme la réponse. */
    fun indices(m: Materiel): List<String> = buildList {
        add("Famille : ${m.categorie}")
        add("Apparition : ${m.libelleAnnee}")
        m.chiffresCles.take(3).forEach { add("${it.libelle} : ${it.valeur}") }
        m.films.firstOrNull()?.let { f ->
            add("Au générique de « ${f.titre} »" + if (f.artisan.isNotBlank()) " — ${f.artisan}" else "")
        }
        add("Fabricant : ${m.fabricant}")
        add(m.aRetenir)
    }.map { masquer(it, m) }

    fun question(
        genre: GenreQuestion,
        parmi: List<Materiel>,
        alea: Random,
        eviter: Materiel? = null
    ): Question? {
        val candidats = parmi
            .filter { genre != GenreQuestion.QUEL_FILM || it.films.isNotEmpty() }
            .let { liste -> liste.filter { it != eviter }.ifEmpty { liste } }
        if (candidats.isEmpty()) return null
        val reponse = candidats.random(alea)

        return when (genre) {
            GenreQuestion.QUI_SUIS_JE -> Question(
                genre = genre,
                reponse = reponse,
                enonce = "Qui suis-je ?",
                indices = indices(reponse),
                choix = propositions(reponse, parmi, alea) { true }
            )
            GenreQuestion.QUEL_FILM -> {
                val film = reponse.films.random(alea)
                val base = titreDeBase(film.titre)
                Question(
                    genre = genre,
                    reponse = reponse,
                    enonce = "${reponse.genre.question} a servi sur « ${film.titre} » ?",
                    indices = buildList {
                        if (film.artisan.isNotBlank()) add("Image ou réalisation : ${film.artisan}")
                        add("Famille : ${reponse.categorie}")
                        add("Apparition : ${reponse.libelleAnnee}")
                        add("Fabricant : ${reponse.fabricant}")
                    }.map { masquer(it, reponse) },
                    /* Un leurre crédité sur le même film rendrait la question fausse. */
                    choix = propositions(reponse, parmi, alea) { autre ->
                        autre.films.none { titreDeBase(it.titre) == base }
                    }
                )
            }
        }
    }

    /**
     * La réponse et des leurres du même genre, de préférence de la même
     * silhouette : l'image seule ne doit pas suffire à trancher.
     */
    private fun propositions(
        reponse: Materiel,
        parmi: List<Materiel>,
        alea: Random,
        admissible: (Materiel) -> Boolean
    ): List<Materiel> {
        val autres = parmi
            .filter { it.genre == reponse.genre && it.nomComplet != reponse.nomComplet && admissible(it) }
            .shuffled(alea)
        val proches = autres.filter { it.silhouette == reponse.silhouette }
        val leurres = (proches + autres.filterNot { it in proches }).take(NOMBRE_DE_CHOIX - 1)
        return (leurres + reponse).shuffled(alea)
    }
}
