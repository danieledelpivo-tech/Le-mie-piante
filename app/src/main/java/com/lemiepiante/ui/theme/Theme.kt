package com.lemiepiante.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6B3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2F1B6),
    onPrimaryContainer = Color(0xFF00210A),
    secondary = Color(0xFF516351),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E8D1),
    onSecondaryContainer = Color(0xFF0F1F11),
    tertiary = Color(0xFF39656D),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF2),
    onBackground = Color(0xFF181D17),
    surface = Color(0xFFF7FBF2),
    onSurface = Color(0xFF181D17),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF97D59B),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF135224),
    onPrimaryContainer = Color(0xFFB2F1B6),
    secondary = Color(0xFFB7CCB5),
    onSecondary = Color(0xFF233425),
    secondaryContainer = Color(0xFF3A4B3A),
    onSecondaryContainer = Color(0xFFD3E8D1),
    tertiary = Color(0xFFA1CED7),
    onTertiary = Color(0xFF00363D),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE0E4DB),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFE0E4DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BD),
)

@Composable
fun LeMiePianteTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
