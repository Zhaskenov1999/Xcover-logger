package com.enterprise.mdm.logger

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class TelemetryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        // Запускаем наш EventSyncService со специальным типом события для телеметрии
        val intent = Intent(applicationContext, EventSyncService::class.java).apply {
            putExtra("EVENT_TYPE", "PERIODIC_TELEMETRY")
            putExtra("EVENT_DETAILS", "status_update")
        }
        
        try {
            applicationContext.startService(intent)
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
