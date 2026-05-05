package com.flash.smasharena.domain.model

sealed class AppError : Exception() {
    // Connectivity
    data object NoInternet : AppError()
    data object ServiceUnavailable : AppError()

    // Auth
    data object NotSignedIn : AppError()
    data object AccountNotFound : AppError()
    data object WrongCredentials : AppError()
    data object EmailInUse : AppError()
    data object WeakPassword : AppError()

    // Slots
    data object SlotAlreadyBooked : AppError()
    data object NotYourBooking : AppError()
    data object PermissionDenied : AppError()

    data class Unknown(override val cause: Throwable) : AppError()
}
