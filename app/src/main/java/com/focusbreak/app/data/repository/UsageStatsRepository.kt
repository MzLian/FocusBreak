package com.focusbreak.app.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.focusbreak.app.domain.RawEvent
import com.focusbreak.app.domain.UsageAggregation
import com.focusbreak.app.domain.UsageAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Uso real leído de UsageStatsManager (requiere el acceso al uso concedido en el onboarding). */
class UsageStatsRepository(private val context: Context) : UsageRepository {
    private val usm = context.getSystemService(UsageStatsManager::class.java)

    override fun today(): Flow<TodayUsage> = flow {
        while (true) {
            emit(compute())
            delay(REFRESH_MS)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun todayPerAppMs(): Map<String, Long> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        aggregateRangeBlocking(startOfDay(0), now).perAppMs
    }

    /** Agrega los eventos de [startMs, endMs): tiempo por app, por hora y desbloqueos. */
    suspend fun aggregateRange(startMs: Long, endMs: Long): UsageAggregation =
        withContext(Dispatchers.IO) { aggregateRangeBlocking(startMs, endMs) }

    private fun aggregateRangeBlocking(startMs: Long, endMs: Long): UsageAggregation = try {
        UsageAggregator.aggregate(readEvents(startMs, endMs), endMs, excludedPackages())
    } catch (e: SecurityException) {
        UsageAggregation(emptyMap(), 0)
    }

    private fun startOfDay(daysAgo: Long): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).minusDays(daysAgo).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun compute(): TodayUsage {
        val now = System.currentTimeMillis()
        val startToday = startOfDay(0)
        val today = aggregateRangeBlocking(startToday, now)
        val yesterday = aggregateRangeBlocking(startOfDay(1), startToday)
        return TodayUsage(today.totalMinutes, yesterday.totalMinutes, today.unlocks, today.perAppMinutes)
    }

    private fun readEvents(from: Long, to: Long): List<RawEvent> {
        val events = usm.queryEvents(from, to)
        val out = ArrayList<RawEvent>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            out += RawEvent(e.packageName, e.eventType, e.timeStamp)
        }
        return out
    }

    /** Se excluyen el launcher, la UI del sistema y la propia app, como hace Bienestar Digital. */
    private fun excludedPackages(): Set<String> {
        val home = context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )?.activityInfo?.packageName
        return setOfNotNull(home, "com.android.systemui", context.packageName)
    }

    private companion object {
        const val REFRESH_MS = 15_000L
    }
}
