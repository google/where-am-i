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
package com.google.wear.whereami.complication

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.wear.complications.datasource.ComplicationDataSourceUpdateRequester

fun Context.forceComplicationUpdate() {
    if (applicationContext.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val request = ComplicationDataSourceUpdateRequester(
            applicationContext, ComponentName.createRelative(
                applicationContext, ".complication.WhereAmIComplicationProviderService"
            )
        )
        request.requestUpdateAll()
    }
}