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
