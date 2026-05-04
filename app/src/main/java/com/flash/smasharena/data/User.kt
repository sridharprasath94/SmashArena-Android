package com.flash.smasharena.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A registered Smash Arena player. We store only what we need for booking
 * decisions; in a real app this would be enriched server-side.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /**
     * Premium members get priority during the configured peak window. See
     * [BookingRepository.PEAK_START_HOUR] / [BookingRepository.PEAK_END_HOUR].
     */
    val isPremium: Boolean,
)
