package com.focusbreak.app.domain

/** Reglas de dominio de los límites diarios (validaciones del informe, cap. VII). */
object LimitRules {
    const val MIN = 5
    const val MAX = 480
    const val STEP = 5
    const val DEFAULT = 30

    /** Ajusta cualquier valor al rango permitido. */
    fun coerce(minutes: Int): Int = minutes.coerceIn(MIN, MAX)

    /** Texto legible: "Sin límite", "45 min", "2 h", "1 h 30 min". */
    fun format(minutes: Int?): String = when {
        minutes == null -> "Sin límite"
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }
}
