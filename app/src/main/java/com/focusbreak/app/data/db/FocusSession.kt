package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Sesión de concentración. status: ACTIVE, COMPLETED o CANCELLED (ver FocusStatus).
 * Se guarda la hora de fin (no una cuenta regresiva), así la sesión sobrevive si la app se cierra.
 * apps: paquetes bloqueados durante la sesión, separados por comas (copia fija al iniciar).
 */
@Entity(tableName = "focus_sessions", indices = [Index("date")])
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // día en que empezó (yyyy-MM-dd)
    val startMs: Long,
    val endMs: Long,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val status: String,
    val apps: String,
)

@Dao
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun findActive(): FocusSession?

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<FocusSession?>

    /** Cierra la sesión solo si sigue activa; devuelve 1 la primera vez y 0 si otro ya la cerró. */
    @Query("UPDATE focus_sessions SET status = :status, actualMinutes = :actual WHERE id = :id AND status = 'ACTIVE'")
    suspend fun finish(id: Long, status: String, actual: Int): Int

    @Query("SELECT COALESCE(SUM(actualMinutes), 0) FROM focus_sessions WHERE date = :date AND status != 'ACTIVE'")
    fun observeFinishedMinutes(date: String): Flow<Int>

    @Query("SELECT * FROM focus_sessions WHERE date BETWEEN :from AND :to ORDER BY startMs")
    fun observe(from: String, to: String): Flow<List<FocusSession>>

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
