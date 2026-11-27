package com.focusbreak.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focusbreak.app.ui.theme.*

/** Cabecera verde oscuro común a las pantallas (como en los mockups). */
@Composable
fun ScreenHeader(eyebrow: String, title: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(ForestDark, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = Lime)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}

@Composable
private fun PlaceholderScreen(eyebrow: String, title: String, pending: String) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(eyebrow, title)
        Card(
            Modifier.padding(20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Próximamente", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(pending, style = MaterialTheme.typography.bodyMedium, color = InkMuted)
            }
        }
    }
}

@Composable fun FocusScreen() = PlaceholderScreen("Modo sin distracciones", "Concentración", "Sesiones de 15, 25, 45 o 60 minutos con bloqueo de apps (HU10, Sprint 3).")
