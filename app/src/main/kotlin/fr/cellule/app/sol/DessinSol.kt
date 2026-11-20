package fr.cellule.app.sol

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import fr.cellule.app.mesure.soleilEn
import fr.cellule.core.ElementSol
import fr.cellule.core.PlanSol
import fr.cellule.core.Projecteur
import fr.cellule.core.Sol
import fr.cellule.core.SoleilPlan
import fr.cellule.core.SymbolesPlan
import fr.cellule.core.Tuiles
import fr.cellule.core.TypeElement
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/*
 * Le dessin du plan au sol, sur une toile Android : le même code sert à
 * l'écran (par la toile native de Compose), à l'image partagée et au PDF.
 * Les couleurs sont celles d'un papier, en clair, pour que le plan imprimé
 * ou envoyé se lise partout.
 */

/** Où le plan tombe sur la toile : pixels par mètre, et position du point (0, 0) du plan. */
data class VueSol(val echelle: Float, val origineX: Float, val origineY: Float) {
    fun sx(m: Double): Float = origineX + (m * echelle).toFloat()
    fun sy(m: Double): Float = origineY + (m * echelle).toFloat()
    fun versPlanX(px: Float): Double = ((px - origineX) / echelle).toDouble()
    fun versPlanY(py: Float): Double = ((py - origineY) / echelle).toDouble()

    companion object {
        /** La vue qui fait tenir tout le plan dans [largeur] × [hauteur] pixels. */
        fun ajustee(plan: PlanSol, largeur: Float, hauteur: Float, marge: Float = 0f): VueSol {
            val b = Sol.bornes(plan)
            val lm = (b[2] - b[0]).coerceAtLeast(1.0)
            val hm = (b[3] - b[1]).coerceAtLeast(1.0)
            val e = minOf((largeur - 2 * marge) / lm, (hauteur - 2 * marge) / hm).toFloat()
            val cx = (b[0] + b[2]) / 2
            val cy = (b[1] + b[3]) / 2
            return VueSol(e, (largeur / 2 - cx * e).toFloat(), (hauteur / 2 - cy * e).toFloat())
        }
    }
}

data class OptionsDessin(
    val selection: Long? = null,
    /** Taille de référence du texte, en pixels. */
    val texte: Float = 28f,
    val metres: Boolean = true,
    /** Les tuiles de carte déjà disponibles (zoom, colonne, ligne) ; null : pas de fond de carte. */
    val tuile: ((Int, Int, Int) -> Bitmap?)? = null,
    val projecteurs: (String) -> Projecteur? = { null },
    val legende: Boolean = true,
    val boussole: Boolean = true,
    /** La place proposée pour un projecteur (même identifiant), dessinée en fantôme. */
    val fantome: ElementSol? = null
)

object Couleurs {
    const val PAPIER = 0xFFFAF7F2.toInt()
    const val GRILLE = 0xFFE7E0D5.toInt()
    const val GRILLE_FORTE = 0xFFCFC4B3.toInt()
    const val ENCRE = 0xFF1A1714.toInt()
    const val GRIS = 0xFF6E665A.toInt()
    const val CAMERA = 0xFFB0283F.toInt()
    const val COMEDIEN = 0xFF2F5597.toInt()
    const val PROJECTEUR = 0xFFE08A12.toInt()
    const val MUR = 0xFF3A332B.toInt()
    const val FENETRE = 0xFF6FB3D9.toInt()
    const val PORTE = 0xFF8C6A4A.toInt()
    const val MEUBLE = 0xFFDCCFB8.toInt()
    const val REFLECTEUR = 0xFF9A9A9A.toInt()
    const val DIFFUSION = 0xFF7D8BA3.toInt()
    const val DRAPEAU = 0xFF101010.toInt()
    const val RAIL = 0xFF6E665A.toInt()
    const val REPERE = 0xFF7A3E9D.toInt()
    const val SELECTION = 0xFF0E9F6E.toInt()
    const val SOLEIL = 0xFFF2B705.toInt()

    fun de(t: TypeElement): Int = when (t) {
        TypeElement.CAMERA -> CAMERA
        TypeElement.COMEDIEN -> COMEDIEN
        TypeElement.PROJECTEUR -> PROJECTEUR
        TypeElement.REFLECTEUR -> REFLECTEUR
        TypeElement.DIFFUSION -> DIFFUSION
        TypeElement.DRAPEAU -> DRAPEAU
        TypeElement.RAIL -> RAIL
        TypeElement.MUR -> MUR
        TypeElement.FENETRE -> FENETRE
        TypeElement.PORTE -> PORTE
        TypeElement.MEUBLE -> MEUBLE
        TypeElement.REPERE -> REPERE
    }
}

