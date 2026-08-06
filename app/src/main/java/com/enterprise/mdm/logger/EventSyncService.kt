package com.enterprise.mdm.logger

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionEntry
import android.content.RestrictionsManager
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class EventSyncService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val customEventType = intent?.getStringExtra("EVENT_TYPE")
        val customEventDetails = intent?.getStringExtra("EVENT_DETAILS")

        if (intent?.action == "ACTION_FETCH_LOCATION") {
            fetchLocation()
        } else if (customEventType != null) {
            saveHardwareEvent(customEventType, customEventDetails)
        } else {
            syncLogs()
        }
        return START_NOT_STICKY
    }

    // Чтение уровня батареи
    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            applicationContext.registerReceiver(null, it)
        }
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level != -1 && scale != -1) (level * 100 / scale) else -1
    }

    // Чтение внутренней памяти (возвращает Pair<Занято МБ, Свободно МБ>)
    private fun getStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalSize = stat.blockCountLong * blockSize
        val availableSize = stat.availableBlocksLong * blockSize
        val usedSize = totalSize - availableSize
        return Pair(usedSize / (1024 * 1024), availableSize / (1024 * 1024))
    }

    // Чтение оператора SIM-карты
    private fun getSimOperator(): String {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return tm.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "no_sim_or_unknown"
    }

    // Получение IMEI и SN (надежный Enterprise-метод через Managed Configs)
    private fun getEnterpriseDeviceIds(): Pair<String, String> {
        val restrictionsManager = getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
        val appRestrictions = restrictionsManager.applicationRestrictions
        // Если EMM передает переменные, берем их. Иначе ставим заглушку для домашних тестов
        val imei = appRestrictions.getString("DEVICE_IMEI", "unknown_imei")
        val sn = appRestrictions.getString("DEVICE_SN", Build.SERIAL ?: "unknown_sn")
        return Pair(imei, sn)
    }

    private fun saveHardwareEvent(eventType: String, eventDetails: String?) {
        serviceScope.launch {
            val db = LocalDatabase.getInstance(applicationContext)
            val finalEventType = if (eventDetails != null && eventDetails != "none") {
                "${eventType}_${eventDetails}"
            } else {
                eventType
            }
            insertEventToDb(db, finalEventType, null, null)
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
            insertEventToDb(db, "LOCATION_UPDATE", location?.latitude, location?.longitude)
            syncLogs()
        }
    }

    // Единая функция для записи в базу (чтобы не дублировать код)
    private suspend fun insertEventToDb(db: LocalDatabase, eventType: String, lat: Double?, lon: Double?) {
        val storage = getStorageInfo()
        val ids = getEnterpriseDeviceIds()
        
        db.eventDao().insert(
            LogEvent(
                mobile_id = ids.second, // Используем SN как главный ID
                imei = ids.first,
                manufacturer = Build.MANUFACTURER,
                device_model = Build.MODEL,
                event_type = eventType,
                lat = lat,
                lon = lon,
                battery_level = getBatteryLevel(),
                storage_used_mb = storage.first,
                storage_free_mb = storage.second,
                sim_operator = getSimOperator(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun syncLogs() {
        serviceScope.launch {
            val db = LocalDatabase.getInstance(applicationContext)
            val events = db.eventDao().getUnsyncedEvents()
            if (events.isEmpty()) {
                stopSelf()
                return@launch
            }

            val serverUrl = "http://192.168.1.35:8080/"

            try {
                val url = URL(serverUrl)
                for (event in events) {
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        doOutput = true
                    }

                    // Формируем блок метрик
                    val metricsParams = mutableListOf<String>()
                    metricsParams.add(""""battery_level": ${event.battery_level}""")
                    metricsParams.add(""""storage_used_mb": ${event.storage_used_mb}""")
                    metricsParams.add(""""storage_free_mb": ${event.storage_free_mb}""")
                    metricsParams.add(""""sim_operator": "${event.sim_operator}"""")
                    
                    if (event.lat != null && event.lon != null) {
                        metricsParams.add(""""location": { "lat": ${event.lat}, "lon": ${event.lon} }""")
                    }

                    val metricsString = metricsParams.joinToString(", ")

                    // Итоговый JSON
                    val jsonBody = """
                    {
                      "mobile_id": "${event.mobile_id}",
                      "imei": "${event.imei}",
                      "manufacturer": "${event.manufacturer}",
                      "device_model": "${event.device_model}",
                      "event_type": "${event.event_type}",
                      "sql": { "metrics": { $metricsString } },
                      "@timestamp": "${event.timestamp}"
                    }
                    """.trimIndent()

                    conn.outputStream.write(jsonBody.toByteArray(Charsets.UTF_8))
                    if (conn.responseCode in 200..299) {
                        db.eventDao().delete(event)
                    }
                    conn.disconnect()
                }
            } catch (e: Exception) {
                // Ждем интернета
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
}
