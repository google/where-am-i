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
package com.google.wear.whereami

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.pm.PackageManager
import android.location.Address
import android.location.Location
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Pair
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.location.LocationRequest
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.getAddressDescription
import com.patloew.rxlocation.RxLocation
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class WhereAmIActivity : FragmentActivity() {
    private val subscriptions = CompositeDisposable()
    private var textView: TextView? = null
    private var rxLocation: RxLocation? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_am_i_activity)
        textView = findViewById<View>(R.id.text) as TextView
        rxLocation = RxLocation(this)
        subscriptions.add(
            checkPermissions()
                .subscribeOn(Schedulers.io())
                .flatMap {
                    rxLocation!!.location().updates(createLocationRequest())
                }
                .flatMapMaybe { location: Location ->
                    rxLocation!!.geocoding()
                        .fromLocation(location)
                        .map { address: Address -> Pair.create(location, address) }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( // onNext
                    { locationAndAddress: Pair<Location, Address> ->
                        textView!!.text = getString(
                            R.string.address_as_of_time_activity,
                            getAddressDescription(this, locationAndAddress.second),
                            getTimeAgo(locationAndAddress.first.time)
                        )
                    }
                )  // onError
                { textView!!.setText(R.string.location_error) }
        )
    }

    public override fun onDestroy() {
        subscriptions.dispose()
        super.onDestroy()
    }

    override fun onStop() {
        forceComplicationUpdate()
        super.onStop()
    }

    private fun forceComplicationUpdate() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val request = ComplicationDataSourceUpdateRequester(
                applicationContext, ComponentName.createRelative(
                    applicationContext, ".complication.WhereAmIComplicationProviderService"
                )
            )
            request.requestUpdateAll()
        }
    }

    private fun checkPermissions(): Observable<Boolean> {
        return RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
            .map { isGranted: Boolean ->
                if (isGranted) return@map true
                throw SecurityException("No location permission")
            }
    }

    private fun getTimeAgo(time: Long): CharSequence {
        return DateUtils.getRelativeTimeSpanString(time)
    }

    companion object {
        private fun createLocationRequest(): LocationRequest {
            return LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TimeUnit.SECONDS.toMillis(10))
                .setSmallestDisplacement(50f)
        }
    }
}