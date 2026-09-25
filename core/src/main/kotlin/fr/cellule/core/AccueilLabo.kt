package fr.cellule.core

/**
 * Les trois domaines du Labo. L'accueil ne montre que ce qui sert au domaine
 * choisi : les calculs du film, ceux du tirage — et, pour la couleur, rien
 * encore, faute de fiches relues.
 */
enum class DomaineLabo(val libelle: String, val calculateurs: List<Calculateur>) {
    FILM(
        "Film N&B",
        listOf(Calculateur.TEMPERATURE, Calculateur.PUSH_PULL, Calculateur.RECIPROCITE, Calculateur.DILUTION)
    ),
    TIRAGE("Tirage", listOf(Calculateur.TIRAGE, Calculateur.DILUTION)),
    COULEUR("Couleur", emptyList())
}

/** Le thème de quiz qui prolonge chaque calculateur. */
val Calculateur.theme: ThemeLabo
    get() = when (this) {
        Calculateur.TEMPERATURE -> ThemeLabo.TEMPERATURE
        Calculateur.PUSH_PULL -> ThemeLabo.PUSH
        Calculateur.RECIPROCITE -> ThemeLabo.RECIPROCITE
        Calculateur.DILUTION -> ThemeLabo.DILUTION
        Calculateur.TIRAGE -> ThemeLabo.DIAPHS
    }

/**
 * Les fiches par type de produit : on cherche « un révélateur liquide » ou
 * « un film 400 » avant de chercher une marque — la marque reste écrite
 * sur chaque fiche, et la recherche la trouve.
 *
 * Le rangement ne s'appuie que sur ce que les fiches relues disent : un film
 * dont la sensibilité n'a pas été relue n'est pas classé d'office ; un
 * révélateur connu seulement par la fiche d'un film reste à part tant que sa
 * propre fiche (et donc sa forme, poudre ou liquide) n'a pas été relue.
 */
enum class TypeFiche(val genre: GenreFiche, val libelle: String, val detail: String) {
    FILM_100(GenreFiche.FILM, "Films ISO 100", ""),
    FILM_400(GenreFiche.FILM, "Films ISO 400", ""),
    FILM_TRES_RAPIDE(GenreFiche.FILM, "Films très rapides", "publiés bien au-delà d'ISO 400"),
    FILM_A_RELIRE(GenreFiche.FILM, "Sensibilité à relire", "la fiche relue ne la donne pas"),
    REVELATEUR_POUDRE(GenreFiche.REVELATEUR, "Révélateurs en poudre", "à dissoudre, puis purs ou dilués"),
    REVELATEUR_LIQUIDE(GenreFiche.REVELATEUR, "Révélateurs liquides", "concentrés, à diluer"),
    REVELATEUR_CITE(GenreFiche.REVELATEUR, "Révélateurs cités par les fiches de films", "leur propre fiche reste à relire"),
    ARRET(GenreFiche.BAIN, "Bains d'arrêt", "juste après le révélateur"),
    FIXATEUR(GenreFiche.BAIN, "Fixateurs", "rendent le négatif insensible à la lumière"),
    FINITION(GenreFiche.BAIN, "Lavage et mouillant", "pour des négatifs propres et durables")
}

object RechercheFiches {

    /** Rangement explicite, fiche par fiche : aucune règle devinée sur le nom. */
    private val FILMS_100 = setOf("Kodak T-MAX 100")
    private val FILMS_400 = setOf("Ilford HP5 Plus", "Kodak Tri-X 400", "Kodak T-MAX 400")
    private val FILMS_TRES_RAPIDES = setOf("Ilford Delta 3200")
    private val POUDRES = setOf("D-76", "ID-11", "MICROPHEN", "PERCEPTOL")
    private val LIQUIDES = setOf("ILFOTEC DD-X", "ILFOSOL 3", "ILFOTEC HC", "ILFOTEC LC29")
    private val ARRETS = setOf("ILFOSTOP", "KODAK Indicator Stop Bath")
    private val FIXATEURS = setOf("RAPID FIXER", "HYPAM", "KODAK Fixer", "KODAK Rapid Fixer", "KODAFIX", "KODAK POLYMAX T")

    fun type(f: FicheLabo): TypeFiche = when (f.genre) {
        GenreFiche.FILM -> when (f.nom) {
            in FILMS_100 -> TypeFiche.FILM_100
            in FILMS_400 -> TypeFiche.FILM_400
            in FILMS_TRES_RAPIDES -> TypeFiche.FILM_TRES_RAPIDE
            else -> TypeFiche.FILM_A_RELIRE
        }
        GenreFiche.REVELATEUR -> when (f.nom) {
            in POUDRES -> TypeFiche.REVELATEUR_POUDRE
            in LIQUIDES -> TypeFiche.REVELATEUR_LIQUIDE
            else -> TypeFiche.REVELATEUR_CITE
        }
        GenreFiche.BAIN -> when (f.nom) {
            in ARRETS -> TypeFiche.ARRET
            in FIXATEURS -> TypeFiche.FIXATEUR
            else -> TypeFiche.FINITION
        }
    }

    /** Sans accents ni majuscules : « revelateur » trouve « Révélateur ». */
    private fun normal(t: String) = sansAccents(t).lowercase()

    /**
     * Les fiches dont le nom, le fabricant, le type, le résumé ou un fait
     * contient chaque mot cherché, dans le genre voulu (tous si [genre] est nul).
     */
    fun chercher(texte: String, genre: GenreFiche?, fiches: List<FicheLabo> = FichesLabo.TOUTES): List<FicheLabo> {
        val mots = normal(texte).split(Regex("\\s+")).filter { it.isNotBlank() }
        return fiches.filter { f ->
            (genre == null || f.genre == genre) && mots.all { mot ->
                val corps = normal(
                    listOf(f.nom, f.fabricant, f.resume, f.genre.libelle, type(f).libelle).joinToString(" ") + " " +
                        f.faits.joinToString(" ") { "${it.intitule} ${it.valeur} ${it.detail}" }
                )
                mot in corps
            }
        }
    }
}
