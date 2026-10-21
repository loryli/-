package com.my.mealjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodEntity::class, EntryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MealDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val NAME = "mealjournal.db"

        /**
         * 1 → 2：只补内置食物的份量字段，不动任何条目。
         * 老版本装过的机器靠这条迁移拿到新增的份量，历史记录原样保留。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                FoodSeed.builtins().forEach { seed ->
                    if (seed.unitLabel != null && seed.unitGrams != null) {
                        db.execSQL(
                            "UPDATE foods SET unitLabel = ?, unitGrams = ? " +
                                "WHERE name = ? AND isBuiltin = 1 AND unitLabel IS NULL",
                            arrayOf(seed.unitLabel, seed.unitGrams, seed.name),
                        )
                    }
                }
            }
        }

        @Volatile
        private var instance: MealDatabase? = null

        fun get(context: Context): MealDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): MealDatabase =
            Room.databaseBuilder(context, MealDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .addCallback(object : Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        FoodSeed.builtins().forEach { seed ->
                            db.execSQL(
                                "INSERT INTO foods (name, kcalPer100g, category, unitLabel, unitGrams, isBuiltin, createdAt) " +
                                    "VALUES (?, ?, ?, ?, ?, 1, ?)",
                                arrayOf(
                                    seed.name,
                                    seed.kcalPer100g,
                                    seed.category,
                                    seed.unitLabel,
                                    seed.unitGrams,
                                    System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                })
                .build()
    }
}
