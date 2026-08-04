package com.enterprise.mdm.logger

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogEvent::class], version = 1)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun eventDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getInstance(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "mdm_logs.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
