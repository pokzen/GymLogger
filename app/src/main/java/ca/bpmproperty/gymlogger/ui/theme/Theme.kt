package ca.bpmproperty.gymlogger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GymLoggerDarkColors = darkColorScheme(
    // Primary — the electric yellow accent
    primary = AccentYellow,
    onPrimary = OnAccent,
    primaryContainer = AccentYellowPressed,
    onPrimaryContainer = OnAccent,

    // Secondary — also yellow, slightly muted use cases
    secondary = AccentYellow,
    onSecondary = OnAccent,

    // Tertiary — unused brand-wise, but set to something coherent
    tertiary = AccentYellow,
    onTertiary = OnAccent,

    // Surfaces
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceElevated,
    surfaceContainerHighest = SurfaceElevated,

    // Borders / dividers
    outline = Outline,
    outlineVariant = OutlineSubtle,

    // Errors
    error = DangerRed,
    onError = TextPrimary
)

/**
 * App theme. Always dark, no dynamic color — we want our brand
 * yellow regardless of the system wallpaper.
 */
@Composable
fun GymLoggerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GymLoggerDarkColors,
        typography = Typography,
        content = content
    )
}
