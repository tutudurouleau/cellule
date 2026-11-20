package fr.cellule.core

/*
 * Les grands loueurs de matériel de tournage en région parisienne, et ce
 * que leurs catalogues en ligne proposent. Le relevé de leurs sites (outils/
 * loueurs.py, sur les machines de GitHub) dit QUI loue QUOI ; les chiffres
 * des fiches restent ceux des fabricants. Un catalogue en ligne n'est jamais
 * complet : « pas listé » ne veut pas dire « pas disponible » — on demande.
 */

enum class Departement(val libelle: String) {
    CAMERA("Caméra et optiques"),
    LUMIERE("Lumière"),
    MACHINERIE("Machinerie"),
    ELECTRIQUE("Électrique et énergie"),
    VEHICULES("Véhicules")
}

data class Loueur(
    val id: String,
    val nom: String,
    val ville: String,
    val site: String,
    val departements: List<Departement>,
    val presentation: String,
    /** La page du site où chercher une référence, si le site en a une. */
    val recherche: String = ""
)

object Loueurs {
    val TOUS: List<Loueur> = listOf(
        Loueur(
            "tsf", "TSF", "La Plaine Saint-Denis", "https://www.tsf.fr",
            listOf(Departement.CAMERA, Departement.LUMIERE, Departement.MACHINERIE, Departement.ELECTRIQUE, Departement.VEHICULES),
            "Le grand loueur français : caméras et optiques, lumière, machinerie (TSF Grip), groupes et véhicules, studios.",
            "https://www.tsf.fr/?s="
        ),
        Loueur(
            "rvz", "RVZ", "Ivry-sur-Seine", "https://rvz-location.fr",
            listOf(Departement.CAMERA, Departement.LUMIERE, Departement.MACHINERIE, Departement.ELECTRIQUE, Departement.VEHICULES),
            "Caméras, lumière, machinerie, énergie et véhicules ; aussi une branche régie.",
            "https://rvz-location.fr/?s="
        ),
        Loueur(
            "transpalux", "Transpalux", "Bry-sur-Marne", "https://transpalux.com",
            listOf(Departement.LUMIERE, Departement.MACHINERIE, Departement.ELECTRIQUE, Departement.VEHICULES),
            "Lumière, machinerie et groupes électrogènes, avec des agences en région (Lyon, Angoulême)."
        ),
        Loueur(
            "panavision", "Panavision Alga", "Aubervilliers", "https://fr.panavision.com",
            listOf(Departement.CAMERA, Departement.LUMIERE, Departement.MACHINERIE),
            "Caméras et optiques Panavision (qui ne se vendent pas : elles se louent), et toutes les grandes marques."
        ),
        Loueur(
            "nextshot", "Next Shot", "Saint-Denis", "https://www.nextshot.com",
            listOf(Departement.CAMERA, Departement.LUMIERE, Departement.MACHINERIE, Departement.VEHICULES),
            "Caméra, lumière, machinerie et véhicules."
        ),
        Loueur(
            "vantage", "Vantage Paris", "La Plaine Saint-Denis", "https://www.vantagefilm.com",
            listOf(Departement.CAMERA),
            "Les optiques anamorphiques Hawk et des caméras, avec un studio d'essais."
        ),
        Loueur(
            "cinesyl", "Cinesyl", "Aubervilliers", "https://www.cinesyl.com",
            listOf(Departement.MACHINERIE, Departement.CAMERA),
            "Machinerie : dollies, grues, têtes, travellings ; documentation technique de chaque appareil."
        )
    )

    fun loueur(id: String): Loueur? = TOUS.firstOrNull { it.id == id }

    /** Les loueurs dont le catalogue en ligne cite ce matériel (identifiant de fiche ou de projecteur). */
    fun chez(identifiant: String): List<Loueur> =
        DisponibiliteLoueurs.DISPO[identifiant].orEmpty().mapNotNull { loueur(it) }
}
