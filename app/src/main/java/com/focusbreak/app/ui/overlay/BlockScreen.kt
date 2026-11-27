package com.focusbreak.app.ui.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focusbreak.app.domain.LimitEvaluator
import com.focusbreak.app.domain.formatDuration
import com.focusbreak.app.ui.components.AppIcon
import com.focusbreak.app.ui.theme.*
import kotlinx.coroutines.delay

data class BlockUiState(
    val packageName: String,
    val appLabel: String,
    val usedMinutes: Int,
    val limitMinutes: Int,
    val canExtend: Boolean,
)

private enum class Mode { Limit, Breathing }

/** Pantalla de bloqueo al 100 % del límite (diseño: Diseno_pantalla_bloqueo_FocusBreak.png). */
@Composable
fun BlockScreen(
    state: BlockUiState,
    onWalk: () -> Unit,
    onExtend: () -> Unit,
    onFinishBreathing: () -> Unit,
) {
    var mode by remember { mutableStateOf(Mode.Limit) }
    Box(Modifier.fillMaxSize().background(ForestDark).padding(horizontal = 28.dp, vertical = 40.dp)) {
        when (mode) {
            Mode.Limit -> LimitContent(state, onWalk, onBreathe = { mode = Mode.Breathing }, onExtend)
            Mode.Breathing -> BreathingContent(onDone = onFinishBreathing, onCancel = { mode = Mode.Limit })
        }
    }
}

@Composable
private fun LimitContent(state: BlockUiState, onWalk: () -> Unit, onBreathe: () -> Unit, onExtend: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("LÍMITE ALCANZADO", style = MaterialTheme.typography.labelMedium, color = Lime, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        AppIcon(state.packageName, Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text("Llegaste a tu límite", style = MaterialTheme.typography.headlineMedium, color = Color.White,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("${state.appLabel} · ${formatDuration(state.usedMinutes)} de ${formatDuration(state.limitMinutes)} hoy",
            style = MaterialTheme.typography.bodyMedium, color = MintSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Una pausa corta te hará bien. ¿Qué tal si cambias de plan?",
            style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onWalk, modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = ForestDark)) {
            Text("Salir a caminar", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBreathe, modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
            Text("Respirar 1 minuto")
        }
        Spacer(Modifier.height(20.dp))
        ExtendOption(state.canExtend, onExtend)
    }
}

/** Fricción consciente: el botón de extensión se habilita tras 10 s de pausa. */
@Composable
private fun ExtendOption(canExtend: Boolean, onExtend: () -> Unit) {
    if (!canExtend) {
        Text("Ya usaste tus extensiones de hoy.", style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f))
        return
    }
    var wait by remember { mutableIntStateOf(10) }
    LaunchedEffect(Unit) { while (wait > 0) { delay(1_000); wait-- } }
    val label = "Necesito ${LimitEvaluator.EXTENSION_MINUTES} minutos más"
    TextButton(onClick = onExtend, enabled = wait == 0) {
        Text(
            if (wait > 0) "$label (espera $wait s)" else label,
            color = if (wait == 0) Color.White else Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun BreathingContent(onDone: () -> Unit, onCancel: () -> Unit) {
    var seconds by remember { mutableIntStateOf(60) }
    LaunchedEffect(Unit) { while (seconds > 0) { delay(1_000); seconds-- } }
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.6f, targetValue = 1f, label = "scale",
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val inhale = ((60 - seconds) / 4) % 2 == 0

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("RESPIRA CONMIGO", style = MaterialTheme.typography.labelMedium, color = Lime, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).background(Color.White.copy(alpha = 0.1f), CircleShape))
            Box(Modifier.size((200f * scale).dp).background(Lime, CircleShape), contentAlignment = Alignment.Center) {
                Text(if (seconds == 0) "Listo" else if (inhale) "Inhala" else "Exhala",
                    color = ForestDark, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(32.dp))
        if (seconds > 0) {
            Text("$seconds s", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Sigue el ritmo del círculo", color = MintSurface, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onCancel) { Text("Cancelar", color = Color.White.copy(alpha = 0.6f)) }
        } else {
            Text("Tómate un momento antes de volver.", color = MintSurface, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = ForestDark)) {
                Text("Volver al inicio", fontWeight = FontWeight.Bold)
            }
        }
    }
}
