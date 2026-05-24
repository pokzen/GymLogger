package ca.sb.gymlogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.sb.gymlogger.BuildConfig
import ca.sb.gymlogger.data.AppPreferences
import ca.sb.gymlogger.ui.components.AppCard
import ca.sb.gymlogger.ui.components.SectionLabel
import ca.sb.gymlogger.ui.viewmodel.CardioType
import ca.sb.gymlogger.ui.viewmodel.SettingsViewModel
import ca.sb.gymlogger.ui.viewmodel.workoutViewModelWithPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = workoutViewModelWithPrefs { repo, prefs ->
        SettingsViewModel(repo, prefs)
    }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showCardioMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Personalization ──
            SectionLabel("Personalization")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Log name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Shown on the home screen, navigation drawer, and About.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.logName,
                        onValueChange = { viewModel.updateLogName(it) },
                        placeholder = { Text(AppPreferences.DEFAULT_LOG_NAME) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Greeting",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The headline below your log name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.greeting,
                        onValueChange = { viewModel.updateGreeting(it) },
                        placeholder = { Text(AppPreferences.DEFAULT_GREETING) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Defaults ──
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Defaults")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Default cardio type",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "What new cardio sessions start as.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val currentType = CardioType.values()
                        .firstOrNull { it.storageValue == viewModel.defaultCardioType }
                        ?: CardioType.TREADMILL

                    Box {
                        OutlinedButton(
                            onClick = { showCardioMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = currentType.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showCardioMenu,
                            onDismissRequest = { showCardioMenu = false }
                        ) {
                            CardioType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    onClick = {
                                        viewModel.updateDefaultCardioType(type.storageValue)
                                        showCardioMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── About ──
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("About")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "GymLogger",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A personal workout logger. No accounts, no analytics, " +
                            "all data stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Made by Shane Byers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Danger zone ──
            Spacer(modifier = Modifier.height(28.dp))
            SectionLabel("Danger zone")
            Spacer(modifier = Modifier.height(10.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Erase all data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deletes every workout, template, and library customization " +
                            "from this device. The default exercise library will be restored. " +
                            "This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        enabled = !viewModel.isWiping,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Filled.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.isWiping) "ERASING…" else "ERASE ALL DATA"
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Erase all data?") },
            text = {
                Text(
                    "This will permanently delete every workout, template, and library " +
                        "customization on this device. You'll have a clean install with " +
                        "the default exercise library.\n\nThis cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    viewModel.wipeAllData()
                }) {
                    Text("ERASE EVERYTHING", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (viewModel.wipeCompleted) {
        AlertDialog(
            onDismissRequest = { viewModel.consumeWipeCompleted() },
            title = { Text("All data erased") },
            text = { Text("Your device is back to a fresh state.") },
            confirmButton = {
                TextButton(onClick = { viewModel.consumeWipeCompleted() }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
