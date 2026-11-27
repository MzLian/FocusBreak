package com.focusbreak.app.domain

/** Tipos de intervención que FocusBreak registra (base para la evaluación del objetivo específico 6). */
enum class InterventionType {
    WARNING_SOFT,       // aviso al 50 %
    WARNING_FIRM,       // aviso al 80 %
    BLOCK,              // el overlay de bloqueo por límite apareció (una vez por bloqueo)
    WALK,               // tocó "Salir a caminar"
    BREATHING_DONE,     // terminó la pausa de respiración
    EXTENSION,          // pidió minutos extra (value = minutos)
    FOCUS_START,        // inició una sesión de concentración (value = minutos planificados)
    FOCUS_COMPLETED,    // la sesión terminó completa (value = minutos)
    FOCUS_CANCELLED,    // terminó la sesión antes de tiempo (value = minutos cumplidos)
    FOCUS_BLOCK,        // se bloqueó una app durante la sesión (una vez por bloqueo)
    STEPS_REDEEMED,     // canjeó pasos por minutos extra (value = minutos)
}

/** Niveles ya avisados hoy para una app, guardados como texto "SOFT,FIRM". */
object NotifiedLevels {
    fun contains(csv: String, level: LimitLevel): Boolean =
        csv.split(',').any { it == level.name }

    fun add(csv: String, level: LimitLevel): String =
        if (contains(csv, level)) csv else if (csv.isEmpty()) level.name else "$csv,${level.name}"
}
