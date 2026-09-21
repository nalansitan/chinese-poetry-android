package com.nalansitan.chinesepoetry.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 浅色主题配色
private val LightColorScheme = lightColorScheme(
    primary = PoetryColors.CinnabarRed,
    onPrimary = PoetryColors.PaperWhite,
    primaryContainer = PoetryColors.CinnabarRed.copy(alpha = 0.1f),
    onPrimaryContainer = PoetryColors.CinnabarRed,
    
    secondary = PoetryColors.Indigo,
    onSecondary = PoetryColors.PaperWhite,
    secondaryContainer = PoetryColors.Indigo.copy(alpha = 0.1f),
    onSecondaryContainer = PoetryColors.Indigo,
    
    tertiary = PoetryColors.PaleGold,
    onTertiary = PoetryColors.InkBlack,
    
    background = PoetryColors.PaperWhite,
    onBackground = PoetryColors.InkBlack,
    
    surface = PoetryColors.LightGray,
    onSurface = PoetryColors.InkBlack,
    surfaceVariant = PoetryColors.WarmBeige,
    onSurfaceVariant = PoetryColors.DarkBrown,
    
    error = PoetryColors.CinnabarRed,
    onError = PoetryColors.PaperWhite,
    
    outline = PoetryColors.DividerColor,
    outlineVariant = PoetryColors.LightGray
)

// 深色主题配色
private val DarkColorScheme = darkColorScheme(
    primary = PoetryDarkColors.Accent,
    onPrimary = PoetryDarkColors.Background,
    primaryContainer = PoetryDarkColors.Accent.copy(alpha = 0.2f),
    onPrimaryContainer = PoetryDarkColors.Accent,
    
    secondary = PoetryColors.Indigo,
    onSecondary = PoetryDarkColors.Background,
    
    background = PoetryDarkColors.Background,
    onBackground = PoetryDarkColors.OnBackground,
    
    surface = PoetryDarkColors.Surface,
    onSurface = PoetryDarkColors.OnSurface,
    
    error = PoetryColors.CinnabarRed,
    onError = PoetryDarkColors.Background
)

@Composable
fun PoetryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 禁用动态颜色以保持古风风格
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PoetryTypography,
        content = content
    )
}
