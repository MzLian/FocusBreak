package com.focusbreak.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

enum class UsageCategory(val label: String) {
    SOCIAL("Social"), VIDEO("Vídeo"), PRODUCTIVITY("Productividad"), OTHER("Otros")
}

/** Cálculos de la pantalla Actividad (puros, para poder probarlos con JUnit). */
object ActivityStats {
    /** Lunes a domingo de la semana que contiene a [ref]. */
    fun weekDays(ref: LocalDate): List<LocalDate> {
        val monday = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    fun monthDays(ref: LocalDate): List<LocalDate> =
        (1..ref.lengthOfMonth()).map { ref.withDayOfMonth(it) }

    /** Media diaria sobre los días ya transcurridos que tienen datos (los días sin datos no bajan la media). */
    fun averageMinutes(totals: Map<LocalDate, Int>, days: List<LocalDate>, today: LocalDate): Int {
        val withData = days.filter { !it.isAfter(today) && (totals[it] ?: 0) > 0 }
        if (withData.isEmpty()) return 0
        return (withData.sumOf { totals[it] ?: 0 }.toDouble() / withData.size).roundToInt()
    }

    /**
     * Hora de inicio de la franja de 2 horas con MENOR uso del teléfono entre las 07:00 y las 21:00
     * (el "momento más enfocado"). Null si no hay datos.
     */
    fun bestWindowStart(hourlyMinutes: List<Int>, fromHour: Int = 7, toHour: Int = 21): Int? {
        if (hourlyMinutes.size < 24 || hourlyMinutes.sum() == 0) return null
        var best = -1
        var bestValue = Int.MAX_VALUE
        for (h in fromHour until toHour - 1) {
            val value = hourlyMinutes[h] + hourlyMinutes[h + 1]
            if (value < bestValue) { bestValue = value; best = h }
        }
        return best
    }
}
