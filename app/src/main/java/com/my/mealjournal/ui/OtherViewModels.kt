package com.my.mealjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.data.MealRepository
import com.my.mealjournal.data.SettingsStore
import com.my.mealjournal.domain.DayEntry
import com.my.mealjournal.domain.DayEvaluator
import com.my.mealjournal.domain.DayStatus
import com.my.mealjournal.domain.MacroGrams
import com.my.mealjournal.domain.MealType
import com.my.mealjournal.domain.Nutrition
import com.my.mealjournal.domain.StreakCalculator
import com.my.mealjournal.notify.ReminderScheduler
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val month: YearMonth,
    val days: Map<LocalDate, DayStatus>,
    val totalKcal: Map<LocalDate, Double>,
    val mealKcal: Map<LocalDate, Map<MealType, Double>> = emptyMap(),
    val goalKcal: Int = SettingsStore.DEFAULT_GOAL,
    val selected: LocalDate? = null,
) {
    fun kcalOn(date: LocalDate): Double = totalKcal[date] ?: 0.0
    fun statusOn(date: LocalDate): DayStatus = days[date] ?: DayStatus.MISSING
    fun mealsOn(date: LocalDate): Map<MealType, Double> = mealKcal[date] ?: emptyMap()

    /** 月概览：只统计有记录的天。 */
    val recordedDays: Int get() = days.values.count { it != DayStatus.MISSING }
    val completeDays: Int get() = days.values.count { it == DayStatus.COMPLETE }
    val achievedDays: Int get() = totalKcal.values.count { it > 0 && it <= goalKcal }
    val avgKcal: Double get() = totalKcal.values.filter { it > 0 }.ifEmpty { null }?.average() ?: 0.0
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repo: MealRepository,
    settings: SettingsStore,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selected = MutableStateFlow<LocalDate?>(LocalDate.now())

    val state: StateFlow<CalendarUiState> = combine(
        month.flatMapLatest { m -> repo.entriesBetween(m.atDay(1), m.atEndOfMonth()).map { m to it } },
        settings.goalKcal,
        selected,
    ) { (m, entries), goal, picked ->
        val byDate = entries.groupBy { it.date }
        CalendarUiState(
            month = m,
            days = byDate.mapValues { (_, list) -> DayEvaluator.statusOf(list.map { DayEntry(it.meal, it.kcal) }) },
            totalKcal = byDate.mapValues { (_, list) -> DayEvaluator.totalKcal(list.map { DayEntry(it.meal, it.kcal) }) },
            mealKcal = byDate.mapValues { (_, list) ->
                MealType.entries.associateWith { meal -> list.filter { it.meal == meal }.sumOf { it.kcal } }
            },
            goalKcal = goal,
            selected = picked,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState(YearMonth.now(), emptyMap(), emptyMap()))

    fun shiftMonth(delta: Long) {
        month.value = month.value.plusMonths(delta)
        selected.value = null
    }

    fun select(date: LocalDate) {
        selected.value = date
    }

    fun reset() {
        month.value = YearMonth.now()
    }
}

data class StatsUiState(
    val trackedDays: Int,
    val trackedDaysWithGoal: Int,
    val achievedDays: Int,
    val avgKcal: Double,
    val bestStreak: Int,
    val currentStreak: Int,
    val mealKcal: Map<MealType, Double>,
    val totalKcal: Double,
    val macroShare: MacroGrams = MacroGrams(0.0, 0.0, 0.0),
) {
    val mealShare: Map<MealType, Float>
        get() = if (totalKcal <= 0) MealType.entries.associateWith { 0f }
        else MealType.entries.associateWith { ((mealKcal[it] ?: 0.0) / totalKcal).toFloat() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(repo: MealRepository, settings: SettingsStore) : ViewModel() {

    val state: StateFlow<StatsUiState> = combine(
        repo.entriesBetween(LocalDate.now().minusDays(29), LocalDate.now()),
        settings.goalKcal,
    ) { entries, goal ->
        val today = LocalDate.now()
        val byDate = entries.groupBy { it.date }
        val statuses = byDate.mapValues { (_, list) -> DayEvaluator.statusOf(list.map { DayEntry(it.meal, it.kcal) }) }
        val mealKcal = MealType.entries.associateWith { meal ->
            entries.filter { it.meal == meal }.sumOf { it.kcal }
        }
        val totals = byDate.mapValues { (_, list) -> DayEvaluator.totalKcal(list.map { DayEntry(it.meal, it.kcal) }) }
        val macro = entries.fold(MacroGrams(0.0, 0.0, 0.0)) { acc, e ->
            acc + MacroGrams(e.protein, e.carbs, e.fat)
        }
        StatsUiState(
            trackedDays = statuses.count { it.value != DayStatus.MISSING },
            trackedDaysWithGoal = statuses.count { it.value == DayStatus.COMPLETE },
            achievedDays = totals.values.count { it > 0 && it <= goal },
            avgKcal = if (totals.isEmpty()) 0.0 else totals.values.average(),
            bestStreak = longestStreak(statuses),
            currentStreak = StreakCalculator.currentStreak(statuses, today),
            mealKcal = mealKcal,
            totalKcal = totals.values.sum(),
            macroShare = Nutrition.shares(macro),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState(0, 0, 0, 0.0, 0, 0, MealType.entries.associateWith { 0.0 }, 0.0),
    )

    private fun longestStreak(statuses: Map<LocalDate, DayStatus>): Int {
        var best = 0
        var run = 0
        val days = statuses.keys.sorted()
        var previous: LocalDate? = null
        for (day in days) {
            val complete = statuses[day] == DayStatus.COMPLETE
            if (complete && (previous == null || previous == day.minusDays(1))) {
                run += 1
            } else {
                run = if (complete) 1 else 0
            }
            previous = day
            best = maxOf(best, run)
        }
        return best
    }
}

class FoodsViewModel(private val repo: MealRepository) : ViewModel() {

    val foods: StateFlow<List<FoodEntity>> = repo.customFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(
        name: String,
        kcalPer100g: Double,
        category: String,
        unitLabel: String?,
        unitGrams: Double?,
        proteinPer100g: Double? = null,
        carbsPer100g: Double? = null,
        fatPer100g: Double? = null,
    ) {
        viewModelScope.launch {
            repo.addCustomFood(name, kcalPer100g, category, unitLabel, unitGrams, proteinPer100g, carbsPer100g, fatPer100g)
        }
    }

    private var lastDeleted: FoodEntity? = null

    fun delete(food: FoodEntity) {
        viewModelScope.launch {
            repo.deleteCustomFood(food)
            lastDeleted = food
        }
    }

    fun undoDelete() {
        val food = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repo.restoreFood(food) }
    }
}

data class SettingsUiState(
    val goalKcal: Int = SettingsStore.DEFAULT_GOAL,
    val reminderEnabled: Boolean = true,
    val reminderMinute: Int = SettingsStore.DEFAULT_REMINDER_MINUTE,
)

class SettingsViewModel(
    private val repo: MealRepository,
    private val settings: SettingsStore,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settings.goalKcal,
        settings.reminderEnabled,
        settings.reminderMinute,
    ) { goal, enabled, minute ->
        SettingsUiState(goal, enabled, minute)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SettingsUiState(),
    )

    fun setGoal(kcal: Int) = viewModelScope.launch { settings.setGoalKcal(kcal) }

    fun setReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setReminderEnabled(enabled)
        scheduler.apply(enabled, state.value.reminderMinute)
    }

    fun setReminderMinute(minute: Int) = viewModelScope.launch {
        settings.setReminderMinute(minute)
        scheduler.apply(true, minute)
    }

    suspend fun buildCsv(): String = repo.exportCsv()
}
