package com.my.mealjournal.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.my.mealjournal.domain.DayStatus

/** 打卡三态的配色与文案：今日页胶囊、日历格子、日历图例共用这一套。 */
data class DayStatusStyle(
    val container: Color,
    val content: Color,
    val label: String,
)

@Composable
fun DayStatus.statusStyle(): DayStatusStyle = when (this) {
    DayStatus.COMPLETE -> DayStatusStyle(
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
        label = "记满",
    )
    DayStatus.PARTIAL -> DayStatusStyle(
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
        label = "部分",
    )
    DayStatus.MISSING -> DayStatusStyle(
        container = MaterialTheme.colorScheme.surfaceVariant,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        label = "未记录",
    )
}
