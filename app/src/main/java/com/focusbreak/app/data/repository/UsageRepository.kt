package com.focusbreak.app.data.repository

import kotlinx.coroutines.flow.Flow

/** Uso del día. perApp: minutos por nombre de paquete. */
data class TodayUsage(
    val totalMinutes: Int,
    val yesterdayMinutes: Int,
    val unlocks: Int,
    val perApp: Map<String, Int>,
)

interface UsageRepository {
    /** Flujo para la UI (se refresca solo). */
    fun today(): Flow<TodayUsage>

    /** Consulta puntual con precisión de milisegundos por app; la usa el motor de límites. */
    suspend fun todayPerAppMs(): Map<String, Long>
}
