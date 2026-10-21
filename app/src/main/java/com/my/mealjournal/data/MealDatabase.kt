package com.my.mealjournal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FoodEntity::class, EntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MealDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun entryDao(): EntryDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val NAME = "mealjournal.db"

        @Volatile
        private var instance: MealDatabase? = null

        fun get(context: Context): MealDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): MealDatabase =
            Room.databaseBuilder(context, MealDatabase::class.java, NAME)
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
