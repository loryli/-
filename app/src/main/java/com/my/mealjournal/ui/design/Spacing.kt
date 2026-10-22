package com.my.mealjournal.ui.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * 间距刻度：4 的倍数，页面左右统一 20dp，卡片内 16dp。
 * 规范见 docs/ui-redesign.md §2。
 */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp

    val page: Dp = 20.dp
    val card: Dp = 16.dp
    val rowMin: Dp = 56.dp
    val buttonHeight: Dp = 48.dp
    val border: Dp = 1.dp
}
