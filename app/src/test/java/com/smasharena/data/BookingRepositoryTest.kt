package com.smasharena.data

import com.flash.smasharena.data.Booking
import com.flash.smasharena.data.BookingDao
import com.flash.smasharena.data.BookingRepository
import com.flash.smasharena.data.BookingResult
import com.flash.smasharena.data.Court
import com.flash.smasharena.data.CourtDao
import com.flash.smasharena.data.User
import com.flash.smasharena.data.UserDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * In-memory fakes so the rule logic in [com.flash.smasharena.data.BookingRepository] can be tested
 * without Room. Each test pins both the clock and the time-zone so the
 * peak-window logic is deterministic regardless of the host machine.
 */
class BookingRepositoryTest {

    private val zone = TimeZone.getTimeZone("UTC")
    private val nineAm: Long = Calendar.getInstance(zone).apply {
        set(2026, Calendar.APRIL, 25, 9, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    private val sixPm: Long = Calendar.getInstance(zone).apply {
        set(2026, Calendar.APRIL, 25, 18, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun newRepo(now: Long = nineAm - TimeUnit.HOURS.toMillis(1)): Pair<BookingRepository, FakeBookingDao> {
        val bookings = FakeBookingDao()
        val courts = FakeCourtDao(listOf(Court(id = 1, name = "Court 1", description = "")))
        val users = FakeUserDao()
        return BookingRepository(
            bookingDao = bookings,
            courtDao = courts,
            userDao = users,
            timeZone = zone,
            nowProvider = { now },
        ).also {
            // Seed two users.
            runBlocking {
                users.insert(User(id = 1, name = "Reg", isPremium = false))
                users.insert(User(id = 2, name = "Pro", isPremium = true))
            }
        } to bookings
    }

    @Test fun `rejects booking shorter than 30 minutes`() = runBlocking {
        val (repo, _) = newRepo()
        val r = repo.book(userId = 1, courtId = 1, startEpochMs = nineAm, durationMinutes = 15)
        assertEquals(BookingResult.InvalidDuration, r)
    }

    @Test fun `rejects booking longer than 120 minutes`() = runBlocking {
        val (repo, _) = newRepo()
        val r = repo.book(userId = 1, courtId = 1, startEpochMs = nineAm, durationMinutes = 150)
        assertEquals(BookingResult.InvalidDuration, r)
    }

    @Test fun `rejects bookings in the past`() = runBlocking {
        val (repo, _) = newRepo(now = nineAm + TimeUnit.HOURS.toMillis(2))
        val r = repo.book(userId = 1, courtId = 1, startEpochMs = nineAm, durationMinutes = 60)
        assertEquals(BookingResult.InPast, r)
    }

    @Test fun `enforces 2-hour daily cap across courts`() = runBlocking {
        val (repo, _) = newRepo()
        // First 90 minutes succeeds.
        val first = repo.book(userId = 1, courtId = 1, startEpochMs = nineAm, durationMinutes = 90)
        assertTrue(first is BookingResult.Success)
        // Adding another 60 minutes the same day should exceed the 120-minute cap.
        val second = repo.book(
            userId = 1,
            courtId = 1,
            startEpochMs = nineAm + TimeUnit.HOURS.toMillis(2),
            durationMinutes = 60,
        )
        assertTrue(second is BookingResult.DailyCapExceeded)
    }

    @Test fun `non-premium cannot take a peak slot already held by premium`() = runBlocking {
        val (repo, _) = newRepo()
        // Premium user (id=2) books 6:00–7:00 PM on court 1.
        val premiumBook = repo.book(userId = 2, courtId = 1, startEpochMs = sixPm, durationMinutes = 60)
        assertTrue(premiumBook is BookingResult.Success)

        // Regular user tries to grab 7:00–8:00 PM on the same court — even
        // though the time itself is free, the peak window is held for
        // Premium because Premium already has activity inside it.
        val regularAttempt = repo.book(
            userId = 1,
            courtId = 1,
            startEpochMs = sixPm + TimeUnit.HOURS.toMillis(1),
            durationMinutes = 60,
        )
        assertEquals(BookingResult.PeakReservedForPremium, regularAttempt)
    }

    @Test fun `non-premium can book peak slot when no premium has booked the court`() = runBlocking {
        val (repo, _) = newRepo()
        val r = repo.book(userId = 1, courtId = 1, startEpochMs = sixPm, durationMinutes = 60)
        assertTrue(r is BookingResult.Success)
    }

    @Test fun `rejects overlapping booking on same court`() = runBlocking {
        val (repo, _) = newRepo()
        val first = repo.book(userId = 1, courtId = 1, startEpochMs = nineAm, durationMinutes = 60)
        assertTrue(first is BookingResult.Success)
        val overlap = repo.book(
            userId = 2,
            courtId = 1,
            startEpochMs = nineAm + TimeUnit.MINUTES.toMillis(30),
            durationMinutes = 60,
        )
        assertEquals(BookingResult.SlotTaken, overlap)
    }
}

/* ---------- minimal in-memory fakes for the DAOs ---------- */

private class FakeBookingDao : BookingDao {
    private val rows = mutableListOf<Booking>()
    private var seq = 0L
    override suspend fun insert(booking: Booking): Long {
        seq += 1
        rows += booking.copy(id = seq)
        return seq
    }
    override suspend fun delete(booking: Booking) { rows.removeAll { it.id == booking.id } }
    override suspend fun bookingsForUserOnDay(
        userId: Long, dayStartInclusive: Long, dayEndExclusive: Long,
    ): List<Booking> = rows.filter {
        it.userId == userId &&
            it.startEpochMs < dayEndExclusive &&
            it.endEpochMs > dayStartInclusive
    }
    override suspend fun overlappingOnCourt(
        courtId: Long, startInclusive: Long, endExclusive: Long,
    ): List<Booking> = rows.filter {
        it.courtId == courtId &&
            it.startEpochMs < endExclusive &&
            it.endEpochMs > startInclusive
    }
    override fun observeUpcomingForUser(userId: Long, nowEpochMs: Long): Flow<List<Booking>> =
        flowOf(rows.filter { it.userId == userId && it.endEpochMs > nowEpochMs })
}

private class FakeCourtDao(seed: List<Court>) : CourtDao {
    private val rows = seed.toMutableList()
    override suspend fun insertAll(courts: List<Court>) { rows.addAll(courts) }
    override suspend fun count(): Int = rows.size
    override fun observeAll(): Flow<List<Court>> = flowOf(rows.toList())
    override suspend fun findById(id: Long): Court? = rows.firstOrNull { it.id == id }
}

private class FakeUserDao : UserDao {
    private val rows = mutableListOf<User>()
    override suspend fun insert(user: User): Long {
        rows += user; return user.id
    }
    override suspend fun findById(id: Long): User? = rows.firstOrNull { it.id == id }
    override fun observeById(id: Long): Flow<User?> = flowOf(findByIdSync(id))
    private fun findByIdSync(id: Long) = rows.firstOrNull { it.id == id }
}
