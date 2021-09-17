package com.google.wear.whereami.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM locationresult WHERE id = :id LIMIT 1")
    fun getLocation(id: Int = 0): Flow<LocationResult?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: LocationResult)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: LocationResult)
}