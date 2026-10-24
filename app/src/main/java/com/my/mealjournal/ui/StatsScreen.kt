package com.my.mealjournal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.domain.MealType
import com.my.mealjournal.ui.design.MetricTile
import com.my.mealjournal.ui.design.SectionHeader
import com.my.mealjournal.ui.design.Spacing
import com.my.mealjournal.ui.design.SurfaceCard
import com.my.mealjournal.ui.theme.nutrientColors

@Composable
fun StatsScreen(vm: StatsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.page)
            .padding(top = Spacing.md, bottom = Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text("最近 30 天", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("完整打卡", "${state.trackedDaysWithGoal} 天", Modifier.weight(1f))
            MetricTile("达标天数", "${state.achievedDays} 天", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("日均热量", "${state.avgKcal.toInt()} kcal", Modifier.weight(1f))
            MetricTile("最长连续", "${state.bestStreak} 天", Modifier.weight(1f))
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader("热量来源分布")
                MealType.entries.forEach { meal ->
                    val share = state.mealShare[meal] ?: 0f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(meal.label, Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { share },
                            modifier = Modifier.weight(1f).height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "${(share * 100).toInt()}%",
                            modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SectionHeader("营养素构成", subtitle = "三大营养素供能占比 · 参考值")
                val nutrients = nutrientColors()
                listOf(
                    Triple("碳水", state.macroShare.carbs, nutrients.carbs),
                    Triple("蛋白质", state.macroShare.protein, nutrients.protein),
                    Triple("脂肪", state.macroShare.fat, nutrients.fat),
                ).forEach { (label, share, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, Modifier.width(52.dp), style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { share.toFloat() },
                            modifier = Modifier.weight(1f).height(8.dp),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                        Text(
                            text = "${(share * 100).toInt()}%",
                            modifier = Modifier.width(48.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SectionHeader(
                    title = "当前连续 ${state.currentStreak} 天",
                    subtitle = "三餐都记上才算完整打卡，加餐只算热量。断一天连击就从头开始。",
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "有记录 ${state.trackedDays} 天 · 30 天累计 ${state.totalKcal.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
