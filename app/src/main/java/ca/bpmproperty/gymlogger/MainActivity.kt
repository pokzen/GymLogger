package ca.bpmproperty.gymlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ca.bpmproperty.gymlogger.ui.theme.GymLoggerTheme
import ca.bpmproperty.gymlogger.ui.screens.LogScreen
import ca.bpmproperty.gymlogger.ui.screens.HistoryScreen
import ca.bpmproperty.gymlogger.ui.screens.SummaryScreen
import ca.bpmproperty.gymlogger.ui.screens.LiftingScreen
import ca.bpmproperty.gymlogger.ui.screens.CardioScreen
import ca.bpmproperty.gymlogger.ui.screens.StretchingScreen

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
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "log",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("log") { LogScreen(navController) }
            composable("history") { HistoryScreen() }
            composable("summary") { SummaryScreen() }
            composable("lifting") { LiftingScreen(onBack = { navController.popBackStack() }) }
            composable("cardio") { CardioScreen(onBack = { navController.popBackStack() }) }
            composable("stretching") { StretchingScreen(onBack = { navController.popBackStack() }) }

        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)