package com.focusbreak.app.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.focusbreak.app.permissions.PermissionChecker as PC
import com.focusbreak.app.ui.theme.*

private data class PermissionItem(
    val title: String,
    val why: String,
    val required: Boolean,
    val granted: Boolean,
    val hint: String? = null,
    val onGrant: () -> Unit,
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }              // fuerza re-evaluar permisos
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ } // al volver de los ajustes del sistema

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val activityLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val items = remember(tick) {
        listOf(
            PermissionItem(
                "Servicio de accesibilidad",
                "Detecta qué aplicación tienes abierta para medir el tiempo en vivo. Solo lee el nombre de la app, nunca el contenido de tu pantalla.",
                required = true, granted = PC.hasAccessibility(ctx),
                hint = "Si Android muestra “Configuración restringida”: Información de la app → menú ⋮ → Permitir ajustes restringidos.",
                onGrant = { ctx.startActivity(PC.accessibilitySettings()) }),
            PermissionItem(
                "Acceso al uso de aplicaciones",
                "Permite consultar cuánto tiempo acumulas en cada app durante el día.",
                required = true, granted = PC.hasUsageAccess(ctx),
                onGrant = { ctx.startActivity(PC.usageAccessSettings()) }),
            PermissionItem(
                "Mostrar sobre otras apps",
                "Necesario para mostrar la pantalla de bloqueo cuando llegas a tu límite.",
                required = true, granted = PC.hasOverlay(ctx),
                onGrant = { ctx.startActivity(PC.overlaySettings(ctx)) }),
            PermissionItem(
                "Notificaciones",
                "Para avisarte al 50 % y 80 % de tu límite y recomendarte pausas.",
                required = false, granted = PC.hasNotifications(ctx),
                onGrant = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }),
            PermissionItem(
                "Actividad física",
                "Para contar tus pasos y permitirte canjearlos por tiempo extra.",
                required = false, granted = PC.hasActivityRecognition(ctx),
                onGrant = { activityLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) }),
        )
    }
    val requiredOk = items.filter { it.required }.all { it.granted }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxWidth()
                .background(ForestDark, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .statusBarsPadding().padding(24.dp)
        ) {
            Text("ANTES DE EMPEZAR", style = MaterialTheme.typography.labelSmall, color = Lime)
            Spacer(Modifier.height(4.dp))
            Text("Activa FocusBreak", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Tus datos se quedan en este teléfono. Puedes revocar cualquier permiso cuando quieras desde los ajustes del sistema.",
                style = MaterialTheme.typography.bodyMedium, color = MintSurface
            )
        }

        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { PermissionCard(it) }
        }

        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
            Button(
                onClick = onFinish, enabled = requiredOk,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = ForestDark)
            ) { Text("Continuar") }
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Omitir por ahora", color = InkMuted)
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionItem) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (item.granted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (item.granted) "Concedido" else "Pendiente",
                tint = if (item.granted) Leaf else InkMuted,
                modifier = Modifier.padding(top = 2.dp).size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (item.required) "Necesario" else "Opcional por ahora",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.required) Amber else InkMuted
                )
                Spacer(Modifier.height(6.dp))
                Text(item.why, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                if (!item.granted && item.hint != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(item.hint, style = MaterialTheme.typography.bodySmall, color = InkMuted)
                }
                if (!item.granted) {
                    Spacer(Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = item.onGrant,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = MintSurface, contentColor = ForestDark)
                    ) { Text("Conceder") }
                }
            }
        }
    }
}
