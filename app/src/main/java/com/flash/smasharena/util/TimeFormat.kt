package com.flash.smasharena.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Tiny helpers around `java.time` so callers don't sprinkle `ZoneId.systemDefault()`
 * across the UI.
 */
object TimeFormat {
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")

    fun formatDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(dateFmt)

    fun formatTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(timeFmt)

    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun atHourMinute(date: LocalDate, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
}
