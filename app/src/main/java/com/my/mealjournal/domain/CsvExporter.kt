package com.my.mealjournal.domain

/** CSV 导出：把记录序列化成表格文本，保证 Excel 可直接打开且不破坏字段。 */
object CsvExporter {

    const val HEADER = "日期,餐次,食物,克数,每100g热量,热量"

    data class Row(
        val date: String,
        val meal: MealType,
        val foodName: String,
        val grams: Double,
        val kcalPer100g: Double,
        val kcal: Double,
    )

    fun toCsv(rows: List<Row>): String = buildString {
        append(HEADER)
        append('\n')
        rows.forEach { row ->
            append(
                listOf(
                    row.date,
                    row.meal.label,
                    row.foodName,
                    formatNumber(row.grams),
                    formatNumber(row.kcalPer100g),
                    formatNumber(row.kcal),
                ).joinToString(",") { escape(it) },
            )
            append('\n')
        }
    }

    private fun formatNumber(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString()
        else String.format(java.util.Locale.US, "%.1f", value)

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
