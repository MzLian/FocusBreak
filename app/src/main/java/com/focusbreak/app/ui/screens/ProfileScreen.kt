package com.focusbreak.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.focusbreak.app.data.db.MonitoredApp
import com.focusbreak.app.data.repository.InstalledApp
import com.focusbreak.app.domain.LimitRules
import com.focusbreak.app.ui.components.AppIcon
import com.focusbreak.app.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)) {
    val monitored by vm.monitored.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<MonitoredApp?>(null) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tu cuenta", "Perfil")
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Límites por aplicación", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Elige qué apps vigilar y cuánto tiempo al día quieres usarlas.",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted
                )
            }
            if (monitored.isEmpty()) {
                item {
                    Text(
                        "Aún no monitoreas ninguna app. Añade la primera para empezar.",
                        style = MaterialTheme.typography.bodyMedium, color = InkMuted
                    )
                }
            }
            items(monitored, key = { it.packageName }) { app ->
                MonitoredAppRow(
                    app = app,
                    onClick = { editing = app },
                    onToggle = { vm.setEnabled(app.packageName, it) }
                )
            }
            item { AddAppButton { vm.loadInstalled(); picking = true } }
        }
    }

    editing?.let { app ->
        LimitDialog(
            app = app,
            onDismiss = { editing = null },
            onSave = { minutes -> vm.setLimit(app.packageName, minutes); editing = null },
            onRemove = { vm.remove(app.packageName); editing = null }
        )
    }

    if (picking) {
        AppPickerSheet(
            candidates = candidates,
            onPick = { vm.add(it); picking = false },
            onDismiss = { picking = false }
        )
    }
}

@Composable
private fun MonitoredAppRow(app: MonitoredApp, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Límite: ${LimitRules.format(app.dailyLimitMinutes)}",
                    style = MaterialTheme.typography.bodySmall, color = InkMuted
                )
            }
            Switch(
                checked = app.enabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = Leaf, checkedThumbColor = androidx.compose.ui.graphics.Color.White)
            )
        }
    }
}

@Composable
private fun AddAppButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestDark)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Añadir límite de aplicación")
    }
}

@Composable
private fun LimitDialog(app: MonitoredApp, onDismiss: () -> Unit, onSave: (Int?) -> Unit, onRemove: () -> Unit) {
    var unlimited by remember { mutableStateOf(app.dailyLimitMinutes == null) }
    var minutes by remember { mutableIntStateOf(app.dailyLimitMinutes ?: LimitRules.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sin límite", Modifier.weight(1f))
                    Switch(checked = unlimited, onCheckedChange = { unlimited = it })
                }
                if (!unlimited) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Límite diario: ${LimitRules.format(minutes)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = minutes.toFloat(),
                        onValueChange = { minutes = ((it / LimitRules.STEP).roundToInt() * LimitRules.STEP).coerceIn(LimitRules.MIN, LimitRules.MAX) },
                        valueRange = LimitRules.MIN.toFloat()..LimitRules.MAX.toFloat(),
                        steps = (LimitRules.MAX - LimitRules.MIN) / LimitRules.STEP - 1,
                    )
                }
                TextButton(onClick = onRemove) {
                    Text("Quitar de la lista", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(if (unlimited) null else minutes) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(candidates: List<InstalledApp>, onPick: (InstalledApp) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(candidates, query) {
        candidates.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxHeight(0.85f).padding(horizontal = 20.dp)) {
            Text("Añadir aplicación", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Buscar") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text(
                    if (query.isBlank()) "Buscando aplicaciones…" else "Sin resultados",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            LazyColumn {
                items(filtered, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app.packageName)
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
