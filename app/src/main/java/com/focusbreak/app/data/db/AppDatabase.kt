package com.focusbreak.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MonitoredApp::class, DailyAppUsage::class, DailySummary::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun usageHistoryDao(): UsageHistoryDao

    companion object {
        /** v1 -> v2: añade el historial diario sin tocar las apps monitoreadas del usuario. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_app_usage` (`date` TEXT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `minutes` INTEGER NOT NULL, PRIMARY KEY(`date`, `packageName`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_summary` (`date` TEXT NOT NULL, " +
                        "`unlocks` INTEGER NOT NULL, `hourlyMinutes` TEXT NOT NULL, PRIMARY KEY(`date`))"
                )
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "focusbreak.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
