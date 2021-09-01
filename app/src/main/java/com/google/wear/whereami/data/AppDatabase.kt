package com.google.wear.whereami.data

import android.content.Context
import androidx.room.*
import java.time.Instant

@Database(entities = [LocationResult::class], version = 1)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    class Converters {
        @TypeConverter
        fun fromTimestamp(value: Long?): Instant? {
            return value?.let { Instant.ofEpochMilli(it) }
        }

        @TypeConverter
        fun dateToTimestamp(date: Instant?): Long? {
            return date?.toEpochMilli()
        }
    }

    companion object {
        private lateinit var INSTANCE: AppDatabase

        fun getDatabase(context: Context): AppDatabase {
            return synchronized(this) {
                if (!::INSTANCE.isInitialized) {
                    val instance = Room.databaseBuilder(
                        context,
                        AppDatabase::class.java,
                        "locations"
                    )
                        .build()
                    INSTANCE = instance
                }

                INSTANCE
            }
        }

    }
}