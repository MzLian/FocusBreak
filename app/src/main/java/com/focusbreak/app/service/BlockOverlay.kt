package com.focusbreak.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.focusbreak.app.ui.overlay.BlockScreen
import com.focusbreak.app.ui.overlay.BlockUiState
import com.focusbreak.app.ui.theme.FocusBreakTheme

/**
 * Dibuja la pantalla de bloqueo sobre la app en uso.
 * Usa TYPE_APPLICATION_OVERLAY si el usuario concedió "Mostrar sobre otras apps"; si no,
 * TYPE_ACCESSIBILITY_OVERLAY, que un servicio de accesibilidad puede usar sin ese permiso.
 */
class BlockOverlay(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val state = mutableStateOf<BlockUiState?>(null)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    fun show(ui: BlockUiState, onWalk: () -> Unit, onExtend: () -> Unit, onFinishBreathing: () -> Unit) {
        state.value = ui                      // si ya está visible, solo se actualizan los datos
        if (view != null) return

        val lifecycleOwner = OverlayLifecycleOwner().also { it.create(); it.resume() }
        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                FocusBreakTheme {
                    state.value?.let { BlockScreen(it, onWalk, onExtend, onFinishBreathing) }
                }
            }
        }
        try {
            windowManager.addView(composeView, layoutParams())
            view = composeView
            owner = lifecycleOwner
            Log.d("FocusBreak", "Overlay de bloqueo mostrado")
        } catch (e: Exception) {
            Log.e("FocusBreak", "No se pudo mostrar el overlay", e)
            lifecycleOwner.destroy()
            state.value = null
        }
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        owner?.destroy()
        view = null
        owner = null
        state.value = null
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Settings.canDrawOverlays(service)) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
    }
}
