package fr.cellule.app.mesure

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.LectureIncidente

/**
 * Le capteur de luminosité ambiante : la seule mesure vraiment *incidente* que
 * porte un téléphone. Elle ignore la matière visée, ce qui est précisément ce
 * qu'on veut d'un posemètre.
 *
 * En contrepartie il est grossier. Il sature bien avant le plein soleil sur la
 * plupart des appareils, quantifie mal dans le bas, et se trouve derrière la
 * vitre de l'écran, dont la réponse angulaire n'a rien de cosinusoïdale. À
 * traiter comme un ordre de grandeur fiable, pas comme une mesure au tiers de
 * diaph.
 */
class CapteurAmbiance(contexte: Context) : SensorEventListener {

    private val gestionnaire = contexte.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val capteur: Sensor? = gestionnaire?.getDefaultSensor(Sensor.TYPE_LIGHT)

    val disponible: Boolean get() = capteur != null

    var lecture by mutableStateOf<LectureIncidente?>(null)
        private set

    /* Le capteur est bruité : une moyenne glissante courte stabilise
       l'affichage sans le rendre paresseux. */
    private val fenetre = ArrayDeque<Float>()

    fun demarrer() {
        val c = capteur ?: return
        gestionnaire?.registerListener(this, c, SensorManager.SENSOR_DELAY_UI)
    }

    fun arreter() {
        gestionnaire?.unregisterListener(this)
        fenetre.clear()
    }

    override fun onSensorChanged(evenement: SensorEvent) {
        val c = capteur ?: return
        val brut = evenement.values.firstOrNull() ?: return
        fenetre.addLast(brut)
        while (fenetre.size > 8) fenetre.removeFirst()
        val moyenne = fenetre.sum() / fenetre.size
        lecture = LectureIncidente(
            lux = moyenne,
            luxMax = c.maximumRange,
            resolution = c.resolution
        )
    }

    override fun onAccuracyChanged(capteur: Sensor?, precision: Int) {
        /* Sans usage : la précision annoncée par ces capteurs ne veut rien dire. */
    }
}
