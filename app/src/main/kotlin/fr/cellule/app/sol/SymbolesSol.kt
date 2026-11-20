package fr.cellule.app.sol

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import fr.cellule.core.ElementSol
import fr.cellule.core.Projecteur
import fr.cellule.core.SourceLampe
import fr.cellule.core.SymboleProjecteur
import fr.cellule.core.SymbolesPlan

/*
 * Les symboles du plan de feu, vus de dessus, l'avant vers le haut (−y) avant
 * rotation : une forme par optique, comme sur les plans de feu du théâtre et
 * des logiciels de tournage. La couleur dit la source — orange tungstène,
 * bleu HMI, vert LED, olive fluo — et la lettre la rappelle en noir et blanc.
 */
object SymbolesSol {

    fun couleur(lampe: SourceLampe?): Int = when (lampe) {
        SourceLampe.TUNGSTENE -> 0xFFE08A12.toInt()
        SourceLampe.HMI -> 0xFF2F7FD8.toInt()
        SourceLampe.LED -> 0xFF13A07F.toInt()
        SourceLampe.FLUO -> 0xFF7E9A22.toInt()
        null -> Couleurs.PROJECTEUR
    }

    fun couleur(e: ElementSol, modele: Projecteur?): Int = couleur(SymbolesPlan.source(e, modele))

    private fun plein(c: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c; style = Paint.Style.FILL }
    private fun trait(c: Int, l: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = c; style = Paint.Style.STROKE; strokeWidth = l; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }

