package com.flash.smasharena.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [User::class, Court::class, Booking::class],
    version = 1,
    exportSchema = false,
)
abstract class SmashArenaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courtDao(): CourtDao
    abstract fun bookingDao(): BookingDao
}
