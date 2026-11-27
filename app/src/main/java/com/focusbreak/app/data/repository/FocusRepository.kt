package com.focusbreak.app.data.repository

import com.focusbreak.app.data.FocusPrefsStore
import com.focusbreak.app.data.db.FocusSession
import com.focusbreak.app.data.db.FocusSessionDao
import com.focusbreak.app.domain.FocusRules
import com.focusbreak.app.domain.FocusStatus
import com.focusbreak.app.domain.InterventionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Sesiones de concentración (HU10): iniciar, terminar y consultar. Lo usan la app y el servicio de accesibilidad. */
class FocusRepository(
    private val dao: FocusSessionDao,
    private val prefs: FocusPrefsStore,
    private val interventions: InterventionRepository,
) {
    val activeSession: Flow<FocusSession?> = dao.observeActive()
    val selectedApps: Flow<Set<String>> = prefs.selectedApps
    val duration: Flow<Int> = prefs.duration

    /** Minutos de sesiones ya terminadas en un día (completas o canceladas, solo lo cumplido). */
    fun finishedMinutes(date: LocalDate): Flow<Int> = dao.observeFinishedMinutes(date.toString())

    fun history(from: LocalDate, to: LocalDate): Flow<List<FocusSession>> =
        dao.observe(from.toString(), to.toString())

    suspend fun setDuration(minutes: Int) = prefs.setDuration(minutes)
    suspend fun toggleApp(packageName: String) = prefs.toggleApp(packageName)

    /** Inicia una sesión. Devuelve false si no hay apps, la duración no es válida o ya hay una sesión activa. */
    suspend fun start(minutes: Int, packages: Collection<String>): Boolean {
        if (packages.isEmpty() || minutes !in FocusRules.DURATIONS) return false
        if (dao.findActive() != null) return false
        val now = System.currentTimeMillis()
        dao.insert(
            FocusSession(
                date = LocalDate.now().toString(),
                startMs = now,
                endMs = FocusRules.endMs(now, minutes),
                plannedMinutes = minutes,
                actualMinutes = 0,
                status = FocusStatus.ACTIVE.name,
                apps = FocusRules.toCsv(packages),
            )
        )
        interventions.log(InterventionType.FOCUS_START, null, minutes)
        return true
    }

    /** Sesión activa ahora mismo; si ya venció, la cierra como completada y devuelve null. */
    suspend fun activeNow(): FocusSession? {
        finishIfExpired()
        return dao.findActive()
    }

    suspend fun finishIfExpired(nowMs: Long = System.currentTimeMillis()) {
        val session = dao.findActive() ?: return
        if (!FocusRules.isExpired(session.endMs, nowMs)) return
        if (dao.finish(session.id, FocusStatus.COMPLETED.name, session.plannedMinutes) > 0) {
            interventions.log(InterventionType.FOCUS_COMPLETED, null, session.plannedMinutes)
        }
    }

    /** Termina la sesión antes de tiempo y registra solo los minutos cumplidos. */
    suspend fun endEarly() {
        val session = dao.findActive() ?: return
        val actual = FocusRules.elapsedMinutes(session.startMs, session.plannedMinutes, System.currentTimeMillis())
        if (dao.finish(session.id, FocusStatus.CANCELLED.name, actual) > 0) {
            interventions.log(InterventionType.FOCUS_CANCELLED, null, actual)
        }
    }

    suspend fun clearAll() {
        dao.deleteAll()
        prefs.clear()
    }
}
