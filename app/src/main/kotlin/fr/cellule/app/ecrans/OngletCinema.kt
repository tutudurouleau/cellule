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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.zIndex
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
import fr.cellule.core.SeriePerso
import fr.cellule.core.CameraPerso
import fr.cellule.app.MonMateriel
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

/* Le matériel à soi se choisit par une clé « perso:… », le catalogue par son nom. */
fun cameraPersoChoisie(reglages: Reglages): CameraPerso? = MonMateriel.camera(reglages.cineCamera)
fun seriePersoChoisie(reglages: Reglages): SeriePerso? = MonMateriel.serie(reglages.cineObjectif)
fun nomCameraChoisie(reglages: Reglages): String? = cameraPersoChoisie(reglages)?.nom ?: cameraChoisie(reglages)?.nomComplet
fun nomObjectifChoisi(reglages: Reglages): String? = seriePersoChoisie(reglages)?.nom ?: objectifChoisi(reglages)?.nomComplet

/** Les modes de capteur de la caméra choisie : les siens, ou ceux relus chez le fabricant. */
private fun modesCameraChoisie(reglages: Reglages): List<ZoneUtile> =
    cameraPersoChoisie(reglages)?.modes ?: MaterielCine.modesDe(reglages.cineCamera)

private fun modeChoisi(reglages: Reglages): ZoneUtile? =
    modesCameraChoisie(reglages).firstOrNull { it.nom == reglages.cineModeCapteur }

