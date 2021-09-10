package com.google.wear.whereami.data

import android.content.Context
import android.util.Log
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.forceComplicationUpdate
import com.google.wear.whereami.tile.WhereAmITileProviderService.Companion.forceTileUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class UpdateHandler(private val applicationContext: Context) {
    private var lastValue = LocationResult.Unknown
    private var updateSent = Instant.ofEpochMilli(0)

    suspend fun process(value: LocationResult) {
        val oldFreshness = lastValue.freshness
        val newFreshness = value.freshness

        val dropping = newFreshness > oldFreshness
        Log.i("WhereAmI", "Freshness check: old: $oldFreshness new: $newFreshness dropping: $dropping time: ${value.time}")

        if (dropping) {
            Log.i("WhereAmI", "Dropping update")
        } else {
            lastValue = value

            // updates more frequently than 30 seconds are dropped
            if (Instant.now() < updateSent.plusSeconds(30)) {
                delay(Duration.ofSeconds(30).toMillis())
            }

            updateSent = Instant.now()
            withContext(Dispatchers.Main) {
                Log.i("WhereAmI", "locationViewModel forcing updates")
                applicationContext.forceComplicationUpdate()
                applicationContext.forceTileUpdate()
            }
        }
    }
}