package com.focusbreak.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Aplicación que el usuario decidió monitorear. dailyLimitMinutes = null significa "sin límite". */
@Entity(tableName = "monitored_apps")
data class MonitoredApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val dailyLimitMinutes: Int?,
    val enabled: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
)
