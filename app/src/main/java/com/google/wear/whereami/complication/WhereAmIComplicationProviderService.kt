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
import android.util.Pair
import androidx.wear.complications.data.*
import androidx.wear.complications.datasource.ComplicationDataSourceService
import androidx.wear.complications.datasource.ComplicationRequest
import com.google.android.gms.location.LocationRequest
import com.google.wear.whereami.R
import com.google.wear.whereami.WhereAmIActivity
import com.patloew.rxlocation.RxLocation
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit

class WhereAmIComplicationProviderService : ComplicationDataSourceService() {
    private var rxLocation: RxLocation? = null
    private val subscriptions = CompositeDisposable()
    override fun onCreate() {
        super.onCreate()
        rxLocation = RxLocation(this)
    }

    override fun onDestroy() {
        subscriptions.dispose()
        super.onDestroy()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onComplicationUpdate(): $request")
        }
        val task: Observable<Pair<Location, Address>>
        task =
            if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationProvider = rxLocation!!.location()
                locationProvider
                    .isLocationAvailable
                    .subscribeOn(Schedulers.io())
                    .flatMapObservable { hasLocation: Boolean ->
                        if (hasLocation) locationProvider.lastLocation()
                            .toObservable() else locationProvider.updates(
                            createLocationRequest()
                        )
                    }
                    .flatMapMaybe { location: Location ->
                        rxLocation!!.geocoding()
                            .fromLocation(location)
                            .map { address: Address -> Pair.create(location, address) }
                    }
            } else {
                Observable.error(SecurityException("No location permission!"))
            }
        subscriptions.add(
            task
                .subscribe( // onNext
                    { locationAddressPair: Pair<Location, Address> ->
                        updateComplication(
                            request,
                            listener,
                            locationAddressPair.first,
                            locationAddressPair.second
                        )
                    }
                )  // onError
                { error: Throwable? ->
                    Log.w(TAG, "Error retrieving location", error)
                    updateComplication(request, listener, null, null)
                }
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val location = Location(LocationManager.GPS_PROVIDER)
        location.longitude = 0.0
        location.latitude = 0.0
        val address = Address(Locale.ENGLISH)
        address.countryName = "Null Island"
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getTimeAgo(location),
                getFullDescription(location, address)
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
                getAddressDescriptionText(this, address),
                getFullDescription(location, address)
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
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    @Throws(RemoteException::class)
    private fun updateComplication(
        complicationRequest: ComplicationRequest,
        complicationRequestListener: ComplicationRequestListener,
        location: Location?,
        address: Address?
    ) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Address: $address")
        }
        var complicationData: ComplicationData? = null
        when (complicationRequest.complicationType) {
            ComplicationType.SHORT_TEXT -> complicationData =
                ShortTextComplicationData.Builder(
                    getTimeAgo(location),
                    getFullDescription(location, address)
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
                getAddressDescriptionText(this, address),
                getFullDescription(location, address)
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

    private fun getFullDescription(location: Location?, address: Address?): ComplicationText {
        return if (location == null || address == null) PlainComplicationText.Builder(getString(R.string.no_location))
            .build() else getTimeAgo(
            location.time
        )
            .build()
    }

    private fun getTimeAgo(location: Location?): ComplicationText {
        return if (location == null) PlainComplicationText.Builder("--").build() else getTimeAgo(
            location.time
        ).build()
    }

    private fun getTimeAgo(fromTime: Long): TimeDifferenceComplicationText.Builder {
        return TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            CountUpTimeReference(fromTime)
        )
            .setMinimumTimeUnit(TimeUnit.MINUTES)
            .setDisplayAsNow(true)
    }

    companion object {
        private const val TAG = "WhereAmIComplication"
        fun getAddressDescriptionText(context: Context, address: Address?): ComplicationText {
            return PlainComplicationText.Builder(getAddressDescription(context, address)).build()
        }

        fun getAddressDescription(context: Context, address: Address?): String {
            if (address == null) return context.getString(R.string.no_location)
            val subThoroughfare = address.subThoroughfare
            val thoroughfare = address.thoroughfare ?: return address.countryName
            return (if (TextUtils.isEmpty(subThoroughfare)) "" else "$subThoroughfare ") + thoroughfare
        }

        fun createLocationRequest(): LocationRequest {
            return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setNumUpdates(1)
                .setExpirationDuration(TimeUnit.SECONDS.toMillis(30))
        }
    }
}