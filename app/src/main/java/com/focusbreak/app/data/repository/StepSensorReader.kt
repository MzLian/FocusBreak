package com.focusbreak.app.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Lee una vez el contador de pasos del teléfono (TYPE_STEP_COUNTER: pasos acumulados desde el último reinicio). */
class StepSensorReader(context: Context) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    val isAvailable: Boolean get() = sensor != null

    /** Devuelve el contador, o null si no hay sensor, no hay permiso o no responde en 5 s. */
    suspend fun readCounter(): Float? {
        val stepSensor = sensor ?: return null
        return withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine<Float?> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorManager.unregisterListener(this)
                        if (cont.isActive) cont.resume(event.values[0])
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                val registered = try {
                    sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
                } catch (e: SecurityException) {
                    false
                }
                if (!registered && cont.isActive) cont.resume(null)
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }
}
