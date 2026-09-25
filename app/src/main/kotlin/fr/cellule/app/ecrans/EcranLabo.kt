package fr.cellule.app.ecrans

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import fr.cellule.app.ChiffreGrand
import fr.cellule.app.Corps
import fr.cellule.app.DepotLabo
import fr.cellule.app.DepotPronostics
import fr.cellule.app.DepotTirages
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.RayonCarte
import fr.cellule.app.TitreCarte
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.app.chrono.Minuteur
import fr.cellule.core.Calculateur
import fr.cellule.core.CarnetLabo
import fr.cellule.core.Chrono
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.DomaineLabo
import fr.cellule.core.Duree
import fr.cellule.core.Echelle
import fr.cellule.core.EntrainementLabo
import fr.cellule.core.Exercice
import fr.cellule.core.Progressions
import fr.cellule.core.Pronostic
import fr.cellule.core.Sequences
import fr.cellule.core.Source
import fr.cellule.core.ThemeLabo
import fr.cellule.core.TirageNote
import fr.cellule.core.Verdict
import fr.cellule.core.libelleDiaphs
import fr.cellule.core.theme
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Où l'on est dans le Labo : l'accueil, ou l'un de ses outils. Le retour du
 * téléphone ramène toujours à l'accueil.
 */
private sealed interface VueLabo {
    data object Accueil : VueLabo
    data class Calcul(val calculateur: Calculateur) : VueLabo
    data object Chrono : VueLabo
    data object Fiches : VueLabo
    data object Quiz : VueLabo
}

private fun titre(v: VueLabo): String = when (v) {
    VueLabo.Accueil -> "Labo"
    is VueLabo.Calcul -> v.calculateur.libelle
    VueLabo.Chrono -> "Chrono"
    VueLabo.Fiches -> "Fiches produits"
    VueLabo.Quiz -> "Quiz"
}

/*
 * L'état vit au-dessus des vues : passer du calcul au quiz ne fait perdre ni
 * la question en cours ni le score.
 */
private class EtatEntrainement(themeInitial: ThemeLabo? = null) {
    var theme by mutableStateOf(themeInitial)
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
 * les chiffres sont ce qu'ils sont. Un accueil par domaine (film, tirage,
 * couleur) mène aux calculateurs, au chrono, aux fiches et au quiz ; un
 * chrono en cours reste toujours à un appui.
 */
