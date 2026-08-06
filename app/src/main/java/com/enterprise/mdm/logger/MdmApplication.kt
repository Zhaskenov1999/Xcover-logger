package com.enterprise.mdm.logger // Укажите ваш пакет

import android.app.Application

class MdmApplication : Application() {

    private lateinit var headsetMonitor: HeadsetMonitor

    override fun onCreate() {
        super.onCreate()
        
        // Запускаем мониторинг наушников сразу при старте приложения/сервиса
        headsetMonitor = HeadsetMonitor(this)
        headsetMonitor.startMonitoring()
    }
}
