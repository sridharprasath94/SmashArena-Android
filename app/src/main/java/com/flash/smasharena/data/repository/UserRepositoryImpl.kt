package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.UserProfile
import com.flash.smasharena.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
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
        )
    }
}
