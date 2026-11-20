package fr.cellule.core

/*
 * Apprendre avant de s'entraîner : une fiche par formule, avec ce que veut
 * dire chaque lettre, un exemple chiffré pas à pas, le repère à retenir et le
 * piège classique. Chaque question de QuizFormules renvoie à sa fiche par
 * son sujet : quand on se trompe, on la relit.
 */

data class FicheFormule(
    val theme: ThemeFormule,
    /** Le même nom que dans les questions : « Loi du carré de la distance ». */
    val sujet: String,
    val formule: String,
    /** Ce que veut dire chaque lettre, avec son unité. */
    val lettres: List<String>,
    val exemple: String,
    val repere: String,
    val piege: String
)

object FichesFormules {

    fun du(theme: ThemeFormule): List<FicheFormule> = TOUTES.filter { it.theme == theme }

    fun pour(theme: ThemeFormule, sujet: String): FicheFormule? = TOUTES.firstOrNull { it.theme == theme && it.sujet == sujet }

    val TOUTES: List<FicheFormule> by lazy {
        listOf(
            /* ── Exposition ── */
            FicheFormule(
                ThemeFormule.EXPOSITION, "Indice de lumination", "EV = log₂(N² / t)",
                listOf("N : nombre d'ouverture (f/…)", "t : temps de pose, en secondes", "EV : indice de lumination, à 100 ISO"),
                "f/8 au 1/125 s : 8² × 125 = 8 000, et log₂(8 000) ≈ 13. EV 13.",
                "f/16 au 1/125 ≈ EV 15 : c'est le « sunny 16 », plein soleil à 100 ISO. Un EV de plus, c'est deux fois plus de lumière.",
                "Oublier le carré sur N : on se trompe de plusieurs EV."
            ),
            FicheFormule(
                ThemeFormule.EXPOSITION, "Sensibilité", "EV(ISO) = EV₁₀₀ + log₂(ISO / 100)",
                listOf("EV₁₀₀ : l'EV mesuré à 100 ISO", "ISO : la sensibilité réglée"),
                "Une scène à EV 12 (100 ISO), en 400 ISO : 12 + log₂(4) = 14. Tu peux fermer de deux diaphs.",
                "Doubler la sensibilité gagne un diaph : 100 → 200 → 400 → 800 ISO, c'est 1, 2, 3 diaphs.",
                "Confondre rapport et diaphs : de 100 à 400 ISO, c'est ×4 mais 2 diaphs, pas 4."
            ),
            FicheFormule(
                ThemeFormule.EXPOSITION, "Cellule incidente", "E (lux) = 2,5 × 2^EV₁₀₀",
                listOf("E : éclairement reçu par le sujet, en lux", "2,5 : la constante d'étalonnage C = 250, divisée par 100 ISO"),
                "EV 10 : 2,5 × 1 024 = 2 560 lx.",
                "Plein soleil ≈ 80 000 lx (EV 15) ; un bureau bien éclairé ≈ 500 lx (EV 7 à 8).",
                "Confondre avec la réfléchie : 2,5 × 2^EV donne des lux (la lumière qui arrive), 0,125 × 2^EV des cd/m² (la lumière renvoyée)."
            ),
            FicheFormule(
                ThemeFormule.EXPOSITION, "Cellule réfléchie", "L (cd/m²) = 0,125 × 2^EV₁₀₀",
                listOf("L : luminance de la surface visée, en candelas par mètre carré", "0,125 = 1/8 : vient de la constante K = 12,5"),
                "EV 11 : 2¹¹ / 8 = 256 cd/m².",
                "La cellule réfléchie ramène tout au gris moyen : un mur blanc mesuré et exposé tel quel sortira gris.",
                "Prendre le 2,5 de l'incident : on trouve 20 fois trop."
            ),
            FicheFormule(
                ThemeFormule.EXPOSITION, "Ouverture pour une vitesse", "N = √(2^EV × t)",
                listOf("N : l'ouverture qui expose juste", "EV : la scène (à l'ISO réglé)", "t : temps de pose, en secondes"),
                "EV 12 au 1/60 s : √(4 096 / 60) ≈ 8,3, donc f/8.",
                "À EV constant, chaque vitesse divisée par deux demande d'ouvrir d'un diaph.",
                "Oublier la racine : c'est N² qui vaut 2^EV × t, pas N."
            ),
            FicheFormule(
                ThemeFormule.EXPOSITION, "Diaphs", "écart (diaphs) = log₂(rapport) ; rapport = 2^diaphs",
                listOf("rapport : entre deux quantités de lumière", "diaphs : l'écart, en puissances de 2"),
                "De 400 à 3 200 lx : rapport 8 = 2³, donc 3 diaphs.",
                "1/3 de diaph = ×1,26 ; 1/2 = ×1,41 ; 1 = ×2 ; 2 = ×4 ; 3 = ×8.",
                "Prendre le rapport pour des diaphs : ×4, c'est 2 diaphs."
            ),

            /* ── Tournage ── */
            FicheFormule(
                ThemeFormule.TOURNAGE, "Obturateur", "t = angle / (360 × cadence)",
                listOf("t : temps de pose, en secondes", "angle : ouverture de l'obturateur, en degrés", "cadence : images par seconde"),
                "180° à 25 im/s : 180 / 9 000 = 1/50 s.",
                "180°, c'est la moitié du temps d'une image : le flou de mouvement « naturel ». 172,8° à 24 im/s donne 1/50 s, sûr en 50 Hz.",
                "Croire que 180° donne toujours 1/50 : à 24 im/s, c'est 1/48."
            ),
            FicheFormule(
                ThemeFormule.TOURNAGE, "Densité neutre", "densité ≈ 0,3 × diaphs ; transmission = 2^−diaphs",
                listOf("densité : ce qui est gravé sur le filtre (ND 0,6…)", "diaphs : la lumière retirée"),
                "ND 0,9 : 0,9 / 0,3 = 3 diaphs, transmission 1/8 = 12,5 %.",
                "ND 0,3 / 0,6 / 0,9 / 1,2 = 1 / 2 / 3 / 4 diaphs. L'autre notation compte en facteur : ND8 = ND 0,9.",
                "Lire ND 0,9 comme 9 diaphs, ou comme 0,9 diaph."
            ),
            FicheFormule(
                ThemeFormule.TOURNAGE, "T-stop qui expose juste", "T = √(2^(EV₁₀₀ + log₂(EI/100) − pertes) × t)",
                listOf("EV₁₀₀ : la scène mesurée à 100 ISO", "EI : l'indice de la caméra", "pertes : filtres devant l'objectif, en diaphs", "t : temps de pose de l'obturateur"),
                "EV 11, EI 400, ND 0,3, 1/48 s : √(2^(11 + 2 − 1) / 48) = √(4 096 / 48) ≈ 9,2, donc T8 et un tiers.",
                "C'est l'ouverture pour une vitesse, avec l'EI et les filtres en plus : un diaph d'EI en plus ou de filtre en moins, c'est un diaph à fermer.",
                "Ajouter les pertes au lieu de les retrancher : un ND oblige à ouvrir, pas à fermer."
            ),
            FicheFormule(
                ThemeFormule.TOURNAGE, "T et f", "T = f / √(transmission)",
                listOf("f : ouverture géométrique (f/…)", "transmission : part de la lumière que laisse passer le verre, de 0 à 1"),
                "f/2,8 avec 70 % de transmission : 2,8 / √0,7 ≈ T3,3.",
                "Le T mesure la lumière qui arrive vraiment au capteur : deux objectifs à T2 exposent pareil, deux objectifs à f/2 pas forcément.",
                "Multiplier par la transmission au lieu de diviser par sa racine."
            ),

            /* ── Lumière ── */
            FicheFormule(
                ThemeFormule.LUMIERE, "Intensité d'une source", "I (cd) = E₀ × d₀²",
                listOf("I : intensité dans l'axe, en candelas", "E₀ : éclairement mesuré à la distance d₀, en lux", "d₀ : en mètres"),
                "2 000 lx à 3 m : 2 000 × 9 = 18 000 cd.",
                "L'intensité ne dépend pas de la distance : c'est la carte d'identité du projecteur, d'où l'on tire l'éclairement à toute distance.",
                "Multiplier par d₀ au lieu de d₀²."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Loi du carré de la distance", "E = I × cos θ / d²",
                listOf("E : éclairement au sujet, en lux", "I : intensité, en candelas", "θ : angle d'arrivée (0° = de face)", "d : distance, en mètres"),
                "1 600 lx à 2 m ; à 4 m : 1 600 × (2 / 4)² = 400 lx.",
                "Distance ×2 = lumière ÷4 = 2 diaphs ; distance ×1,4 = 1 diaph. Valable au-delà d'environ 5 fois la taille de la source.",
                "Penser « deux fois plus loin, deux fois moins de lumière »."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Distance pour un éclairement", "d = √(I × cos θ / E)",
                listOf("d : la distance cherchée", "I : intensité de la source", "E : l'éclairement voulu au sujet"),
                "10 000 cd, je veux 625 lx : √(10 000 / 625) = √16 = 4 m.",
                "Pour gagner un diaph au sujet, rapproche le projecteur de ×0,7 (÷1,4).",
                "Oublier la racine."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Tache de lumière", "D = 2 × d × tan(faisceau / 2)",
                listOf("D : diamètre de la tache", "d : distance au projecteur", "faisceau : angle total du faisceau"),
                "30° à 4 m : 2 × 4 × tan 15° ≈ 2,1 m.",
                "Un faisceau d'environ 53° fait une tache aussi large que la distance.",
                "Prendre tan(faisceau) sans diviser l'angle par deux."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Géométrie", "d = √(recul² + (h_projecteur − h_sujet)²)",
                listOf("recul : distance au sol entre le pied et le sujet", "Δh = h_projecteur − h_sujet"),
                "Projecteur à 3,1 m, visage à 1,6 m, 2 m de recul : Δh = 1,5 ; d = √(4 + 2,25) = 2,5 m.",
                "Le triangle 3-4-5 et ses multiples (1,5-2-2,5…) se calculent de tête.",
                "Prendre la hauteur du projecteur au lieu de Δh, ou additionner recul et Δh."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Plusieurs sources", "E_total = E₁ + E₂ + …",
                listOf("E₁, E₂ : l'éclairement de chaque source au même point"),
                "Face 800 lx + débouchage 200 lx = 1 000 lx sur le côté qu'ils éclairent tous les deux.",
                "Deux sources identiques = un diaph de plus ; quatre = deux diaphs. Une source quatre fois plus faible n'ajoute qu'un tiers de diaph.",
                "Additionner des diaphs : f/4 + f/4 ne fait pas f/8."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Rapports", "face / débouchage = E_face / E_déb ; côté éclairé / côté ombre = (E_face + E_déb) / E_déb",
                listOf("E_face : la lumière principale", "E_déb : le débouchage, qui éclaire aussi le côté ombre"),
                "Face 300 lx, débouchage 100 lx : 3:1 entre les sources, 4:1 entre les deux côtés du visage.",
                "2:1 doux, 4:1 marqué, 8:1 et plus très contrasté (low key).",
                "Confondre les deux rapports."
            ),
            FicheFormule(
                ThemeFormule.LUMIERE, "Unités", "1 foot-candle = 10,764 lux ; 1 pied = 0,3048 m",
                listOf("fc : foot-candle, l'éclairement en unités américaines", "pied : 30,48 cm"),
                "1 000 lx ≈ 93 fc ; 10 pieds ≈ 3 m.",
                "De tête : 1 fc ≈ 10 lux, 1 pied ≈ 30 cm.",
                "Multiplier au lieu de diviser : les fc font toujours un nombre plus petit que les lux."
            ),

            /* ── Couleur ── */
            FicheFormule(
                ThemeFormule.COULEUR, "Mired", "mired = 1 000 000 / K",
                listOf("K : température de couleur, en kelvins"),
                "3 200 K : 312 mireds ; 5 600 K : 179 mireds.",
                "Une gélatine décale toujours du même nombre de mireds, quelle que soit la source : c'est pour ça qu'on compte en mireds.",
                "Comparer des kelvins : 2 700 → 3 200 K (500 K) est un plus gros écart que 5 600 → 6 500 K (900 K)."
            ),
            FicheFormule(
                ThemeFormule.COULEUR, "Décalage à corriger", "Δ = 10⁶/K_cible − 10⁶/K_source",
                listOf("K_source : la lumière telle qu'elle est", "K_cible : la couleur voulue"),
                "De 5 600 à 3 200 K : 312 − 179 = +134 mireds, vers le chaud : une CTO.",
                "Positif : vers l'orange (CTO). Négatif : vers le bleu (CTB). Les gélatines s'additionnent en mireds.",
                "Inverser cible et source, donc le signe."
            ),
            FicheFormule(
                ThemeFormule.COULEUR, "Perte d'une gélatine", "diaphs = −log₂(transmission)",
                listOf("transmission : part de la lumière qui traverse, de 0 à 1"),
                "50 % : 1 diaph ; 25 % : 2 diaphs ; 70 % : environ un demi-diaph.",
                "Les corrections bleues (CTB) coûtent nettement plus de lumière que les orange (CTO).",
                "Prendre 1 − transmission : 70 %, ce n'est pas 0,3 diaph mais un demi."
            ),

            /* ── Optique ── */
            FicheFormule(
                ThemeFormule.OPTIQUE, "Angle de champ", "α = 2 × atan(dimension / (2 × f))",
                listOf("dimension : largeur (ou hauteur, ou diagonale) utile du capteur", "f : focale"),
                "36 mm de large et un 50 mm : 2 × atan(0,36) ≈ 40°.",
                "En plein format le 50 mm couvre ≈ 40° en largeur ; en Super 35, il faut un 35 mm pour le même champ.",
                "Oublier un des deux « 2 »."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE, "Focale pour un angle", "f = dimension / (2 × tan(α / 2))",
                listOf("α : l'angle de champ voulu", "dimension : du capteur, dans le même sens"),
                "60° sur un Super 35 : 24,9 / (2 × tan 30°) ≈ 21,6 mm.",
                "Pour les angles moyens : champ ÷2 ≈ focale ×2.",
                "Mettre α au lieu de α/2 dans la tangente."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE, "Ce que couvre le cadre", "largeur = distance × capteur / f",
                listOf("distance : du sujet", "capteur : largeur utile", "f : focale"),
                "Plein format, 50 mm, 5 m : 5 × 36 / 50 = 3,6 m de décor.",
                "Focale ×2 = largeur ÷2, à distance égale.",
                "Inverser capteur et focale."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE, "Cercle de confusion", "c = diagonale du capteur / 1 500",
                listOf("c : le plus gros point flou qu'on accepte comme net"),
                "Plein format (diagonale 43,3 mm) : 0,029 mm, qu'on arrondit à 0,03.",
                "C'est une convention, pas une loi : pour une projection sur grand écran, on prend plus petit.",
                "Diviser par 150 : un cercle dix fois trop grand, et une profondeur de champ très optimiste."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE, "Hyperfocale", "H = f² / (N × c) + f",
                listOf("f : focale (mm)", "N : ouverture", "c : cercle de confusion (mm)"),
                "50 mm, f/8, c = 0,03 : 2 500 / 0,24 + 50 ≈ 10 467 mm, soit 10,5 m.",
                "Au point sur H, tout est net de H/2 à l'infini. Fermer d'un diaph divise H par ≈1,4.",
                "Oublier le carré sur f, ou mélanger mètres et millimètres."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE, "Profondeur de champ", "proche = s(H − f) / (H + s − 2f) ; loin = s(H − f) / (H − s)",
                listOf("s : distance de mise au point", "H : hyperfocale", "f : focale (souvent négligeable devant s et H)"),
                "H = 10 m, point à 5 m : net de 3,3 m à 10 m.",
                "Aux distances moyennes, environ un tiers de la zone nette est devant le point et deux tiers derrière.",
                "Si s ≥ H, la limite lointaine est l'infini : la formule donnerait une distance négative."
            ),

            /* ── Optique géométrique ── */
            FicheFormule(
                ThemeFormule.OPTIQUE_GEO, "Conjugaison", "1/OA' − 1/OA = 1/f'",
                listOf("O : centre optique", "A : le sujet ; OA est négatif, il est devant l'objectif", "A' : son image", "f' : distance focale image"),
                "50 mm, sujet à 2 m : OA' = 50 × 2 000 / 1 950 ≈ 51,3 mm. L'objectif avance de 1,3 mm pour faire le point (le tirage).",
                "Sujet à l'infini : l'image est au foyer. Plus le sujet se rapproche, plus l'image recule.",
                "Oublier le signe de OA : avec + au lieu de −, l'image passe devant le foyer."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE_GEO, "Grandissement", "γ = OA' / OA",
                listOf("γ : taille de l'image sur taille du sujet", "γ négatif : image renversée"),
                "1,8 m à 3 m d'un 50 mm : γ ≈ −0,017, une image de 30,5 mm, renversée.",
                "Loin de l'objectif, |γ| ≈ f / d.",
                "Inverser OA et OA' : on trouverait une image des dizaines de fois plus grande que le sujet."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE_GEO, "Nombre d'ouverture", "N = f / D",
                listOf("f : focale", "D : diamètre de la pupille d'entrée"),
                "Un 85 mm à f/1,4 : pupille de 61 mm.",
                "Chaque diaph fermé divise D par √2 et la lumière par 2.",
                "Croire que f/2 a la même pupille sur un 25 et un 100 mm : 12,5 contre 50 mm, pour la même exposition."
            ),
            FicheFormule(
                ThemeFormule.OPTIQUE_GEO, "Vergence", "V = 1 / f' (f' en mètres)",
                listOf("V : vergence, en dioptries (δ)", "f' : focale, en mètres"),
                "50 mm : V = 1 / 0,05 = 20 δ. Une bonnette +2 δ sur un objectif réglé à l'infini fait le point à 0,5 m.",
                "Les vergences de lentilles accolées s'additionnent.",
                "Laisser f' en millimètres : 1 / 50 ne fait pas 20 δ."
            ),

            /* ── Sensitométrie ── */
            FicheFormule(
                ThemeFormule.SENSITOMETRIE, "Densité optique", "D = log₁₀(1 / T)",
                listOf("T : transmission, part de la lumière qui traverse, de 0 à 1", "1 / T : l'opacité"),
                "T = 1 % : D = log₁₀(100) = 2.",
                "D = 0,3, c'est la moitié de la lumière : un diaph. C'est la même échelle que les filtres ND.",
                "Prendre log₂ : la densité est un logarithme décimal."
            ),
            FicheFormule(
                ThemeFormule.SENSITOMETRIE, "Gamma", "γ = ΔD / Δlog H",
                listOf("ΔD : écart de densité", "Δlog H : écart de lumination, en log₁₀"),
                "La densité passe de 0,5 à 1,1 pour Δlog H = 1 : γ = 0,6.",
                "Un négatif noir et blanc courant tourne autour de 0,6 à 0,7 ; pousser au développement augmente le gamma, donc le contraste.",
                "Inverser le rapport : 1 / 0,6 ≈ 1,7."
            ),
            FicheFormule(
                ThemeFormule.SENSITOMETRIE, "Latitude", "1 diaph = 0,3 en log H",
                listOf("log H : logarithme décimal de la lumination", "0,3 ≈ log₁₀ 2"),
                "Δlog H = 2,1 : 2,1 / 0,3 = 7 diaphs.",
                "Même règle que les densités neutres : 0,3 par diaph.",
                "Compter un diaph par unité de log H."
            ),
            FicheFormule(
                ThemeFormule.SENSITOMETRIE, "ISO et DIN", "DIN = 10 × log₁₀(ISO) + 1",
                listOf("ISO : sensibilité arithmétique", "DIN : sensibilité logarithmique, en degrés"),
                "ISO 400 : 10 × 2,6 + 1 = 27°.",
                "100 ISO = 21° ; +3° DIN = sensibilité ×2 = un diaph.",
                "Croire la DIN proportionnelle : 27° n'est pas « un peu plus » que 21°, c'est deux diaphs."
            ),

            /* ── Numérique ── */
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Niveaux de quantification", "niveaux = 2^bits",
                listOf("bits : profondeur de codage d'une composante"),
                "10 bits : 1 024 niveaux ; 12 bits : 4 096.",
                "Chaque bit de plus double les nuances : moins de banding sur les dégradés.",
                "Croire que 10 bits = « un peu plus » que 8 bits : c'est quatre fois plus de niveaux."
            ),
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Dynamique", "diaphs = log₂(rapport)",
                listOf("rapport : entre la plus forte et la plus faible lumière enregistrables"),
                "4 096:1 : log₂(4 096) = 12 diaphs.",
                "Une caméra de 14 diaphs distingue un rapport de 16 384:1.",
                "Prendre log₁₀ : 4 096:1 ne fait pas 3,6 diaphs."
            ),
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Dynamique en décibels", "dB = 20 × log₁₀(rapport) ≈ 6 × diaphs",
                listOf("rapport : de signal (tension), haut sur bas"),
                "12 diaphs, soit 4 096:1 : ≈ 72 dB.",
                "Un diaph ≈ 6 dB pour un signal ; c'est aussi ce qu'apporte chaque bit.",
                "Prendre 10 × log₁₀ (puissance) : on trouve la moitié."
            ),
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Débit brut", "débit (octets/s) = largeur × hauteur × bits × cadence / 8",
                listOf("largeur × hauteur : en photosites", "bits : par photosite", "cadence : images par seconde"),
                "1920 × 1080 × 10 bits × 25 im/s ÷ 8 ≈ 65 Mo/s en RAW.",
                "Un RAW garde une valeur par photosite ; une image RVB 4:4:4 en garde trois : ×3.",
                "Oublier le ÷ 8 entre bits et octets."
            ),
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Stockage", "taille (octets) = débit (bits/s) × durée / 8",
                listOf("débit : annoncé en Mbit/s par le codec", "durée : en secondes"),
                "220 Mbit/s pendant 1 h : 220 / 8 × 3 600 = 99 000 Mo ≈ 99 Go.",
                "Les codecs s'annoncent en Mbit/s (petit b), les cartes en Go (grand O) : divise par 8.",
                "Confondre Mb et Mo."
            ),
            FicheFormule(
                ThemeFormule.NUMERIQUE, "Échantillonnage", "f_éch ≥ 2 × f_max (Shannon-Nyquist)",
                listOf("f_éch : fréquence d'échantillonnage", "f_max : la plus haute fréquence à garder"),
                "Un son jusqu'à 20 kHz demande au moins 40 kHz : on tourne en 48 kHz.",
                "En 48 kHz, on garde jusqu'à 24 kHz. Le moiré de l'image vient du même principe : un motif plus fin que deux photosites.",
                "Échantillonner à f_max : tout ce qui dépasse f_éch / 2 revient en fausses fréquences."
            ),

            /* ── Électricité ── */
            FicheFormule(
                ThemeFormule.ELECTRICITE, "Monophasé", "I = P / (U × cos φ)",
                listOf("I : courant, en ampères", "P : puissance, en watts", "U : tension (230 V en Europe, 120 V aux États-Unis)", "cos φ : facteur de puissance, 1 pour du tungstène"),
                "2 000 W en 230 V : ≈ 8,7 A.",
                "En 230 V, 1 A ≈ 230 W ; une prise 16 A tient 3 680 W au plus.",
                "Prendre 400 V (le triphasé) pour une prise domestique."
            ),
            FicheFormule(
                ThemeFormule.ELECTRICITE, "Triphasé", "I = P / (√3 × U × cos φ)",
                listOf("U : tension entre phases, 400 V en Europe", "I : courant dans chaque phase"),
                "12 kW en 400 V : 12 000 / 693 ≈ 17,3 A par phase.",
                "Répartis les projecteurs sur les trois phases : c'est la charge de chaque phase qui compte.",
                "Diviser par 400 seulement."
            ),
            FicheFormule(
                ThemeFormule.ELECTRICITE, "Groupe électrogène", "kW = 0,8 × kVA",
                listOf("kVA : puissance apparente, celle de la plaque du groupe", "0,8 : facteur de puissance nominal"),
                "Un groupe de 6 kVA : 4,8 kW utiles.",
                "Garde de la marge : un HMI à ballast magnétique appelle plus que sa puissance à l'allumage.",
                "Lire les kVA comme des kW."
            ),
            FicheFormule(
                ThemeFormule.ELECTRICITE, "Marge", "charge ≤ 80 % du calibre",
                listOf("calibre : l'intensité du disjoncteur, en ampères"),
                "Circuit 16 A : 12,8 A en charge continue, soit ≈ 2 944 W en 230 V.",
                "C'est la règle américaine (NEC) pour les charges continues ; ailleurs, garde la même prudence.",
                "Charger un circuit à 100 % : le disjoncteur chauffe et finit par sauter."
            ),

            /* ── Scintillement ── */
            FicheFormule(
                ThemeFormule.SCINTILLEMENT, "Battement des sources", "f_battement = 2 × f_secteur",
                listOf("f_secteur : 50 Hz en Europe, 60 Hz aux États-Unis"),
                "50 Hz → 100 Hz ; 60 Hz → 120 Hz.",
                "Concerne les HMI à ballast magnétique, les tubes fluo, certaines LED ; le tungstène bat très peu, grâce à l'inertie du filament.",
                "Prendre la fréquence du secteur : l'intensité s'annule deux fois par période."
            ),
            FicheFormule(
                ThemeFormule.SCINTILLEMENT, "Vitesse sûre", "t × f_battement = nombre entier",
                listOf("t : temps de pose", "f_battement : 100 ou 120 Hz"),
                "En 50 Hz : 1/100, 1/50, 1/25 sont sûrs ; 1/48 ne l'est pas.",
                "En 50 Hz, pense « multiples de 1/100 » ; en 60 Hz, « multiples de 1/120 ».",
                "Tourner au 1/48 (180° à 24 im/s) en Europe, sous des HMI à ballast magnétique."
            ),
            FicheFormule(
                ThemeFormule.SCINTILLEMENT, "Angles sûrs", "angle = k × 360 × cadence / f_battement",
                listOf("k : un nombre entier (1, 2, 3…)", "cadence : images par seconde"),
                "24 im/s sur 50 Hz : 86,4°, 172,8°, 259,2°, 345,6°.",
                "25 im/s sur 50 Hz : 180° est sûr. C'est une des raisons du 25 im/s en Europe.",
                "Garder 180° à 24 im/s sur un secteur à 50 Hz."
            ),
            FicheFormule(
                ThemeFormule.SCINTILLEMENT, "Cadence sûre", "f_battement / cadence = nombre entier",
                listOf("cadence : images par seconde"),
                "En 50 Hz : 25 et 50 im/s tombent juste (100 / 25 = 4) ; 24 non.",
                "Cadence sûre et obturateur réglé juste : chaque image reçoit la même lumière.",
                "Croire qu'une vitesse sûre suffit quand la cadence varie (ralenti progressif)."
            )
        )
    }
}
