package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LimitEvaluatorTest {
    private val min = 60_000L

    @Test fun sinLimiteNuncaIntervienes() {
        assertEquals(LimitLevel.NONE, LimitEvaluator.level(500 * min, null))
    }

    @Test fun nivelesAl50_80y100PorCiento() {
        // límite de 100 min para leer los porcentajes directamente
        assertEquals(LimitLevel.NONE, LimitEvaluator.level(49 * min, 100))
        assertEquals(LimitLevel.SOFT, LimitEvaluator.level(50 * min, 100))
        assertEquals(LimitLevel.SOFT, LimitEvaluator.level(79 * min, 100))
        assertEquals(LimitLevel.FIRM, LimitEvaluator.level(80 * min, 100))
        assertEquals(LimitLevel.FIRM, LimitEvaluator.level(99 * min, 100))
        assertEquals(LimitLevel.BLOCK, LimitEvaluator.level(100 * min, 100))
        assertEquals(LimitLevel.BLOCK, LimitEvaluator.level(130 * min, 100))
    }

    @Test fun laExtensionSumaTiempoAlLimite() {
        // 45 min de uso con límite 45: bloqueo; con +5 de extensión pasa a aviso firme (90 %)
        assertEquals(LimitLevel.BLOCK, LimitEvaluator.level(45 * min, 45))
        assertEquals(LimitLevel.FIRM, LimitEvaluator.level(45 * min, 45, extraMinutes = 5))
        assertEquals(LimitLevel.BLOCK, LimitEvaluator.level(50 * min, 45, extraMinutes = 5))
    }

    @Test fun usaMilisegundosNoSoloMinutos() {
        assertEquals(LimitLevel.NONE, LimitEvaluator.level(30 * min - 1, 60))
        assertEquals(LimitLevel.SOFT, LimitEvaluator.level(30 * min, 60))
    }
}
