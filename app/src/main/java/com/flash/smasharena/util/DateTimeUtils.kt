package com.flash.smasharena.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())

    fun today(): String = dateFormat.format(Date())

    fun dateWithOffset(offsetDays: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        return dateFormat.format(cal.time)
    }

    fun dayLabel(dateString: String): String =
        dateFormat.parse(dateString)?.let { dayOfWeekFormat.format(it).uppercase() } ?: ""

    fun dayNumber(dateString: String): String {
        val date = dateFormat.parse(dateString) ?: return ""
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }

    /** Formats "yyyy-MM-dd" as "Mon, 5 May". */
    fun displayDate(dateString: String): String =
        dateFormat.parse(dateString)?.let { displayDateFormat.format(it) } ?: dateString

    /** Returns how many full days until the given date (negative = past). */
    fun daysUntil(dateString: String): Int {
        val slotDate = dateFormat.parse(dateString)?.time ?: return -1
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return ((slotDate - todayStart) / (1000L * 60 * 60 * 24)).toInt()
    }
}
