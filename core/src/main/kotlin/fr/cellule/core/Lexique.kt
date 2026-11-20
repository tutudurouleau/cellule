package fr.cellule.core

import kotlin.random.Random

/*
 * Les mots du plateau : ce qu'on entend pendant une journée de tournage, et
 * ce que ça veut dire. Le jargon change d'une équipe à l'autre ; on ne garde
 * ici que les sens courants, et chacun peut ajouter les siens (« mes mots »),
 * qui se révisent avec les autres.
 */

enum class RubriqueLexique(val libelle: String) {
    LUMIERE("Lumière et électricité"),
    MACHINERIE("Machinerie"),
    CAMERA("Caméra et image"),
    PLATEAU("Plateau et mise en scène"),
    PRODUCTION("Production et location"),
    PERSO("Mes mots")
}

data class TermePlateau(
    val mot: String,
    val definition: String,
    val rubrique: RubriqueLexique,
    val aussi: List<String> = emptyList(),
    /** L'identifiant d'une fiche du catalogue qui le montre, s'il y en a une. */
    val fiche: String? = null
) {
    val cle: String get() = sansAccents(mot).lowercase()
}

private fun t(mot: String, definition: String, r: RubriqueLexique, vararg aussi: String, fiche: String? = null) =
    TermePlateau(mot, definition, r, aussi.toList(), fiche)

object Lexique {

    private val L = RubriqueLexique.LUMIERE
    private val M = RubriqueLexique.MACHINERIE
    private val C = RubriqueLexique.CAMERA
    private val P = RubriqueLexique.PLATEAU
    private val D = RubriqueLexique.PRODUCTION

