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
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.wear.whereami.complication.forceComplicationUpdate
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.data.ResolvedLocation
import com.google.wear.whereami.format.getAddressDescription
import com.google.wear.whereami.format.getTimeAgo
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitSingle

class WhereAmIActivity : FragmentActivity() {
    private lateinit var locationViewModel: LocationViewModel

    private lateinit var textView: TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_am_i_activity)
        textView = findViewById(R.id.text)

        locationViewModel = LocationViewModel(applicationContext)

        lifecycleScope.launch {
            checkPermissions()

            val location = locationViewModel.readLocationResult()

            val text: String = if (location is ResolvedLocation) {
                getString(
                    R.string.address_as_of_time_activity,
                    getAddressDescription(location),
                    getTimeAgo(location.location.time)
                )
            } else {
                getString(R.string.location_error)
            }

            textView.text = text
            textView.contentDescription = text
        }
    }

    override fun onStop() {
        forceComplicationUpdate()
        super.onStop()
    }

    suspend fun checkPermissions() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
            .doOnNext { isGranted ->
                if (!isGranted) throw SecurityException("No location permission")
            }.awaitSingle()
    }
}