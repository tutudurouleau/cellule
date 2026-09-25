package fr.cellule.core

/**
 * Un temps de développement tel qu'une fiche le publie : un film, un
 * révélateur à une dilution, un indice d'exposition, une température. C'est
 * la matière des fiches produits : chaque ligne vient d'un tableau imprimé,
 * rien n'est déduit.
 */
data class TempsPublie(
    val film: String,
    val revelateur: String,
    val dilution: String,
    val ei: Int,
    val temperature: Double,
    val minutes: Double,
    val source: Source
)

object TempsPublies {

    private fun lignes(
        film: String, source: Source, revelateur: String, dilution: String,
        temperature: Double, vararg paires: Pair<Int, Double>
    ) = paires.map { (ei, m) -> TempsPublie(film, revelateur, dilution, ei, temperature, m, source) }

    /* Ilford HP5 Plus, nov. 2018 : cuve spirale, agitation intermittente, 20 °C. */
    private fun hp5(revelateur: String, dilution: String, vararg paires: Pair<Int, Double>) =
        lignes("Ilford HP5 Plus", Sources.ILFORD_HP5, revelateur, dilution, 20.0, *paires)

    private val HP5 = listOf(
        hp5("ILFOTEC DD-X", "1+4", 400 to 9.0, 800 to 10.0, 1600 to 13.0, 3200 to 20.0),
        hp5("ILFOSOL 3", "1+9", 200 to 5.0, 400 to 6.5, 800 to 13.5),
        hp5("ILFOSOL 3", "1+14", 200 to 7.0, 400 to 11.0, 800 to 19.5),
        hp5("ILFOTEC HC", "1+15", 400 to 3.5, 800 to 5.0, 1600 to 7.5, 3200 to 11.0),
        hp5("ILFOTEC HC", "1+31", 400 to 6.5, 800 to 9.5, 1600 to 14.0),
        hp5("ILFOTEC LC29", "1+9", 400 to 3.5, 800 to 5.0, 1600 to 7.5, 3200 to 11.0),
        hp5("ILFOTEC LC29", "1+19", 400 to 6.5, 800 to 9.5, 1600 to 14.0),
        hp5("ILFOTEC LC29", "1+29", 400 to 9.0),
        hp5("ID-11", "stock", 400 to 7.5, 800 to 10.5, 1600 to 14.0),
        hp5("ID-11", "1+1", 400 to 13.0, 800 to 16.5),
        hp5("ID-11", "1+3", 400 to 20.0),
        hp5("MICROPHEN", "stock", 400 to 6.5, 800 to 8.0, 1600 to 11.0, 3200 to 16.0),
        hp5("MICROPHEN", "1+1", 400 to 12.0, 800 to 15.0),
        hp5("MICROPHEN", "1+3", 400 to 23.0),
        hp5("PERCEPTOL", "stock", 250 to 13.0),
        hp5("PERCEPTOL", "1+1", 320 to 18.0),
        hp5("PERCEPTOL", "1+3", 320 to 25.0),
        hp5("Rodinal", "1+25", 400 to 6.0, 800 to 8.0),
        hp5("Rodinal", "1+50", 400 to 11.0),
        hp5("D-76", "stock", 400 to 7.5, 800 to 9.5, 1600 to 12.5),
        hp5("D-76", "1+1", 400 to 11.0, 800 to 13.0),
        hp5("D-76", "1+3", 400 to 22.0),
        hp5("HC-110", "dilution A", 400 to 2.5, 800 to 3.75, 1600 to 5.5, 3200 to 9.5),
        hp5("HC-110", "dilution B", 400 to 5.0, 800 to 7.5, 1600 to 11.0),
        hp5("T-MAX Developer", "1+4", 400 to 6.5, 800 to 8.0, 1600 to 9.5, 3200 to 11.5),
        hp5("XTOL", "stock", 400 to 8.0, 800 to 11.0, 1600 to 14.0, 3200 to 19.0),
        hp5("XTOL", "1+1", 400 to 12.0, 800 to 17.0)
    ).flatten()

    /* Ilford Delta 3200, juin 2025 : cuve spirale, à 20 °C puis à 24 °C. */
    private fun delta(revelateur: String, dilution: String, temperature: Double, vararg paires: Pair<Int, Double>) =
        lignes("Ilford Delta 3200", Sources.ILFORD_DELTA_3200, revelateur, dilution, temperature, *paires)

