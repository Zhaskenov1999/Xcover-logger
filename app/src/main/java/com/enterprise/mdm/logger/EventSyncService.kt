package com.enterprise.mdm.logger

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class EventSyncService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ловим данные от BroadcastReceiver (наушники, зарядка, Bluetooth)
        val customEventType = intent?.getStringExtra("EVENT_TYPE")
        val customEventDetails = intent?.getStringExtra("EVENT_DETAILS")

        if (intent?.action == "ACTION_FETCH_LOCATION") {
            fetchLocation()
        } else if (customEventType != null) {
            // Если пришло событие аппаратуры, сохраняем его и отправляем
            saveHardwareEvent(customEventType, customEventDetails)
        } else {
            // Обычная синхронизация по таймеру
            syncLogs()
        }
        return START_NOT_STICKY
    }

    private fun saveHardwareEvent(eventType: String, eventDetails: String?) {
        serviceScope.launch {
            val db = LocalDatabase.getInstance(applicationContext)
            
            // Формируем понятный статус, например: "bluetooth_device_connected"
            val finalEventType = if (eventDetails != null && eventDetails != "none") {
                "${eventType}_${eventDetails}"
            } else {
                eventType
            }

            // Записываем событие в локальную базу данных
            db.eventDao().insert(
                LogEvent(
                    mobile_id = Build.SERIAL,
                    device_model = Build.MODEL,
                    event_type = finalEventType,
                    lat = null, // Для наушников координаты не нужны
                    lon = null,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Сразу инициируем отправку накопленных логов на сервер
            syncLogs()
        }
    }

    private fun fetchLocation() {
        serviceScope.launch {
            val context = applicationContext
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                stopSelf()
                return@launch
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val db = LocalDatabase.getInstance(context)
            db.eventDao().insert(
                LogEvent(
                    mobile_id = Build.SERIAL,
                    device_model = Build.MODEL,
                    event_type = "LOCATION_UPDATE",
                    lat = location?.latitude,
                    lon = location?.longitude,
                    timestamp = System.currentTimeMillis()
                )
            )
            syncLogs()
        }
    }

    private fun syncLogs() {
        serviceScope.launch {
            val db = LocalDatabase.getInstance(applicationContext)
            val events = db.eventDao().getUnsyncedEvents()
            if (events.isEmpty()) {
                stopSelf()
                return@launch
            }

            // Ваш временный локальный IP для тестов
            val serverUrl = "http://192.168.1.8:8080/"

            try {
                val url = URL(serverUrl)
                for (event in events) {
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                    }

                    val jsonBody = if (event.lat != null && event.lon != null) {
                        """
                        {
                          "mobile_id": "${event.mobile_id}",
                          "device_model": "${event.device_model}",
                          "event_type": "${event.event_type}",
                          "sql": { "metrics": { "location": { "lat": ${event.lat}, "lon": ${event.lon} } } },
                          "@timestamp": "${event.timestamp}"
                        }
                        """.trimIndent()
                    } else {
                        """
                        {
                          "mobile_id": "${event.mobile_id}",
                          "device_model": "${event.device_model}",
                          "event_type": "${event.event_type}",
                          "@timestamp": "${event.timestamp}"
                        }
                        """.trimIndent()
                    }

                    conn.outputStream.write(jsonBody.toByteArray(Charsets.UTF_8))
                    if (conn.responseCode in 200..299) {
                        db.eventDao().delete(event)
                    }
                    conn.disconnect()
                }
            } catch (e: Exception) {
                // Ошибка сети — данные ждут в базе до следующей синхронизации
            } finally {
                stopSelf()
            }
        }
    }

    override fun onBind(intent: Intent?) = null
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun scheduleLocationAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HardwareEventReceiver::class.java).apply {
                action = "com.enterprise.mdm.ACTION_CHECK_LOCATION"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val interval = 15 * 60 * 1000L // 15 минут
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + interval, interval, pendingIntent)
        }
    }
}
