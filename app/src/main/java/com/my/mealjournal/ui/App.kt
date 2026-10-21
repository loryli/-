package com.my.mealjournal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.my.mealjournal.MealApp

@Composable
fun appContext(): MealApp = LocalContext.current.applicationContext as MealApp

@Composable
inline fun <reified T : ViewModel> appViewModel(crossinline create: (MealApp) -> T): T {
    val app = appContext()
    return viewModel(factory = viewModelFactory { initializer { create(app) } })
}

private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MealJournalApp() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab("今日", Icons.Filled.Today),
        Tab("日历", Icons.Filled.CalendarMonth),
        Tab("统计", Icons.Filled.Insights),
        Tab("食物", Icons.Filled.Restaurant),
        Tab("设置", Icons.Filled.Settings),
    )

    val todayVm: TodayViewModel = appViewModel { TodayViewModel(it.repository, it.settings) }
    val calendarVm: CalendarViewModel = appViewModel { CalendarViewModel(it.repository) }
    val statsVm: StatsViewModel = appViewModel { StatsViewModel(it.repository, it.settings) }
    val foodsVm: FoodsViewModel = appViewModel { FoodsViewModel(it.repository) }
    val settingsVm: SettingsViewModel = appViewModel {
        SettingsViewModel(it.repository, it.settings, it.reminderScheduler)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when (tab) {
                0 -> TodayScreen(todayVm)
                1 -> CalendarScreen(
                    vm = calendarVm,
                    onPickDate = { date ->
                        todayVm.selectDate(date)
                        tab = 0
                    },
                )
                2 -> StatsScreen(statsVm)
                3 -> FoodsScreen(foodsVm)
                else -> SettingsScreen(settingsVm)
            }
        }
    }
}
