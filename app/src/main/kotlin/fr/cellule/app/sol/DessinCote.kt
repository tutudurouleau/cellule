package fr.cellule.app.sol

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import fr.cellule.core.ElementSol
import fr.cellule.core.PlanSol
import fr.cellule.core.Sol
import fr.cellule.core.TypeElement
import fr.cellule.core.Unites
import fr.cellule.core.nombreCourt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.tan

/*
 * La vue de côté : le plan coupé le long de l'axe caméra → sujet. En
 * abscisse, la distance depuis la caméra le long de cet axe ; en ordonnée,
 * la hauteur au-dessus du sol. On y voit la hauteur de l'objectif, son champ
 * vertical, celle des projecteurs et d'où vient leur lumière sur le visage.
 */
object DessinCote {

    private fun trait(couleur: Int, largeur: Float, pointilles: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        style = Paint.Style.STROKE
        strokeWidth = largeur
        strokeCap = Paint.Cap.ROUND
        if (pointilles) pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private fun plein(couleur: Int, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        this.alpha = alpha
        style = Paint.Style.FILL
    }

    private fun texte(couleur: Int, taille: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = couleur
        textSize = taille
    }

    fun dessiner(c: Canvas, plan: PlanSol, camera: ElementSol, sujet: ElementSol?, largeur: Int, hauteur: Int, o: OptionsDessin) {
        c.drawColor(Couleurs.PAPIER)
        /* L'axe : vers le sujet s'il y en a un, sinon dans le regard de la caméra. */
        val (ax, ay) = if (sujet != null && hypot(sujet.x - camera.x, sujet.y - camera.y) > 0.05) {
            val d = hypot(sujet.x - camera.x, sujet.y - camera.y)
            (sujet.x - camera.x) / d to (sujet.y - camera.y) / d
        } else Sol.direction(camera.rotation)

        fun le_long(e: ElementSol) = (e.x - camera.x) * ax + (e.y - camera.y) * ay
        fun de_cote(e: ElementSol) = abs(-(e.x - camera.x) * ay + (e.y - camera.y) * ax)

        val visibles = plan.elements.filter {
            it.identifiant != camera.identifiant &&
                (it.type == TypeElement.COMEDIEN || it.type == TypeElement.PROJECTEUR || it.type == TypeElement.CAMERA) &&
                le_long(it) > -3 && de_cote(it) < 5
        }
        val uMax = maxOf(4.0, (visibles.maxOfOrNull { le_long(it) } ?: 0.0) + 1.0, sujet?.let { le_long(it) + 1.5 } ?: 0.0)
        val uMin = minOf(-0.8, (visibles.minOfOrNull { le_long(it) } ?: 0.0) - 0.8)
        val hMax = maxOf(3.0, (plan.elements.mapNotNull { it.hauteur }.maxOrNull() ?: 0.0) + 0.6)

        val marge = o.texte * 1.6f
        val k = minOf((largeur - 2 * marge) / (uMax - uMin), (hauteur - 2 * marge) / hMax).toFloat()
        fun px(u: Double) = marge + ((u - uMin) * k).toFloat()
        fun py(h: Double) = hauteur - marge - (h * k).toFloat()

        /* Graduations : un mètre (ou un pied). */
        val unite = if (o.metres) 1.0 else Unites.METRES_PAR_PIED
        val fin = trait(Couleurs.GRILLE, 1f)
        val t = texte(Couleurs.GRIS, o.texte * 0.45f)
        var u = ceil(uMin / unite) * unite
        while (u <= uMax) {
            c.drawLine(px(u), py(0.0), px(u), py(hMax), fin)
            c.drawText(nombreCourt(u / unite, 0), px(u) + 3, py(0.0) + o.texte * 0.55f, t)
            u += unite
        }
        var h = unite
        while (h <= hMax) {
            c.drawLine(px(uMin), py(h), px(uMax), py(h), fin)
            c.drawText(nombreCourt(h / unite, 0) + if (o.metres) " m" else " ft", px(uMin) + 3, py(h) - 3, t)
            h += unite
        }
        c.drawLine(px(uMin), py(0.0), px(uMax), py(0.0), trait(Couleurs.ENCRE, 3f))

        /* La caméra et son champ vertical. */
        val hc = camera.hauteur ?: Sol.HAUTEUR_CAMERA
        val champ = Sol.champVertical(camera)
        if (champ != null) {
            val chemin = Path()
            chemin.moveTo(px(0.0), py(hc))
            listOf(camera.inclinaison + champ / 2, camera.inclinaison - champ / 2).forEach { a ->
                val du = uMax
                val dh = du * tan(Math.toRadians(a))
                chemin.lineTo(px(du), py(hc + dh))
            }
            chemin.close()
            c.save()
            c.clipRect(px(uMin), py(hMax), px(uMax), py(0.0))
            c.drawPath(chemin, plein(Couleurs.CAMERA, 24))
            listOf(camera.inclinaison + champ / 2, camera.inclinaison - champ / 2).forEach { a ->
                c.drawLine(px(0.0), py(hc), px(uMax), py(hc + uMax * tan(Math.toRadians(a))), trait(Couleurs.CAMERA, 1.6f, true))
            }
            c.restore()
        }
        c.drawRoundRect(RectF(px(0.0) - 16, py(hc) - 11, px(0.0) + 10, py(hc) + 11), 4f, 4f, plein(Couleurs.CAMERA))
        c.drawLine(px(0.0), py(hc), px(0.0), py(0.0), trait(Couleurs.GRIS, 2f))
        etiquette(c, camera.libelle + " · " + nombreCourt(hc, 2) + " m", px(0.0), py(hc) - 16, o, Couleurs.CAMERA)

        /* Les comédiens, à la hauteur de leur visage. */
        visibles.filter { it.type == TypeElement.COMEDIEN }.forEach { e ->
            val x = px(le_long(e))
            val hv = e.hauteur ?: Sol.HAUTEUR_VISAGE
            c.drawLine(x, py(0.0), x, py(hv - 0.2), trait(Couleurs.COMEDIEN, 5f))
            c.drawCircle(x, py(hv), 0.12f * k, plein(Couleurs.COMEDIEN))
            etiquette(c, e.libelle, x, py(hv) - 0.18f * k - 6, o, Couleurs.COMEDIEN)
        }

        /* Les projecteurs, et la lumière qu'ils envoient au visage du sujet. */
        visibles.filter { it.type == TypeElement.PROJECTEUR }.forEach { e ->
            val x = px(le_long(e))
            val hp = e.hauteur ?: Sol.HAUTEUR_PROJECTEUR
            c.drawLine(x, py(0.0), x, py(hp), trait(Couleurs.GRIS, 2f))
            c.drawRect(RectF(x - 12, py(hp) - 9, x + 12, py(hp) + 9), plein(Couleurs.PROJECTEUR))
            if (sujet != null) {
                val hs = sujet.hauteur ?: Sol.HAUTEUR_VISAGE
                c.drawLine(x, py(hp), px(le_long(sujet)), py(hs), trait(Couleurs.PROJECTEUR, 2f, true))
            }
            val nom = e.nom.ifBlank { o.projecteurs(e.projecteur)?.nom ?: "Projecteur" }
            etiquette(c, nom + " · " + nombreCourt(hp, 2) + " m", x, py(hp) - 14, o, Couleurs.PROJECTEUR)
        }
        visibles.filter { it.type == TypeElement.CAMERA }.forEach { e ->
            val x = px(le_long(e))
            val he = e.hauteur ?: Sol.HAUTEUR_CAMERA
            c.drawRoundRect(RectF(x - 12, py(he) - 8, x + 8, py(he) + 8), 4f, 4f, plein(Couleurs.CAMERA, 150))
            etiquette(c, e.libelle, x, py(he) - 12, o, Couleurs.CAMERA)
        }
    }

    private fun etiquette(c: Canvas, s: String, x: Float, y: Float, o: OptionsDessin, couleur: Int) {
        val p = texte(couleur, o.texte * 0.5f)
        val w = p.measureText(s)
        c.drawRoundRect(RectF(x - w / 2 - 4, y - o.texte * 0.5f, x + w / 2 + 4, y + o.texte * 0.14f), 6f, 6f, plein(Couleurs.PAPIER, 210))
        c.drawText(s, x - w / 2, y, p)
    }
}
