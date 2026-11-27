package com.focusbreak.app.domain

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Evento mínimo de UsageStats (se separa de la API de Android para poder probarlo con JUnit). */
data class RawEvent(val packageName: String?, val type: Int, val timeMs: Long)

data class UsageAggregation(
    val perAppMs: Map<String, Long>,
    val unlocks: Int,
    /** Milisegundos de uso en cada hora del día (0..23). */
    val hourlyMs: List<Long> = List(24) { 0L },
) {
    val perAppMinutes: Map<String, Int> get() = perAppMs.mapValues { (it.value / 60_000).toInt() }
    val totalMinutes: Int get() = (perAppMs.values.sum() / 60_000).toInt()
}

/**
 * Convierte la secuencia de eventos de UsageStatsManager en tiempo de uso por app y por hora.
 * Una app está "en uso" desde ACTIVITY_RESUMED hasta ACTIVITY_PAUSED, o hasta que se apaga la pantalla.
 */
object UsageAggregator {
    // Valores de UsageEvents.Event (API 29+ / 28+)
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_HIDDEN = 18

    fun aggregate(
        events: List<RawEvent>,
        endMs: Long,
        excluded: Set<String> = emptySet(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): UsageAggregation {
        val open = HashMap<String, Long>()
        val totals = HashMap<String, Long>()
        val hours = LongArray(24)
        var unlocks = 0

        fun addToHours(start: Long, end: Long) {
            var t = start
            while (t < end) {
                val at = Instant.ofEpochMilli(t).atZone(zone)
                val nextHour = at.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
                val segmentEnd = minOf(end, nextHour)
                hours[at.hour] += segmentEnd - t
                t = segmentEnd
            }
        }

        fun close(pkg: String, at: Long) {
            val start = open.remove(pkg) ?: return
            if (at > start) {
                totals[pkg] = (totals[pkg] ?: 0L) + (at - start)
                addToHours(start, at)
            }
        }

        for (e in events.sortedBy { it.timeMs }) {
            when (e.type) {
                ACTIVITY_RESUMED -> e.packageName?.takeIf { it !in excluded }?.let { open.putIfAbsent(it, e.timeMs) }
                ACTIVITY_PAUSED -> e.packageName?.let { close(it, e.timeMs) }
                SCREEN_NON_INTERACTIVE -> open.keys.toList().forEach { close(it, e.timeMs) }
                KEYGUARD_HIDDEN -> unlocks++
            }
        }
        open.keys.toList().forEach { close(it, endMs) }   // sesiones aún abiertas al momento de consultar
        return UsageAggregation(totals, unlocks, hours.toList())
    }
}
