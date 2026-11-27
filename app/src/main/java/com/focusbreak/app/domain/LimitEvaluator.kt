package com.focusbreak.app.domain

enum class LimitLevel { NONE, SOFT, FIRM, BLOCK }

/** Decide en qué nivel de intervención está una app según su uso y su límite diario. */
object LimitEvaluator {
    const val SOFT_PERCENT = 50
    const val FIRM_PERCENT = 80
    const val EXTENSION_MINUTES = 5
    const val MAX_EXTENSIONS = 2     // extensiones por app y por día

    /** extraMinutes: minutos extra ya concedidos hoy a esa app (se suman al límite). */
    fun level(usedMs: Long, limitMinutes: Int?, extraMinutes: Int = 0): LimitLevel {
        if (limitMinutes == null) return LimitLevel.NONE
        val limitMs = (limitMinutes + extraMinutes) * 60_000L
        return when {
            usedMs >= limitMs -> LimitLevel.BLOCK
            usedMs * 100 >= limitMs * FIRM_PERCENT -> LimitLevel.FIRM
            usedMs * 100 >= limitMs * SOFT_PERCENT -> LimitLevel.SOFT
            else -> LimitLevel.NONE
        }
    }
}
