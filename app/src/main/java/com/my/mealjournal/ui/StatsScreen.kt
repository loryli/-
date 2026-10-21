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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun StatsScreen(vm: StatsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("最近 30 天", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("完整打卡", "${state.trackedDaysWithGoal} 天", Modifier.weight(1f))
            StatCard("达标天数", "${state.achievedDays} 天", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("日均热量", "${state.avgKcal.toInt()} kcal", Modifier.weight(1f))
            StatCard("最长连续", "${state.bestStreak} 天", Modifier.weight(1f))
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("热量来源分布", style = MaterialTheme.typography.titleMedium)
                MealType.entries.forEach { meal ->
                    val share = state.mealShare[meal] ?: 0f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(meal.label, Modifier.width(48.dp), style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { share },
                            modifier = Modifier.weight(1f).height(8.dp),
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

        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("当前连续 ${state.currentStreak} 天", style = MaterialTheme.typography.titleMedium)
                Text(
                    "三餐都记上才算完整打卡，加餐只算热量。断一天连击就从头开始。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "有记录 ${state.trackedDays} 天 · 30 天累计 ${state.totalKcal.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
