package fr.cellule.app.ecrans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.Corps
import fr.cellule.app.DepotPronostics
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.core.BandeEssai
import fr.cellule.core.Calculateur
import fr.cellule.core.Compensation
import fr.cellule.core.Correction
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.Dilution
import fr.cellule.core.DilutionPubliee
import fr.cellule.core.DilutionsPubliees
import fr.cellule.core.Duree
import fr.cellule.core.LoiIlford
import fr.cellule.core.Push
import fr.cellule.core.Reciprocite
import fr.cellule.core.Reciprocites
import fr.cellule.core.RegleIlford
import fr.cellule.core.Sources
import fr.cellule.core.TablePush
import fr.cellule.core.TableTemperature
import fr.cellule.core.Temperatures
import fr.cellule.core.TirageDiaphs
import fr.cellule.core.TirageNote
import fr.cellule.core.libelleCorrection
import fr.cellule.core.libelleDiaphs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Les cinq calculateurs du Labo. Chacun suit le même geste : les données, ton
 * estimation, puis le calcul — et seulement alors la courbe, le raisonnement
 * et la fiche du fabricant.
 */

/** Graduations « rondes » de 0 à au moins [max]. */
private fun graduations(max: Double): List<Double> {
    val pas = listOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0).first { max / it <= 5 }
    return (0..ceil(max / pas).toInt()).map { it * pas }
}

private fun pourcent(x: Double): String = "${(x * 100).roundToInt()} %"

/** Un temps de développement à 5 s près : la précision au-delà serait fausse. */
private fun temps(secondes: Double): String = Duree.libelle((secondes / 5).roundToInt() * 5.0)

/** Une pose ou un temps d'agrandisseur : au dixième sous 10 s, à la seconde au-delà. */
private fun pose(secondes: Double): String = when {
    secondes < 10 -> fmt(secondes, 1) + " s"
    secondes < 60 -> "${secondes.roundToInt()} s"
    else -> Duree.libelle(secondes)
}

/* ── Temps et température ─────────────────────────────────────────────────── */

