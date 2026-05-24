package ca.sb.gymlogger.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.ImportResult
import ca.sb.gymlogger.data.SessionImporter
import ca.sb.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.launch

/**
 * Drives the import flow. The screen handles file-picker UI and the Replace/Merge
 * dialog, then calls [import] with the picked Uri and the chosen mode. The VM does
 * the rest and reports back via [isImporting], [importError], and [lastResult].
 */
class ImportViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    var isImporting by mutableStateOf(false)
        private set
    var importError by mutableStateOf<String?>(null)
        private set
    var lastResult by mutableStateOf<ImportResult?>(null)
        private set

    fun import(context: Context, uri: Uri, mode: SessionImporter.Mode) {
        if (isImporting) return
        isImporting = true
        importError = null
        lastResult = null
        viewModelScope.launch {
            try {
                val importer = SessionImporter(context.applicationContext, repository)
                lastResult = importer.import(uri, mode)
            } catch (t: Throwable) {
                importError = t.message ?: "Import failed"
            } finally {
                isImporting = false
            }
        }
    }

    fun clearError() {
        importError = null
    }

    fun clearResult() {
        lastResult = null
    }
}
