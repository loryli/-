package com.my.mealjournal.domain

/**
 * 三大营养素（每 100g 克数口径）。
 *
 * 目标不按个人资料推，直接把目标热量按固定供能比拆：
 * 碳水 50% / 蛋白 20% / 脂肪 30%（中国居民膳食指南的常见区间中值），
 * 供能系数蛋白/碳水 4 kcal/g、脂肪 9 kcal/g。
 */
data class MacroGrams(val protein: Double, val carbs: Double, val fat: Double) {
    operator fun plus(other: MacroGrams) =
        MacroGrams(protein + other.protein, carbs + other.carbs, fat + other.fat)

    /** 三营养供能合计 kcal，用于算占比。 */
    val kcal: Double get() = protein * 4 + carbs * 4 + fat * 9
}

/** 某食物每 100g 的营养密度；任一字段未知记 null，未知按 0 参与合计。 */
data class MacroPer100g(val protein: Double?, val carbs: Double?, val fat: Double?) {
    val known: Boolean get() = protein != null || carbs != null || fat != null
}

object Nutrition {

    const val PROTEIN_RATIO = 0.20
    const val CARB_RATIO = 0.50
    const val FAT_RATIO = 0.30
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARB = 4.0
    const val KCAL_PER_G_FAT = 9.0

    fun targets(goalKcal: Int): MacroGrams = MacroGrams(
        protein = goalKcal * PROTEIN_RATIO / KCAL_PER_G_PROTEIN,
        carbs = goalKcal * CARB_RATIO / KCAL_PER_G_CARB,
        fat = goalKcal * FAT_RATIO / KCAL_PER_G_FAT,
    )

    /** 条目营养快照：per100g × 克数，未知字段按 0 记（热量口径不受影响）。 */
    fun ofGrams(per100g: MacroPer100g?, grams: Double): MacroGrams = MacroGrams(
        protein = ((per100g?.protein ?: 0.0) * grams / 100).round1(),
        carbs = ((per100g?.carbs ?: 0.0) * grams / 100).round1(),
        fat = ((per100g?.fat ?: 0.0) * grams / 100).round1(),
    )

    /** 三营养供能占比（0..1），总供能为 0 时全 0。 */
    fun shares(macro: MacroGrams): MacroGrams {
        val total = macro.kcal
        if (total <= 0.0) return MacroGrams(0.0, 0.0, 0.0)
        return MacroGrams(
            protein = macro.protein * KCAL_PER_G_PROTEIN / total,
            carbs = macro.carbs * KCAL_PER_G_CARB / total,
            fat = macro.fat * KCAL_PER_G_FAT / total,
        )
    }
}

private fun Double.round1(): Double = (this * 10).toLong() / 10.0
