package ca.sb.gymlogger.ui.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ca.sb.gymlogger.data.GymLoggerApplication
import ca.sb.gymlogger.data.WorkoutRepository

/**
 * Compose helper: obtain a ViewModel of type [VM] using a factory that
 * receives the repository. Use from a @Composable like:
 *
 *     val vm: LiftingViewModel = workoutViewModel { LiftingViewModel(it) }
 */
@Composable
inline fun <reified VM : ViewModel> workoutViewModel(
    crossinline create: (WorkoutRepository) -> VM
): VM {
    val context = LocalContext.current
    val app = context.applicationContext as GymLoggerApplication
    val factory = viewModelFactory {
        initializer { create(app.repository) }
    }
    return viewModel(factory = factory)
}
