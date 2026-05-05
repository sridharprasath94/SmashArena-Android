package com.flash.smasharena.presentation.mybookings

import com.flash.smasharena.domain.model.AppError

data class BookingItem(
    val docId: String,
    val facilityId: String,
    val facilityDisplayName: String,
    val date: String,
    val displayDate: String,
    val timeLabel: String,
)

data class MyBookingsUiState(
    val bookings: List<BookingItem> = emptyList(),
    val isLoading: Boolean = true,
    val cancellingDocId: String? = null,
    val error: AppError? = null,
)
