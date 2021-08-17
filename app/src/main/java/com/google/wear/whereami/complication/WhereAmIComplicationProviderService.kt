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

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.location.Address
import android.location.Location
import android.location.LocationManager
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import androidx.wear.complications.data.*
import androidx.wear.complications.datasource.ComplicationDataSourceService
import androidx.wear.complications.datasource.ComplicationRequest
import com.google.android.gms.location.LocationRequest
import com.google.wear.whereami.*
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class WhereAmIComplicationProviderService : ComplicationDataSourceService() {
    private lateinit var coGeocoder: CoGeocoder
    private lateinit var coLocation: CoLocation

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onCreate() {
        super.onCreate()

        coLocation = CoLocation.from(applicationContext)
        coGeocoder = CoGeocoder.from(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onComplicationUpdate(): $request")
        }

        serviceScope.launch {
            updateComplication(request, listener, readLocationResult())
        }
    }

    suspend fun readLocationResult(): LocationResult {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return PermissionError
        }

        val location =
            coLocation.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                ?: return NoLocation

        val address = coGeocoder.getAddressFromLocation(location) ?: return Unknown

        val locationResult = ResolvedLocation(location, address)
        return ResolvedLocation(location, address)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.longitude = 0.0
        location.latitude = 0.0
        val address = Address(Locale.ENGLISH)
        address.countryName = "Null Island"

        val locationResult = ResolvedLocation(location, address)

        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getTimeAgo(locationResult),
                getFullDescription(locationResult)
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_my_location
                        )
                    ).build()
                )
                .setTapAction(tapAction)
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                getAddressDescriptionText(this, locationResult),
                getFullDescription(locationResult)
            )
                .setTitle(getTimeAgo(locationResult))
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_my_location
                        )
                    ).build()
                )
                .setTapAction(tapAction)
                .build()
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    @Throws(RemoteException::class)
    private fun updateComplication(
        complicationRequest: ComplicationRequest,
        complicationRequestListener: ComplicationRequestListener,
        location: LocationResult
    ) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Address: $location")
        }

        var complicationData: ComplicationData? = null
        when (complicationRequest.complicationType) {
            ComplicationType.SHORT_TEXT -> complicationData =
                ShortTextComplicationData.Builder(
                    getTimeAgo(location),
                    getFullDescription(location)
                )
                    .setMonochromaticImage(
                        MonochromaticImage.Builder(
                            Icon.createWithResource(
                                this,
                                R.drawable.ic_my_location
                            )
                        ).build()
                    )
                    .setTapAction(tapAction)
                    .build()
            ComplicationType.LONG_TEXT -> complicationData = LongTextComplicationData.Builder(
                getAddressDescriptionText(this, location),
                getFullDescription(location)
            )
                .setTitle(getTimeAgo(location))
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_my_location
                        )
                    ).build()
                )
                .setTapAction(tapAction)
                .build()
            else -> if (Log.isLoggable(TAG, Log.WARN)) {
                Log.w(TAG, "Unexpected complication type $complicationRequest")
            }
        }

        // If no data is sent, we still need to inform the ComplicationManager, so
        // the update job can finish and the wake lock isn't held any longer.
        complicationRequestListener.onComplicationData(complicationData)
    }

    private val tapAction: PendingIntent
        get() {
            val intent = Intent(this, WhereAmIActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

    private fun getFullDescription(location: LocationResult): ComplicationText {
        return if (location is ResolvedLocation)
            getTimeAgo(location.location.time).build()
        else
            PlainComplicationText.Builder(getString(R.string.no_location)).build()
    }

    private fun getTimeAgo(location: LocationResult): ComplicationText {
        return if (location is ResolvedLocation)
            getTimeAgo(location.location.time).build()
        else
            PlainComplicationText.Builder("--").build()
    }

    private fun getTimeAgo(fromTime: Long): TimeDifferenceComplicationText.Builder {
        return TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            CountUpTimeReference(fromTime)
        ).apply {
            setMinimumTimeUnit(TimeUnit.MINUTES)
            setDisplayAsNow(true)
        }
    }

    companion object {
        private const val TAG = "WhereAmIComplication"

        fun getAddressDescriptionText(context: Context, location: LocationResult): ComplicationText {
            return if (location is ResolvedLocation)
                return PlainComplicationText.Builder(getAddressDescription(context, location)).build()
            else
                PlainComplicationText.Builder(context.getString(R.string.no_location)).build()

        }

        fun getAddressDescription(context: Context, location: ResolvedLocation): String {
            val address = location.address
            val subThoroughfare = address.subThoroughfare
            val thoroughfare = address.thoroughfare ?: return address.countryName
            return (if (TextUtils.isEmpty(subThoroughfare)) "" else "$subThoroughfare ") + thoroughfare
        }
    }
}