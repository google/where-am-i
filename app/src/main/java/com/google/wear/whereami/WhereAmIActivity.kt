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
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.coroutines.sendSuspend
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.google.wear.whereami.complication.WhereAmIComplicationProviderService.Companion.forceComplicationUpdate
import com.google.wear.whereami.data.LocationViewModel
import com.google.wear.whereami.data.ResolvedLocation
import kotlinx.coroutines.launch

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

            if (location is ResolvedLocation) {
                textView.text = getString(
                    R.string.address_as_of_time_activity,
                    getAddressDescription(location),
                    getTimeAgo(location.location.time)
                )
            } else {
                textView.setText(R.string.location_error)
            }
        }

        forceComplicationUpdate()
    }

    override fun onStop() {
        forceComplicationUpdate()
        super.onStop()
    }

    suspend fun checkPermissions() {
        val result = permissionsBuilder(Manifest.permission.ACCESS_FINE_LOCATION).build().sendSuspend()

        if (!result.allGranted()) throw SecurityException("No location permission")
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