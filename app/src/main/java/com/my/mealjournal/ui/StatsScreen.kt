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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.my.mealjournal.domain.AchievementState
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

        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SectionHeader("本周小结", subtitle = "周一到现在")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "已记录 ${state.thisWeekDays} 天",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    val delta = state.thisWeekDays - state.lastWeekDays
                    val text = when {
                        state.lastWeekDays == 0 && state.thisWeekDays > 0 -> "上周没有记录，这周动起来了"
                        delta > 0 -> "比上周多 $delta 天"
                        delta < 0 -> "比上周少 ${-delta} 天"
                        else -> "与上周持平"
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (delta > 0 || (state.lastWeekDays == 0 && state.thisWeekDays > 0)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("当前连续", "${state.currentStreak} 天", Modifier.weight(1f))
            MetricTile("完整打卡", "${state.trackedDaysWithGoal} 天", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricTile("达标天数", "${state.achievedDays} 天", Modifier.weight(1f))
            MetricTile("日均热量", "${state.avgKcal.toInt()} kcal", Modifier.weight(1f))
        }
        Text(
            text = "三餐各记一条才算完整打卡，加餐只算热量；断一天连续就从头开始。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.xs),
        )

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

        SectionHeader(
            title = "成就",
            subtitle = "坚持的证物 · 全部本地记录推导",
            modifier = Modifier.padding(top = Spacing.xs),
        )
        state.achievements.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                row.forEach { badge -> BadgeCard(badge, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: AchievementState, modifier: Modifier = Modifier) {
    SurfaceCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = badge.def.emoji,
                fontSize = 24.sp,
                modifier = Modifier.alpha(if (badge.unlocked) 1f else 0.35f),
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(badge.def.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = badge.def.desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (badge.unlocked) "已解锁" else "${badge.progress}/${badge.def.goal}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (badge.unlocked) FontWeight.SemiBold else FontWeight.Normal,
                color = if (badge.unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
