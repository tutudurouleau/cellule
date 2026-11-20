package fr.cellule.core

/*
 * Les marques du cinéma : celles qu'on lit sur les caisses chez le loueur et
 * qu'on entend nommer sur le plateau. Les grandes, et les plus petites qui
 * sont partout.
 */

enum class DomaineMarque(val libelle: String) {
    CAMERA("Caméras"),
    OBJECTIF("Objectifs"),
    LUMIERE("Lumière"),
    FILTRE("Gélatines et filtres"),
    MACHINERIE("Machinerie et supports"),
    ACCESSOIRE("Accessoires caméra et vidéo")
}

data class Marque(
    val nom: String,
    val pays: String,
    val domaines: List<DomaineMarque>,
    val connuePour: String,
    val produits: String,
    /** Une des grandes maisons du métier, plutôt qu'une marque plus récente ou plus spécialisée. */
    val grande: Boolean = true
)

private fun marque(nom: String, pays: String, d: List<DomaineMarque>, connue: String, produits: String, grande: Boolean = true) =
    Marque(nom, pays, d, connue, produits, grande)

object Marques {
    private val CAM = DomaineMarque.CAMERA
    private val OBJ = DomaineMarque.OBJECTIF
    private val LUM = DomaineMarque.LUMIERE
    private val FIL = DomaineMarque.FILTRE
    private val MAC = DomaineMarque.MACHINERIE
    private val ACC = DomaineMarque.ACCESSOIRE

