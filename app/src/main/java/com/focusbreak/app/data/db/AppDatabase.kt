package com.focusbreak.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MonitoredApp::class, DailyAppUsage::class, DailySummary::class,
        DailyAppState::class, DailySteps::class, InterventionEvent::class, FocusSession::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun usageHistoryDao(): UsageHistoryDao
    abstract fun dailyAppStateDao(): DailyAppStateDao
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun interventionEventDao(): InterventionEventDao
    abstract fun focusSessionDao(): FocusSessionDao

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

        /** v2 -> v3: estado diario por app, pasos por día y eventos de intervención. No toca datos existentes. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_app_state` (`date` TEXT NOT NULL, " +
                        "`packageName` TEXT NOT NULL, `extraMinutes` INTEGER NOT NULL, " +
                        "`extensions` INTEGER NOT NULL, `notified` TEXT NOT NULL, PRIMARY KEY(`date`, `packageName`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `daily_steps` (`date` TEXT NOT NULL, " +
                        "`steps` INTEGER NOT NULL, PRIMARY KEY(`date`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `intervention_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`timestampMs` INTEGER NOT NULL, `date` TEXT NOT NULL, `packageName` TEXT, " +
                        "`type` TEXT NOT NULL, `value` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_intervention_events_date` ON `intervention_events` (`date`)")
            }
        }

        /** v3 -> v4: sesiones de concentración (HU10). No toca datos existentes. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`date` TEXT NOT NULL, `startMs` INTEGER NOT NULL, `endMs` INTEGER NOT NULL, " +
                        "`plannedMinutes` INTEGER NOT NULL, `actualMinutes` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `apps` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_date` ON `focus_sessions` (`date`)")
            }
        }

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "focusbreak.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
