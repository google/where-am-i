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
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.android.gms.location.LocationRequest
import com.google.wear.whereami.getAddressDescription
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant

class LocationViewModel(
    private val applicationContext: Context
) {
    private val coGeocoder = CoGeocoder.from(applicationContext)
    private val coLocation = CoLocation.from(applicationContext)
    private val locationDao = AppDatabase.getDatabase(applicationContext).locationDao()

    suspend fun readFreshLocationResult(): LocationResult {
        val permissionCheck = withContext(Dispatchers.Main) {
            if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LocationResult.PermissionError
            } else {
                null
            }
        }

        if (permissionCheck != null) {
            return permissionCheck
        }

        val location = withContext(Dispatchers.IO) {
            var start = System.currentTimeMillis()

            val location = try {
                coLocation.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY)
            } finally {
                Log.i(
                    "WhereAmI",
                    "getAddressFromLocation took " + (System.currentTimeMillis() - start)
                )
            }

            if (location == null) {
                LocationResult.Unknown
            } else {
                start = System.currentTimeMillis()

                val address = try {
                    coGeocoder.getAddressFromLocation(location)
                } catch (ioe: IOException) {
                    Log.w("WhereAmI", "getAddressFromLocation failed", ioe)
                    return@withContext LocationResult(
                        error = LocationResult.ErrorType.Failed,
                        errorMessage = ioe.message
                    )
                } finally {
                    Log.i(
                        "WhereAmI",
                        "getAddressFromLocation took " + (System.currentTimeMillis() - start)
                    )
                }

                if (address == null) {
                    LocationResult.Unknown
                } else {
                    LocationResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        locationName = applicationContext.getAddressDescription(address),
                        time = Instant.ofEpochMilli(location.time)
                    )
                }
            }
        }

        withContext(Dispatchers.IO) {
            locationDao.upsertLocation(location)
        }

        return location
    }

    fun locationStream(): Flow<LocationResult> {
        return locationDao.getLocation().filterNotNull()
    }

    fun setInitialLocation() {
        locationDao.insertLocation(LocationResult.Unknown)
    }
}