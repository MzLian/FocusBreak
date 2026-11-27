package com.focusbreak.app

import android.app.Application
import android.content.Context
import com.focusbreak.app.data.FocusPrefsStore
import com.focusbreak.app.data.StepsStore
import com.focusbreak.app.data.db.AppDatabase
import com.focusbreak.app.data.repository.AppCategoryResolver
import com.focusbreak.app.data.repository.DailyStateRepository
import com.focusbreak.app.data.repository.FocusRepository
import com.focusbreak.app.data.repository.InstalledAppsProvider
import com.focusbreak.app.data.repository.InterventionRepository
import com.focusbreak.app.data.repository.MonitoredAppsRepository
import com.focusbreak.app.data.repository.StepSensorReader
import com.focusbreak.app.data.repository.StepsRepository
import com.focusbreak.app.data.repository.UsageHistoryRepository
import com.focusbreak.app.data.repository.UsageRepository
import com.focusbreak.app.data.repository.UsageStatsRepository
import com.focusbreak.app.notifications.Notifier
import com.focusbreak.app.worker.StepSyncWorker

/** Contenedor de dependencias manual (suficiente para este proyecto). */
class AppContainer(context: Context) {
    private val database = AppDatabase.build(context)
    private val usageStats = UsageStatsRepository(context)

    val monitoredApps = MonitoredAppsRepository(database.monitoredAppDao())
    val installedApps = InstalledAppsProvider(context)
    val usage: UsageRepository = usageStats
    val history = UsageHistoryRepository(usageStats, database.usageHistoryDao())
    val categories = AppCategoryResolver(context)
    val steps = StepsRepository(context, StepSensorReader(context), StepsStore(context), database.dailyStepsDao())
    val dailyState = DailyStateRepository(database.dailyAppStateDao())
    val interventions = InterventionRepository(database.interventionEventDao())
    val focus = FocusRepository(database.focusSessionDao(), FocusPrefsStore(context), interventions)
}

class FocusBreakApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannel(this)
        StepSyncWorker.schedule(this)
    }
}
