package com.example.upitracker.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailySummarySchedulerTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")

    @Test
    fun nextTriggerAtMillis_before8Pm_schedulesSameDayAt8Pm() {
        val now = LocalDateTime.of(2026, 11, 21, 14, 30, 0)
        val expected = LocalDateTime.of(2026, 11, 21, 20, 0, 0)
            .atZone(fixedZone).toInstant().toEpochMilli()

        val actual = DailySummaryScheduler.nextTriggerAtMillis(now, fixedZone)

        assertEquals(expected, actual)
    }

    @Test
    fun nextTriggerAtMillis_atOrAfter8Pm_schedulesNextDayAt8Pm() {
        val atEight = LocalDateTime.of(2026, 11, 21, 20, 0, 0)
        val afterEight = LocalDateTime.of(2026, 11, 21, 22, 15, 0)
        val expected = LocalDateTime.of(2026, 11, 22, 20, 0, 0)
            .atZone(fixedZone).toInstant().toEpochMilli()

        assertEquals(expected, DailySummaryScheduler.nextTriggerAtMillis(atEight, fixedZone))
        assertEquals(expected, DailySummaryScheduler.nextTriggerAtMillis(afterEight, fixedZone))
    }

    @Test
    fun nextTriggerAtMillis_earlyAlarmWithBuffer_schedulesNextDayInsteadOfRefiring() {
        // Alarm delivered 2 minutes early at 19:58; with 5-minute buffer it must advance to tomorrow
        val slightlyEarly = LocalDateTime.of(2026, 11, 21, 19, 58, 0)
        val expectedTomorrow = LocalDateTime.of(2026, 11, 22, 20, 0, 0)
            .atZone(fixedZone).toInstant().toEpochMilli()

        val actual = DailySummaryScheduler.nextTriggerAtMillis(
            now = slightlyEarly,
            zoneId = fixedZone,
            minDelayMinutes = 5L
        )

        assertEquals(expectedTomorrow, actual)
    }
}
