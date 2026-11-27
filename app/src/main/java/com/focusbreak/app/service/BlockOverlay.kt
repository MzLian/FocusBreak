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
import com.focusbreak.app.ui.overlay.FocusBlockScreen
import com.focusbreak.app.ui.overlay.FocusBlockUiState
import com.focusbreak.app.ui.theme.FocusBreakTheme

/**
 * Dibuja la pantalla de bloqueo sobre la app en uso: la de límite alcanzado (HU08) o la de sesión
 * de concentración (HU10). Usa TYPE_APPLICATION_OVERLAY si el usuario concedió "Mostrar sobre otras apps";
 * si no, TYPE_ACCESSIBILITY_OVERLAY, que un servicio de accesibilidad puede usar sin ese permiso.
 */
class BlockOverlay(private val service: AccessibilityService) {

    private sealed interface Content {
        class Limit(
            val ui: BlockUiState,
            val onWalk: () -> Unit,
            val onExtend: () -> Unit,
            val onFinishBreathing: () -> Unit,
            val onRedeem: () -> Unit,
        ) : Content

        class Focus(
            val ui: FocusBlockUiState,
            val onHome: () -> Unit,
            val onEnd: () -> Unit,
            val onExpired: () -> Unit,
        ) : Content
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val content = mutableStateOf<Content?>(null)
    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null

    /** true mientras el overlay está dibujado en pantalla. */
    val isShowing: Boolean get() = view != null

    /** true si lo que se ve es el bloqueo de una sesión de concentración. */
    val isShowingFocus: Boolean get() = view != null && content.value is Content.Focus

    fun show(
        ui: BlockUiState,
        onWalk: () -> Unit,
        onExtend: () -> Unit,
        onFinishBreathing: () -> Unit,
        onRedeem: () -> Unit,
    ) = present(Content.Limit(ui, onWalk, onExtend, onFinishBreathing, onRedeem))

    fun showFocus(ui: FocusBlockUiState, onHome: () -> Unit, onEnd: () -> Unit, onExpired: () -> Unit) =
        present(Content.Focus(ui, onHome, onEnd, onExpired))

    private fun present(newContent: Content) {
        content.value = newContent            // si ya está visible, solo se actualizan los datos
        if (view != null) return

        val lifecycleOwner = OverlayLifecycleOwner().also { it.create(); it.resume() }
        val composeView = ComposeView(service).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                FocusBreakTheme {
                    when (val c = content.value) {
                        is Content.Limit -> BlockScreen(c.ui, c.onWalk, c.onExtend, c.onFinishBreathing, c.onRedeem)
                        is Content.Focus -> FocusBlockScreen(c.ui, c.onHome, c.onEnd, c.onExpired)
                        null -> Unit
                    }
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
            content.value = null
        }
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        owner?.destroy()
        view = null
        owner = null
        content.value = null
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
