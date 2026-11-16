package fr.cellule.app.mesure

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import fr.cellule.core.Boussole
import fr.cellule.core.LieuPlan
import fr.cellule.core.Soleil
import fr.cellule.core.SoleilPlan
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2

/** Comment on tient le téléphone : c'est ce qui décide du sens de la photo et du cadre. */
enum class Tenue(val rotation: Int, val couche: Boolean) {
    DEBOUT(Surface.ROTATION_0, false),
    /** Couché, le haut du téléphone vers la gauche. */
    COUCHE_GAUCHE(Surface.ROTATION_90, true),
    /** Couché, le haut du téléphone vers la droite. */
    COUCHE_DROITE(Surface.ROTATION_270, true)
}

/**
 * Le cap et la tenue du téléphone, lus sur son capteur d'orientation.
 *
 * Le cap est celui de l'axe de la caméra arrière — vers où l'on vise — et non
 * celui du haut du téléphone. Il ne remonte pas dans l'interface à chaque
 * mesure : on le lit au moment de la capture. Seule la tenue (debout, couché)
 * est observée, et elle ne change que quand on tourne vraiment le téléphone.
 */
class CapteurCap(contexte: Context) : SensorEventListener {

    private val gestionnaire = contexte.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val capteur: Sensor? = gestionnaire?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val matrice = FloatArray(9)

    val disponible: Boolean get() = capteur != null

    var tenue by mutableStateOf(Tenue.DEBOUT)
        private set

    /** Cap magnétique de l'axe de visée, en degrés ; null tant qu'aucune lecture n'est arrivée. */
    @Volatile
    var capMagnetique: Double? = null
        private set

