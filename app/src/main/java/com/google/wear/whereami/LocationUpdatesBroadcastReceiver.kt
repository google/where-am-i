package com.google.wear.whereami

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.launch


class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result: LocationResult? = LocationResult.extractResult(intent)
        val location = result?.lastLocation

        if (location == null) {
            Log.w("WhereAmI", "No locations in update")
            return
        }

        context.applicationScope.launch {
            context.locationViewModel.readFreshLocationResult(location)
        }
    }

    companion object {
        const val ACTION_PROCESS_UPDATES =
            "com.google.wear.whereami.locationupdatespendingintent.action.PROCESS_UPDATES"
    }
}