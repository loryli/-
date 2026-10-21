package com.my.mealjournal.domain

/**
 * 份量与克数之间的换算。
 *
 * 库里只存克数，份量只是输入方式：1 个鸡蛋 50 g，输入 1 个就存 50 g。
 * 这样统计、CSV、补记窗口都不需要知道"份"的存在。
 */
object Portion {

    fun gramsOf(portion: Double, unitGrams: Double): Double = portion * unitGrams

    fun portionOf(grams: Double, unitGrams: Double): Double =
        if (unitGrams <= 0.0) 0.0 else grams / unitGrams

    /** 整数不带小数点，其余保留一位小数。 */
    fun label(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

    /** 列表里的一行量：有份量写「1 个 · 50 g」，否则只写克数。 */
    fun describe(grams: Double, unitLabel: String?, unitGrams: Double?): String {
        if (unitLabel == null || unitGrams == null || unitGrams <= 0.0) return "${label(grams)} g"
        return "${label(portionOf(grams, unitGrams))} $unitLabel · ${label(grams)} g"
    }
}
