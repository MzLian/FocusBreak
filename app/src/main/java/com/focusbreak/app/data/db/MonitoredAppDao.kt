package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {
    @Query("SELECT * FROM monitored_apps ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<MonitoredApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: MonitoredApp)

    @Query("UPDATE monitored_apps SET dailyLimitMinutes = :minutes WHERE packageName = :pkg")
    suspend fun setLimit(pkg: String, minutes: Int?)

    @Query("UPDATE monitored_apps SET enabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("DELETE FROM monitored_apps WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}
