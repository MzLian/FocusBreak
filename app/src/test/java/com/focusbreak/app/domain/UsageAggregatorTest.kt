package com.focusbreak.app.domain

import com.focusbreak.app.domain.UsageAggregator.ACTIVITY_PAUSED
import com.focusbreak.app.domain.UsageAggregator.ACTIVITY_RESUMED
import com.focusbreak.app.domain.UsageAggregator.KEYGUARD_HIDDEN
import com.focusbreak.app.domain.UsageAggregator.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageAggregatorTest {
    private val min = 60_000L

    @Test fun sumaElTiempoEntreResumeYPause() {
        val r = UsageAggregator.aggregate(
            listOf(RawEvent("a", ACTIVITY_RESUMED, 0), RawEvent("a", ACTIVITY_PAUSED, 5 * min)), endMs = 10 * min
        )
        assertEquals(5, r.perAppMinutes["a"])
        assertEquals(5, r.totalMinutes)
    }

    @Test fun apagarLaPantallaCierraLaSesion() {
        val r = UsageAggregator.aggregate(
            listOf(RawEvent("b", ACTIVITY_RESUMED, 0), RawEvent("x", SCREEN_NON_INTERACTIVE, 3 * min)), endMs = 60 * min
        )
        assertEquals(3, r.perAppMinutes["b"])
    }

    @Test fun laSesionAbiertaSeCuentaHastaAhora() {
        val r = UsageAggregator.aggregate(listOf(RawEvent("c", ACTIVITY_RESUMED, 2 * min)), endMs = 6 * min)
        assertEquals(4, r.perAppMinutes["c"])
    }

    @Test fun ignoraPausaSinInicioYPaquetesExcluidos() {
        val r = UsageAggregator.aggregate(
            listOf(
                RawEvent("d", ACTIVITY_PAUSED, min),
                RawEvent("launcher", ACTIVITY_RESUMED, 0), RawEvent("launcher", ACTIVITY_PAUSED, 9 * min),
            ),
            endMs = 10 * min, excluded = setOf("launcher")
        )
        assertNull(r.perAppMinutes["d"])
        assertNull(r.perAppMinutes["launcher"])
        assertEquals(0, r.totalMinutes)
    }

    @Test fun variasActividadesDeUnaAppNoDuplicanTiempo() {
        val r = UsageAggregator.aggregate(
            listOf(
                RawEvent("e", ACTIVITY_RESUMED, 0), RawEvent("e", ACTIVITY_PAUSED, 2 * min),
                RawEvent("e", ACTIVITY_RESUMED, 2 * min), RawEvent("e", ACTIVITY_PAUSED, 6 * min),
            ),
            endMs = 10 * min
        )
        assertEquals(6, r.perAppMinutes["e"])
    }

    @Test fun cuentaLosDesbloqueos() {
        val r = UsageAggregator.aggregate(
            listOf(RawEvent(null, KEYGUARD_HIDDEN, 0), RawEvent(null, KEYGUARD_HIDDEN, min)), endMs = 2 * min
        )
        assertEquals(2, r.unlocks)
    }
}
