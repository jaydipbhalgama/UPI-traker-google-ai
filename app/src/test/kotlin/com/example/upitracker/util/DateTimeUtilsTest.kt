package com.example.upitracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class DateTimeUtilsTest {

    private val fixedZone = ZoneId.of("Asia/Kolkata")

    @Test
    fun monthRange_january2026_verifiesInclusionAndExclusion() {
        val ym = YearMonth.of(2026, 1)
        val startMillis = DateTimeUtils.monthStartMillis(ym, fixedZone)
        val endExclusiveMillis = DateTimeUtils.monthEndExclusiveMillis(ym, fixedZone)

        // 2026-01-01 00:00:00.000 in fixedZone
        val tStart = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 0)
            .atZone(fixedZone)
            .toInstant()
            .toEpochMilli()

        // 2026-01-31 23:59:59.999 in fixedZone
        val tEnd = LocalDateTime.of(2026, 1, 31, 23, 59, 59, 999_000_000)
            .atZone(fixedZone)
            .toInstant()
            .toEpochMilli()

        // 2026-02-01 00:00:00.000 in fixedZone
        val tNextMonth = LocalDateTime.of(2026, 2, 1, 0, 0, 0, 0)
            .atZone(fixedZone)
            .toInstant()
            .toEpochMilli()

        assertEquals(tStart, startMillis)
        assertEquals(tNextMonth, endExclusiveMillis)

        // Check inclusive start and exclusive end: timestamp >= start && timestamp < endExclusive
        assertTrue("Start timestamp must be included", tStart in startMillis until endExclusiveMillis)
        assertTrue("End of month timestamp must be included", tEnd in startMillis until endExclusiveMillis)
        assertFalse("Next month 00:00:00 must be excluded", tNextMonth in startMillis until endExclusiveMillis)
    }
}
