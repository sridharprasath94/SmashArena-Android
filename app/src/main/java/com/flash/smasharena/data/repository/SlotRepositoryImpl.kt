package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.Slot
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.domain.repository.SlotRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SlotRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : SlotRepository {

    override fun observeSlots(facilityId: String, date: String): Flow<List<Slot>> = callbackFlow {
        val query = firestore.collection("slots")
            .whereEqualTo("facilityId", facilityId)
            .whereEqualTo("date", date)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val bookedSlots = snapshot?.documents
                ?.mapNotNull { it.toSlot() }
                ?.associateBy { it.hour }
                ?: emptyMap()

            // Always emit all 17 slots; overlay Firestore data on top
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
            .whereEqualTo("bookedBy", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val slots = snapshot?.documents
                    ?.mapNotNull { it.toSlot() }
                    ?.filter { it.date >= today }
                    ?.sortedWith(compareBy({ it.date }, { it.hour }))
                    ?: emptyList()
                trySend(slots)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun bookSlot(facilityId: String, date: String, hour: Int): Result<Unit> =
        runCatching {
            val uid = firebaseAuth.currentUser?.uid ?: error("Not signed in")
            val docRef = firestore.collection("slots")
                .document("${facilityId}_${date}_${hour}")

            suspendCancellableCoroutine { cont ->
                firestore.runTransaction { tx ->
                    val snapshot = tx.get(docRef)
                    if (snapshot.exists() && snapshot.getString("status") == "booked") {
                        throw Exception("This slot was just booked by someone else.")
                    }
                    tx.set(
                        docRef, mapOf(
                            "facilityId" to facilityId,
                            "date" to date,
                            "hour" to hour,
                            "status" to "booked",
                            "bookedBy" to uid,
                            "bookedAt" to com.google.firebase.Timestamp.now(),
                        )
                    )
                }
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }

    private fun DocumentSnapshot.toSlot(): Slot? {
        val facilityId = getString("facilityId") ?: return null
        val date = getString("date") ?: return null
        val hour = getLong("hour")?.toInt() ?: return null
        val status = when (getString("status")) {
            "booked"      -> SlotStatus.BOOKED
            "member_hold" -> SlotStatus.MEMBER_HOLD
            else          -> SlotStatus.AVAILABLE
        }
        return Slot(facilityId, date, hour, status, getString("bookedBy"))
    }
}
