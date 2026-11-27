package com.focusbreak.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.focusbreak.app.MainActivity
import com.focusbreak.app.R
import com.focusbreak.app.domain.LimitLevel
import com.focusbreak.app.domain.formatDuration

/** Avisos escalonados al 50 % y 80 % del límite (HU07). */
class Notifier(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun showLimitAlert(appLabel: String, level: LimitLevel, usedMin: Int, limitMin: Int) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val progress = "Llevas ${formatDuration(usedMin)} de ${formatDuration(limitMin)}."
        val (title, text) = when (level) {
            LimitLevel.SOFT -> "Vas por la mitad en $appLabel" to "$progress Aún vas bien."
            LimitLevel.FIRM -> "Te queda poco en $appLabel" to "$progress ¿Y si tomas una pausa o sales a caminar?"
            else -> return
        }
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_focus)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify("$appLabel:$level".hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "limit_alerts"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Avisos de límite", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Avisos al acercarte al límite diario de una app" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
