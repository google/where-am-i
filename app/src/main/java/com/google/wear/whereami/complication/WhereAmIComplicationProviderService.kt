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

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.complications.data.*
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.complications.datasource.ComplicationRequest
import com.google.wear.whereami.R
import com.google.wear.whereami.WhereAmIActivity.Companion.tapAction
import com.google.wear.whereami.applicationScope
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.kt.CoroutinesComplicationDataSourceService
import com.google.wear.whereami.locationViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

class WhereAmIComplicationProviderService : CoroutinesComplicationDataSourceService() {
    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        Log.i("WhereAmI", "onComplicationActivated $complicationInstanceId")
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.i("WhereAmI", "onComplicationDeactivated $complicationInstanceId")
    }

    override suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest): ComplicationData {
        Log.i("WhereAmI", "onComplicationUpdate $complicationRequest")

        val location = locationViewModel.databaseLocationStream().first()

        // Force a refresh if we have stale (> 20 minutes) results or errors
        if (location.freshness > LocationResult.Freshness.STALE_EXACT) {
            applicationScope.launch {
                // force a refresh
                locationViewModel.readFreshLocationResult(freshLocation = null)
            }
        }

        return toComplicationData(complicationRequest.complicationType, location)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        val locationResult = LocationResult(latitude = 0.0, longitude = 0.0, locationName = "Null Island")
        return toComplicationData(type, locationResult)
    }

    fun toComplicationData(
        type: ComplicationType,
        locationResult: LocationResult
    ): ComplicationData {
        return when (type) {
            ComplicationType.SMALL_IMAGE -> SmallImageComplicationData.Builder(
                SmallImage.Builder(
                    Icon.createWithResource(
                        this,
                        R.drawable.ic_my_location
                    ), SmallImageType.ICON
                ).build(),
                getAddressDescriptionText(locationResult)
            )
                .setTapAction(tapAction())
                .build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                getTimeAgoComplicationText(locationResult.time).build(),
                getAddressDescriptionText(locationResult)
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_my_location
                        )
                    ).build()
                )
                .setTapAction(tapAction())
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                getAddressDescriptionText(locationResult),
                getAddressDescriptionText(locationResult)
            )
                .setTitle(getTimeAgoComplicationText(locationResult.time).build())
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            this,
                            R.drawable.ic_my_location
                        )
                    ).build()
                )
                .setTapAction(tapAction())
                .build()
            else -> throw IllegalArgumentException("Unexpected complication type $type")
        }
    }

    private fun getTimeAgoComplicationText(fromTime: Instant): TimeDifferenceComplicationText.Builder {
        return TimeDifferenceComplicationText.Builder(
            TimeDifferenceStyle.SHORT_SINGLE_UNIT,
            CountUpTimeReference(fromTime)
        ).apply {
            setMinimumTimeUnit(TimeUnit.MINUTES)
            setDisplayAsNow(true)
        }
    }

    private fun getAddressDescriptionText(location: LocationResult): ComplicationText {
        return if (location.locationName != null) {
            return PlainComplicationText.Builder(location.locationName).build()
        } else {
            PlainComplicationText.Builder(getString(R.string.no_location)).build()
        }
    }

    companion object {
        fun Context.forceComplicationUpdate() {
            val request = ComplicationDataSourceUpdateRequester.create(
                applicationContext, ComponentName(
                    applicationContext, WhereAmIComplicationProviderService::class.java
                )
            )
            request.requestUpdateAll()
        }
    }
}