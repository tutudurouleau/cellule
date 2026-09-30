package fr.cellule.core

/**
 * Un tableau de temps publiés, tel qu'une fiche l'affiche : une ligne par
 * indice d'exposition, une colonne par dilution (quand tout est publié à une
 * seule température) ou par température (sinon, un tableau par dilution).
 *
 * [cellules] suit l'ordre de [colonnes] ; null quand le fabricant ne publie
 * pas ce temps-là.
 */
data class GrilleTemps(
    val titre: String?,
    val colonnes: List<String>,
    val lignes: List<LigneGrille>,
    val temperature: Double?,
    val source: Source,
    val eiNominal: Int?
)

data class LigneGrille(val ei: Int, val cellules: List<Double?>)

/**
 * Les temps publiés d'une fiche, rangés pour se lire d'un coup d'œil.
 *
 * Une fiche de film publie des temps pour une quinzaine de révélateurs ; une
 * fiche de révélateur, pour plusieurs films. Plutôt que de tout dérouler, on
 * choisit d'abord l'un d'eux ([choix]), puis on lit son tableau ([grilles]).
 * Rien n'est calculé ici : chaque case est une ligne de [TempsPublies].
 */
object TempsFiche {

    private fun lignes(f: FicheLabo): List<TempsPublie> = when (f.genre) {
        GenreFiche.FILM -> TempsPublies.pourFilm(f.cleTemps)
        GenreFiche.REVELATEUR -> TempsPublies.pourRevelateur(f.cleTemps)
        GenreFiche.BAIN -> emptyList()
    }

    /** Pour un film, on choisit le révélateur ; pour un révélateur, le film. */
    private fun cle(f: FicheLabo, t: TempsPublie): String =
        if (f.genre == GenreFiche.FILM) t.revelateur else t.film

    /** Ce qu'on peut choisir sur la fiche, dans l'ordre où le fabricant le publie. */
    fun choix(f: FicheLabo): List<String> = lignes(f).map { cle(f, it) }.distinct()

    /** La sensibilité nominale d'un film, quand une table de push la donne. */
    private fun nominal(film: String): Int? = Push.TABLES.firstOrNull { it.film == film }?.iso

    fun grilles(f: FicheLabo, choisi: String): List<GrilleTemps> {
        val l = lignes(f).filter { cle(f, it) == choisi }
        if (l.isEmpty()) return emptyList()
        val film = if (f.genre == GenreFiche.FILM) f.cleTemps else choisi
        val ei = nominal(film)
        val dilutions = l.map { it.dilution }.distinct()
        val temperatures = l.map { it.temperature }.distinct().sorted()

        /* Une seule température : un seul tableau, une colonne par dilution. */
        if (temperatures.size == 1) {
            return listOf(
                GrilleTemps(
                    titre = null,
                    colonnes = dilutions.map { it.ifBlank { "temps" } },
                    lignes = l.map { it.ei }.distinct().sorted().map { e ->
                        LigneGrille(e, dilutions.map { d -> l.firstOrNull { it.ei == e && it.dilution == d }?.minutes })
                    },
                    temperature = temperatures.single(),
                    source = l.first().source,
                    eiNominal = ei
                )
            )
        }

        /* Plusieurs températures : un tableau par dilution, une colonne par température. */
        return dilutions.map { d ->
            val ld = l.filter { it.dilution == d }
            val temps = ld.map { it.temperature }.distinct().sorted()
            GrilleTemps(
                titre = d.ifBlank { null },
                colonnes = temps.map { "${nombreSansZero(it)} °C" },
                lignes = ld.map { it.ei }.distinct().sorted().map { e ->
                    LigneGrille(e, temps.map { t -> ld.firstOrNull { it.ei == e && it.temperature == t }?.minutes })
                },
                temperature = null,
                source = ld.first().source,
                eiNominal = ei
            )
        }
    }

    /** Ce que la fiche fermée annonce de son contenu : « Temps avec 15 révélateurs ». */
    fun contenu(f: FicheLabo): String? {
        val n = choix(f).size
        if (n == 0) return null
        return when (f.genre) {
            GenreFiche.FILM -> if (n == 1) "Temps avec 1 révélateur" else "Temps avec $n révélateurs"
            GenreFiche.REVELATEUR -> if (n == 1) "Temps pour 1 film" else "Temps pour $n films"
            GenreFiche.BAIN -> null
        }
    }
}
