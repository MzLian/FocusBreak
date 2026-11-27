package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StepMathTest {
    private val day1 = "2026-11-26"
    private val day2 = "2026-11-27"

    @Test fun primeraLecturaEmpiezaEnCero() {
        val s = StepMath.apply(StepState(), 5_000f, day1)
        assertEquals(0, s.stepsToday)
        assertEquals(5_000f, s.lastCounter)
    }

    @Test fun acumulaLosPasosDelMismoDia() {
        var s = StepMath.apply(StepState(), 5_000f, day1)
        s = StepMath.apply(s, 5_300f, day1)
        s = StepMath.apply(s, 5_800f, day1)
        assertEquals(800, s.stepsToday)
    }

    @Test fun alCambiarDeDiaLosPasosSeReinician() {
        var s = StepMath.apply(StepState(), 1_000f, day1)
        s = StepMath.apply(s, 3_000f, day1)          // 2000 hoy
        s = StepMath.apply(s, 3_150f, day2)          // día nuevo: solo cuentan los 150 de esta lectura
        assertEquals(150, s.stepsToday)
        assertEquals(day2, s.lastDate)
    }

    @Test fun siElTelefonoSeReiniciaElContadorVuelveACero() {
        var s = StepMath.apply(StepState(), 9_000f, day1)
        s = StepMath.apply(s, 9_500f, day1)          // 500
        s = StepMath.apply(s, 120f, day1)            // reinicio: el contador empezó de nuevo en 0
        assertEquals(620, s.stepsToday)
    }
}
