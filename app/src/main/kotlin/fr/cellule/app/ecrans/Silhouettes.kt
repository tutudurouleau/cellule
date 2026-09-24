package fr.cellule.app.ecrans

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.unit.dp
import fr.cellule.core.Silhouette

/*
 * Des silhouettes au trait, dans le même esprit que les icônes de navigation.
 * Jamais de photo : l'application est distribuée hors ligne, et les photos
 * de matériel sont protégées. Le dessin dit la famille — caisson de studio,
 * cube compact, zoom à support — et les indices font le reste.
 *
 * Toutes sur une grille de 48 × 32, objectif vers la droite.
 */

private fun dessin(nom: String, trace: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = nom,
        defaultWidth = 48.dp,
        defaultHeight = 32.dp,
        viewportWidth = 48f,
        viewportHeight = 32f
    ).apply {
        addPath(
            pathData = PathData(trace),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }.build()

private fun PathBuilder.rectangle(x1: Float, y1: Float, x2: Float, y2: Float) {
    moveTo(x1, y1); lineTo(x2, y1); lineTo(x2, y2); lineTo(x1, y2); close()
}

private fun PathBuilder.cercle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcTo(r, r, 0f, true, true, cx + r, cy)
    arcTo(r, r, 0f, true, true, cx - r, cy)
    close()
}

private fun PathBuilder.trait(x1: Float, y1: Float, x2: Float, y2: Float) {
    moveTo(x1, y1); lineTo(x2, y2)
}

/* Grosse caméra modulaire : corps, poignée, viseur, pare-soleil, barres. */
private val Studio = dessin("studio") {
    rectangle(12f, 10f, 32f, 25f)
    moveTo(16f, 10f); lineTo(16f, 6f); lineTo(28f, 6f); lineTo(28f, 10f)
    rectangle(5f, 12f, 12f, 18f)
    rectangle(32f, 13f, 38f, 22f)
    moveTo(38f, 14f); lineTo(45f, 10.5f); lineTo(45f, 24.5f); lineTo(38f, 21f); close()
    trait(9f, 28f, 44f, 28f)
    trait(22f, 25f, 22f, 28f)
}

/* Cube compact : petit corps, écran déporté sur le côté. */
private val Compacte = dessin("compacte") {
    rectangle(16f, 11f, 28f, 23f)
    moveTo(18f, 11f); lineTo(18f, 8f); lineTo(26f, 8f); lineTo(26f, 11f)
    rectangle(28f, 13f, 35f, 21f)
    rectangle(35f, 12f, 37.5f, 22f)
    rectangle(8f, 12.5f, 14f, 18.5f)
    trait(14f, 15.5f, 16f, 15.5f)
}

/* Caméra d'épaule : corps long, appui d'épaule, viseur à l'avant. */
private val Epaule = dessin("epaule") {
    rectangle(8f, 12f, 34f, 22f)
    moveTo(14f, 22f); quadTo(20f, 27.5f, 26f, 22f)
    moveTo(11f, 12f); lineTo(11f, 8f); lineTo(23f, 8f); lineTo(23f, 12f)
    rectangle(26f, 5f, 36f, 9f)
    trait(30f, 9f, 30f, 12f)
    rectangle(34f, 13f, 41f, 21f)
    rectangle(41f, 12f, 43.5f, 22f)
}

/* Boîtier d'appareil photo, vu de face. */
private val Reflex = dessin("reflex") {
    moveTo(8f, 12f); lineTo(18f, 12f); lineTo(20f, 7f); lineTo(28f, 7f); lineTo(30f, 12f)
    lineTo(40f, 12f); lineTo(40f, 27f); lineTo(8f, 27f); close()
    cercle(24f, 19.5f, 6f)
    cercle(24f, 19.5f, 3.5f)
    moveTo(33f, 12f); lineTo(33f, 10f); lineTo(37f, 10f); lineTo(37f, 12f)
}

/* Caméra argentique : les deux « oreilles » du magasin sur le dessus. */
private val Argentique = dessin("argentique") {
    rectangle(14f, 15f, 32f, 26f)
    cercle(18.5f, 9.5f, 5f)
    cercle(28.5f, 9.5f, 5f)
    rectangle(32f, 17f, 39f, 24f)
    rectangle(39f, 16f, 41.5f, 25f)
    moveTo(14f, 18f); lineTo(8f, 17f); lineTo(8f, 22f); lineTo(14f, 21f)
}

