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
import android.content.res.Configuration
import android.os.Bundle
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.data.LocationViewModel
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.awaitSingle

class WhereAmIActivity : AppCompatActivity() {
    private lateinit var locationViewModel: LocationViewModel

    private lateinit var locationTextView: TextView
    private lateinit var timeTextView: TextView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationViewModel = (this.applicationContext as WhereAmIApplication).locationViewModel

        lifecycleScope.launchWhenCreated {
            checkPermissions()

            val flow = locationViewModel.locationStream()

            setContent {
                LocationComponent(flow)
            }
        }
    }

    @Composable
    fun LocationComponent(flow: Flow<LocationResult>) {
        val flowState = flow.collectAsState(LocationResult.Unknown)

        LocationComponent(flowState.value)
    }

    @Composable
    fun LocationComponent(location: LocationResult) {
        MaterialTheme {
            Column(modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = location.description)
                Text(text = location.formattedTime)

                val coroutineScope = rememberCoroutineScope()

                val refreshOnClick: () -> Unit = {
                    coroutineScope.launch {
                        locationViewModel.readFreshLocationResult()
                    }
                }

                Button(onClick = { println("Click") }) {
                    Text(text = "Refresh")
                }
            }
        }
    }

    @Preview(uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_WATCH,
        device = "id:wear_round", widthDp = 240, heightDp = 240
    )
    @Composable
    fun ComposablePreview() {
        LocationComponent(LocationResult(locationName = "1600 Amphitheatre Parkway"))
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