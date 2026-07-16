package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.UserProfile
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.NetworkMonitor
import com.flash.smasharena.util.await
import com.flash.smasharena.util.requireUid
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val networkMonitor: NetworkMonitor,
) : UserRepository {

    @Volatile private var cachedProfile: UserProfile? = null

    private fun requireNetwork() {
        if (!networkMonitor.isConnected()) throw AppError.NoInternet
    }

    override suspend fun getOrCreateProfile(): UserProfile {
        cachedProfile?.let { return it }
        val firebaseUser = firebaseAuth.currentUser ?: throw AppError.NotSignedIn
        val docRef = firestore.collection("users").document(firebaseUser.uid)

        val snapshot = docRef.get().await()
        val profile = if (snapshot.exists()) {
            val p = snapshot.toUserProfile()
            val expiry = p.membershipExpiry
            if (p.membershipTier != MembershipTier.NONE &&
                expiry != null && expiry < System.currentTimeMillis()) {
                val scheduled = p.scheduledNextTier
                if (scheduled != null) {
                    val newExpiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    docRef.update(mapOf(
                        UserFields.MEMBERSHIP_TIER to scheduled.name,
                        UserFields.MEMBERSHIP_EXPIRY to newExpiry,
                        UserFields.SESSIONS_USED to 0,
                        UserFields.SESSIONS_QUOTA to scheduled.sessionsPerMonth,
                        UserFields.MEMBERSHIP_CANCELLED to false,
                        UserFields.SCHEDULED_NEXT_TIER to null,
                    ))
                    p.copy(
                        membershipTier = scheduled,
                        membershipExpiry = newExpiry,
                        sessionsUsed = 0,
                        sessionsQuota = scheduled.sessionsPerMonth,
                        membershipCancelled = false,
                        scheduledNextTier = null,
                    )
                } else {
                    docRef.update(mapOf(
                        UserFields.MEMBERSHIP_TIER to MembershipTier.NONE.name,
                        UserFields.MEMBERSHIP_EXPIRY to null,
                        UserFields.SESSIONS_USED to 0,
                        UserFields.SESSIONS_QUOTA to 0,
                        UserFields.MEMBERSHIP_CANCELLED to false,
                    ))
                    p.copy(
                        membershipTier = MembershipTier.NONE,
                        membershipExpiry = null,
                        sessionsUsed = 0,
                        sessionsQuota = 0,
                        membershipCancelled = false,
                    )
                }
            } else {
                p
            }
        } else {
            val defaults = mapOf(
                UserFields.DISPLAY_NAME to (firebaseUser.displayName ?: ""),
                UserFields.EMAIL to (firebaseUser.email ?: ""),
                UserFields.MEMBERSHIP_TIER to MembershipTier.NONE.name,
                UserFields.MEMBERSHIP_EXPIRY to null,
                UserFields.SESSIONS_USED to 0,
                UserFields.SESSIONS_QUOTA to 0,
            )
            docRef.set(defaults).await()
            UserProfile(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                membershipTier = MembershipTier.NONE,
                membershipExpiry = null,
                sessionsUsed = 0,
                sessionsQuota = 0,
            )
        }
        cachedProfile = profile
        return profile
    }

    override fun observeProfile(): Flow<UserProfile> = callbackFlow {
        val uid = firebaseAuth.currentUser?.uid ?: run { close(AppError.NotSignedIn); return@callbackFlow }
        val docRef = firestore.collection("users").document(uid)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snapshot != null && snapshot.exists()) {
                val profile = snapshot.toUserProfile()
                cachedProfile = profile
                trySend(profile)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun purchaseMembership(tier: MembershipTier) {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        val expiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        docRef.update(
            mapOf(
                UserFields.MEMBERSHIP_TIER to tier.name,
                UserFields.SESSIONS_QUOTA to tier.sessionsPerMonth,
                UserFields.SESSIONS_USED to 0,
                UserFields.MEMBERSHIP_EXPIRY to expiry,
                UserFields.MEMBERSHIP_CANCELLED to false,
            )
        ).await()
        cachedProfile = null
    }

    override suspend fun upgradeMembership(newTier: MembershipTier) {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        val expiry = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        docRef.update(
            mapOf(
                UserFields.MEMBERSHIP_TIER to newTier.name,
                UserFields.SESSIONS_QUOTA to newTier.sessionsPerMonth,
                UserFields.MEMBERSHIP_EXPIRY to expiry,
                UserFields.MEMBERSHIP_CANCELLED to false,
            )
        ).await()
        cachedProfile = null
    }

    override suspend fun cancelMembership() {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        docRef.update(mapOf(
            UserFields.MEMBERSHIP_CANCELLED to true,
            UserFields.SCHEDULED_NEXT_TIER to null,
        )).await()
        cachedProfile = null
    }

    override suspend fun scheduleNextCycleMembership(tier: MembershipTier) {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        docRef.update(mapOf(UserFields.SCHEDULED_NEXT_TIER to tier.name)).await()
        cachedProfile = null
    }

    override suspend fun incrementSessionsUsed() {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        docRef.update(UserFields.SESSIONS_USED, FieldValue.increment(1)).await()
        cachedProfile = null
    }

    override suspend fun decrementSessionsUsed() {
        requireNetwork()
        val uid = firebaseAuth.requireUid()
        val docRef = firestore.collection("users").document(uid)
        firestore.runTransaction { tx ->
            val current = tx.get(docRef).getLong(UserFields.SESSIONS_USED)?.toInt() ?: 0
            tx.update(docRef, UserFields.SESSIONS_USED, (current - 1).coerceAtLeast(0))
        }.await()
        cachedProfile = null
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile {
        val tier = runCatching {
            MembershipTier.valueOf(getString(UserFields.MEMBERSHIP_TIER) ?: "NONE")
        }.getOrDefault(MembershipTier.NONE)

        val scheduledNextTier = runCatching {
            getString(UserFields.SCHEDULED_NEXT_TIER)?.let { MembershipTier.valueOf(it) }
        }.getOrNull()

        return UserProfile(
            uid = id,
            displayName = getString(UserFields.DISPLAY_NAME) ?: "",
            email = getString(UserFields.EMAIL) ?: "",
            membershipTier = tier,
            membershipExpiry = getLong(UserFields.MEMBERSHIP_EXPIRY),
            sessionsUsed = getLong(UserFields.SESSIONS_USED)?.toInt() ?: 0,
            sessionsQuota = getLong(UserFields.SESSIONS_QUOTA)?.toInt() ?: 0,
            membershipCancelled = getBoolean(UserFields.MEMBERSHIP_CANCELLED) ?: false,
            scheduledNextTier = scheduledNextTier,
        )
    }
}
