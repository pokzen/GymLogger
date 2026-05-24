package ca.sb.gymlogger.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin wrapper around SharedPreferences for app-wide user preferences.
 *
 * Anything that's a single-value setting (a string, a boolean, a number) belongs here.
 * Anything that's structured data (sessions, templates, library entries) belongs in Room.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * The cardio type new sessions start with. Stored as the [CardioType.storageValue]
     * string. Defaults to "treadmill" — the original hardcoded default — for users
     * who haven't picked a preference yet.
     */
    var defaultCardioType: String
        get() = prefs.getString(KEY_DEFAULT_CARDIO_TYPE, DEFAULT_CARDIO) ?: DEFAULT_CARDIO
        set(value) {
            prefs.edit().putString(KEY_DEFAULT_CARDIO_TYPE, value).apply()
        }

    /**
     * The user's log name — shown as the eyebrow text on the home screen ("YOUR LOG"
     * in caps there), in the navigation drawer header, and on the About screen.
     */
    var logName: String
        get() = prefs.getString(KEY_LOG_NAME, DEFAULT_LOG_NAME) ?: DEFAULT_LOG_NAME
        set(value) {
            prefs.edit().putString(KEY_LOG_NAME, value).apply()
        }

    /** Headline greeting on the home screen below the eyebrow. Free-form. */
    var greeting: String
        get() = prefs.getString(KEY_GREETING, DEFAULT_GREETING) ?: DEFAULT_GREETING
        set(value) {
            prefs.edit().putString(KEY_GREETING, value).apply()
        }

    companion object {
        private const val PREF_NAME = "gymlogger_prefs"
        private const val KEY_DEFAULT_CARDIO_TYPE = "default_cardio_type"
        private const val DEFAULT_CARDIO = "treadmill"
        private const val KEY_LOG_NAME = "log_name"
        const val DEFAULT_LOG_NAME = "Your Log"
        private const val KEY_GREETING = "greeting"
        const val DEFAULT_GREETING = "What's today?"
    }
}
