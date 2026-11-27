package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class LimitRulesTest {
    @Test fun coerce_ajustaAlRangoPermitido() {
        assertEquals(LimitRules.MIN, LimitRules.coerce(0))
        assertEquals(LimitRules.MAX, LimitRules.coerce(9_999))
        assertEquals(45, LimitRules.coerce(45))
    }

    @Test fun format_muestraTextoLegible() {
        assertEquals("Sin límite", LimitRules.format(null))
        assertEquals("45 min", LimitRules.format(45))
        assertEquals("2 h", LimitRules.format(120))
        assertEquals("1 h 30 min", LimitRules.format(90))
    }
}
