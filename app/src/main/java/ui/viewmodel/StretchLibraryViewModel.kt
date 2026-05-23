package ca.bpmproperty.gymlogger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.bpmproperty.gymlogger.data.LibraryStretch
import ca.bpmproperty.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StretchLibraryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val stretches: StateFlow<List<LibraryStretch>> = repository.getAllLibraryStretches()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Add a new stretch; silently skips if a case-insensitive duplicate exists. */
    fun addStretch(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveLibraryStretchIfNew(name)
        }
    }

    /** Rename an existing stretch. */
    fun updateStretch(stretch: LibraryStretch, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateLibraryStretch(stretch.copy(name = newName.trim()))
        }
    }

    fun deleteStretch(stretch: LibraryStretch) {
        viewModelScope.launch {
            repository.deleteLibraryStretch(stretch)
        }
    }
}
