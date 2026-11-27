package com.focusbreak.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.focusbreak.app.data.SettingsStore
import com.focusbreak.app.ui.navigation.MainScaffold
import com.focusbreak.app.ui.onboarding.OnboardingScreen
import com.focusbreak.app.ui.theme.FocusBreakTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = SettingsStore(applicationContext)

        setContent {
            FocusBreakTheme {
                // null = aún cargando la preferencia (evita parpadeo del onboarding)
                val onboardingDone by store.onboardingDone.collectAsState(initial = null)
                when (onboardingDone) {
                    null -> Unit
                    false -> OnboardingScreen(onFinish = {
                        lifecycleScope.launch { store.setOnboardingDone(true) }
                    })
                    true -> MainScaffold()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Guarda el historial de uso cada vez que se abre la app (Android solo conserva ~7 días de eventos)
        lifecycleScope.launch {
            val container = (application as FocusBreakApp).container
            container.steps.sync()
            container.history.sync()
        }
    }
}
