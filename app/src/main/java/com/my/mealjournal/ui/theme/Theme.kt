package com.my.mealjournal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val WarmLight = lightColorScheme(
    primary = Color(0xFF8A4B2A),
    onPrimary = Color(0xFFFFF3E6),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF331202),
    secondary = Color(0xFF6F5B40),
    background = Color(0xFFFBF7F2),
    surface = Color(0xFFFFFBF6),
)

private val WarmDark = darkColorScheme(
    primary = Color(0xFFFFB689),
    onPrimary = Color(0xFF532100),
    primaryContainer = Color(0xFF70330F),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFDDC3A2),
    background = Color(0xFF17120D),
    surface = Color(0xFF1E1813),
)

@Composable
fun MealJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmDark
        else -> WarmLight
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
