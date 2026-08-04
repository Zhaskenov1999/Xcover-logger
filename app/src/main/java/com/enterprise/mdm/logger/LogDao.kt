package com.enterprise.mdm.logger

import androidx.room.*

@Dao
interface LogDao {
    @Insert
    fun insert(event: LogEvent)

    @Query("SELECT * FROM logs ORDER BY timestamp ASC")
    fun getUnsyncedEvents(): List<LogEvent>

    @Delete
    fun delete(event: LogEvent)
}
