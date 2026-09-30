package fr.cellule.core

import kotlin.math.roundToInt

/*
 * Les recettes maison : révélateurs au café ou aux plantes, fixateur au
 * thiosulfate, arrêt, récupération de l'argent.
 *
 * Aucun fabricant ne les publie. Plutôt que de les mélanger aux fiches
 * produits, chacune dit d'où elle vient et à quel point on peut s'y fier :
 * documentée (un auteur nommé la publie), essayée par des amateurs (publiée,
 * peu recoupée), ou à tester (aucune source : un point de départ).
 *
 * Point de départ : le guide écrit avec Gemini, vérifié recette par recette.
 * Ce qui y était faux est corrigé, et chaque correction est dite.
 */

enum class NiveauRecette(val libelle: String, val detail: String) {
    DOCUMENTEE("Documentées", "Publiées par un auteur nommé, reprises par beaucoup d'autres."),
    AMATEUR("Essayées par des amateurs", "Publiées, mais peu recoupées : à confirmer par un essai."),
    HYPOTHESE("À tester", "Aucune source : un point de départ, pas une recette. Essai obligatoire.")
}

enum class RoleRecette(val libelle: String) {
    REVELATEUR("Révélateur"),
    ARRET("Arrêt"),
    FIXATEUR("Fixateur"),
    RECUPERATION("Récupération de l'argent")
}

/**
 * La forme du carbonate de sodium qu'on a sous la main. Les recettes sont
 * écrites en carbonate anhydre ; les autres formes pèsent plus lourd pour la
 * même quantité de carbonate (rapport des masses molaires : 105,99 g/mol pour
 * l'anhydre, 124,00 pour le monohydraté, 286,14 pour le décahydraté).
 */
enum class FormeCarbonate(val libelle: String, val facteur: Double, val conseil: String) {
    ANHYDRE("Anhydre", 1.0, "Carbonate de sodium pur, en poudre : c'est la forme des recettes."),
    MONOHYDRATE("Monohydraté", 124.00 / 105.99, "Environ 17 % de plus que l'anhydre."),
    DECAHYDRATE("Cristaux (décahydraté)", 286.14 / 105.99, "2,7 fois plus lourd : peser comme de l'anhydre, c'est sous-développer."),
    BICARBONATE(
        "Bicarbonate à cuire", 2 * 84.007 / 105.99,
        "30 min à 1 h à 200 °C le changent en carbonate anhydre ; il perd un tiers de son poids. " +
            "Le mieux : en cuire un bocal d'avance et le peser ensuite comme de l'anhydre."
    )
}

/**
 * Un ingrédient, pour un litre de solution. [texte] remplace la quantité quand
 * elle ne se calcule pas ; [complement] désigne l'eau qui complète au volume ;
 * [carbonate] : la quantité est en carbonate anhydre et suit la forme choisie.
 */
data class Ingredient(
    val nom: String,
    val parLitre: Double? = null,
    val unite: String = "g",
    val note: String = "",
    val carbonate: Boolean = false,
    val complement: Boolean = false,
    val texte: String? = null
)

data class RecetteMaison(
    val nom: String,
    val role: RoleRecette,
    val niveau: NiveauRecette,
    val resume: String,
    val ingredients: List<Ingredient>,
    /** Ce qu'on prépare : « de révélateur », « de fixateur usé »… */
    val volumeDe: String,
    val temps: String,
    /** En minutes : la durée proposée au chrono ; null quand elle reste à établir. */
    val tempsDepart: Double?,
    val temperature: String,
    val agitation: Agitation?,
    val agitationTexte: String,
    val films: String,
    val preparation: List<String>,
    val precautions: List<String>,
    /** Ce qui change par rapport au guide d'origine. */
    val corrections: List<String>,
    val sources: List<Source>
)

object RecettesMaison {

    /* ── Les sources relues ─────────────────────────────────────────────── */

