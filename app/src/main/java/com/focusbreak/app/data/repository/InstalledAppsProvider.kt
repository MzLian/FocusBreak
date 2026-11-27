package com.focusbreak.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String)

/** Lista las apps con ícono de lanzador (requiere el bloque <queries> del manifest en Android 11+). */
class InstalledAppsProvider(private val context: Context) {
    suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Nombre legible de un paquete, o null si no se puede resolver. */
    fun labelOf(packageName: String): String? = runCatching {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0)).loadLabel(pm).toString()
    }.getOrNull()
}
