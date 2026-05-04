package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override val isSignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            suspendCancellableCoroutine { cont ->
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }

    override suspend fun createAccountWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            suspendCancellableCoroutine { cont ->
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            suspendCancellableCoroutine { cont ->
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        }

    override fun signOut() = firebaseAuth.signOut()
}
