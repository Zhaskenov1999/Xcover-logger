package com.enterprise.mdm.logger

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MdmApplication : Application() {

    private lateinit var headsetMonitor: HeadsetMonitor

    override fun onCreate() {
        super.onCreate()
        
        // 1. Запускаем мониторинг наушников
        headsetMonitor = HeadsetMonitor(this)
        headsetMonitor.startMonitoring()

        // 2. Запускаем периодический сбор телеметрии (каждые 15 минут)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Только при наличии сети
            .build()

        val telemetryRequest = PeriodicWorkRequestBuilder<TelemetryWorker>(, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(telemetryRequest)
    }
}
