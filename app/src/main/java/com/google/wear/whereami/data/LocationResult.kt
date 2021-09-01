package com.google.wear.whereami.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Entity
data class LocationResult(
    @PrimaryKey val id: Int = Current,
    @ColumnInfo(name = "latitude") val latitude: Double? = null,
    @ColumnInfo(name = "longitude") val longitude: Double? = null,
    @ColumnInfo(name = "locationName") val locationName: String? = null,
    @ColumnInfo(name = "error") val error: ErrorType? = null,
    @ColumnInfo(name = "errorMessage") val errorMessage: String? = null,
    @ColumnInfo(name = "time") val time: Instant = Instant.now(),
) {
    val formattedTime: String
        get() {
            return timeFormatter.format(LocalDateTime.ofInstant(time, ZoneOffset.systemDefault()))
        }

    enum class ErrorType {
        NoPermission, Failed, Timeout, Unknown
    }

    companion object {
        val PermissionError: LocationResult = LocationResult(error = ErrorType.NoPermission)
        val Unknown: LocationResult = LocationResult(error = ErrorType.Unknown)

        const val Current = 0

        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
}