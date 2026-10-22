package com.my.mealjournal.domain

import com.my.mealjournal.notify.ReminderScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** 提醒时刻换算：minuteOfDay 是「分钟数」，不是秒数。 */
class ReminderScheduleTest {

    private fun local(epochMillis: Long): LocalDateTime =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()

    @Test
    fun `20点对应当天20点而不是00点20分`() {
        val now = LocalDateTime.of(2026, 10, 22, 12, 0)
        val trigger = local(ReminderScheduler.nextTrigger(20 * 60, now))
        assertEquals(LocalDateTime.of(2026, 10, 22, 20, 0), trigger)
    }

    @Test
    fun `过了当天提醒时间就排到明天`() {
        val now = LocalDateTime.of(2026, 10, 22, 21, 0)
        val trigger = local(ReminderScheduler.nextTrigger(20 * 60, now))
        assertEquals(LocalDateTime.of(2026, 10, 23, 20, 0), trigger)
    }

    @Test
    fun `6点33分按分钟换算`() {
        val now = LocalDateTime.of(2026, 10, 22, 6, 31)
        val trigger = local(ReminderScheduler.nextTrigger(6 * 60 + 33, now))
        assertEquals(LocalDateTime.of(2026, 10, 22, 6, 33), trigger)
    }
}
