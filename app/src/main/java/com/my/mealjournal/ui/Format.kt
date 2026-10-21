package com.my.mealjournal.ui

import com.my.mealjournal.domain.MealType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFull = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
private val dayShort = DateTimeFormatter.ofPattern("M/d", Locale.SIMPLIFIED_CHINESE)
private val monthTitle = DateTimeFormatter.ofPattern("yyyy年M月", Locale.SIMPLIFIED_CHINESE)

fun LocalDate.labelFull(): String = format(dayFull)

fun LocalDate.labelShort(): String = format(dayShort)

fun java.time.YearMonth.label(): String = atDay(1).format(monthTitle)

fun MealType.shortLabel(): String = when (this) {
    MealType.BREAKFAST -> "早"
    MealType.LUNCH -> "午"
    MealType.DINNER -> "晚"
    MealType.SNACK -> "加"
}

fun Double.kcalText(): String = "${this.toInt()} kcal"

fun Int.minuteText(): String = "%02d:%02d".format(this / 60, this % 60)
