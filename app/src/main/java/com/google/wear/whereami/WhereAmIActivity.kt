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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.data.LocationViewModel
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitSingle
import kotlinx.coroutines.withContext

class WhereAmIActivity : FragmentActivity() {
    private lateinit var locationViewModel: LocationViewModel

    private lateinit var locationTextView: TextView
    private lateinit var timeTextView: TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.where_am_i_activity)
        locationTextView = findViewById(R.id.location)
        timeTextView = findViewById(R.id.time)

        locationViewModel = (this.applicationContext as WhereAmIApplication).locationViewModel

        lifecycleScope.launchWhenCreated {
            checkPermissions()

            val flow = locationViewModel.locationStream()

            flow.collect {
                withContext(Dispatchers.Main) {
                    updateAndDisplayLocation(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            locationViewModel.readFreshLocationResult()
        }
    }

    private fun updateAndDisplayLocation(location: LocationResult) {
        if (location.locationName != null) {
            locationTextView.text = location.locationName
            timeTextView.text = location.formattedTime
        } else {
            val error = location.errorMessage
            timeTextView.text = ""
            if (error != null) {
                locationTextView.text = error
            } else {
                locationTextView.setText(R.string.location_error)
            }
        }
    }

    suspend fun checkPermissions() {
        RxPermissions(this)
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
            .doOnNext { isGranted ->
                if (!isGranted) throw SecurityException("No location permission")
            }.awaitSingle()
    }

    companion object {
        fun Context.tapAction(): PendingIntent? {
            val intent = Intent(this, WhereAmIActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}