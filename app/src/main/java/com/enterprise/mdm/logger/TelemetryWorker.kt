package com.enterprise.mdm.logger

import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class TelemetryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // 1. Отправляем запрос на сбор и отправку телеметрии в сервис
        val intent = Intent(applicationContext, EventSyncService::class.java).apply {
            putExtra("EVENT_TYPE", "PERIODIC_TELEMETRY")
            putExtra("EVENT_DETAILS", "status_update")
        }
        
        try {
            applicationContext.startService(intent)
        } catch (e: Exception) {
            // Ошибка запуска сервиса
        }

        // 2. Снова планируем этот же воркер через 2 минуты (цикл для тестов)
        val nextRequest = OneTimeWorkRequestBuilder<TelemetryWorker>()
            .setInitialDelay(2, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(nextRequest)

        return Result.success()
    }
}
