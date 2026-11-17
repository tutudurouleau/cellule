package fr.cellule.app.sol

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import fr.cellule.core.CameraVirtuelle
import fr.cellule.core.ElementSol
import fr.cellule.core.P3
import fr.cellule.core.PlanSol
import fr.cellule.core.Rendu3D
import fr.cellule.core.Sol
import fr.cellule.core.TypeElement
import fr.cellule.core.nombreCourt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * La vue caméra du plan au sol : ce que voit la caméra choisie, en
 * perspective. Pas de moteur 3D — des faces projetées et peintes de la plus
 * lointaine à la plus proche. Les hauteurs qu'on n'a pas saisies prennent
 * des valeurs de décor courantes (mur 2,5 m, meuble 0,75 m) : c'est une
 * maquette, pas une mesure.
 */
object DessinVueCamera {

    const val HAUTEUR_MUR = 2.5
    const val HAUTEUR_MEUBLE = 0.75

    private sealed interface Objet { val profondeur: Double }
    private class Face(val points: List<P3>, val couleur: Int, val alpha: Int = 255, val contour: Int? = null) : Objet {
        override val profondeur = points.sumOf { it.z } / points.size
    }
    private class Trait(val a: P3, val b: P3, val couleur: Int, val largeur: Float) : Objet {
        override val profondeur = (a.z + b.z) / 2
    }
    private class Tete(val c: P3, val rayon: Double, val couleur: Int) : Objet {
        override val profondeur = c.z
    }
    private class Etiquette(val p: P3, val texte: String, val couleur: Int) : Objet {
        override val profondeur = -1.0
    }

    private fun mur(a: P3, b: P3, bas: Double, haut: Double) =
        listOf(P3(a.x, a.y, bas), P3(b.x, b.y, bas), P3(b.x, b.y, haut), P3(a.x, a.y, haut))

