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

/** Le journal d'entraînement, sérialisé en JSON dans les préférences. */
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
        appendLine("date;type;annonce;mesure;ecart;note")
        etat.value.forEach {
            appendLine(
                listOf(
                    it.date, it.typeScene, it.annonce, it.mesure,
                    String.format("%.2f", it.ecart), "\"" + it.note.replace("\"", "\"\"") + "\""
                ).joinToString(";")
            )
        }
    }

    private fun charger(): List<EntreeJournal> = try {
        val brut = prefs.getString("journal", "[]") ?: "[]"
        val tableau = JSONArray(brut)
        (0 until tableau.length()).map { i ->
            val o = tableau.getJSONObject(i)
            EntreeJournal(
                identifiant = o.optLong("id", i.toLong()),
                date = o.optString("date", ""),
                typeScene = o.optString("type", ""),
                annonce = o.optDouble("annonce", 0.0),
                mesure = o.optDouble("mesure", 0.0),
                note = o.optString("note", "")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun sauver() {
        val tableau = JSONArray()
        etat.value.forEach {
            tableau.put(
                JSONObject()
                    .put("id", it.identifiant)
                    .put("date", it.date)
                    .put("type", it.typeScene)
                    .put("annonce", it.annonce)
                    .put("mesure", it.mesure)
                    .put("note", it.note)
            )
        }
        prefs.edit().putString("journal", tableau.toString()).apply()
    }
}
