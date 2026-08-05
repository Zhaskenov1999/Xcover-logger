package com.enterprise.mdm.logger

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LogDao { // или EventDao, в зависимости от того, как вы его назвали

    // ИСПРАВЛЕНИЕ: Меняем название таблицы на events
    @Query("SELECT * FROM events")
    fun getUnsyncedEvents(): List<LogEvent>

    @Insert
    fun insert(event: LogEvent)

    @Delete
    fun delete(event: LogEvent)
}
