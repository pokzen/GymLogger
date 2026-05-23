package ca.bpmproperty.gymlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ca.bpmproperty.gymlogger.ui.theme.GymLoggerTheme
import ca.bpmproperty.gymlogger.ui.screens.LogScreen
import ca.bpmproperty.gymlogger.ui.screens.HistoryScreen
import ca.bpmproperty.gymlogger.ui.screens.SummaryScreen
import ca.bpmproperty.gymlogger.ui.screens.LiftingScreen
import ca.bpmproperty.gymlogger.ui.screens.CardioScreen
import ca.bpmproperty.gymlogger.ui.screens.StretchingScreen
import ca.bpmproperty.gymlogger.ui.screens.AboutScreen
import ca.bpmproperty.gymlogger.ui.screens.ExerciseLibraryScreen
import ca.bpmproperty.gymlogger.ui.screens.SettingsScreen
import ca.bpmproperty.gymlogger.ui.screens.StretchLibraryScreen
import ca.bpmproperty.gymlogger.ui.screens.TemplateEditorScreen
import ca.bpmproperty.gymlogger.ui.screens.TemplatePickerScreen
import ca.bpmproperty.gymlogger.ui.screens.TimerPopoutScreen
import ca.bpmproperty.gymlogger.ui.screens.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymLoggerTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val items = listOf(
        BottomNavItem("log", "Log", Icons.Filled.FitnessCenter),
        BottomNavItem("history", "History", Icons.Filled.History),
        BottomNavItem("summary", "Summary", Icons.Filled.BarChart),
        BottomNavItem("timer", "Timer", Icons.Filled.Timer),
    )

    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
        Unit
    }
    val closeDrawer: () -> Unit = {
        scope.launch { drawerState.close() }
        Unit
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerContent(
                onNavigate = { route ->
                    closeDrawer()
                    // Tab routes (log/history/summary/timer) switch tabs; everything else pushes.
                    if (route in setOf("log", "history", "summary", "timer")) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        // Hide the bottom nav on the full-screen timer popout so it actually feels full-screen.
        val showBottomBar = currentRoute?.startsWith("timer_popout") != true

        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "log",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            composable("log") { LogScreen(navController, onOpenDrawer = openDrawer) }
            composable("history") { backStackEntry ->
                // Read scroll target from saved state (set by LogScreen when a calendar day is tapped).
                // Using SavedStateHandle here lets us pass data when switching to an existing tab
                // without pushing a new entry onto the back stack.
                val scrollTo = backStackEntry.savedStateHandle.get<Int>("scrollToDateKey")
                HistoryScreen(
                    scrollToDateKey = scrollTo,
                    onOpenDrawer = openDrawer,
                    onEditLifting = { id -> navController.navigate("lifting?edit=$id") },
                    onEditCardio = { id -> navController.navigate("cardio?edit=$id") },
                    onEditStretching = { id -> navController.navigate("stretching?edit=$id") }
                )
                // Clear it so navigating to History via the bottom nav doesn't re-trigger the scroll.
                LaunchedEffect(scrollTo) {
                    if (scrollTo != null) {
                        backStackEntry.savedStateHandle.remove<Int>("scrollToDateKey")
                    }
                }
            }
            composable("summary") { SummaryScreen(onOpenDrawer = openDrawer) }
            composable(
                route = "lifting?date={date}&template={template}&edit={edit}",
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("template") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("edit") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getInt("date") ?: -1
                val template = backStackEntry.arguments?.getInt("template") ?: -1
                val edit = backStackEntry.arguments?.getInt("edit") ?: -1
                LiftingScreen(
                    onBack = { navController.popBackStack() },
                    initialDateKey = if (date > 0) date else null,
                    initialTemplateId = if (template > 0) template else null,
                    editingSessionId = if (edit > 0) edit else null
                )
            }

            // Template picker — opens when Weights is tapped on the Log screen
            composable("weights_picker") {
                TemplatePickerScreen(
                    onBack = { navController.popBackStack() },
                    onStartEmpty = {
                        navController.popBackStack()
                        navController.navigate("lifting")
                    },
                    onStartFromTemplate = { templateId ->
                        navController.popBackStack()
                        navController.navigate("lifting?template=$templateId")
                    },
                    onCreateNewTemplate = {
                        navController.navigate("template_editor")
                    },
                    onEditTemplate = { templateId ->
                        navController.navigate("template_editor?id=$templateId")
                    },
                    onManageLibrary = {
                        navController.navigate("exercise_library")
                    }
                )
            }

            // Exercise library — full CRUD on the saved-exercises library
            composable("exercise_library") {
                ExerciseLibraryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Template editor (new + edit)
            composable(
                route = "template_editor?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                TemplateEditorScreen(
                    templateId = if (id > 0) id else null,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "cardio?date={date}&edit={edit}",
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("edit") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getInt("date") ?: -1
                val edit = backStackEntry.arguments?.getInt("edit") ?: -1
                CardioScreen(
                    onBack = { navController.popBackStack() },
                    initialDateKey = if (date > 0) date else null,
                    editingSessionId = if (edit > 0) edit else null
                )
            }
            composable(
                route = "stretching?date={date}&edit={edit}",
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.IntType
                        defaultValue = -1
                    },
                    navArgument("edit") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getInt("date") ?: -1
                val edit = backStackEntry.arguments?.getInt("edit") ?: -1
                StretchingScreen(
                    onBack = { navController.popBackStack() },
                    initialDateKey = if (date > 0) date else null,
                    editingSessionId = if (edit > 0) edit else null,
                    onManageLibrary = { navController.navigate("stretch_library") }
                )
            }

            // Stretch library management screen
            composable("stretch_library") {
                StretchLibraryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Settings (placeholder for future units/theme/etc.)
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            // About / version info
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            // Timer (stopwatch + countdowns) — reachable both as a bottom-nav tab
            // and from the drawer. As a tab there's no back stack, so the back
            // button is suppressed.
            composable("timer") {
                TimerScreen(
                    onBack = { navController.popBackStack() },
                    showBackButton = false,
                    onPopOut = { target ->
                        navController.navigate("timer_popout?target=$target")
                    }
                )
            }

            // Full-screen single-timer popout. Free orientation.
            composable(
                route = "timer_popout?target={target}",
                arguments = listOf(navArgument("target") {
                    type = NavType.StringType
                    defaultValue = "stopwatch"
                })
            ) { backStackEntry ->
                val target = backStackEntry.arguments?.getString("target") ?: "stopwatch"
                TimerPopoutScreen(
                    target = target,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
    }
}

@Composable
private fun AppDrawerContent(
    onNavigate: (route: String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SHANE'S LOG",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 20.dp, top = 8.dp)
            )

            // ── Workout ──
            DrawerSectionLabel("Workout")
            DrawerItem(
                label = "Exercise Library",
                icon = Icons.Filled.FitnessCenter,
                onClick = { onNavigate("exercise_library") }
            )
            DrawerItem(
                label = "Stretch Library",
                icon = Icons.Filled.SelfImprovement,
                onClick = { onNavigate("stretch_library") }
            )
            DrawerItem(
                label = "Workout Templates",
                icon = Icons.Filled.LibraryBooks,
                onClick = { onNavigate("weights_picker") }
            )
            DrawerItem(
                label = "Timer",
                icon = Icons.Filled.Timer,
                onClick = { onNavigate("timer") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Data ──
            DrawerSectionLabel("Data")
            DrawerItem(
                label = "Export & Backup",
                icon = Icons.Filled.Share,
                onClick = { onNavigate("summary") } // jumps to Summary where export lives
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── App ──
            DrawerSectionLabel("App")
            DrawerItem(
                label = "Settings",
                icon = Icons.Filled.Settings,
                onClick = { onNavigate("settings") }
            )
            DrawerItem(
                label = "About",
                icon = Icons.Filled.Info,
                onClick = { onNavigate("about") }
            )
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 6.dp)
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.padding(start = 4.dp)) },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            unselectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
