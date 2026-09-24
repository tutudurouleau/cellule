package fr.cellule.core

import java.text.Normalizer

/*
 * Le matériel de tournage : caméras et objectifs de cinéma, pour apprendre à
 * les reconnaître et à savoir pourquoi on choisit l'un plutôt que l'autre.
 *
 * Une fiche se lit à deux niveaux. La fiche récap (nom, punchline, chiffres
 * clés) se retient d'un coup d'œil ; la fiche complète explique d'où vient le
 * matériel, comment on s'en sert sur un plateau, ce qu'il fait mieux que les
 * autres et ce qu'il fait moins bien.
 */

/** Un repère chiffré de la fiche récap. */
data class Paire(val libelle: String, val valeur: String)

/** Un film où le matériel a servi, et qui tenait l'image (ou la réalisation). */
data class ReferenceFilm(val titre: String, val artisan: String = "")

enum class GenreMateriel(val libelle: String, val question: String) {
    CAMERA("Caméra", "Quelle caméra"),
    OBJECTIF("Objectif", "Quel objectif")
}

/**
 * La forme d'ensemble dessinée dans le quiz. Elle dit la famille — caméra
 * d'épaule, cube compact, zoom de studio — jamais le modèle : c'est aux
 * indices de faire le reste.
 */
enum class Silhouette(val genre: GenreMateriel) {
    STUDIO(GenreMateriel.CAMERA),
    COMPACTE(GenreMateriel.CAMERA),
    EPAULE(GenreMateriel.CAMERA),
    REFLEX(GenreMateriel.CAMERA),
    ARGENTIQUE(GenreMateriel.CAMERA),
    CAISSON(GenreMateriel.CAMERA),
    TOURELLE(GenreMateriel.CAMERA),
    STABILISEE(GenreMateriel.CAMERA),
    FIXE(GenreMateriel.OBJECTIF),
    ZOOM(GenreMateriel.OBJECTIF),
    ANAMORPHIQUE(GenreMateriel.OBJECTIF),
    ANCIENNE(GenreMateriel.OBJECTIF)
}

enum class Disponibilite(val libelle: String) {
    ACHAT("À l'achat comme en location"),
    LOCATION("Surtout en location, par quelques loueurs"),
    LOCATION_EXCLUSIVE("Location exclusive : jamais vendue"),
    PLUS_FABRIQUEE("Plus fabriquée : location, occasion ou réhoussage")
}

sealed interface Materiel {
    val genre: GenreMateriel
    val nom: String
    val fabricant: String
    val annee: Int
    val anneeApprox: Boolean
    val categorie: String
    val silhouette: Silhouette
    val punchline: String
    val chiffresCles: List<Paire>
    val histoire: String
    val ficheTechnique: String
    val commentLUtiliser: String
    val atouts: String
    val limites: String
    val films: List<ReferenceFilm>
    val aRetenir: String

    /** « ARRIFLEX 16SR » plutôt que « ARRI ARRIFLEX 16SR ». */
    val nomComplet: String
        get() = if (nom.startsWith(fabricant, ignoreCase = true)) nom else "$fabricant $nom"
    val libelleAnnee: String get() = if (anneeApprox) "vers $annee" else annee.toString()

    /** Identifiant stable, sans accents ni espaces — « arri-alexa-mini ». Nomme les photos. */
    val identifiant: String
        get() = sansAccents(nomComplet).replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

data class Camera(
    override val nom: String,
    override val fabricant: String,
    override val annee: Int,
    override val categorie: String,
    override val silhouette: Silhouette,
    override val punchline: String,
    override val chiffresCles: List<Paire>,
    override val histoire: String,
    override val ficheTechnique: String,
    override val commentLUtiliser: String,
    override val atouts: String,
    override val limites: String,
    override val films: List<ReferenceFilm>,
    override val aRetenir: String,
    override val anneeApprox: Boolean = false
) : Materiel {
    override val genre: GenreMateriel get() = GenreMateriel.CAMERA
}

data class Objectif(
    override val nom: String,
    override val fabricant: String,
    override val annee: Int,
    override val categorie: String,
    override val silhouette: Silhouette,
    val focales: String,
    val ouverture: String,
    val couverture: String,
    val monture: String,
    val disponibilite: Disponibilite,
    override val punchline: String,
    override val histoire: String,
    override val ficheTechnique: String,
    override val commentLUtiliser: String,
    override val atouts: String,
    override val limites: String,
    override val films: List<ReferenceFilm>,
    override val aRetenir: String,
    override val anneeApprox: Boolean = false
) : Materiel {
    override val genre: GenreMateriel get() = GenreMateriel.OBJECTIF
    override val chiffresCles: List<Paire>
        get() = listOf(
            Paire("Focales", focales),
            Paire("Ouverture", ouverture),
            Paire("Couverture", couverture),
            Paire("Monture", monture)
        )
}

/** Un rayon du catalogue : une marque, une époque, une famille. */
data class Etagere<out T : Materiel>(val titre: String, val entrees: List<T>)

/* Petites notations pour garder le catalogue lisible. */
internal infix fun String.vaut(valeur: String) = Paire(this, valeur)
internal infix fun String.par(artisan: String) = ReferenceFilm(this, artisan)

val CATALOGUE_CAMERAS: List<Camera> by lazy { ETAGERES_CAMERAS.flatMap { it.entrees } }
val CATALOGUE_OBJECTIFS: List<Objectif> by lazy { ETAGERES_OBJECTIFS.flatMap { it.entrees } }
val CATALOGUE_MATERIEL: List<Materiel> by lazy { CATALOGUE_CAMERAS + CATALOGUE_OBJECTIFS }

/** Sans accents ni majuscules : « camera » doit trouver « Caméflex ». */
fun sansAccents(texte: String): String =
    Normalizer.normalize(texte, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()

/**
 * Recherche dans le catalogue : chaque mot de la requête doit apparaître
 * quelque part dans le nom, la marque, la famille ou les films.
 */
fun <T : Materiel> chercherMateriel(requete: String, parmi: List<T>): List<T> {
    val mots = sansAccents(requete).split(Regex("\\s+")).filter { it.isNotBlank() }
    if (mots.isEmpty()) return parmi
    return parmi.filter { m ->
        val botte = sansAccents(
            buildString {
                append(m.nomComplet).append(' ').append(m.categorie)
                m.films.forEach { append(' ').append(it.titre).append(' ').append(it.artisan) }
            }
        )
        mots.all { it in botte }
    }
}
