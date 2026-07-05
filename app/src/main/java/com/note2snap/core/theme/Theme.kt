package com.note2snap.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = WhiteboardBlue,
    onPrimary = SurfaceLight,
    surface = SurfaceLight,
    onSurface = InkBlack,
    secondary = DiagramAccent
)

private val DarkColors = darkColorScheme(
    primary = WhiteboardBlueDark,
    onPrimary = InkBlack,
    surface = SurfaceDark,
    onSurface = SurfaceLight,
    secondary = DiagramAccent
)

@Composable
fun Note2SnapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Note2SnapTypography,
        content = content
    )
}