    private val REINHOLD_CM = Source(
        "Reinhold (blog Caffenol)", "Caffenol-C-M, recipe", "", "mars 2010",
        "http://caffenol.blogspot.com/2010/03/caffenol-c-m-recipe.html"
    )
    private val REINHOLD_SEL = Source(
        "Reinhold (blog Caffenol)", "Iodized kitchen salt can replace bromide", "", "mai 2011",
        "http://caffenol.blogspot.com/2011/05/iodized-kitchen-salt-can-replace.html"
    )
    private val PHOTRIO_ARRET = Source(
        "Photrio (forum)", "Stop bath, how important?", "", "",
        "https://www.photrio.com/forum/threads/stop-bath-how-important.98559"
    )
    private val PHOTRIO_HYPO = Source(
        "Photrio (forum)", "Plain hypo fixer questions", "", "",
        "https://www.photrio.com/forum/threads/plain-hypo-fixer-questions.113563/"
    )
    private val PHOTRIO_ARGENT = Source(
        "Photrio (forum)", "Fixer & steel wool", "", "",
        "https://www.photrio.com/forum/threads/fixer-steel-wool.19917"
    )
    private val PHOTRIO_PARODINAL = Source(
        "Photrio (forum)", "Homebrew Parodinal", "", "",
        "https://www.photrio.com/forum/threads/homebrew-parodinal-genz-to-ogs.219011/"
    )
    private val SIMMONS_PARACETAMOL = Source(
        "John E. Simmons", "Paracetamol film developer", "", "",
        "https://johnesimmons.com/Formulas/Film/Paracetamol.html"
    )
    private val REVON_HERBES = Source(
        "Jacques Revon (L'Œil de la photographie)", "Le révélateur écologique aux trois herbes aromatiques", "", "",
        "https://loeildelaphotographie.com/en/the-silver-eye-jacques-revon-the-alternative-ecological-developer-made-with-3-aromatic-herbs-sage-peppermint-and-thyme"
    )

    /* ── Ce qui revient partout ─────────────────────────────────────────── */

    private val EAU = Ingredient("Eau", complement = true)
    private val PRECAUTION_CARBONATE = "Le carbonate irrite les yeux et la peau : gants et lunettes."
    private val PRECAUTION_SOUDE = "Écrire « carbonate de sodium » sur le flacon, jamais « soude » : la soude caustique est tout autre chose."
    private val AGITATION_PAR_DEFAUT = "La source ne la précise pas : celle d'Ilford par défaut, 10 s par minute."

    /** Les recettes du guide sont écrites pour 300 mL : on les ramène au litre. */
    private fun pour300(grammes: Double) = grammes * 1000 / 300

    /* ── Documentées ────────────────────────────────────────────────────── */

