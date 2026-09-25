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
 * Les fiches par marque plutôt que par type : qui développe en HP5 Plus,
 * ID-11 et ILFOSTOP veut voir sa chaîne ensemble.
 */
enum class Marque(val libelle: String) { ILFORD("Ilford"), KODAK("Kodak"), AUTRES("Autres") }

object RechercheFiches {

    fun marque(f: FicheLabo): Marque = when {
        f.fabricant.startsWith("Ilford") -> Marque.ILFORD
        f.fabricant.startsWith("Kodak") -> Marque.KODAK
        else -> Marque.AUTRES
    }

    /** Sans accents ni majuscules : « revelateur » trouve « Révélateur ». */
    private fun normal(t: String) = sansAccents(t).lowercase()

    /**
     * Les fiches dont le nom, le fabricant, le résumé ou un fait contient
     * chaque mot cherché, dans la marque voulue (toutes si [marque] est nul).
     */
    fun chercher(texte: String, marque: Marque?, fiches: List<FicheLabo> = FichesLabo.TOUTES): List<FicheLabo> {
        val mots = normal(texte).split(Regex("\\s+")).filter { it.isNotBlank() }
        return fiches.filter { f ->
            (marque == null || marque(f) == marque) && mots.all { mot ->
                val corps = normal(
                    listOf(f.nom, f.fabricant, f.resume, f.genre.libelle).joinToString(" ") + " " +
                        f.faits.joinToString(" ") { "${it.intitule} ${it.valeur} ${it.detail}" }
                )
                mot in corps
            }
        }
    }
}
