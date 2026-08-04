package com.enterprise.mdm.logger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class HardwareEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val currentAction = intent.action ?: return
        
        when (currentAction) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                val type = if (state == 1) "HEADSET_PLUGGED" else "HEADSET_UNPLUGGED"
                saveAndSync(context, type, null, null)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                saveAndSync(context, "POWER_DISCONNECTED", null, null)
            }
            Intent.ACTION_BOOT_COMPLETED, "com.enterprise.mdm.ACTION_CHECK_LOCATION" -> {
                val serviceIntent = Intent(context, EventSyncService::class.java).apply {
                    this.action = "ACTION_FETCH_LOCATION"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                if (currentAction == Intent.ACTION_BOOT_COMPLETED) {
                    EventSyncService.scheduleLocationAlarm(context)
                }
            }
        }
    }

    private fun saveAndSync(context: Context, type: String, lat: Double?, lon: Double?) {
        Thread {
            val db = LocalDatabase.getInstance(context)
            db.eventDao().insert(
                LogEvent(
                    mobile_id = Build.SERIAL,
                    device_model = Build.MODEL,
                    event_type = type,
                    lat = lat,
                    lon = lon,
                    timestamp = System.currentTimeMillis()
                )
            )
            val serviceIntent = Intent(context, EventSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }.start()
    }
}
