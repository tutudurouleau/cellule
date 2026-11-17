package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.CameraPerso
import fr.cellule.core.FamilleProjecteur
import fr.cellule.core.MesureProjecteur
import fr.cellule.core.PROJECTEURS_CATALOGUE
import fr.cellule.core.Projecteur
import fr.cellule.core.SeriePerso
import fr.cellule.core.ZoneUtile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Le matériel à soi : caméras, séries d'objectifs et projecteurs saisis à la
 * main. Un seul exemplaire pour toute l'appli (le Matériel, le viseur, la
 * Lumière et le plan au sol s'en servent), en JSON dans les préférences, et
 * dans la sauvegarde du carnet avec le carnet Cinéma.
 */
object MonMateriel {

    /** La mention portée par tout chiffre saisi par l'utilisateur. */
    const val SAISI = "saisi par toi"

    /** Préfixe des identifiants de sélection (« perso:1731… ») dans les réglages. */
    const val PREFIXE = "perso:"

    private var prefs: SharedPreferences? = null

    var cameras by mutableStateOf<List<CameraPerso>>(emptyList())
        private set
    var series by mutableStateOf<List<SeriePerso>>(emptyList())
        private set
    var projecteurs by mutableStateOf<List<Projecteur>>(emptyList())
        private set

    fun charger(contexte: Context) {
        if (prefs != null) return
        val p = contexte.applicationContext.getSharedPreferences("cellule", Context.MODE_PRIVATE)
        prefs = p
        try {
            lire(JSONObject(p.getString("materiel_perso", "{}") ?: "{}")).let { (c, s, pr) ->
                cameras = c
                series = s
                projecteurs = pr
            }
        } catch (e: Exception) {
            /* Un enregistrement illisible ne doit pas empêcher l'appli de démarrer. */
        }
    }

    private fun sauver() {
        prefs?.edit()?.putString("materiel_perso", enJson().toString())?.apply()
    }

    fun cle(c: CameraPerso) = PREFIXE + c.identifiant
    fun cle(s: SeriePerso) = PREFIXE + s.identifiant

    fun camera(cle: String): CameraPerso? = cameras.firstOrNull { cle(it) == cle }
    fun serie(cle: String): SeriePerso? = series.firstOrNull { cle(it) == cle }
    /** Un projecteur à soi, ou du catalogue. */
    fun projecteur(identifiant: String): Projecteur? =
        projecteurs.firstOrNull { it.identifiant == identifiant } ?: PROJECTEURS_CATALOGUE.firstOrNull { it.identifiant == identifiant }

    /** Tous les projecteurs qu'on peut choisir : les siens d'abord, puis le catalogue. */
    val tousLesProjecteurs: List<Projecteur> get() = projecteurs + PROJECTEURS_CATALOGUE

    fun libelle(p: Projecteur): String = (if (p.perso) "★ " else "") + p.nomComplet

    fun enregistrer(c: CameraPerso) {
        cameras = if (cameras.any { it.identifiant == c.identifiant }) cameras.map { if (it.identifiant == c.identifiant) c else it } else cameras + c
        sauver()
    }

    fun enregistrer(s: SeriePerso) {
        series = if (series.any { it.identifiant == s.identifiant }) series.map { if (it.identifiant == s.identifiant) s else it } else series + s
        sauver()
    }

    fun enregistrer(p: Projecteur) {
        val q = p.copy(perso = true, source = SAISI)
        projecteurs = if (projecteurs.any { it.identifiant == q.identifiant }) projecteurs.map { if (it.identifiant == q.identifiant) q else it } else projecteurs + q
        sauver()
    }

    fun supprimerCamera(identifiant: Long) {
        cameras = cameras.filterNot { it.identifiant == identifiant }
        sauver()
    }

    fun supprimerSerie(identifiant: Long) {
        series = series.filterNot { it.identifiant == identifiant }
        sauver()
    }

    fun supprimerProjecteur(identifiant: String) {
        projecteurs = projecteurs.filterNot { it.identifiant == identifiant }
        sauver()
    }

    /** Ajoute ce qui manque d'une sauvegarde, sans rien écraser. Rend le nombre d'éléments ajoutés. */
    fun importer(o: JSONObject): Int {
        val (c, s, p) = lire(o)
        val nc = c.filter { n -> cameras.none { it.identifiant == n.identifiant } }
        val ns = s.filter { n -> series.none { it.identifiant == n.identifiant } }
        val np = p.filter { n -> projecteurs.none { it.identifiant == n.identifiant } }
        cameras = cameras + nc
        series = series + ns
        projecteurs = projecteurs + np
        sauver()
        return nc.size + ns.size + np.size
    }

