package ca.bpmproperty.gymlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import ca.bpmproperty.gymlogger.ui.screens.ExerciseLibraryScreen
import ca.bpmproperty.gymlogger.ui.screens.TemplateEditorScreen
import ca.bpmproperty.gymlogger.ui.screens.TemplatePickerScreen

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

    val items = listOf(
        BottomNavItem("log", "Log", Icons.Filled.FitnessCenter),
        BottomNavItem("history", "History", Icons.Filled.History),
        BottomNavItem("summary", "Summary", Icons.Filled.BarChart),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "log",
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            composable("log") { LogScreen(navController) }
            composable("history") { backStackEntry ->
                // Read scroll target from saved state (set by LogScreen when a calendar day is tapped).
                // Using SavedStateHandle here lets us pass data when switching to an existing tab
                // without pushing a new entry onto the back stack.
                val scrollTo = backStackEntry.savedStateHandle.get<Int>("scrollToDateKey")
                HistoryScreen(
                    scrollToDateKey = scrollTo,
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
            composable("summary") { SummaryScreen() }
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
                    editingSessionId = if (edit > 0) edit else null
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