/* Caisson de studio aux coins arrondis : l'insonorisation fait la forme. */
private val Caisson = dessin("caisson") {
    moveTo(9f, 8f); lineTo(33f, 8f)
    arcTo(3f, 3f, 0f, false, true, 36f, 11f)
    lineTo(36f, 25f)
    arcTo(3f, 3f, 0f, false, true, 33f, 28f)
    lineTo(9f, 28f)
    arcTo(3f, 3f, 0f, false, true, 6f, 25f)
    lineTo(6f, 11f)
    arcTo(3f, 3f, 0f, false, true, 9f, 8f)
    close()
    rectangle(36f, 13f, 43f, 23f)
    rectangle(10f, 4f, 18f, 8f)
    cercle(21f, 18f, 4f)
}

/* Caméra à ressort : manivelle à gauche, tourelle de trois objectifs. */
private val Tourelle = dessin("tourelle") {
    rectangle(14f, 6f, 28f, 27f)
    rectangle(28f, 8f, 30f, 25f)
    rectangle(30f, 9f, 37f, 12.5f)
    rectangle(30f, 14.5f, 41f, 19.5f)
    rectangle(30f, 21.5f, 35f, 24.5f)
    cercle(10f, 16.5f, 2.5f)
    trait(12.5f, 16.5f, 14f, 16.5f)
    trait(10f, 16.5f, 6.5f, 21f)
}

/* Caméra dans sa nacelle, deux poignées dessous. */
private val Stabilisee = dessin("stabilisee") {
    rectangle(19f, 7f, 30f, 16f)
    rectangle(30f, 9f, 36f, 14f)
    moveTo(19f, 11.5f); lineTo(15f, 11.5f); lineTo(15f, 21f)
    trait(24.5f, 16f, 24.5f, 21f)
    trait(9f, 21f, 40f, 21f)
    trait(11f, 21f, 11f, 29f)
    trait(38f, 21f, 38f, 29f)
}

/* Focale fixe : monture, fût à bagues crantées, avant évasé. */
private val Fixe = dessin("fixe") {
    rectangle(8f, 11f, 11f, 21f)
    rectangle(11f, 9f, 33f, 23f)
    trait(16f, 9f, 16f, 23f); trait(19f, 9f, 19f, 23f)
    trait(25f, 9f, 25f, 23f); trait(28f, 9f, 28f, 23f)
    moveTo(33f, 9f); lineTo(38f, 7.5f); lineTo(38f, 24.5f); lineTo(33f, 23f)
}

/* Zoom de studio : long fût, nombreuses bagues, support d'objectif. */
private val Zoom = dessin("zoom") {
    rectangle(3f, 12f, 6f, 20f)
    rectangle(6f, 10f, 38f, 22f)
    trait(11f, 10f, 11f, 22f); trait(14f, 10f, 14f, 22f)
    trait(21f, 10f, 21f, 22f); trait(24f, 10f, 24f, 22f); trait(31f, 10f, 31f, 22f)
    moveTo(38f, 10f); lineTo(44f, 8f); lineTo(44f, 24f); lineTo(38f, 22f)
    moveTo(17f, 22f); lineTo(17f, 27f); lineTo(27f, 27f); lineTo(27f, 22f)
}

/* Anamorphique : bloc avant massif, et la traînée horizontale du flare. */
private val Anamorphique = dessin("anamorphique") {
    rectangle(6f, 12f, 9f, 20f)
    rectangle(9f, 10f, 27f, 22f)
    trait(14f, 10f, 14f, 22f); trait(19f, 10f, 19f, 22f)
    rectangle(27f, 6f, 38f, 26f)
    trait(40f, 16f, 43f, 16f)
    trait(44.5f, 16f, 46.5f, 16f)
}

/* Petite optique ancienne : fût court, bague de diaphragme, verre avant. */
private val Ancienne = dessin("ancienne") {
    rectangle(11f, 13f, 14f, 19f)
    rectangle(14f, 11f, 30f, 21f)
    trait(17f, 11f, 17f, 21f); trait(19f, 11f, 19f, 21f); trait(21f, 11f, 21f, 21f)
    rectangle(24f, 10f, 27f, 22f)
    rectangle(30f, 12f, 33f, 20f)
}

fun Silhouette.image(): ImageVector = when (this) {
    Silhouette.STUDIO -> Studio
    Silhouette.COMPACTE -> Compacte
    Silhouette.EPAULE -> Epaule
    Silhouette.REFLEX -> Reflex
    Silhouette.ARGENTIQUE -> Argentique
    Silhouette.CAISSON -> Caisson
    Silhouette.TOURELLE -> Tourelle
    Silhouette.STABILISEE -> Stabilisee
    Silhouette.FIXE -> Fixe
    Silhouette.ZOOM -> Zoom
    Silhouette.ANAMORPHIQUE -> Anamorphique
    Silhouette.ANCIENNE -> Ancienne
}
