package com.focusbreak.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.focusbreak.app.AppContainer
import com.focusbreak.app.domain.LimitEvaluator
import com.focusbreak.app.domain.LimitLevel
import com.focusbreak.app.notifications.Notifier
import com.focusbreak.app.ui.overlay.BlockUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Motor de límites (HU07 y HU08): cada vez que cambia la app en primer plano, y cada 15 s mientras
 * sigue abierta, compara su uso de hoy con su límite y decide: nada, aviso suave (50 %),
 * aviso firme (80 %) o bloqueo (100 %).
 * Las extensiones y los avisos ya enviados viven en memoria y se reinician cada día.
 */
class LimitEngine(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val container: AppContainer,
) {
    private val overlay = BlockOverlay(service)
    private val notifier = Notifier(service)

    private var currentPackage: String? = null
    private var day = LocalDate.now()
    private val notified = HashSet<String>()
    private val extraMinutes = HashMap<String, Int>()
    private val extensions = HashMap<String, Int>()
    private var evalJob: Job? = null
    private val tickJob: Job = scope.launch {
        while (isActive) {
            delay(TICK_MS)
            currentPackage?.let { safeEvaluate(it) }
        }
    }

    fun onForeground(packageName: String) {
        currentPackage = packageName
        evalJob?.cancel()
        evalJob = scope.launch { safeEvaluate(packageName) }
    }

    fun shutdown() {
        tickJob.cancel()
        evalJob?.cancel()
        overlay.hide()
    }

    /** Evita que un error en una evaluación detenga el motor o cierre el servicio; lo deja en Logcat. */
    private suspend fun safeEvaluate(pkg: String) {
        try {
            evaluate(pkg)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al evaluar $pkg", e)
        }
    }

    private suspend fun evaluate(pkg: String) {
        rollDayIfNeeded()
        val app = container.monitoredApps.apps.first()
            .firstOrNull { it.packageName == pkg && it.enabled && it.dailyLimitMinutes != null }
        if (app == null) {
            Log.d(TAG, "evaluar $pkg: no está monitoreada, está desactivada o no tiene límite")
            overlay.hide(); return
        }

        val limit = app.dailyLimitMinutes ?: return
        val extra = extraMinutes[pkg] ?: 0
        val usedMs = container.usage.todayPerAppMs()[pkg] ?: 0L
        val usedMin = (usedMs / 60_000).toInt()

        val level = LimitEvaluator.level(usedMs, limit, extra)
        Log.d(TAG, "evaluar $pkg: usado=${usedMs / 1000}s, límite=${limit}min, extra=${extra}min -> $level")
        when (level) {
            LimitLevel.NONE -> overlay.hide()
            LimitLevel.SOFT, LimitLevel.FIRM -> {
                overlay.hide()
                // un solo aviso por app, nivel y día
                if (notified.add("$pkg:$level")) notifier.showLimitAlert(app.label, level, usedMin, limit + extra)
            }
            LimitLevel.BLOCK -> overlay.show(
                BlockUiState(
                    packageName = pkg,
                    appLabel = app.label,
                    usedMinutes = usedMin,
                    limitMinutes = limit + extra,
                    canExtend = (extensions[pkg] ?: 0) < LimitEvaluator.MAX_EXTENSIONS,
                ),
                onWalk = ::goHome,
                onExtend = { grantExtension(pkg) },
                onFinishBreathing = ::goHome,
            )
        }
    }

    /** "Salir a caminar" / fin de la pausa: manda al usuario a la pantalla de inicio del teléfono. */
    private fun goHome() {
        overlay.hide()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    private fun grantExtension(pkg: String) {
        extensions[pkg] = (extensions[pkg] ?: 0) + 1
        extraMinutes[pkg] = (extraMinutes[pkg] ?: 0) + LimitEvaluator.EXTENSION_MINUTES
        overlay.hide()
        evalJob?.cancel()
        evalJob = scope.launch { safeEvaluate(pkg) }
    }

    private fun rollDayIfNeeded() {
        val today = LocalDate.now()
        if (today != day) {
            day = today
            notified.clear(); extraMinutes.clear(); extensions.clear()
        }
    }

    private companion object {
        const val TAG = "FocusBreak"
        const val TICK_MS = 15_000L
    }
}
