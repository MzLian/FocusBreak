package com.focusbreak.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusbreak.app.domain.StepRedeem
import com.focusbreak.app.domain.DailyGoal
import com.focusbreak.app.domain.StepGoal
import com.focusbreak.app.domain.LimitRules
import com.focusbreak.app.domain.UsageMath
import com.focusbreak.app.domain.formatDuration
import com.focusbreak.app.ui.components.AppIcon
import com.focusbreak.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Datos de ejemplo hasta implementar rachas (HU14, Sprint 3)
private const val SAMPLE_STREAK = 4
private const val SAMPLE_BEST_STREAK = 7

@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onOpenProfile: () -> Unit,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionTick by remember { mutableIntStateOf(0) }
    val stepsGranted = remember(permissionTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionTick++
        vm.syncSteps()
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { HomeHeader(state, onStartFocus) }
        item {
            StepsCard(
                steps = state.steps, granted = stepsGranted, sensorAvailable = state.stepSensorAvailable,
                available = state.stepsAvailable, redeemedMinutes = state.redeemedMinutes,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    Text("HOY", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    Text("Uso por aplicación", style = MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick = onOpenProfile) { Text("Ver todo", color = Leaf) }
            }
        }
        if (state.items.isEmpty()) {
            item {
                Text(
                    "Aún no monitoreas ninguna app. Añade la primera para ver tu uso aquí.",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        } else {
            item {
                Card(
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    state.items.forEachIndexed { index, item ->
                        AppUsageRow(item) { vm.setEnabled(item.app.packageName, it) }
                        if (index < state.items.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        item { AddLimitButton(onOpenProfile) }
        item { StreakCard() }
    }
}

// ------------------------------------------------------------------ cabecera

@Composable
private fun HomeHeader(state: HomeUiState, onStartFocus: () -> Unit) {
    val date = todayLabel()
    Column(
        Modifier.fillMaxWidth()
            .background(ForestDark, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                Text("Hola", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.size(44.dp).background(Lime, CircleShape), contentAlignment = Alignment.Center) {
                Text("F", color = ForestDark, fontWeight = FontWeight.Bold)
            }
        }
        state.lastDetectedLabel?.let { label ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Lime, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Detectado en vivo: $label · ${state.lastDetectedTime}",
                    style = MaterialTheme.typography.labelSmall, color = MintSurface
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        ScreenTimeCard(state)
        Spacer(Modifier.height(12.dp))
        FocusBanner(onStartFocus)
    }
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-PE")))
        .uppercase(Locale.forLanguageTag("es-PE"))

@Composable
private fun ScreenTimeCard(state: HomeUiState) {
    val change = UsageMath.percentChange(state.totalMinutes, state.yesterdayMinutes)
    val diff = state.totalMinutes - state.yesterdayMinutes
    val progress = UsageMath.progress(state.totalMinutes, null)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TIEMPO DE PANTALLA", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(state.totalMinutes), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        if (change != null) {
                            Spacer(Modifier.width(8.dp))
                            val better = change <= 0
                            Box(
                                Modifier.background(if (better) MintSurface else Amber.copy(alpha = 0.3f), RoundedCornerShape(50))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${if (change > 0) "+" else ""}$change%",
                                    style = MaterialTheme.typography.labelMedium, color = ForestDark
                                )
                            }
                        }
                    }
                    if (state.yesterdayMinutes > 0) {
                        Text(
                            "${formatDuration(abs(diff))} ${if (diff <= 0) "menos" else "más"} que ayer",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted
                        )
                    }
                }
                GoalRing(progress)
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outline)
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Desbloqueos", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    Text("${state.unlocks}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Más usada", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                    Text(state.topAppLabel ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GoalRing(progress: Float, modifier: Modifier = Modifier.size(84.dp)) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arc = Size(size.width - stroke.width, size.height - stroke.width)
            drawArc(Color(0xFFE6EAE0), 0f, 360f, false, Offset(inset, inset), arc, style = stroke)
            drawArc(Lime, -90f, 360f * progress, false, Offset(inset, inset), arc, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("de ${DailyGoal.MINUTES / 60} h", style = MaterialTheme.typography.labelSmall, color = InkMuted)
        }
    }
}

@Composable
private fun FocusBanner(onStartFocus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onStartFocus).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).background(Lime, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = ForestDark)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Es hora de concentrarte", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Bloquea distracciones durante 25 min", color = MintSurface, style = MaterialTheme.typography.bodySmall)
        }
        Text("Empezar", color = Lime, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------------ lista de apps

@Composable
private fun AppUsageRow(item: AppUsageItem, onToggle: (Boolean) -> Unit) {
    val limit = item.app.dailyLimitMinutes
    val progress = UsageMath.progress(item.usedMinutes, limit)
    val warn = limit != null && progress >= 0.8f

    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(item.app.packageName, Modifier.size(44.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(item.app.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(formatDuration(item.usedMinutes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = if (warn) Amber else Lime,
                trackColor = Color(0xFFE6EAE0),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Límite: ${LimitRules.format(limit)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (warn) Amber else InkMuted
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = item.app.enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedTrackColor = Leaf, checkedThumbColor = Color.White)
        )
    }
}

@Composable
private fun AddLimitButton(onClick: () -> Unit) {
    val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
    val line = MaterialTheme.colorScheme.outline
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 16.dp).fillMaxWidth().height(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(line, cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(width = 2.dp.toPx(), pathEffect = dash))
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(MintSurface, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Leaf)
        }
        Spacer(Modifier.width(12.dp))
        Text("Añadir límite de aplicación", style = MaterialTheme.typography.titleSmall)
    }
}

// ------------------------------------------------------------------ racha (ejemplo)

@Composable
private fun StreakCard() {
    Card(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8E9DB))
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(Modifier.size(44.dp).background(Amber.copy(alpha = 0.35f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Amber)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("$SAMPLE_STREAK días cumpliendo tu objetivo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Tu mejor racha es de $SAMPLE_BEST_STREAK días. ¡Sigue así!", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(7) { i ->
                        val done = i < SAMPLE_STREAK
                        Box(
                            Modifier.size(24.dp).background(if (done) Amber else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}


// ------------------------------------------------------------------ pasos (HU11)

@Composable
private fun StepsCard(
    steps: Int, granted: Boolean, sensorAvailable: Boolean,
    available: Int, redeemedMinutes: Int, onRequestPermission: () -> Unit,
) {
    val progress = (steps.toFloat() / StepGoal.STEPS).coerceIn(0f, 1f)
    val number = java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-PE"))
    Card(
        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(MintSurface, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DirectionsWalk, contentDescription = null, tint = Leaf)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("PASOS DE HOY", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                when {
                    !sensorAvailable -> Text("Tu teléfono no tiene contador de pasos.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    !granted -> {
                        Text("Concede el permiso de actividad física para contar tus pasos.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MintSurface, contentColor = ForestDark)
                        ) { Text("Conceder") }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(number.format(steps), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("  de ${number.format(StepGoal.STEPS)}", style = MaterialTheme.typography.bodySmall, color = InkMuted)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Lime, trackColor = Color(0xFFE6EAE0),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Canjeables: ${number.format(available)} pasos · " +
                                if (redeemedMinutes > 0) "hoy ganaste $redeemedMinutes min"
                                else "${number.format(StepRedeem.STEPS_PER_BLOCK)} pasos = ${StepRedeem.MINUTES_PER_BLOCK} min",
                            style = MaterialTheme.typography.bodySmall, color = InkMuted,
                        )
                    }
                }
            }
        }
    }
}
