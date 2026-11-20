package fr.cellule.core

/*
 * La machinerie et l'électrique : ce qu'on demande au loueur à côté des
 * caméras et des projecteurs, et ce qu'on entend nommer sur un plateau.
 *
 * Une fiche dit à quoi sert l'objet, comment on l'appelle sur le plateau, et
 * ses repères chiffrés quand ce sont des standards du métier (diamètre des
 * spigots, tailles des apple boxes, calibres des prises). Les charges et
 * poids d'un modèle précis viennent de son fabricant, ou ne figurent pas.
 */

data class FicheMachinerie(
    override val nom: String,
    override val fabricant: String,
    override val genre: GenreMateriel,
    override val categorie: String,
    override val silhouette: Silhouette,
    override val punchline: String,
    override val chiffresCles: List<Paire>,
    /** Comment on le nomme sur un plateau, et ce qu'on y entend. */
    val surLePlateau: String,
    override val commentLUtiliser: String,
    override val atouts: String = "",
    override val limites: String = "",
    override val aRetenir: String = "",
    override val histoire: String = "",
    override val ficheTechnique: String = "",
    /** Les autres noms qu'on lui donne : « Century », « pied C ». */
    val aussi: List<String> = emptyList(),
    override val annee: Int = 0,
    override val anneeApprox: Boolean = false
) : Materiel {
    override val films: List<ReferenceFilm> get() = emptyList()
}

private fun machino(
    nom: String, fabricant: String, etagere: String, silhouette: Silhouette, punchline: String,
    chiffres: List<Paire>, plateau: String, usage: String, retenir: String = "",
    aussi: List<String> = emptyList(), atouts: String = "", limites: String = "", histoire: String = "",
    annee: Int = 0, approx: Boolean = false
) = FicheMachinerie(
    nom, fabricant, GenreMateriel.MACHINERIE, etagere, silhouette, punchline, chiffres, plateau, usage,
    atouts, limites, retenir, histoire, aussi = aussi, annee = annee, anneeApprox = approx
)

private fun elec(
    nom: String, fabricant: String, etagere: String, silhouette: Silhouette, punchline: String,
    chiffres: List<Paire>, plateau: String, usage: String, retenir: String = "",
    aussi: List<String> = emptyList(), atouts: String = "", limites: String = ""
) = FicheMachinerie(
    nom, fabricant, GenreMateriel.ELECTRIQUE, etagere, silhouette, punchline, chiffres, plateau, usage,
    atouts, limites, retenir, aussi = aussi
)

private const val PIEDS = "Les pieds"
private const val CLAMPS = "Clamps et pinces"
private const val BARRES = "Barres, bras et adaptateurs"
private const val LESTS = "Lests et calage"
private const val CADRES = "Drapeaux, cadres et toiles"
private const val SUPPORTS = "Supports caméra"
private const val TRAVELLING = "Travelling"
private const val GRUES = "Grues, bras et véhicules"

private const val PRISES = "Prises et connecteurs"
private const val CABLES = "Câbles et prolongs"
private const val DISTRIB = "Distribution et énergie"
private const val CONTROLE = "Contrôle"

private val BABY = "Spigot baby" vaut "16 mm (5/8 de pouce)"
private val JUNIOR = "Réceptacle junior" vaut "28 mm (1 1/8 de pouce)"

