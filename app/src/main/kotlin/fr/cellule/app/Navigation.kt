package fr.cellule.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/*
 * La carte de l'appli, en un seul endroit : chaque page, son nom, son
 * parent. Le lanceur, la barre épinglée, les sommaires et le fil d'Ariane
 * lisent tous cette liste — une page ajoutée ici apparaît partout à la fois.
 *
 * Les identifiants ne changent jamais : la barre épinglée et les récents
 * les gardent en mémoire d'une version à l'autre.
 */

/**
 * Un écran demande à un autre d'ouvrir une de ses pages : « TABLES:FORMULES ».
 * L'écran visé la prend en s'affichant, et elle disparaît.
 */
object Aiguillage {
    var cible by mutableStateOf("")

    fun prendre(ecran: String): String? {
        val c = cible
        if (!c.startsWith("$ecran:")) return null
        cible = ""
        return c.substringAfter(':')
    }
}

/** La valeur de [Reglages.lumiereOutil] qui ouvre le sommaire Lumière plutôt qu'un outil. */
const val OUTIL_SOMMAIRE = "SOMMAIRE"

internal enum class Univers(val titre: String) {
    TERRAIN("Photo · sur le terrain"),
    APRES("Photo · après la prise"),
    CINEMA("Cinéma · tournage"),
    LUMIERE("Cinéma · lumière"),
    SAVOIR("Apprendre et retrouver")
}

/** Une page de l'appli où l'on peut aller d'un toucher, où qu'elle soit rangée. */
internal data class Destination(
    val id: String,
    val onglet: Onglet,
    val univers: Univers,
    val titre: String,
    /** Son nom dans la barre du bas : neuf lettres au plus. */
    val court: String,
    /** À quoi elle sert, en une ligne : sous son titre, dans le lanceur et les sommaires. */
    val detail: String,
    /** Ce qu'on taperait pour la chercher. */
    val mots: String = "",
    /** La page à ouvrir dans l'écran, par l'[Aiguillage]. */
    val sous: String = "",
    val cineVue: String? = null,
    val outil: String? = null,
    /** Le sommaire qui la range ; null pour une page racine. */
    val parent: String? = null,
    /** Son groupe dans le sommaire de son parent. */
    val groupe: String = "",
    /** Son icône dans les sommaires ; à défaut, celle de son onglet. */
    val icone: ImageVector? = null,
    /** Faux pour une page qui n'a pas encore d'écran à elle : elle sert au fil d'Ariane, pas au lanceur. */
    val dansLanceur: Boolean = true
)

private fun lumiere(id: String, outil: String, groupe: String, icone: ImageVector, titre: String, court: String, detail: String, mots: String) =
    Destination(
        id, Onglet.CINEMA, Univers.LUMIERE, titre, court, detail, mots,
        cineVue = "LUMIERE", outil = outil, parent = "lumiere", groupe = groupe, icone = icone
    )

private const val PLACER = "Placer la lumière"
private const val COULEUR = "Couleur et image"
private const val PLATEAU = "Sur le plateau"

