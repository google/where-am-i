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
package com.google.wear.whereami.kt

import android.util.Log
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.datasource.ComplicationDataSourceService
import androidx.wear.complications.datasource.ComplicationRequest
import com.google.wear.whereami.applicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

abstract class CoroutinesComplicationDataSourceService : ComplicationDataSourceService() {
    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        Log.i("WhereAmI", "onComplicationActivated $complicationInstanceId")
    }

    override fun onComplicationDeactivated(complicationInstanceId: Int) {
        Log.i("WhereAmI", "onComplicationDeactivated $complicationInstanceId")
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        applicationScope.launch {
            listener.onComplicationData(onComplicationUpdate(request))
        }
    }

    abstract suspend fun onComplicationUpdate(complicationRequest: ComplicationRequest): ComplicationData?
}