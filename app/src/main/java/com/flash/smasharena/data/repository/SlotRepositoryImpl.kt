package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.Slot
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.util.NetworkMonitor
import com.flash.smasharena.util.await
import com.flash.smasharena.util.requireUid
import com.flash.smasharena.util.toAppError
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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

    override fun getMyBookings(): Flow<List<Slot>> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid ?: run {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val today = com.flash.smasharena.util.DateTimeUtils.today()
        val listener = firestore.collection("slots")
            .whereEqualTo(SlotFields.BOOKED_BY, uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error.toAppError()); return@addSnapshotListener }
                val slots = snapshot?.documents
                    ?.mapNotNull { it.toSlot() }
                    ?.filter { it.date >= today }
                    ?.sortedWith(compareBy({ it.date }, { it.hour }))
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
            if (snapshot.exists() && snapshot.getString(SlotFields.STATUS) == "booked") {
                throw AppError.SlotAlreadyBooked
            }
            tx.set(
                docRef, mapOf(
                    SlotFields.FACILITY_ID to facilityId,
                    SlotFields.DATE to date,
                    SlotFields.HOUR to hour,
                    SlotFields.STATUS to "booked",
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
            "booked"      -> SlotStatus.BOOKED
            "member_hold" -> SlotStatus.MEMBER_HOLD
            else          -> SlotStatus.AVAILABLE
        }
        return Slot(
            facilityId, date, hour, status,
            getString(SlotFields.BOOKED_BY),
            getBoolean(SlotFields.IS_FREE_MEMBERSHIP) == true,
        )
    }
}
