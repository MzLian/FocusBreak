package com.focusbreak.app.domain

enum class FocusStatus { ACTIVE, COMPLETED, CANCELLED }

/** Reglas puras del modo Concentración (HU10). Sin dependencias de Android para probarlas con JUnit. */
object FocusRules {
    val DURATIONS = listOf(15, 25, 45, 60)      // minutos permitidos (informe, cap. VII)
    const val DEFAULT_MINUTES = 25
    const val DAILY_GOAL_MINUTES = 60           // meta diaria de concentración (valor inicial)
    const val END_EARLY_WAIT_SECONDS = 10       // fricción consciente para terminar antes desde el bloqueo

    fun endMs(startMs: Long, minutes: Int): Long = startMs + minutes * 60_000L

    fun remainingMs(endMs: Long, nowMs: Long): Long = (endMs - nowMs).coerceAtLeast(0L)

    fun isExpired(endMs: Long, nowMs: Long): Boolean = nowMs >= endMs

    /** Minutos completos transcurridos, sin pasar de los planificados. */
    fun elapsedMinutes(startMs: Long, plannedMinutes: Int, nowMs: Long): Int =
        ((nowMs - startMs) / 60_000L).toInt().coerceIn(0, plannedMinutes)

    /** Las apps de una sesión se guardan como texto "pkg1,pkg2" (copia fija al iniciar la sesión). */
    fun toCsv(packages: Collection<String>): String = packages.sorted().joinToString(",")

    fun blocks(appsCsv: String, packageName: String): Boolean =
        appsCsv.isNotEmpty() && appsCsv.split(',').contains(packageName)

    /** "25:00", "00:01". Redondea hacia arriba para no mostrar 00:00 antes de terminar. */
    fun formatClock(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) + 999) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
