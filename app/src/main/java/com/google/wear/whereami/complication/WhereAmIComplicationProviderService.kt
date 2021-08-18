// Copyright 2018 Google LLC
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
package com.google.wear.whereami.complication

import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.os.RemoteException
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.datasource.ComplicationDataSourceService
import androidx.wear.complications.datasource.ComplicationRequest
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.data.ResolvedLocation
import com.google.wear.whereami.format.toComplicationData
import com.google.wear.whereami.kt.CoroutinesComplicationDataSourceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*

class WhereAmIComplicationProviderService : CoroutinesComplicationDataSourceService() {
    private lateinit var locationViewModel: LocationViewModel

    override fun onCreate() {
        super.onCreate()
        locationViewModel = LocationViewModel(applicationContext)
    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest) =
        toComplicationData(complicationRequest.complicationType, locationViewModel.readLocationResult())

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.longitude = 0.0
        location.latitude = 0.0
        val address = Address(Locale.ENGLISH)
        address.countryName = "Null Island"

        val locationResult = ResolvedLocation(location, address)

        return toComplicationData(type, locationResult)
    }
}