package fr.cellule.app.ecrans

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.cellule.app.CadreJson
import fr.cellule.app.Corps
import fr.cellule.app.DepotCinema
import fr.cellule.app.DepotJournal
import fr.cellule.app.Detail
import fr.cellule.app.EtatApplication
import fr.cellule.app.Gouttiere
import fr.cellule.app.Interligne
import fr.cellule.app.Reglages
import fr.cellule.app.ZoneNavFlottante
import fr.cellule.core.CATALOGUE_CAMERAS
import fr.cellule.core.CATALOGUE_OBJECTIFS
import fr.cellule.core.CadrePlan
import fr.cellule.core.Camera
import fr.cellule.core.ETAGERES_CAMERAS
import fr.cellule.core.FORMATS_PELLICULE
import fr.cellule.core.Focales
import fr.cellule.core.MaterielCine
import fr.cellule.core.Objectif
import fr.cellule.core.PELLICULES_CINE
import fr.cellule.core.PelliculeCine
import fr.cellule.core.Silhouette
import fr.cellule.core.ZoneUtile
import fr.cellule.core.nombreCourt
import kotlin.math.abs

/*
 * L'onglet Cinéma : tout ce qui sert à préparer et noter un tournage, à part
 * de la photo. Trois vues — le viseur (cellule de tournage et capture de
 * plan), les plans rangés en dossiers, et le matériel (pellicule ou capteur,
 * caméra, objectifs) qui règle le cadre du viseur.
 */

enum class VueCinema(val libelle: String) { VISEUR("Viseur"), PLANS("Plans"), MATERIEL("Matériel") }

private const val AUCUNE = "Aucune"

/** Le cadre de travail tel qu'il est enregistré. */
private fun cadreDe(reglages: Reglages): CadrePlan = CadreJson.lireTexte(reglages.cineCadre)

private fun ecrireCadre(reglages: Reglages, cadre: CadrePlan) {
    reglages.cineCadre = CadreJson.ecrire(cadre).toString()
}

fun pelliculeChoisie(reglages: Reglages): PelliculeCine? = PELLICULES_CINE.firstOrNull { it.nom == reglages.cinePellicule }
fun formatChoisi(reglages: Reglages): ZoneUtile? = FORMATS_PELLICULE.firstOrNull { it.nom == reglages.cineFormatPellicule }
fun cameraChoisie(reglages: Reglages): Camera? = CATALOGUE_CAMERAS.firstOrNull { it.nom == reglages.cineCamera }
fun objectifChoisi(reglages: Reglages): Objectif? = CATALOGUE_OBJECTIFS.firstOrNull { it.nomComplet == reglages.cineObjectif }
private fun modeChoisi(reglages: Reglages): ZoneUtile? =
    MaterielCine.modesDe(reglages.cineCamera).firstOrNull { it.nom == reglages.cineModeCapteur }

/** Le nom du « capteur » noté avec les plans : pellicule et fenêtre, ou caméra et mode. */
private fun nomSupport(reglages: Reglages): String = if (reglages.cineSupport == "PELLICULE") {
    listOfNotNull(
        pelliculeChoisie(reglages)?.let { p -> p.nom + (if (p.codes.isNotBlank()) " " + p.codes else "") },
        formatChoisi(reglages)?.nom,
        cameraChoisie(reglages)?.nomComplet
    ).joinToString(" · ")
} else {
    listOfNotNull(cameraChoisie(reglages)?.nomComplet, modeChoisi(reglages)?.nom).joinToString(" — ")
}

/** Reporte le choix de matériel dans le cadre de travail : nom, et taille utile quand elle est publiée. */
private fun reporterSupport(reglages: Reglages) {
    val zone = if (reglages.cineSupport == "PELLICULE") formatChoisi(reglages) else modeChoisi(reglages)
    val c = cadreDe(reglages)
    ecrireCadre(
        reglages,
        c.copy(
            capteurNom = nomSupport(reglages).ifBlank { c.capteurNom },
            capteurLargeurMm = zone?.largeurMm ?: c.capteurLargeurMm,
            capteurHauteurMm = zone?.hauteurMm ?: c.capteurHauteurMm
        )
    )
}

/** La ligne qui résume le matériel, sous le sélecteur de vue. */
fun resumeMateriel(reglages: Reglages): String {
    val c = cadreDe(reglages)
    val support = nomSupport(reglages).ifBlank {
        if (reglages.cineSupport == "PELLICULE") "Pellicule — à choisir" else "Numérique — sans caméra choisie"
    }
    val focale = c.focaleMm?.let { nombreCourt(it, 1) + " mm" }
    return listOfNotNull(support, focale, "EI ${reglages.cineEi}").joinToString(" · ")
}

