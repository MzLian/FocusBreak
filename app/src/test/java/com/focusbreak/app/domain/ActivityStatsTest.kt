package com.focusbreak.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ActivityStatsTest {
    @Test fun weekDays_vaDeLunesADomingo() {
        val days = ActivityStats.weekDays(LocalDate.of(2026, 11, 26)) // jueves
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 23), days.first())
        assertEquals(DayOfWeek.MONDAY, days.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, days.last().dayOfWeek)
    }

    @Test fun monthDays_tieneTodosLosDiasDelMes() {
        assertEquals(30, ActivityStats.monthDays(LocalDate.of(2026, 11, 5)).size)
    }

    @Test fun average_ignoraDiasFuturosYDiasSinDatos() {
        val today = LocalDate.of(2026, 11, 26)
        val days = ActivityStats.weekDays(today)
        val totals = mapOf(days[0] to 120, days[1] to 0, days[2] to 180, days[3] to 60)
        // lunes 120, martes sin datos, miércoles 180, jueves(hoy) 60 -> (120+180+60)/3 = 120
        assertEquals(120, ActivityStats.averageMinutes(totals, days, today))
        assertEquals(0, ActivityStats.averageMinutes(emptyMap(), days, today))
    }

    @Test fun bestWindow_eligeLaFranjaConMenosUso() {
        val hourly = MutableList(24) { 10 }
        hourly[9] = 0; hourly[10] = 1               // franja 09–11: casi sin uso
        assertEquals(9, ActivityStats.bestWindowStart(hourly))
    }

    @Test fun bestWindow_sinDatosEsNull() {
        assertNull(ActivityStats.bestWindowStart(List(24) { 0 }))
    }
}