    private fun nuance(couleur: Int, f: Double): Int {
        val k = f.coerceIn(0.3, 1.3)
        val r = ((couleur shr 16 and 0xFF) * k).toInt().coerceIn(0, 255)
        val g = ((couleur shr 8 and 0xFF) * k).toInt().coerceIn(0, 255)
        val b = ((couleur and 0xFF) * k).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun melange(a: Int, b: Int, k: Double): Int {
        fun c(x: Int, s: Int) = (x shr s) and 0xFF
        fun m(s: Int) = (c(a, s) * (1 - k) + c(b, s) * k).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
    }

    /** Une face verticale s'éclaire selon son orientation : le relief se lit sans lumière réelle. */
    private fun ombre(a: P3, b: P3): Double {
        val nx = -(b.y - a.y)
        val ny = b.x - a.x
        val n = hypot(nx, ny).coerceAtLeast(1e-6)
        return 0.75 + 0.3 * ((nx * 0.6 + ny * -0.8) / n)
    }

    fun dessiner(c: Canvas, plan: PlanSol, camera: ElementSol, largeur: Int, hauteur: Int, o: OptionsDessin) {
        val l = largeur.toDouble()
        val h = hauteur.toDouble()
        val vue = CameraVirtuelle.depuis(camera)
        if (vue == null) {
            c.drawColor(Couleurs.PAPIER)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Couleurs.GRIS; textSize = o.texte * 0.6f }
            c.drawText("Donne une focale et un capteur à cette caméra.", 20f, h.toFloat() / 2, p)
            return
        }

        /* Le ciel et le sol, séparés par l'horizon. */
        val loin = vue.position + P3(sin(Math.toRadians(camera.rotation)) * 1000, -cos(Math.toRadians(camera.rotation)) * 1000, 0.0)
        val horizon = vue.versCamera(P3(loin.x, loin.y, vue.position.z)).let { pc ->
            if (pc.z > 0) vue.ecran(pc, l, h).second.toFloat() else if (camera.inclinaison > 0) h.toFloat() else 0f
        }.coerceIn(-1f, h.toFloat() + 1f)
        val ciel = Paint().apply { shader = LinearGradient(0f, 0f, 0f, maxOf(horizon, 1f), 0xFFBFD3E3.toInt(), 0xFFE9EEF1.toInt(), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, l.toFloat(), horizon.coerceAtLeast(0f), ciel)
        val sol = Paint().apply { shader = LinearGradient(0f, horizon, 0f, h.toFloat(), 0xFFD9D1C3.toInt(), 0xFFBFB3A0.toInt(), Shader.TileMode.CLAMP) }
        c.drawRect(0f, horizon.coerceAtMost(h.toFloat()), l.toFloat(), h.toFloat(), sol)

        val objets = mutableListOf<Objet>()
        fun face(pts: List<P3>, couleur: Int, alpha: Int = 255, contour: Int? = null) {
            val coupee = Rendu3D.couper(pts.map { vue.versCamera(it) })
            if (coupee.size >= 3) objets.add(Face(coupee, couleur, alpha, contour))
        }
        fun trait(a: P3, b: P3, couleur: Int, largeurTrait: Float) {
            Rendu3D.couperSegment(vue.versCamera(a), vue.versCamera(b))?.let { (x, y) -> objets.add(Trait(x, y, couleur, largeurTrait)) }
        }
        fun boite(e: ElementSol, l0: Double, p0: Double, bas: Double, haut: Double, couleur: Int) {
            val k = Rendu3D.coins(e, l0, p0)
            for (i in 0..3) {
                val a = P3(k[i].first, k[i].second, 0.0)
                val b = P3(k[(i + 1) % 4].first, k[(i + 1) % 4].second, 0.0)
                face(mur(a, b, bas, haut), nuance(couleur, ombre(a, b)), contour = nuance(couleur, 0.6))
            }
            face(k.map { P3(it.first, it.second, haut) }, nuance(couleur, 1.12), contour = nuance(couleur, 0.6))
        }

        /* Le quadrillage du sol, un mètre. */
        val etendue = 15
        val cx = camera.x.toInt()
        val cy = camera.y.toInt()
        for (i in -etendue..etendue) {
            trait(P3((cx + i).toDouble(), (cy - etendue).toDouble(), 0.0), P3((cx + i).toDouble(), (cy + etendue).toDouble(), 0.0), 0x33000000, 1f)
            trait(P3((cx - etendue).toDouble(), (cy + i).toDouble(), 0.0), P3((cx + etendue).toDouble(), (cy + i).toDouble(), 0.0), 0x33000000, 1f)
        }

        val sujets = plan.elements.filter { it.type == TypeElement.COMEDIEN }
        plan.elements.filter { it.identifiant != camera.identifiant }.forEach { e ->
            when (e.type) {
                TypeElement.MUR -> {
                    val (a, b) = Rendu3D.bouts(e)
                    face(mur(a, b, 0.0, e.hauteur ?: HAUTEUR_MUR), nuance(0xFFE8E2D6.toInt(), ombre(a, b)), contour = 0xFF8A8174.toInt())
                }
                TypeElement.FENETRE -> {
                    val (a, b) = Rendu3D.bouts(e)
                    val haut = e.hauteur ?: HAUTEUR_MUR
                    face(mur(a, b, 0.0, 0.9), nuance(0xFFE8E2D6.toInt(), ombre(a, b)), contour = 0xFF8A8174.toInt())
                    face(mur(a, b, 2.1.coerceAtMost(haut), haut), nuance(0xFFE8E2D6.toInt(), ombre(a, b)), contour = 0xFF8A8174.toInt())
                    face(mur(a, b, 0.9, 2.1.coerceAtMost(haut)), 0xFF9CC9E6.toInt(), alpha = 150, contour = Couleurs.FENETRE)
                }
                TypeElement.PORTE -> {
                    val (a, b) = Rendu3D.bouts(e)
                    face(mur(a, b, 0.0, 2.0), nuance(0xFFA27E5A.toInt(), ombre(a, b)), contour = 0xFF5E4630.toInt())
                }
                TypeElement.MEUBLE -> boite(e, e.largeur, e.profondeur, 0.0, e.hauteur ?: HAUTEUR_MEUBLE, Couleurs.MEUBLE)
                TypeElement.REFLECTEUR -> {
                    val (a, b) = Rendu3D.bouts(e)
                    val milieu = e.hauteur ?: 1.4
                    face(mur(a, b, milieu - e.largeur / 2, milieu + e.largeur / 2), 0xFFF7F7F7.toInt(), contour = 0xFF9A9A9A.toInt())
                }
                TypeElement.DIFFUSION -> {
                    val (a, b) = Rendu3D.bouts(e)
                    val milieu = e.hauteur ?: 1.7
                    face(mur(a, b, milieu - e.largeur / 2, milieu + e.largeur / 2), 0xFFFFFFFF.toInt(), alpha = 120, contour = 0xFF7D8BA3.toInt())
                }
                TypeElement.DRAPEAU -> {
                    val (a, b) = Rendu3D.bouts(e)
                    val milieu = e.hauteur ?: 1.9
                    face(mur(a, b, milieu - 0.3, milieu + 0.3), 0xFF151515.toInt())
                }
                TypeElement.RAIL -> {
                    val k = Rendu3D.coins(e, e.largeur, maxOf(e.profondeur, 0.3))
                    trait(P3(k[0].first, k[0].second, 0.01), P3(k[1].first, k[1].second, 0.01), Couleurs.RAIL, 3f)
                    trait(P3(k[3].first, k[3].second, 0.01), P3(k[2].first, k[2].second, 0.01), Couleurs.RAIL, 3f)
                }
                TypeElement.REPERE -> {
                    trait(P3(e.x - 0.15, e.y - 0.15, 0.01), P3(e.x + 0.15, e.y + 0.15, 0.01), Couleurs.REPERE, 3f)
                    trait(P3(e.x - 0.15, e.y + 0.15, 0.01), P3(e.x + 0.15, e.y - 0.15, 0.01), Couleurs.REPERE, 3f)
                }
                TypeElement.COMEDIEN -> {
                    val visage = e.hauteur ?: Sol.HAUTEUR_VISAGE
                    /* Teinté par la lumière qui l'atteint : chaude sous le tungstène, blanche sous le jour. */
                    val apports = Sol.apports(plan, e, o.projecteurs).filter { (it.lux ?: 0.0) > 0 || (it.lux == null && it.dansFaisceau) }
                    val chaud = apports.any { a -> (o.projecteurs(a.projecteur.projecteur)?.temperatureK ?: 5600) < 4200 }
                    val corps = if (apports.isEmpty()) Couleurs.COMEDIEN
                    else melange(Couleurs.COMEDIEN, if (chaud) 0xFFFFC98A.toInt() else 0xFFFFFFFF.toInt(), 0.35)
                    boite(e, 0.42, 0.24, 0.0, visage - 0.14, corps)
                    Rendu3D.couper(listOf(vue.versCamera(P3(e.x, e.y, visage + 0.02)))).firstOrNull()?.let { objets.add(Tete(it, 0.12, melange(0xFFE8C4A2.toInt(), corps, 0.15))) }
                    vue.versCamera(P3(e.x, e.y, visage + 0.3)).takeIf { it.z > Rendu3D.PROCHE }?.let { objets.add(Etiquette(it, e.libelle, Couleurs.COMEDIEN)) }
                }
                TypeElement.PROJECTEUR -> {
                    val hp = e.hauteur ?: Sol.HAUTEUR_PROJECTEUR
                    trait(P3(e.x, e.y, 0.0), P3(e.x, e.y, hp - 0.15), 0xFF3A3A3A.toInt(), 3f)
                    boite(e, 0.34, 0.3, hp - 0.15, hp + 0.15, Couleurs.PROJECTEUR)
                    faisceau(e, hp, sujets, o)?.forEach { tri -> face(tri, 0xFFFFE7A8.toInt(), alpha = 26) }
                    val nom = e.nom.ifBlank { o.projecteurs(e.projecteur)?.nom ?: "Projecteur" }
                    vue.versCamera(P3(e.x, e.y, hp + 0.35)).takeIf { it.z > Rendu3D.PROCHE }?.let { objets.add(Etiquette(it, nom, Couleurs.PROJECTEUR)) }
                }
                TypeElement.CAMERA -> {
                    val hc = e.hauteur ?: Sol.HAUTEUR_CAMERA
                    trait(P3(e.x, e.y, 0.0), P3(e.x, e.y, hc - 0.12), 0xFF3A3A3A.toInt(), 3f)
                    boite(e, 0.3, 0.4, hc - 0.12, hc + 0.12, Couleurs.CAMERA)
                    vue.versCamera(P3(e.x, e.y, hc + 0.3)).takeIf { it.z > Rendu3D.PROCHE }?.let { objets.add(Etiquette(it, e.libelle, Couleurs.CAMERA)) }
                }
            }
        }

        /* Du plus loin au plus proche ; les étiquettes par-dessus tout. */
        val pinceau = Paint(Paint.ANTI_ALIAS_FLAG)
        val texte = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = o.texte * 0.55f; isFakeBoldText = true }
        objets.sortedByDescending { if (it is Etiquette) -1.0 else it.profondeur }.sortedBy { if (it is Etiquette) 1 else 0 }.forEach { ob ->
            when (ob) {
                is Face -> {
                    val chemin = Path()
                    ob.points.forEachIndexed { i, p ->
                        val (x, y) = vue.ecran(p, l, h)
                        if (i == 0) chemin.moveTo(x.toFloat(), y.toFloat()) else chemin.lineTo(x.toFloat(), y.toFloat())
                    }
                    chemin.close()
                    pinceau.style = Paint.Style.FILL
                    pinceau.color = ob.couleur
                    pinceau.alpha = ob.alpha
                    c.drawPath(chemin, pinceau)
                    ob.contour?.let {
                        pinceau.style = Paint.Style.STROKE
                        pinceau.strokeWidth = 1.4f
                        pinceau.color = it
                        pinceau.alpha = 255
                        c.drawPath(chemin, pinceau)
                    }
                }
                is Trait -> {
                    val (x1, y1) = vue.ecran(ob.a, l, h)
                    val (x2, y2) = vue.ecran(ob.b, l, h)
                    pinceau.style = Paint.Style.STROKE
                    pinceau.strokeWidth = ob.largeur
                    pinceau.color = ob.couleur
                    pinceau.alpha = (ob.couleur ushr 24).coerceAtLeast(1)
                    c.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), pinceau)
                }
                is Tete -> {
                    val (x, y) = vue.ecran(ob.c, l, h)
                    val r = vue.echelle(ob.rayon, ob.c.z, l).toFloat()
                    pinceau.style = Paint.Style.FILL
                    pinceau.color = ob.couleur
                    pinceau.alpha = 255
                    c.drawCircle(x.toFloat(), y.toFloat(), r, pinceau)
                }
                is Etiquette -> {
                    val (x, y) = vue.ecran(ob.p, l, h)
                    val w = texte.measureText(ob.texte)
                    pinceau.style = Paint.Style.FILL
                    pinceau.color = 0xDDFFFFFF.toInt()
                    pinceau.alpha = 220
                    c.drawRoundRect(x.toFloat() - w / 2 - 6, y.toFloat() - o.texte * 0.55f, x.toFloat() + w / 2 + 6, y.toFloat() + o.texte * 0.18f, 8f, 8f, pinceau)
                    texte.color = ob.couleur
                    c.drawText(ob.texte, x.toFloat() - w / 2, y.toFloat(), texte)
                }
            }
        }

        /* Le cartouche : ce que la vue suppose. */
        val cartouche = listOfNotNull(
            camera.libelle,
            camera.focaleMm?.let { nombreCourt(it, 0) + " mm" },
            "objectif à " + nombreCourt(camera.hauteur ?: Sol.HAUTEUR_CAMERA, 2) + " m",
            if (camera.inclinaison != 0.0) "inclinaison " + nombreCourt(camera.inclinaison, 0) + "°" else null
        ).joinToString(" · ")
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = o.texte * 0.5f; color = 0xFFFFFFFF.toInt() }
        val w = p.measureText(cartouche)
        val fond = Paint().apply { color = 0x99000000.toInt() }
        c.drawRect(8f, h.toFloat() - o.texte * 1.0f, 16f + w + 8f, h.toFloat() - 8f, fond)
        c.drawText(cartouche, 16f, h.toFloat() - o.texte * 0.35f, p)
    }

    /** Le cône de lumière d'un projecteur, en triangles : visé vers le sujet le plus proche qu'il éclaire. */
    private fun faisceau(e: ElementSol, hp: Double, sujets: List<ElementSol>, o: OptionsDessin): List<List<P3>>? {
        val choisi = if (e.projecteur.isBlank()) null else o.projecteurs(e.projecteur)
        val ouverture = Sol.faisceau(e, choisi) ?: 50.0
        val cible = sujets.filter { s ->
            Sol.ecartAngulaire(e.rotation, Sol.rotationVers(e.x, e.y, s.x, s.y)) <= ouverture / 2
        }.minByOrNull { Sol.distanceSol(e, it) }
        val tangage = cible?.let { Math.toDegrees(atan2((it.hauteur ?: Sol.HAUTEUR_VISAGE) - hp, Sol.distanceSol(e, it).coerceAtLeast(0.1))) } ?: -15.0
        val l = Math.toRadians(e.rotation)
        val t = Math.toRadians(tangage)
        val axe = P3(sin(l) * cos(t), -cos(l) * cos(t), sin(t))
        val p1 = P3(cos(l), sin(l), 0.0)
        val p2 = P3(-sin(l) * sin(t), cos(l) * sin(t), cos(t))
        val demi = Math.toRadians(ouverture / 2)
        val longueur = cible?.let { sqrt(Sol.distanceSol(e, it).let { d -> d * d } + ((it.hauteur ?: Sol.HAUTEUR_VISAGE) - hp).let { d -> d * d }) + 0.6 } ?: 3.0
        val sommet = P3(e.x, e.y, hp)
        val n = 14
        val anneau = (0 until n).map { i ->
            val a = 2 * Math.PI * i / n
            val dir = axe * cos(demi) + (p1 * cos(a) + p2 * sin(a)) * sin(demi)
            sommet + dir * longueur
        }
        return (0 until n).map { i -> listOf(sommet, anneau[i], anneau[(i + 1) % n]) }
    }
}
