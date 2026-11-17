package fr.cellule.app.sol

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import fr.cellule.app.MonMateriel
import fr.cellule.app.mesure.TuilesOsm
import fr.cellule.app.mesure.reseauDisponible
import fr.cellule.core.Boussole
import fr.cellule.core.PlanSol
import fr.cellule.core.Sol
import fr.cellule.core.TypeElement
import fr.cellule.core.nombreCourt
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Le plan au sol hors de l'écran : en image, et en lignes de texte pour le PDF. */
object ImageSol {

    /** Rend le plan en image. Bloquant (le fond de carte se télécharge) : hors du fil principal. */
    fun rendre(contexte: Context, plan: PlanSol, largeur: Int, hauteur: Int, metres: Boolean, titre: String, sousTitre: String): Bitmap {
        val enLigne = plan.carte && reseauDisponible(contexte)
        val options = OptionsDessin(
            texte = largeur / 50f,
            metres = metres,
            tuile = if (plan.carte) { z, x, y -> if (enLigne) TuilesOsm.tuile(contexte, z, x, y) else TuilesOsm.enMemoire(z, x, y) } else null,
            projecteurs = { MonMateriel.projecteur(it) }
        )
        return DessinSol.image(plan, largeur, hauteur, titre, sousTitre, options)
    }

    /** Ce qu'on lit sur le plan, en lignes « intitulé : valeur ». */
    fun infos(plan: PlanSol): List<Pair<String, String>> {
        val lignes = mutableListOf<Pair<String, String>>()
        val moment = listOf(plan.date, plan.heure).filter { it.isNotBlank() }.joinToString(" à ")
        if (moment.isNotBlank()) lignes.add("Moment" to moment)
        if (plan.soleil) {
            soleilDuPlan(plan)?.let { s ->
                lignes.add(
                    "Soleil" to if (s.hauteur < -0.8) "sous l'horizon"
                    else "azimut " + nombreCourt(s.azimut, 0) + "° (" + Boussole.cardinal(s.azimut) + "), hauteur " + nombreCourt(s.hauteur, 0) + "° — calculé, pas mesuré"
                )
            }
            val lat = plan.latitude
            val lon = plan.longitude
            if (lat != null && lon != null) {
                val jour = runCatching { LocalDate.parse(plan.date) }.getOrNull() ?: LocalDate.now()
                val fuseau = ZoneId.systemDefault().rules.getOffset(jour.atStartOfDay()).totalSeconds / 3600.0
                val h = Sol.heuresDuJour(lat, lon, jour.dayOfYear, fuseau)
                fun c(x: fr.cellule.core.Creneau?) = x?.let { Sol.libelleHeure(it.debut) + "–" + Sol.libelleHeure(it.fin) } ?: "—"
                lignes.add("Heure dorée" to "matin " + c(h.doreeMatin) + " · soir " + c(h.doreeSoir) + " (soleil entre −4° et +6°, par convention)")
                lignes.add("Heure bleue" to "matin " + c(h.bleueMatin) + " · soir " + c(h.bleueSoir) + " (entre −6° et −4°)")
            }
        }
        plan.elements.filter { it.type == TypeElement.CAMERA }.sortedWith(compareBy({ it.nom }, { it.etape })).forEach { e ->
            lignes.add(
                "Caméra " + e.libelle to listOfNotNull(
                    e.focaleMm?.let { nombreCourt(it, 1) + " mm" },
                    Sol.champHorizontal(e)?.let { "champ " + nombreCourt(it, 0) + "°" },
                    e.hauteur?.let { "objectif à " + nombreCourt(it, 2) + " m" },
                    e.plans.takeIf { it.isNotBlank() }?.let { "plans $it" }
                ).joinToString(" · ")
            )
        }
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.forEach { e ->
            val choisi = MonMateriel.projecteur(e.projecteur)
            lignes.add(
                "Projecteur" to listOfNotNull(
                    e.nom.ifBlank { choisi?.nomComplet ?: "sans nom" },
                    e.hauteur?.let { "à " + nombreCourt(it, 2) + " m" },
                    Sol.faisceau(e, choisi)?.let { "faisceau " + nombreCourt(it, 0) + "°" + if (e.spot) " (serré)" else "" },
                    e.pertesDiaphs.takeIf { it > 0 }?.let { "−" + nombreCourt(it, 1) + " diaph (gélatines, diffusion)" }
                ).joinToString(" · ")
            )
        }
        val comediens = plan.elements.filter { it.type == TypeElement.COMEDIEN }.groupBy { it.nom.ifBlank { "Comédien" } }
        if (comediens.isNotEmpty()) {
            lignes.add("Comédiens" to comediens.entries.joinToString(" · ") { (nom, pos) -> nom + if (pos.size > 1) " (${pos.size} positions)" else "" })
        }
        if (plan.notes.isNotBlank()) lignes.add("Notes" to plan.notes)
        return lignes
    }

    private fun dossier(contexte: Context): File = File(contexte.cacheDir, "images").apply {
        mkdirs()
        listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 24L * 3600 * 1000 }?.forEach { it.delete() }
    }

    /** Enregistre l'image dans le cache, prête à partager. Bloquant. */
    fun fichierImage(contexte: Context, plan: PlanSol, metres: Boolean, sousTitre: String): File {
        val image = rendre(contexte, plan, 2400, 1700, metres, plan.nom, sousTitre)
        val propre = plan.nom.replace(Regex("[^\\p{L}\\p{N} _-]+"), " ").trim().replace(Regex("\\s+"), "-").take(60)
        val f = File(dossier(contexte), "Plan-au-sol-" + propre.ifBlank { "cellule" } + ".png")
        f.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        return f
    }

    fun partager(contexte: Context, f: File) {
        val uri = FileProvider.getUriForFile(contexte, contexte.packageName + ".partage", f)
        val envoi = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, f.nameWithoutExtension)
            clipData = ClipData.newRawUri(f.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val choix = Intent.createChooser(envoi, "Partager le plan au sol").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (contexte !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexte.startActivity(choix)
    }
}
