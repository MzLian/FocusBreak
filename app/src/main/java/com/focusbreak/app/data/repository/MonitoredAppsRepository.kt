package com.focusbreak.app.data.repository

import com.focusbreak.app.data.db.MonitoredApp
import com.focusbreak.app.data.db.MonitoredAppDao
import com.focusbreak.app.domain.LimitRules
import kotlinx.coroutines.flow.Flow

/** Único punto de acceso a las apps monitoreadas. Aplica las reglas de dominio antes de guardar. */
class MonitoredAppsRepository(private val dao: MonitoredAppDao) {
    val apps: Flow<List<MonitoredApp>> = dao.observeAll()

    suspend fun add(packageName: String, label: String, limitMinutes: Int? = LimitRules.DEFAULT) =
        dao.upsert(MonitoredApp(packageName, label, limitMinutes?.let(LimitRules::coerce)))

    suspend fun setLimit(packageName: String, minutes: Int?) =
        dao.setLimit(packageName, minutes?.let(LimitRules::coerce))

    suspend fun setEnabled(packageName: String, enabled: Boolean) = dao.setEnabled(packageName, enabled)

    suspend fun remove(packageName: String) = dao.delete(packageName)
}
