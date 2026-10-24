package com.my.mealjournal.domain

import java.time.LocalDate
import java.time.YearMonth

/**
 * 成就徽章：全部从本地记录推导，可随时重算、不落库。
 * 解锁判定看历史极值（最佳连续、累计条数、是否记满过），重复计算结果稳定。
 */
data class BadgeDef(
    val id: String,
    val emoji: String,
    val title: String,
    val desc: String,
    val goal: Int,
)

data class AchievementState(
    val def: BadgeDef,
    val progress: Int,
) {
    val unlocked: Boolean get() = progress >= def.goal
}

/** 从全量条目能算出的成就原料。 */
data class AchievementSnapshot(
    val bestStreak: Int,
    val totalEntries: Int,
    val completeDays: Int,
    val bestMonthCompleteDays: Int,
)

object Achievements {

    val FIRST_FULL = BadgeDef("first_full", "⭐", "初次点亮", "首次记满三餐", 1)
    val STREAK_7 = BadgeDef("streak_7", "🔥", "七日连击", "连续打卡 7 天", 7)
    val STREAK_30 = BadgeDef("streak_30", "🔥", "三十而立", "连续打卡 30 天", 30)
    val STREAK_100 = BadgeDef("streak_100", "🔥", "百日坚持", "连续打卡 100 天", 100)
    val STREAK_365 = BadgeDef("streak_365", "🏆", "全年不断", "连续打卡 365 天", 365)
    val ENTRIES_100 = BadgeDef("entries_100", "📝", "百餐记录", "累计记录 100 条", 100)
    val ENTRIES_500 = BadgeDef("entries_500", "📝", "五百里程碑", "累计记录 500 条", 500)
    val ENTRIES_1000 = BadgeDef("entries_1000", "📖", "千餐日记", "累计记录 1000 条", 1000)
    val MONTH_20 = BadgeDef("month_20", "🗓️", "满月守护", "单月记满 20 天", 20)

    val all: List<BadgeDef> = listOf(
        FIRST_FULL, STREAK_7, STREAK_30, STREAK_100, STREAK_365,
        ENTRIES_100, ENTRIES_500, ENTRIES_1000, MONTH_20,
    )

    fun evaluate(s: AchievementSnapshot): List<AchievementState> = listOf(
        AchievementState(FIRST_FULL, minOf(s.completeDays, 1)),
        AchievementState(STREAK_7, s.bestStreak),
        AchievementState(STREAK_30, s.bestStreak),
        AchievementState(STREAK_100, s.bestStreak),
        AchievementState(STREAK_365, s.bestStreak),
        AchievementState(ENTRIES_100, s.totalEntries),
        AchievementState(ENTRIES_500, s.totalEntries),
        AchievementState(ENTRIES_1000, s.totalEntries),
        AchievementState(MONTH_20, s.bestMonthCompleteDays),
    )

    /** statuses：日期 → 当日状态；totalEntries：条目总数（含加餐）。 */
    fun snapshotOf(statuses: Map<LocalDate, DayStatus>, totalEntries: Int): AchievementSnapshot {
        val completeDates = statuses.filterValues { it == DayStatus.COMPLETE }.keys.sorted()
        return AchievementSnapshot(
            bestStreak = bestStreak(completeDates),
            totalEntries = totalEntries,
            completeDays = completeDates.size,
            bestMonthCompleteDays = completeDates
                .groupingBy { YearMonth.from(it) }
                .eachCount()
                .values
                .maxOrNull() ?: 0,
        )
    }

    /** 全时段最佳连续：排序后数相邻日期的连击段。 */
    private fun bestStreak(sortedCompleteDates: List<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in sortedCompleteDates) {
            run = if (previous == null || previous == day.minusDays(1)) run + 1 else 1
            previous = day
            best = maxOf(best, run)
        }
        return best
    }
}
