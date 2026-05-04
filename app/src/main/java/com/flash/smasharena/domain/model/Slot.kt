package com.flash.smasharena.domain.model

data class Slot(
    val facilityId: String,
    val date: String,           // "YYYY-MM-DD"
    val hour: Int,              // 5..21  (5 = 05:00–06:00)
    val status: SlotStatus,
    val bookedBy: String? = null,
) {
    val docId: String get() = "${facilityId}_${date}_${hour}"
    val timeLabel: String get() = String.format("%02d:00 – %02d:00", hour, hour + 1)
}
