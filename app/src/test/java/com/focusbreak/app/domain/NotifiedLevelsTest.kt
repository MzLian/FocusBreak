package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotifiedLevelsTest {
    @Test fun textoVacioNoTieneNingunNivel() {
        assertFalse(NotifiedLevels.contains("", LimitLevel.SOFT))
    }

    @Test fun agregarNivelesLosConserva() {
        var csv = NotifiedLevels.add("", LimitLevel.SOFT)
        csv = NotifiedLevels.add(csv, LimitLevel.FIRM)
        assertEquals("SOFT,FIRM", csv)
        assertTrue(NotifiedLevels.contains(csv, LimitLevel.SOFT))
        assertTrue(NotifiedLevels.contains(csv, LimitLevel.FIRM))
        assertFalse(NotifiedLevels.contains(csv, LimitLevel.BLOCK))
    }

    @Test fun agregarUnNivelRepetidoNoLoDuplica() {
        val csv = NotifiedLevels.add("SOFT", LimitLevel.SOFT)
        assertEquals("SOFT", csv)
    }
}
