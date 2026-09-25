package fr.cellule.app.chrono

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import fr.cellule.app.MainActivity
import fr.cellule.app.R
import fr.cellule.core.Chrono
import fr.cellule.core.Duree
import fr.cellule.core.Evenement
import fr.cellule.core.SequenceLabo
import fr.cellule.core.Signal

/**
 * Le service du chrono labo.
 *
 * En chambre noire, l'écran reste éteint — la lumière voilerait le papier et
 * aveuglerait l'œil adapté au noir. C'est donc ce service, au premier plan et
 * tenant le processeur éveillé, qui lit la chronologie et prévient à chaque
 * signal : trois bips et une longue vibration pour changer de bain, deux
 * brefs pour agiter, un léger pour s'arrêter d'agiter.
 */
class ServiceChrono : Service() {

    private val boucle = Handler(Looper.getMainLooper())
    private var sequenceVue: SequenceLabo? = null
    private var evenements: List<Evenement> = emptyList()
    private var derniere = -1.0
    private var tourne = false
    private var verrou: PowerManager.WakeLock? = null
    private var tonalite: ToneGenerator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> Minuteur.basculerPause()
            ACTION_SUIVANTE -> Minuteur.etapeSuivante()
            ACTION_ARRETER -> {
                Minuteur.arreter(this)
                return START_NOT_STICKY
            }
        }
        if (Minuteur.sequence == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        premierPlan()
        if (!tourne) {
            tourne = true
            verrou = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "cellule:chrono")
                .apply { acquire(4 * 60 * 60 * 1000L) }
            tonalite = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 80) }.getOrNull()
            boucle.post(tic)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        boucle.removeCallbacksAndMessages(null)
        verrou?.takeIf { it.isHeld }?.release()
        tonalite?.release()
        super.onDestroy()
    }

    /* Quatre lectures par seconde : assez pour qu'un bip tombe à la seconde. */
    private val tic = object : Runnable {
        override fun run() {
            val s = Minuteur.sequence
            if (s == null) {
                stopSelf()
                return
            }
            if (s !== sequenceVue) {
                sequenceVue = s
                evenements = Chrono.evenements(s)
                derniere = -1.0
            }
            Minuteur.reprise?.let {
                derniere = it - 0.001
                Minuteur.oublierReprise()
                premierPlan()
            }
            val t = Minuteur.ecoule()
            if (Minuteur.pause == null) {
                val survenus = Chrono.entre(evenements, derniere, t)
                survenus.forEach { alerter(it.signal) }
                if (survenus.any { it.signal == Signal.ETAPE || it.signal == Signal.FIN }) premierPlan()
                derniere = t
            }
            if (t > s.duree + 3) {
                // La séquence est finie : l'écran garde « terminé », le service s'efface.
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            boucle.postDelayed(this, 250)
        }
    }

    private fun alerter(signal: Signal) {
        val (motif, bips) = when (signal) {
            Signal.ETAPE -> longArrayOf(0, 700, 200, 700) to 3
            Signal.AGITER -> longArrayOf(0, 180, 140, 180) to 2
            Signal.REPOS -> longArrayOf(0, 90) to 0
            Signal.FIN -> longArrayOf(0, 900, 250, 900, 250, 900) to 5
        }
        vibreur()?.vibrate(VibrationEffect.createWaveform(motif, -1))
        repeat(bips) { i ->
            boucle.postDelayed({ tonalite?.startTone(ToneGenerator.TONE_PROP_BEEP, 170) }, i * 280L)
        }
    }

    private fun vibreur(): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)

    private fun premierPlan() {
        val n = notification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION, n)
        }
    }

    private fun notification(): Notification {
        val gestionnaire = getSystemService(NotificationManager::class.java)
        if (gestionnaire.getNotificationChannel(CANAL) == null) {
            gestionnaire.createNotificationChannel(
                NotificationChannel(CANAL, "Chrono labo", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Le bain en cours et le temps qui reste. Les bips viennent du chrono lui-même."
                }
            )
        }
        val ouvrir = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val constructeur = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(R.drawable.ic_chrono)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(ouvrir)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)

        val s = Minuteur.sequence ?: return constructeur.setContentTitle("Chrono labo").build()
        val p = Chrono.position(s, Minuteur.ecoule())
        val etape = s.etapes[p.etape]
        val suivante = s.etapes.getOrNull(p.etape + 1)?.nom
        constructeur
            .setContentTitle("${etape.nom} · ${Duree.libelle(etape.duree)}")
            .setContentText(
                listOfNotNull(
                    etape.agitation?.let { "agiter ${it.libelle}" },
                    suivante?.let { "ensuite : $it" }
                ).joinToString(" · ")
            )
            .addAction(0, if (Minuteur.pause == null) "Pause" else "Reprendre", action(ACTION_PAUSE))
            .addAction(0, "Bain suivant", action(ACTION_SUIVANTE))
            .addAction(0, "Arrêter", action(ACTION_ARRETER))
        if (Minuteur.pause == null && !p.fini) {
            // Le compte à rebours du bain, tenu par le système : aucune mise à jour à la seconde.
            constructeur
                .setWhen(System.currentTimeMillis() + (p.resteEtape * 1000).toLong())
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            constructeur.setSubText(if (p.fini) "terminé" else "en pause")
        }
        return constructeur.build()
    }

    private fun action(nom: String): PendingIntent = PendingIntent.getService(
        this, nom.hashCode(), Intent(this, ServiceChrono::class.java).setAction(nom),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    companion object {
        private const val CANAL = "chrono"
        private const val NOTIFICATION = 7
        private const val ACTION_RAFRAICHIR = "fr.cellule.chrono.RAFRAICHIR"
        private const val ACTION_PAUSE = "fr.cellule.chrono.PAUSE"
        private const val ACTION_SUIVANTE = "fr.cellule.chrono.SUIVANTE"
        private const val ACTION_ARRETER = "fr.cellule.chrono.ARRETER"

        fun demarrer(contexte: Context) {
            ContextCompat.startForegroundService(
                contexte, Intent(contexte, ServiceChrono::class.java).setAction(ACTION_RAFRAICHIR)
            )
        }

        /** Après une pause ou un saut décidé depuis l'écran : la notification suit. */
        fun rafraichir(contexte: Context) {
            if (Minuteur.enCours) demarrer(contexte)
        }

        fun arreter(contexte: Context) {
            contexte.stopService(Intent(contexte, ServiceChrono::class.java))
        }
    }
}
