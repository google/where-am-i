package com.google.wear.whereami

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.data.UpdateHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
            locationViewModel.setInitialLocation()

            // publish location changes to database
            launch {
                locationViewModel.subscribe()
            }

            // database stored update stream
            val flow = locationViewModel.databaseLocationStream()

            val updateHandler = UpdateHandler(applicationContext)
            flow.collect {
                updateHandler.process(it)
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onAppShutdown() {
        applicationScope.cancel()
    }
}