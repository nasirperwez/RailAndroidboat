package com.example.railchatbot.presentation.theme

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

private val Primary = Color(0xFF1976D2)
private val PrimaryVariant = Color(0xFF1565C0)
private val Secondary = Color(0xFFFF6F00)
private val SecondaryVariant = Color(0xFFE65100)
private val Background = Color(0xFFF5F5F5)
private val Surface = Color(0xFFFFFFFF)
private val Error = Color(0xFFB00020)
private val OnPrimary = Color.White
private val OnSecondary = Color.White
private val OnBackground = Color(0xFF212121)
private val OnSurface = Color(0xFF212121)
private val OnError = Color.White

private val DarkPrimary = Color(0xFF90CAF9)
private val DarkSecondary = Color(0xFFFFB74D)
private val DarkBackground = Color(0xFF121212)
private val DarkSurface = Color(0xFF1E1E1E)
private val DarkOnPrimary = Color(0xFF212121)
private val DarkOnSecondary = Color(0xFF212121)
private val DarkOnBackground = Color.White
private val DarkOnSurface = Color.White

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryVariant,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Primary,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Secondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    error = Error,
    onError = OnError
)

@Composable
fun RailChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
