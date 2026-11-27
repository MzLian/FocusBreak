package com.focusbreak.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.focusbreak.app.FocusBreakApp
import com.focusbreak.app.data.db.FocusSession
import com.focusbreak.app.data.db.MonitoredApp
import com.focusbreak.app.data.repository.FocusRepository
import com.focusbreak.app.data.repository.MonitoredAppsRepository
import com.focusbreak.app.domain.FocusRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FocusAppItem(val app: MonitoredApp, val selected: Boolean)

data class FocusUiState(
    val durations: List<Int> = FocusRules.DURATIONS,
    val selectedMinutes: Int = FocusRules.DEFAULT_MINUTES,
    val apps: List<FocusAppItem> = emptyList(),
    val session: FocusSession? = null,
    val remainingMs: Long = 0L,
    val todayMinutes: Int = 0,
    val goalMinutes: Int = FocusRules.DAILY_GOAL_MINUTES,
) {
    val selectedCount: Int get() = apps.count { it.selected }
    val canStart: Boolean get() = session == null && selectedCount > 0
}

@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(
    monitoredRepo: MonitoredAppsRepository,
    private val focus: FocusRepository,
) : ViewModel() {

    private val ticker = flow { while (true) { emit(System.currentTimeMillis()); delay(1_000) } }
    private val today = flow { while (true) { emit(LocalDate.now()); delay(60_000) } }.distinctUntilChanged()
    private val finishedToday = today.flatMapLatest { focus.finishedMinutes(it) }

    val uiState: StateFlow<FocusUiState> = combine(
        combine(monitoredRepo.apps, focus.selectedApps, focus.duration) { apps, selected, minutes ->
            Triple(apps, selected, minutes)
        },
        combine(focus.activeSession, finishedToday) { active, done -> active to done },
        ticker,
    ) { (apps, selected, minutes), (active, done), now ->
        FocusUiState(
            selectedMinutes = minutes,
            apps = apps.map { FocusAppItem(it, it.packageName in selected) },
            session = active,
            remainingMs = active?.let { FocusRules.remainingMs(it.endMs, now) } ?: 0L,
            todayMinutes = done + (active?.let { FocusRules.elapsedMinutes(it.startMs, it.plannedMinutes, now) } ?: 0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    init {
        // Cuando llega la hora de fin de la sesión activa, se cierra como completada
        viewModelScope.launch {
            focus.activeSession.collectLatest { session ->
                if (session != null) {
                    delay((session.endMs - System.currentTimeMillis()).coerceAtLeast(0L) + 200L)
                    focus.finishIfExpired()
                }
            }
        }
    }

    fun selectDuration(minutes: Int) { viewModelScope.launch { focus.setDuration(minutes) } }

    fun toggleApp(packageName: String) { viewModelScope.launch { focus.toggleApp(packageName) } }

    fun start() {
        viewModelScope.launch {
            val state = uiState.value
            focus.start(state.selectedMinutes, state.apps.filter { it.selected }.map { it.app.packageName })
        }
    }

    fun endEarly() { viewModelScope.launch { focus.endEarly() } }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusBreakApp
                FocusViewModel(app.container.monitoredApps, app.container.focus)
            }
        }
    }
}
