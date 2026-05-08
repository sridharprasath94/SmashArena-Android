package com.flash.smasharena.util

import com.flash.smasharena.domain.model.AppError
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}

fun FirebaseAuth.requireUid(): String =
    currentUser?.uid ?: throw AppError.NotSignedIn
