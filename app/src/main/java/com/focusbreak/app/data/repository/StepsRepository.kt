package com.focusbreak.app.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.focusbreak.app.data.StepsStore
import com.focusbreak.app.domain.StepMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/** Pasos de hoy (HU11). Se actualiza al abrir la app, mientras Inicio está visible y cada ~15 min con WorkManager. */
class StepsRepository(
    private val context: Context,
    private val reader: StepSensorReader,
    private val store: StepsStore,
) {
    private val mutex = Mutex()

    val hasSensor: Boolean get() = reader.isAvailable

    val stepsToday: Flow<Int> = store.state.map {
        if (it.lastDate == LocalDate.now().toString()) it.stepsToday else 0
    }

    suspend fun sync() = mutex.withLock {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return@withLock
        val counter = reader.readCounter() ?: return@withLock
        val current = store.state.first()
        store.save(StepMath.apply(current, counter, LocalDate.now().toString()))
    }
}