    private val DELTA_3200 = listOf(
        delta("ILFOTEC DD-X", "1+4", 20.0, 400 to 6.0, 800 to 7.0, 1600 to 8.0, 3200 to 9.5, 6400 to 12.5, 12500 to 17.0),
        delta("ILFOSOL 3", "1+9", 20.0, 400 to 6.0, 800 to 7.5, 1600 to 10.0, 3200 to 11.0, 6400 to 18.0),
        delta("ILFOSOL 3", "1+14", 20.0, 400 to 11.0, 800 to 13.0, 1600 to 15.5, 3200 to 17.0, 6400 to 23.0),
        delta("ILFOTEC HC", "1+15", 20.0, 1600 to 5.0, 3200 to 8.0, 6400 to 13.0),
        delta("ILFOTEC HC", "1+31", 20.0, 400 to 6.0, 800 to 7.5, 1600 to 9.0, 3200 to 14.5),
        delta("ILFOTEC LC29", "1+9", 20.0, 1600 to 5.0, 3200 to 8.0, 6400 to 13.0),
        delta("ILFOTEC LC29", "1+19", 20.0, 400 to 6.0, 800 to 7.5, 1600 to 9.0, 3200 to 14.5),
        delta("ID-11", "stock", 20.0, 400 to 7.0, 800 to 8.0, 1600 to 9.5, 3200 to 10.5, 6400 to 13.0, 12500 to 17.0),
        delta("MICROPHEN", "stock", 20.0, 400 to 6.0, 800 to 7.0, 1600 to 8.0, 3200 to 9.0, 6400 to 12.0, 12500 to 16.5),
        delta("PERCEPTOL", "stock", 20.0, 400 to 11.0, 800 to 13.0, 1600 to 15.0, 3200 to 18.0),
        delta("ILFOTEC DD-X", "1+4", 24.0, 800 to 5.0, 1600 to 6.0, 3200 to 7.0, 6400 to 9.0, 12500 to 12.0),
        delta("ILFOSOL 3", "1+9", 24.0, 400 to 5.5, 800 to 7.0, 1600 to 8.0, 3200 to 9.0, 6400 to 15.5),
        delta("ILFOSOL 3", "1+14", 24.0, 400 to 7.0, 800 to 8.0, 1600 to 10.0, 3200 to 11.0, 6400 to 19.0),
        delta("ILFOTEC HC", "1+15", 24.0, 3200 to 5.5, 6400 to 8.5),
        delta("ILFOTEC HC", "1+31", 24.0, 400 to 5.0, 800 to 6.0, 1600 to 7.0, 3200 to 10.5),
        delta("ILFOTEC LC29", "1+9", 24.0, 3200 to 5.5, 6400 to 8.5),
        delta("ILFOTEC LC29", "1+19", 24.0, 400 to 5.0, 800 to 6.0, 1600 to 7.0, 3200 to 10.5),
        delta("ID-11", "stock", 24.0, 400 to 6.0, 800 to 7.0, 1600 to 8.0, 3200 to 9.0, 6400 to 11.0, 12500 to 13.5),
        delta("MICROPHEN", "stock", 24.0, 800 to 5.0, 1600 to 6.0, 3200 to 7.0, 6400 to 9.5, 12500 to 13.5),
        delta("PERCEPTOL", "stock", 24.0, 400 to 9.5, 800 to 10.5, 1600 to 12.0, 3200 to 15.5),
        // « Pour EI 25000, suivre ce guide » — et faire d'abord des essais.
        delta("ILFOTEC DD-X", "1+4", 20.0, 25000 to 25.0),
        delta("ILFOTEC DD-X", "1+4", 24.0, 25000 to 17.0),
        delta("MICROPHEN", "stock", 20.0, 25000 to 22.0),
        delta("MICROPHEN", "stock", 24.0, 25000 to 17.5)
    ).flatten()

    /* Kodak Tri-X 400, F-4017 : cuve, agitation toutes les 30 s. */
    private val TEMPERATURES_KODAK = listOf(18.0, 20.0, 21.0, 22.0, 24.0)

