package com.flash.smasharena.presentation.mybookings

import com.flash.smasharena.domain.model.AppError

data class BookingItem(
    val docId: String,
    val facilityId: String,
    val facilityDisplayName: String,
    val date: String,
    val displayDate: String,
    val timeLabel: String,
    val isFreeMembership: Boolean = false,
)

data class MyBookingsUiState(
    val bookings: List<BookingItem> = emptyList(),
    val isLoading: Boolean = true,
    val cancellingDocId: String? = null,
    val cancelSuccess: Boolean = false,
    val error: AppError? = null,
)
