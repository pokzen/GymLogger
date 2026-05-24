package ca.sb.gymlogger.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.sb.gymlogger.data.ExportResult
import ca.sb.gymlogger.data.SessionExporter
import ca.sb.gymlogger.data.WorkoutRepository
import kotlinx.coroutines.launch

class ExportViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    var isExporting by mutableStateOf(false)
        private set
    var exportError by mutableStateOf<String?>(null)
        private set

    /**
     * Build the export files and call [onReady] with the share-ready URIs.
     * The caller is responsible for launching the actual share intent — that needs
     * a Context, and we keep this VM Context-free.
     */
    fun export(context: Context, onReady: (ExportResult) -> Unit) {
        if (isExporting) return
        isExporting = true
        exportError = null
        viewModelScope.launch {
            try {
                val exporter = SessionExporter(context.applicationContext, repository)
                val result = exporter.exportAll()
                onReady(result)
            } catch (t: Throwable) {
                exportError = t.message ?: "Export failed"
            } finally {
                isExporting = false
            }
        }
    }

    fun clearError() {
        exportError = null
    }
}
