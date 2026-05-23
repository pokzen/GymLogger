package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.StretchEntry
import ca.bpmproperty.gymlogger.data.StretchingSession
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import ca.bpmproperty.gymlogger.data.todayDateKey
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class StretchingViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val stretches = mutableStateListOf<StretchEntry>()

    var notes by mutableStateOf("")

    /** Calendar day this session will be attributed to. Defaults to today. */
    var selectedDateKey by mutableStateOf(todayDateKey())

    var isSaving by mutableStateOf(false)
        private set
    var saveError by mutableStateOf<String?>(null)
        private set

    private var editingSessionId: Int? = null
    private var editingLoaded = false

    private val jsonReader = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Load an existing stretching session into this view-model for editing. */
    fun loadFromSession(sessionId: Int) {
        if (editingLoaded || sessionId <= 0) return
        editingLoaded = true
        editingSessionId = sessionId
        viewModelScope.launch {
            val session = repository.getStretching(sessionId) ?: return@launch
            val parsed: List<StretchEntry> = try {
                if (session.stretches.isBlank()) emptyList()
                else jsonReader.decodeFromString(session.stretches)
            } catch (_: Throwable) {
                emptyList()
            }
            stretches.clear()
            stretches.addAll(parsed)
            notes = session.notes
            selectedDateKey = session.dateKey
        }
    }

    fun addStretch(name: String, duration: String) {
        stretches.add(StretchEntry(name, duration))
    }

    /** Replace a specific stretch in place. No-op if index is invalid. */
    fun editStretch(index: Int, name: String, duration: String) {
        if (index !in stretches.indices) return
        stretches[index] = StretchEntry(name, duration)
    }

    fun removeStretch(index: Int) {
        if (index !in stretches.indices) return
        stretches.removeAt(index)
    }

    /** Persist the current session. Calls [onDone] when the database write completes. */
    fun save(onDone: () -> Unit) {
        if (stretches.isEmpty() || isSaving) return
        isSaving = true
        saveError = null
        viewModelScope.launch {
            try {
                val json = Json.encodeToString(stretches.toList())
                val id = editingSessionId
                if (id != null) {
                    val existing = repository.getStretching(id)
                    if (existing != null) {
                        repository.updateStretching(
                            existing.copy(
                                dateKey = selectedDateKey,
                                stretches = json,
                                notes = notes
                            )
                        )
                    }
                } else {
                    repository.insertStretching(
                        StretchingSession(
                            dateKey = selectedDateKey,
                            stretches = json,
                            notes = notes
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
