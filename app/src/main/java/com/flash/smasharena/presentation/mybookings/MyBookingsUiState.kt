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
    val isOngoing: Boolean = false,
)

sealed class PastTimelineItem {
    data class YearHeader(val year: Int, val isExpanded: Boolean) : PastTimelineItem()
    data class MonthHeader(val year: Int, val month: Int, val label: String, val isExpanded: Boolean) : PastTimelineItem()
    data class BookingEntry(val item: BookingItem) : PastTimelineItem()
    data class BookingControls(
        val year: Int,
        val month: Int,
        val canShowMore: Boolean,
        val canCollapse: Boolean,
    ) : PastTimelineItem()
    object ShowMore : PastTimelineItem()
}

data class CancelledBookingItem(
    val docId: String,
    val facilityId: String,
    val facilityDisplayName: String,
    val displayDate: String,
    val timeLabel: String,
    val cancelledDisplayDate: String,
    val isSelected: Boolean = false,
)

data class MyBookingsUiState(
    val upcomingItems: List<BookingItem> = emptyList(),
    val isUpcomingLoading: Boolean = true,
    val cancellingDocId: String? = null,
    val cancelSuccess: Boolean = false,
    val pastTimelineItems: List<PastTimelineItem> = emptyList(),
    val isPastLoading: Boolean = false,
    val cancelledItems: List<CancelledBookingItem> = emptyList(),
    val isCancelledLoading: Boolean = true,
    val selectedCancelledIds: Set<String> = emptySet(),
    val isDeletingCancelled: Boolean = false,
    val error: AppError? = null,
)