    val TERMES: List<TermePlateau> = listOf(
        /* ── Lumière et électricité ── */
        t("Blonde", "Projecteur ouvert tungstène de 2000 W, sans lentille. Le nom vient du modèle Ianiro.", L, "2000 ouvert"),
        t("Mandarine", "Projecteur ouvert tungstène de 800 W, la petite sœur de la blonde.", L, "Redhead"),
        t("Un 2K, un 5K, un 10K", "Fresnels tungstène de 2, 5 ou 10 kW. On dit aussi « un 650 », « un 300 » pour les petits.", L),
        t("Un 18K, un 4K, un 1200", "Projecteurs HMI désignés par leur puissance : 18 kW, 4 kW, 1200 W…", L),
        t("M18, M40, M90", "HMI ARRI à réflecteur ouvert, de 1800, 4000 et 9000 W : beaucoup de lumière pour leur taille.", L),
        t("Joker", "Petit HMI K5600 (200 à 1600 W), très polyvalent : nu, en Fresnel, dans une boîte à lumière.", L, "Joker-Bug"),
        t("Dedo", "Petit projecteur Dedolight à optique de précision, pour les touches et les détails.", L, "Dedolight"),
        t("Kino", "Banque de tubes Kino Flo, fluo puis LED : une lumière douce et plate. « Un 4-bank de 120. »", L, "Kino Flo"),
        t("SkyPanel", "Panneau LED ARRI (S30, S60, S120, S360), réglable en couleur, devenu un standard.", L, "Sky"),
        t("Astera", "Tubes LED sans fil sur batterie (Titan, Helios), pilotables à distance.", L, "tube"),
        t("Mini-brute, maxi-brute", "Batteries de lampes PAR : la maxi-brute en aligne neuf de 1000 W. Pour imiter le soleil de loin.", L, "brute"),
        t("Dino", "Énorme batterie de lampes PAR de 1000 W : du soleil artificiel à longue distance.", L),
        t("Spacelight", "Cylindre de tissu suspendu, avec plusieurs lampes : une ambiance douce venue d'en haut.", L, "space"),
        t("Ballon", "Ballon éclairant gonflé, suspendu ou sur mât : une lumière douce qui part dans toutes les directions.", L, "ballon éclairant"),
        t("Boule chinoise", "Lanterne de papier avec une ampoule : une petite ambiance douce, légère et bon marché.", L, "lanterne chinoise"),
        t("Volets", "Les quatre battants à l'avant d'un projecteur, pour couper ou cadrer le faisceau.", L, "barn doors"),
        t("Snoot", "Tube ou cône à l'avant d'un projecteur, pour un faisceau rond et serré.", L, "cône"),
        t("Gélatine", "Filtre souple posé devant une source : couleur, correction, diffusion. On dit « une gél ».", L, "gél", "gélat"),
        t("CTO", "Gélatine orange qui convertit la lumière du jour vers le tungstène. « Full », « 1/2 », « 1/4 », « 1/8 ».", L, "Color Temperature Orange"),
        t("CTB", "Gélatine bleue qui convertit le tungstène vers la lumière du jour.", L, "Color Temperature Blue"),
        t("Plus green, minus green", "Gélatines qui ajoutent ou retirent du vert, pour accorder une source à des fluos ou à une LED.", L),
        t("216", "Gélatine Lee de diffusion forte (White Diffusion). On appelle les diffusions par leur numéro.", L),
        t("250, 251", "Diffusions Lee moyenne (250, Half White) et légère (251, Quarter White).", L),
        t("129", "Frost lourd Lee (Heavy Frost) : il casse la dureté sans trop étaler la lumière.", L, "heavy frost"),
        t("Opal", "Diffusion légère (Lee Opal Frost) : elle adoucit à peine, sans perdre beaucoup.", L),
        t("ND (gélatine)", "Densité neutre : baisse la lumière sans changer sa couleur. ND 0,3 ≈ 1 diaph, 0,6 ≈ 2, 0,9 ≈ 3.", L, "neutre"),
        t("Grid cloth", "Toile de diffusion tissée (1/4, 1/2, full), pour les cadres et les grandes lumières douces.", L, "grid"),
        t("Soie", "Toile de diffusion fine pour les cadres : elle adoucit le soleil au-dessus des comédiens.", L, "silk"),
        t("Ultrabounce", "Toile blanche d'un côté, noire de l'autre : réflecteur ou négatif selon la face.", L),
        t("Habiller un projecteur", "Lui ajouter ce qu'il faut : gélatine, diffusion, volets, drapeaux, nid d'abeille.", L),
        t("Retour", "Lumière renvoyée par une surface claire (poly, réflecteur) pour déboucher les ombres.", L, "bounce", fiche = "reflecteurs-et-polystyrene"),
        t("Contre", "Lumière placée derrière le sujet, face à la caméra : elle dessine les contours.", L, "contre-jour", "backlight"),
        t("Face", "Lumière principale qui éclaire le visage depuis le côté de la caméra.", L, "key"),
        t("Décrochage", "Lumière qui détache le sujet du fond, souvent un contre ou un trois-quarts arrière.", L),
        t("Douche", "Lumière qui tombe d'en haut, à la verticale du sujet.", L, "top light"),
        t("Effet", "Lumière qui imite une source du décor : fenêtre, lampe, télévision, feu.", L, "effet fenêtre"),
        t("Négatif", "Enlever de la lumière avec du noir (borniol, floppy) pour creuser un côté du visage.", L, "negative fill", fiche = "borniol"),
        t("Découverte", "Grand fond imprimé ou peint qu'on voit par les fenêtres d'un décor de studio.", L, "backing"),
        t("Cyclo", "Fond de studio courbe, sans angle visible entre sol et mur.", L, "cyclorama"),
        t("Flicker", "Scintillement à l'image d'une source mal alimentée ou d'une cadence non adaptée.", L, "scintillement"),
        t("Chef électro", "Responsable de l'équipe électrique : il met en œuvre la lumière voulue par le chef opérateur.", L, "gaffer"),
        t("Best boy", "Adjoint du chef électro ou du chef machino : matériel, commandes, équipe.", L),
        t("Le groupe", "Le groupe électrogène ; « le groupiste » le conduit et le surveille.", L, "groupe électrogène", fiche = "groupe-electrogene"),
        t("Un 16 mono, un 32 tétra", "Prolongs et prises P17 désignés par leur calibre et leurs phases : 16 A en 230 V, 32 A en triphasé.", L, fiche = "p17"),
        t("P17", "La prise industrielle ronde, bleue en 230 V, rouge en 400 V.", L, "CEE", fiche = "p17"),
        t("Triplette", "Répartiteur qui fait trois prises d'une seule, sans augmenter le courant disponible.", L, fiche = "triplette-et-doublette"),
        t("Équilibrer les phases", "Répartir les projecteurs sur les trois phases pour qu'aucune ne soit surchargée.", L),
        t("Ça a sauté", "Un disjoncteur ou un différentiel a coupé : surcharge ou défaut, on cherche avant de réarmer.", L, fiche = "differentiel-30-ma"),
        t("Gaffer (ruban)", "Ruban adhésif toilé, mat, qui s'arrache à la main et se retire sans traces.", L, "gaffer tape"),
        t("Chatterton", "Ruban isolant d'électricien, en vinyle.", L),

        /* ── Machinerie ── */
        t("Machino", "Machiniste : il installe les supports, les travellings, les grues, les cadres et drapeaux.", M, "grip"),
        t("Un C", "Un C-stand, le pied à tout faire du machino.", M, "Century", fiche = "matthews-c-stand"),
        t("Junior, baby", "Les deux standards de fixation : spigot baby de 16 mm, réceptacle junior de 28 mm.", M, fiche = "pied-de-projecteur-baby-et-junior"),
        t("Wind-up", "Pied lourd à manivelle pour lever haut un gros projecteur.", M, "crank-up", fiche = "wind-up"),
        t("Rotule", "La tête à deux trous d'un C-stand, qui serre le bras et le drapeau.", M, "gobo head", fiche = "rotule-de-c-stand"),
        t("Mafer", "Pince à vis avec embout de 16 mm ; souvent employé pour toute pince de ce type.", M, fiche = "matthews-mafer"),
        t("Super clamp", "Pince à cliquet Manfrotto, l'accroche universelle.", M, fiche = "manfrotto-super-clamp"),
        t("Cardellini", "Pince à mâchoires réglables, pour les portes et les planches.", M, "Matthellini", fiche = "cardellini"),
        t("C-47", "Pince à linge en bois, pour tenir les gélatines sur les volets.", M, "pince à linge", fiche = "pinces-a-linge-c-47"),
        t("Apple", "Caisse en bois standard (apple box) ; « une demi », « un quart », « un pancake ».", M, "apple box", fiche = "apple-boxes"),
        t("Gueuse", "Poids en fonte pour lester lourd : grues, pieds de cadres.", M, fiche = "gueuse"),
        t("Un sac", "Sac de lest posé sur un pied. Réflexe : un pied, un sac.", M, "shot bag", fiche = "sac-de-lest"),
        t("Drapeau", "Cadre tendu de noir pour couper la lumière.", M, "flag", fiche = "drapeau"),
        t("Floppy", "Grand drapeau dont un pan se déplie pour doubler sa surface.", M, fiche = "cutter-et-floppy"),
        t("Filet", "Cadre de tulle qui baisse la lumière sans la diffuser : simple ou double.", M, "single", "double", fiche = "filets-simple-et-double"),
        t("Cookie", "Panneau ajouré qui projette des ombres de feuillage.", M, "cucoloris", fiche = "cookie"),
        t("Un 12×12", "Grand cadre de 12 pieds de côté, tendu de soie, de grid ou de noir.", M, "butterfly", fiche = "cadres-et-toiles-butterfly"),
        t("Borniol", "Tissu noir mat pour occulter, cacher, creuser.", M, "duvetyne", fiche = "borniol"),
        t("Poly", "Plaque de polystyrène blanc qui renvoie une lumière douce.", M, "polystyrène", fiche = "reflecteurs-et-polystyrene"),
        t("Poser du rail", "Installer et caler de niveau les rails de travelling.", M, fiche = "rails-de-travelling"),
        t("Pousser", "Déplacer la dolly pendant la prise : le machino « pousse » en suivant ses marques.", M),
        t("Crabe", "Direction d'une dolly qui se déplace en biais sans tourner.", M, fiche = "j-l-fisher-dolly-fisher"),
        t("Colonne", "La partie d'une dolly qui lève la caméra pendant le plan.", M),
        t("Télescoper", "Allonger ou rétracter le bras d'une grue télescopique pendant le plan.", M, fiche = "technocrane"),
        t("Praticable", "Plateforme modulable qui surélève caméra, projecteur ou comédien.", M, "prat"),
        t("Nacelle", "Plateforme élévatrice motorisée pour placer une source ou une caméra en hauteur.", M),
        t("Autopole", "Barre télescopique coincée entre sol et plafond.", M, "polecat", fiche = "manfrotto-autopole"),
        t("Bras magique", "Bras articulé qui se bloque d'un tour de molette.", M, "magic arm", fiche = "manfrotto-bras-magique"),
        t("Caler", "Mettre de niveau ou immobiliser avec des cales.", M, fiche = "cales-et-cales-a-gradins"),

        /* ── Caméra et image ── */
        t("Le point", "La netteté. « Faire le point », « le point est sur elle ».", C),
        t("Pointeur", "Premier assistant opérateur : il fait le point pendant la prise et s'occupe de la caméra.", C, "1er assistant", "focus puller"),
        t("Deuxième assistant", "Il tient le clap, remplit les rapports caméra, gère les batteries et les cartes ou le film.", C, "2e assistant"),
        t("DIT", "Technicien de l'image numérique : réglages, étalonnage de plateau, sauvegarde des rushes.", C, "data manager"),
        t("Clap", "La claquette qui annonce le plan et la prise et donne le synchronisme image-son.", C, "claquette"),
        t("Clap de fin", "Clap donné à la fin de la prise, tenu à l'envers.", C, "clap en queue"),
        t("Clap muet", "Clap d'une prise sans son, branches tenues fermées.", C, "MOS"),
        t("Mag", "Le magasin de film d'une caméra argentique ; « un mag chargé ».", C, "magasin"),
        t("Combo", "Le village vidéo : les moniteurs de la réalisation sur le plateau.", C, "village vidéo", "retour vidéo"),
        t("HF vidéo", "Émetteur sans fil qui envoie l'image de la caméra vers les moniteurs.", C, "Teradek"),
        t("Matte box", "Pare-soleil à volets devant l'objectif, qui porte aussi les filtres.", C, "pare-soleil"),
        t("Follow focus", "Commande de mise au point, à la main ou sans fil (moteurs sur l'objectif).", C, "follow"),
        t("Tiges de 15, de 19", "Les tiges qui portent follow focus et matte box : 15 mm (léger) ou 19 mm (studio).", C, "rods"),
        t("Monture PL, LPL", "Les montures d'objectif du cinéma : PL (Super 35), LPL (grand format ARRI).", C),
        t("Diaph T", "Ouverture mesurée en lumière réellement transmise : le standard des objectifs de cinéma.", C, "T-stop"),
        t("Une série", "Un jeu d'objectifs fixes d'une même gamme : « une série de Cooke S4 ».", C),
        t("Anamorphique", "Objectif qui comprime l'image en largeur pour l'écran large, avec des flares et un bokeh ovale.", C, "scope"),
        t("Grandes branches, baby, high hat", "Les trois hauteurs de pieds de caméra, du plus haut au ras du sol.", C, fiche = "trepied-camera"),
        t("Amorce", "Élément au premier plan, flou ou en partie dans le cadre : « en amorce ».", C),
        t("Contrechamp", "Le plan pris dans la direction opposée : champ-contrechamp pour un dialogue.", C),
        t("Passer l'axe", "Franchir la ligne imaginaire entre deux personnages : leurs regards ne raccordent plus.", C, "règle des 180°"),
        t("Plan débullé", "Cadre volontairement penché, la bulle de niveau décalée.", C, "dutch angle"),
        t("Pano", "Panoramique : la caméra pivote sur place, horizontalement ou verticalement.", C, "panoramique"),
        t("Plongée, contre-plongée", "Caméra au-dessus du sujet qui regarde vers le bas, ou en dessous qui regarde vers le haut.", C),
        t("Insert", "Gros plan d'un détail : une main, un objet, un écran.", C),
        t("Plan-séquence", "Une scène entière en un seul plan, sans coupe.", C),
        t("Brûlé, bouché", "Surexposé sans détail dans les blancs ; sous-exposé sans détail dans les noirs.", C),
        t("Rushes", "Les images tournées du jour, avant montage.", C, "dailies"),

        /* ── Plateau et mise en scène ── */
        t("Moteur demandé", "Annonce du premier assistant réalisateur : on lance son et caméra. Réponses : « Ça tourne ! »", P),
        t("Action, coupez", "Le début et la fin de la prise, donnés par le réalisateur.", P),
        t("PAT", "Prêt à tourner : l'heure à laquelle tout doit être en place, sur la feuille de service.", P, "prêt à tourner"),
        t("Mise en place", "On règle la scène avec les comédiens ou leurs doublures avant de tourner.", P),
        t("Doublure lumière", "Personne qui remplace un comédien pendant les réglages de lumière et de cadre.", P, "stand-in"),
        t("Silence plateau", "Plus un bruit : on va tourner.", P),
        t("Ambiance", "Trente secondes de silence enregistrées pour le son du lieu, sans dialogue.", P, "room tone"),
        t("C'est dans la boîte", "La prise est bonne, le plan est terminé.", P),
        t("La der", "Le dernier plan de la journée ou d'un décor.", P),
        t("Une de sécu", "Une prise de plus pour être sûr, alors qu'on a déjà la bonne.", P, "sécurité"),
        t("La bonne", "La prise retenue, cerclée sur le rapport de la scripte.", P, "prise cerclée"),
        t("Raccord", "Continuité d'un plan à l'autre : position, accessoires, lumière. « Faux raccord » quand ça ne suit pas.", P),
        t("Scripte", "Garante de la continuité : elle note chaque prise et ce qui doit raccorder.", P),
        t("Premier assistant réalisateur", "Il organise le plateau, tient le temps et lance les prises.", P, "le premier", "1er AR"),
        t("Feuille de service", "Le programme du jour envoyé la veille : horaires, décors, scènes, équipes.", P, "call sheet"),
        t("Plan de travail", "L'ordre de tournage de toutes les scènes, jour par jour.", P),
        t("Dépouillement", "La liste de ce que chaque scène demande : décors, rôles, accessoires, matériel.", P),
        t("Repérages", "Visites des décors avant le tournage, pour préparer image, lumière et logistique.", P, "repérage"),
        t("Décor naturel", "Un lieu réel, par opposition au décor construit en studio.", P),
        t("HMC", "Habillage, maquillage, coiffure : l'équipe et la loge où passent les comédiens.", P, "loge"),
        t("Régie", "L'équipe logistique : accès, stationnement, cantine, autorisations.", P, "régisseur"),
        t("Silhouette", "Figurant visible, avec parfois une action précise mais sans texte.", P),

        /* ── Production et location ── */
        t("Devis", "Le prix proposé par le loueur pour une liste et une durée ; on part de lui pour négocier.", D),
        t("Bon de commande", "La commande ferme qui réserve le matériel chez le loueur.", D),
        t("Enlèvement, retour", "La prise du matériel chez le loueur et son retour ; on note la date et l'heure de chacun.", D),
        t("Faire le check", "Tester tout le matériel chez le loueur avant de partir : allumer, monter, compter.", D, "check"),
        t("Caution", "Somme ou garantie demandée par le loueur, rendue au retour du matériel en bon état.", D),
        t("Assurance matériel", "L'assurance qui couvre la casse et le vol du matériel loué.", D),
        t("Jour, semaine de location", "Les loueurs facturent à la journée ; la semaine compte souvent moins de jours facturés que de jours réels.", D)
    )

