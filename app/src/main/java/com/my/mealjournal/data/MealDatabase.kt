package com.my.mealjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodEntity::class, EntryEntity::class],
    version = 3,
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

        /**
         * 2 → 3：补三大营养素。
         * foods 加可空的 per-100g 列（内置食物按种子回填）；entries 加按克数算好的快照列，
         * 历史条目按当前食物数据回填，食物缺失或无数据的保持 0（热量口径不受影响）。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN proteinPer100g REAL")
                db.execSQL("ALTER TABLE foods ADD COLUMN carbsPer100g REAL")
                db.execSQL("ALTER TABLE foods ADD COLUMN fatPer100g REAL")
                db.execSQL("ALTER TABLE entries ADD COLUMN protein REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entries ADD COLUMN carbs REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE entries ADD COLUMN fat REAL NOT NULL DEFAULT 0")

                FoodSeed.builtins().forEach { seed ->
                    db.execSQL(
                        "UPDATE foods SET proteinPer100g = ?, carbsPer100g = ?, fatPer100g = ? " +
                            "WHERE name = ? AND isBuiltin = 1 AND proteinPer100g IS NULL",
                        arrayOf(seed.proteinPer100g, seed.carbsPer100g, seed.fatPer100g, seed.name),
                    )
                }
                // COALESCE 兜底：食物缺行或无营养数据时保持 0，不能把 NULL 写进 NOT NULL 列
                listOf("protein" to "proteinPer100g", "carbs" to "carbsPer100g", "fat" to "fatPer100g").forEach { (col, src) ->
                    db.execSQL(
                        "UPDATE entries SET $col = COALESCE((" +
                            "SELECT ROUND(grams * f.$src / 100.0, 1) FROM foods f " +
                            "WHERE f.id = entries.foodId AND f.$src IS NOT NULL), 0) " +
                            "WHERE entries.foodId IS NOT NULL",
                    )
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        FoodSeed.builtins().forEach { seed ->
                            db.execSQL(
                                "INSERT INTO foods (name, kcalPer100g, category, unitLabel, unitGrams, proteinPer100g, carbsPer100g, fatPer100g, isBuiltin, createdAt) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?)",
                                arrayOf(
                                    seed.name,
                                    seed.kcalPer100g,
                                    seed.category,
                                    seed.unitLabel,
                                    seed.unitGrams,
                                    seed.proteinPer100g,
                                    seed.carbsPer100g,
                                    seed.fatPer100g,
                                    System.currentTimeMillis(),
                                ),
                            )
                        }
                    }
                })
                .build()
    }
}
