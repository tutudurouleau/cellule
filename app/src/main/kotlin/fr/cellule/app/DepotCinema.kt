package fr.cellule.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import fr.cellule.core.CadrePlan
import fr.cellule.core.CarnetCinema
import fr.cellule.core.CarnetCinemaDonnees
import fr.cellule.core.ChampPhoto
import fr.cellule.core.ExpositionPlan
import fr.cellule.core.JourCinema
import fr.cellule.core.LienVue
import fr.cellule.core.LieuPlan
import fr.cellule.core.Numerotation
import fr.cellule.core.Plan
import fr.cellule.core.Projet
import fr.cellule.core.SoleilPlan
import fr.cellule.core.TypeJour
import org.json.JSONArray
import org.json.JSONObject

/** Le cadre d'un plan, en JSON : sert au carnet comme au « cadre de travail » des réglages. */
object CadreJson {

    private fun JSONObject.reel(cle: String): Double? = if (has(cle) && !isNull(cle)) optDouble(cle) else null

    fun lire(o: JSONObject?): CadrePlan {
        if (o == null) return CadrePlan()
        val formats = o.optJSONArray("formats") ?: JSONArray()
        return CadrePlan(
            capteurNom = o.optString("capteur", ""),
            capteurLargeurMm = o.reel("largeur"),
            capteurHauteurMm = o.reel("hauteur"),
            focaleMm = o.reel("focale"),
            desanamorphose = o.optDouble("desanamorphose", 1.0),
            formats = (0 until formats.length()).map { formats.optString(it) }.filter { it.isNotBlank() }
        )
    }

    fun lireTexte(texte: String): CadrePlan = try {
        lire(JSONObject(texte))
    } catch (e: Exception) {
        CadrePlan()
    }

    fun ecrire(c: CadrePlan): JSONObject {
        val o = JSONObject()
            .put("capteur", c.capteurNom)
            .put("desanamorphose", c.desanamorphose)
            .put("formats", JSONArray(c.formats))
        c.capteurLargeurMm?.let { o.put("largeur", it) }
        c.capteurHauteurMm?.let { o.put("hauteur", it) }
        c.focaleMm?.let { o.put("focale", it) }
        return o
    }
}

/**
 * Le carnet « Cinéma » : projets, jours, plans, et les vues photo qu'on y
 * range. Même logique que les autres carnets — JSON dans les préférences,
 * chaque champ relu avec un défaut, fusion sans doublon à la restauration.
 * Les photos, elles, restent dans la galerie : on n'en garde que l'adresse.
 */
class DepotCinema(contexte: Context) {

    private val prefs: SharedPreferences =
        contexte.getSharedPreferences("cellule", Context.MODE_PRIVATE)

    private val etat = mutableStateOf(charger())

    val donnees: CarnetCinemaDonnees get() = etat.value
    val plans: List<Plan> get() = etat.value.plans

    fun plan(identifiant: Long): Plan? = etat.value.plans.firstOrNull { it.identifiant == identifiant }

    private fun poser(d: CarnetCinemaDonnees) {
        etat.value = d
        prefs.edit().putString("cinema", enJson().toString()).apply()
    }

    /** Ajoute le plan, ou remplace celui qui porte le même identifiant. */
    fun enregistrer(plan: Plan) = poser(CarnetCinema.remplacer(etat.value, plan))

    /** Modifie un plan en repartant de sa version la plus récente. */
    fun modifier(identifiant: Long, changement: (Plan) -> Plan) {
        val actuel = plan(identifiant) ?: return
        enregistrer(changement(actuel))
    }

    fun supprimerPlan(identifiant: Long) =
        poser(etat.value.copy(plans = etat.value.plans.filterNot { it.identifiant == identifiant }))

    fun deplacer(planId: Long, jourId: Long?) = poser(CarnetCinema.deplacer(etat.value, planId, jourId))

    fun ajouterProjet(nom: String): Projet {
        val p = Projet(System.currentTimeMillis(), nom.trim())
        poser(etat.value.copy(projets = etat.value.projets + p))
        return p
    }

