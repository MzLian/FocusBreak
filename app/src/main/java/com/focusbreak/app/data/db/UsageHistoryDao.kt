package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class UsageHistoryDao {
    @Query("SELECT * FROM daily_app_usage WHERE date BETWEEN :from AND :to")
    abstract fun observeApps(from: String, to: String): Flow<List<DailyAppUsage>>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :from AND :to")
    abstract fun observeSummaries(from: String, to: String): Flow<List<DailySummary>>

    @Query("SELECT date FROM daily_summary WHERE date BETWEEN :from AND :to")
    abstract suspend fun summaryDates(from: String, to: String): List<String>

    @Query("DELETE FROM daily_app_usage WHERE date = :date")
    abstract suspend fun deleteApps(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertApps(rows: List<DailyAppUsage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertSummary(summary: DailySummary)

    /** Reemplaza todo un día de forma atómica. */
    @Transaction
    open suspend fun replaceDay(date: String, apps: List<DailyAppUsage>, summary: DailySummary) {
        deleteApps(date)
        if (apps.isNotEmpty()) insertApps(apps)
        upsertSummary(summary)
    }
}
