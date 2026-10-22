package com.my.mealjournal.data

import com.my.mealjournal.domain.CsvExporter
import com.my.mealjournal.domain.MealType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 单条记录在 UI 层的样子：只保留展示与计算需要的字段。 */
data class EntryView(
    val id: Long,
    val date: LocalDate,
    val meal: MealType,
    val foodName: String,
    val grams: Double,
    val kcal: Double,
    val kcalPer100g: Double = 0.0,
    val unitLabel: String? = null,
    val unitGrams: Double? = null,
)

fun EntryEntity.toView(): EntryView = EntryView(
    id = id,
    date = LocalDate.parse(date),
    meal = runCatching { MealType.valueOf(meal) }.getOrDefault(MealType.SNACK),
    foodName = foodName,
    grams = grams,
    kcal = kcal,
    kcalPer100g = kcalPer100g,
)

fun EntryUnitRow.toView(): EntryView = EntryView(
    id = id,
    date = LocalDate.parse(date),
    meal = runCatching { MealType.valueOf(meal) }.getOrDefault(MealType.SNACK),
    foodName = foodName,
    grams = grams,
    kcal = kcal,
    kcalPer100g = kcalPer100g,
    unitLabel = unitLabel,
    unitGrams = unitGrams,
)

class MealRepository(private val db: MealDatabase) {

    private val foodDao = db.foodDao()
    private val entryDao = db.entryDao()
    private val statsDao = db.statsDao()

    fun entriesFor(date: LocalDate): Flow<List<EntryView>> =
        entryDao.observeForDateWithUnit(date.toString()).map { list -> list.map { it.toView() } }

    fun entriesBetween(from: LocalDate, to: LocalDate): Flow<List<EntryView>> =
        entryDao.observeBetween(from.toString(), to.toString()).map { list -> list.map { it.toView() } }

    fun foods(query: String): Flow<List<FoodEntity>> =
        if (query.isBlank()) foodDao.observeAll() else foodDao.search(query)

    fun customFoods(): Flow<List<FoodEntity>> = foodDao.observeCustom()

    /** 最近 30 天内记过、且食物仍然存在的条目，按最近一次记录排序。 */
    suspend fun recentFoods(limit: Int = 6, today: LocalDate = LocalDate.now()): List<FoodEntity> {
        val ids = entryDao.recentFoodIds(today.minusDays(30).toString(), limit)
        if (ids.isEmpty()) return emptyList()
        val byId = foodDao.byIds(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    suspend fun addEntry(date: LocalDate, meal: MealType, food: FoodEntity, grams: Double): Long {
        val kcal = com.my.mealjournal.domain.CalorieMath.kcal(food.kcalPer100g, grams)
        return entryDao.insert(
            EntryEntity(
                date = date.toString(),
                meal = meal.name,
                foodId = food.id,
                foodName = food.name,
                kcalPer100g = food.kcalPer100g,
                grams = grams,
                kcal = kcal,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateEntryGrams(entryId: Long, grams: Double): Boolean {
        val existing = entryDao.byId(entryId) ?: return false
        val kcal = com.my.mealjournal.domain.CalorieMath.kcal(existing.kcalPer100g, grams)
        entryDao.update(existing.copy(grams = grams, kcal = kcal))
        return true
    }

    suspend fun deleteEntry(entryId: Long) {
        entryDao.byId(entryId)?.let { entryDao.delete(it) }
    }

    /** 删除并返回快照，供 5 秒内撤销。 */
    suspend fun deleteEntryReturning(entryId: Long): EntryEntity? {
        val existing = entryDao.byId(entryId) ?: return null
        entryDao.delete(existing)
        return existing
    }

    suspend fun restoreEntry(entry: EntryEntity) {
        entryDao.insert(entry)
    }

    suspend fun lastGrams(foodId: Long): Double? = entryDao.lastGrams(foodId)

    suspend fun addCustomFood(
        name: String,
        kcalPer100g: Double,
        category: String,
        unitLabel: String? = null,
        unitGrams: Double? = null,
    ): Long =
        foodDao.insert(
            FoodEntity(
                name = name,
                kcalPer100g = kcalPer100g,
                category = category,
                unitLabel = unitLabel,
                unitGrams = unitGrams,
                isBuiltin = false,
                createdAt = System.currentTimeMillis(),
            ),
        )

    suspend fun deleteCustomFood(food: FoodEntity) {
        if (!food.isBuiltin) foodDao.delete(food)
    }

    suspend fun exportCsv(): String {
        val rows = entryDao.all().map { entity ->
            CsvExporter.Row(
                date = entity.date,
                meal = runCatching { MealType.valueOf(entity.meal) }.getOrDefault(MealType.SNACK),
                foodName = entity.foodName,
                grams = entity.grams,
                kcalPer100g = entity.kcalPer100g,
                kcal = entity.kcal,
            )
        }
        return CsvExporter.toCsv(rows)
    }
}
