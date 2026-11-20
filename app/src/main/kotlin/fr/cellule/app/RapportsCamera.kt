package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.PriseCamera
import fr.cellule.core.VerdictPrise
import org.json.JSONArray
import org.json.JSONObject

/** Les prises du rapport caméra, gardées en JSON dans les préférences, comme les plans au sol. */
object RapportsCamera {

    private var prefs: SharedPreferences? = null

    var prises by mutableStateOf<List<PriseCamera>>(emptyList())
        private set

    fun charger(contexte: Context) {
        if (prefs != null) return
        val p = contexte.applicationContext.getSharedPreferences("cellule", Context.MODE_PRIVATE)
        prefs = p
        prises = try {
            lire(JSONArray(p.getString("rapports_camera", "[]") ?: "[]"))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sauver() {
        prefs?.edit()?.putString("rapports_camera", enJson().toString())?.apply()
    }

    fun prise(id: Long): PriseCamera? = prises.firstOrNull { it.identifiant == id }

    fun enregistrer(p: PriseCamera) {
        prises = if (prises.any { it.identifiant == p.identifiant }) prises.map { if (it.identifiant == p.identifiant) p else it } else prises + p
        sauver()
    }

    fun supprimer(id: Long) {
        prises = prises.filterNot { it.identifiant == id }
        sauver()
    }

    private fun lire(a: JSONArray): List<PriseCamera> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        PriseCamera(
            identifiant = o.optLong("id"),
            jourId = o.optLong("jour"),
            sequence = o.optString("seq", ""),
            plan = o.optString("plan", ""),
            prise = o.optInt("prise", 1),
            camera = o.optString("cam", "A"),
            objectif = o.optString("obj", ""),
            tStop = o.optString("t", ""),
            filtres = o.optString("filtres", ""),
            ei = o.optString("ei", ""),
            cadence = o.optString("cadence", ""),
            obturateur = o.optString("obt", ""),
            carte = o.optString("carte", ""),
            heure = o.optString("heure", ""),
            verdict = VerdictPrise.entries.firstOrNull { it.name == o.optString("verdict") } ?: VerdictPrise.A_VOIR,
            remarques = o.optString("rem", "")
        )
    }

    private fun enJson(): JSONArray = JSONArray().apply {
        prises.forEach { p ->
            put(
                JSONObject()
                    .put("id", p.identifiant).put("jour", p.jourId)
                    .put("seq", p.sequence).put("plan", p.plan).put("prise", p.prise).put("cam", p.camera)
                    .put("obj", p.objectif).put("t", p.tStop).put("filtres", p.filtres).put("ei", p.ei)
                    .put("cadence", p.cadence).put("obt", p.obturateur).put("carte", p.carte).put("heure", p.heure)
                    .put("verdict", p.verdict.name).put("rem", p.remarques)
            )
        }
    }
}