internal val DESTINATIONS: List<Destination> by lazy {
    listOf(
        Destination("mesurer", Onglet.MESURER, Univers.TERRAIN, "Mesurer", "Mesurer", "Le posemètre : réfléchi, spot, incident.", "cellule lumiere exposition ev spot incident posemetre"),
        Destination("estimer", Onglet.ESTIMER, Univers.TERRAIN, "Estimer", "Estimer", "Trouver l'exposition sans cellule, facteur par facteur.", "sunny 16 soleil f/16 exposition sans cellule"),
        Destination("focales", Onglet.FOCALES, Univers.TERRAIN, "Focales", "Focales", "Voir les angles de champ, s'entraîner à cadrer.", "angle de champ cadre objectif exercice reconnaitre"),

        Destination("carnet-vues", Onglet.CARNET, Univers.APRES, "Carnet · vues", "Carnet", "Ce que tu as photographié, et ton biais.", "journal notes photos pellicule", sous = "CARNET:VUES"),
        Destination("carnet-films", Onglet.CARNET, Univers.APRES, "Carnet · films", "Films", "Tes développements notés.", "developpement pellicule revelateur", sous = "CARNET:DEVELOPPEMENTS"),
        Destination("carnet-tirages", Onglet.CARNET, Univers.APRES, "Carnet · tirages", "Tirages", "Tes tirages notés.", "papier agrandisseur tirage", sous = "CARNET:TIRAGES"),
        Destination("labo-quiz", Onglet.LABO, Univers.APRES, "Quiz du labo", "Quiz labo", "Bains, dilutions, push, réciprocité, diaphs, couleur.", "quiz labo entrainement question bains revelateur couleur", sous = "LABO:QUIZ"),
        Destination("labo-couleur", Onglet.LABO, Univers.APRES, "Labo couleur", "Couleur", "C-41, ECN-2, E-6, RA-4 : les bains, le chrono, la couleur expliquée.", "couleur c41 c-41 ecn2 ecn-2 e6 e-6 ra4 ra-4 diapo diapositive negatif couleur blanchiment vision3 portra ektachrome kodak", sous = "LABO:COULEUR"),
        Destination("labo-dominante", Onglet.LABO, Univers.APRES, "Corriger une dominante", "Dominante", "La filtration de l'agrandisseur couleur, corrigée.", "dominante filtration filtre jaune magenta cyan tirage couleur ra-4 agrandisseur", sous = "LABO:DOMINANTE"),
        Destination("labo", Onglet.LABO, Univers.APRES, "Labo", "Labo", "Développer, tirer, le chrono, et comprendre pourquoi.", "chrono minuteur revelateur fixateur dilution temperature tirage bande d'essai"),

        /* Le sommaire Cinéma, puis ses quatre pages dans l'ordre de ses cartes. */
        Destination("cinema", Onglet.CINEMA, Univers.CINEMA, "Cinéma", "Cinéma", "Le viseur, les plans, la lumière et le matériel.", "tournage sommaire accueil", cineVue = "ACCUEIL"),
        Destination("cine-viseur", Onglet.CINEMA, Univers.CINEMA, "Viseur", "Viseur", "Le cadre à ta focale, l'exposition, capturer un plan.", "camera cadre focale capturer reperage tournage", cineVue = "VISEUR", parent = "cinema", icone = IconeViseur),
        Destination("cine-plans", Onglet.CINEMA, Univers.CINEMA, "Plans", "Plans", "Les jours de tournage, les plans, le PDF.", "jour scene plan pdf decoupage carnet de tournage", cineVue = "PLANS", parent = "cinema", icone = IconePlans),

        Destination(
            "lumiere", Onglet.CINEMA, Univers.LUMIERE, "Lumière", "Lumière", "Les outils du plateau : placer, doser, corriger la lumière.",
            "eclairage outils plateau sommaire", cineVue = "LUMIERE", outil = OUTIL_SOMMAIRE, parent = "cinema", icone = IconeLumiere
        ),
        Destination("cine-materiel", Onglet.CINEMA, Univers.CINEMA, "Matériel", "Matériel", "Pellicule ou caméra, objectifs, et ton propre matériel.", "camera capteur pellicule objectif serie mon materiel projecteurs", cineVue = "MATERIEL", parent = "cinema", icone = IconeMateriel),
        lumiere("lum-plan", "PLAN_SOL", PLACER, IconePlanSol, "Plan au sol", "Plan sol", "Le décor vu de dessus : caméras, comédiens, projecteurs, soleil.", "plan de feu implantation carte vue camera 3d deplacer projecteur"),
        lumiere("lum-combien", "PROJECTEUR", PLACER, IconeProjecteur, "Combien j'ai", "Lux", "Les lux et le diaph qu'un projecteur donne à une distance.", "lux eclairement distance diaph projecteur footcandle"),
        lumiere("lum-placer", "PLACER", PLACER, IconePlacer, "Où le placer", "Placer", "La distance pour un diaph, ou pour couvrir une largeur.", "distance reculer avancer couvrir faisceau plan au sol"),
        lumiere("lum-rapports", "RAPPORTS", PLACER, IconeRapports, "Rapports", "Rapports", "Face, débouchage, contre : contraste et ambiance.", "contraste ratio key fill ambiance high key low key"),
        lumiere("lum-couleur", "COULEUR", COULEUR, IconeCouleur, "Couleur", "Couleur", "Kelvins, mireds et gélatines de correction.", "mired kelvin cto ctb gelatine temperature de couleur lee rosco"),
        lumiere("lum-champ", "CHAMP", COULEUR, IconeChamp, "Champ", "Champ", "Ce que couvre une focale, hyperfocale, profondeur de champ.", "profondeur de champ hyperfocale angle largeur nettete"),
        lumiere("lum-conv", "CONVERSIONS", COULEUR, IconeConversions, "Conversions", "Convers.", "Lux et footcandles, mètres et pieds, densités, diaphs.", "footcandle pieds nd densite transmission t-stop"),
        lumiere("lum-elec", "ELECTRICITE", PLATEAU, IconeElectricite, "Électricité", "Élec", "Ampères, circuits, groupe électrogène.", "ampere watt disjoncteur prise circuit groupe kva triphase"),
        lumiere("lum-scint", "SCINTILLEMENT", PLATEAU, IconeScintillement, "Scintillement", "Scintill.", "Les cadences et les angles sûrs sous le secteur.", "flicker obturateur 50 hz 60 hz hmi cadence ralenti"),

        Destination("lexique", Onglet.REVISER, Univers.SAVOIR, "Les mots du plateau", "Lexique", "Le jargon du tournage, et tes propres mots à réviser.", "lexique jargon vocabulaire mots plateau argot dictionnaire definition", sous = "REVISER:LEXIQUE"),
        Destination("marques", Onglet.REVISER, Univers.SAVOIR, "Les marques du cinéma", "Marques", "Qui fabrique quoi : caméras, optiques, lumière, machinerie.", "marques fabricants arri cooke zeiss matthews kupo aputure", sous = "REVISER:MARQUES"),
        Destination("fiches-machinerie", Onglet.CAMERAS, Univers.SAVOIR, "Fiches · machinerie", "Machino", "Clamps, pieds, cadres, dollies, grues : à quoi ça sert, comment on le nomme.", "machinerie machino grip clamp mafer cardellini pied c-stand apple box gueuse dolly rail grue drapeau", sous = "CAMERAS:MACHINERIE"),
        Destination("fiches-electrique", Onglet.CAMERAS, Univers.SAVOIR, "Fiches · électrique", "Élec", "P17, Powerlock, Socapex, prolongs, groupe, distribution.", "electrique electricite p17 prolongateur prolong powerlock socapex groupe electrogene triplette distribution dmx", sous = "CAMERAS:ELECTRIQUE"),
        Destination("reviser", Onglet.REVISER, Univers.SAVOIR, "Réviser", "Réviser", "Tous les quiz : formules du BTS, matériel, labo, focales.", "quiz exercices formules reviser entrainement examen concours bts"),
        Destination("fiches-cameras", Onglet.CAMERAS, Univers.SAVOIR, "Fiches caméras", "Caméras", "L'histoire et l'usage de chaque caméra.", "arri alexa red sony panavision camera catalogue", sous = "CAMERAS:CAMERA"),
        Destination("fiches-objectifs", Onglet.CAMERAS, Univers.SAVOIR, "Fiches objectifs", "Objectifs", "Les grandes séries d'optiques de cinéma.", "cooke zeiss angenieux anamorphique optique serie", sous = "CAMERAS:OBJECTIF"),
        Destination("fiches-projecteurs", Onglet.CAMERAS, Univers.SAVOIR, "Fiches projecteurs", "Projos", "63 projecteurs, avec leur photométrie.", "aputure skypanel hmi fresnel led tube lumiere projo", sous = "CAMERAS:PROJECTEUR"),
        Destination("quiz", Onglet.CAMERAS, Univers.SAVOIR, "Quiz", "Quiz", "Reconnaître caméras et objectifs.", "jeu question reviser entrainement", sous = "CAMERAS:QUIZ"),
        Destination("tables-exposition", Onglet.TABLES, Univers.SAVOIR, "Tables · exposition", "Tables", "Les repères d'exposition à retenir.", "ev iso vitesse diaph reciprocite", sous = "TABLES:EXPOSITION"),
        Destination("tables-developpement", Onglet.TABLES, Univers.SAVOIR, "Tables · développement", "Dév.", "Temps, températures, dilutions.", "revelateur temps temperature dilution pousse", sous = "TABLES:DEVELOPPEMENT"),
        Destination("tables-formules", Onglet.TABLES, Univers.SAVOIR, "Tables · formules", "Formules", "Toutes les formules que l'appli utilise.", "formule calcul loi carre inverse mired gelatines", sous = "TABLES:FORMULES")
    )
}

