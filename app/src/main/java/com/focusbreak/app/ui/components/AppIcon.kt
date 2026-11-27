package com.focusbreak.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.focusbreak.app.ui.theme.MintSurface

/** Ícono real de la app instalada; si no se puede cargar, un cuadro verde claro. */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier.size(40.dp)) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = modifier.clip(RoundedCornerShape(10.dp)))
    } else {
        Box(modifier.background(MintSurface, RoundedCornerShape(10.dp)))
    }
}
