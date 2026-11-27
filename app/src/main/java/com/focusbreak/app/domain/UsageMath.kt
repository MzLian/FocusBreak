package com.focusbreak.app.domain

import kotlin.math.roundToInt

/** Objetivo diario de pantalla (5 h, como en el mockup). Más adelante será configurable. */
object DailyGoal {
    const val MINUTES = 300
}

object UsageMath {
    /** Variación porcentual frente a ayer. null si ayer no hay datos. */
    fun percentChange(today: Int, yesterday: Int): Int? =
        if (yesterday <= 0) null else ((today - yesterday) * 100.0 / yesterday).roundToInt()

    /** Fracción 0..1 del uso respecto del límite; sin límite se compara con el objetivo diario. */
    fun progress(usedMinutes: Int, limitMinutes: Int?): Float {
        val base = (limitMinutes ?: DailyGoal.MINUTES).coerceAtLeast(1)
        return (usedMinutes.toFloat() / base).coerceIn(0f, 1f)
    }
}

fun formatDuration(minutes: Int): String = LimitRules.format(minutes.coerceAtLeast(0))