    val TOUTES: List<Marque> = listOf(
        /* Caméras */
        marque("ARRI", "Allemagne", listOf(CAM, LUM, OBJ, ACC), "La référence du cinéma, des caméras argentiques à l'ALEXA, et l'un des grands de la lumière.", "ALEXA 35, ALEXA Mini LF, SkyPanel, Orbiter, M18, objectifs Signature"),
        marque("RED", "États-Unis", listOf(CAM), "Les caméras numériques à très haute définition qui ont bousculé le cinéma à la fin des années 2000.", "V-RAPTOR, KOMODO"),
        marque("Sony", "Japon", listOf(CAM), "De la VENICE du long métrage aux FX du tournage léger.", "VENICE 2, BURANO, FX6, FX3"),
        marque("Blackmagic Design", "Australie", listOf(CAM, ACC), "Des caméras de cinéma abordables et le logiciel d'étalonnage DaVinci Resolve.", "URSA Cine, Pocket Cinema Camera, DaVinci Resolve"),
        marque("Canon", "Japon", listOf(CAM, OBJ), "Caméras Cinema EOS et objectifs de cinéma, très présents en documentaire.", "C70, C300, C500, objectifs CN-E, zooms Cine-Servo"),
        marque("Panavision", "États-Unis", listOf(CAM, OBJ, LUM), "Ne vend rien : loue ses caméras et ses optiques, dont des anamorphiques célèbres.", "Millennium DXL2, séries Primo, Ultra Panatar"),
        marque("Aaton", "France", listOf(CAM), "Caméras 16 et 35 mm de Grenoble, pensées pour l'épaule et le documentaire.", "XTR Prod, A-Minima, Penelope", grande = false),
        marque("Panasonic", "Japon", listOf(CAM), "Caméras de cinéma et hybrides prisées des petites équipes.", "VariCam, LUMIX S1H, GH", grande = false),

        /* Objectifs */
        marque("Cooke", "Royaume-Uni", listOf(OBJ), "Le « Cooke look » : un rendu doux et chaleureux des visages.", "S4/i, S7/i, Panchro/i Classic, anamorphiques /i"),
        marque("ZEISS", "Allemagne", listOf(OBJ), "Des optiques de cinéma précises et neutres, des Super Speed aux Supreme.", "Master Prime, Ultra Prime, Supreme Prime, CP.3"),
        marque("Angénieux", "France", listOf(OBJ), "Les zooms de cinéma de Saint-Héand, utilisés dans le monde entier.", "Optimo 24-290, Optimo Ultra, EZ"),
        marque("Leitz", "Allemagne", listOf(OBJ), "Les optiques Leica de cinéma, compactes et très piquées.", "Summilux-C, Summicron-C, Thalia"),
        marque("Fujinon", "Japon", listOf(OBJ), "Zooms de cinéma et de télévision, de la gamme Premista aux Cabrio.", "Premista, Cabrio, MK"),
        marque("Hawk", "Allemagne", listOf(OBJ), "Les anamorphiques de Vantage Film, loués à Paris par Vantage.", "V-Lite, Vintage '74, MINIHAWK", grande = false),
        marque("Sigma", "Japon", listOf(OBJ), "Optiques de cinéma abordables, très répandues sur les petits tournages.", "Cine FF High Speed, Cine Classic", grande = false),
        marque("Atlas", "États-Unis", listOf(OBJ), "Anamorphiques abordables qui ont démocratisé le format.", "Orion, Mercury", grande = false),
        marque("DZOFilm", "Chine", listOf(OBJ), "Objectifs de cinéma à petit prix, en plein essor.", "Vespid, Pictor, Arles", grande = false),
        marque("Laowa", "Chine", listOf(OBJ), "Optiques originales : macro-sonde, très grands angles.", "Probe 24 mm, Nanomorph", grande = false),

        /* Lumière */
        marque("K5600", "France", listOf(LUM), "HMI compacts et polyvalents, imaginés en France.", "Joker-Bug, Alpha"),
        marque("Kino Flo", "États-Unis", listOf(LUM), "La lumière douce des tubes, fluo puis LED.", "FreeStyle, Celeb, Diva-Lite"),
        marque("Dedolight", "Allemagne", listOf(LUM), "Petits projecteurs à optique de précision.", "Dedo DLH4, Lightstream"),
        marque("Aputure", "Chine", listOf(LUM), "LED ponctuelles puissantes et abordables, devenues incontournables.", "LS 600d, LS 1200d, Nova, Amaran"),
        marque("Creamsource", "Australie", listOf(LUM), "Panneaux LED très puissants pour les gros plateaux.", "Vortex"),
        marque("Astera", "Allemagne", listOf(LUM), "Tubes et petites sources LED sans fil, pilotables à distance.", "Titan, Helios, AX9"),
        marque("Litepanels", "États-Unis", listOf(LUM), "Pionnier des panneaux LED de tournage.", "Gemini, Astra"),
        marque("Nanlux / Nanlite", "Chine", listOf(LUM), "LED puissantes (Nanlux) et sources légères (Nanlite).", "Evoke, Forza, PavoTube", grande = false),
        marque("Godox", "Chine", listOf(LUM), "LED et flashs à petit prix, très répandus en autoproduction.", "Knowled, SL, UL", grande = false),
        marque("Quasar Science", "États-Unis", listOf(LUM), "Tubes LED de tournage.", "Rainbow, Double Rainbow", grande = false),
        marque("Mole-Richardson", "États-Unis", listOf(LUM), "Projecteurs tungstène historiques d'Hollywood.", "Baby, Tweenie, Mole-Richardson PAR"),
        marque("Ianiro", "Italie", listOf(LUM), "Les projecteurs ouverts qui ont donné leur nom à la blonde et à la mandarine.", "Redhead (mandarine), Blonde", grande = false),
        marque("Desisti", "Italie", listOf(LUM), "Fresnels tungstène et HMI.", "Magis, Leonardo", grande = false),
        marque("Rosco", "États-Unis", listOf(FIL, LUM), "Gélatines, diffusions, et projecteurs DMG Lumière.", "Cinegel, E-Colour+, DMG SL1"),
        marque("Lee Filters", "Royaume-Uni", listOf(FIL), "Les gélatines aux numéros à trois chiffres qu'on cite sur les plateaux.", "216, 250, 251, CTO, CTB, filtres caméra"),

        /* Machinerie */
        marque("Matthews", "États-Unis", listOf(MAC), "Le grand nom de la machinerie : C-stands, Mafer, drapeaux.", "C-stand, Mafer, Matthellini, Polecat"),
        marque("Avenger", "Italie", listOf(MAC), "Machinerie du groupe Manfrotto : pieds, rotules, clamps.", "C-stand, wind-up, rotules D200"),
        marque("Manfrotto", "Italie", listOf(MAC), "Super clamps, bras magiques, autopoles, trépieds.", "Super Clamp 035, Magic Arm 143, Autopole"),
        marque("Kupo", "Taïwan", listOf(MAC), "Machinerie robuste et moins chère, très présente chez les loueurs.", "C-stands, Kupo Grip, clamps", grande = false),
        marque("Chapman/Leonard", "États-Unis", listOf(MAC), "Dollies et grues de studio.", "PeeWee, Hybrid"),
        marque("J.L. Fisher", "États-Unis", listOf(MAC), "Les dollies de studio à bras.", "Fisher 10, Fisher 11"),
        marque("Panther", "Allemagne", listOf(MAC), "Dollies à colonne électrique et grues, très répandues en Europe.", "Evolution, Bazooka"),
        marque("Technocrane", "République tchèque", listOf(MAC), "Grues télescopiques.", "Super Technocrane"),
        marque("Sachtler", "Allemagne", listOf(MAC), "Têtes fluides et trépieds.", "Video 18, Cine 30"),
        marque("O'Connor", "États-Unis", listOf(MAC), "Têtes fluides lourdes de cinéma.", "2575, 2560"),
        marque("Cartoni", "Italie", listOf(MAC), "Têtes fluides et trépieds italiens.", "Master, Focus", grande = false),
        marque("Easyrig", "Suède", listOf(MAC), "Le harnais à potence qui soulage le porté.", "Vario, Minimax", grande = false),
        marque("DJI", "Chine", listOf(MAC, ACC), "Stabilisateurs, drones et commandes sans fil.", "Ronin, Inspire, Focus Pro", grande = false),
        marque("Tiffen", "États-Unis", listOf(MAC, FIL), "Le Steadicam et des filtres caméra.", "Steadicam, filtres Black Pro-Mist"),

        /* Accessoires caméra */
        marque("Teradek", "États-Unis", listOf(ACC), "Les émetteurs vidéo sans fil des plateaux.", "Bolt"),
        marque("SmallHD", "États-Unis", listOf(ACC), "Moniteurs de plateau.", "Cine, Ultra", grande = false),
        marque("Atomos", "Australie", listOf(ACC), "Moniteurs-enregistreurs.", "Ninja, Shogun", grande = false),
        marque("Tilta", "Chine", listOf(ACC), "Cages, follow focus et accessoires abordables.", "Nucleus, cages", grande = false),
        marque("Preston", "États-Unis", listOf(ACC), "Commandes de point sans fil de référence.", "FI+Z, Hand Unit 4", grande = false),
        marque("cmotion", "Autriche", listOf(ACC), "Commandes d'objectif sans fil.", "cforce, cPRO", grande = false),
        marque("Bright Tangerine", "Royaume-Uni", listOf(ACC), "Matte box et accessoires.", "Misfit, Strummer", grande = false)
    )

    fun chercher(texte: String, domaine: DomaineMarque?): List<Marque> {
        val mots = sansAccents(texte).lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return TOUTES.filter { m ->
            (domaine == null || domaine in m.domaines) && mots.all { mot ->
                mot in sansAccents("${m.nom} ${m.pays} ${m.connuePour} ${m.produits}").lowercase()
            }
        }
    }
}
