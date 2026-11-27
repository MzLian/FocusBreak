package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusRulesTest {
    @Test fun lasDuracionesPermitidasSonLasDelInforme() {
        assertEquals(listOf(15, 25, 45, 60), FocusRules.DURATIONS)
        assertTrue(FocusRules.DEFAULT_MINUTES in FocusRules.DURATIONS)
    }

    @Test fun laHoraDeFinSumaLosMinutes() {
        assertEquals(1_000L + 25 * 60_000L, FocusRules.endMs(1_000L, 25))
    }

    @Test fun elTiempoRestanteNuncaEsNegativoYLaSesionVence() {
        val end = FocusRules.endMs(0L, 15)
        assertEquals(15 * 60_000L, FocusRules.remainingMs(end, 0L))
        assertEquals(0L, FocusRules.remainingMs(end, end + 5_000L))
        assertFalse(FocusRules.isExpired(end, end - 1))
        assertTrue(FocusRules.isExpired(end, end))
    }

    @Test fun losMinutosCumplidosNoPasanDeLosPlanificados() {
        assertEquals(0, FocusRules.elapsedMinutes(0L, 25, 59_000L))
        assertEquals(10, FocusRules.elapsedMinutes(0L, 25, 10 * 60_000L + 30_000L))
        assertEquals(25, FocusRules.elapsedMinutes(0L, 25, 90 * 60_000L))
    }

    @Test fun elBloqueoSoloAfectaALasAppsDeLaSesion() {
        val csv = FocusRules.toCsv(listOf("com.tiktok", "com.instagram"))
        assertEquals("com.instagram,com.tiktok", csv)
        assertTrue(FocusRules.blocks(csv, "com.tiktok"))
        assertFalse(FocusRules.blocks(csv, "com.whatsapp"))
        assertFalse(FocusRules.blocks("", "com.tiktok"))
    }

    @Test fun elRelojSeFormateaConMinutosYSegundos() {
        assertEquals("25:00", FocusRules.formatClock(25 * 60_000L))
        assertEquals("00:01", FocusRules.formatClock(1L))
        assertEquals("00:00", FocusRules.formatClock(0L))
        assertEquals("59:59", FocusRules.formatClock(59 * 60_000L + 59_000L))
    }
}