internal fun destination(id: String): Destination? = DESTINATIONS.firstOrNull { it.id == id }

/** Les pages qu'un sommaire range, dans l'ordre de la liste. */
internal fun enfants(id: String): List<Destination> = DESTINATIONS.filter { it.parent == id }

/** Le chemin jusqu'à une page, de la racine à son parent (la page elle-même exclue). */
internal fun chemin(id: String): List<Destination> {
    val r = ArrayDeque<Destination>()
    var p = destination(id)?.parent
    /* Cinq niveaux au plus : une boucle dans la liste ne gèlerait pas l'appli. */
    while (p != null && r.size < 5) {
        val d = destination(p) ?: break
        r.addFirst(d)
        p = d.parent
    }
    return r.toList()
}

/* ── Les icônes des sommaires, dans le trait des onglets ────────────────── */

/** Un point d'interrogation dans un cercle : réviser. */
internal val IconeReviser = icone("reviser") {
    moveTo(3.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 20.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 3.5f, 12f); close()
    moveTo(9.6f, 9.6f); arcTo(2.5f, 2.5f, 0f, true, true, 13.4f, 11.6f)
    lineTo(12f, 12.8f); lineTo(12f, 14f)
    moveTo(12f, 16.8f); lineTo(12f, 16.9f)
}

