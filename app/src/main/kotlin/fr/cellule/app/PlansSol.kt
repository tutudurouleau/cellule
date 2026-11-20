package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.ElementSol
import fr.cellule.core.PlanSol
import fr.cellule.core.TypeElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Les plans au sol, en JSON dans les préférences, et dans la sauvegarde du
 * carnet avec le carnet Cinéma. Un plan est rattaché à un jour, ou à aucun.
 */
object PlansSol {

    private var prefs: SharedPreferences? = null

    var plans by mutableStateOf<List<PlanSol>>(emptyList())
        private set

    fun charger(contexte: Context) {
        if (prefs != null) return
        val p = contexte.applicationContext.getSharedPreferences("cellule", Context.MODE_PRIVATE)
        prefs = p
        plans = try {
            lire(JSONArray(p.getString("plans_sol", "[]") ?: "[]"))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun sauver() {
        prefs?.edit()?.putString("plans_sol", enJson().toString())?.apply()
    }

    fun plan(id: Long): PlanSol? = plans.firstOrNull { it.identifiant == id }
    fun duJour(jourId: Long): List<PlanSol> = plans.filter { it.jourId == jourId }

    fun nouveau(nom: String, jourId: Long?): PlanSol {
        val p = PlanSol(System.currentTimeMillis(), nom.ifBlank { "Plan au sol" }, jourId)
        plans = plans + p
        sauver()
        return p
    }

    fun enregistrer(p: PlanSol) {
        plans = if (plans.any { it.identifiant == p.identifiant }) plans.map { if (it.identifiant == p.identifiant) p else it } else plans + p
        sauver()
    }

    fun supprimer(id: Long) {
        plans = plans.filterNot { it.identifiant == id }
        sauver()
    }

    fun importer(tableau: JSONArray): Int {
        val nouveaux = lire(tableau).filter { n -> plans.none { it.identifiant == n.identifiant } }
        plans = plans + nouveaux
        sauver()
        return nouveaux.size
    }

    private fun JSONObject.reel(cle: String): Double? = if (has(cle) && !isNull(cle)) optDouble(cle).takeIf { !it.isNaN() } else null

    private fun lire(a: JSONArray): List<PlanSol> = (0 until a.length()).mapNotNull { i ->
        val o = a.optJSONObject(i) ?: return@mapNotNull null
        val els = o.optJSONArray("elements") ?: JSONArray()
        PlanSol(
            identifiant = o.optLong("id", i.toLong()),
            nom = o.optString("nom", ""),
            jourId = if (o.has("jour") && !o.isNull("jour")) o.optLong("jour") else null,
            elements = (0 until els.length()).mapNotNull { j -> els.optJSONObject(j)?.let { lireElement(it, j) } },
            grille = o.optBoolean("grille", true),
            pasGrille = o.optDouble("pas", 0.5),
            aimanter = o.optBoolean("aimanter", false),
            legende = o.optBoolean("legende", true),
            nordDeg = o.optDouble("nord", 0.0),
            soleil = o.optBoolean("soleil", true),
            date = o.optString("date", ""),
            heure = o.optString("heure", ""),
            latitude = o.reel("lat"),
            longitude = o.reel("lon"),
            carte = o.optBoolean("carte", false),
            plafond = o.reel("plafond"),
            notes = o.optString("notes", "")
        )
    }

    private fun lireElement(o: JSONObject, rang: Int): ElementSol? {
        val type = runCatching { TypeElement.valueOf(o.optString("type")) }.getOrNull() ?: return null
        return ElementSol(
            identifiant = o.optLong("id", rang.toLong()),
            type = type,
            x = o.optDouble("x", 0.0),
            y = o.optDouble("y", 0.0),
            rotation = o.optDouble("rot", 0.0),
            largeur = o.optDouble("l", 0.5),
            profondeur = o.optDouble("p", 0.5),
            hauteur = o.reel("h"),
            nom = o.optString("nom", ""),
            etape = o.optInt("etape", 1),
            focaleMm = o.reel("focale"),
            capteurLargeurMm = o.reel("cl"),
            capteurHauteurMm = o.reel("ch"),
            desanamorphose = o.optDouble("desq", 1.0),
            inclinaison = o.optDouble("incl", 0.0),
            plans = o.optString("plans", ""),
            projecteur = o.optString("projo", ""),
            spot = o.optBoolean("spot", false),
            luxReference = o.reel("lux"),
            distanceReference = o.reel("dref"),
            faisceau = o.reel("faisceau"),
            pertesDiaphs = o.optDouble("pertes", 0.0),
            notes = o.optString("notes", "")
        )
    }

    private fun ecrireElement(e: ElementSol): JSONObject {
        val o = JSONObject()
            .put("id", e.identifiant).put("type", e.type.name)
            .put("x", e.x).put("y", e.y).put("rot", e.rotation)
            .put("l", e.largeur).put("p", e.profondeur)
            .put("nom", e.nom).put("etape", e.etape)
            .put("desq", e.desanamorphose).put("incl", e.inclinaison)
            .put("plans", e.plans).put("projo", e.projecteur).put("spot", e.spot)
            .put("pertes", e.pertesDiaphs).put("notes", e.notes)
        e.hauteur?.let { o.put("h", it) }
        e.focaleMm?.let { o.put("focale", it) }
        e.capteurLargeurMm?.let { o.put("cl", it) }
        e.capteurHauteurMm?.let { o.put("ch", it) }
        e.luxReference?.let { o.put("lux", it) }
        e.distanceReference?.let { o.put("dref", it) }
        e.faisceau?.let { o.put("faisceau", it) }
        return o
    }

    fun enJson(): JSONArray {
        val a = JSONArray()
        plans.forEach { p ->
            val els = JSONArray()
            p.elements.forEach { els.put(ecrireElement(it)) }
            val o = JSONObject()
                .put("id", p.identifiant).put("nom", p.nom).put("elements", els)
                .put("grille", p.grille).put("pas", p.pasGrille).put("aimanter", p.aimanter).put("legende", p.legende)
                .put("nord", p.nordDeg).put("soleil", p.soleil).put("date", p.date).put("heure", p.heure)
                .put("carte", p.carte).put("notes", p.notes)
            p.jourId?.let { o.put("jour", it) }
            p.latitude?.let { o.put("lat", it) }
            p.longitude?.let { o.put("lon", it) }
            p.plafond?.let { o.put("plafond", it) }
            a.put(o)
        }
        return a
    }
}
