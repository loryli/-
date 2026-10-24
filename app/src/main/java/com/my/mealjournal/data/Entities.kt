package com.my.mealjournal.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 食物库条目。内置与自建共用一张表，用 isBuiltin 区分。营养素为 null 表示未知，只按热量记。 */
@Entity(tableName = "foods", indices = [Index(value = ["name"])])
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kcalPer100g: Double,
    val category: String,
    val unitLabel: String?,
    val unitGrams: Double?,
    val proteinPer100g: Double?,
    val carbsPer100g: Double?,
    val fatPer100g: Double?,
    val isBuiltin: Boolean,
    val createdAt: Long,
)

/**
 * 一条进食记录。foodName / kcalPer100g / kcal / 三大营养素是快照，
 * 食物被改名、删除或改营养数据都不影响历史。
 */
@Entity(
    tableName = "entries",
    indices = [Index(value = ["date"]), Index(value = ["date", "meal"])],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val meal: String,
    val foodId: Long?,
    val foodName: String,
    val kcalPer100g: Double,
    val grams: Double,
    val kcal: Double,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val createdAt: Long,
)
