package com.flash.smasharena.presentation.mybookings

import com.flash.smasharena.domain.model.AppError

data class BookingItem(
    val docId: String,
    val facilityId: String,
    val facilityDisplayName: String,
    val date: String,
    val displayDate: String,
    val hour: Int,
    val timeLabel: String,
    val isFreeMembership: Boolean = false,
    val isExpired: Boolean = false,
)

sealed class BookingListItem {
    data class Header(val label: String) : BookingListItem()
    data class Booking(val item: BookingItem) : BookingListItem()
}

data class MyBookingsUiState(
    val listItems: List<BookingListItem> = emptyList(),
    val isLoading: Boolean = true,
    val cancellingDocId: String? = null,
    val cancelSuccess: Boolean = false,
    val error: AppError? = null,
)
