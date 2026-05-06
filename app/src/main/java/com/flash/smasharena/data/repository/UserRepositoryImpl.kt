package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.UserProfile
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor,
) : UserRepository {

    override suspend fun getOrCreateProfile(): UserProfile {
        val firebaseUser = firebaseAuth.currentUser ?: error("Not signed in")
        val docRef = firestore.collection("users").document(firebaseUser.uid)

        return suspendCancellableCoroutine { cont ->
            docRef.get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        cont.resume(snapshot.toUserProfile())
                    } else {
                        val defaults = mapOf(
                            "displayName" to (firebaseUser.displayName ?: ""),
                            "email" to (firebaseUser.email ?: ""),
                            "membershipTier" to MembershipTier.NONE.name,
                            "membershipExpiry" to null,
                            "sessionsUsed" to 0,
                            "sessionsQuota" to 0,
                            "cricketSessionsUsed" to 0,
                        )
                        docRef.set(defaults)
                            .addOnSuccessListener {
                                cont.resume(
                                    UserProfile(
                                        uid = firebaseUser.uid,
                                        displayName = firebaseUser.displayName ?: "",
                                        email = firebaseUser.email ?: "",
                                        membershipTier = MembershipTier.NONE,
                                        membershipExpiry = null,
                                        sessionsUsed = 0,
                                        sessionsQuota = 0,
                                    )
                                )
                            }
                            .addOnFailureListener { cont.resumeWithException(it) }
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun purchaseMembership(tier: MembershipTier) {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
        val uid = firebaseAuth.currentUser?.uid ?: throw AppError.NotSignedIn
        val docRef = firestore.collection("users").document(uid)
        val expiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        suspendCancellableCoroutine { cont ->
            docRef.update(
                mapOf(
                    "membershipTier" to tier.name,
                    "sessionsQuota" to tier.sessionsPerMonth,
                    "sessionsUsed" to 0,
                    "cricketSessionsUsed" to 0,
                    "membershipExpiry" to expiry,
                )
            )
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun upgradeMembership(newTier: MembershipTier) {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
        val uid = firebaseAuth.currentUser?.uid ?: throw AppError.NotSignedIn
        val docRef = firestore.collection("users").document(uid)
        val expiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        suspendCancellableCoroutine { cont ->
            docRef.update(
                mapOf(
                    "membershipTier" to newTier.name,
                    "sessionsQuota" to newTier.sessionsPerMonth,
                    "membershipExpiry" to expiry,
                )
            )
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun cancelMembership() {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
        val uid = firebaseAuth.currentUser?.uid ?: throw AppError.NotSignedIn
        val docRef = firestore.collection("users").document(uid)
        suspendCancellableCoroutine { cont ->
            docRef.update(
                mapOf(
                    "membershipTier" to MembershipTier.NONE.name,
                    "sessionsQuota" to 0,
                    "sessionsUsed" to 0,
                    "cricketSessionsUsed" to 0,
                    "membershipExpiry" to null,
                )
            )
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun incrementSessionsUsed(isCricket: Boolean) {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
        val uid = firebaseAuth.currentUser?.uid ?: throw AppError.NotSignedIn
        val field = if (isCricket) "cricketSessionsUsed" else "sessionsUsed"
        val docRef = firestore.collection("users").document(uid)
        suspendCancellableCoroutine { cont ->
            docRef.update(field, FieldValue.increment(1))
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    override suspend fun decrementSessionsUsed(isCricket: Boolean) {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
        val uid = firebaseAuth.currentUser?.uid ?: throw AppError.NotSignedIn
        val field = if (isCricket) "cricketSessionsUsed" else "sessionsUsed"
        val docRef = firestore.collection("users").document(uid)
        suspendCancellableCoroutine { cont ->
            firestore.runTransaction { tx ->
                val current = tx.get(docRef).getLong(field)?.toInt() ?: 0
                tx.update(docRef, field, (current - 1).coerceAtLeast(0))
            }
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile {
        val tier = runCatching {
            MembershipTier.valueOf(getString("membershipTier") ?: "NONE")
        }.getOrDefault(MembershipTier.NONE)

        return UserProfile(
            uid = id,
            displayName = getString("displayName") ?: "",
            email = getString("email") ?: "",
            membershipTier = tier,
            membershipExpiry = getLong("membershipExpiry"),
            sessionsUsed = getLong("sessionsUsed")?.toInt() ?: 0,
            sessionsQuota = getLong("sessionsQuota")?.toInt() ?: 0,
            cricketSessionsUsed = getLong("cricketSessionsUsed")?.toInt() ?: 0,
        )
    }
}
