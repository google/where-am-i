package com.google.wear.whereami

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
