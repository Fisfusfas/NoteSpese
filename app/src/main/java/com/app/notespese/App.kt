package com.app.notespese

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.app.notespese.notification.NotificationHelper
import com.app.notespese.worker.RicorrenzaWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleRicorrenzaWorker()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            NotificationHelper.CHANNEL_BUDGET,
            "Avvisi budget",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifiche quando si supera il budget mensile di una categoria"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun scheduleRicorrenzaWorker() {
        val request = PeriodicWorkRequestBuilder<RicorrenzaWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ricorrenza_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

