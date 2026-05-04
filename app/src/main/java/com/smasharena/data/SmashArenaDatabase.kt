package com.smasharena.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [User::class, Court::class, Booking::class],
    version = 1,
    exportSchema = false,
)
abstract class SmashArenaDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courtDao(): CourtDao
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile private var INSTANCE: SmashArenaDatabase? = null

        fun get(context: Context, scope: CoroutineScope): SmashArenaDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    SmashArenaDatabase::class.java,
                    "smash_arena.db",
                ).build()
                INSTANCE = db
                scope.launch(Dispatchers.IO) { db.seedIfEmpty() }
                db
            }
        }
    }

    private suspend fun seedIfEmpty() {
        if (courtDao().count() == 0) {
            courtDao().insertAll(
                listOf(
                    Court(name = "Court 1 — Center", description = "Premium wooden flooring"),
                    Court(name = "Court 2 — North", description = "Standard synthetic mat"),
                    Court(name = "Court 3 — South", description = "Standard synthetic mat"),
                    Court(name = "Court 4 — Practice", description = "Half-court for warm-ups"),
                )
            )
        }
    }
}
