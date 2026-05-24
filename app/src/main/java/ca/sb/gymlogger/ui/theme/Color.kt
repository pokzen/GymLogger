package ca.sb.gymlogger.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Brand palette ----

/** Pure black background — OLED-friendly. */
val Background = Color(0xFF000000)

/** Card surface — very dark gray for subtle separation from background. */
val Surface = Color(0xFF0F0F0F)

/** Elevated surface — dialogs, bottom sheets. */
val SurfaceElevated = Color(0xFF1A1A1A)

/** Hairline border on cards and inputs. */
val Outline = Color(0xFF2A2A2A)
val OutlineSubtle = Color(0xFF1F1F1F)

/** Electric yellow — the signature accent. Use sparingly. */
val AccentYellow = Color(0xFFD4FF3F)

/** Slightly darkened yellow for pressed/hover states. */
val AccentYellowPressed = Color(0xFFB8E028)

/** Text on yellow surfaces (use black, not white — high contrast). */
val OnAccent = Color(0xFF000000)

// ---- Text colors ----

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8A8A8A)
val TextTertiary = Color(0xFF5A5A5A)

// ---- Semantic colors ----

val DangerRed = Color(0xFFFF5252)
val SuccessGreen = Color(0xFF4CD964)