    private val CAFFENOL_CM = RecetteMaison(
        nom = "Caffenol-C-M",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.DOCUMENTEE,
        resume = "Le révélateur au café de Reinhold : café soluble, carbonate et vitamine C. Pour les films jusqu'à ISO 100.",
        ingredients = listOf(
            Ingredient("Carbonate de sodium", 54.0, carbonate = true),
            Ingredient("Vitamine C (acide ascorbique pur)", 16.0),
            Ingredient("Café soluble", 40.0, note = "ordinaire, le moins cher convient"),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "15 min à 20 °C : le point de départ de l'auteur.",
        tempsDepart = 15.0,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = AGITATION_PAR_DEFAUT,
        films = "Jusqu'à ISO 100 : au-delà, il voile. Pour une HP5 ou une Tri-X, la version au sel.",
        preparation = listOf(
            "Dissoudre séparément le café, le carbonate et la vitamine C, chacun dans un peu d'eau.",
            "Verser la vitamine C dans le carbonate (ça mousse, c'est normal), puis le tout dans le café.",
            "Compléter au volume et laisser revenir à 20 °C : le carbonate réchauffe l'eau en se dissolvant.",
            "Utiliser dans l'heure ou les deux heures : sans conservateur, il ne se garde pas. Les poudres, elles, se gardent."
        ),
        precautions = listOf(PRECAUTION_CARBONATE, PRECAUTION_SOUDE),
        corrections = listOf(
            "Le guide l'appelait « formule universelle » : son auteur le réserve aux films jusqu'à ISO 100.",
            "Le guide donnait 12 à 15 min ; l'auteur part de 15 min.",
            "Les retours sur la sensibilité obtenue divergent : un rouleau d'essai avant un travail qui compte.",
            "Après ce bain très chargé en carbonate, arrêt à l'eau plutôt qu'au vinaigre."
        ),
        sources = listOf(REINHOLD_CM)
    )

    private val CAFFENOL_SEL = RecetteMaison(
        nom = "Caffenol-C-M au sel",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.DOCUMENTEE,
        resume = "La même recette avec du sel iodé comme retardateur : pour les films plus rapides, les développements longs et le push.",
        ingredients = listOf(
            Ingredient("Carbonate de sodium", 54.0, carbonate = true),
            Ingredient("Vitamine C (acide ascorbique pur)", 16.0),
            Ingredient("Café soluble", 40.0),
            Ingredient("Sel iodé", 6.0, note = "films moyens ; 10 à 12 g par litre pour les films rapides (ISO 400 et plus)"),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "Plus long que sans sel, selon l'auteur : à établir par un essai.",
        tempsDepart = null,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = AGITATION_PAR_DEFAUT,
        films = "Films moyens avec 6 g/L de sel ; films rapides (HP5, Tri-X) avec 10 à 12 g/L.",
        preparation = listOf(
            "Comme le Caffenol-C-M, en dissolvant le sel avec le carbonate.",
            "Le pH ne change pas, selon l'auteur : rien d'autre à ajuster que le temps.",
            "Utiliser dans l'heure ou les deux heures."
        ),
        precautions = listOf(PRECAUTION_CARBONATE, PRECAUTION_SOUDE),
        corrections = listOf(
            "Le guide limitait le sel à moins de 1 g/L : l'auteur en met 6 à 12 g/L. En dessous, il n'agit pas.",
            "Le sel n'est pas « un antivoile puissant » : un retardateur faible, qu'il faut doser franchement. Vers 30 g/L, il dissout l'argent et coûte un demi-diaph (c'est son rôle dans le Perceptol)."
        ),
        sources = listOf(REINHOLD_SEL)
    )

    private val ARRET_EAU = RecetteMaison(
        nom = "Arrêt à l'eau",
        role = RoleRecette.ARRET,
        niveau = NiveauRecette.DOCUMENTEE,
        resume = "Après un révélateur chargé en carbonate, de l'eau plutôt que de l'acide : pas de gaz dans l'émulsion.",
        ingredients = emptyList(),
        volumeDe = "",
        temps = "Deux rinçages, 15 s d'agitation chacun.",
        tempsDepart = null,
        temperature = "Celle du révélateur",
        agitation = Agitations.CONTINUE,
        agitationTexte = "En continu, 15 s par rinçage.",
        films = "Tous.",
        preparation = listOf(
            "Remplir la cuve d'eau à la température du révélateur.",
            "Agiter 15 s, vider.",
            "Recommencer une fois, puis passer au fixateur."
        ),
        precautions = emptyList(),
        corrections = listOf(
            "Le guide proposait du vinaigre 1+4. Après un Caffenol (53 g/L de carbonate), l'acide dégage du gaz carbonique dans la gélatine : risque de petits trous dans l'émulsion."
        ),
        sources = listOf(PHOTRIO_ARRET)
    )

    private val FIXATEUR_THIOSULFATE = RecetteMaison(
        nom = "Fixateur au thiosulfate de sodium",
        role = RoleRecette.FIXATEUR,
        niveau = NiveauRecette.DOCUMENTEE,
        resume = "Le fixateur historique : des cristaux de thiosulfate dans de l'eau. Moins cher et plus lent qu'un fixateur rapide.",
        ingredients = listOf(
            Ingredient("Thiosulfate de sodium (cristaux, pentahydraté)", 200.0, note = "jusqu'à 250 g par litre"),
            Ingredient(
                "Sulfite de sodium (facultatif)", texte = "1 à 2 c. à café par litre",
                note = "le fait durer et le protège de l'acide"
            ),
            EAU
        ),
        volumeDe = "de fixateur",
        temps = "Deux fois le temps de clarification, mesuré au test de l'amorce. Le guide indique 8 à 12 min à 20 °C.",
        tempsDepart = null,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = "Comme pour le révélateur.",
        films = "Films à grain tabulaire (T-MAX, Delta, Acros, Fomapan 200) : deux bains, chacun du temps de clarification.",
        preparation = listOf(
            "Dissoudre les cristaux dans de l'eau tiède (ils la refroidissent en fondant), compléter.",
            "Test de l'amorce : un bout d'amorce dans le fixateur, à la lumière ; chronométrer jusqu'à ce qu'il soit transparent.",
            "Fixer deux fois ce temps.",
            "Il est usé quand le temps de clarification a doublé par rapport au fixateur neuf."
        ),
        precautions = listOf(
            "Acheter les cristaux, au rayon piscine ou bassin : le conditionneur d'aquarium est souvent une solution très diluée.",
            "Sans sulfite, l'acide d'un bain d'arrêt le décompose (il blanchit et dépose du soufre) : rincer à l'eau avant.",
            "Le fixateur usé contient de l'argent : jamais à l'égout."
        ),
        corrections = listOf(
            "60 à 75 g pour 300 mL (200 à 250 g/L), comme dans le guide : c'est juste.",
            "Le guide le déclarait saturé au-delà de 5 min de clarification. C'est le doublement qui compte : les films à grain tabulaire s'éclaircissent lentement même en fixateur neuf."
        ),
        sources = listOf(PHOTRIO_HYPO)
    )

    private val RECUPERATION_ARGENT = RecetteMaison(
        nom = "Récupération de l'argent",
        role = RoleRecette.RECUPERATION,
        niveau = NiveauRecette.DOCUMENTEE,
        resume = "Le fer de la paille de fer prend la place de l'argent dissous dans le fixateur usé ; l'argent se dépose en boue.",
        ingredients = listOf(
            Ingredient("Paille de fer fine (000)", 10.0, note = "10 à 20 g par litre de fixateur usé (quantités du guide)")
        ),
        volumeDe = "de fixateur usé",
        temps = "24 à 48 h (guide).",
        tempsDepart = null,
        temperature = "Ambiante",
        agitation = null,
        agitationTexte = "Aucune.",
        films = "—",
        preparation = listOf(
            "Mettre la paille de fer dans le fixateur usé, en bocal de verre fermé sans serrer.",
            "Attendre 24 à 48 h : une boue grise se dépose au fond.",
            "Filtrer sur un filtre à café et garder la boue.",
            "Porter le liquide en déchetterie, avec les déchets dangereux."
        ),
        precautions = listOf(
            "La boue n'est pas de l'argent pur : environ 60 % d'argent, le reste du fer et des sels de fer. Un affineur doit la traiter."
        ),
        corrections = listOf(
            "Le guide parlait d'« argent métallique pur » : c'est plutôt 60 %."
        ),
        sources = listOf(PHOTRIO_ARGENT)
    )

    /* ── Essayées par des amateurs ──────────────────────────────────────── */

    private val HERBES_REVON = RecetteMaison(
        nom = "Révélateur aux herbes",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.AMATEUR,
        resume = "Thym, sauge ou menthe poivrée, avec carbonate, vitamine C et sel iodé : le révélateur écologique de Jacques Revon.",
        ingredients = listOf(
            Ingredient("Carbonate de sodium", 54.0, carbonate = true, note = "forme non précisée par la source"),
            Ingredient("Vitamine C (acide ascorbique pur)", 16.0),
            Ingredient("Thym", 6.0, note = "ou sauge, ou menthe poivrée ; frais ou sec, non précisé"),
            Ingredient("Sel iodé", 10.0),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "22 à 30 min, entre 22 et 26 °C, pour du 24 × 36.",
        tempsDepart = 22.0,
        temperature = "22 à 26 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = AGITATION_PAR_DEFAUT,
        films = "Non précisé par la source : essai.",
        preparation = listOf(
            "Infuser les herbes dans l'eau chaude, filtrer.",
            "Dissoudre le carbonate, puis la vitamine C, puis le sel.",
            "Ramener à la température de travail."
        ),
        precautions = listOf(PRECAUTION_CARBONATE, PRECAUTION_SOUDE),
        corrections = listOf(
            "Le guide donnait 14 à 18 min pour ses décoctions de plantes : cette recette publiée en demande 22 à 30, et plus au chaud."
        ),
        sources = listOf(REVON_HERBES)
    )

    private val PARODINAL = RecetteMaison(
        nom = "Parodinal",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.AMATEUR,
        resume = "Le Rodinal refait avec du paracétamol : la soude caustique le change en para-aminophénol, l'agent du Rodinal.",
        ingredients = listOf(
            Ingredient("Paracétamol broyé", texte = "selon la recette suivie"),
            Ingredient("Soude caustique (hydroxyde de sodium)", texte = "en large excès"),
            Ingredient("Sulfite ou métabisulfite de sodium", texte = "selon la recette suivie")
        ),
        volumeDe = "",
        temps = "Repos d'environ 72 h avant usage, puis dilué comme le Rodinal (1+25, 1+50). Temps à établir par un essai.",
        tempsDepart = null,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = AGITATION_PAR_DEFAUT,
        films = "Essai obligatoire : sa force dépend de la préparation.",
        preparation = listOf(
            "Suivre une recette publiée en entier : les proportions varient de l'une à l'autre, ne pas les mélanger.",
            "Laisser reposer environ 72 h.",
            "Diluer comme du Rodinal. Les temps du Rodinal (fiche HP5 d'Ilford : 6 min en 1+25) ne valent que si la force est la même : essai."
        ),
        precautions = listOf(
            "La soude caustique brûle la peau et les yeux : gants et lunettes.",
            "Toujours verser la soude dans l'eau, jamais l'inverse : sa dissolution chauffe fort."
        ),
        corrections = listOf(
            "La version du guide (phosphate trisodique, sans soude, sans repos, sans dilution ni temps) n'est pas documentée et ne marchera pas telle quelle : l'hydrolyse demande un grand excès de soude caustique."
        ),
        sources = listOf(PHOTRIO_PARODINAL, SIMMONS_PARACETAMOL)
    )

    private val ARRET_VINAIGRE = RecetteMaison(
        nom = "Arrêt au vinaigre dilué",
        role = RoleRecette.ARRET,
        niveau = NiveauRecette.AMATEUR,
        resume = "Un arrêt acide doux, pour qui préfère l'acide à l'eau après un révélateur au carbonate.",
        ingredients = listOf(
            Ingredient(
                "Vinaigre blanc à 8 %", 62.5, unite = "mL",
                note = "à 12 % : 42 mL par litre ; à 14 % : 36 mL — même force, environ 0,5 % d'acide"
            ),
            EAU
        ),
        volumeDe = "d'arrêt",
        temps = "30 s sous agitation.",
        tempsDepart = null,
        temperature = "Celle du révélateur",
        agitation = Agitations.CONTINUE,
        agitationTexte = "En continu.",
        films = "Tous.",
        preparation = listOf("Diluer le vinaigre dans l'eau (1+15 pour un vinaigre à 8 %)."),
        precautions = listOf("Rincer à l'eau avant un fixateur au thiosulfate sans sulfite."),
        corrections = listOf(
            "Le guide mettait le vinaigre à 1+4, soit 1,6 à 2,8 % d'acide selon le vinaigre (8, 12 ou 14 %). Après un révélateur au carbonate, un utilisateur conseille 1+15."
        ),
        sources = listOf(PHOTRIO_ARRET)
    )

    /* ── À tester ───────────────────────────────────────────────────────── */

    private val BROU_DE_NOIX = RecetteMaison(
        nom = "Brou de noix",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.HYPOTHESE,
        resume = "La recette du guide, corrigée : l'extrait de brou avec carbonate et vitamine C, le sel dosé à part.",
        ingredients = listOf(
            Ingredient("Extrait de brou (macéré dans l'eau, filtré)", pour300(55.0), unite = "mL", note = "50 à 60 mL pour 300 mL dans le guide"),
            Ingredient("Carbonate de sodium", pour300(16.0), carbonate = true),
            Ingredient("Vitamine C (acide ascorbique pur)", pour300(5.0)),
            Ingredient("Sel", 10.0, note = "1 %, ajouté à part"),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "16 à 20 min à 20 °C (guide) : à vérifier par un essai.",
        tempsDepart = 16.0,
        temperature = "20 °C",
        agitation = Agitation(initiale = 60.0, duree = 10.0, intervalle = 60.0),
        agitationTexte = "60 s en continu, puis 4 retournements par minute (guide).",
        films = "Inconnu : essai.",
        preparation = listOf(
            "Macérer le brou dans l'eau, pas dans la saumure, et filtrer.",
            "Dissoudre le carbonate, puis la vitamine C, puis le sel ; ajouter l'extrait et compléter.",
            "Ramener à 20 °C.",
            "Préparer aussi un bain témoin sans brou : c'est lui qui dira ce que le brou apporte vraiment."
        ),
        precautions = listOf(
            "Le brou tache la peau et l'irrite : gants.",
            "Un extrait qui moisit ou fermente se jette ; le garder au frais.",
            PRECAUTION_CARBONATE
        ),
        corrections = listOf(
            "La juglone est la forme oxydée : ce n'est pas elle qui révèle (l'analogue de l'hydroquinone, c'est l'hydrojuglone du brou frais, qui s'oxyde en quelques minutes). S'il y a une action, elle vient des tanins, ou du seul couple carbonate et vitamine C.",
            "Le sel était lié à l'extrait et montait à 1,7 à 2 % ; il passe à 1 %, dosé à part.",
            "16 g de carbonate anhydre valent 43 g de cristaux décahydratés, pas 40."
        ),
        sources = emptyList()
    )

    private val TEANOL = RecetteMaison(
        nom = "Thé (« Teanol »)",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.HYPOTHESE,
        resume = "Une infusion forte de thé noir ou vert à la place du café, avec carbonate et vitamine C.",
        ingredients = listOf(
            Ingredient("Thé (sachets)", pour300(4.0), unite = "sachets", note = "4 sachets pour 300 mL dans le guide ; infusion forte"),
            Ingredient("Carbonate de sodium", pour300(16.0), carbonate = true),
            Ingredient("Vitamine C (acide ascorbique pur)", pour300(5.0)),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "15 à 17 min (guide) : à vérifier par un essai.",
        tempsDepart = 15.0,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = "Non précisée : celle d'Ilford par défaut, 10 s par minute.",
        films = "Inconnu. Sans retardateur, voile probable sur les films rapides : une piste est d'ajouter du sel, comme au Caffenol.",
        preparation = listOf(
            "Infuser le thé dans l'eau chaude, retirer les sachets.",
            "Dissoudre le carbonate, puis la vitamine C.",
            "Ramener à 20 °C, et faire un bain témoin sans thé."
        ),
        precautions = listOf(PRECAUTION_CARBONATE),
        corrections = listOf(
            "« Grain très fin, gris soyeux » : aucune source, et un bain au carbonate sans solvant ne laisse pas attendre un grain fin."
        ),
        sources = emptyList()
    )

    private val DECOCTION = RecetteMaison(
        nom = "Sauge ou lierre (décoction)",
        role = RoleRecette.REVELATEUR,
        niveau = NiveauRecette.HYPOTHESE,
        resume = "Une décoction de feuilles fraîches, avec carbonate et vitamine C. La sauge d'abord : le lierre irrite la peau.",
        ingredients = listOf(
            Ingredient("Feuilles fraîches", pour300(50.0), note = "50 g pour 300 mL dans le guide"),
            Ingredient("Carbonate de sodium", pour300(16.0), carbonate = true),
            Ingredient("Vitamine C (acide ascorbique pur)", pour300(5.0)),
            EAU
        ),
        volumeDe = "de révélateur",
        temps = "14 à 18 min selon le guide — sans doute trop court : la recette publiée aux herbes demande 22 à 30 min.",
        tempsDepart = 18.0,
        temperature = "20 °C",
        agitation = Agitations.ILFORD,
        agitationTexte = "Non précisée : celle d'Ilford par défaut, 10 s par minute.",
        films = "Inconnu : essai.",
        preparation = listOf(
            "Faire bouillir les feuilles dans l'eau, filtrer, laisser refroidir.",
            "Dissoudre le carbonate, puis la vitamine C.",
            "Ramener à 20 °C, et faire un bain témoin sans plante."
        ),
        precautions = listOf(
            "Le lierre irrite la peau (saponines, falcarinol) : gants, ou mieux, la sauge.",
            PRECAUTION_CARBONATE
        ),
        corrections = listOf("« Résultats très équilibrés » : aucune source."),
        sources = emptyList()
    )

    val TOUTES: List<RecetteMaison> = listOf(
        CAFFENOL_CM, CAFFENOL_SEL, ARRET_EAU, FIXATEUR_THIOSULFATE, RECUPERATION_ARGENT,
        HERBES_REVON, PARODINAL, ARRET_VINAIGRE,
        BROU_DE_NOIX, TEANOL, DECOCTION
    )

    /** Ce que le guide proposait et qu'il vaut mieux ne pas faire — avec la raison. */
    val A_EVITER: List<Pair<String, String>> = listOf(
        "Jus de citron" to "0,4 à 0,5 g de vitamine C par litre pour environ 50 g d'acide citrique, qui neutralise le carbonate : inutilisable.",
        "Lessive de cendre" to "Sa force est inconnue (pH vers 11) : impossible de refaire deux fois le même bain.",
        "Parodinal au phosphate trisodique" to "Non documenté : l'hydrolyse du paracétamol demande de la soude caustique et un long repos.",
        "Une pincée de sel iodé" to "Sans effet : c'est la dose, 6 à 12 g par litre, qui compte.",
        "Arrêt acide fort après un Caffenol" to "Le carbonate dégage du gaz dans l'émulsion : risque de petits trous.",
        "« Soude » écrit seul sur un flacon" to "C'est aussi le nom de la soude caustique : toujours « carbonate de sodium »."
    )

    /** L'enchaînement d'une séance, de la pesée à la note au carnet. */
    val ENCHAINEMENT: List<Pair<String, String>> = listOf(
        "Préparer" to "Peser au dixième de gramme, vérifier la forme du carbonate, dissoudre dans l'ordre, ramener à la température.",
        "Essayer" to "Pour tout bain non documenté, un bout de film sacrifié, et un bain témoin sans la plante.",
        "Développer" to "Le temps de départ de la recette, avec son agitation. Au bain-marie : la règle Ilford des 10 % par degré n'est pas prouvée pour ces bains.",
        "Arrêter" to "À l'eau, deux fois, après un bain au carbonate.",
        "Fixer" to "Deux fois le temps de clarification, mesuré au test de l'amorce.",
        "Laver" to "Eau courante, plus longtemps qu'avec un fixateur rapide.",
        "Mouiller" to "Une goutte d'agent mouillant dans le dernier rinçage.",
        "Noter" to "Film, EI, recette, marque, température, temps, résultat : c'est le carnet qui transforme un point de départ en temps fiable.",
        "Jeter" to "Révélateur et arrêt usés mélangés se neutralisent ; le fixateur va en récupération, puis en déchetterie.",
        "Se protéger" to "Gants et lunettes, ustensiles réservés au labo."
    )

    /** La quantité d'un ingrédient pour [volume] mL, dans la forme de carbonate choisie. */
    fun quantite(i: Ingredient, volume: Double, forme: FormeCarbonate): Double? =
        i.parLitre?.let { it * volume / 1000 * (if (i.carbonate) forme.facteur else 1.0) }

    fun nomIngredient(i: Ingredient, forme: FormeCarbonate): String =
        if (i.carbonate) "${i.nom} — ${forme.libelle.lowercase()}" else i.nom

    /** « 16,2 g », « 55 mL », « ≈ 4 sachets », « complément à 300 mL ». */
    fun libelleQuantite(i: Ingredient, volume: Double, forme: FormeCarbonate): String {
        i.texte?.let { return it }
        if (i.complement) return "complément à ${volume.roundToInt()} mL"
        val q = quantite(i, volume, forme) ?: return "—"
        return when (i.unite) {
            "mL" -> "${q.roundToInt()} mL"
            "sachets" -> "≈ ${nombreSansZero((q * 2).roundToInt() / 2.0)} sachets"
            else -> "${nombreSansZero(q)} ${i.unite}"
        }
    }

    /**
     * La séance au chrono : le révélateur de la recette, l'arrêt à l'eau, le
     * fixateur au thiosulfate, le lavage et le mouillant.
     */
    fun sequence(r: RecetteMaison): SequenceLabo = SequenceLabo(
        "Film — ${r.nom}",
        listOf(
            Etape("Révélateur", (r.tempsDepart ?: 0.0) * 60, r.agitation, r.temps),
            Etape("Arrêt à l'eau", 45.0, Agitations.CONTINUE, "Remplir d'eau à la température du bain, agiter 15 s, vider — deux fois."),
            Etape("Fixateur", 10 * 60.0, Agitations.ILFORD, "Au thiosulfate : deux fois le temps de clarification du test de l'amorce. Ajuste la durée."),
            Etape("Lavage", 10 * 60.0, null, "Eau courante : plus longtemps qu'avec un fixateur rapide."),
            Etape("Mouillant", 30.0, null, "Une goutte d'agent mouillant dans le dernier rinçage.")
        ),
        source = r.sources.firstOrNull(),
        note = "Recette maison : les temps sont des points de départ. Note le résultat au carnet."
    )
}
