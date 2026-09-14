package com.example.nutritiontracker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FoodItem::class, FoodLogEntry::class, FavoriteFood::class, WeightEntry::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun weightDao(): WeightDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Adds the favorite_foods table without touching existing tables,
        // so previously logged meals and the cached food list are preserved.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS favorite_foods (
                        nummer INTEGER NOT NULL PRIMARY KEY,
                        namn TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Adds the weight_entries table for the weight tracking feature.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weight_entries (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        date TEXT NOT NULL,
                        weightKg REAL NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nutrition_tracker_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}