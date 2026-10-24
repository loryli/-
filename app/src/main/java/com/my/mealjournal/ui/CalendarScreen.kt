package com.my.mealjournal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.domain.DayStatus
import com.my.mealjournal.domain.MealType
import com.my.mealjournal.ui.design.AppButton
import com.my.mealjournal.ui.design.AppButtonStyle
import com.my.mealjournal.ui.design.MetricTile
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import com.my.mealjournal.ui.design.statusStyle
import java.time.LocalDate

@Composable
fun CalendarScreen(vm: CalendarViewModel, onPickDate: (LocalDate) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val month = state.month
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page)
            .padding(top = Spacing.md, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                text = month.label(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = { vm.shiftMonth(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
            }
        }

        Row {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        var drag by remember { mutableStateOf(0f) }
        Column(
            modifier = Modifier.pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            drag > 80f -> vm.shiftMonth(-1)
                            drag < -80f -> vm.shiftMonth(1)
                        }
                        drag = 0f
                    },
                    onHorizontalDrag = { _, amount -> drag += amount },
                )
            },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            var dayOfMonth = 1
            var week = 0
            while (dayOfMonth <= daysInMonth) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (column in 0..6) {
                        val cellDay = if (week == 0 && column < leading) null else dayOfMonth
                        if (cellDay != null && cellDay <= daysInMonth) {
                            val date = month.atDay(cellDay)
                            DayCell(
                                date = date,
                                status = state.statusOn(date),
                                kcal = state.kcalOn(date),
                                selected = state.selected == date,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.select(date) },
                            )
                            dayOfMonth += 1
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                week += 1
            }
        }

        Legend()

        state.selected?.let { date -> DayDetailCard(state, date, onGo = { onPickDate(date) }) }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("记录天数", "${state.recordedDays} 天", Modifier.weight(1f))
            MetricTile("记满三餐", "${state.completeDays} 天", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("达标天数", "${state.achievedDays} 天", Modifier.weight(1f))
            MetricTile("日均热量", "${state.avgKcal.toInt()} kcal", Modifier.weight(1f))
        }

        Text(
            text = "点日期看当天速览，「去这天」回看并补记；只有最近 7 天可以修改。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus,
    kcal: Double,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val style = status.statusStyle()
    val isToday = date == LocalDate.now()
    // 小格用 12dp 圆角：卡片档的 16dp 在这个尺寸会过圆
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .border(
                width = if (selected || isToday) 2.dp else 0.dp,
                color = if (selected || isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small,
            )
            .background(style.container, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = style.content,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (kcal > 0) {
                Text(
                    text = kcal.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = style.content.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** 选中日期的速览：状态、热量进度、四餐分布；「去这天」跳今日页补记。 */
@Composable
private fun DayDetailCard(state: CalendarUiState, date: LocalDate, onGo: () -> Unit) {
    val style = state.statusOn(date).statusStyle()
    val kcal = state.kcalOn(date)
    val goal = state.goalKcal
    val pct = if (goal <= 0) 0f else (kcal / goal).toFloat().coerceIn(0f, 1f)
    val over = kcal > goal

    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = date.labelFull() + if (date == LocalDate.now()) " · 今天" else "",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "已摄入 ${kcal.toInt()} / $goal kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = style.container, contentColor = style.content) {
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(pct)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
            )
        }

        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MealType.entries.forEach { meal ->
                val value = state.mealsOn(date)[meal] ?: 0.0
                Column(
                    modifier = Modifier.weight(1f).background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small,
                    ).padding(vertical = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = meal.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (value > 0) "${value.toInt()}" else "—",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (value > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (value > 0) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xs))
        AppButton(text = "去这天", onClick = onGo, style = AppButtonStyle.Text, modifier = Modifier.align(Alignment.End))
    }
}

/** 图例与今日页状态胶囊共用一套三态色。 */
@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        DayStatus.entries.forEach { status ->
            val style = status.statusStyle()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(style.container, CircleShape))
                Spacer(Modifier.width(Spacing.xs))
                Text(style.label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
