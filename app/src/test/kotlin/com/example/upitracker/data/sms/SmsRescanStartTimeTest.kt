package com.example.upitracker.data.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [SmsSyncManager.rescanStartTime].
 *
 * This pure companion function computes the start timestamp for a full 90-day rescan.
 * It does NOT depend on any Android classes, so it runs as a plain JVM test.
 */
class SmsRescanStartTimeTest {

    private val ninetyDaysMs = TimeUnit.DAYS.toMillis(SmsSyncManager.INITIAL_IMPORT_DAYS)

    // ── Core contract: always ignores lastSyncedDate ───────────────────────────

    @Test
    fun rescanStartTime_isExactly90DaysBeforeNow() {
        val now = 1_000_000_000_000L // arbitrary epoch-ms
        val start = SmsSyncManager.rescanStartTime(now)
        assertEquals(now - ninetyDaysMs, start)
    }

    @Test
    fun rescanStartTime_doesNotDependOnLastSyncedDate() {
        val now = 1_700_000_000_000L

        // Simulate: lastSynced was 1 day ago (recent sync)
        val recentLastSynced = now - TimeUnit.DAYS.toMillis(1)
        // Simulate: lastSynced was 0 (never synced)
        val neverSynced = 0L

        val startForRecent = SmsSyncManager.rescanStartTime(now)
        val startForNever = SmsSyncManager.rescanStartTime(now)

        // Both must equal now - 90 days, regardless of what lastSynced would be
        val expected = now - ninetyDaysMs
        assertEquals(
            "Re-scan start must always be 90 days before now (recent sync case)",
            expected, startForRecent
        )
        assertEquals(
            "Re-scan start must always be 90 days before now (never-synced case)",
            expected, startForNever
        )

        // Prove it differs from what a normal syncAll() would compute for a recent last-sync
        val normalSyncStart = recentLastSynced - TimeUnit.SECONDS.toMillis(60)
        assertTrue(
            "Re-scan start ($startForRecent) must be EARLIER than normal sync start ($normalSyncStart) when lastSynced is recent",
            startForRecent < normalSyncStart
        )
    }

    @Test
    fun rescanStartTime_alwaysCoversFullNinetyDays() {
        // For any plausible 'now', the window must be exactly INITIAL_IMPORT_DAYS wide
        val samples = listOf(
            System.currentTimeMillis(),
            1_000_000_000_000L,
            1_800_000_000_000L,
            Long.MAX_VALUE / 2
        )
        for (now in samples) {
            val start = SmsSyncManager.rescanStartTime(now)
            assertEquals(
                "Window must be exactly $ninetyDaysMs ms for nowMs=$now",
                ninetyDaysMs, now - start
            )
        }
    }

    @Test
    fun initialImportDays_isNinety() {
        // Guard: if someone changes the constant, this catches it
        assertEquals(90L, SmsSyncManager.INITIAL_IMPORT_DAYS)
    }
}