@Composable
fun EcranLabo(
    pronostics: DepotPronostics,
    etatApplication: EtatApplication,
    labo: DepotLabo,
    tirages: DepotTirages
) {
    /* Un chrono en cours ramène directement à lui. */
    var vue by remember { mutableStateOf<VueLabo>(if (Minuteur.enCours) VueLabo.Chrono else VueLabo.Accueil) }
    var domaine by remember { mutableStateOf(DomaineLabo.FILM) }
    val entrainement = remember { EtatEntrainement() }
    val chrono = remember { EtatChrono() }
    val versChrono: (Double) -> Unit = { secondes ->
        chrono.reglerRevelateur(secondes)
        vue = VueLabo.Chrono
    }
    /* Même geste que « verser au carnet » depuis Estimer : le carnet reprend la proposition. */
    val versCarnet: (DeveloppementNote) -> Unit = { etatApplication.developpementPropose = it }
    val versTirage: (TirageNote) -> Unit = { etatApplication.tiragePropose = it }
    /* Le chrono s'ouvre sur l'enchaînement du domaine : bains film ou bains papier. */
    val ouvrirChrono = {
        if (!Minuteur.enCours) {
            val papier = chrono.modele.nom == Sequences.PAPIER.nom
            if (domaine == DomaineLabo.TIRAGE && !papier) chrono.charger(Sequences.PAPIER)
            if (domaine == DomaineLabo.FILM && papier) chrono.charger(Sequences.FILM_ILFORD)
        }
        vue = VueLabo.Chrono
    }

    BackHandler(enabled = vue != VueLabo.Accueil) { vue = VueLabo.Accueil }

    Column(Modifier.fillMaxSize()) {
        if (vue != VueLabo.Accueil) {
            EnTeteVue(titre(vue)) { vue = VueLabo.Accueil }
        }
        if (Minuteur.enCours && vue != VueLabo.Chrono && vue != VueLabo.Accueil) {
            BandeauChrono { vue = VueLabo.Chrono }
        }
        /* Chaque vue repart du haut. */
        key(vue) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
            ) {
                when (val v = vue) {
                    VueLabo.Accueil -> Accueil(
                        domaine = domaine,
                        surDomaine = { domaine = it },
                        labo = labo,
                        tirages = tirages,
                        ouvrirCalcul = { vue = VueLabo.Calcul(it) },
                        ouvrirChrono = ouvrirChrono,
                        ouvrirFiches = { vue = VueLabo.Fiches },
                        ouvrirQuiz = { theme ->
                            if (theme != null) {
                                entrainement.theme = theme
                                entrainement.suivant()
                            }
                            vue = VueLabo.Quiz
                        }
                    )
                    is VueLabo.Calcul -> {
                        when (v.calculateur) {
                            Calculateur.TEMPERATURE -> CalculTemperature(pronostics, versChrono, versCarnet)
                            Calculateur.PUSH_PULL -> CalculPush(pronostics, versChrono, versCarnet)
                            Calculateur.RECIPROCITE -> CalculReciprocite(pronostics)
                            Calculateur.DILUTION -> CalculDilution(pronostics)
                            Calculateur.TIRAGE -> CalculTirage(pronostics, versTirage)
                        }
                        CarteProgression(pronostics.liste.filter { it.calculateur == v.calculateur })
                        QuestionDuTheme(v.calculateur)
                    }
                    VueLabo.Chrono -> EcranChrono(chrono, versCarnet)
                    VueLabo.Fiches -> EcranFiches()
                    VueLabo.Quiz -> {
                        Carte(
                            sousTitre = "Les calculateurs vérifient ton intuition sur un cas réel. Le quiz entraîne ce qui ne se calcule pas — " +
                                "l'ordre des bains, les durées des fiches — et le calcul de tête, avec des indices si tu bloques."
                        ) {}
                        Entrainement(entrainement)
                    }
                }
            }
        }
    }
}

