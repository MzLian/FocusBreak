package com.focusbreak.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.focusbreak.app.FocusBreakApp
import java.util.concurrent.TimeUnit

/** Lee el contador de pasos cada ~15 min aunque la app esté cerrada, para no perder pasos al cambiar de día o reiniciar. */
class StepSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as FocusBreakApp).container.steps.sync()
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("step_sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
