package com.focusbreak.app.data.repository

import com.focusbreak.app.data.db.DailyAppState
import com.focusbreak.app.data.db.DailyAppStateDao
import com.focusbreak.app.domain.LimitLevel
import java.time.LocalDate

/**
 * Estado diario por app (extensiones, minutos extra, avisos enviados), persistido en Room.
 * Lo comparten el servicio de accesibilidad y la app, y sobrevive a reinicios del servicio.
 */
class DailyStateRepository(private val dao: DailyAppStateDao) {
    private fun today() = LocalDate.now().toString()

    suspend fun get(pkg: String): DailyAppState =
        dao.find(today(), pkg) ?: DailyAppState(today(), pkg, 0, 0, "")

    /** Concede una extensión (cuenta como 1 extensión y suma sus minutos). */
    suspend fun addExtension(pkg: String, minutes: Int) = dao.add(today(), pkg, minutes, 1)

    /** Suma minutos extra sin contar como extensión (para el canje de pasos, HU13). */
    suspend fun addExtraMinutes(pkg: String, minutes: Int) = dao.add(today(), pkg, minutes, 0)

    /** true solo la primera vez que se avisa ese nivel para esa app hoy. */
    suspend fun markNotified(pkg: String, level: LimitLevel): Boolean = dao.markNotified(today(), pkg, level)

    suspend fun purgeOldDays() = dao.deleteBefore(today())

    suspend fun clearAll() = dao.deleteAll()
}
