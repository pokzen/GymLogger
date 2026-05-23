package ca.bpmproperty.gymlogger.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Date utilities for the calendar-day grouping system.
 *
 * Sessions are grouped by a `dateKey: Int` in YYYYMMDD form (e.g., 20260522).
 * This is fast to index/sort in SQLite, sorts chronologically as-is, and
 * converts cleanly to/from human-readable dates.
 */

/** Today's calendar date as a YYYYMMDD Int, in the device's local timezone. */
fun todayDateKey(): Int = epochMillisToDateKey(System.currentTimeMillis())

/** Convert an epoch-millis timestamp to a YYYYMMDD calendar-day Int (local timezone). */
fun epochMillisToDateKey(epochMillis: Long): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = epochMillis
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return y * 10000 + m * 100 + d
}

/** Convert a YYYYMMDD calendar-day Int to an epoch-millis at midnight local time. */
fun dateKeyToEpochMillis(dateKey: Int): Long {
    val y = dateKey / 10000
    val m = (dateKey / 100) % 100
    val d = dateKey % 100
    val cal = Calendar.getInstance().apply {
        clear()
        set(y, m - 1, d, 0, 0, 0) // Calendar.MONTH is 0-based
    }
    return cal.timeInMillis
}

/** Format YYYYMMDD as "MAY 22" (short month + day). */
fun formatDateKeyShort(dateKey: Int): String {
    val formatter = SimpleDateFormat("MMM d", Locale.getDefault())
    return formatter.format(Date(dateKeyToEpochMillis(dateKey))).uppercase()
}

/** Format YYYYMMDD as "FRIDAY MAY 22". */
fun formatDateKeyLong(dateKey: Int): String {
    val formatter = SimpleDateFormat("EEEE MMM d", Locale.getDefault())
    return formatter.format(Date(dateKeyToEpochMillis(dateKey))).uppercase()
}

/** Format YYYYMMDD as "MAY 22, 2026" — used when the year matters for context. */
fun formatDateKeyWithYear(dateKey: Int): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(dateKeyToEpochMillis(dateKey)))
}

/** True if the dateKey is today's calendar date. */
fun isToday(dateKey: Int): Boolean = dateKey == todayDateKey()

/** True if the dateKey is yesterday's calendar date. */
fun isYesterday(dateKey: Int): Boolean {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return dateKey == epochMillisToDateKey(cal.timeInMillis)
}

/**
 * Human-friendly label for a day:
 * - "TODAY" if today
 * - "YESTERDAY" if yesterday
 * - "FRIDAY MAY 22" otherwise
 */
fun dayLabel(dateKey: Int): String = when {
    isToday(dateKey) -> "TODAY"
    isYesterday(dateKey) -> "YESTERDAY"
    else -> formatDateKeyLong(dateKey)
}

// ---- Calendar grid utilities ----

/** Subtract a number of days from a YYYYMMDD dateKey, returning a new dateKey. */
fun dateKeyMinusDays(dateKey: Int, days: Int): Int {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateKeyToEpochMillis(dateKey)
        add(Calendar.DAY_OF_YEAR, -days)
    }
    return epochMillisToDateKey(cal.timeInMillis)
}

/** Day-of-week index for a dateKey, 0=Monday … 6=Sunday. */
fun dayOfWeekMondayBased(dateKey: Int): Int {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateKeyToEpochMillis(dateKey)
    }
    // Calendar.DAY_OF_WEEK: SUNDAY=1, MONDAY=2, ... SATURDAY=7
    val raw = cal.get(Calendar.DAY_OF_WEEK)
    return ((raw + 5) % 7) // -> MONDAY=0, ..., SUNDAY=6
}

/** Three-letter month label for a dateKey (e.g. "MAY"). */
fun monthLabel(dateKey: Int): String {
    val formatter = SimpleDateFormat("MMM", Locale.getDefault())
    return formatter.format(Date(dateKeyToEpochMillis(dateKey))).uppercase()
}

/** Year-month grouping key (e.g. 202605) for grouping dateKeys by month. */
fun monthKey(dateKey: Int): Int = dateKey / 100

/**
 * Build a calendar grid of [weeksBack] weeks ending with today's week.
 * Returns a list of weeks (columns) ordered oldest → newest.
 * Each week is a list of 7 dateKeys ordered Monday → Sunday.
 *
 * The rightmost (last) week is the week containing today. Cells in that week
 * *after* today are future dates — still valid dateKeys but the UI should
 * treat them as non-interactive.
 */
fun buildCalendarGrid(weeksBack: Int): List<List<Int>> {
    val today = todayDateKey()
    val todayDow = dayOfWeekMondayBased(today) // 0=Mon..6=Sun

    // The Sunday of the current week (in Monday-based week, that's offset +6 - todayDow days from today)
    val daysToEndOfWeek = 6 - todayDow
    val sundayOfCurrentWeek = dateKeyMinusDays(today, -daysToEndOfWeek)
    // i.e., we add `daysToEndOfWeek` days to today. dateKeyMinusDays handles negative input.

    // Now build [weeksBack] weeks ending with the week containing today.
    return (0 until weeksBack).map { weekOffsetFromEnd ->
        // Week index 0 = newest (current week), weeksBack-1 = oldest.
        // Build the Monday → Sunday for the week that's weekOffsetFromEnd weeks before the current week.
        val sundayOfThisWeek = dateKeyMinusDays(sundayOfCurrentWeek, weekOffsetFromEnd * 7)
        (6 downTo 0).map { daysBeforeSunday ->
            dateKeyMinusDays(sundayOfThisWeek, daysBeforeSunday)
        }
    }.reversed() // so oldest week is first (leftmost)
}
