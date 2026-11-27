package com.focusbreak.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusbreak.app.domain.StepState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.stepsDataStore by preferencesDataStore(name = "focusbreak_steps")

/** Guarda la última lectura del sensor y los pasos de hoy. */
class StepsStore(private val context: Context) {
    private val counterKey = floatPreferencesKey("last_counter")
    private val dateKey = stringPreferencesKey("last_date")
    private val stepsKey = intPreferencesKey("steps_today")

    val state: Flow<StepState> = context.stepsDataStore.data.map {
        StepState(it[counterKey], it[dateKey], it[stepsKey] ?: 0)
    }

    suspend fun save(state: StepState) {
        context.stepsDataStore.edit { prefs ->
            state.lastCounter?.let { prefs[counterKey] = it }
            state.lastDate?.let { prefs[dateKey] = it }
            prefs[stepsKey] = state.stepsToday
        }
    }
}
