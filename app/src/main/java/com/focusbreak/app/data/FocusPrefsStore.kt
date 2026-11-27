package com.focusbreak.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusbreak.app.domain.FocusRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusPrefsDataStore by preferencesDataStore(name = "focusbreak_focus_prefs")

/** Preferencias del modo Concentración: duración elegida y apps seleccionadas para bloquear. */
class FocusPrefsStore(private val context: Context) {
    private val appsKey = stringSetPreferencesKey("selected_apps")
    private val durationKey = intPreferencesKey("duration_minutes")

    val selectedApps: Flow<Set<String>> =
        context.focusPrefsDataStore.data.map { it[appsKey] ?: emptySet() }

    val duration: Flow<Int> =
        context.focusPrefsDataStore.data.map { it[durationKey] ?: FocusRules.DEFAULT_MINUTES }

    suspend fun setDuration(minutes: Int) {
        context.focusPrefsDataStore.edit { it[durationKey] = minutes }
    }

    suspend fun toggleApp(packageName: String) {
        context.focusPrefsDataStore.edit { prefs ->
            val current = prefs[appsKey] ?: emptySet()
            prefs[appsKey] = if (packageName in current) current - packageName else current + packageName
        }
    }

    suspend fun clear() {
        context.focusPrefsDataStore.edit { it.clear() }
    }
}
