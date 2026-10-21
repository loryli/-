package com.my.mealjournal.domain

import java.time.LocalTime

/** 热量计算与餐次自动归属的纯函数集合。 */
object CalorieMath {

    /** 按"克数 × 每 100g 热量"计算热量。 */
    fun kcal(kcalPer100g: Double, grams: Double): Double {
        require(grams >= 0) { "grams must not be negative: $grams" }
        return kcalPer100g * grams / 100.0
    }
}

object MealTiming {

    /** 05–10 早餐、10–15 午餐、15–21 晚餐，其余时间为加餐。左闭右开。 */
    fun defaultMeal(time: LocalTime): MealType = when {
        time >= LocalTime.of(5, 0) && time < LocalTime.of(10, 0) -> MealType.BREAKFAST
        time >= LocalTime.of(10, 0) && time < LocalTime.of(15, 0) -> MealType.LUNCH
        time >= LocalTime.of(15, 0) && time < LocalTime.of(21, 0) -> MealType.DINNER
        else -> MealType.SNACK
    }
}
