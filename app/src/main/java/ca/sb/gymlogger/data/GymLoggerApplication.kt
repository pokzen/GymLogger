package ca.sb.gymlogger.data

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymLoggerApplication : Application() {
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database) }
    val preferences by lazy { AppPreferences(this) }

    /** Application-scoped timer — survives navigation between screens. Built lazily so
     *  the Context is available for foreground-service bridging. */
    val timer: AppTimer by lazy { AppTimer(applicationContext) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Idempotent: seeds the exercise library on first launch (or after a destructive
        // migration wipes it). No-op if library already has entries.
        applicationScope.launch {
            repository.seedLibraryIfEmpty()
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        // Running timer channel — low importance so the persistent notification doesn't make sound
        val running = NotificationChannel(
            TIMER_RUNNING_CHANNEL_ID,
            "Timer running",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the workout timer while it's running."
            setShowBadge(false)
        }

        // Countdown finished channel — high importance so the alert grabs attention
        val finished = NotificationChannel(
            TIMER_FINISHED_CHANNEL_ID,
            "Countdown finished",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Fires when a countdown timer reaches zero."
            enableVibration(true)
        }

        manager.createNotificationChannels(listOf(running, finished))
    }

    companion object {
        const val TIMER_RUNNING_CHANNEL_ID = "timer_running"
        const val TIMER_FINISHED_CHANNEL_ID = "timer_finished"
    }
}