package com.geostamp.app.ui.theme

import androidx.compose.ui.graphics.Color

// --- Light Theme Colors ---

/** Primary blue for light theme, representing navigation/GPS elements. */
val LightPrimary = Color(0xFF1565C0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD1E4FF)
val LightOnPrimaryContainer = Color(0xFF001D36)

/** Teal secondary for light theme, complementing the GPS aesthetic. */
val LightSecondary = Color(0xFF00897B)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFB2DFDB)
val LightOnSecondaryContainer = Color(0xFF00201D)

val LightTertiary = Color(0xFF7C5800)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDEA1)
val LightOnTertiaryContainer = Color(0xFF271900)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1A1C1E)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF1A1C1E)
val LightSurfaceVariant = Color(0xFFE0E2EC)
val LightOnSurfaceVariant = Color(0xFF44474E)
val LightOutline = Color(0xFF74777F)

// --- Dark Theme Colors ---

/** Light blue primary for dark theme, providing good contrast on dark backgrounds. */
val DarkPrimary = Color(0xFF64B5F6)
val DarkOnPrimary = Color(0xFF003258)
val DarkPrimaryContainer = Color(0xFF004A7C)
val DarkOnPrimaryContainer = Color(0xFFD1E4FF)

/** Teal secondary for dark theme. */
val DarkSecondary = Color(0xFF4DB6AC)
val DarkOnSecondary = Color(0xFF003733)
val DarkSecondaryContainer = Color(0xFF00504B)
val DarkOnSecondaryContainer = Color(0xFFB2DFDB)

val DarkTertiary = Color(0xFFE5C16E)
val DarkOnTertiary = Color(0xFF412D00)
val DarkTertiaryContainer = Color(0xFF5D4200)
val DarkOnTertiaryContainer = Color(0xFFFFDEA1)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE3E2E6)
val DarkSurface = Color(0xFF1A1C1E)
val DarkOnSurface = Color(0xFFE3E2E6)
val DarkSurfaceVariant = Color(0xFF44474E)
val DarkOnSurfaceVariant = Color(0xFFC4C6D0)
val DarkOutline = Color(0xFF8E9099)

// --- Location Panel Backgrounds ---

/** Background color for the location info panel in light theme. */
val LocationPanelBackgroundLight = Color(0xFFF5F5F5)

/** Background color for the location info panel in dark theme. */
val LocationPanelBackgroundDark = Color(0xFF1E1E1E)

// --- GPS Accuracy Indicator Colors ---

/** Green indicator for good GPS accuracy (e.g., < 10m). */
val AccuracyGood = Color(0xFF4CAF50)

/** Orange indicator for medium GPS accuracy (e.g., 10–50m). */
val AccuracyMedium = Color(0xFFFF9800)

/** Red indicator for poor GPS accuracy (e.g., > 50m). */
val AccuracyPoor = Color(0xFFF44336)
