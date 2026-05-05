package com.flash.smasharena.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.flash.smasharena.R

enum class Facility(
    val id: String,
    @StringRes val nameRes: Int,
    @DrawableRes val imageRes: Int,
    val description: String,
) {
    BADMINTON_COURT(
        id = "badminton_court_1",
        nameRes = R.string.facility_badminton,
        imageRes = R.drawable.badminton,
        description = "05:00 – 22:00 · 1-hour sessions",
    ),
    CRICKET_NET(
        id = "cricket_net_1",
        nameRes = R.string.facility_cricket,
        imageRes = R.drawable.cricket,
        description = "05:00 – 22:00 · 1-hour sessions",
    );

    fun priceForHour(hour: Int): Int = when (this) {
        BADMINTON_COURT -> if (hour in 10..15) 200 else 300
        CRICKET_NET     -> if (hour in 10..15) 300 else 400
    }
}