    private fun JSONObject.reel(cle: String): Double? = if (has(cle) && !isNull(cle)) optDouble(cle).takeIf { !it.isNaN() } else null

    private fun lire(o: JSONObject): Triple<List<CameraPerso>, List<SeriePerso>, List<Projecteur>> {
        val c = o.optJSONArray("cameras") ?: JSONArray()
        val s = o.optJSONArray("series") ?: JSONArray()
        val p = o.optJSONArray("projecteurs") ?: JSONArray()
        val cameras = (0 until c.length()).mapNotNull { i ->
            val x = c.optJSONObject(i) ?: return@mapNotNull null
            val modes = x.optJSONArray("modes") ?: JSONArray()
            CameraPerso(
                identifiant = x.optLong("id", i.toLong()),
                nom = x.optString("nom", ""),
                modes = (0 until modes.length()).mapNotNull { j ->
                    val m = modes.optJSONObject(j) ?: return@mapNotNull null
                    val l = m.reel("l") ?: return@mapNotNull null
                    val h = m.reel("h") ?: return@mapNotNull null
                    ZoneUtile(m.optString("nom", ""), l, h, SAISI)
                },
                notes = x.optString("notes", "")
            )
        }
        val series = (0 until s.length()).mapNotNull { i ->
            val x = s.optJSONObject(i) ?: return@mapNotNull null
            val f = x.optJSONArray("focales") ?: JSONArray()
            SeriePerso(
                identifiant = x.optLong("id", i.toLong()),
                nom = x.optString("nom", ""),
                focales = (0 until f.length()).map { f.optDouble(it) }.filter { !it.isNaN() && it > 0 },
                tMax = x.reel("tmax"),
                desanamorphose = x.optDouble("desanamorphose", 1.0),
                notes = x.optString("notes", "")
            )
        }
        val projecteurs = (0 until p.length()).mapNotNull { i ->
            val x = p.optJSONObject(i) ?: return@mapNotNull null
            val m = x.optJSONArray("mesures") ?: JSONArray()
            Projecteur(
                identifiant = x.optString("id", "perso-$i"),
                nom = x.optString("nom", ""),
                fabricant = x.optString("fabricant", ""),
                famille = runCatching { FamilleProjecteur.valueOf(x.optString("famille")) }.getOrDefault(FamilleProjecteur.AUTRE),
                puissanceW = x.reel("w"),
                temperatureK = if (x.has("k") && !x.isNull("k")) x.optInt("k") else null,
                angleFlood = x.reel("flood"),
                angleSpot = x.reel("spot"),
                mesures = (0 until m.length()).mapNotNull { j ->
                    val y = m.optJSONObject(j) ?: return@mapNotNull null
                    val d = y.reel("d") ?: return@mapNotNull null
                    val lux = y.reel("lux") ?: return@mapNotNull null
                    MesureProjecteur(y.optBoolean("spot", false), d, lux)
                },
                source = SAISI,
                perso = true,
                notes = x.optString("notes", "")
            )
        }
        return Triple(cameras, series, projecteurs)
    }

    fun enJson(): JSONObject {
        val c = JSONArray()
        cameras.forEach { x ->
            val modes = JSONArray()
            x.modes.forEach { modes.put(JSONObject().put("nom", it.nom).put("l", it.largeurMm).put("h", it.hauteurMm)) }
            c.put(JSONObject().put("id", x.identifiant).put("nom", x.nom).put("modes", modes).put("notes", x.notes))
        }
        val s = JSONArray()
        series.forEach { x ->
            val o = JSONObject().put("id", x.identifiant).put("nom", x.nom).put("focales", JSONArray(x.focales))
                .put("desanamorphose", x.desanamorphose).put("notes", x.notes)
            x.tMax?.let { o.put("tmax", it) }
            s.put(o)
        }
        val p = JSONArray()
        projecteurs.forEach { x ->
            val mesures = JSONArray()
            x.mesures.forEach { mesures.put(JSONObject().put("spot", it.spot).put("d", it.distanceM).put("lux", it.lux)) }
            val o = JSONObject().put("id", x.identifiant).put("nom", x.nom).put("fabricant", x.fabricant)
                .put("famille", x.famille.name).put("mesures", mesures).put("notes", x.notes)
            x.puissanceW?.let { o.put("w", it) }
            x.temperatureK?.let { o.put("k", it) }
            x.angleFlood?.let { o.put("flood", it) }
            x.angleSpot?.let { o.put("spot", it) }
            p.put(o)
        }
        return JSONObject().put("cameras", c).put("series", s).put("projecteurs", p)
    }
}
