package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageMathTest {
    @Test fun percentChange_calculaVariacionFrenteAAyer() {
        assertEquals(-20, UsageMath.percentChange(80, 100))
        assertEquals(25, UsageMath.percentChange(125, 100))
    }

    @Test fun percentChange_sinDatosDeAyerEsNull() {
        assertNull(UsageMath.percentChange(50, 0))
    }

    @Test fun progress_conLimiteYSinLimite() {
        assertEquals(0.5f, UsageMath.progress(30, 60), 0.001f)
        assertEquals(1f, UsageMath.progress(90, 60), 0.001f)           // se limita a 1
        assertEquals(0.5f, UsageMath.progress(150, null), 0.001f)      // objetivo diario = 300
    }

    @Test fun formatDuration_noMuestraNegativos() {
        assertEquals("0 min", formatDuration(-5))
        assertEquals("3 h 12 min", formatDuration(192))
    }
}
