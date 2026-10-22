package com.my.mealjournal.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.my.mealjournal.ui.design.SurfaceCard

/**
 * 旧的卡片入口，保留签名以免页面改动；实现统一走 [SurfaceCard]。
 * 页面自己的 16dp 内边距仍在外部，迁移到 SurfaceCard(padding) 后即可去掉。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    SurfaceCard(modifier = modifier, padding = 0.dp, content = content)
}
