package com.focusbreak.app.domain

/**
 * Canje de pasos por minutos adicionales de uso (HU13). Reglas puras, sin Android.
 * Se canjea por bloques (1.000 pasos = 10 min) y hay un tope diario para que la mecánica
 * no incentive el uso excesivo. Los minutos canjeados del día se leen del registro de eventos.
 */
object StepRedeem {
    const val STEPS_PER_BLOCK = 1_000
    const val MINUTES_PER_BLOCK = 10
    const val DAILY_CAP_MINUTES = 30

    /** Pasos ya gastados hoy según los minutos canjeados. */
    fun stepsSpent(redeemedMinutes: Int): Int = redeemedMinutes / MINUTES_PER_BLOCK * STEPS_PER_BLOCK

    /** Pasos de hoy que aún no se han canjeado. */
    fun stepsAvailable(stepsToday: Int, redeemedMinutes: Int): Int =
        (stepsToday - stepsSpent(redeemedMinutes)).coerceAtLeast(0)

    /** true si ya no cabe otro bloque dentro del tope diario. */
    fun capReached(redeemedMinutes: Int): Boolean = redeemedMinutes + MINUTES_PER_BLOCK > DAILY_CAP_MINUTES

    /** Minutos que se conceden con el siguiente canje (un bloque), o 0 si no se puede canjear. */
    fun nextRedeemMinutes(stepsToday: Int, redeemedMinutes: Int): Int =
        if (!capReached(redeemedMinutes) && stepsAvailable(stepsToday, redeemedMinutes) >= STEPS_PER_BLOCK) MINUTES_PER_BLOCK else 0
}