val ETAGERES_MACHINERIE: List<Etagere<FicheMachinerie>> = listOf(
    Etagere(
        PIEDS, listOf(
            machino(
                "C-stand", "Matthews", PIEDS, Silhouette.MACHINO_PIED,
                "Le pied à tout faire du machino : drapeaux, petits projecteurs, toiles.",
                listOf(
                    "Pieds" vaut "3, à hauteurs décalées pour s'imbriquer", "Tête" vaut "rotule à deux trous de 16 mm", BABY,
                    "Century 40″ chez TSF" vaut "321 cm, 10 kg admissibles, pèse 7,2 kg"
                ),
                "« Un C », « un Century ». Livré avec sa rotule et son bras (gobo arm) : « mets-moi un drapeau sur un C ».",
                "Ses trois pieds étagés lui permettent de se serrer contre un mur ou un autre pied. On le leste toujours d'un sac, posé sur le pied le plus haut, et on serre la rotule dans le sens où la charge la ferme (le poids à droite quand on regarde la rotule).",
                "Le sac de lest va sur le pied le plus haut, côté charge. Et la règle de la rotule : « righty tighty », le poids doit tendre à la serrer.",
                aussi = listOf("Century stand", "pied C", "pied Century"),
                atouts = "Robuste, imbriquable, accepte tout ce qui a un spigot de 16 mm.",
                limites = "Pas fait pour les projecteurs lourds : au-delà de quelques kilos, on passe à un pied junior."
            ),
            machino(
                "Pied de projecteur (baby et junior)", "", PIEDS, Silhouette.MACHINO_PIED,
                "Le pied télescopique classique : un spigot de 16 mm (baby) ou un réceptacle de 28 mm (junior).",
                listOf(
                    BABY, JUNIOR, "Sections" vaut "2 à 4, serrées par des molettes",
                    "Pied de 1000 acier (TSF)" vaut "288 cm, 10 kg admissibles",
                    "Baby 2 kW (TSF)" vaut "100 cm, 30 kg admissibles"
                ),
                "« Un pied baby », « un pied junior ». On demande au machino « un junior » pour un 2K ou un 5K.",
                "Le baby porte les petites têtes (spigot mâle de 16 mm), le junior les projecteurs lourds (leur tenon de 28 mm entre dans le pied). On le leste, on monte d'abord la section la plus large, et on ne le déploie jamais à fond sous une charge lourde.",
                "Baby = 16 mm, junior = 28 mm : ce sont les deux standards de tout le matériel de tournage.",
                aussi = listOf("pied lumière", "pied baby", "pied junior")
            ),
            machino(
                "Wind-up", "", PIEDS, Silhouette.MACHINO_PIED,
                "Le pied lourd à manivelle, pour lever haut un gros projecteur.",
                listOf(
                    JUNIOR, "Levage" vaut "crémaillère et manivelle",
                    "Baby crank (TSF)" vaut "175 cm, 100 kg admissibles, pèse 30,7 kg",
                    "Long John (TSF)" vaut "340 cm, 120 kg admissibles, pèse 70 kg"
                ),
                "« Un wind-up » (aussi « crank-up ») : on monte la tête en tournant la manivelle, sans la porter à bout de bras.",
                "Pour les HMI de 4 kW et plus, les 5K, 10K, les grosses LED ou une petite toile : la manivelle soulève la charge en douceur. On le leste largement et on le place avant de monter la tête.",
                "Un wind-up se monte vide, se leste, puis reçoit sa tête : jamais l'inverse.",
                aussi = listOf("crank-up", "pied à manivelle", "pied à crémaillère")
            ),
            machino(
                "Pied U126", "", PIEDS, Silhouette.MACHINO_PIED,
                "Le pied de projecteur à jambe de nivellement, partout sur les plateaux français.",
                listOf(
                    "U126, 2 tiges (TSF)" vaut "330 cm, 40 kg admissibles, pèse 8,3 kg",
                    "U126 à roulettes (TSF)" vaut "315 cm, 12 kg admissibles",
                    "Particularité" vaut "une jambe réglable pour rester d'aplomb sur un sol en pente"
                ),
                "« Un U », « un U126 » : « passe-moi un U » veut dire ce pied-là.",
                "Sa jambe de nivellement permet de le poser droit dans un escalier, sur un trottoir ou un terrain en pente. Il porte des projecteurs moyens et se leste comme tout pied.",
                "« Un U » = un pied U126. Le chiffre est le nom du modèle, repris par tous les loueurs.",
                aussi = listOf("U", "pied U", "U126")
            ),
            machino(
                "Low-boy et embase tortue", "", PIEDS, Silhouette.MACHINO_PIED,
                "Mettre un projecteur au ras du sol.",
                listOf(JUNIOR, "Hauteur" vaut "quelques dizaines de centimètres"),
                "« Un low-boy », « une tortue » (turtle base) : pour une contre-plongée de lumière, éclairer par le bas, cacher une source derrière un meuble.",
                "La tortue est une embase à trois pattes qui se démonte du pied junior : le projecteur se pose presque au sol. Le low-boy est un pied très court.",
                aussi = listOf("turtle base", "embase basse", "pied bas")
            ),
            machino(
                "Pied à roulettes", "", PIEDS, Silhouette.MACHINO_PIED,
                "Un pied qu'on déplace sans le soulever, en studio surtout.",
                listOf("Roues" vaut "freinées", JUNIOR),
                "« Un roller » : on pousse la source pendant les réglages, puis on freine.",
                "Très pratique en studio sur sol lisse. En extérieur ou sur un sol irrégulier, on revient au pied à patins, plus stable.",
                aussi = listOf("roller stand", "pied roulant")
            ),
            machino(
                "Combo stand", "", PIEDS, Silhouette.MACHINO_PIED,
                "Le pied lourd polyvalent : un réceptacle junior et, en tête, un spigot baby.",
                listOf(JUNIOR, BABY, "Combo 45 acier (TSF)" vaut "450 cm, 25 kg admissibles, pèse 4,9 kg"),
                "« Un combo » : il prend une tête junior, ou un cadre et un drapeau grâce à son spigot.",
                "Plus lourd qu'un C-stand, il porte cadres 4×4, projecteurs moyens et toiles par vent faible."
            )
        )
    ),
    Etagere(
        CLAMPS, listOf(
            machino(
                "Super Clamp", "Manfrotto", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince à cliquet qui s'accroche partout : tube, planche, rebord.",
                listOf("Serrage" vaut "levier et vis à cliquet", "Embouts" vaut "spigot de 16 mm, rotules, bras magique"),
                "« Un super clamp ». Avec un embout spigot, il devient un point d'accroche pour une petite lumière.",
                "On le serre sur un tube rond ou carré, une planche, un pied de table, puis on y visse un spigot ou un bras magique. Il va par paire avec le bras magique.",
                "Super clamp + spigot 16 mm = un petit projecteur accroché n'importe où.",
                aussi = listOf("Manfrotto 035", "super pince")
            ),
            machino(
                "Mafer", "Matthews", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince à vis historique, avec son embout de 16 mm amovible.",
                listOf("Serrage" vaut "vis et mâchoire", "Embout" vaut "spigot de 16 mm amovible"),
                "« Un Mafer » : sur les plateaux français, le mot désigne souvent toute pince de ce type.",
                "Même usage que le super clamp : on l'accroche sur un tube ou un rebord et on y fixe une petite tête ou un drapeau.",
                aussi = listOf("Mafer clamp")
            ),
            machino(
                "Cardellini", "Cardellini", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince à mâchoires réglables qui saisit une porte, une planche, un tube carré.",
                listOf("Versions" vaut "End Jaw (mâchoire en bout) et Center Jaw (au centre)", "Sortie" vaut "spigot de 16 mm"),
                "« Un Cardellini », ou « un Matthellini » pour la version Matthews.",
                "Très prisée pour accrocher sur le chant d'une porte, une poutre, un garde-corps, avec une ouverture bien plus grande qu'un super clamp.",
                aussi = listOf("Matthellini", "clamp Cardellini")
            ),
            machino(
                "Gaffer grip", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La grosse pince à ressort qui tient une petite lumière sur un bord de porte.",
                listOf("Serrage" vaut "ressort puissant", "Sortie" vaut "un ou deux spigots de 16 mm"),
                "« Un gaffer grip » : la main de l'électro pour un accrochage rapide.",
                "On la pince sur une porte, une étagère, un tuyau, et on y pose une petite source ou un drapeau léger.",
                limites = "Elle tient par le ressort : pas de charge lourde, pas de surface glissante.",
                aussi = listOf("gator grip", "pince gaffer")
            ),
            machino(
                "Pinces à ressort (A-clamps)", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "Les pinces du quotidien : tenir une toile, un borniol, une gélatine.",
                listOf("Tailles" vaut "n° 1 (petite), n° 2, n° 3 (grande)"),
                "« Une pince », « une A-clamp » ; on précise la taille : « passe-moi une 2 ».",
                "Pour pincer un tissu sur un cadre, fermer un rideau, tenir un borniol sur un pied.",
                aussi = listOf("A-clamp", "spring clamp", "pince Bulldog")
            ),
            machino(
                "Pinces à linge (C-47)", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince en bois qui tient les gélatines sur les volets.",
                listOf("Matière" vaut "bois : ne fond pas au contact d'un projecteur chaud"),
                "« Une C-47 », « une pince à linge » : l'outil le plus demandé du plateau.",
                "On pince la gélatine ou la diffusion sur les volets d'un projecteur. En bois, parce que le plastique fond.",
                aussi = listOf("C-47", "bâtonnet")
            ),
            machino(
                "Serre-joint", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince de menuisier, détournée pour fixer planches et cadres.",
                listOf("Ouverture" vaut "selon la longueur de la barre"),
                "« Un serre-joint » : très français, on l'emploie pour tout ce qui doit tenir serré longtemps.",
                "Fixer une planche sur une table, un cadre sur une rambarde, une barre sur un meuble."
            ),
            machino(
                "Bec de canard", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince à mâchoires larges et plates, pour le polystyrène et les toiles.",
                listOf("Mâchoires" vaut "larges et plates", "Sortie" vaut "spigot de 16 mm"),
                "« Un bec de canard » (platypus) : il tient une plaque de polystyrène sans la casser.",
                "Monté sur un C-stand, il présente une plaque de polystyrène (bounce) ou un frost.",
                aussi = listOf("platypus", "duckbill")
            ),
            machino(
                "Big Ben", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "La pince pour les gros tubes, poteaux et branches.",
                listOf("Sortie" vaut "spigot baby ou réceptacle junior"),
                "« Un Big Ben » : pour s'accrocher à un poteau, un tronc, un gros tube de structure.",
                "Elle se serre autour d'un tube de gros diamètre et reçoit un projecteur de taille moyenne."
            ),
            machino(
                "Collier d'échafaudage", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "Le collier qui assemble les tubes d'échafaudage et y accroche les projecteurs.",
                listOf("Tube" vaut "48 mm, l'échafaudage standard", "Sorties" vaut "spigot baby ou junior selon le modèle"),
                "« Un collier », « un coupleur » : sur les structures, les perches, les speed rails.",
                "Il serre un tube de 48 mm ; les versions à spigot portent directement un projecteur sur une structure.",
                aussi = listOf("coupleur", "scaff clamp")
            ),
            machino(
                "Plaques baby et junior", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "Une plaque vissée à un mur ou un décor, avec un spigot ou un réceptacle.",
                listOf(BABY, JUNIOR),
                "« Une plaque baby » (wall plate) : vissée dans le décor, elle porte une petite source ou un drapeau.",
                "On la visse sur une planche, un panneau de décor, une poutre. Variante : la plaque junior pour une tête lourde.",
                aussi = listOf("wall plate", "baby plate")
            ),
            machino(
                "Ventouse", "", CLAMPS, Silhouette.MACHINO_CLAMP,
                "S'accrocher sur une vitre, une carrosserie, un carrelage.",
                listOf("Surface" vaut "lisse et propre"),
                "« Une ventouse » (suction cup) : sur les voitures et les vitres.",
                "Elle porte une petite source ou une caméra légère, toujours doublée d'une sécurité (sangle ou câble)."
            )
        )
    ),
    Etagere(
        BARRES, listOf(
            machino(
                "Rotule de C-stand", "", BARRES, Silhouette.MACHINO_BARRE,
                "La tête à deux trous qui serre bras, drapeaux et spigots.",
                listOf("Trous" vaut "deux, de 16 mm"),
                "« La rotule », « la gobo head » (grip head en anglais).",
                "Elle tient le bras de rallonge et l'oriente. Toujours placer la charge de façon qu'elle tende à resserrer la molette.",
                aussi = listOf("grip head", "gobo head", "tête de C")
            ),
            machino(
                "Bras de rallonge", "", BARRES, Silhouette.MACHINO_BARRE,
                "La tige qui déporte un drapeau ou une petite lumière au bout d'un C-stand.",
                listOf("Longueurs" vaut "environ 1 m (40 pouces) ou 50 cm (20 pouces)"),
                "« Le bras », « un gobo arm », « une rallonge ».",
                "Il prolonge le C-stand d'une rotule à l'autre pour placer un drapeau exactement où il faut.",
                aussi = listOf("gobo arm", "extension arm")
            ),
            machino(
                "Bras magique", "Manfrotto", BARRES, Silhouette.MACHINO_BARRE,
                "Le bras articulé qui se bloque d'un tour de molette.",
                listOf("Articulations" vaut "deux rotules et un coude, bloqués ensemble"),
                "« Un bras magique » (magic arm), presque toujours avec deux super clamps.",
                "Tenir un moniteur, une petite source, un petit drapeau, un réflecteur, dans n'importe quelle position.",
                aussi = listOf("magic arm", "Manfrotto 143")
            ),
            machino(
                "Autopole", "Manfrotto", BARRES, Silhouette.MACHINO_BARRE,
                "La barre télescopique qui se coince entre sol et plafond, ou entre deux murs.",
                listOf("Tenue" vaut "par pression d'un ressort et d'un verrou"),
                "« Un autopole » ; « un polecat » pour la version Matthews.",
                "En décor naturel, il crée un point d'accroche là où il n'y en a pas : on y serre super clamps et petites têtes.",
                aussi = listOf("polecat", "barre télescopique")
            ),
            machino(
                "Écarteur mural", "", BARRES, Silhouette.MACHINO_BARRE,
                "Deux embouts qui transforment une planche en barre coincée entre deux murs.",
                listOf("Planche" vaut "bois de section courante, coupé à la largeur"),
                "« Un wall spreader », « des écarteurs ».",
                "On coupe une planche à la largeur du couloir, on met un écarteur à chaque bout, et la planche devient un point d'accroche au plafond.",
                aussi = listOf("wall spreader")
            ),
            machino(
                "Menace", "", BARRES, Silhouette.MACHINO_BARRE,
                "Le bras lourd sur pied qui suspend une source au-dessus du décor.",
                listOf("Équilibre" vaut "contrepoids à l'arrière"),
                "« Un bras menace » (menace arm) : pour une douche de lumière au-dessus d'une table.",
                "Il déporte un projecteur loin au-dessus de la scène sans pied dans le champ, contrebalancé par un poids.",
                aussi = listOf("menace arm", "bras de déport")
            ),
            machino(
                "Adaptateurs de spigot", "", BARRES, Silhouette.MACHINO_BARRE,
                "Passer d'un standard à l'autre : 16 mm, 28 mm, filetages photo.",
                listOf(BABY, JUNIOR, "Photo" vaut "filetages 1/4 et 3/8 de pouce"),
                "« Un spigot », « un réducteur junior-baby », « un double spigot ».",
                "Ils permettent de monter une tête baby sur un pied junior, de relier deux rotules, de passer au filetage photo."
            ),
            machino(
                "Speed rail et tubes", "", BARRES, Silhouette.MACHINO_BARRE,
                "Les tubes qu'on assemble en structure pour accrocher la lumière.",
                listOf("Diamètre courant" vaut "48 mm (échafaudage)"),
                "« Du tube », « du speed rail » ; on assemble avec des colliers et des raccords.",
                "On construit un cadre au plafond, une perche, une herse provisoire, sur lesquels se serrent les projecteurs."
            )
        )
    ),
    Etagere(
        LESTS, listOf(
            machino(
                "Apple boxes", "", LESTS, Silhouette.MACHINO_LEST,
                "Les caisses en bois qui calent, surélèvent, assoient : le couteau suisse du plateau.",
                listOf(
                    "Pleine" vaut "51 × 30 × 20 cm (20 × 12 × 8 pouces)",
                    "Demi" vaut "10 cm d'épaisseur",
                    "Quart" vaut "5 cm",
                    "Pancake" vaut "2,5 cm"
                ),
                "« Une apple », « une demi », « un quart », « un pancake ». On précise le sens : « à plat », « sur la tranche », « debout ».",
                "Elles relèvent un comédien, calent une caméra, servent de siège, de marche. Les quatre tailles s'empilent pour atteindre la hauteur voulue.",
                "Pleine 8 pouces, demi 4, quart 2, pancake 1 : chaque taille fait la moitié de la précédente.",
                aussi = listOf("apple box", "pomme")
            ),
            machino(
                "Sac de lest", "", LESTS, Silhouette.MACHINO_LEST,
                "Le poids qu'on pose sur chaque pied : la sécurité de base.",
                listOf("Remplissage" vaut "sable ou grenaille", "Forme" vaut "deux poches pour chevaucher le pied"),
                "« Un sac », « un shot bag ». Le réflexe : « un pied, un sac ».",
                "On le pose à cheval sur le pied le plus haut d'un C-stand ou sur l'embase d'un pied de projecteur.",
                "Tout pied qui porte quelque chose se leste.",
                aussi = listOf("shot bag", "sandbag", "sac de sable")
            ),
            machino(
                "Gueuse", "", LESTS, Silhouette.MACHINO_LEST,
                "Le poids en fonte, pour lester lourd : grues, pieds de cadres, structures.",
                listOf("Matière" vaut "fonte, souvent en galettes empilables"),
                "« Une gueuse » : on en charge les grues, les pieds de cadres, les bases de structures.",
                "Plus dense qu'un sac, elle sert aux contrepoids et au lestage des grosses charges."
            ),
            machino(
                "Cales et cales à gradins", "", LESTS, Silhouette.MACHINO_LEST,
                "Mettre de niveau rails, pieds, dolly.",
                listOf("Formes" vaut "biseautées (wedges) ou en escalier (step blocks)"),
                "« Des cales », « des wedges », « des step blocks ».",
                "Sous les rails de travelling pour les mettre de niveau, sous une roue, sous un pied sur un sol en pente."
            ),
            machino(
                "Cup blocks", "", LESTS, Silhouette.MACHINO_LEST,
                "Les blocs creusés qui empêchent un pied à roulettes de bouger.",
                listOf("Forme" vaut "bloc avec une cuvette pour la roue"),
                "« Des cup blocks », « des coupelles ».",
                "On les glisse sous les roues d'un pied ou d'un praticable pour qu'il ne roule plus."
            )
        )
    ),
    Etagere(
        CADRES, listOf(
            machino(
                "Drapeau", "", CADRES, Silhouette.MACHINO_CADRE,
                "Le cadre tendu de tissu noir qui coupe la lumière.",
                listOf("Tailles courantes" vaut "45 × 60 cm et 60 × 90 cm (18×24 et 24×36 pouces)", "Monture" vaut "tige de 16 mm"),
                "« Un drapeau », « un flag », « un noir ». « Coupe-moi ça avec un drapeau. »",
                "Il protège l'objectif d'un flare, coupe une partie du faisceau, crée une ombre. Plus il est loin de la source, plus son ombre est nette.",
                "Près du projecteur : coupe douce. Près du sujet : coupe nette.",
                aussi = listOf("flag", "noir")
            ),
            machino(
                "Cutter et floppy", "", CADRES, Silhouette.MACHINO_CADRE,
                "Le drapeau long et étroit, et le grand drapeau dont un pan se déplie.",
                listOf("Floppy" vaut "un cadre carré et un pan rabattable"),
                "« Un cutter » pour une coupe en longueur ; « un floppy » quand il faut beaucoup de noir d'un coup.",
                "Le cutter coupe une bande de lumière sur un mur. Le floppy, déplié, double sa surface pour faire du négatif.",
                aussi = listOf("cutter", "floppy")
            ),
            machino(
                "Filets simple et double", "", CADRES, Silhouette.MACHINO_CADRE,
                "Les cadres de tulle qui baissent la lumière sans la diffuser.",
                listOf("Simple" vaut "environ ½ diaph", "Double" vaut "environ 1 diaph"),
                "« Un filet », « un single », « un double », et « un demi-filet » ouvert d'un côté.",
                "On les place dans le faisceau pour baisser une zone (le haut d'un mur, un premier plan) sans changer la qualité de la lumière.",
                aussi = listOf("scrim", "net", "single", "double")
            ),
            machino(
                "Cookie", "", CADRES, Silhouette.MACHINO_CADRE,
                "Le panneau découpé qui projette des ombres de feuillage ou de fenêtre.",
                listOf("Matière" vaut "contreplaqué ou plastique ajouré"),
                "« Un cookie », « un cucoloris ».",
                "Placé dans le faisceau, il casse une lumière trop propre avec des taches d'ombre.",
                aussi = listOf("cucoloris", "cuke")
            ),
            machino(
                "Cadres et toiles (butterfly)", "", CADRES, Silhouette.MACHINO_CADRE,
                "Les grands cadres tendus de diffusion, de réflecteur ou de noir.",
                listOf(
                    "Tailles courantes" vaut "4×4, 6×6, 8×8, 12×12, 20×20 (en pieds)",
                    "Toiles" vaut "soie, grid cloth, ultrabounce, borniol"
                ),
                "« Un 12×12 de grid », « un 6×6 de soie », « un butterfly ». On dit la taille puis la toile.",
                "Au-dessus des comédiens au soleil (pour adoucir), devant une source pour une grande lumière douce, ou en noir pour creuser.",
                "Un 12×12 prend le vent comme une voile : lestage lourd, et une équipe pour le tenir.",
                aussi = listOf("butterfly", "overhead")
            ),
            machino(
                "Borniol", "", CADRES, Silhouette.MACHINO_CADRE,
                "Le tissu noir mat qui absorbe la lumière : cacher, creuser, occulter.",
                listOf("Matière" vaut "molleton noir mat"),
                "« Du borniol » : pour occulter une fenêtre, cacher du matériel, faire du négatif sur un visage.",
                "On le tend sur un cadre, on l'accroche aux pinces : il supprime les réflexions parasites et assombrit un côté.",
                aussi = listOf("duvetyne", "molleton")
            ),
            machino(
                "Nid d'abeille", "", CADRES, Silhouette.MACHINO_CADRE,
                "La grille qui resserre une lumière douce sans la durcir.",
                listOf("Ouverture" vaut "en degrés : 40°, 30°…"),
                "« Un nid d'abeille », « un grid », « un egg crate » pour les softbox.",
                "Fixé devant une boîte à lumière ou un panneau LED, il limite le débordement de la lumière sur le décor.",
                aussi = listOf("honeycomb", "egg crate", "grid")
            ),
            machino(
                "Réflecteurs et polystyrène", "", CADRES, Silhouette.MACHINO_CADRE,
                "Renvoyer la lumière : plaque de polystyrène, réflecteur pliable, cadre argent.",
                listOf("Faces" vaut "blanc, argent, or, noir"),
                "« Un poly », « une plaque », « un réflecteur » ; « fais-moi un retour ».",
                "Le blanc adoucit les ombres, l'argent renvoie plus fort, le noir creuse (négatif).",
                aussi = listOf("poly", "bounce", "réflo")
            )
        )
    ),
    Etagere(
        SUPPORTS, listOf(
            machino(
                "Trépied caméra", "", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Les « pieds » de la caméra : standard, baby, high hat.",
                listOf("Bols" vaut "75, 100 ou 150 mm", "Plateau lourd" vaut "embase Mitchell"),
                "« Les grandes branches », « les baby », « le high hat » (au ras du sol).",
                "On choisit la hauteur (standard, baby, high hat) et le bol qui correspond à la tête. Les araignées (spreaders) empêchent les branches de glisser.",
                "La tête et les pieds doivent avoir le même bol : 100 mm avec 100 mm, 150 avec 150, ou une embase Mitchell.",
                aussi = listOf("pieds", "sticks", "baby legs", "high hat")
            ),
            machino(
                "Tête fluide", "Sachtler", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "La tête panoramique à friction hydraulique : des mouvements doux à la main.",
                listOf("Réglages" vaut "friction et contrepoids, en paliers"),
                "« La tête fluide » ; on règle « la friction » et « le contrepoids » selon la caméra.",
                "Le contrepoids équilibre la caméra quand on l'incline ; la friction donne la résistance du panoramique. Sachtler, O'Connor et Cartoni sont les références.",
                aussi = listOf("fluid head")
            ),
            machino(
                "Tête à manivelles", "ARRI", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Deux manivelles, une pour le panoramique, une pour l'inclinaison : la précision du cinéma.",
                listOf("Commande" vaut "manivelles, avec plusieurs rapports de vitesse"),
                "« La tête à manivelles » (geared head) : Arrihead, Panahead, Oconnor… Le cadreur « tourne les manivelles ».",
                "Elle donne des mouvements d'une régularité parfaite, reproductibles prise après prise. Il faut l'apprendre comme un instrument.",
                aussi = listOf("geared head", "Arrihead", "Panahead")
            ),
            machino(
                "Demi-boule et embase de niveau", "", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Mettre la tête de niveau sans retoucher les branches.",
                listOf("Bols" vaut "75, 100, 150 mm"),
                "« La demi-boule », « mettre de niveau à la bulle ».",
                "La demi-boule se règle dans le bol du trépied, une bulle indique l'horizontale. Pour une tête lourde sur embase Mitchell, on ajoute une embase de niveau."
            ),
            machino(
                "Steadicam", "Tiffen", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Le harnais à bras articulé qui fait flotter la caméra au bout d'un opérateur.",
                listOf("Éléments" vaut "gilet, bras à ressorts, poteau (sled) équilibré"),
                "« Le Steadicam », « le steadi », et son opérateur « le steadicamer ».",
                "Le bras absorbe les pas, le poteau équilibré garde l'horizon : on suit un comédien dans les escaliers, à travers les portes.",
                histoire = "Inventé par Garrett Brown dans les années 1970, il a changé le mouvement de caméra au cinéma.",
                annee = 1975, approx = true
            ),
            machino(
                "Stabilisateur motorisé (gimbal)", "DJI", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "La nacelle à moteurs qui stabilise la caméra sur trois axes.",
                listOf("Axes" vaut "panoramique, inclinaison, roulis"),
                "« Le gimbal », « le Ronin », « le MōVI ».",
                "Porté à la main, sur un Easyrig, une voiture ou un drone, il garde l'horizon et lisse les mouvements.",
                aussi = listOf("Ronin", "MōVI", "nacelle")
            ),
            machino(
                "Easyrig", "Easyrig", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Le harnais à potence qui porte la caméra par un câble : le dos dit merci.",
                listOf("Principe" vaut "potence à ressort au-dessus de l'épaule"),
                "« L'Easyrig » : pour les longues journées caméra à l'épaule ou au gimbal.",
                "Le poids passe par les hanches au lieu des bras : on tient plus longtemps sans perdre la liberté du porté."
            ),
            machino(
                "Slider", "", SUPPORTS, Silhouette.MACHINO_SUPPORT_CAMERA,
                "Un petit rail posé sur pieds ou sur une table : un travelling d'un mètre.",
                listOf("Course" vaut "de 60 cm à environ 1,5 m"),
                "« Le slider » : pour animer un plan serré ou un insert.",
                "Monté sur deux pieds ou des apple boxes, il donne un petit mouvement latéral ou avant sans installer de rails."
            )
        )
    ),
    Etagere(
        TRAVELLING, listOf(
            machino(
                "Rails de travelling", "", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "Les rails droits et courbes sur lesquels roule la dolly.",
                listOf("Écartement" vaut "environ 62 cm (24,5 pouces), le standard", "Éléments" vaut "droits et courbes, à assembler"),
                "« Les rails », « poser du rail », « un travelling de 6 mètres ». « Mettre de niveau » avec des cales.",
                "On les pose sur le sol, on les cale de niveau, on vérifie qu'aucune jonction ne cogne. Le machino « pousse » la dolly en suivant les marques.",
                "Un rail mal calé se voit à l'image : le niveau se vérifie à chaque jonction.",
                aussi = listOf("track", "voie")
            ),
            machino(
                "PeeWee", "Chapman/Leonard", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "La dolly compacte à colonne hydraulique, qui passe les portes.",
                listOf("Colonne" vaut "hydraulique", "Roues" vaut "sur rail ou sur sol lisse"),
                "« La PeeWee », « la Chapman ».",
                "Petite et étroite, elle passe dans les décors naturels et lève la caméra en douceur pendant le plan."
            ),
            machino(
                "Dolly Fisher", "J.L. Fisher", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "La dolly de studio américaine, avec son bras et sa direction en crabe.",
                listOf("Modèles connus" vaut "Fisher 10 et 11", "Direction" vaut "normale, crabe, rond"),
                "« La Fisher », « la 10 », « la 11 » ; « passe en crabe ».",
                "Le bras lève la caméra pendant le mouvement ; la direction en crabe déplace la dolly en biais sans la tourner."
            ),
            machino(
                "Dolly Panther", "Panther", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "La dolly allemande à colonne électrique, très répandue en Europe.",
                listOf("Colonne" vaut "électrique, programmable"),
                "« La Panther », « la Evolution », « la Bazooka » pour sa colonne de surélévation.",
                "Sa colonne électrique monte et descend à vitesse réglée ; elle accepte rails et roues de sol."
            ),
            machino(
                "Dolly plateau et western", "", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "Le plateau à roues : simple, robuste, pour caméra et opérateur.",
                listOf("Roues" vaut "pneus (western) ou galets pour rail"),
                "« La western », « la doorway dolly », « le plateau ».",
                "On y pose un trépied ou un high hat, l'opérateur s'assoit ou se tient debout, un machino pousse.",
                aussi = listOf("doorway dolly", "western dolly")
            ),
            machino(
                "Dolly araignée", "", TRAVELLING, Silhouette.MACHINO_TRAVELLING,
                "La petite dolly à pattes repliables, pour les rails et les petits espaces.",
                listOf("Exemple" vaut "Elemack Cricket"),
                "« L'Elemack », « l'araignée » (spider dolly).",
                "Ses pattes se replient en ligne pour rouler sur un rail ou s'écartent pour une base stable."
            )
        )
    ),
    Etagere(
        GRUES, listOf(
            machino(
                "Louma", "Louma", GRUES, Silhouette.MACHINO_GRUE,
                "La grue à tête télécommandée inventée en France : la caméra va là où personne ne tient.",
                listOf("Principe" vaut "bras léger, caméra pilotée à distance"),
                "« La Louma » : le mot a longtemps désigné toutes les grues télécommandées.",
                "Le cadreur reste au sol devant un moniteur et pilote la tête ; le bras emmène la caméra au-dessus d'une foule ou à travers une fenêtre.",
                histoire = "Mise au point en France dans les années 1970, elle a ouvert l'ère des grues à tête télécommandée.",
                annee = 1975, approx = true
            ),
            machino(
                "Technocrane", "Technocrane", GRUES, Silhouette.MACHINO_GRUE,
                "La grue télescopique : le bras s'allonge pendant le plan.",
                listOf("Particularité" vaut "bras télescopique et tête télécommandée"),
                "« La Techno » ; on dit « télescoper » quand le bras s'allonge.",
                "Le bras s'étire ou se rétracte pendant la prise : la caméra avance vers un comédien sans que la base bouge."
            ),
            machino(
                "Jimmy Jib", "Jimmy Jib", GRUES, Silhouette.MACHINO_GRUE,
                "La grue légère démontable, à tête télécommandée.",
                listOf("Montage" vaut "par sections, longueur variable"),
                "« Le Jimmy Jib », « un jib » pour une petite grue.",
                "Très utilisée en télévision et en clip ; elle se monte vite et passe partout."
            ),
            machino(
                "Grue sur véhicule (Russian Arm)", "Filmotechnic", GRUES, Silhouette.MACHINO_GRUE,
                "La grue montée sur une voiture de travelling, pour suivre un véhicule.",
                listOf("Base" vaut "véhicule rapide, tête stabilisée"),
                "« La Russian Arm », « la voiture travelling ».",
                "Elle filme les poursuites et les voitures en mouvement, la tête stabilisée gommant les secousses."
            ),
            machino(
                "Car mount et hostess tray", "", GRUES, Silhouette.MACHINO_GRUE,
                "Fixer la caméra sur une voiture : capot, portière, ventouses.",
                listOf("Fixations" vaut "ventouses, sangles, plateau de portière"),
                "« Un hostess » (le plateau accroché à la portière), « un car mount », « une ventouse ».",
                "On filme le conducteur à travers le pare-brise ou par la vitre latérale ; tout est doublé de sangles de sécurité."
            )
        )
    )
)

