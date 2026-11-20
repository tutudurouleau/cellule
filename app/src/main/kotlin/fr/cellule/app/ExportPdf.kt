package fr.cellule.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import fr.cellule.app.ecrans.PhotosPlans
import fr.cellule.app.sol.ImageSol
import fr.cellule.app.mesure.TuilesOsm
import fr.cellule.app.mesure.reseauDisponible
import fr.cellule.core.Boussole
import fr.cellule.core.Cadreur
import fr.cellule.core.CarnetCinema
import fr.cellule.core.CarnetCinemaDonnees
import fr.cellule.core.JourCinema
import fr.cellule.core.Plan
import fr.cellule.core.PlanSol
import fr.cellule.core.PriseCamera
import fr.cellule.core.RapportCamera
import fr.cellule.core.VerdictPrise
import fr.cellule.core.Tuiles
import fr.cellule.core.nombreCourt
import java.io.File
import java.util.Locale

/**
 * Les PDF de plans, fabriqués sur le téléphone, sans réseau : un plan seul,
 * ou un dossier (un jour, un projet) avec page de garde et sommaire.
 *
 * Format A4, en couleur, sans logo. Le lien Google Maps est écrit en toutes
 * lettres : les lecteurs de PDF le rendent cliquable. La mini-carte n'est
 * ajoutée que s'il y a du réseau (ou des tuiles déjà en cache) au moment de
 * l'export ; sinon, coordonnées et lien seulement.
 */
object ExportPdf {

    private const val LARGEUR = 595
    private const val HAUTEUR = 842
    private const val MARGE = 40f
    private const val UTILE = LARGEUR - 2 * MARGE

    private const val ENCRE = 0xFF1A1714.toInt()
    private const val GRIS = 0xFF6E665A.toInt()
    private const val ACCENT = 0xFF8F4A05.toInt()
    private const val LIEN = 0xFF2F5597.toInt()
    private const val FILET = 0xFFD4CBBD.toInt()
    private val COULEURS_FORMATS = intArrayOf(
        0xFFF0A93F.toInt(), 0xFF6FD3F2.toInt(), 0xFFF07FCF.toInt(), 0xFF9BE06B.toInt(), 0xFFF2E26F.toInt()
    )

    private fun pinceau(taille: Float, couleur: Int = ENCRE, gras: Boolean = false): TextPaint =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = taille
            color = couleur
            typeface = if (gras) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

    /** Une page en cours d'écriture : on y descend bloc après bloc. */
    private class Redaction(val document: PdfDocument, val piedDePage: String) {
        var numero = 0
        private var page: PdfDocument.Page? = null
        lateinit var toile: Canvas
        var y = MARGE

        fun nouvellePage() {
            fermer()
            numero++
            val p = document.startPage(PdfDocument.PageInfo.Builder(LARGEUR, HAUTEUR, numero).create())
            page = p
            toile = p.canvas
            y = MARGE
        }

        fun fermer() {
            val p = page ?: return
            val pied = pinceau(8f, GRIS)
            toile.drawText(piedDePage, MARGE, HAUTEUR - 22f, pied)
            val n = numero.toString()
            toile.drawText(n, LARGEUR - MARGE - pied.measureText(n), HAUTEUR - 22f, pied)
            document.finishPage(p)
            page = null
        }

        private fun mise(texte: String, p: TextPaint, largeur: Float): StaticLayout =
            StaticLayout.Builder.obtain(texte, 0, texte.length, p, largeur.toInt().coerceAtLeast(20))
                .setLineSpacing(1.5f, 1f)
                .build()

        fun hauteur(texte: String, p: TextPaint, largeur: Float = UTILE): Float = mise(texte, p, largeur).height.toFloat()

        /** Écrit un paragraphe à la position courante et descend d'autant. */
        fun texte(texte: String, p: TextPaint, x: Float = MARGE, largeur: Float = UTILE, apres: Float = 0f) {
            if (texte.isBlank()) return
            val bloc = mise(texte, p, largeur)
            toile.save()
            toile.translate(x, y)
            bloc.draw(toile)
            toile.restore()
            y += bloc.height + apres
        }

