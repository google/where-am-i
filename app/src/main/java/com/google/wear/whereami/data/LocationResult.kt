package com.google.wear.whereami.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration
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
    fun newer(lastValue: LocationResult): Boolean {
        val thatFreshness = lastValue.freshness
        val thisFreshness = freshness
        return thisFreshness < thatFreshness || (thisFreshness == thatFreshness && time > lastValue.time)
    }

    val description: String
        get() {
            return locationName ?: errorMessage ?: error.toString()
        }

    val formattedTime: String
        get() {
            return timeFormatter.format(LocalDateTime.ofInstant(time, ZoneOffset.systemDefault()))
        }

    enum class ErrorType {
        NoPermission, Failed, Timeout, Unknown
    }

    val freshness: Freshness
        get() {
            val age = Duration.between(time, Instant.now())

            return when {
                error == ErrorType.Unknown -> Freshness.UNKNOWN
                age < THREE_MINUTES && locationName != null -> Freshness.FRESH_EXACT
                age < TWENTY_MINUTES && locationName != null -> Freshness.STALE_EXACT
                age < THREE_MINUTES && error != null -> Freshness.FRESH_ERROR
                age < TWENTY_MINUTES && error != null -> Freshness.STALE_ERROR
                age >= THREE_MINUTES && locationName != null -> Freshness.OLD_EXACT
                age >= THREE_MINUTES && error != null -> Freshness.OLD_ERROR
                else -> Freshness.UNKNOWN
            }
        }

    enum class Freshness {
        FRESH_EXACT, STALE_EXACT, FRESH_ERROR, STALE_ERROR, UNKNOWN, OLD_EXACT, OLD_ERROR
    }

    companion object {
        val PermissionError: LocationResult = LocationResult(error = ErrorType.NoPermission)
        val Unknown: LocationResult = LocationResult(error = ErrorType.Unknown)

        const val Current = 0

        val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

        val THREE_MINUTES: Duration = Duration.ofMinutes(3L)
        val TWENTY_MINUTES: Duration = Duration.ofMinutes(20L)
    }
}