package com.xianliticn.yuefu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = Color.White,
    primaryContainer = BlueAccentContainer,
    onPrimaryContainer = Color(0xFF1A237E),

    secondary = GreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E6D9),
    onSecondaryContainer = Color(0xFF3D3C2E),

    tertiary = YellowMain,
    onTertiary = Color(0xFF3D3A1A),
    tertiaryContainer = YellowContainer,
    onTertiaryContainer = Color(0xFF5C5720),

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = BgLight,
    onBackground = TextPrimary,
    surface = BgLight,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF0EFE6),
    onSurfaceVariant = GreenDark,
    outline = GreenMuted,
    outlineVariant = Color(0xFFD5D3C5),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    inverseSurface = TextPrimary,
    inverseOnSurface = Color(0xFFF5F4F0),
    inversePrimary = BlueAccentDark,
    surfaceTint = BlueAccent,
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccentDark,
    onPrimary = Color(0xFF1A237E),
    primaryContainer = BlueAccentContainerDark,
    onPrimaryContainer = BlueAccentContainer,

    secondary = GreenDarkDark,
    onSecondary = Color(0xFF3D3C2E),
    secondaryContainer = Color(0xFF555341),
    onSecondaryContainer = Color(0xFFE8E6D9),

    tertiary = YellowMainDark,
    onTertiary = Color(0xFF3D3A1A),
    tertiaryContainer = YellowContainerDark,
    onTertiaryContainer = YellowContainer,

    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = BgDark,
    onBackground = Color(0xFFE8E6DF),
    surface = BgDark,
    onSurface = Color(0xFFE8E6DF),
    surfaceVariant = Color(0xFF2A2A22),
    onSurfaceVariant = GreenDarkDark,
    outline = Color(0xFF8A8870),
    outlineVariant = Color(0xFF444430),
    surfaceContainerLow = Color(0xFF2A2A14),
    surfaceContainer = Color(0xFF242418),
    inverseSurface = Color(0xFFE8E6DF),
    inverseOnSurface = TextPrimary,
    inversePrimary = BlueAccent,
    surfaceTint = BlueAccentDark,
)

@Composable
fun YuefuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
