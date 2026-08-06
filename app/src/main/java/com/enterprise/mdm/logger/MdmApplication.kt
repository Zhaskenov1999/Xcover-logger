package com.enterprise.mdm.logger

import android.app.Application

class MdmApplication : Application() {

    private lateinit var headsetMonitor: HeadsetMonitor

    override fun onCreate() {
        super.onCreate()
        headsetMonitor = HeadsetMonitor(this)
        headsetMonitor.startMonitoring()
    }
}