        fun filet(apres: Float = 8f) {
            val p = Paint().apply { color = FILET; strokeWidth = 0.7f }
            toile.drawLine(MARGE, y, LARGEUR - MARGE, y, p)
            y += apres
        }

        fun reste(): Float = HAUTEUR - MARGE - 24f - y

        /** Un intitulé de bloc, en petites capitales d'accent. */
        fun rubrique(titre: String) {
            y += 6f
            texte(titre.uppercase(Locale.FRANCE), pinceau(8.5f, ACCENT, gras = true), apres = 3f)
        }

        /** Une ligne « intitulé : valeur », la valeur pouvant courir sur plusieurs lignes. */
        fun ligne(intitule: String, valeur: String, largeur: Float = UTILE, couleur: Int = ENCRE) {
            if (valeur.isBlank()) return
            val colonne = 92f
            val depart = y
            texte(intitule, pinceau(9.5f, GRIS), largeur = colonne)
            val apresIntitule = y
            y = depart
            texte(valeur, pinceau(9.5f, couleur), x = MARGE + colonne, largeur = largeur - colonne)
            y = maxOf(y, apresIntitule) + 2.5f
        }
    }

    /** La photo et ses cadres, inscrits dans la zone donnée. Rend la hauteur occupée. */
    private fun dessinerPhoto(toile: Canvas, photo: Bitmap, plan: Plan, x: Float, y: Float, largeur: Float, hauteurMax: Float): Float {
        val echelle = minOf(largeur / photo.width, hauteurMax / photo.height)
        val l = photo.width * echelle
        val h = photo.height * echelle
        val gauche = x + (largeur - l) / 2
        val zone = RectF(gauche, y, gauche + l, y + h)
        toile.drawBitmap(photo, null, zone, Paint(Paint.FILTER_BITMAP_FLAG))

        val trace = plan.champ?.let { Cadreur.tracer(it, plan.cadre) }
        if (trace != null && !trace.tropLarge) {
            fun rect(r: fr.cellule.core.RectCadre) = RectF(
                zone.left + (r.gauche * l).toFloat(), zone.top + (r.haut * h).toFloat(),
                zone.left + (r.droite * l).toFloat(), zone.top + (r.bas * h).toFloat()
            )
            val capteur = rect(trace.capteur)
            val cache = Paint().apply { color = 0x80000000.toInt() }
            toile.drawRect(zone.left, zone.top, zone.right, capteur.top, cache)
            toile.drawRect(zone.left, capteur.bottom, zone.right, zone.bottom, cache)
            toile.drawRect(zone.left, capteur.top, capteur.left, capteur.bottom, cache)
            toile.drawRect(capteur.right, capteur.top, zone.right, capteur.bottom, cache)
            val trait = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = android.graphics.Color.WHITE
            }
            toile.drawRect(capteur, trait)
            trace.formats.forEachIndexed { i, format ->
                val couleur = COULEURS_FORMATS[i % COULEURS_FORMATS.size]
                val r = rect(format.rect)
                trait.color = couleur
                trait.strokeWidth = 1.4f
                toile.drawRect(r, trait)
                val etiquette = pinceau(8f, couleur, gras = true).apply {
                    setShadowLayer(2f, 0f, 0f, android.graphics.Color.BLACK)
                }
                toile.drawText(format.nom, r.left + 4f, r.top + 11f + i * 10f, etiquette)
            }
        }
        return h
    }

    /** La page d'un plan : grande photo, caméra et exposition, lieu, notes. */
    private fun pagePlan(
        contexte: Context, r: Redaction, plan: Plan, enTete: String, grandCote: Int, avecCarte: Boolean
    ) {
        r.nouvellePage()
        if (enTete.isNotBlank()) r.texte(enTete, pinceau(8.5f, GRIS), apres = 2f)
        r.texte(plan.reference, pinceau(19f, ENCRE, gras = true), apres = 1f)
        if (plan.titre.isNotBlank()) r.texte(plan.titre, pinceau(12.5f, ENCRE), apres = 1f)
        r.texte(listOf(plan.date, plan.heure).filter { it.isNotBlank() }.joinToString(" · "), pinceau(9.5f, GRIS), apres = 8f)

        val photo = PhotosPlans.charger(contexte, plan.photo, grandCote)
        if (photo != null) {
            val h = dessinerPhoto(r.toile, photo, plan, MARGE, r.y, UTILE, 330f)
            r.y += h + 6f
        } else {
            r.texte(
                if (plan.photo.isBlank()) "Pas de photo pour ce plan."
                else "Photo introuvable dans la galerie de ce téléphone au moment de l'export.",
                pinceau(9.5f, GRIS), apres = 6f
            )
        }

        val trace = plan.champ?.let { Cadreur.tracer(it, plan.cadre) }
        val avisCadre = when {
            !plan.cadre.complet -> ""
            plan.champ == null -> "Le champ de la photo est inconnu : le cadre n'y est pas tracé."
            trace != null && trace.tropLarge ->
                "Trop large pour le téléphone : la photo ne montre rien de plus large que " +
                    nombreCourt(trace.focaleMinimale, 1) + " mm sur ce capteur. Le cadre n'est pas tracé."
            else -> ""
        }
        r.texte(avisCadre, pinceau(8.5f, GRIS), apres = 4f)

        /* ── Caméra, optique, filtres, exposition ──────────────────────── */
        r.rubrique("Caméra et optique")
        val c = plan.cadre
        r.ligne("Caméra", c.capteurNom)
        if (c.capteurLargeurMm != null && c.capteurHauteurMm != null) {
            r.ligne(
                "Zone utile",
                nombreCourt(c.capteurLargeurMm!!, 2) + " × " + nombreCourt(c.capteurHauteurMm!!, 2) + " mm"
            )
        }
        r.ligne(
            "Focale",
            listOfNotNull(
                c.focaleMm?.let { nombreCourt(it, 1) + " mm" },
                if (c.desanamorphose != 1.0) "anamorphique ×" + nombreCourt(c.desanamorphose, 2) else null
            ).joinToString(" · ")
        )
        if (trace != null) {
            r.ligne("Champ", nombreCourt(trace.champHorizontal, 1) + "° × " + nombreCourt(trace.champVertical, 1) + "°")
        }
        r.ligne("Formats", c.formats.joinToString(" · "))
        r.ligne("Téléphone", plan.cameraTelephone)

        r.rubrique("Exposition")
        val e = plan.exposition
        r.ligne("Mesure", e.ev100?.let { "EV " + String.format(Locale.FRANCE, "%.1f", it) + " à 100 ISO" } ?: "")
        r.ligne("Réglages", e.resume)
        r.ligne("Filtres", e.filtres)

        /* ── Le lieu ───────────────────────────────────────────────────── */
        r.rubrique("Lieu")
        val lieu = plan.lieu
        val carte = if (avecCarte && lieu != null) TuilesOsm.vignette(contexte, lieu.latitude, lieu.longitude) else null
        val coteCarte = 118f
        val largeurTexte = if (carte != null) UTILE - coteCarte - 14f else UTILE
        val hautDuLieu = r.y
        if (lieu != null) {
            r.ligne("Adresse", lieu.adresse, largeurTexte)
            r.ligne("Coordonnées", lieu.coordonnees, largeurTexte)
            r.ligne(
                "Précision",
                listOfNotNull(
                    lieu.precisionM?.let { "± " + nombreCourt(it, 0) + " m" },
                    lieu.altitudeM?.let { "altitude " + nombreCourt(it, 0) + " m" }
                ).joinToString(" · "),
                largeurTexte
            )
            r.ligne("Carte", lieu.lienMaps, largeurTexte, LIEN)
        } else {
            r.ligne("Position", "non relevée", largeurTexte)
        }
        r.ligne(
            "Boussole",
            plan.cap?.let {
                Boussole.libelle(it) + if (lieu == null) " (nord magnétique)" else " (direction de visée)"
            } ?: "",
            largeurTexte
        )
        r.ligne("Soleil", plan.soleil?.let { it.resume + " — calculé" } ?: "", largeurTexte)
        r.ligne("Heure", listOf(plan.date, plan.heure).filter { it.isNotBlank() }.joinToString(" à "), largeurTexte)
        if (carte != null) {
            val gauche = LARGEUR - MARGE - coteCarte
            r.toile.drawBitmap(carte, null, RectF(gauche, hautDuLieu, gauche + coteCarte, hautDuLieu + coteCarte), Paint(Paint.FILTER_BITMAP_FLAG))
            r.toile.drawText(Tuiles.MENTION, gauche, hautDuLieu + coteCarte + 9f, pinceau(6.5f, GRIS))
            r.y = maxOf(r.y, hautDuLieu + coteCarte + 14f)
        }

        /* ── Les notes ─────────────────────────────────────────────────── */
        if (plan.notes.isNotBlank()) {
            val corps = pinceau(10f)
            if (r.hauteur(plan.notes, corps) + 30f > r.reste()) {
                r.nouvellePage()
                r.texte(plan.reference + " — suite", pinceau(8.5f, GRIS), apres = 4f)
            }
            r.rubrique("Notes")
            r.texte(plan.notes, corps)
        }

        /* ── Le raccord lumière ────────────────────────────────────────── */
        RaccordsLumiere.du(plan.identifiant)?.let { raccord ->
            val mesures = raccord.mesures.filter { it.lux != null }
            if (mesures.isEmpty() && raccord.sources.isEmpty() && raccord.notes.isBlank() && raccord.ciel.isBlank()) return@let
            if (r.reste() < 90f) {
                r.nouvellePage()
                r.texte(plan.reference + " — suite", pinceau(8.5f, GRIS), apres = 4f)
            }
            r.rubrique("Raccord lumière")
            r.ligne("Moment", listOf(raccord.heure, raccord.ciel).filter { it.isNotBlank() }.joinToString(" · "))
            mesures.forEach { m ->
                if (r.reste() < 20f) r.nouvellePage()
                r.ligne(m.endroit.ifBlank { "Mesure" }, nombreCourt(m.lux ?: 0.0, 0) + " lx" + if (m.note.isNotBlank()) " — " + m.note else "")
            }
            fr.cellule.core.Raccord.rapport(raccord)?.let { r.ligne("Face / débouchage", nombreCourt(it, 1) + " : 1") }
            raccord.sources.filter { it.nom.isNotBlank() || it.reglage.isNotBlank() }.forEach { s ->
                if (r.reste() < 20f) r.nouvellePage()
                r.ligne(s.nom.ifBlank { "Source" }, s.reglage)
            }
            if (raccord.notes.isNotBlank()) r.ligne("Notes", raccord.notes)
        }
    }

    private fun fichier(contexte: Context, nom: String): File {
        val dossier = File(contexte.cacheDir, "pdf").apply { mkdirs() }
        /* Les anciens exports ne servent plus une fois partagés. */
        dossier.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24L * 3600 * 1000 }
            ?.forEach { it.delete() }
        val propre = nom.replace(Regex("[^\\p{L}\\p{N} _-]+"), " ").trim().replace(Regex("\\s+"), "-").take(60)
        return File(dossier, propre.ifBlank { "plan" } + ".pdf")
    }

    /** Le PDF d'un plan seul. Bloquant : à appeler hors du fil principal. */
    fun planSeul(contexte: Context, plan: Plan, donnees: CarnetCinemaDonnees): File {
        val document = PdfDocument()
        val r = Redaction(document, "Cellule — fiche de plan")
        val jour = donnees.jours.firstOrNull { it.identifiant == plan.jourId }
        val projet = jour?.let { j -> donnees.projets.firstOrNull { it.identifiant == j.projetId } }
        val enTete = listOfNotNull(projet?.nom?.takeIf { it.isNotBlank() }, jour?.intitule).joinToString(" › ")
        pagePlan(contexte, r, plan, enTete, 1400, reseauDisponible(contexte))
        r.fermer()
        val sortie = fichier(contexte, "Plan " + plan.reference + " " + plan.date)
        sortie.outputStream().use { document.writeTo(it) }
        document.close()
        return sortie
    }

    /**
     * Le dossier d'un jour ou d'un projet : page de garde (projet, dates,
     * lieux), sommaire, puis une page par plan.
     */
    fun dossier(
        contexte: Context, titre: String, sousTitre: String,
        plans: List<Plan>, jours: List<JourCinema>, vues: List<String> = emptyList()
    ): File {
        val document = PdfDocument()
        val r = Redaction(document, "Cellule — $titre" + if (sousTitre.isNotBlank()) " · $sousTitre" else "")
        val avecCarte = reseauDisponible(contexte)
        val nomDuJour = jours.associate { it.identifiant to it.intitule }

        /* ── Page de garde ─────────────────────────────────────────────── */
        r.nouvellePage()
        r.y = 150f
        r.texte("DOSSIER DE PLANS", pinceau(10f, ACCENT, gras = true), apres = 10f)
        r.texte(titre, pinceau(30f, ENCRE, gras = true), apres = 6f)
        r.texte(sousTitre, pinceau(15f, GRIS), apres = 26f)
        r.filet(14f)
        r.ligne("Dates", CarnetCinema.periode(plans, jours))
        r.ligne("Plans", plans.size.toString())
        if (jours.size > 1) r.ligne("Jours", jours.joinToString("\n") { it.intitule + " — " + it.type.libelle.lowercase(Locale.FRANCE) })
        val lieux = CarnetCinema.lieux(plans)
        r.ligne("Lieux", if (lieux.isEmpty()) "aucune position relevée" else lieux.take(12).joinToString("\n"))
        if (vues.isNotEmpty()) r.ligne("Vues photo", vues.take(20).joinToString("\n"))
        r.y += 10f
        r.texte(
            if (avecCarte) "Les mini-cartes viennent d'OpenStreetMap (${Tuiles.MENTION})."
            else "Exporté sans réseau : pas de mini-carte, les coordonnées et le lien suffisent à retrouver chaque lieu.",
            pinceau(8.5f, GRIS)
        )

        /* ── Sommaire ──────────────────────────────────────────────────── */
        val parPage = 34
        val pagesSommaire = maxOf(1, (plans.size + parPage - 1) / parPage)
        plans.chunked(parPage).ifEmpty { listOf(emptyList()) }.forEachIndexed { rang, morceau ->
            r.nouvellePage()
            r.texte(if (rang == 0) "Sommaire" else "Sommaire (suite)", pinceau(17f, ENCRE, gras = true), apres = 10f)
            r.filet(8f)
            morceau.forEachIndexed { i, plan ->
                val index = rang * parPage + i
                val page = (1 + pagesSommaire + index + 1).toString()
                val gras = pinceau(10f, ENCRE)
                val depart = r.y
                val intitule = plan.intitule + (plan.jourId?.let { id -> nomDuJour[id] }?.takeIf { jours.size > 1 }?.let { "   ($it)" } ?: "")
                r.texte(intitule, gras, largeur = UTILE - 40f)
                r.toile.drawText(page, LARGEUR - MARGE - gras.measureText(page), depart + 11f, gras)
                r.y += 5f
            }
        }

        /* ── Une page par plan ─────────────────────────────────────────── */
        plans.forEach { plan ->
            val enTete = listOfNotNull(titre.takeIf { it.isNotBlank() }, plan.jourId?.let { nomDuJour[it] }).joinToString(" › ")
            pagePlan(contexte, r, plan, enTete, 1000, avecCarte)
        }

        /* ── Les plans au sol des jours du dossier ─────────────────────── */
        PlansSol.plans.filter { s -> jours.any { it.identifiant == s.jourId } }.forEach { s ->
            val enTete = listOfNotNull(titre.takeIf { it.isNotBlank() }, s.jourId?.let { nomDuJour[it] }).joinToString(" › ")
            pagePlanSol(contexte, r, s, enTete, metres = true)
        }
        r.fermer()
        val sortie = fichier(contexte, "Dossier $titre $sousTitre")
        sortie.outputStream().use { document.writeTo(it) }
        document.close()
        return sortie
    }

    /* ── Plans au sol ───────────────────────────────────────────────────── */

    private fun pagePlanSol(contexte: Context, r: Redaction, plan: PlanSol, enTete: String, metres: Boolean) {
        r.nouvellePage()
        if (enTete.isNotBlank()) r.texte(enTete, pinceau(8.5f, GRIS), apres = 4f)
        r.texte("PLAN AU SOL", pinceau(8.5f, ACCENT, gras = true), apres = 2f)
        r.texte(plan.nom, pinceau(18f, ENCRE, gras = true), apres = 8f)
        val image = ImageSol.rendre(contexte, plan, 1800, 1300, metres, plan.nom, enTete)
        val h = UTILE * image.height / image.width
        r.toile.drawBitmap(image, null, RectF(MARGE, r.y, MARGE + UTILE, r.y + h), Paint(Paint.FILTER_BITMAP_FLAG))
        image.recycle()
        r.y += h + 12f
        ImageSol.infos(plan).forEach { (intitule, valeur) ->
            if (r.reste() < 30f) r.nouvellePage()
            r.ligne(intitule, valeur)
        }

        /* Ce qu'il faut louer, et ce que porte chaque circuit. */
        val location = fr.cellule.core.ListeTournage.complete(plan, PlansSol::modele, { MonMateriel.projecteur(it.projecteur) }, PlansSol::puissance)
        val charges = fr.cellule.core.ListeLocation.charges(plan, PlansSol::puissance, PlansSol::modele)
        if (location.isNotEmpty()) {
            if (r.reste() < 80f) r.nouvellePage()
            r.rubrique("À louer")
            location.forEach {
                if (r.reste() < 20f) r.nouvellePage()
                r.ligne(it.categorie, it.nom + "   " + it.combien)
            }
        }
        if (charges.isNotEmpty()) {
            if (r.reste() < 80f) r.nouvellePage()
            r.rubrique("Électricité")
            charges.forEach { c ->
                if (r.reste() < 20f) r.nouvellePage()
                r.ligne(
                    if (c.circuit == fr.cellule.core.ListeLocation.SANS_CIRCUIT) "Sans circuit" else "Circuit " + c.circuit,
                    nombreCourt(c.puissanceW, 0) + " W — " + c.sources.joinToString(", ") +
                        if (c.sansPuissance > 0) " (" + c.sansPuissance + " sans puissance connue)" else ""
                )
            }
            val total = charges.sumOf { it.puissanceW }
            r.ligne("Total", nombreCourt(total, 0) + " W · groupe d'au moins " + nombreCourt(fr.cellule.core.ListeLocation.groupeKva(total), 1) + " kVA")
        }

        /* Ce que voit chaque caméra du plan, en perspective. */
        val cameras = plan.elements.filter { it.type == fr.cellule.core.TypeElement.CAMERA && fr.cellule.core.CameraVirtuelle.depuis(it) != null }
        cameras.take(6).forEach { cam ->
            val rapport = ((cam.capteurLargeurMm ?: 1.78) * cam.desanamorphose / (cam.capteurHauteurMm ?: 1.0)).coerceIn(0.5, 3.0)
            val hauteurImage = (UTILE / rapport).toFloat()
            if (r.reste() < hauteurImage + 40f) r.nouvellePage()
            r.rubrique("Vue caméra " + cam.libelle)
            val vue = ImageSol.vueCamera(plan, cam, 1400, (1400 / rapport).toInt())
            r.toile.drawBitmap(vue, null, RectF(MARGE, r.y, MARGE + UTILE, r.y + hauteurImage), Paint(Paint.FILTER_BITMAP_FLAG))
            vue.recycle()
            r.y += hauteurImage + 8f
        }
    }

    /** Le PDF d'un plan au sol seul. Bloquant : à appeler hors du fil principal. */
    fun planSol(contexte: Context, plan: PlanSol, enTete: String, metres: Boolean): File {
        val document = PdfDocument()
        val r = Redaction(document, "Cellule — plan au sol")
        pagePlanSol(contexte, r, plan, enTete, metres)
        r.fermer()
        val sortie = fichier(contexte, "Plan au sol " + plan.nom)
        sortie.outputStream().use { document.writeTo(it) }
        document.close()
        return sortie
    }

    /**
     * Le rapport caméra d'un jour : une ligne par prise, en tableau, les
     * titres de colonnes repris en haut de chaque page. Bloquant.
     */
    fun rapportCamera(contexte: Context, prises: List<PriseCamera>, enTete: String, jour: String): File {
        val document = PdfDocument()
        val r = Redaction(document, "Cellule — rapport caméra")
        val colonnes = listOf(
            "Séq. · plan · prise" to 96f, "Cam." to 30f, "Objectif" to 60f, "T" to 32f, "Filtres" to 66f,
            "EI · im/s · obt." to 78f, "Carte" to 40f, "Prise" to 40f, "Remarques" to (UTILE - 442f)
        )
        val corps = pinceau(8.5f)
        val titre = pinceau(7.5f, GRIS, gras = true)
        fun ecrire(cellules: List<String>, p: TextPaint): Float {
            val hauteur = cellules.zip(colonnes).maxOf { (t, c) -> r.hauteur(t.ifBlank { " " }, p, c.second - 4f) }
            val depart = r.y
            var x = MARGE
            cellules.zip(colonnes).forEach { (t, c) ->
                r.y = depart
                r.texte(t, p, x = x, largeur = c.second - 4f)
                x += c.second
            }
            r.y = depart + hauteur + 3f
            r.filet(apres = 3f)
            return hauteur
        }
        fun titres() = ecrire(colonnes.map { it.first }, titre)

        r.nouvellePage()
        if (enTete.isNotBlank()) r.texte(enTete, pinceau(8.5f, GRIS), apres = 4f)
        r.texte("RAPPORT CAMÉRA", pinceau(8.5f, ACCENT, gras = true), apres = 2f)
        r.texte(jour, pinceau(18f, ENCRE, gras = true), apres = 4f)
        r.texte(RapportCamera.bilan(prises), pinceau(9.5f, GRIS), apres = 10f)
        titres()
        prises.forEach { p ->
            val cellules = listOf(
                p.reference, p.camera, p.objectif, p.tStop, p.filtres,
                listOf(p.ei, p.cadence, p.obturateur.takeIf { it.isNotBlank() }?.let { "$it°" } ?: "").filter { it.isNotBlank() }.joinToString(" · "),
                p.carte,
                if (p.verdict == VerdictPrise.A_VOIR) "" else p.verdict.court,
                listOf(p.heure, p.remarques).filter { it.isNotBlank() }.joinToString(" — ")
            )
            val hauteur = cellules.zip(colonnes).maxOf { (t, c) -> r.hauteur(t.ifBlank { " " }, corps, c.second - 4f) }
            if (r.reste() < hauteur + 8f) {
                r.nouvellePage()
                r.texte(jour + " — suite", pinceau(8.5f, GRIS), apres = 6f)
                titres()
            }
            ecrire(cellules, corps)
        }
        r.fermer()
        val sortie = fichier(contexte, "Rapport camera " + jour)
        sortie.outputStream().use { document.writeTo(it) }
        document.close()
        return sortie
    }

    /** Ouvre la feuille de partage d'Android sur le PDF. */
    fun partager(contexte: Context, pdf: File) {
        val uri = FileProvider.getUriForFile(contexte, contexte.packageName + ".partage", pdf)
        val envoi = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, pdf.nameWithoutExtension)
            clipData = ClipData.newRawUri(pdf.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val choix = Intent.createChooser(envoi, "Partager le PDF").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (contexte !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexte.startActivity(choix)
    }
}
