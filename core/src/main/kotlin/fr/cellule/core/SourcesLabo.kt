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

    /* ── La couleur : les publications Kodak des procédés chromogènes ── */

    val KODAK_Z131 = Source(
        "Kodak Alaris", "Using KODAK FLEXICOLOR Chemicals (Process C-41)", "publication Z-131", "",
        "https://business.kodakmoments.com/sites/default/files/wysiwyg/pro/chemistry/z131.pdf"
    )

    val KODAK_E6_KIT = Source(
        "Eastman Kodak", "KODAK PROFESSIONAL Single-Use Chemistry Kit, Process E-6", "TI-2443", "nov. 2008",
        "https://business.kodakmoments.com/sites/default/files/files/resources/ti2443.pdf"
    )

    /** Le manuel E-6, chapitre 3 : temps, températures et tolérances des bains. */
    val KODAK_Z119 = Source(
        "Eastman Kodak", "Using KODAK Chemicals, Process E-6", "publication Z-119", "6e édition",
        "https://kodak.com/global/plugins/acrobat/en/service/Zmanuals/z119-3.pdf"
    )

    val KODAK_ECN2 = Source(
        "Eastman Kodak", "Processing KODAK Motion Picture Films, Module 7 : Process ECN-2", "H-24.07", "2020",
        "https://www.kodak.com/content/products-brochures/Film/Processing-KODAK-Motion-Picture-Films-Module-7.pdf"
    )

    val KODAK_KIT_CINEMA = Source(
        "Eastman Kodak", "Using KODAK Kit Chemicals in Motion Picture Film Laboratories", "", "2020",
        "https://www.kodak.com/content/products-brochures/Film/Using-KODAK-Kit-Chemicals-in-Motion-Picture-Film-Laboratories.pdf"
    )

    val KODAK_RA4 = Source(
        "Kodak Alaris", "Process RA-4, temps et températures", "CIS-269", "",
        "https://business.kodakmoments.com/sites/default/files/wysiwyg/retailers/chemistry/techpub/CIS-269.pdf"
    )
}
