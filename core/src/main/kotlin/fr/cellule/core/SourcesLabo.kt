package fr.cellule.core

/**
 * D'où vient un chiffre du Labo : la fiche technique du fabricant, avec sa
 * référence et son édition, pour pouvoir y retourner sans l'appli. Même
 * logique que le crédit sous chaque photo du catalogue Caméras.
 *
 * Chaque chiffre a été relu dans le PDF du fabricant ; aucun ne vient d'une
 * base recopiée.
 */
data class Source(
    val fabricant: String,
    val document: String,
    val reference: String,
    val edition: String,
    val adresse: String
) {
    /** « Kodak Alaris · KODAK Developer D-76, J-78 (déc. 2017) » */
    val citation: String
        get() = buildString {
            append("$fabricant · $document")
            if (reference.isNotBlank()) append(", $reference")
            if (edition.isNotBlank()) append(" ($edition)")
        }
}

object Sources {

    val KODAK_D76 = Source(
        "Kodak Alaris", "KODAK Developer D-76", "publication J-78", "déc. 2017",
        "https://imaging.kodakalaris.com/sites/prod/files/files/resources/j78.pdf"
    )

    val KODAK_TRIX = Source(
        "Kodak Alaris", "KODAK PROFESSIONAL TRI-X 320 and 400 Films", "publication F-4017", "",
        "https://business.kodakmoments.com/sites/default/files/files/resources/f4017_TriX.pdf"
    )

    val ILFORD_HP5 = Source(
        "Ilford (HARMAN technology)", "HP5 PLUS, Technical Information", "", "nov. 2018",
        "https://www.ilfordphoto.com/amfile/file/download/file/1903/product/691/"
    )

    val ILFORD_PANF = Source(
        "Ilford (HARMAN technology)", "PAN F PLUS, Technical Information", "", "déc. 2018",
        "https://www.ilfordphoto.com/amfile/file/download/file/1905/product/699/"
    )

    val ILFORD_DELTA_3200 = Source(
        "Ilford (HARMAN technology)", "DELTA 3200 PROFESSIONAL, Technical Information", "", "juin 2025",
        "https://www.ilfordphoto.com/amfile/file/download/file/1913/product/682/"
    )

    val ILFORD_ID11 = Source(
        "Ilford (HARMAN technology)", "PERCEPTOL, ID-11 and MICROPHEN film developers", "", "août 2024",
        "https://www.ilfordphoto.com/amfile/file/download/file/1829/product/551/"
    )
}
