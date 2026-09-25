package fr.cellule.core

/**
 * Les fiches produits du Labo : ce qu'un fabricant dit de son film, de son
 * révélateur ou de ses bains, résumé et daté. Chaque fait porte la fiche d'où
 * il vient ; les temps de développement sont lus dans [TempsPublies].
 *
 * Une fiche ne dit que ce que la source dit. Ce qui manque est écrit comme
 * manquant (champ [lacune]) plutôt que complété de mémoire.
 */
enum class GenreFiche(val libelle: String) {
    FILM("Films"),
    REVELATEUR("Révélateurs"),
    BAIN("Arrêt, fixage, rinçage")
}

data class Fait(val intitule: String, val valeur: String, val detail: String = "")

data class FicheLabo(
    val nom: String,
    val fabricant: String,
    val genre: GenreFiche,
    val resume: String,
    val faits: List<Fait>,
    val source: Source,
    val lacune: String = "",
    val autresSources: List<Source> = emptyList(),
    /** Pour un film ou un révélateur : la clé de ses lignes dans [TempsPublies]. */
    val cleTemps: String = nom
)

object FichesLabo {

    private val FILMS = listOf(
        FicheLabo(
            "Ilford HP5 Plus", "Ilford (HARMAN technology)", GenreFiche.FILM,
            "Film noir et blanc rapide, ISO 400/27°, pour le reportage, la lumière disponible et l'usage général.",
            listOf(
                Fait("Sensibilité", "ISO 400/27°", "meilleurs résultats à EI 400 ; bonne qualité jusqu'à EI 3200 avec un développement prolongé"),
                Fait("Plage d'EI", "400 à 3200", "évaluation pratique, pas la vitesse normalisée du pied de courbe"),
                Fait("Supports", "35 mm : acétate 0,125 mm", "120 : acétate 0,110 mm avec dos anti-halo qui s'efface au développement ; plan-film : polyester 0,180 mm"),
                Fait("Réciprocité", "Tc = Tm^1,31", "rien à corriger de 1/10 000 à ½ s"),
                Fait("Agitation en cuve", "4 retournements", "dans les 10 premières secondes, puis dans les 10 premières secondes de chaque minute"),
                Fait("Agitation continue", "temps −15 % au plus", "en cuvette, ou en rotation sans prémouillage"),
                Fait("Manipulation", "noir complet"),
                Fait("Conservation", "10 à 20 °C, au sec", "au réfrigérateur ou au congélateur possible, en laissant le film revenir à température ; développer vite une fois exposé"),
                Fait("Séchage", "30 à 40 °C", "en armoire, ou à température ambiante sans poussière")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "Kodak Tri-X 400", "Kodak Alaris", GenreFiche.FILM,
            "Le film rapide de Kodak. Les temps de départ visent un indice de contraste de 0,56 ; à ajuster par essais.",
            listOf(
                Fait("Contraste visé", "CI 0,56", "par les temps de départ de la fiche"),
                Fait("Push", "jusqu'à EI 3200", "un diaph de sous-exposition au temps normal ; deux ou trois diaphs en poussant, avec plus de contraste et de grain et moins de détail dans les ombres"),
                Fait("Réciprocité", "1 s → 2 s · 10 s → 50 s · 100 s → 1200 s", "développement −10, −20, −30 % ; pose inchangée de 1/1000 à 1/10 s"),
                Fait("Poses très brèves", "1/10 000 s : +½ diaph", "1/100 000 s : +1 diaph ; développement +10 % dès 1/1000, jusqu'à +20 %"),
                Fait("Agitation en cuve", "5 à 7 retournements en 5 s", "puis la même agitation toutes les 30 s"),
                Fait("Temps courts", "plus de 5 min", "en dessous, le développement risque d'être irrégulier"),
                Fait("Bains suivants", "18 à 24 °C", "arrêt 30 s, fixage (Rapid Fixer 2 à 4 min), lavage 20 à 30 min, PHOTO-FLO 30 s")
            ),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "Kodak T-MAX 100", "Kodak Alaris", GenreFiche.FILM,
            "Temps et push lus dans la fiche du révélateur D-76.",
            listOf(
                Fait("Push en D-76", "EI 200 au temps normal", "EI 400 : 11 min à 20 °C au lieu de 9"),
                Fait("Régénération", "révélateur modifié", "Kodak conseille un régénérateur D-76 modifié quand on ne développe que des T-MAX")
            ),
            Sources.KODAK_D76,
            lacune = "La fiche du film lui-même (Kodak F-4016) n'a pas pu être relue : réciprocité et autres révélateurs manquent."
        ),
        FicheLabo(
            "Kodak T-MAX 400", "Kodak Alaris", GenreFiche.FILM,
            "Temps et push lus dans la fiche du révélateur D-76.",
            listOf(
                Fait("Push en D-76", "EI 800 au temps normal", "EI 1600 : 10½ min à 20 °C au lieu de 8"),
                Fait("Régénération", "révélateur modifié", "Kodak conseille un régénérateur D-76 modifié quand on ne développe que des T-MAX")
            ),
            Sources.KODAK_D76,
            lacune = "La fiche du film lui-même (Kodak F-4016) n'a pas pu être relue : réciprocité et autres révélateurs manquent."
        ),
        FicheLabo(
            "Ilford Delta 3200", "Ilford (HARMAN technology)", GenreFiche.FILM,
            "Film très rapide, publié d'EI 400 à EI 12500, et jusqu'à EI 25000 à titre de guide.",
            listOf(
                Fait("Révélateurs conseillés", "ILFOTEC DD-X, ID-11, MICROPHEN", "marqués comme recommandés dans les tableaux"),
                Fait("Réciprocité", "Tc = Tm^1,33", "au-delà d'une seconde"),
                Fait("Température", "12 min à 20 °C", "deviennent 10 min à 22 °C et 15 min à 18 °C"),
                Fait("EI 12500 et plus", "essais d'abord", "le fabricant demande de tester avant un travail qui compte")
            ),
            Sources.ILFORD_DELTA_3200,
            lacune = "Sa sensibilité nominale n'a pas été relue dans la fiche : le Labo ne l'affiche pas."
        ),
        FicheLabo(
            "Ilford Pan F Plus", "Ilford (HARMAN technology)", GenreFiche.FILM,
            "Fiche partielle.",
            listOf(
                Fait("Réciprocité", "Tc = Tm^1,33", "rien à corriger de 1/10 000 à ½ s"),
                Fait("Température", "4 min à 20 °C", "deviennent 3 min à 23 °C et 6 min à 16 °C")
            ),
            Sources.ILFORD_PANF,
            lacune = "Seules la réciprocité et la compensation de température ont été relues ; les temps de développement manquent."
        )
    )

    private val REVELATEURS = listOf(
        FicheLabo(
            "D-76", "Kodak Alaris", GenreFiche.REVELATEUR,
            "Révélateur en poudre : pleine sensibilité, bon détail dans les ombres, contraste normal, grain fin. Dilué 1+1 : plus de netteté, un peu plus de grain.",
            listOf(
                Fait("Conservation, bouteille pleine", "6 mois", "à moitié pleine : 2 mois ; en cuvette : 24 h ; en cuve à couvercle flottant : 1 mois"),
                Fait("Capacité sans régénération", "4 films par litre", "en allongeant le temps de 15 % tous les 4 films par gallon (3,8 L)"),
                Fait("Dilué 1+1", "usage unique", "diluer juste avant, jeter après ; ne pas régénérer"),
                Fait("Volume en 1+1", "473 mL par film 135-36", "avec 237 mL seulement, allonger le temps de 10 %"),
                Fait("Régénération", "22 à 30 mL de D-76R par film", "sans allonger les temps"),
                Fait("Agitation en cuve", "5 à 7 cycles en 5 s", "repos jusqu'à 30 s, puis 5 s toutes les 30 s"),
                Fait("Temps courts", "plus de 5 min", "en dessous, uniformité douteuse"),
                Fait("Réglage du contraste", "±10 à 15 %", "allonger si trop doux, raccourcir si trop dur")
            ),
            Sources.KODAK_D76
        ),
        FicheLabo(
            "ID-11", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Révélateur en poudre. Dans sa fiche HP5 Plus, Ilford le range parmi ses meilleurs choix ; la règle de température vient de la fiche ID-11.",
            listOf(
                Fait("Stock", "meilleure qualité d'ensemble", "HP5 Plus à EI 400 et 800"),
                Fait("1+1", "usage unique"),
                Fait("1+3", "netteté maximale, économie"),
                Fait("Régénérable", "oui"),
                Fait("Température", "10 % par degré", "à composer : 6 min à 20 °C font 4½ min à 23 °C et 9 min à 16 °C")
            ),
            Sources.ILFORD_HP5,
            lacune = "Les tableaux propres à la fiche ID-11 n'ont pas été relus : les temps viennent des fiches des films.",
            autresSources = listOf(Sources.ILFORD_ID11)
        ),
        FicheLabo(
            "MICROPHEN", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Révélateur en poudre qui tire le maximum de sensibilité.",
            listOf(
                Fait("Stock", "EI 1600 et 3200", "meilleure qualité et sensibilité maximale avec la HP5 Plus"),
                Fait("1+1", "usage unique"),
                Fait("1+3", "économie")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "PERCEPTOL", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Révélateur en poudre à grain le plus fin — au prix d'un peu de sensibilité.",
            listOf(
                Fait("Stock", "grain le plus fin", "HP5 Plus exposée à EI 250 ; 320 en 1+1 et 1+3"),
                Fait("Surexposition accidentelle", "EI 50 à 200", "Ilford donne des temps pour sauver une HP5 exposée trop généreusement : négatifs utilisables, pas de la même qualité")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "ILFOTEC DD-X", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Liquide concentré, 1+4 en usage unique.",
            listOf(Fait("Pour la HP5 Plus", "meilleure qualité, grain le plus fin, sensibilité maximale", "de EI 400 à 3200")),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "ILFOSOL 3", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Liquide concentré, 1+9 en usage unique ; publié aussi à 1+14.",
            listOf(Fait("1+9", "netteté maximale, usage unique", "avec la HP5 Plus")),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "ILFOTEC HC", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Liquide concentré, régénérable.",
            listOf(Fait("Régénérable", "oui"), Fait("Dilutions", "1+15 et 1+31")),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "ILFOTEC LC29", "Ilford (HARMAN technology)", GenreFiche.REVELATEUR,
            "Liquide concentré ; à 1+29, le choix économique d'Ilford.",
            listOf(Fait("Dilutions", "1+9, 1+19, 1+29")),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "XTOL", "Kodak Alaris", GenreFiche.REVELATEUR,
            "Révélateur Kodak, publié pur ou dilué 1+1.",
            listOf(Fait("Dilutions publiées", "stock et 1+1", "dans les fiches Tri-X (Kodak) et HP5 Plus (Ilford)")),
            Sources.KODAK_TRIX,
            lacune = "La fiche du XTOL (Kodak J-109) n'a pas pu être relue."
        ),
        FicheLabo(
            "HC-110", "Kodak Alaris", GenreFiche.REVELATEUR,
            "Révélateur Kodak, publié en « dilutions » désignées par des lettres.",
            listOf(Fait("Dilutions publiées", "A et B", "dans les fiches Tri-X (Kodak) et HP5 Plus (Ilford)")),
            Sources.KODAK_TRIX,
            lacune = "La fiche du HC-110 (Kodak J-24), qui donne le rapport de chaque lettre, n'a pas pu être relue."
        ),
        FicheLabo(
            "Rodinal", "Agfa, selon la fiche Ilford", GenreFiche.REVELATEUR,
            "Publié à 1+25 et 1+50 dans la fiche HP5 Plus d'Ilford.",
            listOf(
                Fait("Dilutions publiées", "1+25 et 1+50", "dans la fiche HP5 Plus d'Ilford"),
                Fait("À titre indicatif", "selon Ilford", "les temps d'autres marques sont donnés « pour commodité » ; leurs fabricants peuvent changer leurs produits")
            ),
            Sources.ILFORD_HP5,
            lacune = "La fiche de son fabricant actuel n'a pas pu être relue."
        ),
        FicheLabo(
            "T-MAX RS", "Kodak Alaris", GenreFiche.REVELATEUR,
            "Révélateur et régénérateur Kodak : des temps courts.",
            listOf(
                Fait("Tri-X 400 à 20 °C", "4 min 30", "EI 1600 : 7 min 45 ; EI 3200 : 9 min 30"),
                Fait("Temps courts", "moins de 5 min", "Kodak prévient qu'en dessous de 5 min, le résultat peut être irrégulier")
            ),
            Sources.KODAK_TRIX,
            lacune = "La fiche du révélateur n'a pas pu être relue : seuls les temps de la fiche Tri-X figurent ici."
        ),
        FicheLabo(
            "Acufine", "selon la fiche Ilford", GenreFiche.REVELATEUR,
            "Publié pur dans la fiche HP5 Plus d'Ilford, jusqu'à EI 1600.",
            listOf(Fait("À titre indicatif", "selon Ilford", "temps d'une autre marque, donnés « pour commodité »")),
            Sources.ILFORD_HP5,
            lacune = "La fiche de son fabricant n'a pas pu être relue."
        ),
        FicheLabo(
            "Tetenal Ultrafin SF", "Tetenal, selon la fiche Ilford", GenreFiche.REVELATEUR,
            "Publié pur et en 1+1 dans la fiche HP5 Plus d'Ilford.",
            listOf(Fait("À titre indicatif", "selon Ilford", "temps d'une autre marque, donnés « pour commodité »")),
            Sources.ILFORD_HP5,
            lacune = "La fiche de Tetenal n'a pas pu être relue."
        ),
        FicheLabo(
            "Tetenal Ultrafin Plus", "Tetenal, selon la fiche Ilford", GenreFiche.REVELATEUR,
            "Publié en 1+4 dans la fiche HP5 Plus d'Ilford, jusqu'à EI 1600.",
            listOf(Fait("À titre indicatif", "selon Ilford", "temps d'une autre marque, donnés « pour commodité »")),
            Sources.ILFORD_HP5,
            lacune = "La fiche de Tetenal n'a pas pu être relue."
        ),
        FicheLabo(
            "T-MAX Developer", "Kodak Alaris", GenreFiche.REVELATEUR,
            "Révélateur Kodak.",
            listOf(Fait("Dilution", "1+4", "selon la fiche HP5 Plus d'Ilford")),
            Sources.KODAK_TRIX,
            lacune = "La fiche du révélateur n'a pas pu être relue."
        )
    )

    private val BAINS = listOf(
        FicheLabo(
            "ILFOSTOP", "Ilford (HARMAN technology)", GenreFiche.BAIN,
            "Bain d'arrêt à indicateur coloré. Il stoppe le développement aussitôt et ménage le fixateur.",
            listOf(
                Fait("Dilution", "1+19"),
                Fait("Température", "18 à 24 °C"),
                Fait("Durée à 20 °C", "10 s", "minimum ; plus long sans inconvénient"),
                Fait("Capacité", "15 films 135-36 par litre", "sans régénération")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "RAPID FIXER", "Ilford (HARMAN technology)", GenreFiche.BAIN,
            "Fixateur conseillé par Ilford pour ses films, avec le HYPAM.",
            listOf(
                Fait("Dilution pour film", "1+4"),
                Fait("Température", "18 à 24 °C"),
                Fait("Durée à 20 °C", "2 à 5 min"),
                Fait("Capacité", "24 films 135-36 par litre", "sans régénération")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "HYPAM", "Ilford (HARMAN technology)", GenreFiche.BAIN,
            "L'autre fixateur conseillé par Ilford pour ses films, aux mêmes réglages que le RAPID FIXER.",
            listOf(
                Fait("Dilution pour film", "1+4"),
                Fait("Température", "18 à 24 °C"),
                Fait("Durée à 20 °C", "2 à 5 min"),
                Fait("Capacité", "24 films 135-36 par litre", "sans régénération")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "ILFOTOL", "Ilford (HARMAN technology)", GenreFiche.BAIN,
            "Agent mouillant du rinçage final : le film sèche vite et régulièrement.",
            listOf(
                Fait("Dose de départ", "5 mL par litre", "1+200, à ajuster selon l'eau et le séchage"),
                Fait("Attention", "ni trop, ni trop peu", "l'un comme l'autre laisse des traces")
            ),
            Sources.ILFORD_HP5
        ),
        FicheLabo(
            "Lavage Ilford en cuve", "Ilford (HARMAN technology)", GenreFiche.BAIN,
            "Plus rapide et plus économe que l'eau courante, pour des négatifs aptes à la conservation.",
            listOf(
                Fait("1", "remplir, 5 retournements, vider"),
                Fait("2", "remplir, 10 retournements, vider"),
                Fait("3", "remplir, 20 retournements, vider"),
                Fait("Eau", "à 5 °C au plus des bains"),
                Fait("Sinon", "eau courante 5 à 10 min")
            ),
            Sources.ILFORD_HP5
        ),
        /* Kodak, fiche Tri-X : tous les bains qui suivent le révélateur, entre 18 et 24 °C. */
        FicheLabo(
            "KODAK Indicator Stop Bath", "Kodak Alaris", GenreFiche.BAIN,
            "Bain d'arrêt de Kodak, à indicateur coloré.",
            listOf(Fait("Durée", "30 s", "avec agitation"), Fait("Température", "18 à 24 °C")),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAK Fixer", "Kodak Alaris", GenreFiche.BAIN,
            "Le fixateur classique de Kodak : plus lent que les fixateurs rapides.",
            listOf(Fait("Durée", "5 à 10 min", "avec agitation fréquente"), Fait("Température", "18 à 24 °C")),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAK Rapid Fixer", "Kodak Alaris", GenreFiche.BAIN,
            "Fixateur rapide de Kodak.",
            listOf(Fait("Durée", "2 à 4 min", "avec agitation fréquente"), Fait("Température", "18 à 24 °C")),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAFIX", "Kodak Alaris", GenreFiche.BAIN,
            "Fixateur liquide de Kodak.",
            listOf(Fait("Durée", "2 à 4 min", "avec agitation fréquente"), Fait("Température", "18 à 24 °C")),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAK POLYMAX T", "Kodak Alaris", GenreFiche.BAIN,
            "Fixateur Kodak, dilué 1:3 — une part pour trois d'eau.",
            listOf(Fait("Dilution", "1+3", "écrit « 1:3 » par Kodak"), Fait("Durée", "2 à 4 min", "avec agitation fréquente")),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAK Hypo Clearing Agent", "Kodak Alaris", GenreFiche.BAIN,
            "Éliminateur d'hyposulfite : il raccourcit le lavage.",
            listOf(
                Fait("Avant", "30 s de rinçage à l'eau"),
                Fait("Bain", "1 à 2 min"),
                Fait("Ensuite", "5 min d'eau courante", "au lieu de 20 à 30 min sans lui")
            ),
            Sources.KODAK_TRIX
        ),
        FicheLabo(
            "KODAK PHOTO-FLO", "Kodak Alaris", GenreFiche.BAIN,
            "Agent mouillant du rinçage final.",
            listOf(Fait("Durée", "30 s"), Fait("Séchage", "à l'abri de la poussière")),
            Sources.KODAK_TRIX
        )
    )

    val TOUTES: List<FicheLabo> = FILMS + REVELATEURS + BAINS

    fun parGenre(g: GenreFiche): List<FicheLabo> = TOUTES.filter { it.genre == g }

    /** Sans accents, sans majuscules, sans la marque en tête. */
    private fun cleNom(t: String): String {
        val n = sansAccents(t).lowercase()
        return listOf("kodak ", "ilford ", "tetenal ").fold(n) { s, prefixe -> s.removePrefix(prefixe) }
    }

    /**
     * La fiche que désigne un nom de calculateur ou de table publiée, quel
     * que soit son habillage : marque en tête, dilution ou variante ajoutée
     * après (« Kodak D-76 1+1 », « HC-110 dilution B » pointent la même fiche
     * que « D-76 » ou « HC-110 »). Null si aucune fiche ne correspond : mieux
     * vaut ne pas proposer de lien que d'en proposer un faux.
     */
    fun parNom(nom: String): FicheLabo? {
        val cible = cleNom(nom)
        return TOUTES.firstOrNull { f ->
            val base = cleNom(f.nom)
            base == cible || cible.startsWith("$base ") || base.startsWith("$cible ")
        }
    }

    /** Les temps publiés à afficher sur une fiche, du plus usuel au plus poussé. */
    fun temps(f: FicheLabo): List<TempsPublie> = when (f.genre) {
        GenreFiche.FILM -> TempsPublies.pourFilm(f.cleTemps)
        GenreFiche.REVELATEUR -> TempsPublies.pourRevelateur(f.cleTemps)
        GenreFiche.BAIN -> emptyList()
    }.sortedWith(compareBy({ it.temperature != 20.0 }, { it.film }, { it.revelateur }, { it.dilution }, { it.ei }, { it.temperature }))

    /**
     * Ce qui manque encore au Labo, faute d'avoir pu relire la fiche du
     * fabricant. Mieux vaut une case vide qu'un chiffre recopié.
     */
    val A_RELIRE = listOf(
        "Ilford FP4 Plus, Delta 100, Delta 400, Kentmere",
        "Kodak T-MAX 100 et 400 (fiche F-4016), T-MAX P3200",
        "Foma : Fomapan 100, 200, 400 et leurs révélateurs",
        "Adox : Rodinal (Adonal), et ses temps pour les films Foma",
        "Fixateurs Foma (Fomafix) et Adox ; ILFORD WASHAID",
        "Kodak XTOL (J-109) et HC-110 (J-24)",
        "Révélateurs papier : Dektol, Ilford Multigrade et PQ Universal, Fomatol"
    )
}