    /** Les termes, plus ceux de la personne. */
    fun tous(perso: List<TermePlateau>): List<TermePlateau> = TERMES + perso

    fun chercher(texte: String, termes: List<TermePlateau>): List<TermePlateau> {
        val mots = sansAccents(texte).lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (mots.isEmpty()) return termes
        return termes.filter { t ->
            val corps = sansAccents(t.mot + " " + t.aussi.joinToString(" ") + " " + t.definition).lowercase()
            mots.all { it in corps }
        }.sortedBy { if (mots.all { m -> m in sansAccents(it.mot).lowercase() }) 0 else 1 }
    }

    /**
     * Une question : la définition, et le bon mot parmi trois autres de la
     * même rubrique quand c'est possible.
     */
    fun question(termes: List<TermePlateau>, alea: Random, eviter: String? = null): QuestionLexique? {
        if (termes.size < 4) return null
        val t = termes.filter { it.cle != eviter }.random(alea)
        val memes = termes.filter { it.rubrique == t.rubrique && it.cle != t.cle }.shuffled(alea)
        val autres = termes.filter { it.rubrique != t.rubrique }.shuffled(alea)
        val leurres = (memes + autres).map { it.mot }.distinct().filter { it != t.mot }.take(3)
        if (leurres.size < 3) return null
        val choix = (leurres + t.mot).shuffled(alea)
        return QuestionLexique(t, choix, choix.indexOf(t.mot))
    }
}

data class QuestionLexique(val terme: TermePlateau, val choix: List<String>, val bonne: Int)

/** Les mots ajoutés par la personne, en texte simple : une ligne par mot, « mot\tdéfinition ». */
object LexiquePerso {
    fun lire(texte: String): List<TermePlateau> = texte.lines().mapNotNull { l ->
        val morceaux = l.split('\t')
        val mot = morceaux.getOrNull(0)?.trim().orEmpty()
        val def = morceaux.getOrNull(1)?.trim().orEmpty()
        if (mot.isBlank()) null else TermePlateau(mot, def.replace("\\n", "\n"), RubriqueLexique.PERSO)
    }

    fun ecrire(termes: List<TermePlateau>): String = termes.joinToString("\n") {
        it.mot.replace('\t', ' ').replace('\n', ' ') + "\t" + it.definition.replace('\t', ' ').replace("\n", "\\n")
    }
}
