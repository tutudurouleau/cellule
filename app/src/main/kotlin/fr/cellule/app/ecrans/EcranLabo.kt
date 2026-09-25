package fr.cellule.app.ecrans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.DepotPronostics
import fr.cellule.app.EtatApplication
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.app.chrono.Minuteur
import fr.cellule.core.Calculateur
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.Echelle
import fr.cellule.core.EntrainementLabo
import fr.cellule.core.Exercice
import fr.cellule.core.Progressions
import fr.cellule.core.Pronostic
import fr.cellule.core.Source
import fr.cellule.core.ThemeLabo
import fr.cellule.core.Verdict
import fr.cellule.core.libelleDiaphs
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class ModeLabo(val libelle: String) {
    CALCULER("Calculer"),
    CHRONO("Chrono"),
    FICHES("Fiches"),
    ENTRAINER("Quiz")
}

/*
 * L'état vit au-dessus des modes : passer du calcul à l'entraînement ne fait
 * perdre ni la question en cours ni le score.
 */
private class EtatEntrainement {
    var theme by mutableStateOf<ThemeLabo?>(null)
    var exercice by mutableStateOf<Exercice?>(null)
    var devoiles by mutableStateOf(0)
    var choisi by mutableStateOf<Int?>(null)
    var justes by mutableStateOf(0)
    var posees by mutableStateOf(0)
    private val alea = Random(System.nanoTime())

    init { suivant() }

    fun suivant() {
        exercice = theme?.let { EntrainementLabo.exercice(it, alea) } ?: EntrainementLabo.auHasard(alea)
        devoiles = 0
        choisi = null
    }

    fun repondre(i: Int) {
        val e = exercice ?: return
        if (choisi != null) return
        choisi = i
        posees += 1
        if (i == e.bonne) justes += 1
    }
}

/**
 * Le Labo : développer ses films, tirer ses épreuves — et comprendre pourquoi
 * les chiffres sont ce qu'ils sont. Chaque calculateur demande d'abord ton
 * estimation ; chaque chiffre renvoie à la fiche du fabricant. Le chrono
 * enchaîne les bains, écran éteint.
 */
@Composable
fun EcranLabo(pronostics: DepotPronostics, etatApplication: EtatApplication) {
    /* Un chrono en cours ramène directement à lui. */
    var mode by remember { mutableStateOf(if (Minuteur.enCours) ModeLabo.CHRONO else ModeLabo.CALCULER) }
    var calculateur by remember { mutableStateOf(Calculateur.TEMPERATURE) }
    val entrainement = remember { EtatEntrainement() }
    val chrono = remember { EtatChrono() }
    val versChrono: (Double) -> Unit = { secondes ->
        chrono.reglerRevelateur(secondes)
        mode = ModeLabo.CHRONO
    }
    /* Même geste que « verser au carnet » depuis Estimer : le carnet reprend la proposition. */
    val versCarnet: (DeveloppementNote) -> Unit = { etatApplication.developpementPropose = it }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 6.dp)) {
            ChoixSegmente(
                options = ModeLabo.entries.toList(),
                selection = mode,
                libelle = { it.libelle },
                surChoix = { mode = it }
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
        ) {
            when (mode) {
                ModeLabo.CALCULER -> {
                    Deroulant(
                        intitule = "Calculateur",
                        options = Calculateur.entries.toList(),
                        selection = calculateur,
                        libelle = { it.libelle },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Gouttiere)
                    ) { calculateur = it }
                    Spacer(Modifier.height(4.dp))
                    when (calculateur) {
                        Calculateur.TEMPERATURE -> CalculTemperature(pronostics, versChrono, versCarnet)
                        Calculateur.PUSH_PULL -> CalculPush(pronostics, versChrono, versCarnet)
                        Calculateur.RECIPROCITE -> CalculReciprocite(pronostics)
                        Calculateur.DILUTION -> CalculDilution(pronostics)
                        Calculateur.TIRAGE -> CalculTirage(pronostics)
                    }
                    CarteProgression(pronostics.liste.filter { it.calculateur == calculateur })
                }
                ModeLabo.CHRONO -> EcranChrono(chrono, versCarnet)
                ModeLabo.FICHES -> EcranFiches()
                ModeLabo.ENTRAINER -> Entrainement(entrainement)
            }
        }
    }
}