/** « ‹ Labo » et le nom de l'outil ouvert. */
@Composable
private fun EnTeteVue(titre: String, retour: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹ Labo",
            style = Corps,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { retour() }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(titre, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** L'heure du téléphone, relue quatre fois par seconde pour les comptes à rebours. */
@Composable
private fun maintenant(): Long {
    var t by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(Unit) {
        while (true) {
            t = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    return t
}

/** Le chrono en cours, résumé en une ligne au-dessus des autres outils. */
@Composable
private fun BandeauChrono(ouvrir: () -> Unit) {
    val s = Minuteur.sequence ?: return
    val p = Chrono.position(s, Minuteur.ecoule(maintenant()))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { ouvrir() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(IconeChrono, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            if (p.fini) "Chrono terminé" else "${s.etapes[p.etape].nom} · ${Duree.chrono(p.resteEtape)}" +
                when {
                    Minuteur.pause != null -> " · en pause"
                    p.agite -> " · agite !"
                    else -> ""
                },
            style = Corps,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )
        Text("›", style = Corps, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/* ── L'accueil ────────────────────────────────────────────────────────────── */

private class Tuile(val titre: String, val detail: String, val icone: ImageVector, val ouvrir: () -> Unit)

@Composable
private fun Accueil(
    domaine: DomaineLabo,
    surDomaine: (DomaineLabo) -> Unit,
    labo: DepotLabo,
    tirages: DepotTirages,
    ouvrirCalcul: (Calculateur) -> Unit,
    ouvrirChrono: () -> Unit,
    ouvrirFiches: () -> Unit,
    ouvrirQuiz: (ThemeLabo?) -> Unit
) {
    /* En chambre noire, c'est l'information qui compte : elle passe devant tout. */
    if (Minuteur.enCours) CarteChronoEnCours(ouvrirChrono)

    Box(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 6.dp)) {
        ChoixSegmente(DomaineLabo.entries.toList(), domaine, { it.libelle }) { surDomaine(it) }
    }

    val calculs = domaine.calculateurs.map { c -> Tuile(c.libelle, detailCalcul(c), iconeCalcul(c)) { ouvrirCalcul(c) } }
    when (domaine) {
        DomaineLabo.FILM -> {
            FilConducteur(
                listOf(
                    "Calcule" to "temps, push, dilution",
                    "Chronomètre" to "les bains, écran éteint",
                    "Note" to "au carnet, avec ton estimation"
                )
            )
            Tuiles(
                calculs + listOf(
                    Tuile("Chrono", "enchaîner les bains", IconeChrono, ouvrirChrono),
                    Tuile("Fiches", "films, révélateurs, bains", IconeFiches, ouvrirFiches),
                    Tuile("Quiz", "ce qui ne se calcule pas", IconeQuiz) { ouvrirQuiz(null) }
                )
            )
            DernierFilm(labo)
        }
        DomaineLabo.TIRAGE -> {
            FilConducteur(
                listOf(
                    "Bande d'essai" to "à pas égaux, en diaphs",
                    "Corrige" to "zone par zone",
                    "Note" to "au carnet de tirage"
                )
            )
            Tuiles(
                calculs + listOf(
                    Tuile("Chrono", "bains papier", IconeChrono, ouvrirChrono),
                    Tuile("Quiz", "les diaphs de tête", IconeQuiz) { ouvrirQuiz(ThemeLabo.DIAPHS) }
                )
            )
            Carte(
                titre = "Fiches papier : à venir",
                sousTitre = "Papiers RC et barytés, multigrades et grades fixes, filtres 00 à 5, révélateurs papier : " +
                    "ces chiffres viendront des fiches Ilford Multigrade et des révélateurs, une fois relues chez leur fabricant."
            ) {}
            DernierTirage(tirages)
        }
        DomaineLabo.COULEUR -> CarteCouleur()
    }
}

private fun iconeCalcul(c: Calculateur): ImageVector = when (c) {
    Calculateur.TEMPERATURE -> IconeTemperature
    Calculateur.PUSH_PULL -> IconePush
    Calculateur.RECIPROCITE -> IconeReciprocite
    Calculateur.DILUTION -> IconeDilution
    Calculateur.TIRAGE -> IconeTirage
}

private fun detailCalcul(c: Calculateur): String = when (c) {
    Calculateur.TEMPERATURE -> "le temps selon le bain"
    Calculateur.PUSH_PULL -> "exposé à un autre EI"
    Calculateur.RECIPROCITE -> "les poses longues"
    Calculateur.DILUTION -> "concentré et eau"
    Calculateur.TIRAGE -> "bandes, masquage, brûlage"
}

/** Les tuiles, deux par ligne, de même hauteur. */
@Composable
private fun Tuiles(tuiles: List<Tuile>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(Interligne)) {
        tuiles.chunked(2).forEach { ligne ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Interligne)) {
                ligne.forEach { t ->
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(RayonCarte))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { t.ouvrir() }
                            .padding(Gouttiere)
                    ) {
                        Icon(t.icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(t.titre, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                        Text(t.detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (ligne.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Le fil d'un travail au labo, en trois temps : c'est lui qui relie les outils. */
@Composable
private fun FilConducteur(etapes: List<Pair<String, String>>) {
    Carte {
        Row(Modifier.fillMaxWidth()) {
            etapes.forEachIndexed { i, (nom, detail) ->
                Column(Modifier.weight(1f)) {
                    Text("${i + 1}. $nom", style = Corps, color = MaterialTheme.colorScheme.primary)
                    Text(detail, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (i < etapes.lastIndex) {
                    Text("→", style = Corps, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
    }
}

/** Le chrono en cours, en grand, en tête de l'accueil. */
@Composable
private fun CarteChronoEnCours(ouvrir: () -> Unit) {
    val s = Minuteur.sequence ?: return
    val p = Chrono.position(s, Minuteur.ecoule(maintenant()))
    Carte(modifier = Modifier.clickable { ouvrir() }) {
        Etiquette("Chrono en cours · ${s.nom}", accent = true)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.etapes[p.etape].nom, style = TitreCarte, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    when {
                        p.fini -> "terminé"
                        Minuteur.pause != null -> "en pause"
                        p.agite -> "agite !"
                        else -> "bain ${p.etape + 1} sur ${s.etapes.size}"
                    },
                    style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (p.fini) "✓" else Duree.chrono(p.resteEtape),
                style = ChiffreGrand,
                color = if (p.agite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** La preuve de progression, dès l'accueil : le dernier film noté. */
@Composable
private fun DernierFilm(labo: DepotLabo) {
    val n = labo.notes.lastOrNull() ?: return
    val bilan = CarnetLabo.bilan(labo.notes)
    Carte(titre = "Ton dernier film", sousTitre = "${labo.notes.size} film(s) au carnet · le détail est dans Carnet › Films") {
        Ligne(n.titre, n.date)
        listOfNotNull(
            n.estimation?.let { "estimé ${Duree.libelle(it)}" },
            n.tempsCalcule?.let { "calculé ${Duree.libelle(it)}" },
            n.tempsDonne?.let { "donné ${Duree.libelle(it)}" }
        ).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString("  ·  "), style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        n.resultat?.let { Ligne("Résultat", it.libelle) }
        bilan?.erreurRecente?.let {
            Ligne("Ton intuition", "${(it * 100).roundToInt()} %", "erreur moyenne de tes dernières estimations")
        }
    }
}

@Composable
private fun DernierTirage(tirages: DepotTirages) {
    val t = tirages.tirages.lastOrNull() ?: return
    Carte(titre = "Ton dernier tirage", sousTitre = "${tirages.tirages.size} tirage(s) au carnet · le détail est dans Carnet › Tirages") {
        Ligne(t.titre, t.date, listOf(t.papier, t.filtre, t.ouverture).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null })
        t.deroule().forEach { Text(it, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/**
 * La couleur, pour l'instant : de quoi comprendre les trois procédés, sans un
 * chiffre qui n'ait pas été relu chez Kodak.
 */
@Composable
private fun CarteCouleur() {
    Carte(
        titre = "La couleur, bientôt",
        sousTitre = "Découverte d'abord, pas de calculateur : il s'agit de comprendre l'exigence avant de s'y risquer."
    ) {
        Ligne("C-41", "négatif couleur", "la chimie des films comme la Portra ou l'Ektar")
        Ligne("E-6", "diapositive", "un procédé inversible, aux étapes plus nombreuses")
        Ligne("RA-4", "tirage couleur", "le papier couleur, sous agrandisseur ou en machine")
        Spacer(Modifier.height(6.dp))
        Paragraphe(
            "Ces procédés tiennent leur température bien plus serré que le noir et blanc. Leurs chiffres viendront des manuels " +
                "Kodak (Z-131 pour le C-41, Z-130 pour le RA-4) dès qu'ils auront pu être relus ; d'ici là, le Labo n'affiche rien " +
                "plutôt qu'un chiffre de seconde main."
        )
    }
}

/** Au bas de chaque calculateur, une question de tête sur le même thème. */
@Composable
private fun QuestionDuTheme(c: Calculateur) {
    val etat = remember(c) { EtatEntrainement(c.theme) }
    CarteExercice(etat, entete = "Pour t'entraîner : une question de tête")
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

    CarteExercice(etat)

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

/** Une question : énoncé, indices à la demande, propositions, puis l'explication et sa source. */
@Composable
private fun CarteExercice(etat: EtatEntrainement, entete: String? = null) {
    val e = etat.exercice ?: return
    val choisi = etat.choisi
    val repondu = choisi != null
    Carte {
        Etiquette(entete ?: e.theme.libelle, accent = true)
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
}

/** « 0,83 » : un facteur de temps lisible. */
fun facteur(x: Double): String = "×" + fmt(x, 2)