    private fun triX(revelateur: String, dilution: String, ei: Int, vararg m: Double?) =
        TEMPERATURES_KODAK.zip(m.toList()).mapNotNull { (t, minutes) ->
            minutes?.let { TempsPublie("Kodak Tri-X 400", revelateur, dilution, ei, t, it, Sources.KODAK_TRIX) }
        }

    private val TRI_X = listOf(
        triX("T-MAX Developer", "", 400, 6.75, 6.0, 5.75, 5.5, 4.75),
        triX("HC-110", "dilution B", 400, 4.5, 3.75, 3.5, 3.0, 2.5),
        triX("D-76", "stock", 400, 8.0, 6.75, 6.25, 5.5, 4.75),
        triX("D-76", "1+1", 400, 10.75, 9.75, 9.0, 8.5, 7.75),
        triX("XTOL", "stock", 400, 8.0, 7.0, 6.25, 5.75, 4.75),
        triX("XTOL", "1+1", 400, 10.0, 9.0, 8.5, 8.0, 7.25),
        triX("T-MAX Developer", "", 1600, 9.5, 8.75, 8.25, 7.75, 7.0),
        triX("HC-110", "dilution B", 1600, 7.0, 6.0, 5.5, 5.0, 4.25),
        triX("D-76", "stock", 1600, 11.25, 9.5, 8.75, 7.75, 6.5),
        triX("D-76", "1+1", 1600, 14.75, 13.25, 12.5, 11.75, 10.75),
        triX("XTOL", "stock", 1600, 11.25, 9.75, 8.75, 8.0, 6.75),
        triX("XTOL", "1+1", 1600, 14.5, 13.25, 12.25, 11.5, 10.5),
        triX("T-MAX Developer", "", 3200, null, null, null, null, 8.25),
        triX("D-76", "stock", 3200, 12.75, 11.0, 9.75, 9.0, 7.5),
        triX("D-76", "1+1", 3200, 17.5, 16.0, 15.0, 14.25, 12.75),
        triX("XTOL", "stock", 3200, null, 11.5, 10.5, 9.5, 8.0),
        triX("XTOL", "1+1", 3200, null, 15.5, 14.5, 13.75, 12.25)
    ).flatten()

    /* Kodak T-MAX 100 et 400, J-78 (fiche du D-76) : cuve, agitation toutes les 30 s. */
    private fun tmax(film: String, dilution: String, ei: Int, vararg m: Double) =
        TEMPERATURES_KODAK.zip(m.toList()).map { (t, minutes) ->
            TempsPublie(film, "D-76", dilution, ei, t, minutes, Sources.KODAK_D76)
        }

    private val T_MAX = listOf(
        tmax("Kodak T-MAX 100", "stock", 100, 10.5, 9.0, 8.0, 7.0, 6.0),
        tmax("Kodak T-MAX 100", "1+1", 100, 14.5, 12.0, 11.0, 10.0, 8.5),
        tmax("Kodak T-MAX 400", "stock", 400, 9.0, 8.0, 7.0, 6.5, 5.5),
        tmax("Kodak T-MAX 400", "1+1", 400, 14.5, 12.5, 11.0, 10.0, 9.0),
        // Tableau des films poussés du J-78, à 20 et 24 °C.
        listOf(
            TempsPublie("Kodak T-MAX 100", "D-76", "stock", 200, 20.0, 9.0, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 100", "D-76", "stock", 200, 24.0, 6.0, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 100", "D-76", "stock", 400, 20.0, 11.0, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 100", "D-76", "stock", 400, 24.0, 7.5, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 400", "D-76", "stock", 800, 20.0, 8.0, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 400", "D-76", "stock", 800, 24.0, 5.5, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 400", "D-76", "stock", 1600, 20.0, 10.5, Sources.KODAK_D76),
            TempsPublie("Kodak T-MAX 400", "D-76", "stock", 1600, 24.0, 7.0, Sources.KODAK_D76)
        )
    ).flatten()

    val LISTE: List<TempsPublie> = HP5 + DELTA_3200 + TRI_X + T_MAX

    fun pourFilm(film: String): List<TempsPublie> = LISTE.filter { it.film == film }

    fun pourRevelateur(revelateur: String): List<TempsPublie> = LISTE.filter { it.revelateur == revelateur }
}
