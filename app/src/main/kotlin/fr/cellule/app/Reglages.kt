package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import fr.cellule.core.Calculateur
import fr.cellule.core.EntreeJournal
import fr.cellule.core.Pronostic
import fr.cellule.core.SauvegardeCarnet
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

    /**
     * Le carnet entier, dans un fichier qu'on range où l'on veut (Drive,
     * Téléchargements, un mail à soi-même) : de quoi changer de téléphone.
     */
    fun versSauvegarde(): String = JSONObject()
        .put("format", SauvegardeCarnet.FORMAT)
        .put("version", 1)
        .put("entrees", enJson(etat.value))
        .toString(2)

    /**
     * Relit une sauvegarde — ou, faute de mieux, le fichier de préférences
     * d'une ancienne installation — et ajoute les vues qui manquent.
     * Renvoie le nombre de vues ajoutées ; lève une exception si le fichier
     * n'est pas un carnet.
     */
    fun importer(texte: String): Int {
        val brut = texte.trim().removePrefix("\uFEFF")
        val json = if (brut.startsWith("<")) {
            SauvegardeCarnet.extraireDesPreferences(brut)
                ?: throw IllegalArgumentException("aucun carnet dans ce fichier")
        } else brut
        val tableau = if (json.trimStart().startsWith("[")) JSONArray(json)
        else JSONObject(json).getJSONArray("entrees")
        val avant = etat.value.size
        etat.value = SauvegardeCarnet.fusionner(etat.value, lire(tableau))
        sauver()
        return etat.value.size - avant
    }

    private fun charger(): List<EntreeJournal> = try {
        lire(JSONArray(prefs.getString("journal", "[]") ?: "[]"))
    } catch (e: Exception) {
        emptyList()
    }

    private fun lire(tableau: JSONArray): List<EntreeJournal> =
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

    private fun sauver() {
        prefs.edit().putString("journal", enJson(etat.value).toString()).apply()
    }

    private fun enJson(entrees: List<EntreeJournal>): JSONArray {
        val tableau = JSONArray()
        entrees.forEach { e ->
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
        return tableau
    }
}

/**
 * Les paris du Labo : ce que tu avais estimé avant de voir le calcul, et le
 * calcul lui-même. C'est la trace de ta progression — l'intuition qui, pari
 * après pari, rejoint le calcul.
 */
class DepotPronostics(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    private val etat = mutableStateOf(charger())

    val liste: List<Pronostic> get() = etat.value

    fun ajouter(p: Pronostic) {
        etat.value = etat.value + p
        sauver()
    }

    private fun charger(): List<Pronostic> = try {
        val tableau = JSONArray(prefs.getString("pronostics", "[]") ?: "[]")
        (0 until tableau.length()).mapNotNull { i ->
            val o = tableau.getJSONObject(i)
            /* Un calculateur renommé ou retiré ne doit pas faire perdre les autres paris. */
            val calculateur = runCatching { Calculateur.valueOf(o.getString("calculateur")) }.getOrNull()
                ?: return@mapNotNull null
            Pronostic(
                calculateur = calculateur,
                date = o.optString("date", ""),
                estime = o.getDouble("estime"),
                calcule = o.getDouble("calcule"),
                contexte = o.optString("contexte", "")
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun sauver() {
        val tableau = JSONArray()
        etat.value.forEach { p ->
            tableau.put(
                JSONObject()
                    .put("calculateur", p.calculateur.name)
                    .put("date", p.date)
                    .put("estime", p.estime)
                    .put("calcule", p.calcule)
                    .put("contexte", p.contexte)
            )
        }
        prefs.edit().putString("pronostics", tableau.toString()).apply()
    }
}
