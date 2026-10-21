package com.my.mealjournal.domain

import java.time.LocalDate

/** 一条记录的评分快照，只保留判定需要的字段。 */
data class DayEntry(val meal: MealType, val kcal: Double)

object DayEvaluator {

    /** 三餐各至少一条 = COMPLETE；有任意记录 = PARTIAL；全无 = MISSING。 */
    fun statusOf(entries: List<DayEntry>): DayStatus {
        if (entries.isEmpty()) return DayStatus.MISSING
        val mealsWithEntry = entries.map { it.meal }.toSet()
        val allMainMeals = MealType.entries.filter { it.countsForCheckIn }.toSet()
        return if (mealsWithEntry.containsAll(allMainMeals)) DayStatus.COMPLETE else DayStatus.PARTIAL
    }

    fun totalKcal(entries: List<DayEntry>): Double = entries.sumOf { it.kcal }
}

object StreakCalculator {

    /**
     * 连续完整打卡天数。
     * 今天已完整则从今天算起；今天未完整则从昨天算起。中间断一天即中断。
     */
    fun currentStreak(statuses: Map<LocalDate, DayStatus>, today: LocalDate): Int {
        var cursor = if (statuses[today] == DayStatus.COMPLETE) today else today.minusDays(1)
        var streak = 0
        while (statuses[cursor] == DayStatus.COMPLETE) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}

object EditWindow {

    /** 可编辑天数（含今天），超过这个窗口的历史只读。 */
    const val EDITABLE_DAYS = 7

    fun isEditable(date: LocalDate, today: LocalDate): Boolean =
        !date.isAfter(today) && date.isAfter(today.minusDays(EDITABLE_DAYS.toLong()))
}
