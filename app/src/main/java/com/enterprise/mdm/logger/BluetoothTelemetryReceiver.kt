import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
// Если используете корутины для записи в БД:
// import kotlinx.coroutines.*

class BluetoothTelemetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // 1. Безопасное извлечение объекта устройства с учетом новых API (Android 13+)
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        if (device == null) {
            Log.w("AppTelemetry", "Получен интент без данных об устройстве")
            return
        }

        // 2. Получение текущего состояния профиля (Подключен/Отключен/В процессе)
        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED)

        // 3. Строгая проверка прав перед обращением к свойствам BluetoothDevice
        // Без этой проверки на Android 12+ приложение упадет с SecurityException
        val hasPermission = ActivityCompat.checkSelfPermission(
            context, 
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        val deviceName = if (hasPermission) {
            // Если разрешение есть, пытаемся получить имя (может быть null)
            device.name ?: "Unknown Device"
        } else {
            "Permission_Denied"
        }
        val macAddress = device.address ?: "Unknown MAC"

        // 4. Фильтрация и маршрутизация событий
        when (action) {
            // Обработка мультимедийного профиля (НАУШНИКИ)
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                handleProfileStateChange(context, "A2DP (Media)", state, deviceName, macAddress)
            }
            // Обработка профиля звонков (МИКРОФОН/ГАРНИТУРА)
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                handleProfileStateChange(context, "HFP (Calls/Mic)", state, deviceName, macAddress)
            }
        }
    }

    private fun handleProfileStateChange(
        context: Context, 
        profileType: String, 
        state: Int, 
        deviceName: String, 
        macAddress: String
    ) {
        when (state) {
            BluetoothProfile.STATE_CONNECTED -> {
                val logMessage = "Connected: $profileType | Name: $deviceName | MAC: $macAddress"
                Log.i("AppTelemetry", logMessage)
                
                // Здесь вызывается метод записи в локальную базу данных, 
                // отправка в SIEM или на ваш сервер логирования.
                saveTelemetryToDatabase(context, "CONNECT", profileType, deviceName, macAddress)
            }
            BluetoothProfile.STATE_DISCONNECTED -> {
                val logMessage = "Disconnected: $profileType | Name: $deviceName | MAC: $macAddress"
                Log.i("AppTelemetry", logMessage)
                
                saveTelemetryToDatabase(context, "DISCONNECT", profileType, deviceName, macAddress)
            }
            // Состояния STATE_CONNECTING и STATE_DISCONNECTING обычно не нужны для чистой статистики, 
            // но при желании их тоже можно отловить.
        }
    }

    private fun saveTelemetryToDatabase(
        context: Context, 
        event: String, 
        profile: String, 
        name: String, 
        mac: String
    ) {
        // ВАЖНО: onReceive выполняется в главном UI-потоке!
        // Операции записи в БД (Room/SQLite) или отправка по сети должны выполняться асинхронно.
        
        /* Пример с корутинами:
        CoroutineScope(Dispatchers.IO).launch {
            myDatabase.telemetryDao().insert(
                TelemetryRecord(
                    timestamp = System.currentTimeMillis(),
                    eventType = event,
                    bluetoothProfile = profile,
                    deviceName = name,
                    deviceMac = mac
                )
            )
        }
        */
    }
}
