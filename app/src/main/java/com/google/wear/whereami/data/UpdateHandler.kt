package com.google.wear.whereami.data

import android.content.Context
import android.util.Log
import com.google.wear.whereami.applicationScope
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.forceComplicationUpdate
import com.google.wear.whereami.tile.WhereAmITileProviderService.Companion.forceTileUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.coroutineContext

class UpdateHandler(private val applicationContext: Context) {
    private var lastValue = LocationResult.Unknown
    private var updateSent = Instant.ofEpochMilli(0)

    suspend fun process(value: LocationResult) {
        val isNewer = value.newer(lastValue)
        Log.i(
            "WhereAmI",
            "Freshness check: old: ${lastValue.freshness} new: ${value.freshness} isNewer: $isNewer time: ${value.time}"
        )

        if (!isNewer) {
            Log.i("WhereAmI", "Dropping update")
        } else {
            lastValue = value

            // updates more frequently than 30 seconds are dropped
            if (Instant.now() < updateSent.plusSeconds(30)) {
                Log.i("WhereAmI", "also sending a delayed updates")

                applicationContext.applicationScope.launch {
                    delay(Duration.ofSeconds(30).toMillis())
                    sendUpdate()
                }
            }

            sendUpdate()
        }
    }

    private fun sendUpdate() {
        updateSent = Instant.now()

        Log.i("WhereAmI", "locationViewModel forcing updates")
        applicationContext.forceComplicationUpdate()
        applicationContext.forceTileUpdate()
    }
}