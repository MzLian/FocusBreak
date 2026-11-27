package com.focusbreak.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "focusbreak_settings")

/** Preferencias simples del usuario (DataStore). En el Sprint 1 solo guarda si ya terminó el onboarding. */
class SettingsStore(private val context: Context) {
    private val onboardingKey = booleanPreferencesKey("onboarding_done")

    val onboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun setOnboardingDone(value: Boolean) {
        context.dataStore.edit { it[onboardingKey] = value }
    }
}
