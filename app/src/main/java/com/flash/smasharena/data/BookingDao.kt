package com.flash.smasharena.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Insert
    suspend fun insert(booking: Booking): Long

    @Delete
    suspend fun delete(booking: Booking)

    /**
     * Returns all bookings for a user that fall on the same calendar day as
     * the supplied window. The repository uses this to enforce the 2-hour
     * daily cap.
     */
    @Query(
        """
        SELECT * FROM bookings
        WHERE userId = :userId
          AND startEpochMs < :dayEndExclusive
          AND endEpochMs   > :dayStartInclusive
        """
    )
    suspend fun bookingsForUserOnDay(
        userId: Long,
        dayStartInclusive: Long,
        dayEndExclusive: Long,
    ): List<Booking>

    /**
     * Anything that overlaps a [start, end) window on a given court. Used to
     * detect double-bookings and to answer "is the peak slot already held by a
     * Premium member?" questions.
     */
    @Query(
        """
        SELECT * FROM bookings
        WHERE courtId = :courtId
          AND startEpochMs < :endExclusive
          AND endEpochMs   > :startInclusive
        """
    )
    suspend fun overlappingOnCourt(
        courtId: Long,
        startInclusive: Long,
        endExclusive: Long,
    ): List<Booking>

    @Query(
        """
        SELECT * FROM bookings
        WHERE userId = :userId AND endEpochMs > :nowEpochMs
        ORDER BY startEpochMs ASC
        """
    )
    fun observeUpcomingForUser(userId: Long, nowEpochMs: Long): Flow<List<Booking>>
}
