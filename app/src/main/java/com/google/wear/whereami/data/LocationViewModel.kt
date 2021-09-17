// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.wear.whereami.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.wear.whereami.LocationUpdatesBroadcastReceiver
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Duration
import java.time.Instant

class LocationViewModel(
    private val applicationContext: Context
) {
    private val coGeocoder = CoGeocoder.from(applicationContext)
    private val coLocation = CoLocation.from(applicationContext)
    private val locationDao = AppDatabase.getDatabase(applicationContext).locationDao()
    private val updateHandler = UpdateHandler(applicationContext)

    val locationServices = LocationServices.getFusedLocationProviderClient(applicationContext)

    val intent: PendingIntent =
        Intent(applicationContext, LocationUpdatesBroadcastReceiver::class.java).run {
            action = LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                this,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

    suspend fun readFreshLocationResult(freshLocation: Location? = null) {
        val location = if (freshLocation == null) {
            val permissionCheck = withContext(Dispatchers.Main) {
                if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    LocationResult.PermissionError
                } else {
                    null
                }
            }

            if (permissionCheck != null) {
                null
            } else {
                coLocation.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY)
            }
        } else {
            freshLocation
        }

        if (location != null) {
            val locationResult = addressForLocation(location)

            locationDao.upsertLocation(locationResult)
            updateHandler.process(locationResult)
        }
    }

    private suspend fun addressForLocation(location: Location): LocationResult {
        return try {
            val address = coGeocoder.getAddressFromLocation(location)

            if (address == null) {
                LocationResult.Unknown
            } else {
                LocationResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = getAddressDescription(address),
                    time = Instant.ofEpochMilli(location.time)
                )
            }
        } catch (ioe: IOException) {
            Log.w("WhereAmI", "getAddressFromLocation failed", ioe)
            LocationResult(
                error = LocationResult.ErrorType.Failed,
                errorMessage = ioe.message
            )
        }
    }

    fun getAddressDescription(address: Address): String {
        val subThoroughfare = address.subThoroughfare
        val thoroughfare = address.thoroughfare ?: return address.countryName
        return (if (TextUtils.isEmpty(subThoroughfare)) "" else "$subThoroughfare ") + thoroughfare
    }

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun subscribe() {
        val permissionCheck = withContext(Dispatchers.Main) {
            applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.w("WhereAmI", "location permission not granted: $permissionCheck")
            return
        }

        val request = LocationRequest.create().apply {
            isWaitForAccurateLocation = true
            smallestDisplacement = 10f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fastestInterval = Duration.ofSeconds(45).toMillis()
            interval = Duration.ofMinutes(3).toMillis()
        }

        locationServices.requestLocationUpdates(request, intent).await()
    }

    fun databaseLocationStream(): Flow<LocationResult> {
        return locationDao.getLocation().filterNotNull()
    }

    suspend fun setInitialLocation() {
        locationDao.insertLocation(LocationResult.Unknown)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun subscribeRealtime() {
        val permissionCheck = withContext(Dispatchers.Main) {
            applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.w("WhereAmI", "realtime... location permission not granted: $permissionCheck")
            return
        }

        val request = LocationRequest.create().apply {
            isWaitForAccurateLocation = true
            smallestDisplacement = 10f
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fastestInterval = Duration.ofSeconds(15).toMillis()
            interval = Duration.ofSeconds(45).toMillis()
        }

        Log.i("WhereAmI", "subscribeRealtime")
        coLocation.getLocationUpdates(request).collect {
            readFreshLocationResult(it)
        }
    }
}