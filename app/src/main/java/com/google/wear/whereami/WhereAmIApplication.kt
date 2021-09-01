package com.google.wear.whereami

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.forceComplicationUpdate
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.tile.WhereAmITileProviderService.Companion.forceTileUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.time.ExperimentalTime

class WhereAmIApplication: Application(), LifecycleObserver {
    lateinit var locationViewModel: LocationViewModel
    lateinit var applicationScope: CoroutineScope

    @OptIn(ExperimentalTime::class)
    override fun onCreate() {
        super.onCreate()

        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        locationViewModel = LocationViewModel(applicationContext)

        applicationScope.launch {
            locationViewModel.setInitialLocation()

            val flow = locationViewModel.locationStream()

            flow.collect {
                forceComplicationUpdate()
                forceTileUpdate()
            }
        }

        applicationScope.launch {
            while (true) {
                locationViewModel.readFreshLocationResult()
                delay(kotlin.time.Duration.seconds(30))
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppShutdown() {
        applicationScope.cancel()
    }
}