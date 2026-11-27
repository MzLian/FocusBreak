package com.focusbreak.app.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Evento de intervención (aviso, bloqueo, extensión...). Permite medir el efecto de la app (objetivo 6). */
@Entity(tableName = "intervention_events", indices = [Index("date")])
data class InterventionEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val date: String,
    val packageName: String?,
    val type: String,        // nombre de InterventionType
    val value: Int,          // dato opcional (minutos usados, minutos extra...)
)

@Dao
interface InterventionEventDao {
    @Insert
    suspend fun insert(event: InterventionEvent)

    @Query("SELECT * FROM intervention_events WHERE date BETWEEN :from AND :to ORDER BY timestampMs")
    fun observe(from: String, to: String): Flow<List<InterventionEvent>>

    @Query("SELECT COUNT(*) FROM intervention_events WHERE date = :date AND type = :type")
    suspend fun count(date: String, type: String): Int

    /** Suma de `value` de un tipo de evento en un día (p. ej. minutos canjeados con pasos). */
    @Query("SELECT COALESCE(SUM(value), 0) FROM intervention_events WHERE date = :date AND type = :type")
    suspend fun sum(date: String, type: String): Int

    @Query("SELECT COALESCE(SUM(value), 0) FROM intervention_events WHERE date = :date AND type = :type")
    fun observeSum(date: String, type: String): Flow<Int>

    @Query("DELETE FROM intervention_events")
    suspend fun deleteAll()
}
