package com.enterprise.mdm.logger

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MdmApplication : Application() {

    private lateinit var headsetMonitor: HeadsetMonitor

    override fun onCreate() {
        super.onCreate()
        
        // Запуск мониторинга наушников
        headsetMonitor = HeadsetMonitor(this)
        headsetMonitor.startMonitoring()

        // Запускаем первый тестовый сбор через 2 минуты
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val telemetryRequest = OneTimeWorkRequestBuilder<TelemetryWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(telemetryRequest)
    }
}
