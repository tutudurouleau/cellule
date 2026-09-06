package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import fr.cellule.core.EntreeJournal
import org.json.JSONArray
import org.json.JSONObject
import kotlin.reflect.KProperty

/**
 * Préférences persistées et observables par Compose. Volontairement sur
 * SharedPreferences : quelques valeurs, aucune migration, aucune dépendance.
 */
class Reglages(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    /** Sensibilité du film chargé dans le boîtier, pas celle du téléphone. */
    var iso by EntierPref(prefs, "iso", 100)

    /** La pellicule en cours : elle porte l'ISO et la latitude à respecter. */
    var pellicule by TextePref(prefs, "pellicule", "Kodak Portra 400")

    /** Vitesse de référence en secondes ; 0 signifie « 1/ISO », la règle du Sunny 16. */
    var vitesseReference by ReelPref(prefs, "vitesseReference", 0.0)

    /** Décalage mesuré une fois sur charte grise, en diaphs. */
    var etalonnage by ReelPref(prefs, "etalonnage", 0.0)

    /**
     * Le plan Y arrive en plage vidéo (16-235) sur la plupart des appareils,
     * en plage pleine (0-255) sur certains. Si la charte grise lit toujours à
     * côté d'un demi-diaph, c'est le premier réglage à essayer.
     */
    var plageVideo by BooleenPref(prefs, "plageVideo", true)

    var latitude by ReelPref(prefs, "latitude", 42.30)
    var longitude by ReelPref(prefs, "longitude", 9.15)
    var fuseau by ReelPref(prefs, "fuseau", 2.0)
}

class BooleenPref(private val p: SharedPreferences, private val cle: String, defaut: Boolean) {
    private val etat = mutableStateOf(p.getBoolean(cle, defaut))
    operator fun getValue(hote: Any?, prop: KProperty<*>): Boolean = etat.value
    operator fun setValue(hote: Any?, prop: KProperty<*>, v: Boolean) {
        etat.value = v
        p.edit().putBoolean(cle, v).apply()
    }
}

class EntierPref(private val p: SharedPreferences, private val cle: String, defaut: Int) {
    private val etat = mutableStateOf(p.getInt(cle, defaut))
    operator fun getValue(hote: Any?, prop: KProperty<*>): Int = etat.value
    operator fun setValue(hote: Any?, prop: KProperty<*>, v: Int) {
        etat.value = v
        p.edit().putInt(cle, v).apply()
    }
}

class ReelPref(private val p: SharedPreferences, private val cle: String, defaut: Double) {
    private val etat = mutableStateOf(p.getFloat(cle, defaut.toFloat()).toDouble())
    operator fun getValue(hote: Any?, prop: KProperty<*>): Double = etat.value
    operator fun setValue(hote: Any?, prop: KProperty<*>, v: Double) {
        etat.value = v
        p.edit().putFloat(cle, v.toFloat()).apply()
    }
}

class TextePref(private val p: SharedPreferences, private val cle: String, defaut: String) {
    private val etat = mutableStateOf(p.getString(cle, defaut) ?: defaut)
    operator fun getValue(hote: Any?, prop: KProperty<*>): String = etat.value
    operator fun setValue(hote: Any?, prop: KProperty<*>, v: String) {
        etat.value = v
        p.edit().putString(cle, v).apply()
    }
}

/**
 * Le carnet, sérialisé en JSON dans les préférences.
 *
 * Chaque champ se relit avec un défaut : un carnet écrit par une version
 * antérieure de l'application se rouvre sans rien perdre.
 */
class DepotJournal(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    private val etat = mutableStateOf(charger())

    val entrees: List<EntreeJournal> get() = etat.value

    fun ajouter(entree: EntreeJournal) {
        etat.value = etat.value + entree
        sauver()
    }

    fun supprimer(identifiant: Long) {
        etat.value = etat.value.filterNot { it.identifiant == identifiant }
        sauver()
    }

    fun vider() {
        etat.value = emptyList()
        sauver()
    }

    fun versCsv(): String = buildString {
        appendLine("date;heure;pellicule;vue;sujet;scene;annonce;mesure;ecart;reglage;note")
        etat.value.forEach { e ->
            appendLine(
                listOf(
                    e.date, e.heure, e.pellicule, e.vue,
                    guillemets(e.sujet), e.typeScene,
                    e.annonce?.let { String.format("%.1f", it) } ?: "",
                    e.mesure?.let { String.format("%.1f", it) } ?: "",
                    e.ecart?.let { String.format("%.2f", it) } ?: "",
                    e.reglage, guillemets(e.note)
                ).joinToString(";")
            )
        }
    }

    private fun guillemets(t: String) = "\"" + t.replace("\"", "\"\"") + "\""

    private fun charger(): List<EntreeJournal> = try {
        val brut = prefs.getString("journal", "[]") ?: "[]"
        val tableau = JSONArray(brut)
        (0 until tableau.length()).map { i ->
            val o = tableau.getJSONObject(i)
            EntreeJournal(
                identifiant = o.optLong("id", i.toLong()),
                date = o.optString("date", ""),
                heure = o.optString("heure", ""),
                pellicule = o.optString("pellicule", ""),
                vue = o.optString("vue", ""),
                sujet = o.optString("sujet", ""),
                typeScene = o.optString("type", ""),
                annonce = if (o.has("annonce") && !o.isNull("annonce")) o.optDouble("annonce") else null,
                mesure = if (o.has("mesure") && !o.isNull("mesure")) o.optDouble("mesure") else null,
                reglage = o.optString("reglage", ""),
                note = o.optString("note", "")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun sauver() {
        val tableau = JSONArray()
        etat.value.forEach { e ->
            val o = JSONObject()
                .put("id", e.identifiant)
                .put("date", e.date)
                .put("heure", e.heure)
                .put("pellicule", e.pellicule)
                .put("vue", e.vue)
                .put("sujet", e.sujet)
                .put("type", e.typeScene)
                .put("reglage", e.reglage)
                .put("note", e.note)
            e.annonce?.let { o.put("annonce", it) }
            e.mesure?.let { o.put("mesure", it) }
            tableau.put(o)
        }
        prefs.edit().putString("journal", tableau.toString()).apply()
    }
}
