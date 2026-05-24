package ca.sb.gymlogger.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Application-scoped timer hub. Lives for the lifetime of the app process so it survives
 * navigation between screens — start it on the Timer screen, go log a workout, come back
 * and see it still ticking. While any timer is active, also keeps a foreground service
 * alive to survive the app being backgrounded or the screen being off.
 *
 * Holds:
 *  - ONE stopwatch (counts up from 0) for total-workout timing.
 *  - A list of named countdowns (e.g. "Rest 90s") that can run independently and stack.
 *
 * A single ticker coroutine updates every active timer; it shuts down when no timers
 * are running.
 */
class AppTimer(private val appContext: Context) {

    // ── Stopwatch ────────────────────────────────────────────────────────────

    /** Elapsed milliseconds on the stopwatch. */
    var stopwatchMillis by mutableStateOf(0L)
        private set

    /** True while the stopwatch is ticking. */
    var stopwatchRunning by mutableStateOf(false)
        private set

    // ── Countdowns ───────────────────────────────────────────────────────────

    /**
     * Live list of countdowns. UI observes this directly via Compose.
     * Each entry carries its own state and ticks independently.
     */
    val countdowns: SnapshotStateList<Countdown> = mutableStateListOf()

    /** A pending "this countdown just finished" signal for the service to fire an alert. */
    var pendingFinishedAlert by mutableStateOf<Countdown?>(null)
        private set

    // ── Ticker ───────────────────────────────────────────────────────────────

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var lastTickTime: Long = 0L
    private var nextCountdownId: Int = 1

    /** True if anything is currently ticking — drives the foreground service lifecycle. */
    val anyRunning: Boolean
        get() = stopwatchRunning || countdowns.any { it.isRunning }

    // ── Stopwatch API ────────────────────────────────────────────────────────

    fun startStopwatch() {
        if (stopwatchRunning) return
        stopwatchRunning = true
        ensureTickerRunning()
        ensureServiceRunning()
    }

    fun pauseStopwatch() {
        stopwatchRunning = false
        evaluateLifecycle()
    }

    fun resetStopwatch() {
        stopwatchRunning = false
        stopwatchMillis = 0L
        evaluateLifecycle()
    }

    // ── Countdown API ────────────────────────────────────────────────────────

    /**
     * Add a new countdown to the list. Created in paused state; user starts it manually.
     * @return the newly created countdown.
     */
    fun addCountdown(name: String, durationMillis: Long): Countdown {
        val cleanName = name.ifBlank { "Countdown" }
        val clampedDuration = durationMillis.coerceAtLeast(1_000L)
        val countdown = Countdown(
            id = nextCountdownId++,
            name = cleanName,
            startMillis = clampedDuration,
            remainingMillis = clampedDuration,
            isRunning = false,
            justFinished = false
        )
        countdowns.add(countdown)
        return countdown
    }

    fun removeCountdown(id: Int) {
        countdowns.removeAll { it.id == id }
        evaluateLifecycle()
    }

    fun startCountdown(id: Int) {
        val idx = countdowns.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = countdowns[idx]
        if (current.isRunning) return
        // If it had finished (remaining = 0), restart from start.
        val newRemaining = if (current.remainingMillis <= 0L) current.startMillis else current.remainingMillis
        countdowns[idx] = current.copy(
            remainingMillis = newRemaining,
            isRunning = true,
            justFinished = false
        )
        ensureTickerRunning()
        ensureServiceRunning()
    }

    fun pauseCountdown(id: Int) {
        val idx = countdowns.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = countdowns[idx]
        if (!current.isRunning) return
        countdowns[idx] = current.copy(isRunning = false)
        evaluateLifecycle()
    }

    fun resetCountdown(id: Int) {
        val idx = countdowns.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = countdowns[idx]
        countdowns[idx] = current.copy(
            remainingMillis = current.startMillis,
            isRunning = false,
            justFinished = false
        )
        evaluateLifecycle()
    }

    /** Called by the UI / service after it's consumed the just-finished signal. */
    fun consumeFinishedSignal() {
        pendingFinishedAlert = null
        // Also clear the per-countdown justFinished flag so the UI doesn't keep flashing.
        countdowns.forEachIndexed { idx, c ->
            if (c.justFinished) {
                countdowns[idx] = c.copy(justFinished = false)
            }
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun ensureTickerRunning() {
        if (tickJob?.isActive == true) return
        lastTickTime = System.currentTimeMillis()
        tickJob = scope.launch {
            while (anyRunning) {
                // Tick at 30ms when the stopwatch is running so the 1/100s display
                // updates smoothly; otherwise 100ms is plenty for whole-second
                // countdowns and saves battery.
                delay(if (stopwatchRunning) 30L else 100L)
                val now = System.currentTimeMillis()
                val delta = now - lastTickTime
                lastTickTime = now

                // Stopwatch
                if (stopwatchRunning) {
                    stopwatchMillis += delta
                }

                // Countdowns — iterate by index so we can mutate in place.
                for (i in countdowns.indices) {
                    val c = countdowns[i]
                    if (!c.isRunning) continue
                    val newRemaining = c.remainingMillis - delta
                    if (newRemaining <= 0L) {
                        val finished = c.copy(
                            remainingMillis = 0L,
                            isRunning = false,
                            justFinished = true
                        )
                        countdowns[i] = finished
                        // Surface the alert to the service.
                        pendingFinishedAlert = finished
                    } else {
                        countdowns[i] = c.copy(remainingMillis = newRemaining)
                    }
                }
            }
            tickJob = null
        }
    }

    private fun ensureServiceRunning() {
        if (anyRunning) {
            TimerForegroundService.start(appContext)
        }
    }

    /** Stop the foreground service if nothing is running anymore. */
    private fun evaluateLifecycle() {
        if (!anyRunning) {
            TimerForegroundService.stop(appContext)
        }
    }
}

/**
 * One countdown timer in the AppTimer hub.
 * Immutable — state changes produce new copies via [SnapshotStateList] for Compose reactivity.
 */
data class Countdown(
    val id: Int,
    val name: String,
    val startMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean,
    val justFinished: Boolean
)

/**
 * Format a millisecond value as "HH:MM:SS" or "MM:SS" depending on length.
 * Used for countdowns (whole-second display) and the foreground notification.
 * Examples: 45_000L → "00:45"; 4_500_000L → "01:15:00"
 */
fun formatTimerDisplay(millis: Long): String {
    val total = millis.coerceAtLeast(0L) / 1000
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * Format a millisecond value as "HH:MM:SS.cc" or "MM:SS.cc" where `cc` is hundredths
 * of a second (00–99). Used for the stopwatch so the user can see sub-second precision.
 * Examples: 45_837L → "00:45.83"; 4_500_120L → "01:15:00.12"
 */
fun formatStopwatchDisplay(millis: Long): String {
    val clamped = millis.coerceAtLeast(0L)
    val totalSeconds = clamped / 1000
    val hundredths = (clamped % 1000) / 10
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, hundredths)
    } else {
        "%02d:%02d.%02d".format(minutes, seconds, hundredths)
    }
}
