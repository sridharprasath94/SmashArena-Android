package com.flash.smasharena.presentation.slots

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.SlotStatus

data class DateItem(
    val dateString: String,
    val dayLabel: String,
    val dayNumber: String,
    val isSelected: Boolean,
    val isBrowsable: Boolean,
)

data class DisplaySlot(
    val hour: Int,
    val timeLabel: String,
    val effectiveStatus: SlotStatus,
    val isSelected: Boolean,
    val isFreeMembership: Boolean = false,
)

enum class ResultType { BOOKED, CANCELLED }

data class BookingResultInfo(
    val type: ResultType,
    val facilityName: String,
    val dateLabel: String,
    val timeLabel: String,
)

sealed class SlotsEvent {
    data class ShowBookingResult(val info: BookingResultInfo) : SlotsEvent()
}

data class SlotsUiState(
    val facilityName: String = "",
    val dates: List<DateItem> = emptyList(),
    val slots: List<DisplaySlot> = emptyList(),
    val selectedSlot: DisplaySlot? = null,
    val isCancelling: Boolean = false,
    val isBookingFree: Boolean = false,
    val error: AppError? = null,
)
