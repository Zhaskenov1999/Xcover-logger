package com.enterprise.mdm.logger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class LogEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mobile_id: String, // Будем использовать как SN или заданный ID
    val imei: String,
    val manufacturer: String,
    val device_model: String,
    val event_type: String,
    val lat: Double?,
    val lon: Double?,
    val battery_level: Int,
    val storage_used_mb: Long,
    val storage_free_mb: Long,
    val sim_operator: String,
    val timestamp: Long
)
