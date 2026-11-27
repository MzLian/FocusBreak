package com.focusbreak.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.focusbreak.app.FocusBreakApp
import com.focusbreak.app.data.db.MonitoredApp
import com.focusbreak.app.data.repository.InstalledApp
import com.focusbreak.app.data.repository.InstalledAppsProvider
import com.focusbreak.app.data.repository.MonitoredAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repo: MonitoredAppsRepository,
    private val installedProvider: InstalledAppsProvider,
) : ViewModel() {

    val monitored: StateFlow<List<MonitoredApp>> =
        repo.apps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val installed = MutableStateFlow<List<InstalledApp>>(emptyList())

    /** Apps instaladas que todavía no están monitoreadas. */
    val candidates: StateFlow<List<InstalledApp>> =
        combine(installed, monitored) { all, mon ->
            val taken = mon.map { it.packageName }.toSet()
            all.filter { it.packageName !in taken }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun loadInstalled() {
        viewModelScope.launch { installed.value = installedProvider.launchableApps() }
    }

    fun add(app: InstalledApp) = viewModelScope.launch { repo.add(app.packageName, app.label) }
    fun setLimit(pkg: String, minutes: Int?) = viewModelScope.launch { repo.setLimit(pkg, minutes) }
    fun setEnabled(pkg: String, enabled: Boolean) = viewModelScope.launch { repo.setEnabled(pkg, enabled) }
    fun remove(pkg: String) = viewModelScope.launch { repo.remove(pkg) }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusBreakApp
                ProfileViewModel(app.container.monitoredApps, app.container.installedApps)
            }
        }
    }
}
