package com.my.mealjournal.domain

/** 一餐的类型。只有三餐参与"完整打卡"判定，加餐只计热量。 */
enum class MealType(val label: String, val countsForCheckIn: Boolean) {
    BREAKFAST("早餐", true),
    LUNCH("午餐", true),
    DINNER("晚餐", true),
    SNACK("加餐", false),
}
