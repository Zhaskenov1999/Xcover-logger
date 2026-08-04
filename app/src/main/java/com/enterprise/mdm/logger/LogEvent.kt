package com.enterprise.mdm.logger

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mobile_id: String,
    val device_model: String,
    val event_type: String,
    val lat: Double?,
    val lon: Double?,
    val timestamp: Long
)
