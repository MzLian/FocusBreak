package com.focusbreak.app.domain

/** Meta diaria de pasos (valor inicial; más adelante será configurable). */
object StepGoal {
    const val STEPS = 6_000
}

/** Estado guardado del contador: el sensor entrega pasos acumulados desde el último reinicio. */
data class StepState(
    val lastCounter: Float? = null,
    val lastDate: String? = null,   // yyyy-MM-dd de la última lectura
    val stepsToday: Int = 0,
)

object StepMath {
    /**
     * Aplica una nueva lectura del contador:
     * - primera lectura: se empieza a contar desde cero;
     * - lectura menor que la anterior: el teléfono se reinició, y el contador vuelve a empezar;
     * - cambio de día: los pasos se reinician. Los pasos entre dos lecturas se asignan al día de la lectura más reciente.
     */
    fun apply(state: StepState, counter: Float, today: String): StepState {
        val last = state.lastCounter ?: return StepState(counter, today, 0)
        val delta = if (counter >= last) (counter - last).toInt() else counter.toInt()
        val base = if (state.lastDate == today) state.stepsToday else 0
        return StepState(counter, today, base + delta)
    }
}
