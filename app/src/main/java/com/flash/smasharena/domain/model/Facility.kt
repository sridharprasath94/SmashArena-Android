package com.flash.smasharena.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flash.smasharena.R

enum class Facility(
    val id: String,
    val displayName: String,
    @StringRes val nameRes: Int,
    @DrawableRes val imageRes: Int,
    val description: String,
) {
    BADMINTON_COURT(
        id = "badminton_court_1",
        displayName = "Badminton Court",
        nameRes = R.string.facility_badminton,
        imageRes = R.drawable.badminton,
        description = "05:00 – 22:00 · 1-hour sessions",
    );

    fun priceForHour(hour: Int): Int = when (this) {
        BADMINTON_COURT -> if (hour in 10..15) 200 else 300
    }
}
