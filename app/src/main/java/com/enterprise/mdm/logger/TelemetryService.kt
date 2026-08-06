package com.enterprise.mdm

import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothHeadset
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class TelemetryService : Service() {

    private lateinit var bluetoothReceiver: BluetoothTelemetryReceiver

    override fun onCreate() {
        super.onCreate()
        Log.i("AppTelemetry", "Служба телеметрии Bluetooth запущена")

        bluetoothReceiver = BluetoothTelemetryReceiver()
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }
        
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY 
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            Log.e("AppTelemetry", "Ошибка при отписке ресивера", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
