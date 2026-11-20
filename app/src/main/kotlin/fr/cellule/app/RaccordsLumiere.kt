package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.MesureRaccord
import fr.cellule.core.RaccordLumiere
import fr.cellule.core.SourceRaccord
import org.json.JSONArray
import org.json.JSONObject

/** Les raccords lumière, un par plan du carnet Cinéma, gardés en JSON dans les préférences. */
object RaccordsLumiere {

    private var prefs: SharedPreferences? = null

    var raccords by mutableStateOf<List<RaccordLumiere>>(emptyList())
        private set

    fun charger(contexte: Context) {
        if (prefs != null) return
        val p = contexte.applicationContext.getSharedPreferences("cellule", Context.MODE_PRIVATE)
        prefs = p
        raccords = try {
            lire(JSONArray(p.getString("raccords_lumiere", "[]") ?: "[]"))
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun du(planId: Long): RaccordLumiere? = raccords.firstOrNull { it.planId == planId }

    fun enregistrer(r: RaccordLumiere) {
        raccords = if (raccords.any { it.planId == r.planId }) raccords.map { if (it.planId == r.planId) r else it } else raccords + r
        prefs?.edit()?.putString("raccords_lumiere", enJson().toString())?.apply()
    }

    private fun lire(a: JSONArray): List<RaccordLumiere> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        val ms = o.optJSONArray("mesures") ?: JSONArray()
        val ss = o.optJSONArray("sources") ?: JSONArray()
        RaccordLumiere(
            planId = o.optLong("plan"),
            heure = o.optString("heure", ""),
            ciel = o.optString("ciel", ""),
            mesures = (0 until ms.length()).mapNotNull { j ->
                ms.optJSONObject(j)?.let { m ->
                    MesureRaccord(
                        m.optString("endroit", ""),
                        if (m.has("lux") && !m.isNull("lux")) m.optDouble("lux").takeIf { !it.isNaN() } else null,
                        m.optString("note", "")
                    )
                }
            },
            sources = (0 until ss.length()).mapNotNull { j -> ss.optJSONObject(j)?.let { SourceRaccord(it.optString("nom", ""), it.optString("reglage", "")) } },
            planSolId = if (o.has("sol") && !o.isNull("sol")) o.optLong("sol") else null,
            notes = o.optString("notes", "")
        )
    }

    private fun enJson(): JSONArray = JSONArray().apply {
        raccords.forEach { r ->
            val o = JSONObject()
                .put("plan", r.planId).put("heure", r.heure).put("ciel", r.ciel).put("notes", r.notes)
                .put("mesures", JSONArray().apply {
                    r.mesures.forEach { m ->
                        val mo = JSONObject().put("endroit", m.endroit).put("note", m.note)
                        m.lux?.let { mo.put("lux", it) }
                        put(mo)
                    }
                })
                .put("sources", JSONArray().apply { r.sources.forEach { put(JSONObject().put("nom", it.nom).put("reglage", it.reglage)) } })
            r.planSolId?.let { o.put("sol", it) }
            put(o)
        }
    }
}