/** Les focales qu'on propose au viseur : celles de la série choisie, sinon des focales courantes. */
private fun focalesProposees(reglages: Reglages): List<Double> =
    objectifChoisi(reglages)?.let { MaterielCine.lireFocales(it.focales).choix } ?: MaterielCine.FOCALES_COURANTES

/** L'onglet entier. */
@Composable
fun EcranOngletCinema(reglages: Reglages, etat: EtatApplication, cinema: DepotCinema, journal: DepotJournal) {
    val vue = runCatching { VueCinema.valueOf(reglages.cineVue) }.getOrDefault(VueCinema.VISEUR)
    val choisir: (VueCinema) -> Unit = { reglages.cineVue = it.name }

    if (vue == VueCinema.VISEUR) {
        EcranMesurer(
            reglages, etat, cinema,
            modeCinema = true,
            enTete = { EnTeteCinema(reglages, vue, sombre = true, surVue = choisir) }
        )
        return
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = Gouttiere, vertical = 8.dp)) {
            EnTeteCinema(reglages, vue, sombre = false, surVue = choisir)
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(bottom = ZoneNavFlottante)
        ) {
            when (vue) {
                VueCinema.PLANS -> CarnetCinemaEcran(cinema, journal, reglages)
                else -> PageMateriel(reglages)
            }
        }
    }
}

/** Le sélecteur Viseur | Plans | Matériel, et le matériel en cours sur une ligne (touche : Matériel). */
@Composable
private fun EnTeteCinema(reglages: Reglages, vue: VueCinema, sombre: Boolean, surVue: (VueCinema) -> Unit) {
    ChoixSegmente(VueCinema.entries.toList(), vue, { it.libelle }) { surVue(it) }
    Text(
        resumeMateriel(reglages),
        style = Detail,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = if (sombre) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { surVue(VueCinema.MATERIEL) }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    )
}

/* ── Le viseur : focale ou angle, choisis sans quitter le cadre ─────────── */

/** Une petite pastille lisible sur l'image. */
@Composable
private fun PastilleViseur(texte: String, actif: Boolean, surClic: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (actif) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.14f))
            .clickable { surClic() }
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(
            texte,
            style = Detail,
            maxLines = 1,
            color = if (actif) MaterialTheme.colorScheme.onPrimary else Color.White
        )
    }
}

/**
 * En haut du viseur de tournage : on choisit la focale, ou directement
 * l'angle de champ voulu — Cellule en déduit la focale pour le capteur du
 * cadre de travail. Le cadre tracé sur l'image suit aussitôt.
 */
@Composable
fun BoxScope.BarreFocale(reglages: Reglages, surMateriel: () -> Unit) {
    val cadre = remember(reglages.cineCadre) { cadreDe(reglages) }
    var parAngle by remember { mutableStateOf(false) }
    val largeur = cadre.capteurLargeurMm?.takeIf { it > 0 }?.let { it * cadre.desanamorphose }
    val hauteur = cadre.capteurHauteurMm?.takeIf { it > 0 }
    val focale = cadre.focaleMm

    Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PastilleViseur(if (parAngle) "Angle" else "Focale", actif = false) { parAngle = !parAngle }
            Spacer(Modifier.padding(start = 6.dp))
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (!parAngle) {
                    focalesProposees(reglages).forEach { f ->
                        PastilleViseur(nombreCourt(f, 1), actif = focale != null && abs(focale - f) < 0.05) {
                            ecrireCadre(reglages, cadre.copy(focaleMm = f))
                        }
                    }
                } else if (largeur != null) {
                    val angleActuel = focale?.let { Focales.angleDeChamp(largeur, it) }
                    MaterielCine.ANGLES_COURANTS.forEach { a ->
                        PastilleViseur(
                            nombreCourt(a, 0) + "°",
                            actif = angleActuel != null && abs(angleActuel - a) < 0.5
                        ) {
                            val f = MaterielCine.focalePourAngle(largeur, a)
                            if (!f.isNaN()) ecrireCadre(reglages, cadre.copy(focaleMm = Math.round(f * 10) / 10.0))
                        }
                    }
                } else {
                    PastilleViseur("Choisis d'abord ton capteur", actif = false, surClic = surMateriel)
                }
            }
        }
        val ligne = when {
            largeur == null || hauteur == null -> "Taille du capteur inconnue : choisis la caméra ou la pellicule dans Matériel."
            focale == null -> "Choisis une focale ou un angle de champ."
            else -> nombreCourt(focale, 1) + " mm · champ " +
                nombreCourt(Focales.angleDeChamp(largeur, focale), 0) + "° × " +
                nombreCourt(Focales.angleDeChamp(hauteur, focale), 0) + "°" +
                (if (cadre.desanamorphose != 1.0) " (désanamorphosé ×" + nombreCourt(cadre.desanamorphose, 1) + ")" else "")
        }
        Text(
            ligne,
            style = Detail,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .padding(start = 4.dp, top = 5.dp)
                .then(if (largeur == null) Modifier.clickable { surMateriel() } else Modifier)
        )
    }
}

