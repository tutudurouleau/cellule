package fr.cellule.app.chrono

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.cellule.core.Chrono
import fr.cellule.core.SequenceLabo

/**
 * L'état du chrono, partagé par l'écran et le service. Une seule horloge :
 * l'instant, en temps système depuis le démarrage du téléphone (qui continue
 * de courir écran éteint), où la séquence a commencé. Une pause ne fait que
 * décaler ce départ ; sauter une étape aussi.
 */
object Minuteur {

    var sequence by mutableStateOf<SequenceLabo?>(null)
        private set

    private var depart by mutableStateOf(0L)

    var pause by mutableStateOf<Long?>(null)
        private set

    /**
     * Où reprendre la lecture des signaux après un saut d'étape : le service
     * ne doit pas rejouer d'un coup les bips des bains sautés.
     */
    var reprise by mutableStateOf<Double?>(null)
        private set

    val enCours: Boolean get() = sequence != null

    fun ecoule(maintenant: Long = SystemClock.elapsedRealtime()): Double =
        ((pause ?: maintenant) - depart) / 1000.0

    fun lancer(contexte: Context, s: SequenceLabo) {
        sequence = s
        depart = SystemClock.elapsedRealtime()
        pause = null
        reprise = null
        ServiceChrono.demarrer(contexte)
    }

    fun basculerPause() {
        val maintenant = SystemClock.elapsedRealtime()
        val p = pause
        if (p == null) pause = maintenant
        else {
            depart += maintenant - p
            pause = null
        }
    }

    /** Passe au bain suivant — ou termine, depuis le dernier. */
    fun etapeSuivante() {
        val s = sequence ?: return
        val position = Chrono.position(s, ecoule())
        if (position.fini) return
        val cible = if (position.etape < s.etapes.lastIndex) s.debut(position.etape + 1) else s.duree
        depart = (pause ?: SystemClock.elapsedRealtime()) - (cible * 1000).toLong()
        reprise = cible
    }

    fun oublierReprise() {
        reprise = null
    }

    fun arreter(contexte: Context) {
        sequence = null
        pause = null
        reprise = null
        ServiceChrono.arreter(contexte)
    }
}
