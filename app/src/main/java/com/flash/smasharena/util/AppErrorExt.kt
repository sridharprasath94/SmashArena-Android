package com.flash.smasharena.util

import android.content.Context
import com.flash.smasharena.R
import com.flash.smasharena.domain.model.AppError

fun AppError.toUserMessage(context: Context): String = context.getString(
    when (this) {
        AppError.NoInternet -> R.string.error_no_internet
        AppError.ServiceUnavailable -> R.string.error_service_unavailable
        AppError.NotSignedIn -> R.string.error_not_signed_in
        AppError.AccountNotFound -> R.string.error_account_not_found
        AppError.WrongCredentials -> R.string.error_wrong_credentials
        AppError.EmailInUse -> R.string.error_email_in_use
        AppError.WeakPassword -> R.string.error_weak_password
        AppError.SlotAlreadyBooked -> R.string.error_slot_taken
        AppError.NotYourBooking -> R.string.error_not_your_booking
        AppError.PermissionDenied -> R.string.error_permission_denied
        is AppError.Unknown -> R.string.error_unknown
    }
)