/* ── Le matériel ───────────────────────────────────────────────────────── */

private val ETAGERES_PELLICULE = setOf("ARRI — la pellicule", "L'histoire en pellicule")
private val PANAVISION_PELLICULE = listOf("Panaflex", "System 65", "Millennium XL2")

private fun camerasPellicule(): List<Camera> = ETAGERES_CAMERAS.flatMap { e ->
    if (e.titre in ETAGERES_PELLICULE) e.entrees
    else e.entrees.filter { c -> PANAVISION_PELLICULE.any { c.nom.startsWith(it) } }
}

/** Les caméras numériques : celles dont les tailles de capteur sont relues d'abord. */
private fun camerasNumeriques(): List<Camera> {
    val pellicule = camerasPellicule().toSet()
    val toutes = CATALOGUE_CAMERAS.filter { it !in pellicule }
    val avecModes = toutes.filter { MaterielCine.modesDe(it.nom).isNotEmpty() }
    return avecModes + (toutes - avecModes.toSet())
}

@Composable
private fun PageMateriel(reglages: Reglages) {
    var cadreOuvert by remember { mutableStateOf(false) }
    val pellicule = reglages.cineSupport == "PELLICULE"

    Carte(
        titre = "Sur quoi tu tournes",
        sousTitre = "Comme la pellicule chargée dans l'onglet Mesurer, mais pour le cinéma. Le matériel choisi règle l'indice de la cellule de tournage et le cadre tracé dans le viseur."
    ) {
        ChoixSegmente(listOf("NUMERIQUE", "PELLICULE"), reglages.cineSupport, {
            if (it == "PELLICULE") "Pellicule" else "Numérique"
        }) {
            reglages.cineSupport = it
            reporterSupport(reglages)
        }
    }

    if (pellicule) CartePellicule(reglages) else CarteNumerique(reglages) { cadreOuvert = true }
    CarteObjectifs(reglages)

    val cadre = remember(reglages.cineCadre) { cadreDe(reglages) }
    Carte(
        titre = "Cadre de travail",
        sousTitre = "Ce que reçoivent le viseur et chaque nouveau plan. Tout reste modifiable à la main, plan par plan."
    ) {
        Text(cadre.resume.ifBlank { "Rien de réglé pour l'instant." }, style = Corps, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(Interligne))
        BoutonPlat("Régler le cadre, les formats, la numérotation", modifier = Modifier.fillMaxWidth()) { cadreOuvert = true }
    }
    Spacer(Modifier.height(Interligne))

    if (cadreOuvert) FeuilleCadreDeTravail(reglages) { cadreOuvert = false }
}

