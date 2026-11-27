package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepRedeemTest {
    @Test fun losPasosGastadosSonBloquesCompletos() {
        assertEquals(0, StepRedeem.stepsSpent(0))
        assertEquals(1_000, StepRedeem.stepsSpent(10))
        assertEquals(3_000, StepRedeem.stepsSpent(30))
    }

    @Test fun losPasosDisponiblesDescuentanLoYaCanjeado() {
        assertEquals(1_500, StepRedeem.stepsAvailable(2_500, 10))
        assertEquals(0, StepRedeem.stepsAvailable(500, 10))   // nunca negativo
    }

    @Test fun sinPasosSuficientesNoSeCanjea() {
        assertEquals(0, StepRedeem.nextRedeemMinutes(999, 0))
        assertEquals(StepRedeem.MINUTES_PER_BLOCK, StepRedeem.nextRedeemMinutes(1_000, 0))
    }

    @Test fun elCanjeRespetaLoYaGastadoHoy() {
        // 3.000 pasos, 10 min ya canjeados: quedan 2.000 pasos, se puede otro bloque
        assertEquals(10, StepRedeem.nextRedeemMinutes(3_000, 10))
        // 1.500 pasos con 10 min canjeados: solo quedan 500, no alcanza
        assertEquals(0, StepRedeem.nextRedeemMinutes(1_500, 10))
    }

    @Test fun elTopeDiarioImpideMasCanjes() {
        assertFalse(StepRedeem.capReached(20))
        assertTrue(StepRedeem.capReached(StepRedeem.DAILY_CAP_MINUTES))
        assertEquals(10, StepRedeem.nextRedeemMinutes(5_000, 20))
        assertEquals(0, StepRedeem.nextRedeemMinutes(9_000, 30))
    }
}
