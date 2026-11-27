package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.focusbreak.app.domain.LimitLevel
import com.focusbreak.app.domain.NotifiedLevels

/**
 * Estado de una app en un día: minutos extra concedidos (extensiones y, más adelante, canje de pasos),
 * número de extensiones usadas y niveles de aviso ya enviados. Una fila por (fecha, app).
 */
@Entity(tableName = "daily_app_state", primaryKeys = ["date", "packageName"])
data class DailyAppState(
    val date: String,
    val packageName: String,
    val extraMinutes: Int,
    val extensions: Int,
    val notified: String,
)

@Dao
abstract class DailyAppStateDao {
    @Query("SELECT * FROM daily_app_state WHERE date = :date AND packageName = :pkg")
    abstract suspend fun find(date: String, pkg: String): DailyAppState?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertIfAbsent(row: DailyAppState)

    @Query("UPDATE daily_app_state SET extraMinutes = extraMinutes + :minutes, extensions = extensions + :ext WHERE date = :date AND packageName = :pkg")
    abstract suspend fun increment(date: String, pkg: String, minutes: Int, ext: Int)

    @Query("UPDATE daily_app_state SET notified = :notified WHERE date = :date AND packageName = :pkg")
    abstract suspend fun setNotified(date: String, pkg: String, notified: String)

    @Query("DELETE FROM daily_app_state WHERE date < :date")
    abstract suspend fun deleteBefore(date: String)

    @Query("DELETE FROM daily_app_state")
    abstract suspend fun deleteAll()

    /** Suma minutos/extensiones de forma atómica (el motor y la app pueden escribir a la vez). */
    @Transaction
    open suspend fun add(date: String, pkg: String, minutes: Int, ext: Int) {
        insertIfAbsent(DailyAppState(date, pkg, 0, 0, ""))
        increment(date, pkg, minutes, ext)
    }

    /** Marca un nivel como avisado. Devuelve true solo la primera vez en el día. */
    @Transaction
    open suspend fun markNotified(date: String, pkg: String, level: LimitLevel): Boolean {
        insertIfAbsent(DailyAppState(date, pkg, 0, 0, ""))
        val current = find(date, pkg)?.notified ?: ""
        if (NotifiedLevels.contains(current, level)) return false
        setNotified(date, pkg, NotifiedLevels.add(current, level))
        return true
    }
}
