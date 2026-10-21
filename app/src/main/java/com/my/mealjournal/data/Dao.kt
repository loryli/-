package com.my.mealjournal.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY isBuiltin DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' ORDER BY isBuiltin DESC, name COLLATE NOCASE")
    fun search(query: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isBuiltin = 0 ORDER BY name COLLATE NOCASE")
    fun observeCustom(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun byId(id: Long): FoodEntity?

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(food: FoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Update
    suspend fun update(food: FoodEntity)

    @Delete
    suspend fun delete(food: FoodEntity)
}

@Dao
interface EntryDao {

    @Query("SELECT * FROM entries WHERE date = :date ORDER BY CASE meal WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END, createdAt")
    fun observeForDate(date: String): Flow<List<EntryEntity>>

    @Query(
        "SELECT e.*, f.unitLabel AS unitLabel, f.unitGrams AS unitGrams " +
            "FROM entries e LEFT JOIN foods f ON f.id = e.foodId " +
            "WHERE e.date = :date " +
            "ORDER BY CASE e.meal WHEN 'BREAKFAST' THEN 0 WHEN 'LUNCH' THEN 1 WHEN 'DINNER' THEN 2 ELSE 3 END, e.createdAt",
    )
    fun observeForDateWithUnit(date: String): Flow<List<EntryUnitRow>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :from AND :to ORDER BY date, createdAt")
    fun observeBetween(from: String, to: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE date BETWEEN :from AND :to ORDER BY date, createdAt")
    suspend fun forRange(from: String, to: String): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY date, createdAt")
    suspend fun all(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun byId(id: Long): EntryEntity?

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)
}

@Dao
interface StatsDao {

    @Query("SELECT date, SUM(kcal) AS kcal FROM entries GROUP BY date")
    fun observeDailyTotals(): Flow<List<DailyTotal>>

    @Query("SELECT EXISTS(SELECT 1 FROM entries WHERE date = :date)")
    suspend fun hasEntryOn(date: String): Boolean

    @Query("SELECT SUM(kcal) FROM entries WHERE date = :date")
    fun observeKcalForDate(date: String): Flow<Double?>
}

data class DailyTotal(val date: String, val kcal: Double)

/** 条目 + 对应食物当前的份量信息；食物被删掉时为 null，列表只显示克数。 */
data class EntryUnitRow(
    val id: Long,
    val date: String,
    val meal: String,
    val foodId: Long?,
    val foodName: String,
    val kcalPer100g: Double,
    val grams: Double,
    val kcal: Double,
    val createdAt: Long,
    val unitLabel: String?,
    val unitGrams: Double?,
)
