package com.smasharena.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A confirmed booking. Times are stored as epoch millis to keep the schema
 * trivial — the UI converts to local times at the boundary.
 *
 * `bookedByPremium` is denormalised here so peak-hour priority checks can be
 * answered with a single table scan instead of joining users on every query.
 */
@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Court::class,
            parentColumns = ["id"],
            childColumns = ["courtId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("userId"), Index("courtId"), Index("startEpochMs")],
)
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val courtId: Long,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val bookedByPremium: Boolean,
) {
    val durationMinutes: Long
        get() = (endEpochMs - startEpochMs) / 60_000L
}