    fun renommerProjet(identifiant: Long, nom: String) = poser(
        etat.value.copy(projets = etat.value.projets.map { if (it.identifiant == identifiant) it.copy(nom = nom.trim()) else it })
    )

    fun supprimerProjet(identifiant: Long) = poser(CarnetCinema.sansProjet(etat.value, identifiant))

    fun ajouterJour(projetId: Long, nom: String, date: String, type: TypeJour): JourCinema {
        val j = JourCinema(System.currentTimeMillis(), projetId, nom.trim(), date.trim(), type)
        poser(etat.value.copy(jours = etat.value.jours + j))
        return j
    }

    fun modifierJour(jour: JourCinema) = poser(
        etat.value.copy(jours = etat.value.jours.map { if (it.identifiant == jour.identifiant) jour else it })
    )

    fun supprimerJour(identifiant: Long) = poser(CarnetCinema.sansJour(etat.value, identifiant))

    fun lierVue(vueId: Long, projetId: Long?) = poser(CarnetCinema.lier(etat.value, vueId, projetId))

    /** Renvoie le nombre de projets, de jours et de plans ajoutés. */
    fun importer(objet: JSONObject): Int {
        val avant = etat.value
        val apres = CarnetCinema.fusionner(avant, lire(objet))
        poser(apres)
        return (apres.projets.size - avant.projets.size) + (apres.jours.size - avant.jours.size) +
            (apres.plans.size - avant.plans.size)
    }

    private fun charger(): CarnetCinemaDonnees = try {
        lire(JSONObject(prefs.getString("cinema", "{}") ?: "{}"))
    } catch (e: Exception) {
        CarnetCinemaDonnees()
    }

    private fun JSONObject.reel(cle: String): Double? = if (has(cle) && !isNull(cle)) optDouble(cle) else null
    private fun JSONObject.entierLong(cle: String): Long? = if (has(cle) && !isNull(cle)) optLong(cle) else null

    private fun lire(o: JSONObject): CarnetCinemaDonnees {
        val projets = o.optJSONArray("projets") ?: JSONArray()
        val jours = o.optJSONArray("jours") ?: JSONArray()
        val plans = o.optJSONArray("plans") ?: JSONArray()
        val liens = o.optJSONArray("liens") ?: JSONArray()
        return CarnetCinemaDonnees(
            projets = (0 until projets.length()).map { i ->
                val p = projets.getJSONObject(i)
                Projet(p.optLong("id", i.toLong()), p.optString("nom", ""), p.optString("notes", ""))
            },
            jours = (0 until jours.length()).map { i ->
                val j = jours.getJSONObject(i)
                JourCinema(
                    identifiant = j.optLong("id", i.toLong()),
                    projetId = j.optLong("projet", 0),
                    nom = j.optString("nom", ""),
                    date = j.optString("date", ""),
                    type = runCatching { TypeJour.valueOf(j.optString("type")) }.getOrDefault(TypeJour.TOURNAGE)
                )
            },
            plans = (0 until plans.length()).map { i -> lirePlan(plans.getJSONObject(i), i) },
            liens = (0 until liens.length()).map { i ->
                val l = liens.getJSONObject(i)
                LienVue(l.optLong("vue"), l.optLong("projet"))
            }
        )
    }

