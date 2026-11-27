package com.focusbreak.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.focusbreak.app.AppContainer
import com.focusbreak.app.data.db.FocusSession
import com.focusbreak.app.domain.FocusRules
import com.focusbreak.app.domain.InterventionType
import com.focusbreak.app.domain.LimitEvaluator
import com.focusbreak.app.domain.LimitLevel
import com.focusbreak.app.domain.StepRedeem
import com.focusbreak.app.notifications.Notifier
import com.focusbreak.app.ui.overlay.BlockUiState
import com.focusbreak.app.ui.overlay.FocusBlockUiState
import com.focusbreak.app.ui.overlay.RedeemUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Motor de bloqueo. Cada vez que cambia la app en primer plano, y cada 15 s mientras sigue abierta, decide:
 *  1) Sesión de concentración activa (HU10): si la app está entre las elegidas, la bloquea hasta que termine.
 *  2) Si no, límites diarios (HU07 y HU08): nada, aviso suave (50 %), aviso firme (80 %) o bloqueo (100 %).
 * Las extensiones, los minutos extra y los avisos ya enviados se guardan en Room (DailyStateRepository);
 * cada intervención queda registrada en InterventionRepository.
 */
class LimitEngine(
    private val service: AccessibilityService,
    private val scope: CoroutineScope,
    private val container: AppContainer,
) {
    private val overlay = BlockOverlay(service)
    private val notifier = Notifier(service)
    private val dailyState = container.dailyState
    private val interventions = container.interventions

    private val redeemMutex = Mutex()     // evita gastar dos veces los mismos pasos con un doble toque
    private var currentPackage: String? = null
    private var day = LocalDate.now()
    private var evalJob: Job? = null
    private val tickJob: Job = scope.launch {
        while (isActive) {
            delay(TICK_MS)
            // cierra la sesión de concentración si ya venció, aunque no haya una app en primer plano evaluable
            try {
                container.focus.finishIfExpired()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar la sesión de concentración", e)
            }
            currentPackage?.let { safeEvaluate(it) }
        }
    }

    init {
        launchSafe { dailyState.purgeOldDays() }
    }

    fun onForeground(packageName: String) {
        currentPackage = packageName
        reevaluate(packageName)
    }

    /**
     * La persona abrió FocusBreak. Se olvida la app anterior para que el tick de 15 s no dibuje
     * un bloqueo encima de la propia app (por ejemplo, al iniciar una sesión desde Concentración).
     */
    fun onOwnActivityOpened() {
        currentPackage = null
        evalJob?.cancel()
        overlay.hide()
    }

    fun shutdown() {
        tickJob.cancel()
        evalJob?.cancel()
        overlay.hide()
    }

    private fun reevaluate(pkg: String) {
        evalJob?.cancel()
        evalJob = scope.launch { safeEvaluate(pkg) }
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

    /** Lanza trabajo secundario (guardar en Room) sin que un error cierre el servicio. */
    private fun launchSafe(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar el estado", e)
            }
        }
    }

    private suspend fun evaluate(pkg: String) {
        rollDayIfNeeded()

        // 1) Sesión de concentración: tiene prioridad sobre los límites diarios
        val session = container.focus.activeNow()
        if (session != null && FocusRules.blocks(session.apps, pkg)) {
            blockForFocus(session, pkg)
            return
        }

        // 2) Límites diarios
        val app = container.monitoredApps.apps.first()
            .firstOrNull { it.packageName == pkg && it.enabled && it.dailyLimitMinutes != null }
        if (app == null) {
            Log.d(TAG, "evaluar $pkg: no está monitoreada, está desactivada o no tiene límite")
            overlay.hide(); return
        }

        val limit = app.dailyLimitMinutes ?: return
        val state = dailyState.get(pkg)
        val extra = state.extraMinutes
        val usedMs = container.usage.todayPerAppMs()[pkg] ?: 0L
        val usedMin = (usedMs / 60_000).toInt()

        val level = LimitEvaluator.level(usedMs, limit, extra)
        Log.d(TAG, "evaluar $pkg: usado=${usedMs / 1000}s, límite=${limit}min, extra=${extra}min -> $level")
        when (level) {
            LimitLevel.NONE -> overlay.hide()
            LimitLevel.SOFT, LimitLevel.FIRM -> {
                overlay.hide()
                // un solo aviso por app, nivel y día (persistido: no se repite aunque el servicio se reinicie)
                if (dailyState.markNotified(pkg, level)) {
                    notifier.showLimitAlert(app.label, level, usedMin, limit + extra)
                    val type = if (level == LimitLevel.SOFT) InterventionType.WARNING_SOFT else InterventionType.WARNING_FIRM
                    interventions.log(type, pkg, usedMin)
                }
            }
            LimitLevel.BLOCK -> {
                val wasShowing = overlay.isShowing
                val redeem = redeemUi()
                // al bloquear se refrescan los pasos para que el siguiente ciclo (15 s) muestre el valor al día
                if (!wasShowing && container.steps.canTrack) launchSafe { container.steps.sync() }
                overlay.show(
                    BlockUiState(
                        packageName = pkg,
                        appLabel = app.label,
                        usedMinutes = usedMin,
                        limitMinutes = limit + extra,
                        canExtend = state.extensions < LimitEvaluator.MAX_EXTENSIONS,
                        redeem = redeem,
                    ),
                    onWalk = { launchSafe { interventions.log(InterventionType.WALK, pkg) }; goHome() },
                    onExtend = { grantExtension(pkg) },
                    onFinishBreathing = { launchSafe { interventions.log(InterventionType.BREATHING_DONE, pkg) }; goHome() },
                    onRedeem = { redeemSteps(pkg) },
                )
                // se registra una sola vez por bloqueo (el motor vuelve a evaluar cada 15 s con el overlay visible)
                if (!wasShowing && overlay.isShowing) interventions.log(InterventionType.BLOCK, pkg, usedMin)
            }
        }
    }

    /** Estado del canje de pasos para la pantalla de bloqueo; null si no hay sensor o permiso. */
    private suspend fun redeemUi(): RedeemUi? {
        if (!container.steps.canTrack) return null
        val steps = container.steps.stepsToday.first()
        val redeemed = interventions.sumToday(InterventionType.STEPS_REDEEMED)
        return RedeemUi(
            stepsAvailable = StepRedeem.stepsAvailable(steps, redeemed),
            canRedeem = StepRedeem.nextRedeemMinutes(steps, redeemed) > 0,
            capReached = StepRedeem.capReached(redeemed),
        )
    }

    /** Canjea un bloque de pasos por minutos extra para esta app (HU13). Lee el sensor justo antes de gastar. */
    private fun redeemSteps(pkg: String) {
        launchSafe {
            redeemMutex.withLock {
                container.steps.sync()
                val steps = container.steps.stepsToday.first()
                val redeemed = interventions.sumToday(InterventionType.STEPS_REDEEMED)
                val minutes = StepRedeem.nextRedeemMinutes(steps, redeemed)
                if (minutes > 0) {
                    dailyState.addExtraMinutes(pkg, minutes)
                    interventions.log(InterventionType.STEPS_REDEEMED, pkg, minutes)
                    overlay.hide()
                }
            }
            reevaluate(pkg)
        }
    }

    /** Bloquea la app durante la sesión de concentración. */
    private suspend fun blockForFocus(session: FocusSession, pkg: String) {
        val label = container.monitoredApps.apps.first().firstOrNull { it.packageName == pkg }?.label
            ?: container.installedApps.labelOf(pkg)
            ?: pkg
        val wasShowingFocus = overlay.isShowingFocus
        overlay.showFocus(
            FocusBlockUiState(packageName = pkg, appLabel = label, endMs = session.endMs),
            onHome = ::goHome,
            onEnd = { launchSafe { container.focus.endEarly(); reevaluate(pkg) } },
            onExpired = { reevaluate(pkg) },
        )
        if (!wasShowingFocus && overlay.isShowingFocus) interventions.log(InterventionType.FOCUS_BLOCK, pkg)
    }

    /** "Salir a caminar" / fin de la pausa / "Volver al inicio": manda al usuario a la pantalla de inicio del teléfono. */
    private fun goHome() {
        overlay.hide()
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    private fun grantExtension(pkg: String) {
        launchSafe {
            dailyState.addExtension(pkg, LimitEvaluator.EXTENSION_MINUTES)
            interventions.log(InterventionType.EXTENSION, pkg, LimitEvaluator.EXTENSION_MINUTES)
            overlay.hide()
            reevaluate(pkg)
        }
    }

    /** Las filas de Room llevan la fecha, así que al cambiar de día basta con borrar las antiguas. */
    private suspend fun rollDayIfNeeded() {
        val today = LocalDate.now()
        if (today != day) {
            day = today
            dailyState.purgeOldDays()
        }
    }

    private companion object {
        const val TAG = "FocusBreak"
        const val TICK_MS = 15_000L
    }
}
