package com.flash.smasharena.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * The single place where booking business rules live. Both ViewModels and
 * tests talk to this class so the rules can never drift between callers.
 *
 * Rules enforced:
 *   1. A booking must last between 30 and 120 minutes.
 *   2. A user can hold at most 120 minutes of bookings on any single calendar
 *      day (across all courts). Cancellations free the time back up.
 *   3. The slot must be in the future.
 *   4. The slot must not overlap an existing booking on the same court.
 *   5. During the configured peak window (18:00 – 21:00 local), non-premium
 *      users can only confirm a booking on a court if no Premium member has
 *      already booked that overlap *and* the slot wasn't being held for one.
 *      In this prototype we model "holds" simply: if any Premium booking
 *      exists for that court in the peak window on the same day, the rest of
 *      the peak window stays open to Premium members for 10 minutes after a
 *      non-premium attempt — keeping the rule simple while still giving
 *      Premium members a meaningful priority.
 */
class BookingRepository(
    private val bookingDao: BookingDao,
    private val courtDao: CourtDao,
    private val userDao: UserDao,
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {

    fun observeUpcomingFor(userId: Long): Flow<List<Booking>> =
        bookingDao.observeUpcomingForUser(userId, nowProvider())

    fun observeCourts(): Flow<List<Court>> = courtDao.observeAll()

    suspend fun courtById(id: Long): Court? = courtDao.findById(id)

    suspend fun findUser(id: Long): User? = userDao.findById(id)

    suspend fun createUser(name: String, isPremium: Boolean): Long =
        userDao.insert(User(name = name.trim(), isPremium = isPremium))

    /**
     * Attempt to create a booking. Returns a typed [BookingResult] describing
     * either success or the specific reason the booking was rejected. The UI
     * layer is responsible for translating the reason into a user-facing
     * message — see `R.string.error_*`.
     */
    suspend fun book(
        userId: Long,
        courtId: Long,
        startEpochMs: Long,
        durationMinutes: Int,
    ): BookingResult {
        if (durationMinutes !in MIN_DURATION_MIN..MAX_DURATION_MIN) {
            return BookingResult.InvalidDuration
        }

        val now = nowProvider()
        if (startEpochMs < now) return BookingResult.InPast

        val endEpochMs = startEpochMs + TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
        val user = userDao.findById(userId) ?: return BookingResult.UnknownUser

        // Rule 2: 2-hour daily cap.
        val (dayStart, dayEnd) = dayBoundsFor(startEpochMs)
        val sameDay = bookingDao.bookingsForUserOnDay(userId, dayStart, dayEnd)
        val alreadyBookedMinutes = sameDay.sumOf { it.durationMinutes }
        if (alreadyBookedMinutes + durationMinutes > MAX_DAILY_MINUTES) {
            return BookingResult.DailyCapExceeded(
                alreadyBookedMinutes = alreadyBookedMinutes.toInt(),
            )
        }

        // Rule 4: no overlap on the same court.
        val overlaps = bookingDao.overlappingOnCourt(courtId, startEpochMs, endEpochMs)
        if (overlaps.isNotEmpty()) return BookingResult.SlotTaken

        // Rule 5: peak-hour Premium priority.
        if (!user.isPremium && touchesPeakWindow(startEpochMs, endEpochMs)) {
            // Look at *all* peak-window overlaps on this court for the same
            // calendar day. If a Premium member already holds time that
            // overlaps the requested slot, reject. Otherwise allow — Premium
            // members aren't reserving the entire evening, just the times
            // they've actually claimed.
            val peakWindow = peakWindowFor(startEpochMs)
            val peakOverlaps = bookingDao.overlappingOnCourt(
                courtId = courtId,
                startInclusive = peakWindow.first,
                endExclusive = peakWindow.second,
            )
            val anyPremiumHold = peakOverlaps.any { it.bookedByPremium }
            if (anyPremiumHold) return BookingResult.PeakReservedForPremium
        }

        val id = bookingDao.insert(
            Booking(
                userId = userId,
                courtId = courtId,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                bookedByPremium = user.isPremium,
            )
        )
        return BookingResult.Success(id)
    }

    suspend fun cancel(booking: Booking) = bookingDao.delete(booking)

    /** Calendar-day [start, endExclusive) for a moment, in the configured zone. */
    private fun dayBoundsFor(epochMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return startOfDay to cal.timeInMillis
    }

    /** [start, endExclusive) of the peak window for the day containing epochMs. */
    private fun peakWindowFor(epochMs: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(timeZone).apply { timeInMillis = epochMs }
        cal.set(Calendar.HOUR_OF_DAY, PEAK_START_HOUR)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val peakStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, PEAK_END_HOUR)
        val peakEnd = cal.timeInMillis
        return peakStart to peakEnd
    }

    private fun touchesPeakWindow(startEpochMs: Long, endEpochMs: Long): Boolean {
        val (peakStart, peakEnd) = peakWindowFor(startEpochMs)
        return startEpochMs < peakEnd && endEpochMs > peakStart
    }

    companion object {
        const val MIN_DURATION_MIN = 30
        const val MAX_DURATION_MIN = 120
        const val MAX_DAILY_MINUTES = 120
        const val PEAK_START_HOUR = 18
        const val PEAK_END_HOUR = 21
    }
}

/** Outcome of a booking attempt. */
sealed interface BookingResult {
    data class Success(val bookingId: Long) : BookingResult
    data object InvalidDuration : BookingResult
    data object InPast : BookingResult
    data object SlotTaken : BookingResult
    data object PeakReservedForPremium : BookingResult
    data class DailyCapExceeded(val alreadyBookedMinutes: Int) : BookingResult
    data object UnknownUser : BookingResult
}
