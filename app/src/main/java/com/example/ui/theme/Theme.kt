package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TacticalColorScheme = darkColorScheme(
    primary = CyanIntel,         // Premium Lavender / Purple Accent (#D0BCFF)
    secondary = SurfaceGunmetalLight, // (#2F3033)
    tertiary = AccentAmber,
    background = TacticalDark,   // (#0F1113)
    surface = SurfaceGunmetal,   // (#1A1C1E)
    onPrimary = TacticalDark,
    onSecondary = BrightWhiteText,
    onTertiary = TacticalDark,
    onBackground = BrightWhiteText,
    onSurface = BrightWhiteText,
    error = AuditingCrimson      // Coral red alert
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force gorgeous tactile dark mode
    dynamicColor: Boolean = false, // Keep styling unified & authentic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}
