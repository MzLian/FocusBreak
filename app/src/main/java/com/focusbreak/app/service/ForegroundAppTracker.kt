package com.focusbreak.app.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Detection(val packageName: String, val atMs: Long)

/**
 * Estado compartido entre el servicio de accesibilidad y la app.
 * current: app en primer plano ahora (incluida FocusBreak). last: última app AJENA detectada.
 */
object ForegroundAppTracker {
    private val _current = MutableStateFlow<String?>(null)
    val current: StateFlow<String?> = _current.asStateFlow()

    private val _last = MutableStateFlow<Detection?>(null)
    val last: StateFlow<Detection?> = _last.asStateFlow()

    fun onForeground(packageName: String, isOwnApp: Boolean) {
        _current.value = packageName
        if (!isOwnApp) _last.value = Detection(packageName, System.currentTimeMillis())
    }

    fun clear() { _current.value = null }
}