/** Le soleil au moment noté sur le plan, s'il a un lieu ; sinon null. */
fun soleilDuPlan(plan: PlanSol): SoleilPlan? {
    val lat = plan.latitude ?: return null
    val lon = plan.longitude ?: return null
    val jour = runCatching { LocalDate.parse(plan.date) }.getOrNull() ?: LocalDate.now()
    val heure = runCatching { LocalTime.parse(plan.heure) }.getOrNull() ?: LocalTime.now()
    return soleilEn(lat, lon, jour.atTime(heure).atZone(ZoneId.systemDefault()))
}

object DessinSol {

    private fun trait(couleur: Int, largeur: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        style = Paint.Style.STROKE
        strokeWidth = largeur
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private fun plein(couleur: Int, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        this.alpha = alpha
        style = Paint.Style.FILL
    }

    private fun texte(couleur: Int, taille: Float, gras: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        textSize = taille
        isFakeBoldText = gras
    }

    fun dessiner(c: Canvas, plan: PlanSol, largeur: Int, hauteur: Int, vue: VueSol, o: OptionsDessin) {
        c.drawColor(Couleurs.PAPIER)
        if (plan.carte) fondDeCarte(c, plan, largeur, hauteur, vue, o)
        if (plan.grille) grille(c, plan, largeur, hauteur, vue)

        /* Le décor d'abord, puis la lumière, la caméra, et les gens par-dessus. */
        val ordre = listOf(
            TypeElement.MEUBLE, TypeElement.MUR, TypeElement.FENETRE, TypeElement.PORTE, TypeElement.RAIL,
            TypeElement.REFLECTEUR, TypeElement.DIFFUSION, TypeElement.DRAPEAU, TypeElement.REPERE
        )
        ordre.forEach { t -> plan.elements.filter { it.type == t }.forEach { element(c, it, vue, o) } }
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.forEach { faisceau(c, it, vue, o) }
        plan.elements.filter { it.type == TypeElement.CAMERA }.forEach { cone(c, it, vue) }
        Sol.fleches(plan).forEach { (a, b) -> fleche(c, a, b, vue, o) }
        listOf(TypeElement.PROJECTEUR, TypeElement.CAMERA, TypeElement.COMEDIEN).forEach { t ->
            plan.elements.filter { it.type == t }.forEach { element(c, it, vue, o) }
        }
        plan.elements.forEach { etiquette(c, it, vue, o) }
        o.fantome?.let { f -> plan.element(f.identifiant)?.let { fantome(c, it, f, vue, o) } }
        o.selection?.let { id -> plan.element(id)?.let { selection(c, it, vue) } }

        if (o.boussole) boussole(c, plan, largeur, vue, o)
        echelleGraphique(c, hauteur, vue, o)
        if (o.legende && plan.legende) legende(c, plan, largeur, hauteur, o)
        if (plan.carte) {
            val p = texte(Couleurs.GRIS, o.texte * 0.55f)
            c.drawText(Tuiles.MENTION, largeur - p.measureText(Tuiles.MENTION) - 8f, hauteur - 8f, p)
        }
    }

    /* ── Fond ──────────────────────────────────────────────────────────── */

    private fun grille(c: Canvas, plan: PlanSol, largeur: Int, hauteur: Int, vue: VueSol) {
        val pas = plan.pasGrille.coerceIn(0.1, 10.0)
        if (pas * vue.echelle < 6) return
        val fin = trait(Couleurs.GRILLE, 1f)
        val fort = trait(Couleurs.GRILLE_FORTE, 1.4f)
        val x0 = floor(vue.versPlanX(0f) / pas).toInt()
        val x1 = floor(vue.versPlanX(largeur.toFloat()) / pas).toInt() + 1
        val y0 = floor(vue.versPlanY(0f) / pas).toInt()
        val y1 = floor(vue.versPlanY(hauteur.toFloat()) / pas).toInt() + 1
        for (i in x0..x1) {
            val m = i * pas
            val x = vue.sx(m)
            c.drawLine(x, 0f, x, hauteur.toFloat(), if (abs(m - m.roundToInt()) < 1e-6) fort else fin)
        }
        for (j in y0..y1) {
            val m = j * pas
            val y = vue.sy(m)
            c.drawLine(0f, y, largeur.toFloat(), y, if (abs(m - m.roundToInt()) < 1e-6) fort else fin)
        }
    }

    /** Le zoom de tuiles dont le pixel est le plus proche de celui de la toile. */
    fun zoomCarte(latitude: Double, echelle: Float): Int {
        val mppVoulu = 1.0 / echelle
        val z = ln(40_075_016.686 * cos(Math.toRadians(latitude)) / (Tuiles.COTE * mppVoulu)) / ln(2.0)
        return z.roundToInt().coerceIn(12, Tuiles.ZOOM_MAX)
    }

    /** Les tuiles qui couvrent la toile : (zoom, colonne, ligne, x et y en mètres vers l'est et le sud depuis l'origine). */
    fun tuilesNecessaires(plan: PlanSol, largeur: Int, hauteur: Int, vue: VueSol): List<IntArray> {
        val lat = plan.latitude ?: return emptyList()
        val lon = plan.longitude ?: return emptyList()
        val z = zoomCarte(lat, vue.echelle)
        val mpp = Tuiles.metresParPixel(lat, z)
        /* Les coins de la toile, ramenés en mètres est / sud (plan tourné de son nord). */
        val coins = listOf(0f to 0f, largeur.toFloat() to 0f, 0f to hauteur.toFloat(), largeur.toFloat() to hauteur.toFloat()).map { (px, py) ->
            val x = vue.versPlanX(px)
            val y = vue.versPlanY(py)
            val r = Math.toRadians(-plan.nordDeg)
            (x * cos(r) - y * sin(r)) to (x * sin(r) + y * cos(r))
        }
        val ox = Tuiles.x(lon, z) * Tuiles.COTE
        val oy = Tuiles.y(lat, z) * Tuiles.COTE
        val tx0 = floor((ox + coins.minOf { it.first } / mpp) / Tuiles.COTE).toInt()
        val tx1 = floor((ox + coins.maxOf { it.first } / mpp) / Tuiles.COTE).toInt()
        val ty0 = floor((oy + coins.minOf { it.second } / mpp) / Tuiles.COTE).toInt()
        val ty1 = floor((oy + coins.maxOf { it.second } / mpp) / Tuiles.COTE).toInt()
        if ((tx1 - tx0 + 1) * (ty1 - ty0 + 1) > 48) return emptyList()
        val liste = mutableListOf<IntArray>()
        for (tx in tx0..tx1) for (ty in ty0..ty1) {
            val ligne = Tuiles.ligne(ty.toDouble(), z) ?: continue
            liste.add(intArrayOf(z, Tuiles.colonne(tx.toDouble(), z), ligne, tx, ty))
        }
        return liste
    }

    private fun fondDeCarte(c: Canvas, plan: PlanSol, largeur: Int, hauteur: Int, vue: VueSol, o: OptionsDessin) {
        val obtenir = o.tuile ?: return
        val lat = plan.latitude ?: return
        val lon = plan.longitude ?: return
        val tuiles = tuilesNecessaires(plan, largeur, hauteur, vue)
        if (tuiles.isEmpty()) return
        val z = tuiles.first()[0]
        val mpp = Tuiles.metresParPixel(lat, z)
        val ox = Tuiles.x(lon, z) * Tuiles.COTE
        val oy = Tuiles.y(lat, z) * Tuiles.COTE
        val pinceau = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 215 }
        c.save()
        c.translate(vue.origineX, vue.origineY)
        c.rotate(plan.nordDeg.toFloat())
        val cote = (Tuiles.COTE * mpp * vue.echelle).toFloat()
        tuiles.forEach { t ->
            val image = obtenir(t[0], t[1], t[2]) ?: return@forEach
            val x = ((t[3] * Tuiles.COTE - ox) * mpp * vue.echelle).toFloat()
            val y = ((t[4] * Tuiles.COTE - oy) * mpp * vue.echelle).toFloat()
            c.drawBitmap(image, null, RectF(x, y, x + cote + 0.5f, y + cote + 0.5f), pinceau)
        }
        c.restore()
    }

