package com.example.meuassistente.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.meuassistente.data.local.datastore.ThemeModePref

private val OfficialDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF153A44),
    onPrimaryContainer = DarkPrimary,
    secondary = DarkPrimary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    tertiary = WarningAmber,
    onTertiary = OnWarningAmber,
    tertiaryContainer = Color(0xFF3E2E08),
    onTertiaryContainer = WarningAmber,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0B1219),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF203040),
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF2C4054),
    outline = DarkOutline,
    outlineVariant = Color(0xFF2A3B4C),
    error = AlertCoral,
    onError = OnAlertCoral,
    errorContainer = Color(0xFF47181B),
    onErrorContainer = Color(0xFFFFDAD8)
)

private val OfficialLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFD6EFEF),
    onPrimaryContainer = Color(0xFF043B3D),
    secondary = LightPrimary,
    onSecondary = LightOnPrimary,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightOnSurface,
    tertiary = WarningAmber,
    onTertiary = OnWarningAmber,
    tertiaryContainer = Color(0xFFFFF0D1),
    onTertiaryContainer = Color(0xFF3D2900),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFE9F0F4),
    surfaceContainerHigh = LightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFD6E3EA),
    outline = LightOutline,
    outlineVariant = Color(0xFFD8E3EA),
    error = AlertCoral,
    onError = OnAlertCoral,
    errorContainer = Color(0xFFFDE8E8),
    onErrorContainer = Color(0xFF6E1416)
)

@Composable
fun MeuAssistenteTheme(
    themeMode: ThemeModePref = ThemeModePref.SYSTEM,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModePref.DARK -> true
        ThemeModePref.LIGHT -> false
        ThemeModePref.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) OfficialDarkColorScheme else OfficialLightColorScheme
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * fontScale.coerceIn(0.85f, 1.35f)
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
