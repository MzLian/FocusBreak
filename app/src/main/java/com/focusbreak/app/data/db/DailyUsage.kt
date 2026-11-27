package com.focusbreak.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Minutos de uso de una app en un día (fecha ISO yyyy-MM-dd). */
@Entity(tableName = "daily_app_usage", primaryKeys = ["date", "packageName"])
data class DailyAppUsage(val date: String, val packageName: String, val minutes: Int)

/** Resumen del día: desbloqueos y minutos de uso por hora (24 valores separados por comas). */
@Entity(tableName = "daily_summary")
data class DailySummary(
    @PrimaryKey val date: String,
    val unlocks: Int,
    val hourlyMinutes: String,
)
