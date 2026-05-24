package ca.sb.gymlogger.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads a GymLogger JSON export from a [Uri] and writes the sessions back into the
 * Room database. Mirrors [SessionExporter].
 *
 * Imported records always get fresh auto-generated IDs — the IDs in the export file
 * are read but ignored. This keeps merge mode safe (no PK collisions) and replace mode
 * clean (fresh sequence after the wipe).
 */
class SessionImporter(
    private val context: Context,
    private val repository: WorkoutRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** What to do with the user's existing session data before inserting the import. */
    enum class Mode { REPLACE, MERGE }

    suspend fun import(uri: Uri, mode: Mode): ImportResult = withContext(Dispatchers.IO) {
        // Read the file the user picked.
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().readText()
        } ?: throw IllegalStateException("Could not open the selected file.")

        if (text.isBlank()) {
            throw IllegalStateException("The selected file is empty.")
        }

        val exportData: ExportData = try {
            json.decodeFromString(text)
        } catch (t: Throwable) {
            throw IllegalStateException(
                "Could not parse this file as a GymLogger export. " +
                    "Make sure you picked the .json file, not the .md file."
            )
        }

        if (exportData.exportVersion > SessionExporter.EXPORT_VERSION) {
            throw IllegalStateException(
                "This export was made by a newer version of GymLogger (v${exportData.exportVersion}) " +
                    "than this app supports (v${SessionExporter.EXPORT_VERSION}). " +
                    "Update the app and try again."
            )
        }

        if (mode == Mode.REPLACE) {
            repository.wipeAllSessions()
        }

        var lifting = 0
        var cardio = 0
        var stretching = 0
        var skipped = 0

        exportData.days.forEach { day ->
            day.sessions.forEach { session ->
                try {
                    when (session.type) {
                        "weights" -> {
                            repository.insertLifting(
                                LiftingSession(
                                    id = 0, // let Room assign
                                    date = parseIsoToMillis(session.loggedAt),
                                    dateKey = day.dateKey,
                                    exercises = json.encodeToString(session.exercises)
                                )
                            )
                            lifting++
                        }
                        "cardio" -> {
                            repository.insertCardio(
                                CardioSession(
                                    id = 0,
                                    date = parseIsoToMillis(session.loggedAt),
                                    dateKey = day.dateKey,
                                    cardioType = session.cardioType ?: "treadmill",
                                    duration = session.duration.orEmpty(),
                                    distance = session.distance.orEmpty(),
                                    calories = session.calories.orEmpty(),
                                    phases = if (session.phases.isEmpty()) "" else json.encodeToString(session.phases),
                                    notes = session.notes.orEmpty()
                                )
                            )
                            cardio++
                        }
                        "stretching" -> {
                            repository.insertStretching(
                                StretchingSession(
                                    id = 0,
                                    date = parseIsoToMillis(session.loggedAt),
                                    dateKey = day.dateKey,
                                    stretches = json.encodeToString(session.stretches),
                                    notes = session.notes.orEmpty()
                                )
                            )
                            stretching++
                        }
                        else -> skipped++
                    }
                } catch (_: Throwable) {
                    skipped++
                }
            }
        }

        ImportResult(
            liftingImported = lifting,
            cardioImported = cardio,
            stretchingImported = stretching,
            skipped = skipped,
            mode = mode
        )
    }

    /**
     * Best-effort ISO-8601 parse. Falls back to "now" if the format is unrecognized —
     * the dateKey on the parent day is the authoritative calendar grouping, so a stray
     * loggedAt millis won't misplace the session in the calendar view.
     */
    private fun parseIsoToMillis(iso: String): Long {
        return try {
            val patterns = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                try {
                    val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                    return sdf.parse(iso)?.time ?: continue
                } catch (_: Throwable) {
                    // try next pattern
                }
            }
            System.currentTimeMillis()
        } catch (_: Throwable) {
            System.currentTimeMillis()
        }
    }
}

data class ImportResult(
    val liftingImported: Int,
    val cardioImported: Int,
    val stretchingImported: Int,
    val skipped: Int,
    val mode: SessionImporter.Mode
) {
    val total: Int get() = liftingImported + cardioImported + stretchingImported

    fun summary(): String {
        val parts = buildList {
            if (liftingImported > 0) add("$liftingImported lifting")
            if (cardioImported > 0) add("$cardioImported cardio")
            if (stretchingImported > 0) add("$stretchingImported stretching")
        }
        return if (parts.isEmpty()) {
            "No sessions imported."
        } else {
            val core = parts.joinToString(", ") + " session${if (total == 1) "" else "s"} imported"
            val modeNote = when (mode) {
                SessionImporter.Mode.REPLACE -> " (existing data replaced)"
                SessionImporter.Mode.MERGE -> " (added to existing data)"
            }
            val skippedNote = if (skipped > 0) " — $skipped entry/entries skipped" else ""
            "$core$modeNote.$skippedNote"
        }
    }
}
