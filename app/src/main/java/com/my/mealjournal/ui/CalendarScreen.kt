package com.my.mealjournal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.domain.DayStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
fun CalendarScreen(vm: CalendarViewModel, onPickDate: (LocalDate) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val month = state.month
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value + 6) % 7
    val daysInMonth = month.lengthOfMonth()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.shiftMonth(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
            }
            Text(
                text = month.label(),
                style = MaterialTheme.typography.titleLarge,
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

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                modifier = Modifier.weight(1f),
                                onClick = { onPickDate(date) },
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = "点任意日期回看当天记录；只有最近 7 天可以补记或修改。",
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val background = when (status) {
        DayStatus.COMPLETE -> MaterialTheme.colorScheme.primaryContainer
        DayStatus.PARTIAL -> MaterialTheme.colorScheme.secondaryContainer
        DayStatus.MISSING -> MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = when (status) {
        DayStatus.COMPLETE -> MaterialTheme.colorScheme.onPrimaryContainer
        DayStatus.PARTIAL -> MaterialTheme.colorScheme.onSecondaryContainer
        DayStatus.MISSING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val isToday = date == LocalDate.now()
    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small,
            )
            .background(background, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = foreground,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
            if (kcal > 0) {
                Text(
                    text = kcal.toInt().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(MaterialTheme.colorScheme.primaryContainer, "三餐记满")
        LegendDot(MaterialTheme.colorScheme.secondaryContainer, "记了一部分")
        LegendDot(MaterialTheme.colorScheme.surfaceVariant, "没记录")
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

fun daysBetween(from: YearMonth, to: YearMonth): Long = ChronoUnit.MONTHS.between(from, to)