/** Le nom du « capteur » noté avec les plans : pellicule et fenêtre, ou caméra et mode. */
private fun nomSupport(reglages: Reglages): String = if (reglages.cineSupport == "PELLICULE") {
    listOfNotNull(
        pelliculeChoisie(reglages)?.let { p -> p.nom + (if (p.codes.isNotBlank()) " " + p.codes else "") },
        formatChoisi(reglages)?.nom,
        nomCameraChoisie(reglages)
    ).joinToString(" · ")
} else {
    listOfNotNull(nomCameraChoisie(reglages), modeChoisi(reglages)?.nom).joinToString(" — ")
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
    seriePersoChoisie(reglages)?.focales?.takeIf { it.isNotEmpty() }
        ?: objectifChoisi(reglages)?.let { MaterielCine.lireFocales(it.focales).choix }
        ?: MaterielCine.FOCALES_COURANTES

/** L'onglet entier. */
@Composable
fun EcranOngletCinema(reglages: Reglages, etat: EtatApplication, cinema: DepotCinema, journal: DepotJournal) {
    val vue = runCatching { VueCinema.valueOf(reglages.cineVue) }.getOrDefault(VueCinema.VISEUR)
    val choisir: (VueCinema) -> Unit = { reglages.cineVue = it.name }

    if (vue == VueCinema.VISEUR) {
        EcranMesurer(
            reglages, etat, cinema,
            modeCinema = true,
            enTete = { EnTeteCinema(reglages, vue, sombre = true, surVue = choisir) },
            enTeteVertical = { SelecteurVueVertical(vue, choisir) }
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

/** Le sélecteur en colonne, pour le paysage : trois gélules l'une sous l'autre. */
@Composable
private fun SelecteurVueVertical(vue: VueCinema, surVue: (VueCinema) -> Unit) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VueCinema.entries.forEach { v ->
            val actif = v == vue
            Box(
                Modifier
                    .width(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (actif) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { surVue(v) }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    v.libelle,
                    style = Corps,
                    maxLines = 1,
                    color = if (actif) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Le sélecteur Viseur | Plans | Matériel, et le matériel en cours sur une ligne (touche : Matériel). */
@Composable
private fun EnTeteCinema(reglages: Reglages, vue: VueCinema, sombre: Boolean, surVue: (VueCinema) -> Unit) {
    ChoixSegmente(VueCinema.entries.toList(), vue, { it.libelle }) { surVue(it) }
    /* Dans le viseur, la place est à l'image : le résumé passe dans les détails. */
    if (!sombre) Text(
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
 * En haut du viseur de tournage, une seule pastille : « 50 mm · 39° ».
 * Touchée, elle déplie les focales — ou les angles de champ, dont Cellule
 * déduit la focale pour le capteur du cadre de travail — puis se replie dès
 * qu'on a choisi. Le cadre (ou le recadrage) suit aussitôt.
 */
@Composable
fun BoxScope.BarreFocale(reglages: Reglages, surMateriel: () -> Unit) {
    val cadre = remember(reglages.cineCadre) { cadreDe(reglages) }
    var ouverte by remember { mutableStateOf(false) }
    var parAngle by remember { mutableStateOf(false) }
    val largeur = cadre.capteurLargeurMm?.takeIf { it > 0 }?.let { it * cadre.desanamorphose }
    val hauteur = cadre.capteurHauteurMm?.takeIf { it > 0 }
    val focale = cadre.focaleMm
    val choisir: (Double) -> Unit = { f ->
        ecrireCadre(reglages, cadre.copy(focaleMm = f))
        ouverte = false
    }

    val resume = when {
        largeur == null || hauteur == null -> "Capteur ?"
        focale == null -> "Focale ?"
        else -> nombreCourt(focale, 1) + " mm · " + nombreCourt(Focales.angleDeChamp(largeur, focale), 0) + "°"
    }

    Column(
        Modifier
            .align(Alignment.TopEnd)
            /* Au-dessus de l'aperçu, même quand celui-ci est agrandi. */
            .zIndex(2f)
            .then(if (fr.cellule.app.LocalPaysage.current) Modifier.statusBarsPadding() else Modifier)
            .padding(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PastilleViseur(resume, actif = ouverte) {
                if (largeur == null || hauteur == null) surMateriel() else ouverte = !ouverte
            }
            if (ouverte) {
                PastilleViseur(if (parAngle) "par l'angle" else "par la focale", actif = false) { parAngle = !parAngle }
            }
        }
        if (ouverte && largeur != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .horizontalScroll(rememberScrollState())
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (!parAngle) {
                    focalesProposees(reglages).forEach { f ->
                        PastilleViseur(nombreCourt(f, 1), actif = focale != null && abs(focale - f) < 0.05) { choisir(f) }
                    }
                } else {
                    val angleActuel = focale?.let { Focales.angleDeChamp(largeur, it) }
                    MaterielCine.ANGLES_COURANTS.forEach { a ->
                        PastilleViseur(nombreCourt(a, 0) + "°", actif = angleActuel != null && abs(angleActuel - a) < 0.5) {
                            val f = MaterielCine.focalePourAngle(largeur, a)
                            if (!f.isNaN()) choisir(Math.round(f * 10) / 10.0)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Le haut des détails du viseur Cinéma : ce qu'on règle en premier — la
 * caméra du téléphone, le mode de mesure, recadré ou repérage, le matériel.
 */
@Composable
fun ReglagesViseurCinema(
    reglages: Reglages,
    moteur: fr.cellule.app.mesure.MoteurCamera,
    mode: ModeMesure,
    surMode: (ModeMesure) -> Unit
) {
    Etiquette("Caméra du téléphone")
    Spacer(Modifier.height(6.dp))
    val cameras = moteur.cameras
    if (cameras.size > 1) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            cameras.forEach { c ->
                Pastille(c.libelle, accent = c.id == moteur.cameraActive) { moteur.choisirCamera(c.id) }
            }
        }
        Text(
            "Focales équivalentes 24×36 de chaque module, d'après ce que le téléphone déclare. En mode recadré, Cellule zoome d'elle-même : choisis le module dont la focale est juste en dessous de celle que tu simules.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    } else {
        Text(
            "Ce téléphone ne montre qu'une caméra arrière aux applications" +
                (cameras.firstOrNull()?.equivalent?.let { " (équivalent ${Math.round(it)} mm)" } ?: "") +
                ". Ses autres modules, s'il en a, restent réservés à son appli photo : Cellule ne peut pas les ouvrir.",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        (moteur.imagesParSeconde?.let { "Aperçu à $it images par seconde" } ?: "Aperçu à la cadence du téléphone") +
            " : la cadence de tournage choisie ne sert qu'aux calculs d'exposition, pour que l'image reste fluide quand tu bouges.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
    if (moteur.zoomMaximal > moteur.zoomMinimal) {
        Text(
            "Zoom en cours ×" + nombreCourt(moteur.zoomDemande.toDouble(), 1) +
                " (de ×" + nombreCourt(moteur.zoomMinimal.toDouble(), 1) + " à ×" + nombreCourt(moteur.zoomMaximal.toDouble(), 1) + ")",
            style = Detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    if (moteur.avisCamera.isNotBlank()) {
        Text(moteur.avisCamera, style = Detail, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp))
    }

    Separateur()
    Etiquette("Image")
    Spacer(Modifier.height(6.dp))
    ChoixSegmente(listOf(true, false), reglages.cineRecadre, { if (it) "Recadré à la focale" else "Repérage (image entière)" }) {
        reglages.cineRecadre = it
    }
    Text(
        if (reglages.cineRecadre) "Le viseur ne montre que ce que voit ta caméra avec cette focale, et la photo rangée dans la galerie est recadrée pareil. Au-delà du zoom du téléphone, l'image perd en netteté."
        else "Le viseur montre toute l'image du téléphone, le cadre de ta caméra tracé dessus ; la photo garde l'image entière.",
        style = Detail,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )

    Separateur()
    Etiquette("Mesure")
    Spacer(Modifier.height(6.dp))
    ChoixSegmente(ModeMesure.entries.toList(), mode, { it.libelle }, surMode)
    if (mode == ModeMesure.SPOT) {
        Spacer(Modifier.height(4.dp))
        Text("Taille du spot", style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.material3.Slider(
            value = moteur.rayon,
            onValueChange = { moteur.reglerRayon(it) },
            valueRange = 0.02f..0.25f
        )
    }

    Separateur()
    Etiquette("Matériel")
    Spacer(Modifier.height(4.dp))
    Text(resumeMateriel(reglages), style = Corps, color = MaterialTheme.colorScheme.onSurface)
    BoutonPlat("Changer le matériel", modifier = Modifier.padding(top = 6.dp)) { reglages.cineVue = VueCinema.MATERIEL.name }
    Separateur()
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
    CarteMonMateriel()

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
        val cles = listOf(AUCUNE) + MonMateriel.cameras.map { MonMateriel.cle(it) } + cameras.map { it.nom }
        Deroulant("Caméra (facultatif)", cles, reglages.cineCamera.takeIf { it in cles } ?: AUCUNE, { n ->
            when {
                n == AUCUNE -> "Aucune caméra"
                n.startsWith(MonMateriel.PREFIXE) -> "★ " + (MonMateriel.camera(n)?.nom ?: "")
                else -> cameras.first { it.nom == n }.nomComplet
            }
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
    val perso = cameraPersoChoisie(reglages)
    val modes = modesCameraChoisie(reglages)
    val mode = modeChoisi(reglages)
    val cles = listOf(AUCUNE) + MonMateriel.cameras.map { MonMateriel.cle(it) } + cameras.map { it.nom }
    Carte(titre = "Caméra", sousTitre = "Facultatif : sans caméra, tu saisis la taille du capteur à la main dans le cadre de travail. ★ : ton matériel.") {
        Deroulant("Caméra", cles, reglages.cineCamera.takeIf { it in cles } ?: AUCUNE, { n ->
            when {
                n == AUCUNE -> "Aucune caméra"
                n.startsWith(MonMateriel.PREFIXE) -> "★ " + (MonMateriel.camera(n)?.nom ?: "")
                else -> cameras.first { it.nom == n }.let { c ->
                    c.nomComplet + if (MaterielCine.modesDe(c.nom).isEmpty()) "  (capteur à saisir)" else ""
                }
            }
        }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineCamera = if (nom == AUCUNE) "" else nom
            reglages.cineModeCapteur = modesCameraChoisie(reglages).firstOrNull()?.nom ?: ""
            reporterSupport(reglages)
        }
        if (perso != null && modes.isEmpty()) {
            Spacer(Modifier.height(Interligne))
            Text(
                "Ta caméra n'a pas encore de mode de capteur : ajoute-le dans « Mon matériel », plus bas.",
                style = Detail,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (perso != null && modes.isNotEmpty()) {
            Spacer(Modifier.height(Interligne))
            Deroulant("Mode de capteur", modes.map { it.nom }, mode?.nom ?: modes.first().nom, { it }, Modifier.fillMaxWidth()) { nom ->
                reglages.cineModeCapteur = nom
                reporterSupport(reglages)
            }
            (mode ?: modes.first()).let {
                Spacer(Modifier.height(6.dp))
                Ligne("Zone utile", it.dimensions, it.source, accent = true)
            }
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
    val seriePerso = seriePersoChoisie(reglages)
    val cles = listOf(AUCUNE) + MonMateriel.series.map { MonMateriel.cle(it) } + CATALOGUE_OBJECTIFS.map { it.nomComplet }
    Carte(titre = "Objectifs", sousTitre = "Facultatif : la série choisie donne les focales proposées en haut du viseur. ★ : ton matériel.") {
        Deroulant("Série d'objectifs", cles, reglages.cineObjectif.takeIf { it in cles } ?: AUCUNE, {
            when {
                it == AUCUNE -> "Aucune série (focales courantes)"
                it.startsWith(MonMateriel.PREFIXE) -> "★ " + (MonMateriel.serie(it)?.nom ?: "")
                else -> it
            }
        }, Modifier.fillMaxWidth()) { nom ->
            reglages.cineObjectif = if (nom == AUCUNE) "" else nom
            val c = cadreDe(reglages)
            val choix = focalesProposees(reglages)
            /* Une focale hors de la série est ramenée à la plus proche qu'elle propose. */
            val focale = c.focaleMm?.let { f -> choix.minByOrNull { abs(it - f) } }
            val sienne = seriePersoChoisie(reglages)
            ecrireCadre(
                reglages,
                c.copy(
                    objectif = if (nom == AUCUNE) "" else nomObjectifChoisi(reglages) ?: "",
                    focaleMm = focale ?: c.focaleMm,
                    /* Sa série anamorphique : le facteur saisi passe au cadre. */
                    desanamorphose = sienne?.desanamorphose ?: c.desanamorphose
                )
            )
        }
        if (seriePerso != null) {
            Spacer(Modifier.height(6.dp))
            Ligne(
                "Focales",
                seriePerso.focales.takeIf { it.isNotEmpty() }?.joinToString(", ") { nombreCourt(it, 1) }?.let { "$it mm" } ?: "aucune saisie",
                MonMateriel.SAISI
            )
            seriePerso.tMax?.let { Ligne("Ouverture", "T" + nombreCourt(it, 1), MonMateriel.SAISI) }
            if (seriePerso.desanamorphose != 1.0) {
                Ligne("Désanamorphose", "×" + nombreCourt(seriePerso.desanamorphose, 1), "reportée dans le cadre de travail")
            }
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
