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
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.location.LocationRequest
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.getAddressDescription
import com.patloew.colocation.CoGeocoder
import com.patloew.colocation.CoLocation
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitSingle

class WhereAmIActivity : FragmentActivity() {
    private lateinit var coGeocoder: CoGeocoder
    private lateinit var coLocation: CoLocation
    private lateinit var textView: TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_am_i_activity)
        textView = findViewById(R.id.text)

        coLocation = CoLocation.from(applicationContext)
        coGeocoder = CoGeocoder.from(applicationContext)

        lifecycleScope.launch {
            try {
                checkPermissions()

                val location =
                    coLocation.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                        ?: throw IllegalArgumentException("No location")

                val address = coGeocoder.getAddressFromLocation(location)
                    ?: throw IllegalArgumentException("No location")

                val locationResult = ResolvedLocation(location, address)

                textView.text = getString(
                    R.string.address_as_of_time_activity,
                    getAddressDescription(this@WhereAmIActivity, locationResult),
                    getTimeAgo(locationResult.location.time)
                )
            } catch (e: Exception) {
                textView.setText(R.string.location_error)
            }
        }
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

    suspend fun checkPermissions() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
            .map { isGranted: Boolean ->
                if (isGranted) return@map true
                throw SecurityException("No location permission")
            }.awaitSingle()
    }

    private fun getTimeAgo(time: Long): CharSequence {
        return DateUtils.getRelativeTimeSpanString(time)
    }
}