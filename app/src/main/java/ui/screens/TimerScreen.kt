package ca.bpmproperty.gymlogger.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import ca.bpmproperty.gymlogger.data.Countdown
import ca.bpmproperty.gymlogger.data.GymLoggerApplication
import ca.bpmproperty.gymlogger.data.formatStopwatchDisplay
import ca.bpmproperty.gymlogger.data.formatTimerDisplay

/** Quick-pick countdown durations (in seconds). */
private val countdownPresets = listOf(30, 45, 60, 90, 120, 300, 600, 900, 1200)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onBack: () -> Unit = {},
    showBackButton: Boolean = true,
    onPopOut: (target: String) -> Unit = {}
) {
    val context = LocalContext.current
    val timer = remember { (context.applicationContext as GymLoggerApplication).timer }

    // Runtime notification permission for Android 13+. We ask the first time the user
    // starts any timer so the foreground-service notification can show up.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result is fine either way — timer still works without notifications */ }

    val askNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // In-app beep+vibrate when a countdown finishes while the user is looking at the screen.
    // (The foreground service also fires its own alert when backgrounded.)
    LaunchedEffect(timer.pendingFinishedAlert) {
        if (timer.pendingFinishedAlert != null) {
            playCountdownFinishedAlert(context)
            // Don't consume here — the service consumes after firing its own alert.
            // Consuming here would race with the service. The signal naturally clears within ~500ms.
        }
    }

    // Add-countdown dialog state
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "TIMER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StopwatchCard(
                millis = timer.stopwatchMillis,
                isRunning = timer.stopwatchRunning,
                onStart = {
                    askNotificationPermissionIfNeeded()
                    timer.startStopwatch()
                },
                onPause = { timer.pauseStopwatch() },
                onReset = { timer.resetStopwatch() },
                onPopOut = { onPopOut("stopwatch") }
            )

            // ── Countdowns section ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COUNTDOWNS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { showAddDialog = true }) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("Add")
                    }
                }

                if (timer.countdowns.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No countdowns. Tap + Add to create one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    timer.countdowns.forEach { countdown ->
                        CountdownCard(
                            countdown = countdown,
                            onStart = {
                                askNotificationPermissionIfNeeded()
                                timer.startCountdown(countdown.id)
                            },
                            onPause = { timer.pauseCountdown(countdown.id) },
                            onReset = { timer.resetCountdown(countdown.id) },
                            onDelete = { timer.removeCountdown(countdown.id) },
                            onPopOut = { onPopOut(countdown.id.toString()) }
                        )
                    }
                }
            }

            if (timer.anyRunning) {
                Text(
                    text = "Timers keep running while you use other parts of the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddCountdownDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, durationMillis ->
                timer.addCountdown(name, durationMillis)
                showAddDialog = false
            }
        )
    }
}

// ── Stopwatch ────────────────────────────────────────────────────────────────

@Composable
private fun StopwatchCard(
    millis: Long,
    isRunning: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onPopOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left spacer balances the popout icon on the right so the label stays centred.
            Spacer(modifier = Modifier.size(32.dp))
            Text(
                text = "STOPWATCH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onPopOut, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.OpenInFull,
                    contentDescription = "Pop out to full screen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Tap the big digits to pop out to full-screen.
        Text(
            text = formatStopwatchDisplay(millis),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onPopOut)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onReset,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset")
            }
            FilledIconButton(
                onClick = { if (isRunning) onPause() else onStart() },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(32.dp)
                )
            }
            // Right-side spacer for symmetry with reset button.
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

// ── Single countdown card ────────────────────────────────────────────────────

@Composable
private fun CountdownCard(
    countdown: Countdown,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
    onPopOut: () -> Unit
) {
    val running = countdown.isRunning
    val urgent = running && countdown.remainingMillis < 10_000L
    val timeColor = when {
        urgent -> MaterialTheme.colorScheme.error
        countdown.justFinished -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = countdown.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPopOut, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.OpenInFull,
                        contentDescription = "Pop out to full screen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete countdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatTimerDisplay(countdown.remainingMillis),
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            color = timeColor,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onPopOut)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onReset,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.size(16.dp))
            FilledIconButton(
                onClick = { if (running) onPause() else onStart() },
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (running) "Pause" else "Start",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = "From ${formatTimerDisplay(countdown.startMillis)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp)
        )
    }
}

// ── Add-countdown dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, durationMillis: Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedSeconds by remember { mutableStateOf(60) }
    var customMinutes by remember { mutableStateOf("") }
    var customSeconds by remember { mutableStateOf("") }
    val useCustom = customMinutes.isNotBlank() || customSeconds.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Rest 90s") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "QUICK PICK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CountdownPresetGrid(
                    selectedSeconds = if (useCustom) -1 else selectedSeconds,
                    onSelect = { secs ->
                        selectedSeconds = secs
                        customMinutes = ""
                        customSeconds = ""
                    }
                )

                Text(
                    text = "OR CUSTOM",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = customSeconds,
                        onValueChange = { customSeconds = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Sec") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalSeconds = if (useCustom) {
                        val m = customMinutes.toIntOrNull() ?: 0
                        val s = customSeconds.toIntOrNull() ?: 0
                        (m * 60 + s).coerceAtLeast(1)
                    } else {
                        selectedSeconds
                    }
                    val resolvedName = name.ifBlank {
                        when {
                            totalSeconds < 60 -> "Countdown ${totalSeconds}s"
                            totalSeconds % 60 == 0 -> "Countdown ${totalSeconds / 60}m"
                            else -> "Countdown ${totalSeconds / 60}m ${totalSeconds % 60}s"
                        }
                    }
                    onConfirm(resolvedName, totalSeconds * 1000L)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Preset chip grid (reused inside the add dialog) ─────────────────────────

@Composable
private fun CountdownPresetGrid(
    selectedSeconds: Int,
    onSelect: (Int) -> Unit
) {
    // Chunk into rows of 3 so longer labels (e.g. "1m 30s", "20m") fit comfortably
    // on narrow phones without truncating.
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        countdownPresets.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { seconds ->
                    PresetChip(
                        seconds = seconds,
                        selected = seconds == selectedSeconds,
                        onClick = { onSelect(seconds) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(seconds: Int, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val label = when {
        seconds < 60 -> "${seconds}s"
        // Common short rest interval — read it as "90s" on the chip rather than "1m 30s".
        seconds == 90 -> "90s"
        seconds % 60 == 0 -> "${seconds / 60}m"
        else -> "${seconds / 60}m ${seconds % 60}s"
    }
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .then(
                if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg
        )
    }
}

/**
 * Beep + vibrate when a countdown finishes (in-app foreground path).
 * Service fires its own alert when backgrounded.
 */
private fun playCountdownFinishedAlert(context: Context) {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { tone.release() } catch (_: Throwable) {}
        }, 800)
    } catch (_: Throwable) {}

    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1)
        )
    } catch (_: Throwable) {}
}