    /* ── Éléments ──────────────────────────────────────────────────────── */

    /** La nouvelle place d'un projecteur : un trait pointillé depuis l'ancienne, et le projecteur en transparence. */
    private fun fantome(c: Canvas, avant: ElementSol, apres: ElementSol, vue: VueSol, o: OptionsDessin) {
        val couleur = Couleurs.de(apres.type)
        c.drawLine(
            vue.sx(avant.x), vue.sy(avant.y), vue.sx(apres.x), vue.sy(apres.y),
            trait(couleur, 2.4f).apply { pathEffect = DashPathEffect(floatArrayOf(12f, 9f), 0f) }
        )
        c.saveLayerAlpha(null, 120)
        faisceau(c, apres, vue, o)
        element(c, apres, vue, o)
        c.restore()
        c.drawCircle(vue.sx(apres.x), vue.sy(apres.y), 0.32f * vue.echelle, trait(couleur, 2f).apply { pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f) })
    }

    private fun element(c: Canvas, e: ElementSol, vue: VueSol, o: OptionsDessin) {
        val k = vue.echelle
        val couleur = Couleurs.de(e.type)
        c.save()
        c.translate(vue.sx(e.x), vue.sy(e.y))
        c.rotate(e.rotation.toFloat())
        val l = (e.largeur * k).toFloat()
        val p = (e.profondeur * k).toFloat()
        when (e.type) {
            TypeElement.CAMERA -> {
                val corps = RectF(-0.17f * k, -0.08f * k, 0.17f * k, 0.2f * k)
                c.drawRoundRect(corps, 0.04f * k, 0.04f * k, plein(couleur))
                val objectif = Path().apply {
                    moveTo(-0.07f * k, -0.08f * k); lineTo(-0.12f * k, -0.22f * k)
                    lineTo(0.12f * k, -0.22f * k); lineTo(0.07f * k, -0.08f * k); close()
                }
                c.drawPath(objectif, plein(couleur))
            }
            TypeElement.COMEDIEN -> {
                val r = 0.24f * k
                c.drawCircle(0f, 0f, r, plein(couleur))
                val nez = Path().apply { moveTo(-0.09f * k, -r * 0.8f); lineTo(0f, -r - 0.13f * k); lineTo(0.09f * k, -r * 0.8f); close() }
                c.drawPath(nez, plein(couleur))
                c.drawCircle(0f, 0f, r, trait(0xFFFFFFFF.toInt(), 2f))
            }
            TypeElement.PROJECTEUR -> {
                val modele = modele(e, o)
                SymbolesSol.dessiner(c, SymbolesPlan.symbole(e, modele), taillesymbole(k), SymbolesSol.couleur(e, modele))
            }
            TypeElement.MUR -> c.drawRect(RectF(-l / 2, -p / 2, l / 2, p / 2), plein(couleur))
            TypeElement.FENETRE -> {
                c.drawRect(RectF(-l / 2, -p / 2, l / 2, p / 2), plein(0xFFFFFFFF.toInt()))
                c.drawRect(RectF(-l / 2, -p / 2, l / 2, p / 2), trait(couleur, 3f))
                c.drawLine(-l / 2, 0f, l / 2, 0f, trait(couleur, 2f))
            }
            TypeElement.PORTE -> {
                c.drawLine(-l / 2, 0f, l / 2, 0f, trait(couleur, maxOf(3f, p)))
                val arc = RectF(-l / 2 - l, -l, -l / 2 + l, l)
                c.drawArc(arc, 270f, 90f, false, trait(couleur, 1.6f).apply { pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f) })
                c.drawLine(-l / 2, 0f, -l / 2, -l, trait(couleur, 2.5f))
            }
            TypeElement.MEUBLE -> {
                c.drawRect(RectF(-l / 2, -p / 2, l / 2, p / 2), plein(couleur))
                c.drawRect(RectF(-l / 2, -p / 2, l / 2, p / 2), trait(Couleurs.GRIS, 1.6f))
            }
            TypeElement.REFLECTEUR -> {
                val ep = maxOf(4f, 0.06f * k)
                c.drawRect(RectF(-l / 2, -ep / 2, l / 2, ep / 2), plein(0xFFFFFFFF.toInt()))
                c.drawRect(RectF(-l / 2, -ep / 2, l / 2, ep / 2), trait(couleur, 2f))
            }
            TypeElement.DIFFUSION -> {
                c.drawLine(-l / 2, 0f, l / 2, 0f, trait(couleur, maxOf(3f, 0.05f * k)).apply { pathEffect = DashPathEffect(floatArrayOf(12f, 7f), 0f) })
                c.drawLine(-l / 2, -0.08f * k, -l / 2, 0.08f * k, trait(couleur, 2.5f))
                c.drawLine(l / 2, -0.08f * k, l / 2, 0.08f * k, trait(couleur, 2.5f))
            }
            TypeElement.DRAPEAU -> c.drawLine(-l / 2, 0f, l / 2, 0f, trait(couleur, maxOf(5f, 0.07f * k)))
            TypeElement.RAIL -> {
                val ecart = maxOf(p, 0.3f * k) / 2
                c.drawLine(-l / 2, -ecart, l / 2, -ecart, trait(couleur, 2.5f))
                c.drawLine(-l / 2, ecart, l / 2, ecart, trait(couleur, 2.5f))
                var x = -l / 2
                val pas = maxOf(0.5f * k, 8f)
                while (x <= l / 2 + 0.5f) {
                    c.drawLine(x, -ecart * 1.3f, x, ecart * 1.3f, trait(couleur, 1.6f))
                    x += pas
                }
            }
            TypeElement.REPERE -> {
                val r = 0.15f * k
                c.drawLine(-r, -r, r, r, trait(couleur, 3f))
                c.drawLine(-r, r, r, -r, trait(couleur, 3f))
            }
        }
        c.restore()
        if (e.type == TypeElement.PROJECTEUR) {
            val modele = modele(e, o)
            SymbolesPlan.source(e, modele)?.let { SymbolesSol.lettre(c, it, vue.sx(e.x), vue.sy(e.y), taillesymbole(k)) }
        }
    }

    private fun modele(e: ElementSol, o: OptionsDessin): Projecteur? = if (e.projecteur.isBlank()) null else o.projecteurs(e.projecteur)

    /** La demi-taille d'un symbole : environ 40 cm sur le plan, jamais illisible. */
    private fun taillesymbole(k: Float): Float = maxOf(0.2f * k, 10f)

    private fun cone(c: Canvas, e: ElementSol, vue: VueSol) {
        val champ = Sol.champHorizontal(e) ?: return
        val longueur = 8.0
        val chemin = Path()
        chemin.moveTo(vue.sx(e.x), vue.sy(e.y))
        listOf(e.rotation - champ / 2, e.rotation + champ / 2).forEach { r ->
            val (dx, dy) = Sol.direction(r)
            chemin.lineTo(vue.sx(e.x + dx * longueur), vue.sy(e.y + dy * longueur))
        }
        chemin.close()
        c.drawPath(chemin, plein(Couleurs.CAMERA, 26))
        val bord = trait(Couleurs.CAMERA, 1.6f).apply { pathEffect = DashPathEffect(floatArrayOf(14f, 9f), 0f) }
        listOf(e.rotation - champ / 2, e.rotation + champ / 2).forEach { r ->
            val (dx, dy) = Sol.direction(r)
            c.drawLine(vue.sx(e.x), vue.sy(e.y), vue.sx(e.x + dx * longueur), vue.sy(e.y + dy * longueur), bord)
        }
    }

    private fun faisceau(c: Canvas, e: ElementSol, vue: VueSol, o: OptionsDessin) {
        val choisi = if (e.projecteur.isBlank()) null else o.projecteurs(e.projecteur)
        val angle = Sol.faisceau(e, choisi) ?: 50.0
        val longueur = 4.0
        val chemin = Path()
        chemin.moveTo(vue.sx(e.x), vue.sy(e.y))
        val pas = 12
        for (i in 0..pas) {
            val r = e.rotation - angle / 2 + angle * i / pas
            val (dx, dy) = Sol.direction(r)
            chemin.lineTo(vue.sx(e.x + dx * longueur), vue.sy(e.y + dy * longueur))
        }
        chemin.close()
        val teinte = SymbolesSol.couleur(e, choisi)
        c.drawPath(chemin, plein(teinte, 40))
        c.drawPath(chemin, trait(teinte, 1.2f).apply { alpha = 120 })
    }

    private fun fleche(c: Canvas, a: ElementSol, b: ElementSol, vue: VueSol, o: OptionsDessin) {
        val x1 = vue.sx(a.x); val y1 = vue.sy(a.y)
        val x2 = vue.sx(b.x); val y2 = vue.sy(b.y)
        val longueur = kotlin.math.hypot((x2 - x1).toDouble(), (y2 - y1).toDouble()).toFloat()
        if (longueur < 4) return
        val ux = (x2 - x1) / longueur
        val uy = (y2 - y1) / longueur
        val retrait = 0.3f * vue.echelle
        val sx = x1 + ux * retrait; val sy = y1 + uy * retrait
        val ex = x2 - ux * retrait; val ey = y2 - uy * retrait
        val couleur = Couleurs.de(a.type)
        c.drawLine(sx, sy, ex, ey, trait(couleur, 2.4f).apply { pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f) })
        val t = o.texte * 0.5f
        val pointe = Path().apply {
            moveTo(ex, ey)
            lineTo(ex - ux * t * 1.6f - uy * t * 0.8f, ey - uy * t * 1.6f + ux * t * 0.8f)
            lineTo(ex - ux * t * 1.6f + uy * t * 0.8f, ey - uy * t * 1.6f - ux * t * 0.8f)
            close()
        }
        c.drawPath(pointe, plein(couleur))
    }

    private fun etiquette(c: Canvas, e: ElementSol, vue: VueSol, o: OptionsDessin) {
        val libelle = when (e.type) {
            TypeElement.CAMERA -> listOfNotNull(
                e.libelle,
                e.focaleMm?.let { nombreCourt(it, 0) + " mm" },
                e.plans.takeIf { it.isNotBlank() }?.let { "pl. $it" }
            ).joinToString(" · ")
            TypeElement.PROJECTEUR -> e.nom.ifBlank { o.projecteurs(e.projecteur)?.nom ?: "Projecteur" }
            TypeElement.MUR, TypeElement.RAIL -> if (e.nom.isNotBlank()) e.nom else ""
            else -> if (e.type == TypeElement.COMEDIEN || e.nom.isNotBlank()) e.libelle else ""
        }
        if (libelle.isBlank()) return
        val p = texte(Couleurs.de(e.type).let { if (e.type == TypeElement.MEUBLE || e.type == TypeElement.FENETRE) Couleurs.ENCRE else it }, o.texte * 0.62f, gras = e.type.enMouvement)
        val fond = plein(Couleurs.PAPIER, 200)
        val w = p.measureText(libelle)
        val decalage = (maxOf(e.largeur, e.profondeur).coerceAtMost(0.6) / 2 * vue.echelle).toFloat() + o.texte * 0.75f
        val x = vue.sx(e.x) - w / 2
        val y = vue.sy(e.y) + decalage
        c.drawRoundRect(RectF(x - 4, y - o.texte * 0.6f, x + w + 4, y + o.texte * 0.18f), 6f, 6f, fond)
        c.drawText(libelle, x, y, p)
        /* Sous le nom d'un projecteur : sa puissance, sa gélatine, son circuit. */
        if (e.type == TypeElement.PROJECTEUR) {
            val detail = SymbolesPlan.etiquette(e, modele(e, o))
            if (detail.isNotBlank()) {
                val pd = texte(Couleurs.GRIS, o.texte * 0.5f)
                val wd = pd.measureText(detail)
                val xd = vue.sx(e.x) - wd / 2
                val yd = y + o.texte * 0.62f
                c.drawRoundRect(RectF(xd - 4, yd - o.texte * 0.5f, xd + wd + 4, yd + o.texte * 0.15f), 6f, 6f, fond)
                c.drawText(detail, xd, yd, pd)
            }
        }
    }

    private fun selection(c: Canvas, e: ElementSol, vue: VueSol) {
        val r = (maxOf(e.largeur, e.profondeur, 0.6) / 2 * vue.echelle).toFloat() + 10f
        c.drawCircle(vue.sx(e.x), vue.sy(e.y), r, trait(Couleurs.SELECTION, 3f).apply { pathEffect = DashPathEffect(floatArrayOf(10f, 6f), 0f) })
        /* La poignée de rotation, dans la direction de l'élément. */
        val (dx, dy) = Sol.direction(e.rotation)
        val hx = vue.sx(e.x) + (dx * (r + 22)).toFloat()
        val hy = vue.sy(e.y) + (dy * (r + 22)).toFloat()
        c.drawLine(vue.sx(e.x) + (dx * r).toFloat(), vue.sy(e.y) + (dy * r).toFloat(), hx, hy, trait(Couleurs.SELECTION, 2.5f))
        c.drawCircle(hx, hy, 11f, plein(Couleurs.SELECTION))
    }

    /** Où se trouve la poignée de rotation de [e], à l'écran. */
    fun poigneeRotation(e: ElementSol, vue: VueSol): Pair<Float, Float> {
        val r = (maxOf(e.largeur, e.profondeur, 0.6) / 2 * vue.echelle).toFloat() + 10f
        val (dx, dy) = Sol.direction(e.rotation)
        return (vue.sx(e.x) + (dx * (r + 22)).toFloat()) to (vue.sy(e.y) + (dy * (r + 22)).toFloat())
    }

    /* ── Repères ───────────────────────────────────────────────────────── */

    private fun boussole(c: Canvas, plan: PlanSol, largeur: Int, vue: VueSol, o: OptionsDessin) {
        val r = o.texte * 1.25f
        val cx = largeur - r - o.texte * 0.6f
        val cy = r + o.texte * 0.6f
        c.drawCircle(cx, cy, r, plein(0xFFFFFFFF.toInt(), 225))
        c.drawCircle(cx, cy, r, trait(Couleurs.GRIS, 1.5f))
        val (nx, ny) = Sol.direction(plan.nordDeg)
        val pointe = Path().apply {
            moveTo(cx + (nx * r * 0.85).toFloat(), cy + (ny * r * 0.85).toFloat())
            lineTo(cx - (ny * r * 0.22).toFloat(), cy + (nx * r * 0.22).toFloat())
            lineTo(cx + (ny * r * 0.22).toFloat(), cy - (nx * r * 0.22).toFloat())
            close()
        }
        c.drawPath(pointe, plein(Couleurs.CAMERA))
        val n = texte(Couleurs.ENCRE, o.texte * 0.55f, gras = true)
        c.drawText("N", cx + (nx * r * 1.0).toFloat() - n.measureText("N") / 2 - (if (nx < -0.5) 8 else 0), cy + (ny * r * 1.0).toFloat() + (if (ny > 0.5) o.texte * 0.5f else -2f), n)

        if (!plan.soleil) return
        val s = soleilDuPlan(plan) ?: return
        val sx = cx - 2.6f * r
        c.drawCircle(sx, cy, r, plein(0xFFFFFFFF.toInt(), 225))
        c.drawCircle(sx, cy, r, trait(Couleurs.GRIS, 1.5f))
        val (dx, dy) = Sol.direction(Sol.soleilSurPlan(plan.nordDeg, s.azimut))
        val px = sx + (dx * r * 0.75).toFloat()
        val py = cy + (dy * r * 0.75).toFloat()
        if (s.hauteur > -0.8) {
            c.drawCircle(px, py, r * 0.22f, plein(Couleurs.SOLEIL))
            /* La lumière part du soleil et traverse le plan. */
            c.drawLine(px, py, sx - (dx * r * 0.7).toFloat(), cy - (dy * r * 0.7).toFloat(), trait(Couleurs.SOLEIL, 3f))
        } else {
            c.drawCircle(px, py, r * 0.18f, trait(Couleurs.GRIS, 2f))
        }
        val t = texte(Couleurs.GRIS, o.texte * 0.45f)
        val ligne = if (s.hauteur > -0.8) nombreCourt(s.hauteur, 0) + "°" else "nuit"
        c.drawText(ligne, sx - t.measureText(ligne) / 2, cy + r + o.texte * 0.55f, t)
    }

    private fun echelleGraphique(c: Canvas, hauteur: Int, vue: VueSol, o: OptionsDessin) {
        val unite = if (o.metres) 1.0 else Unites.METRES_PAR_PIED
        val cible = 140f / vue.echelle / unite
        val puissance = 10.0.pow(floor(log10(cible)))
        val pas = listOf(1.0, 2.0, 5.0, 10.0).map { it * puissance }.minByOrNull { abs(it - cible) } ?: 1.0
        val longueur = (pas * unite * vue.echelle).toFloat()
        val x = o.texte * 0.7f
        val y = hauteur - o.texte * 0.9f
        c.drawLine(x, y, x + longueur, y, trait(Couleurs.ENCRE, 3f))
        c.drawLine(x, y - 8, x, y + 8, trait(Couleurs.ENCRE, 2f))
        c.drawLine(x + longueur, y - 8, x + longueur, y + 8, trait(Couleurs.ENCRE, 2f))
        val libelle = nombreCourt(pas, 2) + if (o.metres) " m" else " ft"
        c.drawText(libelle, x, y - 12, texte(Couleurs.ENCRE, o.texte * 0.5f))
    }

    private fun legende(c: Canvas, plan: PlanSol, largeur: Int, hauteur: Int, o: OptionsDessin) {
        val types = TypeElement.entries.filter { t -> plan.elements.any { it.type == t } }
        if (types.isEmpty()) return
        val lignes = mutableListOf<Pair<Int, String>>()
        types.forEach { t -> lignes.add(Couleurs.de(t) to t.pluriel) }
        plan.elements.filter { it.type == TypeElement.CAMERA }.forEach { cam ->
            lignes.add(Couleurs.CAMERA to "  " + cam.libelle + listOfNotNull(
                cam.focaleMm?.let { nombreCourt(it, 0) + " mm" },
                cam.hauteur?.let { "h " + nombreCourt(it, 2) + " m" },
                cam.plans.takeIf { it.isNotBlank() }?.let { "plans $it" }
            ).joinToString(" · ", prefix = " : "))
        }
        plan.elements.filter { it.type == TypeElement.PROJECTEUR }.forEach { p ->
            val m = modele(p, o)
            val nom = p.nom.ifBlank { m?.nomComplet ?: "Projecteur" }
            val detail = SymbolesPlan.etiquette(p, m)
            lignes.add(SymbolesSol.couleur(p, m) to "  " + nom + (p.hauteur?.let { " · h " + nombreCourt(it, 2) + " m" } ?: "") +
                (if (detail.isNotBlank()) " · $detail" else ""))
        }
        /* La clé des symboles et des sources présents sur le plan. */
        val projos = plan.elements.filter { it.type == TypeElement.PROJECTEUR }
        val symboles = projos.map { SymbolesPlan.symbole(it, modele(it, o)) }.distinct()
        val sources = projos.mapNotNull { SymbolesPlan.source(it, modele(it, o)) }.distinct()
        val cle = symboles.map { it.libelle } + sources.map { "${it.lettre} = ${it.libelle}" }
        val t = texte(Couleurs.ENCRE, o.texte * 0.48f)
        val pas = o.texte * 0.66f
        val w = (lignes.maxOf { t.measureText(it.second) } + o.texte * 1.2f).coerceAtMost(largeur * 0.6f)
        val pasCle = o.texte * 0.95f
        val h = pas * lignes.size + o.texte * 0.5f + if (cle.isEmpty()) 0f else pasCle * symboles.size + pas * sources.size + o.texte * 0.3f
        val x = largeur - w - o.texte * 0.4f
        val y = hauteur - h - o.texte * 0.4f
        c.drawRoundRect(RectF(x, y, x + w, y + h), 10f, 10f, plein(0xFFFFFFFF.toInt(), 230))
        c.drawRoundRect(RectF(x, y, x + w, y + h), 10f, 10f, trait(Couleurs.GRILLE_FORTE, 1.5f))
        lignes.forEachIndexed { i, (couleur, libelle) ->
            val ly = y + o.texte * 0.25f + pas * (i + 1) - pas * 0.25f
            c.drawCircle(x + o.texte * 0.4f, ly - pas * 0.25f, o.texte * 0.14f, plein(couleur))
            c.drawText(libelle, x + o.texte * 0.75f, ly, t)
        }
        var ly = y + o.texte * 0.25f + pas * lignes.size + o.texte * 0.3f
        symboles.forEach { s ->
            c.save()
            c.translate(x + o.texte * 0.5f, ly + pasCle * 0.45f)
            SymbolesSol.dessiner(c, s, o.texte * 0.22f, Couleurs.GRIS)
            c.restore()
            c.drawText(s.libelle, x + o.texte * 1.1f, ly + pasCle * 0.6f, t)
            ly += pasCle
        }
        sources.forEach { src ->
            c.drawCircle(x + o.texte * 0.4f, ly + pas * 0.5f, o.texte * 0.14f, plein(SymbolesSol.couleur(src)))
            c.drawText("${src.lettre} : ${src.libelle}", x + o.texte * 0.75f, ly + pas * 0.75f, t)
            ly += pas
        }
    }

    /** Le plan entier en image, avec un bandeau de titre : pour le partage et le PDF. */
    fun image(plan: PlanSol, largeur: Int, hauteur: Int, titre: String, sousTitre: String, o: OptionsDessin): Bitmap {
        val image = Bitmap.createBitmap(largeur, hauteur, Bitmap.Config.ARGB_8888)
        val c = Canvas(image)
        val bandeau = o.texte * 2.6f
        c.save()
        c.translate(0f, bandeau)
        val h = (hauteur - bandeau).toInt()
        val vue = VueSol.ajustee(plan, largeur.toFloat(), h.toFloat(), o.texte * 1.5f)
        c.clipRect(0f, 0f, largeur.toFloat(), h.toFloat())
        dessiner(c, plan, largeur, h, vue, o.copy(selection = null))
        c.restore()
        c.drawRect(0f, 0f, largeur.toFloat(), bandeau, plein(0xFFFFFFFF.toInt()))
        c.drawLine(0f, bandeau, largeur.toFloat(), bandeau, trait(Couleurs.GRILLE_FORTE, 2f))
        c.drawText(titre, o.texte * 0.7f, o.texte * 1.25f, texte(Couleurs.ENCRE, o.texte * 0.95f, gras = true))
        c.drawText(sousTitre, o.texte * 0.7f, o.texte * 2.15f, texte(Couleurs.GRIS, o.texte * 0.55f))
        return image
    }

    @Suppress("unused")
    private fun tangente(d: Double) = tan(Math.toRadians(d))
}
