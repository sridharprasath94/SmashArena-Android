package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.Slot
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.util.DateTimeUtils
import com.flash.smasharena.util.NetworkMonitor
import com.flash.smasharena.util.await
import com.flash.smasharena.util.requireUid
import com.flash.smasharena.util.toAppError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SlotRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor,
) : SlotRepository {

    private fun requireNetwork() {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
    }

    override fun observeSlots(facilityId: String, date: String): Flow<List<Slot>> = callbackFlow {
        val query = firestore.collection("slots")
            .whereEqualTo(SlotFields.FACILITY_ID, facilityId)
            .whereEqualTo(SlotFields.DATE, date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error.toAppError())
                return@addSnapshotListener
            }
            val bookedSlots = snapshot?.documents
                ?.mapNotNull { it.toSlot() }
                ?.filter { it.status != SlotStatus.CANCELLED }
                ?.associateBy { it.hour }
                ?: emptyMap()

            val allSlots = (5..21).map { hour ->
                bookedSlots[hour] ?: Slot(
                    facilityId = facilityId,
                    date = date,
                    hour = hour,
                    status = SlotStatus.AVAILABLE,
                )
            }
            trySend(allSlots)
        }
        awaitClose { listener.remove() }
    }

    override fun getUpcomingBookings(): Flow<List<Slot>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val today = DateTimeUtils.today()
        val listener = firestore.collection("slots")
            .whereEqualTo(SlotFields.BOOKED_BY, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error.toAppError()); return@addSnapshotListener }
                val slots = snapshot?.documents
                    ?.mapNotNull { it.toSlot() }
                    ?.filter { it.status == SlotStatus.BOOKED && it.date >= today }
                    ?.sortedWith(compareBy({ it.date }, { it.hour }))
                    ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPastBookings(): List<Slot> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyList()
        val today = DateTimeUtils.today()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val snapshot = firestore.collection("slots")
            .whereEqualTo(SlotFields.BOOKED_BY, uid)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { it.toSlot() }
            .filter { it.status == SlotStatus.BOOKED &&
                (it.date < today || (it.date == today && it.hour < currentHour)) }
            .sortedWith(compareByDescending<Slot> { it.date }.thenByDescending { it.hour })
    }

    override fun getCancelledBookings(): Flow<List<Slot>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = firestore.collection("slots")
            .whereEqualTo(SlotFields.BOOKED_BY, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error.toAppError()); return@addSnapshotListener }
                val slots = snapshot?.documents
                    ?.mapNotNull { it.toSlot() }
                    ?.filter { it.status == SlotStatus.CANCELLED }
                    ?.sortedByDescending { it.cancelledAtMs ?: 0L }
                    ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun cancelBooking(docId: String): Result<Unit> = runCatching {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("slots").document(docId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            if (snapshot.getString(SlotFields.BOOKED_BY) != uid) throw AppError.NotYourBooking
            tx.update(
                docRef, mapOf(
                    SlotFields.STATUS to SlotFields.STATUS_CANCELLED,
                    SlotFields.CANCELLED_AT to com.google.firebase.Timestamp.now(),
                )
            )
        }.await()
    }

    override suspend fun deleteCancelledBooking(docId: String): Result<Unit> = runCatching {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("slots").document(docId)
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            if (snapshot.getString(SlotFields.BOOKED_BY) != uid) throw AppError.NotYourBooking
            tx.delete(docRef)
        }.await()
    }

    override suspend fun bookSlot(
        facilityId: String,
        date: String,
        hour: Int,
        isFreeMembership: Boolean,
    ): Result<Unit> = runCatching {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("slots").document("${facilityId}_${date}_${hour}")
        firestore.runTransaction { tx ->
            val snapshot = tx.get(docRef)
            if (snapshot.exists() && snapshot.getString(SlotFields.STATUS) == SlotFields.STATUS_BOOKED) {
                throw AppError.SlotAlreadyBooked
            }
            tx.set(
                docRef, mapOf(
                    SlotFields.FACILITY_ID to facilityId,
                    SlotFields.DATE to date,
                    SlotFields.HOUR to hour,
                    SlotFields.STATUS to SlotFields.STATUS_BOOKED,
                    SlotFields.BOOKED_BY to uid,
                    SlotFields.BOOKED_AT to com.google.firebase.Timestamp.now(),
                    SlotFields.IS_FREE_MEMBERSHIP to isFreeMembership,
                )
            )
        }.await()
    }

    private fun DocumentSnapshot.toSlot(): Slot? {
        val facilityId = getString(SlotFields.FACILITY_ID) ?: return null
        val date = getString(SlotFields.DATE) ?: return null
        val hour = getLong(SlotFields.HOUR)?.toInt() ?: return null
        val status = when (getString(SlotFields.STATUS)) {
            SlotFields.STATUS_BOOKED -> SlotStatus.BOOKED
            "member_hold"            -> SlotStatus.MEMBER_HOLD
            SlotFields.STATUS_CANCELLED -> SlotStatus.CANCELLED
            else                     -> SlotStatus.AVAILABLE
        }
        val cancelledAtMs = getTimestamp(SlotFields.CANCELLED_AT)?.toDate()?.time
        return Slot(
            facilityId, date, hour, status,
            getString(SlotFields.BOOKED_BY),
            getBoolean(SlotFields.IS_FREE_MEMBERSHIP) == true,
            cancelledAtMs,
        )
    }
}
