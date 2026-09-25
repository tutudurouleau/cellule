package fr.cellule.app.ecrans

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.cellule.app.Corps
import fr.cellule.app.Detail
import fr.cellule.app.Gouttiere
import fr.cellule.app.RayonCarte
import fr.cellule.app.StyleEtiquette
import fr.cellule.core.Echelle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Les instruments de la V2 : ce qui se lit d'un coup d'œil plutôt qu'en
 * chiffres alignés. Tous se dessinent à plat, sans flou ni ombre ; les seules
 * animations sont ponctuelles (une aiguille qui rejoint sa valeur, un ticket
 * qui se révèle) — rien ne tourne en boucle.
 */

private fun polaire(rayon: Float, angle: Double) =
    Offset((rayon * cos(angle)).toFloat(), (rayon * sin(angle)).toFloat())

/* ── Le cadran du posemètre ───────────────────────────────────────────────── */

private const val EV_BAS = -2f
private const val EV_HAUT = 20f

/**
 * Un galvanomètre, comme sur les cellules à aiguille : de EV −2 à EV 20 sur un
 * demi-cercle. L'aiguille rejoint la mesure avec un peu d'inertie, ce qui lisse
 * le frémissement de la caméra ; sans mesure, elle retombe au repos, grisée.
 */
@Composable
fun CadranEV(ev: Double?, modifier: Modifier = Modifier) {
    val cible = ((ev?.toFloat() ?: EV_BAS).coerceIn(EV_BAS, EV_HAUT) - EV_BAS) / (EV_HAUT - EV_BAS)
    val aiguille by animateFloatAsState(
        cible, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "aiguille"
    )
    val actif = ev != null
    val accent = MaterialTheme.colorScheme.primary
    val repos = MaterialTheme.colorScheme.outline
    val graduation = MaterialTheme.colorScheme.onSurfaceVariant
    val piste = MaterialTheme.colorScheme.surfaceVariant
    val mesureur = rememberTextMeasurer()
    val chiffres = remember(mesureur) {
        listOf(0, 10, 20).map { it to mesureur.measure(it.toString(), Detail.copy(fontSize = 9.sp)) }
    }

    Canvas(modifier.size(width = 86.dp, height = 50.dp)) {
        val trait = 5.dp.toPx()
        val r = min(size.width / 2f, size.height) - trait / 2f - 1.dp.toPx()
        val centre = Offset(size.width / 2f, size.height - 2.dp.toPx())
        val coin = Offset(centre.x - r, centre.y - r)
        val cadre = Size(2 * r, 2 * r)

        drawArc(piste, 180f, 180f, false, coin, cadre, style = Stroke(trait, cap = StrokeCap.Round))
        if (actif) {
            drawArc(accent.copy(alpha = 0.35f), 180f, 180f * aiguille, false, coin, cadre, style = Stroke(trait, cap = StrokeCap.Round))
        }
        for (e in EV_BAS.toInt()..EV_HAUT.toInt()) {
            val angle = Math.PI * (1 + (e - EV_BAS) / (EV_HAUT - EV_BAS))
            val long = e % 5 == 0
            val r1 = r - trait / 2f - 2.dp.toPx()
            val r2 = r1 - (if (long) 5.dp else 2.5.dp).toPx()
            drawLine(
                graduation.copy(alpha = if (long) 0.9f else 0.4f),
                centre + polaire(r1, angle), centre + polaire(r2, angle),
                strokeWidth = (if (long) 1.4.dp else 1.dp).toPx()
            )
        }
        chiffres.forEach { (e, texte) ->
            val angle = Math.PI * (1 + (e - EV_BAS) / (EV_HAUT - EV_BAS))
            val p = centre + polaire(r - trait / 2f - 13.dp.toPx(), angle)
            drawText(
                texte, graduation.copy(alpha = 0.8f),
                Offset(p.x - texte.size.width / 2f, p.y - texte.size.height / 2f)
            )
        }
        val angle = Math.PI * (1 + aiguille)
        val couleur = if (actif) accent else repos
        drawLine(couleur, centre, centre + polaire(r, angle), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(couleur, 4.dp.toPx(), centre)
    }
}

/* ── L'anneau du chrono ───────────────────────────────────────────────────── */

/**
 * Le temps qui reste, en anneau qui se vide dans le sens des aiguilles d'une
 * montre ; autour, une encoche par agitation — pleine quand c'est l'heure,
 * pâle quand elle est passée. On voit venir la prochaine sans lire de chiffre.
 */
@Composable
fun AnneauChrono(
    duree: Double,
    ecoule: Double,
    creneaux: List<ClosedFloatingPointRange<Double>>,
    actif: Boolean,
    modifier: Modifier = Modifier,
    centre: @Composable ColumnScope.() -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val eteint = MaterialTheme.colorScheme.outline
    val piste = MaterialTheme.colorScheme.surfaceVariant
    val encoche = MaterialTheme.colorScheme.onSurfaceVariant
    val tete = MaterialTheme.colorScheme.onSurface
    Box(modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val trait = 12.dp.toPx()
            val r = size.minDimension / 2f - trait / 2f - 10.dp.toPx()
            val coin = Offset(center.x - r, center.y - r)
            val cadre = Size(2 * r, 2 * r)
            val f = if (duree > 0) (ecoule / duree).coerceIn(0.0, 1.0).toFloat() else 1f

            drawArc(piste, 0f, 360f, false, coin, cadre, style = Stroke(trait))
            if (f < 1f) {
                drawArc(if (actif) accent else eteint, -90f + 360f * f, 360f * (1 - f), false, coin, cadre, style = Stroke(trait))
            }

            if (duree > 0) {
                val rExt = r + trait / 2f + 6.dp.toPx()
                val coinExt = Offset(center.x - rExt, center.y - rExt)
                val cadreExt = Size(2 * rExt, 2 * rExt)
                creneaux.forEach { c ->
                    val debut = (c.start / duree).toFloat()
                    val fin = (c.endInclusive / duree).toFloat()
                    val couleur = when {
                        ecoule >= c.start && ecoule < c.endInclusive -> accent
                        ecoule >= c.endInclusive -> encoche.copy(alpha = 0.22f)
                        else -> encoche.copy(alpha = 0.75f)
                    }
                    drawArc(
                        couleur, -90f + 360f * debut, max(360f * (fin - debut), 1.5f), false,
                        coinExt, cadreExt, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            val angle = Math.toRadians(-90.0 + 360.0 * f)
            drawCircle(tete, trait * 0.3f, center + polaire(r, angle))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = centre)
    }
}

/* ── Le ticket du Labo ────────────────────────────────────────────────────── */

/**
 * Un ticket : un rectangle arrondi entaillé de deux demi-cercles à hauteur de
 * la perforation, qui sépare le talon du reste.
 */
private class FormeTicket(private val perforation: Dp, private val rayon: Dp, private val encoche: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        with(density) {
            val corps = Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(rayon.toPx())))
            }
            val y = perforation.toPx()
            if (y <= 0f || y >= size.height) return Outline.Generic(corps)
            val trous = Path().apply {
                addOval(Rect(Offset(0f, y), encoche.toPx()))
                addOval(Rect(Offset(size.width, y), encoche.toPx()))
            }
            Outline.Generic(Path.combine(PathOperation.Difference, corps, trous))
        }
}

