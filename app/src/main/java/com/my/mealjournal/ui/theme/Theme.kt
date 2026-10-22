package com.my.mealjournal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * 品牌主题入口：固定暖白 + 低饱和绿，不跟随壁纸取色。
 * 深色模式跟随系统，使用品牌深色板；字体与形状统一从这里下发。
 */
@Composable
fun MealJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
