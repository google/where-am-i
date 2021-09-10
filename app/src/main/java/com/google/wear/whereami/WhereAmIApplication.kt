package com.google.wear.whereami

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleObserver
import com.google.wear.whereami.data.LocationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

class WhereAmIApplication : Application(), LifecycleObserver {
    lateinit var locationViewModel: LocationViewModel
    lateinit var applicationScope: CoroutineScope

    @OptIn(ExperimentalTime::class)
    override fun onCreate() {
        super.onCreate()

        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        locationViewModel = LocationViewModel(applicationContext)

        applicationScope.launch {
            // Populate an unknown location if not existing value
            locationViewModel.setInitialLocation()

            // Subscribe the LocationUpdatesBroadcastReceiver
            Log.i("WhereAmI", "locationViewModel.subscribe")
            locationViewModel.subscribe()
        }
    }
}

val Context.locationViewModel: LocationViewModel
    get() = (this.applicationContext as WhereAmIApplication).locationViewModel

val Context.applicationScope: CoroutineScope
    get() = (this.applicationContext as WhereAmIApplication).applicationScope