/**
 * Le résultat d'un calcul du Labo, en ticket : en haut, ton estimation et le
 * calcul face à face ; sous la perforation, le talon — l'écart, et la suite.
 */
@Composable
fun Ticket(
    hauteurHaut: Dp = 104.dp,
    haut: @Composable RowScope.() -> Unit,
    talon: @Composable ColumnScope.() -> Unit
) {
    val forme = remember(hauteurHaut) { FormeTicket(hauteurHaut, RayonCarte, 11.dp) }
    val pointilles = MaterialTheme.colorScheme.outline
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Gouttiere, vertical = 5.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, forme)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, forme)
    ) {
        Row(
            Modifier.fillMaxWidth().height(hauteurHaut).padding(horizontal = Gouttiere + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = haut
        )
        Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 18.dp)) {
            drawLine(
                pointilles.copy(alpha = 0.6f), Offset(0f, 0f), Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
            )
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = Gouttiere + 2.dp, vertical = Gouttiere), content = talon)
    }
}

/** Une apparition unique, du bas vers sa place : pour ce qu'on vient de révéler. */
@Composable
fun Modifier.revelation(delaiMs: Int = 0): Modifier {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(durationMillis = 380, delayMillis = delaiMs)) }
    return this.graphicsLayer {
        alpha = a.value
        translationY = (1 - a.value) * 14.dp.toPx()
    }
}

/**
 * La réglette d'écart : le juste au milieu, le proche de part et d'autre, le
 * loin aux bouts. Le curseur part du milieu et glisse jusqu'à ton écart.
 */
