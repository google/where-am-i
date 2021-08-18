// Copyright 2021 Google LLC
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
package com.google.wear.whereami.format

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.text.TextUtils
import android.text.format.DateUtils
import androidx.wear.complications.data.*
import com.google.wear.whereami.R
import com.google.wear.whereami.WhereAmIActivity
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.data.ResolvedLocation
import java.util.concurrent.TimeUnit

fun Context.getTimeAgo(time: Long): CharSequence {
    return DateUtils.getRelativeTimeSpanString(time)
}

fun Context.getAddressDescriptionText(location: LocationResult): ComplicationText {
    return if (location is ResolvedLocation)
        return PlainComplicationText.Builder(getAddressDescription(location)).build()
    else
        PlainComplicationText.Builder(getString(R.string.no_location)).build()
}

fun Context.getAddressDescription(location: ResolvedLocation): String {
    val address = location.address
    val subThoroughfare = address.subThoroughfare
    val thoroughfare = address.thoroughfare ?: return address.countryName
    return (if (TextUtils.isEmpty(subThoroughfare)) "" else "$subThoroughfare ") + thoroughfare
}

fun Context.getFullDescription(location: LocationResult): ComplicationText {
    return if (location is ResolvedLocation)
        getTimeAgoComplicationText(location.location.time).build()
    else
        PlainComplicationText.Builder(getString(R.string.no_location)).build()
}

fun Context.getTimeAgoComplicationText(location: LocationResult): ComplicationText {
    return if (location is ResolvedLocation)
        getTimeAgoComplicationText(location.location.time).build()
    else
        PlainComplicationText.Builder("--").build()
}

fun Context.describeLocation(location: LocationResult): String {
    return when (location) {
        is ResolvedLocation -> getAddressDescription(location)
        else -> "Unknown"
    }
}

fun Context.getTimeAgoComplicationText(fromTime: Long): TimeDifferenceComplicationText.Builder {
    return TimeDifferenceComplicationText.Builder(
        TimeDifferenceStyle.SHORT_SINGLE_UNIT,
        CountUpTimeReference(fromTime)
    ).apply {
        setMinimumTimeUnit(TimeUnit.MINUTES)
        setDisplayAsNow(true)
    }
}

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

fun Context.toComplicationData(
    type: ComplicationType,
    locationResult: LocationResult
) = when (type) {
    ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
        getTimeAgoComplicationText(locationResult),
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
        .setTapAction(tapAction())
        .build()
    ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
        getAddressDescriptionText(locationResult),
        getFullDescription(locationResult)
    )
        .setTitle(getTimeAgoComplicationText(locationResult))
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