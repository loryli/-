package com.my.mealjournal.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {

    private val d = LocalDate.of(2026, 10, 1)

    private fun complete(vararg offsets: Long) =
        offsets.associate { d.plusDays(it) to DayStatus.COMPLETE }

    @Test
    fun `best streak counts consecutive complete days`() {
        val s = Achievements.snapshotOf(complete(0, 1, 2, 5, 6, 9), 6)
        assertEquals(3, s.bestStreak)
    }

    @Test
    fun `best streak ignores partial days`() {
        val statuses = complete(0, 1, 2).toMutableMap()
        statuses[d.plusDays(3)] = DayStatus.PARTIAL
        statuses[d.plusDays(4)] = DayStatus.COMPLETE
        val s = Achievements.snapshotOf(statuses, 5)
        // 0-2 是 3 连击；第 3 天部分不算，第 4 天重新起算，不会拼成 5
        assertEquals(3, s.bestStreak)
        assertEquals(4, s.completeDays)
    }

    @Test
    fun `month grouping picks the best month`() {
        val statuses = complete(0, 1, 2, 3) + // 10 月 4 天
            complete(-31, -30) // 9 月 2 天
        val s = Achievements.snapshotOf(statuses, 6)
        assertEquals(4, s.bestMonthCompleteDays)
    }

    @Test
    fun `first full unlocks with a single complete day`() {
        val states = Achievements.evaluate(Achievements.snapshotOf(complete(0), 1))
        assertTrue(states.first { it.def.id == "first_full" }.unlocked)
        assertFalse(states.first { it.def.id == "streak_7" }.unlocked)
    }

    @Test
    fun `streak badges share best streak progress`() {
        val states = Achievements.evaluate(Achievements.snapshotOf(complete(0, 1, 2, 3, 4, 5), 6))
        val streak7 = states.first { it.def.id == "streak_7" }
        assertFalse(streak7.unlocked)
        assertEquals(6, streak7.progress)
    }

    @Test
    fun `entry count badges unlock by total entries`() {
        val states = Achievements.evaluate(Achievements.snapshotOf(emptyMap(), 100))
        assertTrue(states.first { it.def.id == "entries_100" }.unlocked)
        assertFalse(states.first { it.def.id == "entries_500" }.unlocked)
    }

    @Test
    fun `empty history unlocks nothing`() {
        val states = Achievements.evaluate(Achievements.snapshotOf(emptyMap(), 0))
        assertTrue(states.none { it.unlocked })
    }
}