/** Un cadre 16:9 et sa croix de centre : le viseur. */
internal val IconeViseur = icone("viseur") {
    moveTo(3f, 6f); lineTo(21f, 6f); lineTo(21f, 18f); lineTo(3f, 18f); close()
    moveTo(12f, 10f); lineTo(12f, 14f)
    moveTo(10f, 12f); lineTo(14f, 12f)
}

/** Deux vignettes et leurs notes : le découpage en plans. */
internal val IconePlans = icone("plans") {
    moveTo(3.5f, 4f); lineTo(11f, 4f); lineTo(11f, 10f); lineTo(3.5f, 10f); close()
    moveTo(13.5f, 5.5f); lineTo(20.5f, 5.5f)
    moveTo(13.5f, 8.5f); lineTo(18.5f, 8.5f)
    moveTo(3.5f, 14f); lineTo(11f, 14f); lineTo(11f, 20f); lineTo(3.5f, 20f); close()
    moveTo(13.5f, 15.5f); lineTo(20.5f, 15.5f)
    moveTo(13.5f, 18.5f); lineTo(18.5f, 18.5f)
}

/** Un objectif de profil, ses bagues et sa lentille frontale : le matériel. */
internal val IconeMateriel = icone("materiel") {
    moveTo(4f, 8f); lineTo(13f, 8f); lineTo(13f, 16f); lineTo(4f, 16f); close()
    moveTo(13f, 6.5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(13f, 17.5f); close()
    moveTo(7f, 8f); lineTo(7f, 16f)
    moveTo(10f, 8f); lineTo(10f, 16f)
}

/** Une ampoule : la lumière. */
internal val IconeLumiere = icone("lumiere") {
    moveTo(9.2f, 14.5f); arcTo(5.5f, 5.5f, 0f, true, true, 14.8f, 14.5f)
    lineTo(14.5f, 17f); lineTo(9.5f, 17f); close()
    moveTo(10f, 19.8f); lineTo(14f, 19.8f)
}

/** Une pièce vue de dessus : un mur, une lampe, le champ d'une caméra. */
internal val IconePlanSol = icone("plan-sol") {
    moveTo(3.5f, 3.5f); lineTo(20.5f, 3.5f); lineTo(20.5f, 20.5f); lineTo(3.5f, 20.5f); close()
    moveTo(13.5f, 3.5f); lineTo(13.5f, 10f)
    moveTo(6f, 8.5f); arcTo(2f, 2f, 0f, true, true, 10f, 8.5f); arcTo(2f, 2f, 0f, true, true, 6f, 8.5f); close()
    moveTo(11.5f, 13.5f); lineTo(17f, 16.25f); lineTo(11.5f, 19f)
}

/** Un projecteur sur son pied, et ses rayons. */
internal val IconeProjecteur = icone("projecteur") {
    moveTo(3.5f, 8f); lineTo(11f, 8f); lineTo(11f, 15f); lineTo(3.5f, 15f); close()
    moveTo(7.25f, 15f); lineTo(7.25f, 20.5f)
    moveTo(5f, 20.5f); lineTo(9.5f, 20.5f)
    moveTo(14f, 8.5f); lineTo(20.5f, 5.5f)
    moveTo(14f, 11.5f); lineTo(21f, 11.5f)
    moveTo(14f, 14.5f); lineTo(20.5f, 17.5f)
}

/** Une source et la distance qui la sépare du sujet. */
internal val IconePlacer = icone("placer") {
    moveTo(3.3f, 12f); arcTo(2.2f, 2.2f, 0f, true, true, 7.7f, 12f); arcTo(2.2f, 2.2f, 0f, true, true, 3.3f, 12f); close()
    moveTo(10f, 12f); lineTo(20.5f, 12f)
    moveTo(17.8f, 9.3f); lineTo(20.5f, 12f); lineTo(17.8f, 14.7f)
    moveTo(12.7f, 9.3f); lineTo(10f, 12f); lineTo(12.7f, 14.7f)
}

/** Un visage mi-éclairé, mi-dans l'ombre : le contraste. */
internal val IconeRapports = icone("rapports") {
    moveTo(3.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 20.5f, 12f); arcTo(8.5f, 8.5f, 0f, true, true, 3.5f, 12f); close()
    moveTo(12f, 3.5f); lineTo(12f, 20.5f)
    moveTo(14.2f, 8f); lineTo(18.6f, 8f)
    moveTo(14.2f, 12f); lineTo(19.5f, 12f)
    moveTo(14.2f, 16f); lineTo(18.6f, 16f)
}

/** Trois disques qui se recouvrent : la couleur. */
internal val IconeCouleur = icone("couleur") {
    moveTo(7.4f, 8.6f); arcTo(4.6f, 4.6f, 0f, true, true, 16.6f, 8.6f); arcTo(4.6f, 4.6f, 0f, true, true, 7.4f, 8.6f); close()
    moveTo(4f, 14.6f); arcTo(4.6f, 4.6f, 0f, true, true, 13.2f, 14.6f); arcTo(4.6f, 4.6f, 0f, true, true, 4f, 14.6f); close()
    moveTo(10.8f, 14.6f); arcTo(4.6f, 4.6f, 0f, true, true, 20f, 14.6f); arcTo(4.6f, 4.6f, 0f, true, true, 10.8f, 14.6f); close()
}

/** L'angle d'un objectif, et deux plans nets. */
internal val IconeChamp = icone("champ") {
    moveTo(20.5f, 5f); lineTo(4f, 12f); lineTo(20.5f, 19f)
    moveTo(11f, 9f); lineTo(11f, 15f)
    moveTo(17f, 6.5f); lineTo(17f, 17.5f)
}

/** Deux flèches opposées : d'une unité à l'autre. */
internal val IconeConversions = icone("conversions") {
    moveTo(4f, 8.5f); lineTo(19.5f, 8.5f)
    moveTo(16f, 5f); lineTo(19.5f, 8.5f); lineTo(16f, 12f)
    moveTo(20f, 15.5f); lineTo(4.5f, 15.5f)
    moveTo(8f, 12f); lineTo(4.5f, 15.5f); lineTo(8f, 19f)
}

/** Un éclair : l'électricité. */
internal val IconeElectricite = icone("electricite") {
    moveTo(13.5f, 3f); lineTo(6f, 13.5f); lineTo(11.5f, 13.5f); lineTo(10.5f, 21f); lineTo(18f, 10.5f); lineTo(12.5f, 10.5f); close()
}

/** Une sinusoïde : le secteur qui fait battre la lumière. */
internal val IconeScintillement = icone("scintillement") {
    moveTo(3f, 12f)
    quadTo(5.25f, 4f, 7.5f, 12f)
    quadTo(9.75f, 20f, 12f, 12f)
    quadTo(14.25f, 4f, 16.5f, 12f)
    quadTo(18.75f, 20f, 21f, 12f)
}