    fun demarrer() {
        val c = capteur ?: return
        gestionnaire?.registerListener(this, c, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun arreter() {
        gestionnaire?.unregisterListener(this)
        capMagnetique = null
    }

    override fun onSensorChanged(evenement: SensorEvent) {
        try {
            /* Certains appareils livrent cinq valeurs, que les anciennes
               versions d'Android refusent : on n'en garde que quatre. */
            val v = if (evenement.values.size > 4) evenement.values.copyOf(4) else evenement.values
            SensorManager.getRotationMatrixFromVector(matrice, v)
            /* La matrice passe du repère du téléphone à celui du monde (est,
               nord, haut). La caméra arrière regarde le long de −Z. */
            val est = -matrice[2].toDouble()
            val nord = -matrice[5].toDouble()
            val haut = -matrice[8].toDouble()
            capMagnetique = if (abs(haut) > 0.92) {
                /* Téléphone à plat : la visée n'a plus de cap, on prend celui de son bord haut. */
                Boussole.normaliser(Math.toDegrees(atan2(matrice[1].toDouble(), matrice[4].toDouble())))
            } else {
                Boussole.normaliser(Math.toDegrees(atan2(est, nord)))
            }
            /* Lequel des bords pointe vers le ciel : le droit (X), ou le haut (Y) ? */
            val x = matrice[6]
            val y = matrice[7]
            val nouvelle = when {
                abs(x) < 0.5f && abs(y) < 0.5f -> tenue /* presque à plat : on ne change rien */
                x > 0.65f && abs(x) > abs(y) -> Tenue.COUCHE_GAUCHE
                x < -0.65f && abs(x) > abs(y) -> Tenue.COUCHE_DROITE
                y > 0.65f -> Tenue.DEBOUT
                else -> tenue
            }
            if (nouvelle != tenue) tenue = nouvelle
        } catch (e: Exception) {
            /* Une lecture ratée n'est pas une panne. */
        }
    }

    override fun onAccuracyChanged(capteur: Sensor?, precision: Int) {
        /* Sans usage ici. */
    }
}

/** Y a-t-il du réseau, là, maintenant ? Les conforts en ligne (adresse, carte) en dépendent. */
fun reseauDisponible(contexte: Context): Boolean = try {
    val cm = contexte.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val reseau = cm.activeNetwork
    val capacites = if (reseau != null) cm.getNetworkCapabilities(reseau) else null
    capacites != null && capacites.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
} catch (e: Exception) {
    false
}

/**
 * La position du téléphone, demandée une fois et relâchée aussitôt : aucun
 * suivi en continu. Sans autorisation, elle rend simplement null.
 */
class Localisation(private val contexte: Context) {

    private val principal = Handler(Looper.getMainLooper())

    fun autorisee(): Boolean =
        ContextCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(contexte, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Rend la position sur le fil principal : une position récente si le
     * téléphone en connaît une, sinon la première qui arrive dans le délai.
     */
    @SuppressLint("MissingPermission")
    fun obtenir(delaiMs: Long = 20_000, surResultat: (Location?) -> Unit) {
        if (!autorisee()) {
            surResultat(null)
            return
        }
        try {
            val lm = contexte.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val fournisseurs = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
            val connues = fournisseurs.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            val recente = connues
                .filter { System.currentTimeMillis() - it.time < 120_000 }
                .minByOrNull { it.accuracy }
            if (recente != null && recente.accuracy <= 30f) {
                surResultat(recente)
                return
            }
            if (fournisseurs.isEmpty()) {
                surResultat(recente)
                return
            }
            var fini = false
            val ecouteur = object : LocationListener {
                override fun onLocationChanged(position: Location) {
                    if (fini) return
                    fini = true
                    runCatching { lm.removeUpdates(this) }
                    surResultat(position)
                }

                @Deprecated("Requis par les anciennes versions d'Android")
                override fun onStatusChanged(fournisseur: String?, etat: Int, extras: Bundle?) {
                }

                override fun onProviderEnabled(fournisseur: String) {
                }

                override fun onProviderDisabled(fournisseur: String) {
                }
            }
            fournisseurs.forEach {
                runCatching { lm.requestLocationUpdates(it, 1000L, 0f, ecouteur, Looper.getMainLooper()) }
            }
            principal.postDelayed({
                if (!fini) {
                    fini = true
                    runCatching { lm.removeUpdates(ecouteur) }
                    surResultat(recente ?: connues.maxByOrNull { it.time })
                }
            }, delaiMs)
        } catch (e: Exception) {
            surResultat(null)
        }
    }

    /**
     * L'adresse d'un point, cherchée en ligne — donc seulement s'il y a du
     * réseau. Rend une chaîne vide sinon. Bloquant : à appeler hors du fil principal.
     */
    fun adresse(latitude: Double, longitude: Double): String {
        if (!reseauDisponible(contexte) || !Geocoder.isPresent()) return ""
        return try {
            @Suppress("DEPRECATION")
            val resultats = Geocoder(contexte, Locale.FRANCE).getFromLocation(latitude, longitude, 1)
            resultats?.firstOrNull()?.getAddressLine(0) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

/** Ce qu'on tire d'une position : le lieu tel qu'on le note avec le plan. */
fun Location.enLieu(adresse: String = ""): LieuPlan = LieuPlan(
    latitude = latitude,
    longitude = longitude,
    precisionM = if (hasAccuracy()) accuracy.toDouble() else null,
    altitudeM = if (hasAltitude()) altitude else null,
    adresse = adresse
)

/** Écart entre le nord magnétique et le nord géographique en ce point, en degrés. */
fun declinaisonMagnetique(lieu: LieuPlan): Double = try {
    GeomagneticField(
        lieu.latitude.toFloat(), lieu.longitude.toFloat(),
        (lieu.altitudeM ?: 0.0).toFloat(), System.currentTimeMillis()
    ).declination.toDouble()
} catch (e: Exception) {
    0.0
}

/** Où est le soleil en ce lieu, à cet instant (heure et fuseau du téléphone). */
fun soleilEn(latitude: Double, longitude: Double, instant: ZonedDateTime = ZonedDateTime.now()): SoleilPlan {
    val jour = instant.dayOfYear
    val heure = instant.hour + instant.minute / 60.0 + instant.second / 3600.0
    val fuseau = instant.offset.totalSeconds / 3600.0
    val solaire = Soleil.tempsSolaire(heure, jour, longitude, fuseau)
    return SoleilPlan(Soleil.azimut(latitude, jour, solaire), Soleil.hauteur(latitude, jour, solaire))
}