@Composable
private fun CartePellicule(reglages: Reglages) {
    val p = pelliculeChoisie(reglages)
    val format = formatChoisi(reglages)
    Carte(titre = "Pellicule", sousTitre = "Les indices sont ceux que publie le fabricant, filtre de conversion compris.") {
        Deroulant("Pellicule chargée", listOf(AUCUNE) + PELLICULES_CINE.map { it.nom }, p?.nom ?: AUCUNE, { it }, Modifier.fillMaxWidth()) { nom ->
            reglages.cinePellicule = if (nom == AUCUNE) "" else nom
            pelliculeChoisie(reglages)?.let { reglages.cineEi = it.ei(reglages.cineTungstene) }
            reporterSupport(reglages)
        }
        if (p != null) {
            Spacer(Modifier.height(Interligne))
            ChoixSegmente(listOf(false, true), reglages.cineTungstene, { if (it) "Tungstène 3200 K" else "Lumière du jour" }) {
                reglages.cineTungstene = it
                reglages.cineEi = p.ei(it)
            }
            Spacer(Modifier.height(6.dp))
            val filtre = p.filtre(reglages.cineTungstene)
            Ligne(
                "Indice publié", "EI ${p.ei(reglages.cineTungstene)}",
                (if (filtre.isNotBlank()) "avec filtre $filtre · " else "sans filtre · ") + p.type.libelle +
                    (if (p.codes.isNotBlank()) " · " + p.codes else ""),
                accent = true
            )
            Text(
                "La cellule de tournage passe à cet indice. Le filtre de conversion est déjà compté dans l'indice publié : ne l'ajoute pas une seconde fois dans les filtres. Source : ${p.source}.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(Interligne + 2.dp))
        Deroulant("Fenêtre de prise de vues", listOf(AUCUNE) + FORMATS_PELLICULE.map { it.nom }, format?.nom ?: AUCUNE, { it }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineFormatPellicule = if (nom == AUCUNE) "" else nom
            reporterSupport(reglages)
        }
        format?.let {
            Spacer(Modifier.height(6.dp))
            Ligne("Zone utile", it.dimensions, "Source : ${it.source}")
        }
        Spacer(Modifier.height(Interligne + 2.dp))
        val cameras = camerasPellicule()
        val camera = cameraChoisie(reglages)?.takeIf { it in cameras }
        Deroulant("Caméra (facultatif)", listOf(AUCUNE) + cameras.map { it.nom }, camera?.nom ?: AUCUNE, { n ->
            if (n == AUCUNE) "Aucune caméra" else cameras.first { it.nom == n }.nomComplet
        }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineCamera = if (nom == AUCUNE) "" else nom
            reglages.cineModeCapteur = ""
            reporterSupport(reglages)
        }
    }
}

@Composable
private fun CarteNumerique(reglages: Reglages, ouvrirCadre: () -> Unit) {
    val cameras = camerasNumeriques()
    val camera = cameraChoisie(reglages)?.takeIf { it in cameras }
    val modes = camera?.let { MaterielCine.modesDe(it.nom) } ?: emptyList()
    val mode = modeChoisi(reglages)
    Carte(titre = "Caméra", sousTitre = "Facultatif : sans caméra, tu saisis la taille du capteur à la main dans le cadre de travail.") {
        Deroulant("Caméra du catalogue", listOf(AUCUNE) + cameras.map { it.nom }, camera?.nom ?: AUCUNE, { n ->
            if (n == AUCUNE) "Aucune caméra" else cameras.first { it.nom == n }.let { c ->
                c.nomComplet + if (MaterielCine.modesDe(c.nom).isEmpty()) "  (capteur à saisir)" else ""
            }
        }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineCamera = if (nom == AUCUNE) "" else nom
            reglages.cineModeCapteur = MaterielCine.modesDe(reglages.cineCamera).firstOrNull()?.nom ?: ""
            reporterSupport(reglages)
        }
        if (camera != null) {
            Spacer(Modifier.height(Interligne))
            if (modes.isNotEmpty()) {
                Deroulant("Mode de capteur", modes.map { it.nom }, mode?.nom ?: modes.first().nom, { it }, Modifier.fillMaxWidth()) { nom ->
                    reglages.cineModeCapteur = nom
                    reporterSupport(reglages)
                }
                (mode ?: modes.first()).let {
                    Spacer(Modifier.height(6.dp))
                    Ligne("Zone utile", it.dimensions, "Source : ${it.source}", accent = true)
                }
            } else {
                Text(
                    "Les tailles de capteur de cette caméra ne sont pas encore relues sur une fiche du fabricant : Cellule n'en invente pas. Saisis la largeur et la hauteur utiles du mode que tu utilises.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                BoutonPlat("Saisir la taille du capteur", modifier = Modifier.fillMaxWidth(), surClic = ouvrirCadre)
            }
            camera.chiffresCles.firstOrNull { it.libelle == "Capteur" }?.let {
                Spacer(Modifier.height(4.dp))
                Text("Fiche : capteur ${it.valeur}", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CarteObjectifs(reglages: Reglages) {
    val objectif = objectifChoisi(reglages)
    val noms = CATALOGUE_OBJECTIFS.map { it.nomComplet }
    Carte(titre = "Objectifs", sousTitre = "Facultatif : la série choisie donne les focales proposées en haut du viseur.") {
        Deroulant("Série d'objectifs", listOf(AUCUNE) + noms, objectif?.nomComplet ?: AUCUNE, {
            if (it == AUCUNE) "Aucune série (focales courantes)" else it
        }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineObjectif = if (nom == AUCUNE) "" else nom
            val c = cadreDe(reglages)
            val choix = focalesProposees(reglages)
            /* Une focale hors de la série est ramenée à la plus proche qu'elle propose. */
            val focale = c.focaleMm?.let { f -> choix.minByOrNull { abs(it - f) } }
            ecrireCadre(reglages, c.copy(objectif = if (nom == AUCUNE) "" else nom, focaleMm = focale ?: c.focaleMm))
        }
        if (objectif != null) {
            val serie = MaterielCine.lireFocales(objectif.focales)
            Spacer(Modifier.height(6.dp))
            Ligne("Focales", objectif.focales, if (serie.exactes) "les focales publiées de la série" else "plage de la série ; les pastilles du viseur sont des focales courantes dans cette plage, à vérifier chez le loueur")
            Ligne("Ouverture", objectif.ouverture)
            Ligne("Couverture", objectif.couverture)
            Ligne("Monture", objectif.monture)
            if (objectif.silhouette == Silhouette.ANAMORPHIQUE) {
                Text(
                    "Série anamorphique : règle le facteur de désanamorphose de la série (×1,3, ×1,5, ×1,8 ou ×2) dans le cadre de travail, d'après la fiche du fabricant.",
                    style = Detail,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
