@file:OptIn(ExperimentalTextApi::class)

package fr.cellule.app.ecrans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import fr.cellule.app.Detail

/** Une série de points à relier ; en pointillés quand elle sert de comparaison. */
data class Serie(
    val points: List<Pair<Double, Double>>,
    val couleur: Color,
    val pointilles: Boolean = false,
    val epaisseur: Float = 2.5f
)

/**
 * Un graphique sans bibliothèque : une grille, des courbes, les points que
 * publie le fabricant et la position du calcul en cours. Les échelles sont
 * linéaires ; pour une échelle logarithmique, l'appelant passe des logarithmes
 * et des graduations déjà libellées.
 */
@Composable
fun Graphique(
    series: List<Serie>,
    x: ClosedFloatingPointRange<Double>,
    y: ClosedFloatingPointRange<Double>,
    graduationsX: List<Pair<Double, String>>,
    graduationsY: List<Pair<Double, String>>,
    modifier: Modifier = Modifier,
    reperes: List<Pair<Double, Double>> = emptyList(),
    curseur: Pair<Double, Double>? = null
) {
    val mesureur = rememberTextMeasurer()
    val style = Detail.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val grille = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val texte = MaterialTheme.colorScheme.onSurface

    Canvas(modifier.fillMaxWidth().height(190.dp)) {
        val gauche = 44.dp.toPx()
        val bas = 22.dp.toPx()
        val haut = 8.dp.toPx()
        val droite = 10.dp.toPx()
        val l = size.width - gauche - droite
        val h = size.height - bas - haut
        fun px(v: Double) = gauche + ((v - x.start) / (x.endInclusive - x.start) * l).toFloat()
        fun py(v: Double) = haut + h - ((v - y.start) / (y.endInclusive - y.start) * h).toFloat()

        graduationsY.forEach { (v, libelle) ->
            drawLine(grille, Offset(gauche, py(v)), Offset(gauche + l, py(v)), 1f)
            val m = mesureur.measure(libelle, style)
            drawText(m, topLeft = Offset(gauche - m.size.width - 6.dp.toPx(), py(v) - m.size.height / 2f))
        }
        graduationsX.forEach { (v, libelle) ->
            drawLine(grille, Offset(px(v), haut), Offset(px(v), haut + h), 1f)
            val m = mesureur.measure(libelle, style)
            drawText(m, topLeft = Offset(px(v) - m.size.width / 2f, haut + h + 4.dp.toPx()))
        }

        clipRect(gauche, haut, gauche + l, haut + h) {
            series.forEach { s ->
                if (s.points.size < 2) return@forEach
                val chemin = Path()
                s.points.forEachIndexed { i, (a, b) ->
                    if (i == 0) chemin.moveTo(px(a), py(b)) else chemin.lineTo(px(a), py(b))
                }
                drawPath(
                    chemin, s.couleur,
                    style = Stroke(
                        width = s.epaisseur.dp.toPx() / 1.5f,
                        pathEffect = if (s.pointilles) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                    )
                )
            }
            reperes.forEach { (a, b) ->
                drawCircle(texte, 4.5.dp.toPx(), Offset(px(a), py(b)), style = Stroke(1.6.dp.toPx()))
            }
            curseur?.let { (a, b) ->
                drawLine(accent.copy(alpha = 0.5f), Offset(px(a), haut + h), Offset(px(a), py(b)), 1.5f)
                drawCircle(accent, 5.dp.toPx(), Offset(px(a), py(b)))
            }
        }
    }
}
