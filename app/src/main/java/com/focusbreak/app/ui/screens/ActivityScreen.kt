package com.focusbreak.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusbreak.app.domain.DailyGoal
import com.focusbreak.app.domain.UsageCategory
import com.focusbreak.app.domain.formatDuration
import com.focusbreak.app.ui.theme.*
import kotlin.math.abs
import kotlin.math.max

private fun categoryColor(c: UsageCategory): Color = when (c) {
    UsageCategory.SOCIAL -> Color(0xFF7DB45A)
    UsageCategory.VIDEO -> Amber
    UsageCategory.PRODUCTIVITY -> ForestDark
    UsageCategory.OTHER -> Color(0xFFD5DACB)
}

@Composable
fun ActivityScreen(vm: ActivityViewModel = viewModel(factory = ActivityViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ActivityHeader(state, vm::setRange) }
        item { Spacer(Modifier.height(16.dp)); ChartCard(state) }
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
                Text("DISTRIBUCIÓN", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Text("¿En qué usas tu tiempo?", style = MaterialTheme.typography.titleLarge)
            }
        }
        item { DistributionCard(state) }
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
                Text("TUS PATRONES", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Text("Datos destacados", style = MaterialTheme.typography.titleLarge)
            }
        }
        item { HighlightCards(state) }
    }
}

// ------------------------------------------------------------------ cabecera

@Composable
private fun ActivityHeader(state: ActivityUiState, onRange: (ActivityRange) -> Unit) {
    val week = state.range == ActivityRange.WEEK
    Column(
        Modifier.fillMaxWidth()
            .background(ForestDark, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TU PROGRESO", style = MaterialTheme.typography.labelSmall, color = Lime)
                Text("Actividad", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
            RangeToggle(state.range, onRange)
        }
        Spacer(Modifier.height(20.dp))
        Text("MEDIA DIARIA", style = MaterialTheme.typography.labelSmall, color = MintSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.avgMinutes > 0) formatDuration(state.avgMinutes) else "—",
                style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Bold
            )
            state.changePercent?.let { change ->
                Spacer(Modifier.width(10.dp))
                Box(Modifier.background(if (change <= 0) Lime else Amber, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                    Text("${if (change > 0) "+" else ""}$change%", style = MaterialTheme.typography.labelMedium, color = ForestDark, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            if (state.changePercent != null) "respecto a ${if (week) "la semana anterior" else "el mes anterior"}"
            else "Aún no hay datos de ${if (week) "la semana" else "el mes"} anterior",
            style = MaterialTheme.typography.bodySmall, color = MintSurface
        )
    }
}

@Composable
private fun RangeToggle(selected: ActivityRange, onChange: (ActivityRange) -> Unit) {
    Row(Modifier.background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(4.dp)) {
        listOf(ActivityRange.WEEK to "Semana", ActivityRange.MONTH to "Mes").forEach { (r, label) ->
            val on = r == selected
            Box(
                Modifier.background(if (on) Lime else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onChange(r) }.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = if (on) ForestDark else Color.White, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

// ------------------------------------------------------------------ gráfico de barras

@Composable
private fun ChartCard(state: ActivityUiState) {
    Card(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("TIEMPO DE PANTALLA", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    Text(if (state.range == ActivityRange.WEEK) "Esta semana" else "Este mes", style = MaterialTheme.typography.titleLarge)
                }
                Text(formatDuration(state.periodTotalMinutes), style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
            Spacer(Modifier.height(16.dp))
            BarChart(state.bars)
            Spacer(Modifier.height(10.dp))
            Text("--- Objetivo diario: ${DailyGoal.MINUTES / 60} h", style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
private fun BarChart(bars: List<BarUi>) {
    val maxValue = max(DailyGoal.MINUTES * 1.15f, (bars.maxOfOrNull { it.minutes } ?: 0) * 1.1f)
    Column {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val slot = size.width / bars.size
            val barWidth = (slot * 0.6f).coerceAtMost(28.dp.toPx())
            bars.forEachIndexed { i, b ->
                val x = i * slot + (slot - barWidth) / 2
                drawRoundRect(Color(0xFFEEF0EA), Offset(x, 0f), Size(barWidth, size.height), CornerRadius(barWidth / 2))
                if (b.minutes > 0) {
                    val h = size.height * (b.minutes / maxValue)
                    drawRoundRect(
                        if (b.isToday) Lime else Color(0xFFB9CDB0),
                        Offset(x, size.height - h), Size(barWidth, h), CornerRadius(barWidth / 2)
                    )
                }
            }
            val goalY = size.height * (1f - DailyGoal.MINUTES / maxValue)
            drawLine(Color(0xFF9FB094), Offset(0f, goalY), Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f)))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEach { b ->
                Text(b.label, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                    color = if (b.isToday) ForestDark else InkMuted,
                    fontWeight = if (b.isToday) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
            }
        }
    }
}

// ------------------------------------------------------------------ dona

@Composable
private fun DistributionCard(state: ActivityUiState) {
    Card(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (state.categories.isEmpty()) {
            Text("Todavía no hay uso registrado hoy.", Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            return@Card
        }
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Donut(state.categories, state.todayMinutes)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.categories.forEach { slice ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(categoryColor(slice.category), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(slice.category.label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Text(formatDuration(slice.minutes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Donut(slices: List<CategorySlice>, totalMinutes: Int) {
    val sum = slices.sumOf { it.minutes }.coerceAtLeast(1)
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            slices.forEach { s ->
                val sweep = 360f * s.minutes / sum
                drawArc(categoryColor(s.category), start, (sweep - 2f).coerceAtLeast(0.5f), false,
                    Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatDuration(totalMinutes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("total hoy", style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

// ------------------------------------------------------------------ datos destacados

@Composable
private fun HighlightCards(state: ActivityUiState) {
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(Modifier.size(40.dp).background(MintSurface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LockOpen, contentDescription = null, tint = Leaf)
                }
                Spacer(Modifier.height(12.dp))
                Text("DESBLOQUEOS", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Text("${state.unlocksToday}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val diff = state.unlocksDiff
                Text(
                    when {
                        diff == null -> "Sin datos de ayer"
                        diff == 0 -> "Igual que ayer"
                        diff < 0 -> "${abs(diff)} menos que ayer"
                        else -> "$diff más que ayer"
                    },
                    style = MaterialTheme.typography.labelSmall, color = Leaf
                )
            }
        }
        Card(
            Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ForestDark)
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(Modifier.size(40.dp).background(Lime, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, tint = ForestDark)
                }
                Spacer(Modifier.height(12.dp))
                Text("MEJOR FRANJA", style = MaterialTheme.typography.labelSmall, color = MintSurface)
                Text(state.bestWindow ?: "—", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Tu momento más enfocado", style = MaterialTheme.typography.labelSmall, color = Lime)
            }
        }
    }
}
