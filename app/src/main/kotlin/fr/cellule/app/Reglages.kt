package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import fr.cellule.core.Calculateur
import fr.cellule.core.CarnetLabo
import fr.cellule.core.CarnetTirage
import fr.cellule.core.Correction
import fr.cellule.core.DeveloppementNote
import fr.cellule.core.EntreeJournal
import fr.cellule.core.Pronostic
import fr.cellule.core.Resultat
import fr.cellule.core.SauvegardeCarnet
import fr.cellule.core.TirageNote
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
     * Version 2 : les développements, les tirages et les paris du Labo
     * voyagent avec les vues ; une version antérieure de l'appli relit les
     * vues et ignore le reste.
     */
    fun versSauvegarde(labo: DepotLabo, tirages: DepotTirages, pronostics: DepotPronostics): String = JSONObject()
        .put("format", SauvegardeCarnet.FORMAT)
        .put("version", 2)
        .put("entrees", enJson(etat.value))
        .put("developpements", labo.enJson())
        .put("tirages", tirages.enJson())
        .put("pronostics", pronostics.enJson())
        .toString(2)

    /**
     * Relit une sauvegarde — ou, faute de mieux, le fichier de préférences
     * d'une ancienne installation — et ajoute les vues, les développements,
     * les tirages et les paris qui manquent. Renvoie le nombre de vues, de
     * développements et de tirages ajoutés ; lève une exception si le fichier
     * n'est pas un carnet.
     */
    fun importer(texte: String, labo: DepotLabo, tirages: DepotTirages, pronostics: DepotPronostics): Int {
        val brut = texte.trim().removePrefix("\uFEFF")
        val json = if (brut.startsWith("<")) {
            SauvegardeCarnet.extraireDesPreferences(brut)
                ?: throw IllegalArgumentException("aucun carnet dans ce fichier")
        } else brut
        val objet = if (json.trimStart().startsWith("[")) null else JSONObject(json)
        val tableau = objet?.optJSONArray("entrees") ?: if (objet == null) JSONArray(json) else JSONArray()
        if (objet != null && !objet.has("entrees") && !objet.has("developpements") && !objet.has("tirages")) {
            throw IllegalArgumentException("aucun carnet dans ce fichier")
        }
        val avant = etat.value.size
        etat.value = SauvegardeCarnet.fusionner(etat.value, lire(tableau))
        sauver()
        val developpements = objet?.optJSONArray("developpements")?.let { labo.importer(it) } ?: 0
        val nouveauxTirages = objet?.optJSONArray("tirages")?.let { tirages.importer(it) } ?: 0
        objet?.optJSONArray("pronostics")?.let { pronostics.importer(it) }
        return etat.value.size - avant + developpements + nouveauxTirages
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
        lire(JSONArray(prefs.getString("pronostics", "[]") ?: "[]"))
    } catch (e: Exception) {
        emptyList()
    }

    private fun lire(tableau: JSONArray): List<Pronostic> =
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

    /** Ajoute les paris d'une sauvegarde qui ne sont pas déjà là. */
    fun importer(tableau: JSONArray) {
        val nouveaux = lire(tableau).filterNot { it in etat.value }
        if (nouveaux.isNotEmpty()) {
            etat.value = etat.value + nouveaux
            sauver()
        }
    }

    fun enJson(): JSONArray {
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
        return tableau
    }

    private fun sauver() {
        prefs.edit().putString("pronostics", enJson().toString()).apply()
    }
}

/**
 * Le carnet de développement du Labo : un film développé par ligne. Même
 * logique que le carnet de vues — JSON dans les préférences, champs relus
 * avec un défaut, fusion sans doublon à la restauration.
 */
