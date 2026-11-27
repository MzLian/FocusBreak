package com.focusbreak.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.focusbreak.app.FocusBreakApp
import com.focusbreak.app.data.db.DailyAppUsage
import com.focusbreak.app.data.db.DailySummary
import com.focusbreak.app.data.repository.AppCategoryResolver
import com.focusbreak.app.data.repository.UsageHistoryRepository
import com.focusbreak.app.domain.ActivityStats
import com.focusbreak.app.domain.UsageCategory
import com.focusbreak.app.domain.UsageMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class ActivityRange { WEEK, MONTH }

data class BarUi(val label: String, val minutes: Int, val isToday: Boolean, val isFuture: Boolean)
data class CategorySlice(val category: UsageCategory, val minutes: Int)

data class ActivityUiState(
    val range: ActivityRange = ActivityRange.WEEK,
    val bars: List<BarUi> = emptyList(),
    val avgMinutes: Int = 0,
    val changePercent: Int? = null,
    val periodTotalMinutes: Int = 0,
    val categories: List<CategorySlice> = emptyList(),
    val todayMinutes: Int = 0,
    val unlocksToday: Int = 0,
    val unlocksDiff: Int? = null,
    val bestWindow: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(
    private val history: UsageHistoryRepository,
    private val categoryResolver: AppCategoryResolver,
) : ViewModel() {

    private val range = MutableStateFlow(ActivityRange.WEEK)

    val uiState: StateFlow<ActivityUiState> = range
        .flatMapLatest { r ->
            val today = LocalDate.now()
            val (from, to) = bounds(r, today)
            history.observe(from, to).map { (apps, sums) -> build(r, today, apps, sums) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityUiState())

    init {
        // Mantiene al día el historial mientras la pantalla está en uso
        viewModelScope.launch {
            while (isActive) {
                history.sync()
                delay(60_000)
            }
        }
    }

    fun setRange(value: ActivityRange) { range.value = value }

    private fun bounds(r: ActivityRange, today: LocalDate): Pair<LocalDate, LocalDate> = when (r) {
        ActivityRange.WEEK -> ActivityStats.weekDays(today.minusWeeks(1)).first() to ActivityStats.weekDays(today).last()
        ActivityRange.MONTH -> today.minusMonths(1).withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
    }

    private fun build(r: ActivityRange, today: LocalDate, apps: List<DailyAppUsage>, sums: List<DailySummary>): ActivityUiState {
        val week = r == ActivityRange.WEEK
        val days = if (week) ActivityStats.weekDays(today) else ActivityStats.monthDays(today)
        val prevDays = if (week) ActivityStats.weekDays(today.minusWeeks(1)) else ActivityStats.monthDays(today.minusMonths(1))

        val totals: Map<LocalDate, Int> = apps.groupBy { LocalDate.parse(it.date) }
            .mapValues { (_, rows) -> rows.sumOf { it.minutes } }

        val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")
        val bars = days.map { d ->
            val label = if (week) dayLetters[d.dayOfWeek.value - 1]
            else if (d.dayOfMonth == 1 || d.dayOfMonth % 5 == 0) d.dayOfMonth.toString() else ""
            BarUi(label, totals[d] ?: 0, isToday = d == today, isFuture = d.isAfter(today))
        }

        val avg = ActivityStats.averageMinutes(totals, days, today)
        val prevAvg = ActivityStats.averageMinutes(totals, prevDays, prevDays.last())
        val change = if (avg > 0 && prevAvg > 0) UsageMath.percentChange(avg, prevAvg) else null

        val todayKey = today.toString()
        val todayRows = apps.filter { it.date == todayKey }
        val categories = todayRows
            .groupBy { categoryResolver.categoryOf(it.packageName) }
            .map { (cat, rows) -> CategorySlice(cat, rows.sumOf { it.minutes }) }
            .filter { it.minutes > 0 }
            .sortedByDescending { it.minutes }

        val unlocksToday = sums.firstOrNull { it.date == todayKey }?.unlocks ?: 0
        val unlocksYesterday = sums.firstOrNull { it.date == today.minusDays(1).toString() }?.unlocks

        // Mejor franja: promedio horario de los últimos 7 días
        val hourly = IntArray(24)
        val weekAgo = today.minusDays(6)
        sums.filter { val d = LocalDate.parse(it.date); !d.isBefore(weekAgo) && !d.isAfter(today) }.forEach { s ->
            s.hourlyMinutes.split(',').forEachIndexed { i, v -> if (i < 24) hourly[i] += v.toIntOrNull() ?: 0 }
        }
        val best = ActivityStats.bestWindowStart(hourly.toList())?.let { "%02d–%02d h".format(it, it + 2) }

        return ActivityUiState(
            range = r, bars = bars, avgMinutes = avg, changePercent = change,
            periodTotalMinutes = days.sumOf { totals[it] ?: 0 },
            categories = categories, todayMinutes = totals[today] ?: 0,
            unlocksToday = unlocksToday,
            unlocksDiff = unlocksYesterday?.let { unlocksToday - it },
            bestWindow = best,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as FocusBreakApp
                ActivityViewModel(app.container.history, app.container.categories)
            }
        }
    }
}
