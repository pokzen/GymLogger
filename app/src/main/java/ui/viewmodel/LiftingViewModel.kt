package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.ExerciseEntry
import ca.bpmproperty.gymlogger.data.LiftingSession
import ca.bpmproperty.gymlogger.data.SetEntry
import ca.bpmproperty.gymlogger.data.TemplateExercise
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LiftingViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    // In-progress session state, observed by Compose
    val exercises = mutableStateListOf<ExerciseEntry>()

    /**
     * Prescribed reps for each exercise, parallel to [exercises].
     * Map key is the index in `exercises`; value is the list of prescribed reps from the template.
     * Used so that when the user taps "Add Set" we can pre-fill the reps field with the
     * next prescribed rep value (set 1 → list[0], set 2 → list[1], etc.).
     *
     * Empty for exercises added freehand (no template prescription).
     */
    val prescribedReps = mutableStateMapOf<Int, List<Int>>()

    /** Calendar day this session will be attributed to. Defaults to today. */
    var selectedDateKey by mutableStateOf(todayDateKey())

    /** Once true, we've applied any incoming template — guards against double-loading on recompose. */
    private var templateApplied = false

    /**
     * When non-null, save() updates this session row rather than inserting a new one.
     * Set by [loadFromSession] when entering the screen in edit mode.
     */
    private var editingSessionId: Int? = null
    private var editingLoaded = false

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    fun addExercise(name: String, muscleGroup: String = "") {
        exercises.add(ExerciseEntry(name = name, sets = emptyList(), muscleGroup = muscleGroup))
    }

    /**
     * Pre-load this session from a template. Called once on first composition when a
     * templateId was passed via navigation.
     */
    fun applyTemplate(templateId: Int) {
        if (templateApplied || templateId <= 0) return
        templateApplied = true
        viewModelScope.launch {
            val template = repository.getTemplate(templateId) ?: return@launch
            val parsed: List<TemplateExercise> = try {
                if (template.exercises.isBlank()) emptyList()
                else Json.decodeFromString(template.exercises)
            } catch (_: Throwable) {
                emptyList()
            }
            exercises.clear()
            prescribedReps.clear()
            parsed.forEachIndexed { index, te ->
                exercises.add(
                    ExerciseEntry(
                        name = te.name,
                        sets = emptyList(),
                        muscleGroup = te.muscleGroup
                    )
                )
                if (te.prescribedReps.isNotEmpty()) {
                    prescribedReps[index] = te.prescribedReps
                }
            }
        }
    }

    /**
     * Load an existing lifting session for editing. Called once when entering edit mode.
     * Populates `exercises` and `selectedDateKey` from the stored row. Subsequent save()
     * calls will update this row instead of inserting.
     */
    fun loadFromSession(sessionId: Int) {
        if (editingLoaded || sessionId <= 0) return
        editingLoaded = true
        editingSessionId = sessionId
        viewModelScope.launch {
            val session = repository.getLifting(sessionId) ?: return@launch
            val parsed: List<ExerciseEntry> = try {
                if (session.exercises.isBlank()) emptyList()
                else Json.decodeFromString(session.exercises)
            } catch (_: Throwable) {
                emptyList()
            }
            exercises.clear()
            exercises.addAll(parsed)
            selectedDateKey = session.dateKey
            // Prescriptions aren't stored on past sessions, so we don't restore them.
        }
    }

    /** Get the prescribed reps value for the next set of the given exercise, or null. */
    fun nextPrescribedRepsFor(exerciseIndex: Int): Int? {
        val list = prescribedReps[exerciseIndex] ?: return null
        val setsAlreadyAdded = exercises.getOrNull(exerciseIndex)?.sets?.size ?: 0
        return list.getOrNull(setsAlreadyAdded)
    }

    /**
     * Total prescribed sets across all exercises that have a prescription.
     * Freehand exercises (no prescription) don't contribute.
     */
    val totalPrescribedSets: Int
        get() = prescribedReps.values.sumOf { it.size }

    /**
     * Completed sets out of the prescribed total. Capped per-exercise at its prescription
     * length so bonus sets don't push the count above 100%.
     */
    val completedPrescribedSets: Int
        get() = prescribedReps.entries.sumOf { (exerciseIndex, repsList) ->
            val setsLogged = exercises.getOrNull(exerciseIndex)?.sets?.size ?: 0
            minOf(setsLogged, repsList.size)
        }

    /** Percentage of prescribed sets completed (0..100). Null when there are no prescribed sets. */
    val completionPercent: Int?
        get() {
            val total = totalPrescribedSets
            if (total <= 0) return null
            return (completedPrescribedSets * 100) / total
        }

    fun addSet(index: Int, reps: String, weight: String) {
        if (index !in exercises.indices) return
        val updated = exercises[index].copy(
            sets = exercises[index].sets + SetEntry(reps, weight)
        )
        exercises[index] = updated
    }

    /** Replace a specific set within an exercise. No-op if either index is invalid. */
    fun editSet(exerciseIndex: Int, setIndex: Int, reps: String, weight: String) {
        if (exerciseIndex !in exercises.indices) return
        val ex = exercises[exerciseIndex]
        if (setIndex !in ex.sets.indices) return
        val newSets = ex.sets.toMutableList().also {
            it[setIndex] = SetEntry(reps, weight)
        }
        exercises[exerciseIndex] = ex.copy(sets = newSets)
    }

    /** Remove a specific set within an exercise. No-op if either index is invalid. */
    fun removeSet(exerciseIndex: Int, setIndex: Int) {
        if (exerciseIndex !in exercises.indices) return
        val ex = exercises[exerciseIndex]
        if (setIndex !in ex.sets.indices) return
        val newSets = ex.sets.toMutableList().also { it.removeAt(setIndex) }
        exercises[exerciseIndex] = ex.copy(sets = newSets)
    }

    fun removeExercise(index: Int) {
        if (index !in exercises.indices) return
        exercises.removeAt(index)
        // Shift prescribedReps map: remove the index, decrement higher keys.
        val newMap = prescribedReps
            .filter { (k, _) -> k != index }
            .mapKeys { (k, _) -> if (k > index) k - 1 else k }
        prescribedReps.clear()
        prescribedReps.putAll(newMap)
    }

    /** Persist the current session. Calls [onDone] when the database write completes. */
    fun save(onDone: () -> Unit) {
        if (exercises.isEmpty() || isSaving) return
        isSaving = true
        saveError = null
        viewModelScope.launch {
            try {
                val json = Json.encodeToString(exercises.toList())
                val id = editingSessionId
                if (id != null) {
                    // Update existing row, preserving its original timestamp.
                    val existing = repository.getLifting(id)
                    if (existing != null) {
                        repository.updateLifting(
                            existing.copy(dateKey = selectedDateKey, exercises = json)
                        )
                    }
                } else {
                    repository.insertLifting(
                        LiftingSession(
                            dateKey = selectedDateKey,
                            exercises = json
                        )
                    )
                }
                onDone()
            } catch (t: Throwable) {
                saveError = t.message ?: "Failed to save session"
            } finally {
                isSaving = false
            }
        }
    }

    fun clearError() {
        saveError = null
    }
}
