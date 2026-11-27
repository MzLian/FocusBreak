package com.focusbreak.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.focusbreak.app.FocusBreakApp
import com.focusbreak.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Detecta la app en primer plano y activa el motor de límites.
 * Solo recibe eventos de cambio de ventana (nombre del paquete); NO lee el contenido de la pantalla.
 */
class FocusAccessibilityService : AccessibilityService() {
    private var scope: CoroutineScope? = null
    private var engine: LimitEngine? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servicio de accesibilidad conectado")
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = newScope
        engine = LimitEngine(this, newScope, (application as FocusBreakApp).container)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (isIgnorable(pkg)) return
        Log.d(TAG, "Primer plano: $pkg")

        val own = pkg == packageName
        ForegroundAppTracker.onForeground(pkg, isOwnApp = own)
        // Las ventanas propias (incluido el overlay de bloqueo) no deben reiniciar el motor.
        if (!own) {
            engine?.onForeground(pkg)
        } else if (event.className?.toString() == MainActivity::class.java.name) {
            // La persona abrió FocusBreak: el motor deja de evaluar la app anterior
            engine?.onOwnActivityOpened()
        }
    }

    /** El teclado y la UI del sistema abren ventanas propias que no son la app que usa la persona. */
    private fun isIgnorable(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg == defaultKeyboardPackage()

    private fun defaultKeyboardPackage(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
            ?.let { ComponentName.unflattenFromString(it)?.packageName }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private fun shutdown() {
        engine?.shutdown()
        engine = null
        scope?.cancel()
        scope = null
        ForegroundAppTracker.clear()
    }

    private companion object {
        const val TAG = "FocusBreak"
    }
}
