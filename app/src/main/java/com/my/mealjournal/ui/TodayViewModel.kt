package com.my.mealjournal.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.mealjournal.data.EntryView
import com.my.mealjournal.data.FoodEntity
import com.my.mealjournal.data.MealRepository
import com.my.mealjournal.data.SettingsStore
import com.my.mealjournal.domain.DayEntry
import com.my.mealjournal.domain.DayEvaluator
import com.my.mealjournal.domain.DayStatus
import com.my.mealjournal.domain.EditWindow
import com.my.mealjournal.domain.MealType
import com.my.mealjournal.domain.StreakCalculator
import java.time.LocalDate
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

data class TodayUiState(
    val date: LocalDate,
    val entriesByMeal: Map<MealType, List<EntryView>>,
    val totalKcal: Double,
    val goalKcal: Int,
    val status: DayStatus,
    val streak: Int,
    val editable: Boolean,
) {
    val remainingKcal: Double get() = goalKcal - totalKcal
    val progress: Float get() = if (goalKcal <= 0) 0f else (totalKcal / goalKcal).toFloat().coerceIn(0f, 1f)

    companion object {
        fun empty(date: LocalDate, goal: Int = SettingsStore.DEFAULT_GOAL) = TodayUiState(
            date = date,
            entriesByMeal = MealType.entries.associateWith { emptyList() },
            totalKcal = 0.0,
            goalKcal = goal,
            status = DayStatus.MISSING,
            streak = 0,
            editable = true,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: MealRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    val state: StateFlow<TodayUiState> = combine(
        _date.flatMapLatest { day -> repo.entriesFor(day) },
        _date.flatMapLatest { day -> repo.entriesBetween(day.minusDays(60), day) },
        settings.goalKcal,
        _date,
    ) { todayEntries, windowEntries, goal, day ->
        val byMeal = MealType.entries.associateWith { meal -> todayEntries.filter { it.meal == meal } }
        val status = DayEvaluator.statusOf(todayEntries.map { DayEntry(it.meal, it.kcal) })
        val statuses: Map<LocalDate, DayStatus> = windowEntries
            .groupBy { it.date }
            .mapValues { (_, list) -> DayEvaluator.statusOf(list.map { DayEntry(it.meal, it.kcal) }) }
        TodayUiState(
            date = day,
            entriesByMeal = byMeal,
            totalKcal = DayEvaluator.totalKcal(todayEntries.map { DayEntry(it.meal, it.kcal) }),
            goalKcal = goal,
            status = status,
            streak = StreakCalculator.currentStreak(statuses, day),
            editable = EditWindow.isEditable(day, LocalDate.now()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.empty(LocalDate.now()))

    fun selectDate(day: LocalDate) {
        _date.value = day
    }

    fun addEntry(food: FoodEntity, grams: Double, meal: MealType) {
        viewModelScope.launch { repo.addEntry(_date.value, meal, food, grams) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repo.deleteEntry(id) }
    }

    fun updateGrams(id: Long, grams: Double) {
        viewModelScope.launch { repo.updateEntryGrams(id, grams) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FoodPickerViewModel(private val repo: MealRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** null = 全部分类。 */
    private val _category = MutableStateFlow<String?>(null)
    val category: StateFlow<String?> = _category.asStateFlow()

    private val _recent = MutableStateFlow<List<FoodEntity>>(emptyList())
    val recent: StateFlow<List<FoodEntity>> = _recent.asStateFlow()

    /** 内置食物里出现过的分类，保持固定展示顺序。 */
    val categories: StateFlow<List<String>> = repo.foods("")
        .map { list -> CATEGORY_ORDER.filter { category -> list.any { it.category == category } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val foods: StateFlow<List<FoodEntity>> = combine(_query, _category) { q, c -> q to c }
        .flatMapLatest { (q, c) ->
            repo.foods(q).map { list -> if (c == null) list else list.filter { it.category == c } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { _recent.value = repo.recentFoods() }
    }

    fun search(text: String) {
        _query.value = text
    }

    fun selectCategory(value: String?) {
        _category.value = value
    }

    fun clearFilters() {
        _query.value = ""
        _category.value = null
    }

    fun clearQuery() = clearFilters()

    fun addCustom(name: String, kcalPer100g: Double, category: String, unitLabel: String?, unitGrams: Double?) {
        viewModelScope.launch {
            repo.addCustomFood(name, kcalPer100g, category, unitLabel, unitGrams)
            _recent.value = repo.recentFoods()
        }
    }

    companion object {
        val CATEGORY_ORDER = listOf("主食", "肉蛋", "蔬菜", "水果", "奶豆", "零食", "饮品", "调味")
    }
}
