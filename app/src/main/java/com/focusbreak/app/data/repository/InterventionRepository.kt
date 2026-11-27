package com.focusbreak.app.data.repository

import com.focusbreak.app.data.db.InterventionEvent
import com.focusbreak.app.data.db.InterventionEventDao
import com.focusbreak.app.domain.InterventionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Registro local de las intervenciones de FocusBreak. Los datos no salen del dispositivo. */
class InterventionRepository(private val dao: InterventionEventDao) {

    suspend fun log(type: InterventionType, packageName: String? = null, value: Int = 0) {
        dao.insert(
            InterventionEvent(
                timestampMs = System.currentTimeMillis(),
                date = LocalDate.now().toString(),
                packageName = packageName,
                type = type.name,
                value = value,
            )
        )
    }

    fun observe(from: LocalDate, to: LocalDate): Flow<List<InterventionEvent>> =
        dao.observe(from.toString(), to.toString())

    suspend fun countToday(type: InterventionType): Int =
        dao.count(LocalDate.now().toString(), type.name)

    /** Suma de `value` de un tipo de evento hoy (minutos canjeados, etc.). */
    suspend fun sumToday(type: InterventionType): Int = dao.sum(LocalDate.now().toString(), type.name)

    fun observeSum(date: LocalDate, type: InterventionType): Flow<Int> = dao.observeSum(date.toString(), type.name)

    suspend fun clearAll() = dao.deleteAll()
}