class DepotLabo(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    private val etat = mutableStateOf(charger())

    val notes: List<DeveloppementNote> get() = etat.value

    fun ajouter(n: DeveloppementNote) {
        etat.value = etat.value + n
        sauver()
    }

    fun supprimer(identifiant: Long) {
        etat.value = etat.value.filterNot { it.identifiant == identifiant }
        sauver()
    }

    /** Renvoie le nombre de développements ajoutés. */
    fun importer(tableau: JSONArray): Int {
        val avant = etat.value.size
        etat.value = CarnetLabo.fusionner(etat.value, lire(tableau))
        sauver()
        return etat.value.size - avant
    }

    private fun charger(): List<DeveloppementNote> = try {
        lire(JSONArray(prefs.getString("developpements", "[]") ?: "[]"))
    } catch (e: Exception) {
        emptyList()
    }

    private fun JSONObject.reel(cle: String): Double? = if (has(cle) && !isNull(cle)) optDouble(cle) else null

    private fun lire(tableau: JSONArray): List<DeveloppementNote> =
        (0 until tableau.length()).map { i ->
            val o = tableau.getJSONObject(i)
            DeveloppementNote(
                identifiant = o.optLong("id", i.toLong()),
                date = o.optString("date", ""),
                film = o.optString("film", ""),
                ei = if (o.has("ei") && !o.isNull("ei")) o.optInt("ei") else null,
                revelateur = o.optString("revelateur", ""),
                dilution = o.optString("dilution", ""),
                temperature = o.reel("temperature"),
                tempsCalcule = o.reel("tempsCalcule"),
                estimation = o.reel("estimation"),
                tempsDonne = o.reel("tempsDonne"),
                cuve = o.optString("cuve", ""),
                agitation = o.optString("agitation", ""),
                resultat = runCatching { Resultat.valueOf(o.getString("resultat")) }.getOrNull(),
                notes = o.optString("notes", "")
            )
        }

    fun enJson(): JSONArray {
        val tableau = JSONArray()
        etat.value.forEach { n ->
            val o = JSONObject()
                .put("id", n.identifiant)
                .put("date", n.date)
                .put("film", n.film)
                .put("revelateur", n.revelateur)
                .put("dilution", n.dilution)
                .put("cuve", n.cuve)
                .put("agitation", n.agitation)
                .put("notes", n.notes)
            n.ei?.let { o.put("ei", it) }
            n.temperature?.let { o.put("temperature", it) }
            n.tempsCalcule?.let { o.put("tempsCalcule", it) }
            n.estimation?.let { o.put("estimation", it) }
            n.tempsDonne?.let { o.put("tempsDonne", it) }
            n.resultat?.let { o.put("resultat", it.name) }
            tableau.put(o)
        }
        return tableau
    }

    private fun sauver() {
        prefs.edit().putString("developpements", enJson().toString()).apply()
    }
}

/** Le carnet de tirage : chaque épreuve, ses réglages et ses corrections en diaphs. */
class DepotTirages(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    private val etat = mutableStateOf(charger())

    val tirages: List<TirageNote> get() = etat.value

    fun ajouter(t: TirageNote) {
        etat.value = etat.value + t
        sauver()
    }

    fun supprimer(identifiant: Long) {
        etat.value = etat.value.filterNot { it.identifiant == identifiant }
        sauver()
    }

    fun importer(tableau: JSONArray): Int {
        val avant = etat.value.size
        etat.value = CarnetTirage.fusionner(etat.value, lire(tableau))
        sauver()
        return etat.value.size - avant
    }

    private fun charger(): List<TirageNote> = try {
        lire(JSONArray(prefs.getString("tirages", "[]") ?: "[]"))
    } catch (e: Exception) {
        emptyList()
    }

    private fun lire(tableau: JSONArray): List<TirageNote> =
        (0 until tableau.length()).map { i ->
            val o = tableau.getJSONObject(i)
            val corrections = o.optJSONArray("corrections") ?: JSONArray()
            TirageNote(
                identifiant = o.optLong("id", i.toLong()),
                date = o.optString("date", ""),
                negatif = o.optString("negatif", ""),
                papier = o.optString("papier", ""),
                filtre = o.optString("filtre", ""),
                ouverture = o.optString("ouverture", ""),
                format = o.optString("format", ""),
                base = if (o.has("base") && !o.isNull("base")) o.optDouble("base") else null,
                corrections = (0 until corrections.length()).map { j ->
                    val c = corrections.getJSONObject(j)
                    Correction(c.optString("zone", ""), c.optDouble("diaphs", 0.0))
                },
                notes = o.optString("notes", "")
            )
        }

    fun enJson(): JSONArray {
        val tableau = JSONArray()
        etat.value.forEach { t ->
            val corrections = JSONArray()
            t.corrections.forEach { corrections.put(JSONObject().put("zone", it.zone).put("diaphs", it.diaphs)) }
            val o = JSONObject()
                .put("id", t.identifiant)
                .put("date", t.date)
                .put("negatif", t.negatif)
                .put("papier", t.papier)
                .put("filtre", t.filtre)
                .put("ouverture", t.ouverture)
                .put("format", t.format)
                .put("corrections", corrections)
                .put("notes", t.notes)
            t.base?.let { o.put("base", it) }
            tableau.put(o)
        }
        return tableau
    }

    private fun sauver() {
        prefs.edit().putString("tirages", enJson().toString()).apply()
    }
}
