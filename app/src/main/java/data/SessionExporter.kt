package ca.bpmproperty.gymlogger.data

import android.content.Context
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Builds JSON + Markdown exports of all session data and writes them to the app's cache
 * directory. Returns URIs (via FileProvider) that can be passed to a share intent.
 */
class SessionExporter(
    private val context: Context,
    private val repository: WorkoutRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    private val inputJson = Json { ignoreUnknownKeys = true }

    /**
     * Build both files and return their FileProvider URIs.
     * Files are placed under cacheDir/exports/ which is declared in file_paths.xml.
     */
    suspend fun exportAll(): ExportResult = withContext(Dispatchers.IO) {
        // Snapshot one emission of each flow
        val lifting = repository.getAllLifting().first()
        val cardio = repository.getAllCardio().first()
        val stretching = repository.getAllStretching().first()

        val days = buildDayData(lifting, cardio, stretching)

        val exportObject = ExportData(
            exportedAt = isoNow(),
            exportVersion = EXPORT_VERSION,
            days = days
        )
        val jsonString = json.encodeToString(exportObject)
        val markdownString = renderMarkdown(exportObject)

        val timestamp = filenameTimestamp()
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val jsonFile = File(exportDir, "gymlogger-$timestamp.json").apply {
            writeText(jsonString)
        }
        val mdFile = File(exportDir, "gymlogger-$timestamp.md").apply {
            writeText(markdownString)
        }

        val authority = "${context.packageName}.fileprovider"
        ExportResult(
            jsonUri = FileProvider.getUriForFile(context, authority, jsonFile),
            markdownUri = FileProvider.getUriForFile(context, authority, mdFile),
            displayName = "GymLogger Export — $timestamp"
        )
    }

    // ---- Day grouping ----

    private fun buildDayData(
        lifting: List<LiftingSession>,
        cardio: List<CardioSession>,
        stretching: List<StretchingSession>
    ): List<ExportDay> {
        val keys = buildSet<Int> {
            lifting.forEach { add(it.dateKey) }
            cardio.forEach { add(it.dateKey) }
            stretching.forEach { add(it.dateKey) }
        }
        return keys.sortedDescending().map { dateKey ->
            val sessions = mutableListOf<ExportSession>()
            lifting.filter { it.dateKey == dateKey }.forEach { sessions.add(toExportSession(it)) }
            cardio.filter { it.dateKey == dateKey }.forEach { sessions.add(toExportSession(it)) }
            stretching.filter { it.dateKey == dateKey }.forEach { sessions.add(toExportSession(it)) }
            ExportDay(
                date = dateKeyToIsoDate(dateKey),
                dateKey = dateKey,
                sessions = sessions
            )
        }
    }

    private fun toExportSession(s: LiftingSession): ExportSession {
        val exercises: List<ExerciseEntry> = try {
            if (s.exercises.isBlank()) emptyList() else inputJson.decodeFromString(s.exercises)
        } catch (_: Throwable) {
            emptyList()
        }
        return ExportSession(
            type = "weights",
            id = s.id,
            loggedAt = isoTime(s.date),
            exercises = exercises
        )
    }

    private fun toExportSession(s: CardioSession): ExportSession {
        val phases: List<CardioPhase> = try {
            if (s.phases.isBlank()) emptyList() else inputJson.decodeFromString(s.phases)
        } catch (_: Throwable) {
            emptyList()
        }
        return ExportSession(
            type = "cardio",
            id = s.id,
            loggedAt = isoTime(s.date),
            cardioType = s.cardioType,
            duration = s.duration.ifBlank { null },
            distance = s.distance.ifBlank { null },
            calories = s.calories.ifBlank { null },
            phases = phases,
            notes = s.notes.ifBlank { null }
        )
    }

    private fun toExportSession(s: StretchingSession): ExportSession {
        val stretches: List<StretchEntry> = try {
            if (s.stretches.isBlank()) emptyList() else inputJson.decodeFromString(s.stretches)
        } catch (_: Throwable) {
            emptyList()
        }
        return ExportSession(
            type = "stretching",
            id = s.id,
            loggedAt = isoTime(s.date),
            stretches = stretches,
            notes = s.notes.ifBlank { null }
        )
    }

    // ---- Markdown rendering ----

    private fun renderMarkdown(data: ExportData): String = buildString {
        appendLine("# GymLogger Export")
        appendLine()
        appendLine("_Exported ${data.exportedAt} — ${data.days.size} day(s) of training_")
        appendLine()

        if (data.days.isEmpty()) {
            appendLine("No workouts logged yet.")
            return@buildString
        }

        data.days.forEach { day ->
            appendLine("## ${formatDateKeyHuman(day.dateKey)}")
            appendLine()
            day.sessions.forEach { session ->
                renderSessionMarkdown(this, session)
                appendLine()
            }
        }
    }

    private fun renderSessionMarkdown(sb: StringBuilder, s: ExportSession) {
        when (s.type) {
            "weights" -> {
                sb.appendLine("### Weights")
                if (s.exercises.isEmpty()) {
                    sb.appendLine("_(no exercises recorded)_")
                    return
                }
                val totalSets = s.exercises.sumOf { it.sets.size }
                val totalVolume = s.exercises.sumOf { ex ->
                    ex.sets.sumOf { set ->
                        (set.reps.toIntOrNull() ?: 0) * (set.weight.toIntOrNull() ?: 0)
                    }
                }
                sb.appendLine("**${s.exercises.size} exercises · $totalSets sets · ${totalVolume} lbs volume**")
                sb.appendLine()
                s.exercises.forEach { ex ->
                    val muscle = if (ex.muscleGroup.isNotBlank()) " _(${ex.muscleGroup})_" else ""
                    sb.appendLine("- **${ex.name}**$muscle")
                    ex.sets.forEachIndexed { i, set ->
                        sb.appendLine("  - Set ${i + 1}: ${set.reps} reps @ ${set.weight} lbs")
                    }
                }
            }
            "cardio" -> {
                val typeLabel = (s.cardioType ?: "").replaceFirstChar { it.uppercase() }
                sb.appendLine("### Cardio${if (typeLabel.isNotBlank()) " · $typeLabel" else ""}")
                val parts = buildList {
                    s.duration?.let { add("$it min") }
                    s.distance?.let { add("$it mi") }
                    s.calories?.let { add("$it cal") }
                }
                if (parts.isNotEmpty()) {
                    sb.appendLine(parts.joinToString(" · "))
                }
                if (s.phases.isNotEmpty()) {
                    sb.appendLine()
                    sb.appendLine("Phases:")
                    s.phases.forEach { phase ->
                        sb.appendLine("- ${formatPhaseLine(phase)}")
                    }
                }
                s.notes?.let {
                    sb.appendLine()
                    sb.appendLine("_${it}_")
                }
            }
            "stretching" -> {
                sb.appendLine("### Stretching")
                if (s.stretches.isEmpty()) {
                    sb.appendLine("_(no stretches recorded)_")
                } else {
                    s.stretches.forEach { st ->
                        val dur = if (st.duration.isNotBlank()) " — ${st.duration}s" else ""
                        sb.appendLine("- ${st.name}$dur")
                    }
                }
                s.notes?.let {
                    sb.appendLine()
                    sb.appendLine("_${it}_")
                }
            }
        }
    }

    private fun formatPhaseLine(phase: CardioPhase): String {
        val parts = mutableListOf<String>()
        if (phase.label.isNotBlank()) parts.add(phase.label)
        if (phase.durationMin.isNotBlank()) parts.add("${phase.durationMin} min")
        val speed = rangeOrSingle(phase.speedStart, phase.speedEnd)
        if (speed != null) parts.add("$speed mph")
        val incline = rangeOrSingle(phase.inclineStart, phase.inclineEnd)
        if (incline != null) parts.add("$incline% incline")
        return parts.joinToString(" · ")
    }

    private fun rangeOrSingle(start: String, end: String): String? {
        val s = start.trim()
        val e = end.trim()
        return when {
            s.isBlank() && e.isBlank() -> null
            s.isBlank() -> e
            e.isBlank() -> s
            s == e -> s
            else -> "$s→$e"
        }
    }

    // ---- Helpers ----

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())

    private fun isoTime(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date(epochMillis))

    private fun filenameTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date())

    private fun dateKeyToIsoDate(dateKey: Int): String {
        val y = dateKey / 10000
        val m = (dateKey / 100) % 100
        val d = dateKey % 100
        return "%04d-%02d-%02d".format(y, m, d)
    }

    private fun formatDateKeyHuman(dateKey: Int): String {
        val formatter = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(dateKeyToEpochMillis(dateKey)))
    }

    companion object {
        const val EXPORT_VERSION = 1
    }
}

/** The two file URIs from a successful export, plus a label for the share-sheet subject. */
data class ExportResult(
    val jsonUri: android.net.Uri,
    val markdownUri: android.net.Uri,
    val displayName: String
)

// ---- JSON shape (kept separate from DB entities so the export format is stable) ----

@Serializable
data class ExportData(
    val exportedAt: String,
    val exportVersion: Int,
    val days: List<ExportDay>
)

@Serializable
data class ExportDay(
    val date: String,         // ISO yyyy-MM-dd
    val dateKey: Int,
    val sessions: List<ExportSession>
)

@Serializable
data class ExportSession(
    val type: String,         // "weights" | "cardio" | "stretching"
    val id: Int,
    val loggedAt: String,
    // weights
    val exercises: List<ExerciseEntry> = emptyList(),
    // cardio
    val cardioType: String? = null,
    val duration: String? = null,
    val distance: String? = null,
    val calories: String? = null,
    val phases: List<CardioPhase> = emptyList(),
    // stretching
    val stretches: List<StretchEntry> = emptyList(),
    // common
    val notes: String? = null
)
