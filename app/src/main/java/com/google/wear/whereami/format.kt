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
package com.google.wear.whereami

import android.content.Context
import android.text.TextUtils
import android.text.format.DateUtils
import com.google.wear.whereami.data.LocationResult
import com.google.wear.whereami.data.ResolvedLocation

fun Context.getTimeAgo(time: Long): CharSequence {
    return DateUtils.getRelativeTimeSpanString(time)
}

fun Context.getAddressDescription(location: ResolvedLocation): String {
    val address = location.address
    val subThoroughfare = address.subThoroughfare
    val thoroughfare = address.thoroughfare ?: return address.countryName
    return (if (TextUtils.isEmpty(subThoroughfare)) "" else "$subThoroughfare ") + thoroughfare
}

fun Context.describeLocation(location: LocationResult): String {
    return when (location) {
        is ResolvedLocation -> getAddressDescription(location)
        else -> "Unknown"
    }
}
