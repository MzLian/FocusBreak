package com.focusbreak.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.focusbreak.app.data.StepsStore
import com.focusbreak.app.data.db.DailySteps
import com.focusbreak.app.data.db.DailyStepsDao
import com.focusbreak.app.domain.StepMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Pasos de hoy (HU11). Se actualiza al abrir la app, mientras la app está abierta y cada ~15 min con WorkManager.
 * DataStore guarda la última lectura del sensor; Room guarda el historial de pasos por día.
 */
class StepsRepository(
    private val context: Context,
    private val reader: StepSensorReader,
    private val store: StepsStore,
    private val dao: DailyStepsDao,
) {
    private val mutex = Mutex()

    val hasSensor: Boolean get() = reader.isAvailable

    /** true si el teléfono tiene sensor y la persona concedió el permiso de actividad física. */
    val canTrack: Boolean
        get() = reader.isAvailable && ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    val stepsToday: Flow<Int> = store.state.map {
        if (it.lastDate == LocalDate.now().toString()) it.stepsToday else 0
    }

    /** Historial de pasos por día (para rachas, resumen semanal y evaluación). */
    fun history(from: LocalDate, to: LocalDate): Flow<List<DailySteps>> =
        dao.observe(from.toString(), to.toString())

    suspend fun sync() = mutex.withLock {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return@withLock
        val counter = reader.readCounter() ?: return@withLock
        val today = LocalDate.now().toString()
        val next = StepMath.apply(store.state.first(), counter, today)
        store.save(next)
        dao.upsert(DailySteps(today, next.stepsToday))
    }

    suspend fun clearHistory() = dao.deleteAll()
}
