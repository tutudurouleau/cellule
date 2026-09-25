package fr.cellule.app.ecrans

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import fr.cellule.app.Detail
import fr.cellule.app.Interligne
import fr.cellule.app.TitreCarte
import fr.cellule.core.LigneRevelateur
import fr.cellule.core.RegleIlford
import fr.cellule.core.Source
import fr.cellule.core.Sources
import fr.cellule.core.TablesLabo
import fr.cellule.core.TypeFiche

/**
 * Le développement en six tables, à lire dans l'ordre la première fois :
 * les bains, les mots, les révélateurs, la température, les dilutions, le
 * push. Chaque chiffre vient d'une fiche de fabricant déjà relue.
 *
 * [versFiche] n'existe que dans le Labo : il ouvre la fiche d'un produit.
 * Depuis l'onglet Tables, seules les sources s'affichent.
 */
@Composable
fun TablesDeveloppement(versFiche: ((String) -> Unit)? = null) {
    val source = @Composable { s: Source, produit: String? ->
        if (versFiche != null) LigneSourceEtFiche(s, produit, versFiche) else LigneSource(s)
    }
    val m = TablesLabo::minutes

    Carte(
        titre = "Le développement en six tables",
        sousTitre = "À lire dans l'ordre la première fois : chacune s'appuie sur la précédente. " +
            "Tous les chiffres viennent des fiches d'Ilford et de Kodak."
    ) {}

    /* ── 1. Les bains ─────────────────────────────────────────────────── */
    Carte(
        titre = "1 · Les cinq bains, dans l'ordre",
        sousTitre = "Un film se développe toujours dans cet ordre. Seul le premier bain change vraiment d'un film à l'autre : c'est lui que règlent toutes les tables qui suivent."
    ) {
        Tableau(
            listOf(Colonne("Bain", 0.9f), Colonne("Ilford", 1.25f), Colonne("Kodak", 1.1f)),
            TablesLabo.BAINS.mapIndexed { i, b ->
                RangTableau(listOf("${i + 1}. ${b.nom}", b.ilford, b.kodak), detail = b.role, accent = i == 0)
            },
            petit = true
        )
        NoteTable(
            "Arrêt, fixateur et lavage se font entre 18 et 24 °C, selon les deux fabricants. " +
                "Le révélateur, lui, se règle au degré près : c'est la table 4."
        )
        source(Sources.ILFORD_HP5, null)
        source(Sources.KODAK_TRIX, null)
    }

    /* ── 2. Les mots ──────────────────────────────────────────────────── */
    Carte(
        titre = "2 · Les mots du labo",
        sousTitre = "Ceux qu'on croise sur chaque fiche, et que les fiches n'expliquent jamais."
    ) {
        Tableau(
            listOf(Colonne("Mot", 1f)),
            TablesLabo.MOTS.map { RangTableau(listOf(it.mot), detail = it.sens) }
        )
    }

    /* ── 3. Les révélateurs ───────────────────────────────────────────── */
    Carte(
        titre = "3 · Un film, plusieurs révélateurs",
        sousTitre = "La même HP5 Plus, exposée à EI ${TablesLabo.EI_REFERENCE}, développée à 20 °C : chaque révélateur a son temps et son caractère. " +
            "Même révélateur, plus d'eau : plus long."
    ) {
        TableauRevelateurs(TablesLabo.REVELATEURS_ILFORD)
        NoteTable(
            "PERCEPTOL, le révélateur au grain le plus fin, n'y figure pas : Ilford ne le publie pas à EI 400 pour ce film, " +
                "mais pour une HP5 exposée à EI 250 — la finesse se paie en sensibilité."
        )
        Depliable("Les révélateurs d'autres marques") {
            Text(
                "Ilford donne aussi leurs temps dans la même fiche, « pour commodité » : c'est lui qui les publie, pas leur fabricant.",
                style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            TableauRevelateurs(TablesLabo.REVELATEURS_AUTRES)
        }
        source(Sources.ILFORD_HP5, TablesLabo.FILM_REFERENCE)
    }

    /* ── 4. La température ────────────────────────────────────────────── */
    Carte(
        titre = "4 · La température change le temps",
        sousTitre = "Au chaud, le révélateur travaille plus vite. Règle d'Ilford : chaque degré en moins allonge de 10 %, " +
            "chaque degré en plus raccourcit de 10 % — et ces 10 % se multiplient, ils ne s'additionnent pas."
    ) {
        Tableau(
            listOf(Colonne("Température", 1.1f), Colonne("Facteur", 0.9f, aDroite = true), Colonne("6 min deviennent", 1.3f, aDroite = true)),
            TablesLabo.REGLE_ILFORD.map {
                val exemple = it.temperature in TablesLabo.TEMPERATURES_EXEMPLE
                RangTableau(
                    listOf("${fmt(it.temperature, 0)} °C", TablesLabo.facteur(it.facteur), "${m(it.minutes)} min"),
                    detail = if (exemple && it.temperature != 20.0) "l'exemple écrit dans la fiche" else null,
                    accent = it.temperature == 20.0
                )
            }
        )
        Spacer(Modifier.height(Interligne))
        Graphique(
            series = listOf(
                Serie(
                    (0..32).map { 16.0 + it * 0.25 }.map { it to TablesLabo.TEMPS_EXEMPLE * RegleIlford.facteur(it) },
                    MaterialTheme.colorScheme.primary, nom = "10 % multipliés (règle Ilford)", principale = true
                ),
                Serie(
                    listOf(16.0, 24.0).map { it to TablesLabo.TEMPS_EXEMPLE * RegleIlford.lineaire(it) },
                    MaterialTheme.colorScheme.onSurfaceVariant, pointilles = true, epaisseur = 1.5f,
                    nom = "10 % additionnés (faux)"
                )
            ),
            x = 16.0..24.0,
            y = 0.0..10.0,
            graduationsX = (16..24 step 2).map { it.toDouble() to "$it°" },
            graduationsY = listOf(2.0, 4.0, 6.0, 8.0, 10.0).map { it to "${it.toInt()}′" },
            reperes = TablesLabo.TEMPERATURES_EXEMPLE.sorted().map { it to TablesLabo.TEMPS_EXEMPLE * RegleIlford.facteur(it) },
            titreY = "Temps pour 6 min à 20 °C",
            titreX = "Température du révélateur",
            lire = { t, min -> "${fmt(t, if (t % 1.0 == 0.0) 0 else 1)} °C → ${minutesLisibles(min)}" },
            lireCourt = ::minutesCourtes,
            lireAutre = { _, _, min -> "additionnés : ${minutesCourtes(min)}" },
            pas = 0.5
        )
        NoteTable(
            "Pour ton propre temps : multiplie-le par le facteur de la ligne. 8 min à 20 °C, révélateur à 22 °C : 8 × 0,83 ≈ 6 min 40. " +
                "Additionner les 10 % se tromperait d'autant plus qu'on s'éloigne de 20 °C : à 16 °C, 8 min 24 au lieu de 9 min."
        )
        source(Sources.ILFORD_ID11, "ID-11")

        Separateur()
        Text("Quand le fabricant publie sa propre table", style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Tri-X 400 en D-76 : la table de Kodak, et la règle d'Ilford partie du même temps à 20 °C.",
            style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Tableau(
            listOf(Colonne("Température", 1.1f), Colonne("Kodak publie", 1.1f, aDroite = true), Colonne("La règle donne", 1.1f, aDroite = true)),
            TablesLabo.KODAK_FACE_A_LA_REGLE.map {
                RangTableau(
                    listOf("${fmt(it.temperature, 0)} °C", "${m(it.publie)} min", "${m(it.regle)} min"),
                    accent = it.temperature == 20.0
                )
            }
        )
        Spacer(Modifier.height(Interligne))
        val kodak = TablesLabo.KODAK_FACE_A_LA_REGLE
        val t20Kodak = kodak.first { it.temperature == 20.0 }.publie
        Graphique(
            series = listOf(
                Serie(
                    (0..24).map { 18.0 + it * 0.25 }.map { it to t20Kodak * RegleIlford.facteur(it) },
                    MaterialTheme.colorScheme.onSurfaceVariant, pointilles = true, epaisseur = 1.5f,
                    nom = "règle Ilford"
                ),
                Serie(
                    kodak.map { it.temperature to it.publie },
                    MaterialTheme.colorScheme.primary, nom = "table Kodak", principale = true
                )
            ),
            x = 17.5..24.5,
            y = 0.0..10.0,
            graduationsX = listOf(18, 20, 22, 24).map { it.toDouble() to "$it°" },
            graduationsY = listOf(2.0, 4.0, 6.0, 8.0, 10.0).map { it to "${it.toInt()}′" },
            reperes = kodak.map { it.temperature to it.publie },
            titreY = "Tri-X 400 en D-76",
            titreX = "Température du révélateur",
            lire = { t, min -> "${fmt(t, 0)} °C → ${minutesLisibles(min)}" },
            lireCourt = ::minutesCourtes,
            lireAutre = { serie, _, min -> "${serie.nom} : ${minutesCourtes(min)}" },
            reperesSeulement = true
        )
        NoteTable(
            "La règle tombe à un quart de minute près. Mais quand une table existe, c'est elle qui fait foi : " +
                "prends le chiffre du fabricant, et garde la règle pour les couples qu'il ne publie pas."
        )
        source(Sources.KODAK_TRIX, "D-76")
    }

    /* ── 5. Les dilutions ─────────────────────────────────────────────── */
    Carte(
        titre = "5 · Lire une dilution",
        sousTitre = "« 1+9 », c'est une part de concentré et neuf parts d'eau : dix parts en tout. " +
            "Le concentré fait donc un dixième du mélange — pas un neuvième."
    ) {
        Tableau(
            listOf(Colonne("Dilution", 0.8f), Colonne("Concentré", 0.9f, aDroite = true), Colonne("Pour 500 mL", 1.5f, aDroite = true)),
            TablesLabo.DILUTIONS.map {
                RangTableau(
                    listOf(it.dilution.libelle, TablesLabo.pourcentConcentre(it.dilution), TablesLabo.pour(it.dilution)),
                    detail = it.produits.joinToString(", ")
                )
            }
        )
        NoteTable(
            "Dans « Pour 500 mL », le premier chiffre est le concentré, le second l'eau. Mesure le concentré avec soin : " +
                "c'est lui qui fixe l'activité du bain."
        )
        source(Sources.ILFORD_HP5, null)
    }

    /* ── 6. Pousser ───────────────────────────────────────────────────── */
    Carte(
        titre = "6 · Pousser un film",
        sousTitre = "HP5 Plus à 20 °C. Exposée à EI 800, elle a reçu un diaph de moins que prévu : on développe plus longtemps — " +
            "d'un facteur qui dépend du révélateur. Temps en minutes."
    ) {
        Tableau(
            listOf(Colonne("EI", 0.95f)) + TablesLabo.REVELATEURS_PUSH.map { (r, d) ->
                Colonne(if (d == "stock") r.removePrefix("ILFOTEC ") else "${r.removePrefix("ILFOTEC ")} $d", 1f, aDroite = true)
            },
            TablesLabo.PUSH.map { l ->
                RangTableau(
                    listOf(if (l.diaphs == 0) "${l.ei}" else "${l.ei} · +${l.diaphs}") + l.minutes.map { it?.let(m) ?: "—" },
                    accent = l.diaphs == 0
                )
            }
        )
        Spacer(Modifier.height(Interligne))
        val couleurs = listOf(
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        val eis = TablesLabo.PUSH.map { it.ei }
        Graphique(
            series = TablesLabo.REVELATEURS_PUSH.mapIndexed { i, (r, d) ->
                Serie(
                    TablesLabo.PUSH.mapNotNull { l -> l.minutes[i]?.let { l.diaphs.toDouble() to it } },
                    couleurs[i],
                    pointilles = i == 2,
                    nom = if (d == "stock") r.removePrefix("ILFOTEC ") else "${r.removePrefix("ILFOTEC ")} $d",
                    principale = i == 1
                )
            },
            x = -0.25..3.25,
            y = 0.0..22.0,
            graduationsX = TablesLabo.PUSH.map { it.diaphs.toDouble() to "${it.ei}" },
            graduationsY = listOf(5.0, 10.0, 15.0, 20.0).map { it to "${it.toInt()}′" },
            titreY = "HP5 Plus à 20 °C",
            titreX = "Indice d'exposition (EI)",
            lire = { dx, min -> "EI ${eis[dx.roundToInt().coerceIn(0, eis.lastIndex)]} → DD-X ${minutesCourtes(min)}" },
            lireCourt = ::minutesCourtes,
            lireAutre = { serie, _, min -> "${serie.nom} ${minutesCourtes(min)}" },
            pas = 1.0
        )
        NoteTable(
            "« +1 » : un diaph de sous-exposition. Pousser fait monter les tons moyens et les hautes lumières, " +
                "pas les ombres où presque rien ne s'est inscrit : le contraste et le grain augmentent, le détail perdu ne revient pas. " +
                "Un tiret : Ilford ne publie pas ce temps."
        )
        source(Sources.ILFORD_HP5, TablesLabo.FILM_REFERENCE)
    }
}

/** Une ligne par couple révélateur-dilution ; la forme et le caractère sous le nom. */
@Composable
private fun TableauRevelateurs(lignes: List<LigneRevelateur>) {
    Tableau(
        listOf(Colonne("Révélateur", 1.5f), Colonne("Dilution", 0.9f), Colonne("Temps", 0.7f, aDroite = true)),
        lignes.mapIndexed { i, l ->
            val premier = i == 0 || lignes[i - 1].revelateur != l.revelateur
            val forme = when (l.forme) {
                TypeFiche.REVELATEUR_POUDRE -> "poudre"
                TypeFiche.REVELATEUR_LIQUIDE -> "liquide"
                else -> null
            }
            RangTableau(
                listOf(l.revelateur, l.dilution, "${TablesLabo.minutes(l.minutes)} min"),
                detail = listOfNotNull(forme?.takeIf { premier }, l.caractere.ifBlank { null }).joinToString(" · ").ifBlank { null }
            )
        }
    )
    Spacer(Modifier.height(Interligne / 2))
}

@Composable
private fun NoteTable(texte: String) {
    Text(
        texte,
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp)
    )
}