@Composable
fun CalculTemperature(pronostics: DepotPronostics, versChrono: (Double) -> Unit, versCarnet: (DeveloppementNote) -> Unit, versFiche: (String) -> Unit) {
    var compensation by remember { mutableStateOf<Compensation>(RegleIlford) }
    var base by remember { mutableStateOf("6:00") }
    var temperature by remember { mutableStateOf(23f) }
    val t20 = Duree.lire(base)?.takeIf { it > 0 }
    val t = temperature.toDouble()

    Carte(
        titre = "Temps et température",
        sousTitre = "Ta fiche donne un temps à 20 °C ; ton révélateur n'y est pas. Combien de temps ?"
    ) {
        Deroulant(
            intitule = "D'après",
            options = Temperatures.COMPENSATIONS,
            selection = compensation,
            libelle = { it.intitule },
            modifier = Modifier.fillMaxWidth()
        ) {
            compensation = it
            if (it is TableTemperature) base = Duree.chrono(it.t20 * 60)
        }
        Spacer(Modifier.height(Interligne))
        Champ(base, "Temps à 20 °C", Modifier.fillMaxWidth(), indication = "6:30 ou 6,5") { base = it }
        Spacer(Modifier.height(Interligne))
        Text("Révélateur à ${fmt(t, 1)} °C", style = Corps, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = temperature, onValueChange = { temperature = (it * 2).roundToInt() / 2f }, valueRange = 16f..26f)
        (compensation as? TableTemperature)?.let {
            Text("Table publiée : ${it.note}, de ${fmt(it.plage.start, 0)} à ${fmt(it.plage.endInclusive, 0)} °C.", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Pari(
        cle = listOf(compensation.intitule, base, temperature),
        calculateur = Calculateur.TEMPERATURE,
        calcule = t20?.let { Temperatures.temps(it, t, compensation) },
        question = "À ${fmt(t, 1)} °C, combien de temps ?",
        indication = "par exemple 5:30",
        lire = Duree::lire,
        afficher = ::temps,
        contexte = "${t20?.let { temps(it) } ?: ""} à 20 °C → ${fmt(t, 1)} °C",
        pronostics = pronostics
    ) { estime ->
        ExplicationTemperature(compensation, t20 ?: return@Pari, t, versChrono, versFiche) { resultat ->
            val table = compensation as? TableTemperature
            versCarnet(
                DeveloppementNote(
                    identifiant = 0, date = "",
                    film = table?.film.orEmpty(),
                    revelateur = table?.revelateur.orEmpty(),
                    temperature = t,
                    tempsCalcule = resultat,
                    estimation = estime
                )
            )
        }
    }
}

@Composable
private fun ExplicationTemperature(
    c: Compensation, t20: Double, t: Double,
    versChrono: (Double) -> Unit, versFiche: (String) -> Unit, versCarnet: (Double) -> Unit
) {
    val resultat = t20 * c.facteur(t)
    val aPlat = t20 * (1 - c.tauxParDegre * (t - 20))
    val max = t20 * c.facteur(16.0) / 60
    Carte(titre = "Pourquoi une courbe", sousTitre = c.intitule) {
        Graphique(
            series = listOf(
                Serie((0..40).map { 16.0 + it * 0.25 }.map { it to t20 * c.facteur(it) / 60 }, MaterialTheme.colorScheme.primary),
                Serie(
                    listOf(16.0, 26.0).map { it to t20 * (1 - c.tauxParDegre * (it - 20)) / 60 },
                    MaterialTheme.colorScheme.onSurfaceVariant, pointilles = true, epaisseur = 1.5f
                )
            ),
            x = 16.0..26.0,
            y = 0.0..graduations(max).last(),
            graduationsX = listOf(16, 18, 20, 22, 24, 26).map { it.toDouble() to "$it°" },
            graduationsY = graduations(max).drop(1).map { it to fmt(it, if (it % 1 == 0.0) 0 else 1) + "′" },
            reperes = c.reperes.map { (temp, f) -> temp to t20 * f / 60 },
            curseur = t to resultat / 60
        )
        Text(
            "Trait plein : la courbe du fabricant. Pointillés : le même taux appliqué à plat. Cercles : les points qu'il publie.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Ligne("Facteur à ${fmt(t, 1)} °C", facteur(c.facteur(t)))
        Ligne("Taux de cette courbe", "${pourcent(c.tauxParDegre)} par degré", "de plus par degré en moins")
        if (t !in c.plage) {
            BandeauEtat("Hors des points publiés (${fmt(c.plage.start, 0)}–${fmt(c.plage.endInclusive, 0)} °C) : tendance prolongée, à vérifier.", alerte = true)
        }
        if (c is TableTemperature && resultat < Temperatures.MINIMUM_UNIFORME * 60) {
            BandeauEtat("Kodak : en cuve, moins de 5 min risquent un développement irrégulier.", alerte = true)
        }
        Depliable {
            Paragraphe(
                "Le révélateur est une réaction chimique : chaque degré gagné l'accélère d'un même facteur. " +
                    "Le temps se divise donc, degré après degré, par le même nombre — il ne perd pas le même nombre de secondes. " +
                    "D'où une courbe, raide à froid et plus douce à chaud."
            )
            Paragraphe(
                "Appliqué à plat, le même taux donnerait ${temps(aPlat)} au lieu de ${temps(resultat)}. " +
                    "Près de 20 °C, la différence est faible ; elle grandit à mesure qu'on s'en éloigne."
            )
            when (c) {
                RegleIlford -> {
                    Paragraphe(
                        "Ilford écrit « 10 % par degré ». Ses propres exemples — 6 min à 20 °C deviennent 4½ min à 23 °C et 9 min à 16 °C — " +
                            "montrent que les 10 % se composent : additionnés, ils donneraient 4 min 12 et 8 min 24."
                    )
                    LigneSourceEtFiche(c.source, "ID-11", versFiche)
                    val d = Temperatures.DELTA_3200_ID11
                    Paragraphe(
                        "La règle reste un repère : quand une table existe, elle prime. La Delta 3200 en ID-11 à EI 3200 passe de 10½ min à 20 °C " +
                            "à 9 min à 24 °C, là où la règle donnerait ${temps(d.t20 * 60 * RegleIlford.facteur(24.0))}."
                    )
                    LigneSourceEtFiche(d.source, "Ilford Delta 3200", versFiche)
                }
                is TableTemperature -> {
                    val pur = Temperatures.TABLES.firstOrNull { it.film == c.film && it.revelateur == c.revelateur.removeSuffix(" 1+1") }
                    if (pur != null && pur != c) {
                        Paragraphe(
                            "Dilué, le révélateur craint moins la température : ${pourcent(c.tauxParDegre)} par degré ici, " +
                                "contre ${pourcent(pur.tauxParDegre)} pour le ${pur.revelateur} pur dans la même fiche."
                        )
                    }
                    LigneSourceEtFiche(c.source, c.revelateur, versFiche)
                }
                else -> LigneSource(c.source)
            }
        }
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            BoutonPlat("Chrono", modifier = Modifier.weight(1f)) { versChrono((resultat / 5).roundToInt() * 5.0) }
            BoutonPlat("Verser au carnet", modifier = Modifier.weight(1f)) { versCarnet(resultat) }
        }
    }
}

/* ── Push et pull ─────────────────────────────────────────────────────────── */

@Composable
fun CalculPush(pronostics: DepotPronostics, versChrono: (Double) -> Unit, versCarnet: (DeveloppementNote) -> Unit, versFiche: (String) -> Unit) {
    var film by remember { mutableStateOf(Push.FILMS.first()) }
    val tables = Push.TABLES.filter { it.film == film }
    var table by remember(film) { mutableStateOf(tables.first()) }
    var ei by remember(table) { mutableStateOf(table.indices.firstOrNull { it > table.iso } ?: table.iso) }

    Carte(
        titre = "Push et pull",
        sousTitre = "Tu as exposé à un autre indice que la sensibilité nominale : quel temps de développement ?"
    ) {
        Deroulant("Film", Push.FILMS, film, { it }, Modifier.fillMaxWidth()) { film = it }
        Spacer(Modifier.height(Interligne))
        Deroulant("Révélateur", tables, table, { it.revelateur }, Modifier.fillMaxWidth()) { table = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Indice d'exposition (EI)")
        Spacer(Modifier.height(4.dp))
        ChoixSegmente(table.indices, ei, { "$it" }) { ei = it }
        Spacer(Modifier.height(6.dp))
        Text(
            "Temps normal à EI ${table.iso} : ${temps(table.normal * 60)} à 20 °C, ${table.note}.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val verser = { estime: Double? ->
        versCarnet(
            DeveloppementNote(
                identifiant = 0, date = "",
                film = table.film, ei = ei, revelateur = table.revelateur, temperature = 20.0,
                tempsCalcule = table.minutes[ei]?.let { it * 60 }, estimation = estime
            )
        )
    }
    if (ei == table.iso) {
        ExplicationPush(table, ei, versChrono, versFiche) { verser(null) }
        return
    }
    Pari(
        cle = listOf(table.intitule, ei),
        calculateur = Calculateur.PUSH_PULL,
        calcule = table.minutes[ei]?.let { it * 60 },
        question = "À EI $ei, combien de temps ?",
        indication = "par exemple 9:30",
        lire = Duree::lire,
        afficher = ::temps,
        contexte = "${table.intitule} · EI $ei",
        pronostics = pronostics
    ) { estime ->
        ExplicationPush(table, ei, versChrono, versFiche) { verser(estime) }
    }
}

@Composable
private fun ExplicationPush(table: TablePush, ei: Int, versChrono: (Double) -> Unit, versFiche: (String) -> Unit, versCarnet: () -> Unit) {
    val minutes = table.minutes.getValue(ei)
    val d = table.diaphs(ei)
    val memeFilm = Push.TABLES.filter { it.film == table.film }
    val indices = memeFilm.flatMap { it.indices }.distinct().sorted()
    val max = memeFilm.maxOf { it.minutes.values.max() }
    Carte(titre = "Ce que fait le push", sousTitre = table.intitule) {
        Ligne("Temps publié à EI $ei", temps(minutes * 60), accent = true)
        Ligne("Écart d'exposition", if (d == 0.0) "nominal" else libelleDiaphs(d) + " diaph")
        Ligne("Par rapport au temps normal", facteur(minutes / table.normal))
        Spacer(Modifier.height(6.dp))
        Graphique(
            series = memeFilm.filter { it != table }.map { t ->
                Serie(t.indices.map { t.diaphs(it) to t.minutes.getValue(it) }, MaterialTheme.colorScheme.outlineVariant, epaisseur = 1.5f)
            } + Serie(table.indices.map { table.diaphs(it) to table.minutes.getValue(it) }, MaterialTheme.colorScheme.primary),
            x = log2(indices.first().toDouble() / table.iso) - 0.3..log2(indices.last().toDouble() / table.iso) + 0.3,
            y = 0.0..graduations(max).last(),
            graduationsX = indices.map { log2(it.toDouble() / table.iso) to "$it" },
            graduationsY = graduations(max).drop(1).map { it to fmt(it, 0) + "′" },
            reperes = table.indices.map { table.diaphs(it) to table.minutes.getValue(it) },
            curseur = d to minutes
        )
        Text(
            "En couleur : ${table.revelateur}. En gris : les autres révélateurs de la même fiche pour ce film.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Depliable {
            if (d > 0) {
                Paragraphe(
                    "Sous-exposer, c'est inscrire moins de lumière partout. Prolonger le développement fait surtout monter les zones " +
                        "qui en ont reçu — tons moyens et hautes lumières — bien plus que les ombres, où presque rien ne s'est inscrit. " +
                        "Le contraste monte, le grain aussi ; le détail absent des ombres ne revient pas."
                )
            } else if (d < 0) {
                Paragraphe(
                    "Surexposer puis développer moins : les ombres gagnent de la matière, les hautes lumières sont retenues, " +
                        "le contraste baisse et le grain reste fin."
                )
            }
            val facteurs = memeFilm.mapNotNull { it.facteur(ei) }
            if (d != 0.0 && facteurs.size > 1) {
                Paragraphe(
                    "Pas de facteur universel : pour ${table.film} à EI $ei, selon le révélateur, le fabricant multiplie le temps " +
                        "par ${facteur(facteurs.min())} à ${facteur(facteurs.max())}. Seule la table fait foi."
                )
            }
            if (table.source == Sources.KODAK_TRIX || table.source == Sources.KODAK_D76) {
                Paragraphe(
                    "Kodak : un diaph de sous-exposition se développe au temps normal, la latitude du film l'encaisse (légère perte dans les ombres). " +
                        "À deux diaphs, il faut pousser : contraste et grain augmentent, les ombres perdent encore du détail."
                )
            } else if (table.source == Sources.ILFORD_HP5) {
                Paragraphe(
                    "Ilford : la HP5 Plus donne ses meilleurs résultats à EI 400, et une bonne qualité d'image jusqu'à EI 3200 avec un développement prolongé. " +
                        "Cette plage repose sur une évaluation pratique, pas sur la sensibilité ISO normalisée."
                )
            }
            LigneSourceEtFiche(table.source, table.revelateur, versFiche)
        }
        Spacer(Modifier.height(6.dp))
        LigneBoutons {
            BoutonPlat("Chrono", modifier = Modifier.weight(1f)) { versChrono(minutes * 60) }
            BoutonPlat("Verser au carnet", modifier = Modifier.weight(1f)) { versCarnet() }
        }
    }
}

/* ── Réciprocité ──────────────────────────────────────────────────────────── */

@Composable
fun CalculReciprocite(pronostics: DepotPronostics, versFiche: (String) -> Unit) {
    var film by remember { mutableStateOf(Reciprocites.LISTE.first()) }
    var saisie by remember { mutableStateOf("10") }
    val mesure = Duree.lireSecondes(saisie)?.takeIf { it > 0 }

    Carte(
        titre = "Réciprocité",
        sousTitre = "Pose longue : la cellule donne un temps, le film en demande davantage."
    ) {
        Deroulant("Film", Reciprocites.LISTE, film, { it.film }, Modifier.fillMaxWidth()) { film = it }
        Spacer(Modifier.height(Interligne))
        Champ(saisie, "Pose mesurée (s)", Modifier.fillMaxWidth(), indication = "8, 30, 1:30…") { saisie = it }
    }

    val corrige = mesure?.let { film.corrige(it) }
    if (mesure != null && corrige == null) {
        Carte {
            BandeauEtat("La fiche s'arrête à ${pose(film.maximum ?: 0.0)} : au-delà, rien n'est publié. Fais un essai.", alerte = true)
            LigneSourceEtFiche(film.source, film.film, versFiche)
        }
        return
    }
    Pari(
        cle = listOf(film.film, saisie),
        calculateur = Calculateur.RECIPROCITE,
        calcule = corrige,
        question = "Quelle pose donner réellement ?",
        indication = "en secondes, ou 2:30",
        lire = Duree::lireSecondes,
        afficher = ::pose,
        contexte = "${film.film} · ${mesure?.let { pose(it) } ?: ""} mesurées",
        pronostics = pronostics
    ) {
        ExplicationReciprocite(film, mesure ?: return@Pari, versFiche)
    }
}

@Composable
private fun ExplicationReciprocite(film: Reciprocite, mesure: Double, versFiche: (String) -> Unit) {
    val corrige = film.corrige(mesure) ?: return
    Carte(titre = "Pourquoi le film ralentit", sousTitre = film.film) {
        Ligne("Pose à donner", pose(corrige), accent = true)
        Ligne("Correction", libelleDiaphs(log2(corrige / mesure)).let { if (it == "juste") "aucune" else "$it diaph" })
        film.developpement(mesure)?.let { Ligne("Développement", it, "Kodak, ligne la plus proche du tableau") }
        Spacer(Modifier.height(6.dp))
        Graphique(
            series = Reciprocites.LISTE.filter { it != film }.map { r ->
                Serie(courbeReciprocite(r), MaterialTheme.colorScheme.outlineVariant, epaisseur = 1.5f)
            } + Serie(courbeReciprocite(film), MaterialTheme.colorScheme.primary),
            x = 0.0..3.0,
            y = 0.0..4.0,
            graduationsX = listOf(0.0 to "1 s", 1.0 to "10 s", 2.0 to "100 s", 3.0 to "1000 s"),
            graduationsY = listOf(1.0, 2.0, 3.0, 4.0).map { it to "+${fmt(it, 0)}" },
            curseur = if (mesure >= 1) log10(mesure) to log2(corrige / mesure) else null
        )
        Text(
            "Diaphs à ajouter selon la pose mesurée. En couleur : ${film.film} ; en gris : les autres films.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Depliable {
            Paragraphe(
                "Pour qu'un grain d'argent devienne développable, plusieurs photons doivent l'atteindre en peu de temps. " +
                    "Sous une lumière faible, ils arrivent trop espacés et une partie de leur effet se perd. " +
                    "La perte grandit avec la durée : la correction croît plus vite que la pose."
            )
            when (film) {
                is LoiIlford -> Paragraphe(
                    "Ilford résume sa courbe par une formule : pose à donner = pose mesurée ^ ${film.exposant.toString().replace('.', ',')}. " +
                        "Rien à corriger jusqu'à ${if (film.seuil < 1) "½ s" else "1 s"}."
                )
                else -> Paragraphe(
                    "Kodak publie trois points — 1 s → 2 s, 10 s → 50 s, 100 s → 1200 s — et renvoie à ses graphiques entre eux : " +
                        "des droites en échelles logarithmiques, que suit ce calcul. Il raccourcit aussi le développement, de 10 à 30 %. " +
                        "Au-delà de 100 s, la fiche ne dit rien."
                )
            }
            val autres = Reciprocites.LISTE.filter { it != film }.mapNotNull { r -> r.corrige(mesure)?.let { r.film to it } }
            if (autres.isNotEmpty() && mesure >= 2) {
                Paragraphe(
                    "Chaque film a sa courbe : pour ${pose(mesure)} mesurées, " +
                        autres.joinToString(", ") { (nom, c) -> "$nom demande ${pose(c)}" } + "."
                )
            }
            LigneSourceEtFiche(film.source, film.film, versFiche)
        }
    }
}

private fun courbeReciprocite(r: Reciprocite): List<Pair<Double, Double>> =
    (0..60).map { it * 0.05 }.mapNotNull { x ->
        val m = Math.pow(10.0, x)
        r.corrige(m)?.let { x to log2(it / m) }
    }

/* ── Dilutions ────────────────────────────────────────────────────────────── */

@Composable
fun CalculDilution(pronostics: DepotPronostics, versFiche: (String) -> Unit) {
    var produit by remember { mutableStateOf<DilutionPubliee?>(DilutionsPubliees.LISTE.first()) }
    var saisieDilution by remember { mutableStateOf(DilutionsPubliees.LISTE.first().dilution.libelle) }
    var saisieVolume by remember { mutableStateOf("500") }
    val dilution = Dilution.lire(saisieDilution)
    val volume = saisieVolume.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it > 0 }

    Carte(
        titre = "Dilutions",
        sousTitre = "Un volume de solution et un rapport « 1+N » : combien de concentré, combien d'eau ?"
    ) {
        Deroulant(
            intitule = "Produit",
            options = listOf<DilutionPubliee?>(null) + DilutionsPubliees.LISTE,
            selection = produit,
            libelle = { it?.let { p -> "${p.intitule} · ${p.usage}" } ?: "Autre produit" },
            modifier = Modifier.fillMaxWidth()
        ) {
            produit = it
            if (it != null) saisieDilution = it.dilution.libelle
        }
        Spacer(Modifier.height(Interligne))
        Row {
            Champ(saisieDilution, "Dilution", Modifier.weight(1f), indication = "1+9") { saisieDilution = it }
            Spacer(Modifier.width(Interligne))
            Champ(saisieVolume, "Volume (mL)", Modifier.weight(1f), indication = "500") { saisieVolume = it }
        }
        if (saisieDilution.isNotBlank() && dilution == null) {
            Spacer(Modifier.height(6.dp))
            BandeauEtat("Écris la dilution sous la forme 1+9 ou 1:31.", alerte = true)
        }
    }

    Pari(
        cle = listOf(saisieDilution, saisieVolume),
        calculateur = Calculateur.DILUTION,
        calcule = if (dilution != null && volume != null) dilution.concentre(volume) else null,
        question = "Combien de mL de concentré ?",
        indication = "en mL",
        lire = { it.lowercase().replace("ml", "").replace(',', '.').trim().toDoubleOrNull() },
        afficher = { fmt(it, if (it < 10) 1 else 0) + " mL" },
        contexte = "${dilution?.libelle ?: ""} · ${volume?.let { fmt(it, 0) } ?: ""} mL",
        pronostics = pronostics
    ) {
        ExplicationDilution(dilution ?: return@Pari, volume ?: return@Pari, produit?.takeIf { it.dilution == dilution }, versFiche)
    }
}

@Composable
private fun ExplicationDilution(d: Dilution, volume: Double, produit: DilutionPubliee?, versFiche: (String) -> Unit) {
    val parts = d.partsTotales
    val c = d.concentre(volume)
    Carte(titre = "Lire « ${d.libelle} »", sousTitre = produit?.let { "${it.produit} · ${it.usage}" }) {
        Ligne("Concentré", fmt(c, if (c < 10) 1 else 0) + " mL", accent = true)
        Ligne("Eau", fmt(d.eau(volume), 0) + " mL")
        Ligne("Parts en tout", nombre(parts), "une de concentré, ${nombre(d.partsEau)} d'eau")
        Spacer(Modifier.height(6.dp))
        BarreParts(parts)
        Spacer(Modifier.height(6.dp))
        Depliable {
            Paragraphe(
                "« ${d.libelle} » se lit : une part de concentré pour ${nombre(d.partsEau)} parts d'eau, soit ${nombre(parts)} parts. " +
                    "Chacune vaut ${fmt(volume, 0)} ÷ ${nombre(parts)} = ${fmt(volume / parts, 1)} mL — le concentré fait ${fmt(100 / parts, 1)} % de la solution."
            )
            if (d.partsEau >= 1) {
                val faux = volume / d.partsEau
                Paragraphe(
                    "L'erreur classique divise par ${nombre(d.partsEau)} au lieu de ${nombre(parts)} : ${fmt(faux, 1)} mL, " +
                        "soit ${(((faux / c) - 1) * 100).roundToInt()} % de concentré en trop — un révélateur plus actif que prévu."
                )
            }
            Paragraphe(
                "Mesure le concentré avec soin, à la seringue ou à l'éprouvette fine : c'est lui qui fixe l'activité du bain. " +
                    "Quelques mL d'erreur sur l'eau ne comptent pas ; sur un petit volume de concentré, ils font vite 10 ou 20 %."
            )
            produit?.let {
                if (it.note.isNotBlank()) Paragraphe(it.note)
                LigneSourceEtFiche(it.source, it.produit, versFiche)
            }
        }
    }
}

private fun nombre(x: Double): String = if (x % 1 == 0.0) x.toLong().toString() else fmt(x, 1)

/** Les N+1 parts de la solution ; au-delà de 40, la part du concentré à l'échelle. */
@Composable
private fun BarreParts(parts: Double) {
    val accent = MaterialTheme.colorScheme.primary
    val reste = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val n = parts.roundToInt()
        if (n in 1..40) {
            val ecart = 2.dp.toPx()
            val l = (size.width - ecart * (n - 1)) / n
            (0 until n).forEach { i ->
                drawRoundRect(
                    if (i == 0) accent else reste,
                    topLeft = Offset(i * (l + ecart), 0f),
                    size = Size(l, size.height),
                    cornerRadius = CornerRadius(3.dp.toPx())
                )
            }
        } else {
            drawRoundRect(reste, size = size, cornerRadius = CornerRadius(3.dp.toPx()))
            drawRoundRect(accent, size = Size(max(size.width / parts.toFloat(), 2f), size.height), cornerRadius = CornerRadius(3.dp.toPx()))
        }
    }
}

/* ── Tirage en diaphs ─────────────────────────────────────────────────────── */

private enum class ModeTirage(val libelle: String) { CORRECTION("Correction"), BANDE("Bande d'essai") }

private val PAS_TIRAGE = listOf(1.0 / 3, 0.5, 1.0)

private fun libellePas(p: Double): String = when (p) {
    0.5 -> "½"
    1.0 -> "1"
    else -> "⅓"
}

private fun abs(x: Double) = kotlin.math.abs(x)

@Composable
fun CalculTirage(pronostics: DepotPronostics, versTirage: (TirageNote) -> Unit) {
    var mode by remember { mutableStateOf(ModeTirage.CORRECTION) }
    Column(Modifier.fillMaxWidth()) {
        Carte(
            titre = "Tirage en diaphs",
            sousTitre = "Le papier répond en diaphs, comme le négatif : on compte les corrections en diaphs, puis on les convertit en secondes."
        ) {
            ChoixSegmente(ModeTirage.entries.toList(), mode, { it.libelle }) { mode = it }
        }
        when (mode) {
            ModeTirage.CORRECTION -> CorrectionTirage(pronostics, versTirage)
            ModeTirage.BANDE -> Bande(pronostics)
        }
    }
}

@Composable
private fun CorrectionTirage(pronostics: DepotPronostics, versTirage: (TirageNote) -> Unit) {
    var saisie by remember { mutableStateOf("10") }
    var pas by remember { mutableStateOf(PAS_TIRAGE.first()) }
    var crans by remember { mutableStateOf(1f) }
    val base = Duree.lireSecondes(saisie)?.takeIf { it > 0 }
    val delta = crans.roundToInt() * pas

    Carte {
        Champ(saisie, "Temps de base (s)", Modifier.fillMaxWidth(), indication = "10") { saisie = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Pas")
        Spacer(Modifier.height(4.dp))
        ChoixSegmente(PAS_TIRAGE, pas, { libellePas(it) + " diaph" }) { pas = it; crans = 1f }
        Spacer(Modifier.height(Interligne))
        Text("Correction : ${libelleCorrection(delta).let { if (it == "juste") "aucune" else "$it diaph" }}", style = Corps, color = MaterialTheme.colorScheme.onSurface)
        val maxCrans = (2 / pas).roundToInt().toFloat()
        Slider(
            value = crans,
            onValueChange = { crans = it.roundToInt().toFloat() },
            valueRange = -maxCrans..maxCrans,
            steps = (2 * maxCrans).toInt() - 1
        )
    }

    Pari(
        cle = listOf(saisie, pas, crans),
        calculateur = Calculateur.TIRAGE,
        calcule = base?.takeIf { delta != 0.0 }?.let { TirageDiaphs.temps(it, delta) },
        question = "Quel temps total pour cette zone ?",
        indication = "en secondes",
        lire = Duree::lireSecondes,
        afficher = ::pose,
        contexte = "${base?.let { pose(it) } ?: ""} ${libelleCorrection(delta)} diaph",
        pronostics = pronostics
    ) {
        val b = base ?: return@Pari
        ExplicationCorrection(b, delta, pas) {
            versTirage(TirageNote(0, "", base = b, corrections = listOf(Correction("", delta))))
        }
    }
}

@Composable
private fun ExplicationCorrection(base: Double, delta: Double, pas: Double, versTirage: () -> Unit) {
    val total = TirageDiaphs.temps(base, delta)
    Carte(titre = "La correction en secondes") {
        Ligne("Temps total de la zone", pose(total), accent = true)
        if (delta > 0) {
            Ligne("Brûlage", "+ " + pose(TirageDiaphs.brulage(base, delta)), "après l'exposition de base, en cachant le reste")
        } else {
            Ligne("Masquage", pose(TirageDiaphs.masquage(base, -delta)), "pendant l'exposition de base, sur la zone seulement")
        }
        Separateur()
        Etiquette("L'échelle autour de ${pose(base)}")
        (-3..3).map { it * pas }.forEach { d ->
            Ligne(
                if (d == 0.0) "Base" else libelleCorrection(d) + " diaph",
                pose(TirageDiaphs.temps(base, d)),
                accent = abs(d - delta) < 1e-9
            )
        }
        Spacer(Modifier.height(6.dp))
        Depliable {
            Paragraphe(
                "Un diaph double la lumière, donc le temps : un tiers le multiplie par 1,26, un demi par 1,41. " +
                    "Compter en diaphs rend une correction indépendante du temps de base : « +⅓ dans le ciel » vaut sur un tirage de 8 s comme de 40 s."
            )
            Paragraphe(
                if (delta > 0) "Brûler de ${libelleCorrection(delta).removePrefix("+")} diaph, c'est donner à la zone ${pose(total)} en tout : " +
                    "elle a déjà reçu la base, il reste ${pose(total - base)} à ajouter."
                else "Masquer de ${libelleCorrection(-delta).removePrefix("+")} diaph, c'est ne donner à la zone que ${pose(total)} : " +
                    "tu la caches pendant ${pose(base - total)} de la base."
            )
            Paragraphe("Même règle à l'agrandisseur qu'à la prise de vue : fermer l'objectif d'un diaph double le temps.")
        }
        Spacer(Modifier.height(6.dp))
        BoutonPlat("Verser au carnet de tirage", modifier = Modifier.fillMaxWidth()) { versTirage() }
    }
}

@Composable
private fun Bande(pronostics: DepotPronostics) {
    var saisie by remember { mutableStateOf("4") }
    var pas by remember { mutableStateOf(0.5) }
    var nombre by remember { mutableStateOf(7) }
    val premier = Duree.lireSecondes(saisie)?.takeIf { it > 0 }

    Carte {
        Champ(saisie, "Première bande (s)", Modifier.fillMaxWidth(), indication = "4") { saisie = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Pas entre deux bandes")
        Spacer(Modifier.height(4.dp))
        ChoixSegmente(PAS_TIRAGE, pas, { libellePas(it) + " diaph" }) { pas = it }
        Spacer(Modifier.height(Interligne))
        Etiquette("Nombre de bandes")
        Spacer(Modifier.height(4.dp))
        ChoixSegmente(listOf(5, 7, 9), nombre, { "$it" }) { nombre = it }
    }

    Pari(
        cle = listOf(saisie, pas, nombre),
        calculateur = Calculateur.TIRAGE,
        calcule = premier?.let { BandeEssai.diaphs(it, pas, nombre).last().total },
        question = "Combien de temps en tout pour la dernière bande ?",
        indication = "en secondes",
        lire = Duree::lireSecondes,
        afficher = ::pose,
        contexte = "bande de $nombre · pas ${libellePas(pas)}",
        pronostics = pronostics
    ) {
        ExplicationBande(premier ?: return@Pari, pas, nombre)
    }
}

@Composable
private fun ExplicationBande(premier: Double, pas: Double, nombre: Int) {
    val bandes = BandeEssai.diaphs(premier, pas, nombre)
    val lineaire = BandeEssai.lineaire(premier, premier, nombre)
    val echelle = max(bandes.last().diaphs, lineaire.last().diaphs)
    Carte(titre = "La bande, pas à pas") {
        Etiquette("En diaphs : des pas égaux")
        Spacer(Modifier.height(4.dp))
        Nuancier(bandes.map { it.diaphs / echelle }, bandes.map { pose(it.total) })
        Spacer(Modifier.height(Interligne))
        Etiquette("En secondes (${pose(premier)}, ${pose(2 * premier)}, ${pose(3 * premier)}…) : des pas qui se tassent")
        Spacer(Modifier.height(4.dp))
        Nuancier(lineaire.map { it.diaphs / echelle }, lineaire.map { pose(it.total) })
        Spacer(Modifier.height(Interligne))
        Separateur()
        Etiquette("Au minuteur, en découvrant une bande à chaque fois")
        bandes.forEach { b ->
            Ligne(
                "Bande ${b.rang + 1}",
                if (b.rang == 0) pose(b.ajout) else "+ " + pose(b.ajout),
                "total ${pose(b.total)} · ${if (b.rang == 0) "base" else libelleCorrection(b.diaphs) + " diaph"}"
            )
        }
        Spacer(Modifier.height(6.dp))
        Depliable {
            Paragraphe(
                "La densité du papier suit le logarithme de la lumière reçue. Des bandes à ${pose(premier)}, ${pose(2 * premier)}, ${pose(3 * premier)}… " +
                    "sautent d'un diaph entier au début puis ne gagnent plus qu'un tiers à la fin : les premières se distinguent mal des suivantes, " +
                    "les dernières se confondent. À pas constant en diaphs, chaque bande ajoute la même quantité de gris."
            )
        }
    }
}

/** Des cases de gris, du clair au sombre selon la part d'exposition, légendées de leur temps. */
@Composable
private fun Nuancier(parts: List<Double>, legendes: List<String>) {
    val clair = Color(0xFFF2EFE8)
    val sombre = Color(0xFF1A1917)
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val n = parts.size
        val l = size.width / n
        parts.forEachIndexed { i, p ->
            drawRect(lerp(clair, sombre, (0.15 + 0.8 * p).toFloat().coerceIn(0f, 1f)), Offset(i * l, 0f), Size(l - 1f, size.height))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        legendes.forEach {
            Text(it, style = Detail.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
        }
    }
}
