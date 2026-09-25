package fr.cellule.app.ecrans

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Une série de points à relier ; en pointillés quand elle sert de comparaison.
 * [nom] apparaît dans la légende et dans la bulle ; la série [principale] est
 * celle que le doigt suit.
 */
data class Serie(
    val points: List<Pair<Double, Double>>,
    val couleur: Color,
    val pointilles: Boolean = false,
    val epaisseur: Float = 2.5f,
    val nom: String? = null,
    val principale: Boolean = false,
    /** Faux pour une série de fond, nommée dans la bulle mais pas dans la légende. */
    val legende: Boolean = true
)

/** La valeur d'une série en [x], par interpolation entre ses deux points voisins ; null hors de la série. */
private fun Serie.valeur(x: Double): Double? {
    if (points.isEmpty()) return null
    val tries = points.sortedBy { it.first }
    if (x < tries.first().first - 1e-9 || x > tries.last().first + 1e-9) return null
    tries.zipWithNext().forEach { (a, b) ->
        if (x >= a.first - 1e-9 && x <= b.first + 1e-9) {
            val f = if (b.first == a.first) 0.0 else (x - a.first) / (b.first - a.first)
            return a.second + f * (b.second - a.second)
        }
    }
    return tries.last().second
}

/**
 * Un graphique sans bibliothèque : une grille, des courbes, les points que
 * publie le fabricant et la position du calcul en cours. Les échelles sont
 * linéaires ; pour une échelle logarithmique, l'appelant passe des logarithmes
 * et des graduations déjà libellées.
 *
 * Quand [lire] est donné, le graphique se lit au doigt : un appui ou un
 * glissé horizontal place un repère, et une bulle — au-dessus de la courbe,
 * jamais sous le doigt — dit la valeur. Près d'un point publié, le repère s'y
 * aimante (avec une légère vibration) : on sait alors qu'on lit le chiffre du
 * fabricant, pas l'interpolation. Le repère reste après qu'on a levé le doigt,
 * le temps de lire. Rien ne bouge tant qu'on ne touche pas : aucune animation
 * continue.
 *
 * @param lire la phrase de la bulle pour un point (x, y) de la série principale.
 * @param lireCourt l'étiquette brève d'une ordonnée : au-dessus des points publiés et du calcul.
 * @param lireAutre la valeur d'une autre série au même x, pour la seconde ligne de la bulle.
 * @param pas en unités de x : le doigt avance par pas (0,5 °C par exemple).
 * @param reperesSeulement le repère ne se pose que sur les points publiés (quand rien n'existe entre eux) ;
 *   les autres séries n'y sont lues que si elles publient ce même point.
 * @param surChoix prévenu à chaque repère posé, pour qu'un tableau voisin suive le doigt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Graphique(
    series: List<Serie>,
    x: ClosedFloatingPointRange<Double>,
    y: ClosedFloatingPointRange<Double>,
    graduationsX: List<Pair<Double, String>>,
    graduationsY: List<Pair<Double, String>>,
    modifier: Modifier = Modifier,
    reperes: List<Pair<Double, Double>> = emptyList(),
    curseur: Pair<Double, Double>? = null,
    titreX: String? = null,
    titreY: String? = null,
    lire: ((Double, Double) -> String)? = null,
    lireCourt: ((Double) -> String)? = null,
    lireAutre: ((Serie, Double, Double) -> String)? = null,
    pas: Double? = null,
    reperesSeulement: Boolean = false,
    libelleCurseur: String = "ton calcul",
    surChoix: ((Double) -> Unit)? = null
) {
    val mesureur = rememberTextMeasurer()
    val style = Detail.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val styleEtiquette = Detail.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    val styleBulle = Corps.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
    val styleBulle2 = Detail.copy(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
    val grille = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val texte = MaterialTheme.colorScheme.onSurface
    val fondCarte = MaterialTheme.colorScheme.surfaceContainer
    val vibreur = LocalHapticFeedback.current

    val interactif = lire != null
    val principale = series.lastOrNull { it.principale } ?: series.lastOrNull { !it.pointilles } ?: series.lastOrNull()

    /* Le x choisi, en unités du graphique ; il survit aux recompositions tant que les courbes ne changent pas. */
    var choisi by remember(series, reperes) { mutableStateOf<Double?>(null) }
    var repereAimante by remember(series, reperes) { mutableStateOf(-1) }

    Column(modifier.fillMaxWidth()) {
        if (titreY != null) {
            Text(titreY, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        /* Marges du tracé, partagées entre le dessin et le toucher. */
        val gaucheDp = 44.dp
        val droiteDp = 12.dp
        val hautDp = if (interactif) 50.dp else 14.dp
        val basDp = 24.dp

        fun placer(px: Float, largeur: Float, densite: Float) {
            val gauche = gaucheDp.value * densite
            val l = largeur - gauche - droiteDp.value * densite
            if (l <= 0) return
            var v = x.start + ((px - gauche) / l).coerceIn(0f, 1f) * (x.endInclusive - x.start)
            if (pas != null) v = (v / pas).roundToInt() * pas
            /* Près d'un point publié (moins de 16 dp), ou toujours s'il n'y a que ça à lire : on s'y aimante. */
            val proche = reperes.withIndex().minByOrNull { abs(it.value.first - v) }
            val seuil = 16 * densite / l * (x.endInclusive - x.start)
            if (proche != null && (reperesSeulement || abs(proche.value.first - v) <= seuil)) {
                v = proche.value.first
                if (proche.index != repereAimante) {
                    repereAimante = proche.index
                    vibreur.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } else {
                repereAimante = -1
            }
            val retenu = v.coerceIn(x.start, x.endInclusive)
            choisi = retenu
            surChoix?.invoke(retenu)
        }

        /* Une autre série au x choisi : interpolée, ou seulement si elle publie ce point. */
        fun Serie.lue(v: Double): Double? =
            if (reperesSeulement) points.firstOrNull { abs(it.first - v) < 1e-6 }?.second else valeur(v)

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(if (interactif) 250.dp else 200.dp)
                .then(
                    if (!interactif) Modifier else Modifier
                        .pointerInput(series, reperes) {
                            detectTapGestures { p -> placer(p.x, size.width.toFloat(), density) }
                        }
                        .pointerInput(series, reperes) {
                            detectHorizontalDragGestures(
                                onDragStart = { p -> placer(p.x, size.width.toFloat(), density) }
                            ) { change, _ ->
                                change.consume()
                                placer(change.position.x, size.width.toFloat(), density)
                            }
                        }
                )
        ) {
            val gauche = gaucheDp.toPx()
            val bas = basDp.toPx()
            val haut = hautDp.toPx()
            val droite = droiteDp.toPx()
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
                drawText(m, topLeft = Offset(px(v) - m.size.width / 2f, haut + h + 5.dp.toPx()))
            }

            clipRect(gauche, haut, gauche + l, haut + h) {
                /* Sous la courbe principale, un voile très léger : on voit d'un coup d'œil où elle monte. */
                principale?.takeIf { it.points.size >= 2 }?.let { s ->
                    val voile = Path()
                    val tries = s.points.sortedBy { it.first }
                    voile.moveTo(px(tries.first().first), haut + h)
                    tries.forEach { (a, b) -> voile.lineTo(px(a), py(b)) }
                    voile.lineTo(px(tries.last().first), haut + h)
                    voile.close()
                    drawPath(voile, s.couleur.copy(alpha = 0.08f))
                }
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
            }

            /* Les points publiés, avec leur valeur : ce sont les seuls chiffres qui ne sont pas calculés. */
            reperes.forEachIndexed { i, (a, b) ->
                val c = Offset(px(a), py(b))
                drawCircle(fondCarte, 5.dp.toPx(), c)
                drawCircle(texte, 4.5.dp.toPx(), c, style = Stroke(1.6.dp.toPx()))
                if (lireCourt != null && i != repereAimante) {
                    val m = mesureur.measure(lireCourt(b), styleEtiquette.copy(color = texte))
                    drawText(m, topLeft = Offset((c.x - m.size.width / 2f).coerceIn(gauche, gauche + l - m.size.width), c.y - m.size.height - 6.dp.toPx()))
                }
            }

            /* Le calcul en cours : un point plein, sa verticale, et sa valeur. */
            curseur?.let { (a, b) ->
                if (a < x.start || a > x.endInclusive) return@let
                val c = Offset(px(a), py(b))
                drawLine(accent.copy(alpha = 0.5f), Offset(c.x, haut + h), c, 1.5.dp.toPx())
                drawCircle(fondCarte, 7.dp.toPx(), c)
                drawCircle(accent, 5.5.dp.toPx(), c)
                if (lireCourt != null && choisi == null) {
                    val m = mesureur.measure("$libelleCurseur · ${lireCourt(b)}", styleEtiquette.copy(color = accent))
                    val gaucheTexte = (c.x - m.size.width / 2f).coerceIn(gauche, gauche + l - m.size.width)
                    drawText(m, topLeft = Offset(gaucheTexte, (c.y + 9.dp.toPx()).coerceAtMost(haut + h - m.size.height - 2.dp.toPx())))
                }
            }

            /* Le repère du doigt, et sa bulle en haut du graphique. */
            val v = choisi
            if (v != null && lire != null && principale != null) {
                val publie = reperes.getOrNull(repereAimante)
                val valeur = publie?.second ?: principale.valeur(v)
                if (valeur != null) {
                    val c = Offset(px(v), py(valeur))
                    drawLine(
                        texte.copy(alpha = 0.55f), Offset(c.x, haut + h), Offset(c.x, haut - 4.dp.toPx()), 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    series.filter { it !== principale }.forEach { s ->
                        s.lue(v)?.let { w -> drawCircle(s.couleur, 3.5.dp.toPx(), Offset(c.x, py(w))) }
                    }
                    drawCircle(fondCarte, 8.dp.toPx(), c)
                    drawCircle(accent, 6.dp.toPx(), c)
                    drawCircle(fondCarte, 2.5.dp.toPx(), c)

                    val ligne1 = mesureur.measure(lire(v, valeur), styleBulle)
                    val autres = if (lireAutre == null) emptyList() else series
                        .filter { it !== principale && it.nom != null }
                        .mapNotNull { s -> s.lue(v)?.let { w -> lireAutre(s, v, w) } }
                        .take(3)
                    val secondes = listOfNotNull(
                        if (publie != null) "publié par le fabricant" else null
                    ) + autres
                    val ligne2 = secondes.takeIf { it.isNotEmpty() }?.let {
                        mesureur.measure(
                            it.joinToString("  ·  "), styleBulle2,
                            overflow = TextOverflow.Ellipsis, maxLines = 1,
                            constraints = Constraints(maxWidth = (size.width - 24.dp.toPx()).toInt())
                        )
                    }
                    val marge = 8.dp.toPx()
                    val largeur = maxOf(ligne1.size.width, ligne2?.size?.width ?: 0) + 2 * marge
                    val hauteur = ligne1.size.height + (ligne2?.size?.height ?: 0) + 1.2f * marge
                    val gaucheBulle = (c.x - largeur / 2f).coerceIn(0f, size.width - largeur)
                    val hautBulle = 2.dp.toPx()
                    drawRoundRect(
                        if (publie != null) accent else accent.copy(alpha = 0.92f),
                        Offset(gaucheBulle, hautBulle), Size(largeur, hauteur), CornerRadius(12.dp.toPx())
                    )
                    drawText(ligne1, topLeft = Offset(gaucheBulle + marge, hautBulle + 0.6f * marge))
                    ligne2?.let { drawText(it, topLeft = Offset(gaucheBulle + marge, hautBulle + 0.6f * marge + ligne1.size.height)) }
                }
            }
        }

        if (titreX != null) {
            Text(
                titreX, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }

        /* La légende : ce que veut dire chaque trait. */
        val nommees = series.filter { it.nom != null && it.legende }
        if (nommees.isNotEmpty() || reperes.isNotEmpty() || curseur != null) {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                nommees.forEach { s -> Legende(s.nom!!) { EchantillonTrait(s) } }
                if (reperes.isNotEmpty()) Legende("points publiés") { EchantillonPoint(texte, plein = false) }
                if (curseur != null) Legende(libelleCurseur) { EchantillonPoint(accent, plein = true) }
            }
        }
        if (interactif) {
            Text(
                "Touche ou fais glisser le doigt sur le graphique pour lire une valeur.",
                style = Detail,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun Legende(nom: String, echantillon: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        echantillon()
        Text(nom, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun EchantillonTrait(s: Serie) {
    Canvas(Modifier.width(20.dp).height(10.dp)) {
        drawLine(
            s.couleur, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
            strokeWidth = s.epaisseur.dp.toPx() / 1.5f,
            pathEffect = if (s.pointilles) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null
        )
    }
}

@Composable
private fun EchantillonPoint(couleur: Color, plein: Boolean) {
    Canvas(Modifier.size(10.dp)) {
        if (plein) drawCircle(couleur, size.minDimension / 2f)
        else drawCircle(couleur, size.minDimension / 2f - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
    }
}

/** Pour les bulles : une fraction de minute lisible, « 5 min 35 ». */
fun minutesLisibles(minutes: Double): String {
    val s = ((minutes * 60) / 5).roundToInt() * 5
    val m = s / 60
    val r = s % 60
    return when {
        m == 0 -> "$r s"
        r == 0 -> "$m min"
        else -> "$m min ${r.toString().padStart(2, '0')}"
    }
}

/** Pour les étiquettes : « 5′35 », « 9′ ». */
fun minutesCourtes(minutes: Double): String {
    val s = ((minutes * 60) / 5).roundToInt() * 5
    val m = s / 60
    val r = s % 60
    return if (r == 0) "$m′" else "$m′${r.toString().padStart(2, '0')}"
}
