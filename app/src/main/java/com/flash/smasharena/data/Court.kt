package com.flash.smasharena.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A bookable badminton court. Courts are seeded on first launch — see
 * [SmashArenaDatabase.seedIfEmpty].
 */
@Entity(tableName = "courts")
data class Court(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
)