@Composable
fun RegletteEcart(ecart: Double, echelle: Echelle, sousLeCalcul: String, auDessus: String) {
    val cible = (ecart / echelle.portee).coerceIn(-1.0, 1.0).toFloat()
    val curseur = remember { Animatable(0f) }
    LaunchedEffect(cible) { curseur.animateTo(cible, tween(durationMillis = 520, delayMillis = 180)) }
    val juste = MaterialTheme.colorScheme.primary
    val loin = MaterialTheme.colorScheme.surfaceVariant
    val marque = MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val h = 8.dp.toPx()
            val y = size.height / 2f - h / 2f
            val milieu = size.width / 2f
            val demi = size.width / 2f
            fun x(e: Double) = (milieu + e / echelle.portee * demi).toFloat()
            drawRoundRect(loin, Offset(0f, y), Size(size.width, h), CornerRadius(h / 2f))
            drawRect(juste.copy(alpha = 0.25f), Offset(x(-echelle.proche), y), Size(x(echelle.proche) - x(-echelle.proche), h))
            drawRect(juste.copy(alpha = 0.7f), Offset(x(-echelle.juste), y), Size(x(echelle.juste) - x(-echelle.juste), h))
            val cx = milieu + curseur.value * demi
            drawLine(marque, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        Row(Modifier.fillMaxWidth()) {
            Text(sousLeCalcul, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("juste", style = Detail, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(auDessus, style = Detail, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
    }
}

/* ── La frise du carnet ───────────────────────────────────────────────────── */

private val FormatJour = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
private val FormatJourAnnee = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

/** « Aujourd'hui », « Hier », « Mardi 3 septembre », « 12 mars 2025 ». */
fun jourFrise(iso: String): String {
    val d = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso.ifBlank { "Sans date" }
    val aujourdhui = LocalDate.now()
    return when (d) {
        aujourdhui -> "Aujourd'hui"
        aujourdhui.minusDays(1) -> "Hier"
        else -> d.format(if (d.year == aujourdhui.year) FormatJour else FormatJourAnnee)
            .replaceFirstChar { it.uppercase(Locale.FRENCH) }
    }
}

/**
 * Une frise : un fil vertical, un jalon par élément, les jours en repères. Les
 * éléments arrivent dans l'ordre d'affichage (le plus récent en haut).
 */
@Composable
fun <T> Frise(
    elements: List<T>,
    jour: (T) -> String,
    marque: (T) -> Boolean = { false },
    contenu: @Composable (T) -> Unit
) {
    val fil = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val neutre = MaterialTheme.colorScheme.onSurfaceVariant
    val fond = MaterialTheme.colorScheme.surfaceContainer
    var precedent: String? = null
    elements.forEachIndexed { i, e ->
        val j = jour(e)
        if (j != precedent) {
            precedent = j
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.width(22.dp).fillMaxHeight()) {
                    val x = 7.dp.toPx()
                    if (i > 0) drawLine(fil, Offset(x, 0f), Offset(x, size.height / 2f), strokeWidth = 2.dp.toPx())
                    drawLine(fil, Offset(x, size.height / 2f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                    drawCircle(accent, 5.dp.toPx(), Offset(x, size.height / 2f))
                }
                Text(
                    jourFrise(j).uppercase(Locale.FRENCH),
                    style = StyleEtiquette,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        val dernier = i == elements.lastIndex
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Canvas(Modifier.width(22.dp).fillMaxHeight()) {
                val x = 7.dp.toPx()
                val y = 20.dp.toPx()
                drawLine(fil, Offset(x, 0f), Offset(x, if (dernier) y else size.height), strokeWidth = 2.dp.toPx())
                drawCircle(fond, 4.5.dp.toPx(), Offset(x, y))
                drawCircle(
                    if (marque(e)) accent else neutre, 4.dp.toPx(), Offset(x, y),
                    style = if (marque(e)) androidx.compose.ui.graphics.drawscope.Fill else Stroke(1.6.dp.toPx())
                )
            }
            Box(Modifier.weight(1f)) { contenu(e) }
        }
    }
}

/* ── Les tableaux ─────────────────────────────────────────────────────────── */

class Colonne(val titre: String, val poids: Float = 1f, val aDroite: Boolean = false)

class RangTableau(val cellules: List<String>, val detail: String? = null, val accent: Boolean = false)

/**
 * Un vrai tableau : des en-têtes, des colonnes alignées, une ligne sur deux
 * légèrement teintée pour que l'œil ne saute pas de rang en travers.
 */
@Composable
fun Tableau(colonnes: List<Colonne>, rangs: List<RangTableau>) {
    val pair = MaterialTheme.colorScheme.surfaceContainerHigh
    val accent = MaterialTheme.colorScheme.primaryContainer
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
            colonnes.forEach { c ->
                Text(
                    c.titre.uppercase(Locale.FRENCH),
                    style = StyleEtiquette,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (c.aDroite) TextAlign.End else TextAlign.Start,
                    modifier = Modifier.weight(c.poids)
                )
            }
        }
        rangs.forEachIndexed { i, rang ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        when {
                            rang.accent -> accent
                            i % 2 == 0 -> pair
                            else -> Color.Transparent
                        },
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    rang.cellules.forEachIndexed { k, texte ->
                        val c = colonnes.getOrNull(k) ?: return@forEachIndexed
                        Text(
                            texte,
                            style = Corps,
                            color = if (rang.accent) MaterialTheme.colorScheme.onPrimaryContainer
                            else if (k == 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                            textAlign = if (c.aDroite) TextAlign.End else TextAlign.Start,
                            modifier = Modifier.weight(c.poids).padding(end = if (k < colonnes.lastIndex) 6.dp else 0.dp)
                        )
                    }
                }
                if (!rang.detail.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        rang.detail,
                        style = Detail,
                        color = if (rang.accent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