    private fun lirePlan(o: JSONObject, rang: Int): Plan {
        val e = o.optJSONObject("exposition") ?: JSONObject()
        val l = o.optJSONObject("lieu")
        val s = o.optJSONObject("soleil")
        val c = o.optJSONObject("champ")
        return Plan(
            identifiant = o.optLong("id", rang.toLong()),
            date = o.optString("date", ""),
            heure = o.optString("heure", ""),
            photo = o.optString("photo", ""),
            rapportPhoto = o.optDouble("rapport", 4.0 / 3.0),
            importee = o.optBoolean("importee", false),
            jourId = o.entierLong("jour"),
            numerotation = runCatching { Numerotation.valueOf(o.optString("numerotation")) }
                .getOrDefault(Numerotation.SEQUENCE),
            sequence = o.optString("sequence", ""),
            plan = o.optString("plan", ""),
            prise = o.optString("prise", ""),
            numero = o.optString("numero", ""),
            titre = o.optString("titre", ""),
            exposition = ExpositionPlan(
                ev100 = e.reel("ev100"),
                tournage = e.optBoolean("tournage", true),
                cadence = e.reel("cadence"),
                angle = e.reel("angle"),
                ei = if (e.has("ei") && !e.isNull("ei")) e.optInt("ei") else null,
                ouverture = e.optString("ouverture", ""),
                tempsPose = e.optString("temps", ""),
                filtres = e.optString("filtres", "")
            ),
            lieu = l?.let {
                LieuPlan(
                    latitude = it.optDouble("lat", 0.0),
                    longitude = it.optDouble("lon", 0.0),
                    precisionM = it.reel("precision"),
                    altitudeM = it.reel("altitude"),
                    adresse = it.optString("adresse", "")
                )
            },
            cap = o.reel("cap"),
            soleil = s?.let { SoleilPlan(it.optDouble("azimut", 0.0), it.optDouble("hauteur", 0.0)) },
            cadre = CadreJson.lire(o.optJSONObject("cadre")),
            champ = c?.let { ChampPhoto(it.optDouble("h", 0.0), it.optDouble("v", 0.0)) },
            cameraTelephone = o.optString("camera", ""),
            notes = o.optString("notes", "")
        )
    }

    private fun ecrirePlan(p: Plan): JSONObject {
        val e = JSONObject()
            .put("tournage", p.exposition.tournage)
            .put("ouverture", p.exposition.ouverture)
            .put("temps", p.exposition.tempsPose)
            .put("filtres", p.exposition.filtres)
        p.exposition.ev100?.let { e.put("ev100", it) }
        p.exposition.cadence?.let { e.put("cadence", it) }
        p.exposition.angle?.let { e.put("angle", it) }
        p.exposition.ei?.let { e.put("ei", it) }
        val o = JSONObject()
            .put("id", p.identifiant)
            .put("date", p.date)
            .put("heure", p.heure)
            .put("photo", p.photo)
            .put("rapport", p.rapportPhoto)
            .put("importee", p.importee)
            .put("numerotation", p.numerotation.name)
            .put("sequence", p.sequence)
            .put("plan", p.plan)
            .put("prise", p.prise)
            .put("numero", p.numero)
            .put("titre", p.titre)
            .put("exposition", e)
            .put("cadre", CadreJson.ecrire(p.cadre))
            .put("camera", p.cameraTelephone)
            .put("notes", p.notes)
        p.jourId?.let { o.put("jour", it) }
        p.cap?.let { o.put("cap", it) }
        p.lieu?.let { l ->
            val lieu = JSONObject().put("lat", l.latitude).put("lon", l.longitude).put("adresse", l.adresse)
            l.precisionM?.let { lieu.put("precision", it) }
            l.altitudeM?.let { lieu.put("altitude", it) }
            o.put("lieu", lieu)
        }
        p.soleil?.let { o.put("soleil", JSONObject().put("azimut", it.azimut).put("hauteur", it.hauteur)) }
        p.champ?.let { o.put("champ", JSONObject().put("h", it.horizontal).put("v", it.vertical)) }
        return o
    }

    fun enJson(): JSONObject {
        val d = etat.value
        val projets = JSONArray()
        d.projets.forEach { projets.put(JSONObject().put("id", it.identifiant).put("nom", it.nom).put("notes", it.notes)) }
        val jours = JSONArray()
        d.jours.forEach {
            jours.put(
                JSONObject().put("id", it.identifiant).put("projet", it.projetId).put("nom", it.nom)
                    .put("date", it.date).put("type", it.type.name)
            )
        }
        val plans = JSONArray()
        d.plans.forEach { plans.put(ecrirePlan(it)) }
        val liens = JSONArray()
        d.liens.forEach { liens.put(JSONObject().put("vue", it.vueId).put("projet", it.projetId)) }
        return JSONObject().put("projets", projets).put("jours", jours).put("plans", plans).put("liens", liens)
    }
}
