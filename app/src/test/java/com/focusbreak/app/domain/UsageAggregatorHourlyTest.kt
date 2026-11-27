package com.focusbreak.app.domain

import com.focusbreak.app.domain.UsageAggregator.ACTIVITY_PAUSED
import com.focusbreak.app.domain.UsageAggregator.ACTIVITY_RESUMED
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class UsageAggregatorHourlyTest {
    private val min = 60_000L
    private val hour = 60 * min

    @Test fun repartePorHoraUnaSesionQueCruzaLaHora() {
        // 08:30 a 09:45 UTC -> 30 min en la hora 8 y 45 min en la hora 9
        val start = 8 * hour + 30 * min
        val r = UsageAggregator.aggregate(
            listOf(RawEvent("a", ACTIVITY_RESUMED, start), RawEvent("a", ACTIVITY_PAUSED, 9 * hour + 45 * min)),
            endMs = 12 * hour, zone = ZoneOffset.UTC
        )
        assertEquals(30 * min, r.hourlyMs[8])
        assertEquals(45 * min, r.hourlyMs[9])
        assertEquals(0L, r.hourlyMs[10])
        assertEquals(75, r.totalMinutes)
    }
}
