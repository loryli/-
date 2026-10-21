package com.my.mealjournal.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieMathTest {

    @Test
    fun `150g 米饭 116kcal per 100g 得到 174`() {
        assertEquals(174.0, CalorieMath.kcal(116.0, 150.0), 0.001)
    }

    @Test
    fun `0 克得到 0 热量`() {
        assertEquals(0.0, CalorieMath.kcal(300.0, 0.0), 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `负克数直接报错`() {
        CalorieMath.kcal(100.0, -1.0)
    }
}

class MealTimingTest {

    @Test
    fun `五个边界时刻归到正确的餐`() {
        assertEquals(MealType.BREAKFAST, MealTiming.defaultMeal(LocalTime.of(5, 0)))
        assertEquals(MealType.LUNCH, MealTiming.defaultMeal(LocalTime.of(10, 0)))
        assertEquals(MealType.DINNER, MealTiming.defaultMeal(LocalTime.of(15, 0)))
        assertEquals(MealType.SNACK, MealTiming.defaultMeal(LocalTime.of(21, 0)))
        assertEquals(MealType.SNACK, MealTiming.defaultMeal(LocalTime.of(4, 59)))
    }

    @Test
    fun `早餐时段内任意时刻都是早餐`() {
        assertEquals(MealType.BREAKFAST, MealTiming.defaultMeal(LocalTime.of(8, 30)))
    }
}

class DayEvaluatorTest {

    @Test
    fun `三餐都有条目是完整`() {
        val entries = listOf(
            DayEntry(MealType.BREAKFAST, 300.0),
            DayEntry(MealType.LUNCH, 600.0),
            DayEntry(MealType.DINNER, 500.0),
        )
        assertEquals(DayStatus.COMPLETE, DayEvaluator.statusOf(entries))
    }

    @Test
    fun `只记了午饭是部分`() {
        assertEquals(DayStatus.PARTIAL, DayEvaluator.statusOf(listOf(DayEntry(MealType.LUNCH, 600.0))))
    }

    @Test
    fun `只有加餐不算完整`() {
        assertEquals(DayStatus.PARTIAL, DayEvaluator.statusOf(listOf(DayEntry(MealType.SNACK, 200.0))))
    }

    @Test
    fun `没有记录是缺失`() {
        assertEquals(DayStatus.MISSING, DayEvaluator.statusOf(emptyList()))
    }

    @Test
    fun `总热量把加餐也算进去`() {
        val entries = listOf(
            DayEntry(MealType.BREAKFAST, 300.0),
            DayEntry(MealType.SNACK, 120.5),
        )
        assertEquals(420.5, DayEvaluator.totalKcal(entries), 0.001)
    }
}

class StreakCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 21)

    @Test
    fun `今天和昨天都完整返回 2`() {
        val map = mapOf(today to DayStatus.COMPLETE, today.minusDays(1) to DayStatus.COMPLETE)
        assertEquals(2, StreakCalculator.currentStreak(map, today))
    }

    @Test
    fun `今天没记但昨天完整时从昨天开始数`() {
        val map = mapOf(
            today.minusDays(1) to DayStatus.COMPLETE,
            today.minusDays(2) to DayStatus.COMPLETE,
        )
        assertEquals(2, StreakCalculator.currentStreak(map, today))
    }

    @Test
    fun `中间断一天即中断`() {
        val map = mapOf(
            today to DayStatus.COMPLETE,
            today.minusDays(1) to DayStatus.COMPLETE,
            today.minusDays(2) to DayStatus.MISSING,
            today.minusDays(3) to DayStatus.COMPLETE,
        )
        assertEquals(2, StreakCalculator.currentStreak(map, today))
    }

    @Test
    fun `今天只有部分记录不计入连续`() {
        val map = mapOf(
            today to DayStatus.PARTIAL,
            today.minusDays(1) to DayStatus.COMPLETE,
        )
        assertEquals(1, StreakCalculator.currentStreak(map, today))
    }

    @Test
    fun `完全没有记录返回 0`() {
        assertEquals(0, StreakCalculator.currentStreak(emptyMap(), today))
    }
}

class EditWindowTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 21)

    @Test
    fun `今天和六天前可编辑`() {
        assertTrue(EditWindow.isEditable(today, today))
        assertTrue(EditWindow.isEditable(today.minusDays(6), today))
    }

    @Test
    fun `七天前已只读`() {
        assertFalse(EditWindow.isEditable(today.minusDays(7), today))
    }

    @Test
    fun `未来日期不可编辑`() {
        assertFalse(EditWindow.isEditable(today.plusDays(1), today))
    }
}

class CsvExporterTest {

    private fun row(name: String, grams: Double = 150.0, kcal: Double = 174.0) = CsvExporter.Row(
        date = "2026-10-21",
        meal = MealType.LUNCH,
        foodName = name,
        grams = grams,
        kcalPer100g = 116.0,
        kcal = kcal,
    )

    @Test
    fun `输出包含表头且行数与记录数一致`() {
        val csv = CsvExporter.toCsv(listOf(row("米饭"), row("鸡胸肉")))
        val lines = csv.trim().lines()
        assertEquals(CsvExporter.HEADER, lines[0])
        assertEquals(3, lines.size)
        assertEquals("2026-10-21,午餐,米饭,150,116,174", lines[1])
    }

    @Test
    fun `含逗号和引号的食物名被正确转义`() {
        val csv = CsvExporter.toCsv(listOf(row("鸡,腿\"大\"")))
        assertTrue(csv.contains("\"鸡,腿\"\"大\"\"\""))
    }

    @Test
    fun `小数克数保留一位`() {
        val csv = CsvExporter.toCsv(listOf(row("牛奶", grams = 250.5, kcal = 162.75)))
        assertTrue(csv.contains("牛奶,250.5,116,162.8"))
    }
}