    /**
     * Dessine [s] centré sur l'origine du canevas (déjà translaté et tourné).
     * [u] est la demi-taille en pixels.
     */
    fun dessiner(c: Canvas, s: SymboleProjecteur, u: Float, couleur: Int) {
        val fond = plein(couleur)
        val encre = trait(Couleurs.ENCRE, maxOf(1.4f, u * 0.09f))
        val blanc = trait(0xFFFFFFFF.toInt(), maxOf(1.4f, u * 0.1f))
        when (s) {
            SymboleProjecteur.FRESNEL -> {
                val corps = RectF(-u * 0.8f, -u * 0.45f, u * 0.8f, u)
                c.drawRoundRect(corps, u * 0.12f, u * 0.12f, fond)
                c.drawRoundRect(corps, u * 0.12f, u * 0.12f, encre)
                /* La lentille à échelons : une plaque bombée et ses anneaux. */
                val lentille = RectF(-u * 0.8f, -u * 1.05f, u * 0.8f, -u * 0.15f)
                c.drawArc(lentille, 180f, 180f, true, plein(0xFFFFFFFF.toInt()))
                c.drawArc(lentille, 180f, 180f, true, encre)
                c.drawArc(RectF(-u * 0.55f, -u * 0.85f, u * 0.55f, -u * 0.05f), 200f, 140f, false, encre)
                c.drawArc(RectF(-u * 0.3f, -u * 0.65f, u * 0.3f, u * 0.05f), 210f, 120f, false, encre)
            }
            SymboleProjecteur.OUVERT -> {
                val p = Path().apply {
                    moveTo(-u * 0.5f, u); lineTo(u * 0.5f, u); lineTo(u * 1.05f, -u * 0.8f); lineTo(-u * 1.05f, -u * 0.8f); close()
                }
                c.drawPath(p, fond)
                c.drawPath(p, encre)
                /* Pas de lentille : l'avant reste ouvert, la lampe se voit. */
                c.drawLine(-u * 0.9f, -u * 0.8f, u * 0.9f, -u * 0.8f, blanc)
                c.drawCircle(0f, -u * 0.25f, u * 0.2f, plein(0xFFFFFFFF.toInt()))
            }
            SymboleProjecteur.DECOUPE -> {
                val corps = RectF(-u * 0.7f, -u * 0.1f, u * 0.7f, u * 1.05f)
                c.drawRoundRect(corps, u * 0.2f, u * 0.2f, fond)
                c.drawRoundRect(corps, u * 0.2f, u * 0.2f, encre)
                val nez = RectF(-u * 0.42f, -u * 1.15f, u * 0.42f, -u * 0.1f)
                c.drawRect(nez, fond)
                c.drawRect(nez, encre)
                c.drawLine(-u * 0.42f, -u * 0.6f, u * 0.42f, -u * 0.6f, encre)
                /* Les couteaux, de part et d'autre. */
                c.drawLine(-u * 0.95f, u * 0.2f, -u * 0.7f, u * 0.2f, encre)
                c.drawLine(u * 0.7f, u * 0.2f, u * 0.95f, u * 0.2f, encre)
            }
            SymboleProjecteur.PAR -> {
                val p = Path().apply {
                    moveTo(-u * 0.85f, u); lineTo(u * 0.85f, u); lineTo(u * 0.85f, -u * 0.15f)
                    arcTo(RectF(-u * 0.85f, -u * 1f, u * 0.85f, u * 0.7f), 0f, -180f)
                    close()
                }
                c.drawPath(p, fond)
                c.drawPath(p, encre)
                c.drawLine(-u * 0.85f, -u * 0.15f, u * 0.85f, -u * 0.15f, encre)
            }
            SymboleProjecteur.PANNEAU -> {
                val r = RectF(-u * 1.4f, -u * 0.4f, u * 1.4f, u * 0.4f)
                c.drawRoundRect(r, u * 0.08f, u * 0.08f, fond)
                c.drawRoundRect(r, u * 0.08f, u * 0.08f, encre)
                /* La face émettrice, côté sujet, et la grille des LED. */
                c.drawLine(-u * 1.4f, -u * 0.4f, u * 1.4f, -u * 0.4f, trait(Couleurs.ENCRE, maxOf(2.4f, u * 0.2f)))
                var x = -u * 1.05f
                while (x < u * 1.1f) {
                    c.drawCircle(x, 0f, maxOf(1f, u * 0.07f), plein(0xFFFFFFFF.toInt()))
                    x += u * 0.35f
                }
            }
            SymboleProjecteur.TUBE -> {
                val r = RectF(-u * 1.8f, -u * 0.2f, u * 1.8f, u * 0.2f)
                c.drawRoundRect(r, u * 0.2f, u * 0.2f, fond)
                c.drawRoundRect(r, u * 0.2f, u * 0.2f, encre)
                c.drawLine(-u * 1.5f, -u * 0.2f, u * 1.5f, -u * 0.2f, trait(Couleurs.ENCRE, maxOf(2f, u * 0.14f)))
            }
            SymboleProjecteur.BANQUE -> {
                val r = RectF(-u * 1.3f, -u * 0.55f, u * 1.3f, u * 0.55f)
                c.drawRect(r, fond)
                c.drawRect(r, encre)
                listOf(-0.3f, 0f, 0.3f).forEach { y ->
                    c.drawLine(-u * 1.15f, u * y, u * 1.15f, u * y, blanc)
                }
                c.drawLine(-u * 1.3f, -u * 0.55f, u * 1.3f, -u * 0.55f, trait(Couleurs.ENCRE, maxOf(2.4f, u * 0.18f)))
            }
            SymboleProjecteur.BALLON -> {
                c.drawCircle(0f, 0f, u, fond)
                c.drawCircle(0f, 0f, u, encre)
                c.drawOval(RectF(-u * 0.45f, -u, u * 0.45f, u), trait(0xFFFFFFFF.toInt(), maxOf(1f, u * 0.07f)))
                c.drawLine(-u, 0f, u, 0f, trait(0xFFFFFFFF.toInt(), maxOf(1f, u * 0.07f)))
            }
            SymboleProjecteur.BOITE -> {
                val corps = RectF(-u * 0.45f, u * 0.45f, u * 0.45f, u * 1.05f)
                c.drawRect(corps, fond)
                c.drawRect(corps, encre)
                val boite = Path().apply {
                    moveTo(-u * 0.45f, u * 0.45f); lineTo(u * 0.45f, u * 0.45f)
                    lineTo(u * 1.25f, -u * 0.9f); lineTo(-u * 1.25f, -u * 0.9f); close()
                }
                c.drawPath(boite, plein(0xFFF4EEE4.toInt()))
                c.drawPath(boite, encre)
                /* La face de diffusion. */
                c.drawLine(-u * 1.25f, -u * 0.9f, u * 1.25f, -u * 0.9f, trait(couleur, maxOf(2.4f, u * 0.2f)))
                c.drawLine(-u * 1.25f, -u * 0.9f, u * 1.25f, -u * 0.9f, trait(Couleurs.ENCRE, 1.2f).apply {
                    pathEffect = DashPathEffect(floatArrayOf(6f, 5f), 0f)
                })
            }
            SymboleProjecteur.AUTRE -> {
                c.drawRoundRect(RectF(-u * 0.9f, -u * 0.7f, u * 0.9f, u * 0.8f), u * 0.15f, u * 0.15f, fond)
                c.drawRect(RectF(-u, -u, u, -u * 0.7f), plein(Couleurs.ENCRE))
            }
        }
    }

    /** La lettre de la source, droite quelle que soit la rotation. */
    fun lettre(c: Canvas, lampe: SourceLampe, x: Float, y: Float, u: Float) {
        if (u < 7f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Couleurs.ENCRE
            textSize = u * 0.85f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val fond = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); alpha = 225 }
        c.drawCircle(x, y, u * 0.5f, fond)
        c.drawText(lampe.lettre, x, y + u * 0.3f, p)
    }
}
