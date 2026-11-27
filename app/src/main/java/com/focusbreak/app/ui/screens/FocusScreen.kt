package com.focusbreak.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusbreak.app.domain.FocusRules
import com.focusbreak.app.permissions.PermissionChecker
import com.focusbreak.app.ui.components.AppIcon
import com.focusbreak.app.ui.theme.*

/** Pantalla Concentración (HU10): elegir duración y apps, iniciar la sesión y ver el progreso del día. */
@Composable
fun FocusScreen(vm: FocusViewModel = viewModel(factory = FocusViewModel.Factory)) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }       // al volver de los ajustes del sistema
    val accessibilityOn = remember(tick) { PermissionChecker.hasAccessibility(context) }
    var confirmEnd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Modo sin distracciones", "Concentración")
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!accessibilityOn) {
                item { AccessibilityWarning { context.startActivity(PermissionChecker.accessibilitySettings()) } }
            }
            item {
                if (state.session != null) {
                    ActiveSessionCard(state, onEnd = { confirmEnd = true })
                } else {
                    SetupCard(state, onSelect = { vm.selectDuration(it) }, onStart = { vm.start() })
                }
            }
            item { DailyProgressCard(state) }
            if (state.session == null) {
                item { AppsCard(state, onToggle = { vm.toggleApp(it) }) }
            }
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("¿Terminar la sesión?") },
            text = { Text("Se guardarán los minutos que llevas hasta ahora y las apps volverán a estar disponibles.") },
            confirmButton = { TextButton(onClick = { confirmEnd = false; vm.endEarly() }) { Text("Terminar") } },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Seguir") } },
        )
    }
}

@Composable
private fun AccessibilityWarning(onOpenSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8E9DB)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Falta activar la accesibilidad", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Sin el servicio de accesibilidad, FocusBreak no puede bloquear las apps durante la sesión.",
                style = MaterialTheme.typography.bodySmall, color = InkMuted,
            )
            TextButton(onClick = onOpenSettings) { Text("Abrir ajustes", color = Leaf) }
        }
    }
}

@Composable
private fun SetupCard(state: FocusUiState, onSelect: (Int) -> Unit, onStart: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DURACIÓN DE LA SESIÓN", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                FocusRules.formatClock(state.selectedMinutes * 60_000L),
                style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.durations.forEach { minutes ->
                    FilterChip(
                        selected = minutes == state.selectedMinutes,
                        onClick = { onSelect(minutes) },
                        label = { Text("$minutes min") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MintSurface, selectedLabelColor = ForestDark,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart, enabled = state.canStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = ForestDark),
            ) { Text("Iniciar sesión", fontWeight = FontWeight.Bold) }
            if (!state.canStart) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (state.apps.isEmpty()) "Primero añade apps a monitorear en Perfil."
                    else "Elige al menos una app para bloquear.",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun ActiveSessionCard(state: FocusUiState, onEnd: () -> Unit) {
    val session = state.session ?: return
    val total = (session.endMs - session.startMs).coerceAtLeast(1L)
    val progress = (1f - state.remainingMs.toFloat() / total).coerceIn(0f, 1f)
    val blocked = session.apps.split(',').count { it.isNotEmpty() }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ForestDark),
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SESIÓN EN CURSO", style = MaterialTheme.typography.labelSmall, color = Lime)
            Spacer(Modifier.height(8.dp))
            Text(
                FocusRules.formatClock(state.remainingMs),
                style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Lime, trackColor = Color.White.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (blocked == 1) "1 app bloqueada" else "$blocked apps bloqueadas",
                style = MaterialTheme.typography.bodyMedium, color = MintSurface,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onEnd, modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) { Text("Terminar sesión") }
        }
    }
}

@Composable
private fun DailyProgressCard(state: FocusUiState) {
    val progress = (state.todayMinutes.toFloat() / state.goalMinutes).coerceIn(0f, 1f)
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("HOY", style = MaterialTheme.typography.labelSmall, color = InkMuted)
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.todayMinutes} de ${state.goalMinutes} min de concentración",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Leaf, trackColor = MintSurface,
            )
        }
    }
}

@Composable
private fun AppsCard(state: FocusUiState, onToggle: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
                Text("APPS A BLOQUEAR", style = MaterialTheme.typography.labelSmall, color = InkMuted)
                Text(
                    "Se bloquean solo durante la sesión.",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted,
                )
            }
            if (state.apps.isEmpty()) {
                Text(
                    "Aún no monitoreas ninguna app. Añádelas en Perfil para poder elegirlas aquí.",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted,
                    modifier = Modifier.padding(20.dp),
                )
            }
            state.apps.forEachIndexed { index, item ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onToggle(item.app.packageName) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(item.app.packageName, Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(item.app.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Checkbox(
                        checked = item.selected, onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Leaf),
                    )
                }
                if (index < state.apps.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
