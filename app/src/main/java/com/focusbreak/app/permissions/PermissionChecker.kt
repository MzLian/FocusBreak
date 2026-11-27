package com.focusbreak.app.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.focusbreak.app.service.FocusAccessibilityService

/** Comprueba el estado de cada permiso y construye los Intents a los ajustes del sistema. */
object PermissionChecker {

    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java)
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlay(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun hasAccessibility(ctx: Context): Boolean {
        val expected = ComponentName(ctx, FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun hasNotifications(ctx: Context) = has(ctx, Manifest.permission.POST_NOTIFICATIONS)

    fun hasActivityRecognition(ctx: Context) = has(ctx, Manifest.permission.ACTIVITY_RECOGNITION)

    private fun has(ctx: Context, permission: String) =
        ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

    fun accessibilitySettings() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    fun usageAccessSettings() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    fun overlaySettings(ctx: Context) = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")
    )
    fun appDetails(ctx: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")
    )
}
