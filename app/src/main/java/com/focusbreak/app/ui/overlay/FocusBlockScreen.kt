package com.focusbreak.app.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusbreak.app.domain.FocusRules
import com.focusbreak.app.ui.components.AppIcon
import com.focusbreak.app.ui.theme.*
import kotlinx.coroutines.delay

data class FocusBlockUiState(
    val packageName: String,
    val appLabel: String,
    val endMs: Long,
)

/** Pantalla que bloquea una app elegida mientras dura una sesión de concentración (HU10). */
@Composable
fun FocusBlockScreen(
    state: FocusBlockUiState,
    onHome: () -> Unit,
    onEnd: () -> Unit,
    onExpired: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val expiredCallback by rememberUpdatedState(onExpired)
    LaunchedEffect(state.endMs) {
        while (System.currentTimeMillis() < state.endMs) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
        now = System.currentTimeMillis()
        expiredCallback()
    }

    Box(Modifier.fillMaxSize().background(ForestDark).padding(horizontal = 28.dp, vertical = 40.dp)) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("SESIÓN DE CONCENTRACIÓN", style = MaterialTheme.typography.labelMedium, color = Lime, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            AppIcon(state.packageName, Modifier.size(72.dp))
            Spacer(Modifier.height(20.dp))
            Text("Ahora toca concentrarte", style = MaterialTheme.typography.headlineMedium, color = Color.White,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("${state.appLabel} está bloqueada hasta que termine tu sesión.",
                style = MaterialTheme.typography.bodyMedium, color = MintSurface, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(FocusRules.formatClock(FocusRules.remainingMs(state.endMs, now)),
                style = MaterialTheme.typography.displayMedium, color = Lime, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = ForestDark)) {
                Text("Volver al inicio", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            EndEarlyOption(onEnd)
        }
    }
}

/** Fricción consciente: terminar antes de tiempo se habilita tras unos segundos de espera. */
@Composable
private fun EndEarlyOption(onEnd: () -> Unit) {
    var wait by remember { mutableIntStateOf(FocusRules.END_EARLY_WAIT_SECONDS) }
    LaunchedEffect(Unit) { while (wait > 0) { delay(1_000); wait-- } }
    val label = "Terminar la sesión"
    TextButton(onClick = onEnd, enabled = wait == 0) {
        Text(
            if (wait > 0) "$label (espera $wait s)" else label,
            color = if (wait == 0) Color.White else Color.White.copy(alpha = 0.4f)
        )
    }
}
