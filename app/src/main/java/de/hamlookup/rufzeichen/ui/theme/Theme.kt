package de.hamlookup.rufzeichen.ui.theme

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
    primary = Navy,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = SkyBlue,
    tertiary = Amber,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White
)

private val DarkColors = darkColorScheme(
    primary = LightSky,
    onPrimary = NavyDark,
    secondary = SkyBlue,
    tertiary = Amber,
    background = SurfaceDark,
    surface = NavyDark
)

@Composable
fun RufzeichenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
