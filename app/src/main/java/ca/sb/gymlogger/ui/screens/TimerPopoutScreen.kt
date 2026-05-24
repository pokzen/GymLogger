package ca.sb.gymlogger.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.sb.gymlogger.data.GymLoggerApplication
import ca.sb.gymlogger.data.formatStopwatchDisplay
import ca.sb.gymlogger.data.formatTimerDisplay

/**
 * Full-screen popout for a single timer. Reached by tapping a card on TimerScreen.
 *
 * @param target either "stopwatch" or the integer id (as a string) of a countdown.
 *
 * Orientation: this screen unlocks orientation while it's on top of the back stack
 * so the user can rotate to landscape for an even larger display. When the screen
 * leaves composition, orientation is restored to portrait.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerPopoutScreen(target: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val timer = remember { (context.applicationContext as GymLoggerApplication).timer }

    // Unlock orientation while this screen is active. Restore on dispose.
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val previous = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        onDispose {
            activity?.requestedOrientation =
                previous ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Resolve which timer we're showing.
    val isStopwatch = target == "stopwatch"
    val countdownId = target.toIntOrNull()
    val countdown = if (!isStopwatch && countdownId != null) {
        timer.countdowns.firstOrNull { it.id == countdownId }
    } else null

    // If the countdown was deleted while we were popped out, bail back.
    LaunchedEffect(countdown == null, isStopwatch) {
        if (!isStopwatch && countdown == null) onBack()
    }

    val isLandscape = LocalConfiguration.current.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isStopwatch) "STOPWATCH" else (countdown?.name?.uppercase() ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Massive digits. Landscape gets even bigger.
                val timeText: String
                val isRunning: Boolean
                val urgent: Boolean
                when {
                    isStopwatch -> {
                        timeText = formatStopwatchDisplay(timer.stopwatchMillis)
                        isRunning = timer.stopwatchRunning
                        urgent = false
                    }
                    countdown != null -> {
                        timeText = formatTimerDisplay(countdown.remainingMillis)
                        isRunning = countdown.isRunning
                        urgent = isRunning && countdown.remainingMillis < 10_000L
                    }
                    else -> {
                        timeText = "—"
                        isRunning = false
                        urgent = false
                    }
                }

                val fontSize = when {
                    isLandscape && isStopwatch -> 140.sp
                    isLandscape -> 160.sp
                    isStopwatch -> 76.sp
                    else -> 96.sp
                }

                Text(
                    text = timeText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    color = if (urgent) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 40.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            if (isStopwatch) timer.resetStopwatch()
                            else countdown?.let { timer.resetCountdown(it.id) }
                        },
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            if (isStopwatch) {
                                if (isRunning) timer.pauseStopwatch() else timer.startStopwatch()
                            } else {
                                countdown?.let {
                                    if (isRunning) timer.pauseCountdown(it.id)
                                    else timer.startCountdown(it.id)
                                }
                            }
                        },
                        modifier = Modifier.size(96.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}
