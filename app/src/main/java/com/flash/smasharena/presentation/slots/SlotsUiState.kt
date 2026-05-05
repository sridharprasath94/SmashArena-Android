package com.flash.smasharena.presentation.slots

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
)

data class SlotsUiState(
    val facilityName: String = "",
    val dates: List<DateItem> = emptyList(),
    val slots: List<DisplaySlot> = emptyList(),
    val selectedSlot: DisplaySlot? = null,
    val isBooking: Boolean = false,
    val bookingSuccess: Boolean = false,
    val isCancelling: Boolean = false,
    val cancelSuccess: Boolean = false,
    val error: String? = null,
)
