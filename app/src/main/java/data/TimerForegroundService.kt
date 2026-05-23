package ca.bpmproperty.gymlogger.data

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ca.bpmproperty.gymlogger.MainActivity
import ca.bpmproperty.gymlogger.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the workout timers alive when the app is backgrounded
 * or the screen is off. Displays a persistent notification showing a summary of all
 * running timers, and fires a per-countdown beep + vibrate + alert notification when
 * any countdown finishes.
 *
 * Lifecycle: started when any timer starts running, stopped when all stop.
 * Reads timer state directly from the singleton [GymLoggerApplication.timer].
 */
class TimerForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = applicationContext as GymLoggerApplication
        val timer = app.timer

        // Start in foreground immediately with a placeholder notification.
        startForeground(NOTIFICATION_ID, buildRunningNotification(timer))

        // Tick: every 500ms, refresh the notification and check for finished-countdown alerts.
        tickJob?.cancel()
        tickJob = scope.launch {
            while (timer.anyRunning || timer.pendingFinishedAlert != null) {
                // Fire any pending finished-countdown alert first.
                timer.pendingFinishedAlert?.let { finished ->
                    fireFinishedAlert(finished)
                    timer.consumeFinishedSignal()
                }

                NotificationManagerCompat.from(this@TimerForegroundService).apply {
                    if (areNotificationsEnabled()) {
                        notify(NOTIFICATION_ID, buildRunningNotification(timer))
                    }
                }
                delay(500L)
            }
            stopSelfSafely()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun stopSelfSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    /**
     * Persistent notification. Title summarizes what's running; expandable body lists each timer.
     * Examples:
     *   Title: "Workout: 12:34 · 2 countdowns"
     *   Title: "Workout: 03:21"
     *   Title: "2 countdowns running"
     */
    private fun buildRunningNotification(timer: AppTimer): Notification {
        val runningCountdowns = timer.countdowns.filter { it.isRunning }

        val title = buildString {
            if (timer.stopwatchRunning) {
                append("Workout: ${formatTimerDisplay(timer.stopwatchMillis)}")
                if (runningCountdowns.isNotEmpty()) {
                    val count = runningCountdowns.size
                    append(" · $count countdown${if (count == 1) "" else "s"}")
                }
            } else {
                val count = runningCountdowns.size
                append("$count countdown${if (count == 1) "" else "s"} running")
            }
        }

        val bodyLines = buildList {
            if (timer.stopwatchRunning) add("Stopwatch — ${formatTimerDisplay(timer.stopwatchMillis)}")
            runningCountdowns.forEach { c ->
                add("${c.name} — ${formatTimerDisplay(c.remainingMillis)} left")
            }
        }
        val body = bodyLines.joinToString(separator = "\n")

        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(this, 0, openIntent, pendingFlags)

        return NotificationCompat.Builder(this, GymLoggerApplication.TIMER_RUNNING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(bodyLines.firstOrNull() ?: "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Fires when a countdown reaches zero. Posts a separate high-importance notification
     * naming the specific countdown, plays a beep, and vibrates. Runs regardless of
     * whether the screen is on.
     */
    private fun fireFinishedAlert(countdown: Countdown) {
        // Beep
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { tone.release() } catch (_: Throwable) {}
            }, 800)
        } catch (_: Throwable) {}

        // Vibrate
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            // VibrationEffect / createWaveform are API 26+. minSdk is 24, so fall back
            // to the deprecated long-pattern overload on API 24-25.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 300, 150, 300), -1)
            }
        } catch (_: Throwable) {}

        // Visual notification — named after the specific countdown.
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        // Use the countdown id as the request code so each alert is distinct.
        val contentPi = PendingIntent.getActivity(this, countdown.id, openIntent, pendingFlags)

        val finished = NotificationCompat.Builder(this, GymLoggerApplication.TIMER_FINISHED_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("${countdown.name} — time's up")
            .setContentText("Your countdown has finished.")
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(this).apply {
            if (areNotificationsEnabled()) {
                // Unique id per countdown so they don't overwrite each other.
                notify(FINISHED_NOTIFICATION_ID_BASE + countdown.id, finished)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val FINISHED_NOTIFICATION_ID_BASE = 2000

        /** Start the service. Idempotent — safe to call when already running. */
        fun start(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stop the service. */
        fun stop(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
