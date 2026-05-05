package com.flash.smasharena.data.repository

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.repository.AuthRepository
import com.flash.smasharena.util.NetworkMonitor
import com.flash.smasharena.util.toAppError
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient,
    private val networkMonitor: NetworkMonitor,
) : AuthRepository {

    override val isSignedIn: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            if (!networkMonitor.isConnected()) throw AppError.NoInternet
            suspendCancellableCoroutine { cont ->
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it.toAppError()) }
            }
        }

    override suspend fun createAccountWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            if (!networkMonitor.isConnected()) throw AppError.NoInternet
            suspendCancellableCoroutine { cont ->
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it.toAppError()) }
            }
        }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
        runCatching {
            if (!networkMonitor.isConnected()) throw AppError.NoInternet
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            suspendCancellableCoroutine { cont ->
                firebaseAuth.signInWithCredential(credential)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resumeWithException(it.toAppError()) }
            }
        }

    override fun signOut() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
    }
}