val ETAGERES_ELECTRIQUE: List<Etagere<FicheMachinerie>> = listOf(
    Etagere(
        PRISES, listOf(
            elec(
                "P17", "", PRISES, Silhouette.ELEC_PRISE,
                "La prise industrielle ronde du tournage : bleue en 230 V, rouge en 400 V.",
                listOf(
                    "Calibres" vaut "16, 32, 63, 125 A",
                    "Bleue" vaut "230 V, monophasé : 3 broches",
                    "Rouge" vaut "400 V, triphasé : 5 broches"
                ),
                "« Un 16 mono », « un 32 tétra », « un 63 », « un 125 ». P17 est son nom courant ; la norme est la CEE (IEC 60309).",
                "On demande les prolongs par calibre et par nombre de phases : « trois 16 mono de 25 m et un 32 tétra ». La couleur dit la tension, la taille dit l'intensité.",
                "Bleu = mono 230 V, rouge = tri 400 V. Plus la prise est grosse, plus elle passe d'ampères.",
                aussi = listOf("prise CEE", "prise industrielle", "tétra")
            ),
            elec(
                "Powerlock", "", PRISES, Silhouette.ELEC_PRISE,
                "Les connecteurs unipolaires des fortes puissances : un câble par phase.",
                listOf("Intensités" vaut "400 A et 660 A", "Couleurs" vaut "une par phase, bleu le neutre, vert-jaune la terre"),
                "« Du Powerlock », « les mono-conducteurs » : entre le groupe électrogène et la première armoire.",
                "Chaque conducteur (trois phases, neutre, terre) a son câble et sa fiche verrouillable. On respecte l'ordre de branchement : la terre d'abord, à débrancher en dernier.",
                aussi = listOf("monoconducteurs", "single")
            ),
            elec(
                "Socapex", "", PRISES, Silhouette.ELEC_PRISE,
                "Le connecteur multibroche qui fait passer six circuits dans un seul câble.",
                listOf("Broches" vaut "19", "Circuits" vaut "6"),
                "« Un Soca », « une éclateuse » au bout pour récupérer les six circuits.",
                "Très utilisé avec les gradateurs : un seul câble jusqu'à la perche, puis une éclateuse (break-out) en six prises.",
                aussi = listOf("Soca", "multipaire", "éclateuse")
            ),
            elec(
                "powerCON", "Neutrik", PRISES, Silhouette.ELEC_PRISE,
                "Le connecteur verrouillable des projecteurs LED et des appareils de scène.",
                listOf("Intensité" vaut "jusqu'à 20 A", "Versions" vaut "bleu (entrée), gris (sortie), TRUE1"),
                "« Un powerCON », « un TRUE1 ».",
                "Il alimente les panneaux LED, les ballasts, et permet de chaîner l'alimentation d'un appareil à l'autre (TRUE1)."
            ),
            elec(
                "Prise domestique 16 A", "", PRISES, Silhouette.ELEC_PRISE,
                "La prise murale française (2P+T) : à n'utiliser que pour les petites charges.",
                listOf("Intensité" vaut "16 A au mieux, souvent moins sur une installation ancienne"),
                "« Une prise secteur », « du domestique ».",
                "Un 2K en tungstène tire à lui seul près de 9 A en 230 V : deux sur la même prise murale font sauter le disjoncteur.",
                "On ne branche jamais un gros projecteur sur une prise domestique sans connaître le circuit."
            ),
            elec(
                "XLR 4 broches et D-Tap", "", PRISES, Silhouette.ELEC_PRISE,
                "Le courant continu des batteries : moniteurs, petites LED, accessoires caméra.",
                listOf("Tension" vaut "12 à 14 V en continu (batteries V-mount, Gold Mount)"),
                "« Un câble XLR 4 », « un D-Tap ».",
                "Ils alimentent moniteurs, émetteurs vidéo, petites lumières depuis une batterie de caméra."
            )
        )
    ),
    Etagere(
        CABLES, listOf(
            elec(
                "Prolongateur P17", "", CABLES, Silhouette.ELEC_CABLE,
                "La rallonge industrielle : par calibre, par phases, par longueur.",
                listOf(
                    "16 A mono" vaut "câble 3G2,5 en général",
                    "32 A tri" vaut "câble 5G6 en général",
                    "Longueurs" vaut "10, 25, 50 m"
                ),
                "« Un prolong », « un 16 de 25 », « un 32 tétra de 50 ».",
                "Câble souple caoutchouc (H07RN-F) pour l'extérieur. On compte les longueurs sur le plan : distance au groupe plus les détours, sans tendre le câble.",
                "Un prolong enroulé chauffe sous forte charge : on le déroule entièrement.",
                aussi = listOf("prolongateur", "rallonge")
            ),
            elec(
                "Triplette et doublette", "", CABLES, Silhouette.ELEC_CABLE,
                "Le répartiteur qui fait trois (ou deux) prises d'une seule.",
                listOf("Courant" vaut "le total ne dépasse jamais le calibre de l'entrée"),
                "« Une triplette » : trois 16 mono depuis un 16 mono.",
                "Pratique au pied d'un groupe de petits projecteurs. Trois prises ne veulent pas dire trois fois plus de courant : on additionne les puissances."
            ),
            elec(
                "Câble de tête HMI", "", CABLES, Silhouette.ELEC_CABLE,
                "Le câble entre le ballast et la tête d'un HMI.",
                listOf("Particularité" vaut "propre au modèle de projecteur"),
                "« Le câble de tête », « la rallonge de tête ».",
                "Il porte la haute tension d'amorçage : on le branche et débranche projecteur éteint, et on emporte une rallonge de tête si le ballast doit rester loin."
            ),
            elec(
                "Enrouleur", "", CABLES, Silhouette.ELEC_CABLE,
                "Le câble sur touret : pratique, mais à dérouler en entier sous charge.",
                listOf("Règle" vaut "déroulé entièrement au-delà de la petite charge"),
                "« Un enrouleur », « un touret ».",
                "Un câble enroulé ne dissipe pas sa chaleur : sous un projecteur puissant, il peut fondre. On le déroule complètement."
            ),
            elec(
                "Câble DMX", "", CABLES, Silhouette.ELEC_CABLE,
                "Le câble de commande des projecteurs : intensité, couleur, effets.",
                listOf("Connecteur" vaut "XLR 5 broches (3 broches parfois)"),
                "« Du DMX », « chaîner en DMX », et « un bouchon » (terminaison) au bout de la chaîne.",
                "On relie la console au premier projecteur, puis de projecteur en projecteur. Chaque appareil prend une adresse."
            )
        )
    ),
    Etagere(
        DISTRIB, listOf(
            elec(
                "Groupe électrogène", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "Le moteur qui produit l'électricité du tournage là où il n'y en a pas assez.",
                listOf("Puissance" vaut "en kVA ; on compte souvent kW ≈ 0,8 × kVA", "Version tournage" vaut "insonorisée"),
                "« Le groupe », « un 60 », « un 100 kVA » ; « le groupiste » le conduit et le surveille.",
                "On additionne les puissances des projecteurs (le bilan du plan au sol), on ajoute une marge, et on choisit le groupe. Il se place loin du plateau, à cause du bruit.",
                "Le bilan électrique du plan au sol donne la puissance à demander."
            ),
            elec(
                "Armoire de distribution", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "Le boîtier qui répartit une grosse arrivée en prises protégées.",
                listOf("Entrée" vaut "63 ou 125 A triphasé, ou Powerlock", "Sorties" vaut "32 A tri, 16 A mono…", "Protection" vaut "disjoncteurs et différentiels 30 mA"),
                "« Une armoire », « une distrib », « une boîte de distribution ».",
                "Elle fait le lien entre le groupe (ou le branchement forain) et les prolongs. On y répartit les projecteurs sur les trois phases pour équilibrer la charge.",
                "Répartir les circuits sur les trois phases : c'est le travail du chef électro, et le bilan du plan au sol l'aide."
            ),
            elec(
                "Différentiel 30 mA", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "La protection des personnes : il coupe à la moindre fuite de courant.",
                listOf("Sensibilité" vaut "30 mA"),
                "« Le diff », « ça a sauté au diff ».",
                "Il coupe si une partie du courant fuit vers la terre, par un câble abîmé ou un projecteur mouillé. Un diff qui saute signale un défaut : on cherche avant de réarmer."
            ),
            elec(
                "Ballast HMI", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "L'alimentation électronique d'un HMI : amorçage, puissance, flicker-free.",
                listOf("Électronique" vaut "sans scintillement aux cadences courantes"),
                "« Le ballast », « le flicker-free ».",
                "Il allume la lampe à décharge et la maintient stable. En électronique, il évite le scintillement à l'image ; certains permettent de réduire la puissance."
            ),
            elec(
                "Gradateur", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "Le variateur des projecteurs tungstène.",
                listOf("Effet secondaire" vaut "la lumière se réchauffe quand on baisse"),
                "« Un gradateur », « un dimmer », « un bloc ».",
                "Il baisse un tungstène sans gélatine ; mais la lumière devient plus orange en baissant. Les LED se règlent par leur propre commande."
            ),
            elec(
                "Branchement forain", "", DISTRIB, Silhouette.ELEC_DISTRIBUTION,
                "Le raccordement provisoire au réseau, demandé au distributeur d'électricité.",
                listOf("Délai" vaut "à demander longtemps à l'avance"),
                "« Le forain », « un branchement chantier ».",
                "Pour un décor fixe utilisé plusieurs jours, il remplace le groupe : silencieux, sans carburant. Il se demande bien avant le tournage."
            )
        )
    ),
    Etagere(
        CONTROLE, listOf(
            elec(
                "Console DMX", "", CONTROLE, Silhouette.ELEC_CONTROLE,
                "Le pupitre qui pilote les projecteurs : intensités, couleurs, effets.",
                listOf("Protocole" vaut "DMX512, univers de 512 canaux"),
                "« La console », « le pupitre », « envoie-moi du 50 % sur le 3 ».",
                "Chaque projecteur a une adresse ; la console règle ses canaux. On enregistre les ambiances en mémoires."
            ),
            elec(
                "DMX sans fil (CRMX)", "LumenRadio", CONTROLE, Silhouette.ELEC_CONTROLE,
                "Le DMX par radio : plus de câble jusqu'aux projecteurs.",
                listOf("Principe" vaut "émetteur à la console, récepteurs intégrés ou ajoutés"),
                "« Le CRMX », « appairer » un projecteur.",
                "Beaucoup de LED récentes le reçoivent directement : on pilote depuis une console ou une tablette sans tirer de câble."
            ),
            elec(
                "Applications de pilotage", "", CONTROLE, Silhouette.ELEC_CONTROLE,
                "Piloter les LED depuis un téléphone : Sidus Link, apps des fabricants.",
                listOf("Liaison" vaut "Bluetooth ou réseau sans fil"),
                "« Je te le passe en app », « Sidus ».",
                "Pratique pour une petite équipe : intensité, température de couleur et effets se règlent depuis le plateau."
            )
        )
    )
)

val CATALOGUE_MACHINERIE: List<FicheMachinerie> by lazy { ETAGERES_MACHINERIE.flatMap { it.entrees } }
val CATALOGUE_ELECTRIQUE: List<FicheMachinerie> by lazy { ETAGERES_ELECTRIQUE.flatMap { it.entrees } }
