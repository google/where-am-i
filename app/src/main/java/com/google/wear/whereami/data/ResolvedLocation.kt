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
package com.google.wear.whereami.data

import android.location.Address
import android.location.Location

sealed class LocationResult {
    companion object
}

object Unknown: LocationResult()

object PermissionError: LocationResult()

object NoLocation: LocationResult()

data class LocationError(val e: Exception): LocationResult()

data class ResolvedLocation(val location: Location, val address: Address): LocationResult()
