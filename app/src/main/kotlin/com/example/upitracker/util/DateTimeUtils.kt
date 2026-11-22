package com.example.upitracker.util

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM, h:mm a", Locale.getDefault())

    fun monthStartMillis(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return yearMonth.atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun monthEndExclusiveMillis(
        yearMonth: YearMonth,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        return yearMonth.plusMonths(1)
            .atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        return yearMonth.format(monthYearFormatter)
    }

    fun formatDateTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zonedDateTime.format(dateTimeFormatter)
    }

    fun formatShortDateTime(
        epochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return zonedDateTime.format(shortDateTimeFormatter)
    }
}
