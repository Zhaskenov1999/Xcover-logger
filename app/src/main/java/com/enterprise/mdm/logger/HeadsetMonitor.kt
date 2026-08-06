package com.enterprise.mdm.logger

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log

class HeadsetMonitor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            addedDevices?.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    Log.d("HeadsetMonitor", "Проводные наушники подключены")
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            removedDevices?.forEach { device ->
                if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || 
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                    Log.d("HeadsetMonitor", "Проводные наушники отключены")
                }
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
