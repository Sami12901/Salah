package com.salahmate.app

import android.app.Application
import com.salahmate.app.core.notify.NotificationChannels
import com.salahmate.app.workers.MaintenanceWorker

class SalahMateApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationChannels.ensureCreated(this)
        // One deferrable daily safety net; time-critical work is done by AlarmManager alarms.
        MaintenanceWorker.enqueue(this)
    }
}
