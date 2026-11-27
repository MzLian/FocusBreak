package com.focusbreak.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.focusbreak.app.FocusBreakApp
import com.focusbreak.app.data.db.MonitoredApp
import com.focusbreak.app.data.repository.InstalledAppsProvider
import com.focusbreak.app.data.repository.InterventionRepository
import com.focusbreak.app.data.repository.MonitoredAppsRepository
import com.focusbreak.app.data.repository.StepsRepository
import com.focusbreak.app.data.repository.UsageRepository
import com.focusbreak.app.domain.InterventionType
import com.focusbreak.app.domain.StepRedeem
import com.focusbreak.app.service.ForegroundAppTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppUsageItem(val app: MonitoredApp, val usedMinutes: Int)

data class HomeUiState(
    val totalMinutes: Int = 0,
    val yesterdayMinutes: Int = 0,
    val unlocks: Int = 0,
    val topAppLabel: String? = null,
    val items: List<AppUsageItem> = emptyList(),
    /** Última app ajena detectada por el servicio de accesibilidad (evidencia de HU05). */
    val lastDetectedLabel: String? = null,
    val lastDetectedTime: String? = null,
    val steps: Int = 0,
    val stepSensorAvailable: Boolean = true,
    /** Pasos de hoy aún sin canjear y minutos ganados hoy con pasos (HU13). */
    val stepsAvailable: Int = 0,
    val redeemedMinutes: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val monitoredRepo: MonitoredAppsRepository,
    usageRepo: UsageRepository,
    private val installed: InstalledAppsProvider,
    private val stepsRepo: StepsRepository,
    interventions: InterventionRepository,
) : ViewModel() {

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    private val today = flow { while (true) { emit(LocalDate.now()); delay(60_000) } }.distinctUntilChanged()
    private val redeemedToday = today.flatMapLatest { interventions.observeSum(it, InterventionType.STEPS_REDEEMED) }

    val uiState: StateFlow<HomeUiState> =
        combine(monitoredRepo.apps, usageRepo.today(), ForegroundAppTracker.last, stepsRepo.stepsToday, redeemedToday) { apps, usage, last, steps, redeemed ->
            val items = apps.map { AppUsageItem(it, usage.perApp[it.packageName] ?: 0) }
            HomeUiState(
                totalMinutes = usage.totalMinutes,
                yesterdayMinutes = usage.yesterdayMinutes,
                unlocks = usage.unlocks,
                topAppLabel = items.filter { it.usedMinutes > 0 }.maxByOrNull { it.usedMinutes }?.app?.label,
                items = items,
                lastDetectedLabel = last?.let { installed.labelOf(it.packageName) ?: it.packageName },
                lastDetectedTime = last?.let { timeFormat.format(Instant.ofEpochMilli(it.atMs)) },
                steps = steps,
                stepSensorAvailable = stepsRepo.hasSensor,
                stepsAvailable = StepRedeem.stepsAvailable(steps, redeemed),
                redeemedMinutes = redeemed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Mientras Inicio está abierta, los pasos se refrescan cada 20 s
        viewModelScope.launch {
            while (isActive) {
                stepsRepo.sync()
                delay(20_000)
            }
        }
    }

    fun syncSteps() {
        viewModelScope.launch { stepsRepo.sync() }
    }

    fun setEnabled(pkg: String, enabled: Boolean) {
        viewModelScope.launch { monitoredRepo.setEnabled(pkg, enabled) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusBreakApp
                HomeViewModel(
                    app.container.monitoredApps, app.container.usage, app.container.installedApps,
                    app.container.steps, app.container.interventions,
                )
            }
        }
    }
}
