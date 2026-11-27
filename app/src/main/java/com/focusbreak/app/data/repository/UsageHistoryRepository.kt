package com.focusbreak.app.data.repository

import com.focusbreak.app.data.db.DailyAppUsage
import com.focusbreak.app.data.db.DailySummary
import com.focusbreak.app.data.db.UsageHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Historial diario de uso. Android solo conserva los eventos unos 7 a 10 días, así que se guarda
 * una copia en Room: hoy y ayer se recalculan siempre; los días anteriores se rellenan una sola vez.
 */
class UsageHistoryRepository(
    private val usageStats: UsageStatsRepository,
    private val dao: UsageHistoryDao,
) {
    fun observe(from: LocalDate, to: LocalDate): Flow<Pair<List<DailyAppUsage>, List<DailySummary>>> =
        combine(
            dao.observeApps(from.toString(), to.toString()),
            dao.observeSummaries(from.toString(), to.toString()),
        ) { apps, summaries -> apps to summaries }

    suspend fun sync(days: Int = 7) = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        val existing = dao.summaryDates(today.minusDays(days - 1L).toString(), today.toString()).toSet()

        for (i in 0 until days) {
            val date = today.minusDays(i.toLong())
            val key = date.toString()
            if (i >= 2 && key in existing) continue                      // día ya guardado
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = if (i == 0) now else date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            val agg = usageStats.aggregateRange(start, end)
            val empty = agg.perAppMs.isEmpty() && agg.unlocks == 0
            if (empty && key in existing) continue                       // no pisar datos buenos con vacío

            val rows = agg.perAppMs
                .map { (pkg, ms) -> DailyAppUsage(key, pkg, (ms / 60_000.0).roundToInt()) }
                .filter { it.minutes > 0 }
            val hourly = agg.hourlyMs.joinToString(",") { (it / 60_000.0).roundToInt().toString() }
            dao.replaceDay(key, rows, DailySummary(key, agg.unlocks, hourly))
        }
    }
}
