package com.google.wear.whereami

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.dropbox.android.external.store4.StoreRequest
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.forceComplicationUpdate
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.data.LocationViewModel.Companion.Current
import com.google.wear.whereami.tile.WhereAmITileProviderService.Companion.forceTileUpdate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class WhereAmIApplication: Application(), LifecycleObserver {
    lateinit var locationViewModel: LocationViewModel
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()

        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        locationViewModel = LocationViewModel(applicationContext, applicationScope)

        applicationScope.launch {
            val flow = locationViewModel.store.stream(StoreRequest.cached(Current, refresh = true))

            flow.collect {
                forceComplicationUpdate()
                forceTileUpdate()
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppShutdown() {
        applicationScope.cancel()
    }
}