package com.enterprise.mdm.logger

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvCount = findViewById<TextView>(R.id.tvCount)
        val btnSync = findViewById<Button>(R.id.btnSync)

        updateQueueCount(tvCount)

        btnSync.setOnClickListener {
            tvStatus.text = "Проверка связи с Elasticsearch..."
            tvStatus.setTextColor(Color.BLUE)

            activityScope.launch(Dispatchers.IO) {
                try {
                    val serverUrl = "http://192.168.1.8:8080/"
                    val url = URL(serverUrl)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        connectTimeout = 4000
                        readTimeout = 4000
                        doOutput = true
                    }

                    val testJson = """{"test": "ping", "@timestamp": "${System.currentTimeMillis()}"}"""
                    conn.outputStream.write(testJson.toByteArray(Charsets.UTF_8))
                    val responseCode = conn.responseCode
                    conn.disconnect()

                    withContext(Dispatchers.Main) {
                        if (responseCode in 200..299) {
                            tvStatus.text = "Успешно! Сервер ответил (Код: $responseCode)"
                            tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Зеленый
                        } else {
                            tvStatus.text = "Ошибка сервера! Код ответа: $responseCode"
                            tvStatus.setTextColor(Color.RED)
                        }
                        updateQueueCount(tvCount)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Нет связи с сервером!\nОшибка: ${e.localizedMessage}"
                        tvStatus.setTextColor(Color.RED)
                    }
                }
            }
        }
    }

    private fun updateQueueCount(tvCount: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = LocalDatabase.getInstance(applicationContext)
            val count = db.eventDao().getUnsyncedEvents().size
            withContext(Dispatchers.Main) {
                tvCount.text = "Логов в очереди на отправку: $count"
            }
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