/* ── Le pari : annoncer avant de voir ─────────────────────────────────────── */

private class EtatPari {
    var saisie by mutableStateOf("")
    var revele by mutableStateOf(false)
    var estime by mutableStateOf<Double?>(null)
}

/**
 * Le geste commun à tous les calculateurs : tu tapes ton estimation, puis le
 * calcul apparaît à côté. Le résultat — et tout ce qui l'explique — reste
 * caché jusque-là. [cle] résume les données du calcul : si elles changent, le
 * pari recommence.
 *
 * [lire] et [afficher] travaillent dans l'unité de [calcule] (secondes, mL).
 * La révélation reçoit l'estimation, pour la verser au carnet avec le calcul.
 */
@Composable
fun Pari(
    cle: Any,
    calculateur: Calculateur,
    calcule: Double?,
    question: String,
    indication: String,
    lire: (String) -> Double?,
    afficher: (Double) -> String,
    contexte: String,
    pronostics: DepotPronostics,
    revelation: @Composable (estime: Double?) -> Unit
) {
    val etat = remember(cle) { EtatPari() }
    if (calcule == null) return

    if (!etat.revele) {
        Carte(titre = "Ton estimation", sousTitre = question) {
            Champ(
                valeur = etat.saisie,
                intitule = "Avant de voir le calcul",
                indication = indication,
                modifier = Modifier.fillMaxWidth()
            ) { etat.saisie = it }
            Spacer(Modifier.height(Interligne))
            val estime = lire(etat.saisie)
            LigneBoutons {
                BoutonPlat("Voir sans parier", modifier = Modifier.weight(1f)) { etat.revele = true }
                BoutonPlat("Révéler", actif = estime != null && estime > 0, accent = true, modifier = Modifier.weight(1f)) {
                    etat.estime = estime
                    etat.revele = true
                    if (estime != null) {
                        pronostics.ajouter(Pronostic(calculateur, LocalDate.now().toString(), estime, calcule, contexte))
                    }
                }
            }
        }
        return
    }

    val estime = etat.estime
    Carte {
        Row(Modifier.fillMaxWidth()) {
            if (estime != null) {
                Column(Modifier.weight(1f)) {
                    Etiquette("Ton estimation")
                    Text(afficher(estime), style = ChiffreGrand, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Column(Modifier.weight(1f)) {
                Etiquette("Le calcul", accent = true)
                Text(afficher(calcule), style = ChiffreGrand, color = MaterialTheme.colorScheme.primary)
            }
        }
        if (estime != null) {
            val p = Pronostic(calculateur, "", estime, calcule)
            Spacer(Modifier.height(Interligne))
            BandeauEtat(
                "Écart ${p.libelleEcart} — ${p.verdict.libelle}. " + when (p.verdict) {
                    Verdict.JUSTE -> "Ça ne se verrait pas."
                    Verdict.PROCHE -> "Ça se verrait, mais se rattrape."
                    Verdict.LOIN -> "Relis le raisonnement ci-dessous."
                },
                alerte = p.verdict == Verdict.LOIN
            )
        }
        Spacer(Modifier.height(6.dp))
        BoutonPlat("Nouveau pari", modifier = Modifier.fillMaxWidth()) {
            etat.revele = false
            etat.saisie = ""
            etat.estime = null
        }
    }
    revelation(estime)
}

/** La source d'un chiffre, qu'on peut ouvrir pour aller vérifier soi-même. */
@Composable
fun LigneSource(source: Source) {
    val liens = LocalUriHandler.current
    Text(
        "Source : ${source.citation} ↗",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { runCatching { liens.openUri(source.adresse) } }
            .padding(vertical = 6.dp)
    )
}

/** Un paragraphe d'explication, au corps du texte. */
@Composable
fun Paragraphe(texte: String) {
    Text(
        texte,
        style = Corps,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

/* ── La progression ───────────────────────────────────────────────────────── */

private fun libelleErreur(x: Double, echelle: Echelle): String = when (echelle) {
    Echelle.RELATIVE -> "${(x * 100).roundToInt()} %"
    Echelle.DIAPHS -> libelleDiaphs(x).removePrefix("+") + " diaph"
}

/**
 * Le carnet de l'intuition : tes paris sur ce calculateur, du premier au
 * dernier. Ce qui compte n'est pas un bon score, c'est une erreur qui fond.
 */
@Composable
private fun CarteProgression(liste: List<Pronostic>) {
    val p = Progressions.progression(liste) ?: return
    val echelle = liste.first().calculateur.echelle
    Carte(
        titre = "Ta progression",
        sousTitre = "Chaque estimation révélée est gardée ici. Le but : que l'intuition rejoigne le calcul."
    ) {
        Ligne("Paris", "${p.nombre}", "${(p.partJustes * 100).roundToInt()} % jugés justes")
        if (p.nombre > Progressions.FENETRE) {
            Ligne(
                "Erreur moyenne",
                "${libelleErreur(p.erreurDebut, echelle)} → ${libelleErreur(p.erreurRecente, echelle)}",
                "des ${Progressions.FENETRE} premiers aux ${Progressions.FENETRE} derniers",
                accent = p.progresse
            )
        } else {
            Ligne("Erreur moyenne", libelleErreur(p.erreurRecente, echelle), "la tendance apparaîtra après ${Progressions.FENETRE + 1} paris")
        }
        Separateur()
        liste.takeLast(5).reversed().forEach {
            Ligne(
                it.contexte.ifBlank { it.calculateur.libelle },
                it.libelleEcart,
                it.date + " · " + it.verdict.libelle,
                accent = it.verdict == Verdict.JUSTE
            )
        }
    }
}

/* ── S'entraîner ──────────────────────────────────────────────────────────── */

@Composable
private fun Entrainement(etat: EtatEntrainement) {
    Deroulant(
        intitule = "Thème",
        options = listOf<ThemeLabo?>(null) + ThemeLabo.entries,
        selection = etat.theme,
        libelle = { it?.libelle ?: "Tout mélanger" },
        modifier = Modifier.fillMaxWidth().padding(horizontal = Gouttiere)
    ) { etat.theme = it; etat.suivant() }
    Spacer(Modifier.height(4.dp))

    val e = etat.exercice ?: return
    val choisi = etat.choisi
    val repondu = choisi != null
    Carte {
        Etiquette(e.theme.libelle, accent = true)
        Spacer(Modifier.height(2.dp))
        Text(e.enonce, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))

        /* Une fois la réponse donnée, tous les indices se découvrent : ils
           deviennent le raisonnement à retenir. */
        val visibles = if (repondu) e.indices else e.indices.take(etat.devoiles)
        visibles.forEachIndexed { i, indice ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("${i + 1}", style = Detail, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                Text(indice, style = Corps, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        val reste = e.indices.size - etat.devoiles
        if (!repondu && reste > 0) {
            BoutonPlat(
                if (etat.devoiles == 0) "Un indice" else "Encore un indice ($reste)",
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) { etat.devoiles += 1 }
        }

        Spacer(Modifier.height(Interligne))
        e.choix.forEachIndexed { i, c ->
            BoutonPlat(
                c,
                actif = !repondu || i == e.bonne,
                accent = repondu && i == e.bonne,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { etat.repondre(i) }
        }

        if (choisi != null) {
            Spacer(Modifier.height(6.dp))
            val juste = choisi == e.bonne
            BandeauEtat(
                if (juste) "Juste" + if (etat.devoiles <= 1) " !" else ", avec ${etat.devoiles} indices."
                else "Non : tu as répondu ${e.choix[choisi]}.",
                alerte = !juste
            )
            Spacer(Modifier.height(6.dp))
            Paragraphe(e.explication)
            e.source?.let { LigneSource(it) }
            Spacer(Modifier.height(6.dp))
            BoutonPlat("Suivant", accent = true, modifier = Modifier.fillMaxWidth()) { etat.suivant() }
        }
    }

    if (etat.posees > 0) {
        Carte {
            Ligne(
                "Score", "${etat.justes} / ${etat.posees}",
                if (etat.posees < 5) "encore un peu court"
                else "${(etat.justes * 100.0 / etat.posees).roundToInt()} % de réussite"
            )
        }
    }
}

/** « 0,83 » : un facteur de temps lisible. */
fun facteur(x: Double): String = "×" + fmt(x, 2)
