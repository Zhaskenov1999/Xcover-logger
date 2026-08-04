package com.enterprise.mdm.logger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HardwareEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        var eventType = "unknown"
        var eventDetails = "none"

        // Определяем, какое именно событие разбудило приложение
        when (action) {
            "android.intent.action.HEADSET_PLUG" -> {
                // У проводных наушников есть параметр state: 1 = подключены, 0 = отключены
                val state = intent.getIntExtra("state", -1)
                eventType = "headset_wired"
                eventDetails = if (state == 1) "connected" else "disconnected"
            }
            "android.bluetooth.device.action.ACL_CONNECTED" -> {
                eventType = "bluetooth_device"
                eventDetails = "connected"
            }
            "android.bluetooth.device.action.ACL_DISCONNECTED" -> {
                eventType = "bluetooth_device"
                eventDetails = "disconnected"
            }
            "android.intent.action.ACTION_POWER_DISCONNECTED" -> {
                eventType = "power_cable"
                eventDetails = "disconnected"
            }
            "android.intent.action.BOOT_COMPLETED" -> {
                eventType = "system"
                eventDetails = "device_rebooted"
            }
        }

        // Если событие распознано, запускаем наш сервис для отправки лога на сервер
        if (eventType != "unknown") {
            val serviceIntent = Intent(context, EventSyncService::class.java).apply {
                putExtra("EVENT_TYPE", eventType)
                putExtra("EVENT_DETAILS", eventDetails)
            }
            
            // Запускаем фоновую службу (на Android 8+ рекомендуется использовать startForegroundService, 
            // если приложение не в белом списке батареи, но так как у нас MDM - сработает и обычный)
            context.startService(serviceIntent)
        }
    }
}
