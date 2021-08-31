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
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.MemoryPolicy
import com.dropbox.android.external.store4.Store
import com.dropbox.android.external.store4.StoreBuilder
import com.google.android.gms.location.LocationRequest
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Duration

class LocationViewModel(private val applicationContext: Context, private val coroutineScope: CoroutineScope) {
    private val coGeocoder = CoGeocoder.from(applicationContext)
    private val coLocation = CoLocation.from(applicationContext)

    @OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class,
        kotlin.time.ExperimentalTime::class
    )
    val store: Store<String, LocationResult> = StoreBuilder
        .from<String, LocationResult>(Fetcher.of { readLocationResult() })
        .cachePolicy(MemoryPolicy.builder<String, LocationResult>()
            .setMaxSize(10)
            .setExpireAfterWrite(Duration.Companion.minutes(1))
            .build())
        .build()

    private suspend fun readLocationResult(): LocationResult {
        if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return PermissionError
        }

        return withContext(Dispatchers.IO) {
            var start = System.currentTimeMillis()

            val location = try {
                coLocation.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY)
            } finally {
               println("getCurrentLocation took " + (System.currentTimeMillis() - start))
            }

            if (location == null) {
                NoLocation
            } else {
                start = System.currentTimeMillis()

                val address = try {
                    coGeocoder.getAddressFromLocation(location)
                } catch (ioe: IOException) {
                    return@withContext LocationError(ioe)
                } finally {
                    println("getAddressFromLocation took " + (System.currentTimeMillis() - start))
                }

                if (address == null) {
                    Unknown
                } else {
                    ResolvedLocation(location, address)
                }
            }
        }
    }

    companion object {
        val Current = "current"
    }
}