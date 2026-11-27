package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Pasos totales de un día (fecha ISO yyyy-MM-dd). Historial para rachas, resumen semanal y evaluación. */
@Entity(tableName = "daily_steps")
data class DailySteps(@PrimaryKey val date: String, val steps: Int)

@Dao
interface DailyStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DailySteps)

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observe(from: String, to: String): Flow<List<DailySteps>>

    @Query("DELETE FROM daily_steps")
    suspend fun deleteAll()
}
