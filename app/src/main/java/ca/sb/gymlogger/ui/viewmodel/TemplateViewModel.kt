package ca.sb.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.TemplateExercise
import ca.sb.gymlogger.data.WorkoutRepository
import ca.sb.gymlogger.data.WorkoutTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A template surfaced for display — with the exercises parsed out of JSON.
 */
data class TemplateView(
    val id: Int,
    val name: String,
    val exercises: List<TemplateExercise>
) {
    val exerciseCount: Int get() = exercises.size
    val totalSets: Int get() = exercises.sumOf { it.prescribedReps.size }
}

class TemplateViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    val templates: StateFlow<List<TemplateView>> = repository.getAllTemplates()
        .map { list ->
            list.map { t ->
                val parsed: List<TemplateExercise> = try {
                    if (t.exercises.isBlank()) emptyList() else json.decodeFromString(t.exercises)
                } catch (_: Throwable) {
                    emptyList()
                }
                TemplateView(id = t.id, name = t.name, exercises = parsed)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Save a template (insert if id == 0, update otherwise). Returns the saved row id. */
    fun saveTemplate(
        id: Int,
        name: String,
        exercises: List<TemplateExercise>,
        onDone: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val exercisesJson = Json.encodeToString(exercises)
            if (id == 0) {
                val newId = repository.insertTemplate(
                    WorkoutTemplate(name = name, exercises = exercisesJson, displayOrder = 0)
                ).toInt()
                onDone(newId)
            } else {
                val existing = repository.getTemplate(id)
                if (existing != null) {
                    repository.updateTemplate(
                        existing.copy(name = name, exercises = exercisesJson)
                    )
                    onDone(id)
                }
            }
        }
    }

    fun deleteTemplate(template: TemplateView) {
        viewModelScope.launch {
            val existing = repository.getTemplate(template.id) ?: return@launch
            repository.deleteTemplate(existing)
        }
    }

    /** Load a single template synchronously into a callback. Used by the editor when opening. */
    fun loadTemplate(id: Int, onLoaded: (TemplateView?) -> Unit) {
        viewModelScope.launch {
            val t = repository.getTemplate(id)
            val view = t?.let {
                val parsed: List<TemplateExercise> = try {
                    if (it.exercises.isBlank()) emptyList() else json.decodeFromString(it.exercises)
                } catch (_: Throwable) {
                    emptyList()
                }
                TemplateView(id = it.id, name = it.name, exercises = parsed)
            }
            onLoaded(view)
        }
    }
}
