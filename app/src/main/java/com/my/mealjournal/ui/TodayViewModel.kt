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

    private val query = MutableStateFlow("")
    val foods: StateFlow<List<FoodEntity>> = query
        .flatMapLatest { q -> repo.foods(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(text: String) {
        query.value = text
    }

    fun addCustom(name: String, kcalPer100g: Double, category: String) {
        viewModelScope.launch { repo.addCustomFood(name, kcalPer100g, category) }
    }
}
