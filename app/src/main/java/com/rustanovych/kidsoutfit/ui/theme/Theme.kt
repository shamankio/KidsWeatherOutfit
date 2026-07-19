package com.rustanovych.kidsoutfit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Sky40,
    onPrimary = Sky100,
    primaryContainer = Sky90,
    onPrimaryContainer = Sky10,
    secondary = Teal40,
    onSecondary = Teal100,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal10,
    tertiary = Sun40,
    onTertiary = Sun100,
    tertiaryContainer = Sun90,
    onTertiaryContainer = Sun10,
    error = Rose40,
    onError = Rose100,
    errorContainer = Rose90,
    onErrorContainer = Rose10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
)

private val DarkColorScheme = darkColorScheme(
    primary = Sky80,
    onPrimary = Sky20,
    primaryContainer = Sky30,
    onPrimaryContainer = Sky90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Sun80,
    onTertiary = Sun20,
    tertiaryContainer = Sun30,
    onTertiaryContainer = Sun90,
    error = Rose80,
    onError = Rose20,
    errorContainer = Rose40,
    onErrorContainer = Rose90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
)

@Composable
fun KidsWeatherOutfitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is intentionally disabled: the background changes with
    // the weather, so the app needs full control over its color scheme.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
