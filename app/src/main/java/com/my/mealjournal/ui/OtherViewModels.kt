package com.my.mealjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.data.MealRepository
import com.my.mealjournal.data.SettingsStore
import com.my.mealjournal.domain.AchievementState
import com.my.mealjournal.domain.Achievements
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
import kotlinx.coroutines.flow.asStateFlow
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
    val thisWeekDays: Int = 0,
    val lastWeekDays: Int = 0,
    val achievements: List<AchievementState> = emptyList(),
) {
    val mealShare: Map<MealType, Float>
        get() = if (totalKcal <= 0) MealType.entries.associateWith { 0f }
        else MealType.entries.associateWith { ((mealKcal[it] ?: 0.0) / totalKcal).toFloat() }
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(repo: MealRepository, settings: SettingsStore) : ViewModel() {

    /** 全量数据一个源：近 30 天指标、本周小结、成就都从这里派生。 */
    val state: StateFlow<StatsUiState> = combine(
        repo.allEntries(),
        settings.goalKcal,
    ) { entries, goal ->
        val today = LocalDate.now()
        val recent = entries.filter { !it.date.isBefore(today.minusDays(29)) }
        val byDate = recent.groupBy { it.date }
        val statuses = byDate.mapValues { (_, list) -> DayEvaluator.statusOf(list.map { DayEntry(it.meal, it.kcal) }) }
        val mealKcal = MealType.entries.associateWith { meal ->
            recent.filter { it.meal == meal }.sumOf { it.kcal }
        }
        val totals = byDate.mapValues { (_, list) -> DayEvaluator.totalKcal(list.map { DayEntry(it.meal, it.kcal) }) }
        val macro = recent.fold(MacroGrams(0.0, 0.0, 0.0)) { acc, e ->
            acc + MacroGrams(e.protein, e.carbs, e.fat)
        }

        // 本周小结：周一起算，本周只数到今天
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)
        val allStatuses = entries.groupBy { it.date }
            .mapValues { (_, list) -> DayEvaluator.statusOf(list.map { DayEntry(it.meal, it.kcal) }) }
        val thisWeekDays = allStatuses.count { (day, s) -> s != DayStatus.MISSING && !day.isBefore(weekStart) && !day.isAfter(today) }
        val lastWeekDays = allStatuses.count { (day, s) -> s != DayStatus.MISSING && day.isBefore(weekStart) && !day.isBefore(weekStart.minusWeeks(1)) }

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
            thisWeekDays = thisWeekDays,
            lastWeekDays = lastWeekDays,
            achievements = Achievements.evaluate(Achievements.snapshotOf(allStatuses, entries.size)),
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

    /** 「自定义」作为虚拟分类出现在 chips 里，只列自建食物。 */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    /** 全库浏览：搜索 + 分类过滤，内置在前自建在后。 */
    val foods: StateFlow<List<FoodEntity>> = combine(repo.foods(""), _query, _category) { all, q, c ->
        val byQuery = if (q.isBlank()) all else all.filter { it.name.contains(q) }
        when (c) {
            null -> byQuery
            CUSTOM_CATEGORY -> byQuery.filter { !it.isBuiltin }
            else -> byQuery.filter { it.category == c }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<String>> = repo.foods("").map { list ->
        val present = FoodPickerViewModel.CATEGORY_ORDER.filter { cat -> list.any { it.category == cat } }
        if (list.any { !it.isBuiltin }) present + CUSTOM_CATEGORY else present
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 页面副标题：内置/自建各多少种。 */
    val summary: StateFlow<Pair<Int, Int>> = repo.foods("").map { list ->
        list.count { it.isBuiltin } to list.count { !it.isBuiltin }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    fun search(text: String) {
        _query.value = text
    }

    fun selectCategory(value: String?) {
        _category.value = value
    }

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

    companion object {
        const val CUSTOM_CATEGORY = "自定义"
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
