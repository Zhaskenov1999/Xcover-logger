package com.enterprise.mdm.logger

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class HeadsetMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            addedDevices?.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    val message = "Проводные наушники подключены"
                    Log.d("HeadsetMonitor", message)
                    sendEventToServer(message) // Отправка на Python-сервер
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            removedDevices?.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    val message = "Проводные наушники отключены"
                    Log.d("HeadsetMonitor", message)
                    sendEventToServer(message) // Отправка на Python-сервер
                }
            }
        }
    }

    private fun sendEventToServer(eventText: String) {
        // Выполняем сетевой запрос в отдельном потоке, так как в Android нельзя делать сеть в главном потоке
        thread {
            try {
                // ЗАМЕНИТЕ НА IP ВАШЕГО КОМПЬЮТЕРА И ПОРТ PYTHON-СЕРВЕРА
                val url = URL("http://192.168.1.50:5000/log") 
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true

                // Формируем простой JSON
                val jsonInputString = "{\"event\": \"$eventText\", \"device\": \"Samsung SM-G556B\"}"
                
                connection.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                Log.d("HeadsetMonitor", "Лог отправлен на сервер, код ответа: $responseCode")
            } catch (e: Exception) {
                Log.e("HeadsetMonitor", "Ошибка отправки лога на сервер: ${e.message}")
            }
        }
    }

    fun startMonitoring() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
    }

    fun stopMonitoring() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
}
