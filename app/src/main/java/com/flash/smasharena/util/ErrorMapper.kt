package com.flash.smasharena.util

import com.flash.smasharena.domain.model.AppError
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is FirebaseAuthInvalidUserException -> AppError.AccountNotFound
    is FirebaseAuthInvalidCredentialsException -> AppError.WrongCredentials
    is FirebaseAuthUserCollisionException -> AppError.EmailInUse
    is FirebaseAuthWeakPasswordException -> AppError.WeakPassword
    is FirebaseNetworkException -> AppError.NoInternet
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> AppError.NoInternet
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.PermissionDenied
        else -> AppError.Unknown(this)
    }
    else -> when {
        message?.contains("Not signed in", ignoreCase = true) == true -> AppError.NotSignedIn
        message?.contains("Not your booking", ignoreCase = true) == true -> AppError.NotYourBooking
        message?.contains("just booked", ignoreCase = true) == true -> AppError.SlotAlreadyBooked
        else -> AppError.Unknown(this)
    }
}
