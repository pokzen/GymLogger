package ca.sb.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.LibraryExercise
import ca.sb.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A muscle group with its associated exercises (sorted by name).
 * Used to drive the grouped picker UI.
 */
data class ExerciseGroup(
    val muscleGroup: String,
    val exercises: List<LibraryExercise>
)

class LibraryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    /** Flat list of all library exercises. */
    val all: StateFlow<List<LibraryExercise>> = repository.getAllLibraryExercises()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Same data grouped by muscle group, with groups in [MUSCLE_GROUP_ORDER] order. */
    val grouped: StateFlow<List<ExerciseGroup>> = repository.getAllLibraryExercises()
        .map { list ->
            val byGroup = list.groupBy { it.muscleGroup.trim().ifBlank { "Other" } }
            // Use the canonical order for known groups; append unknown groups in alpha order.
            val known = MUSCLE_GROUP_ORDER.filter { byGroup.containsKey(it) }
            val unknown = byGroup.keys.filterNot { it in MUSCLE_GROUP_ORDER }.sorted()
            (known + unknown).map { group ->
                ExerciseGroup(
                    muscleGroup = group,
                    exercises = (byGroup[group] ?: emptyList()).sortedBy { it.name.lowercase() }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun addExercise(name: String, muscleGroup: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertLibraryExercise(
                LibraryExercise(
                    name = name.trim(),
                    muscleGroup = muscleGroup.trim().ifBlank { "Other" },
                    isSeeded = false
                )
            )
        }
    }

    fun updateExercise(exercise: LibraryExercise) {
        viewModelScope.launch {
            repository.updateLibraryExercise(exercise)
        }
    }

    fun deleteExercise(exercise: LibraryExercise) {
        viewModelScope.launch {
            repository.deleteLibraryExercise(exercise)
        }
    }
}

/** Canonical ordering of muscle groups in pickers and library screens. */
val MUSCLE_GROUP_ORDER = listOf(
    "Chest",
    "Back",
    "Shoulders",
    "Biceps",
    "Triceps",
    "Legs",
    "Calves",
    "Abs",
    "Other"
)
