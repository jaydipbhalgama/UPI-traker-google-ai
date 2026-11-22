package com.example.upitracker.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.upitracker.data.category.AutoCategorizer
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class SyncResult(
    val scanned: Int = 0,
    val imported: Int = 0,
    val duplicates: Int = 0,
    val skipped: Int = 0,
    val error: String? = null
)

/**
 * Reads SMS messages from the system Telephony SMS provider, parses UPI transactions,
 * and saves unique entries to the Room database.
 *
 * Privacy rule: Never log raw SMS body, sender, or account numbers.
 */
class SmsSyncManager(
    private val context: Context,
    private val repository: TransactionRepository
) {

    companion object {
        const val INITIAL_IMPORT_DAYS: Long = 90
        private const val PREFS_NAME = "upi_tracker_sms_sync"
        private const val KEY_LAST_SYNCED = "key_last_synced_timestamp"

        /**
         * Pure function: returns the epoch-ms start time for a full 90-day rescan.
         * Kept in the companion so it can be unit-tested without Android classes.
         */
        fun rescanStartTime(nowMs: Long): Long =
            nowMs - TimeUnit.DAYS.toMillis(INITIAL_IMPORT_DAYS)
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun syncAll(): SyncResult = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext SyncResult(error = "READ_SMS permission not granted")
        }

        val lastSynced = prefs.getLong(KEY_LAST_SYNCED, 0L)
        val startTime = if (lastSynced == 0L) {
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(INITIAL_IMPORT_DAYS)
        } else {
            // Overlap by 60 seconds to ensure no edge-case messages are missed
            (lastSynced - TimeUnit.SECONDS.toMillis(60)).coerceAtLeast(0L)
        }

        runSyncFrom(startTime, saveTimestamp = true)
    }

    suspend fun syncRecent(): SyncResult = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext SyncResult(error = "READ_SMS permission not granted")
        }

        val lastSynced = prefs.getLong(KEY_LAST_SYNCED, 0L)
        val startTime = if (lastSynced == 0L) {
            System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(15)
        } else {
            (lastSynced - TimeUnit.SECONDS.toMillis(60)).coerceAtLeast(0L)
        }

        runSyncFrom(startTime, saveTimestamp = true)
    }

    /**
     * Always scans the last [INITIAL_IMPORT_DAYS] days regardless of [lastSyncedDate].
     * Duplicates are skipped by the existing smsHash unique index.
     * Does NOT update lastSyncedDate so normal sync cadence is unaffected.
     */
    suspend fun rescanLast90Days(): SyncResult = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext SyncResult(error = "READ_SMS permission not granted")
        }

        val startTime = rescanStartTime(System.currentTimeMillis())
        runSyncFrom(startTime, saveTimestamp = false)
    }

    private suspend fun runSyncFrom(startTime: Long, saveTimestamp: Boolean): SyncResult {
        var scanned = 0
        var imported = 0
        var duplicates = 0
        var skipped = 0
        var latestDateSeen = startTime

        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )
        val selection = "${Telephony.Sms.Inbox.DATE} >= ?"
        val selectionArgs = arrayOf(startTime.toString())
        val sortOrder = "${Telephony.Sms.Inbox.DATE} ASC"

        // Load rules ONCE per sync
        val rulesMap: Map<String, String> = try {
            repository.getAllMerchantRules().associate { it.merchantKey to it.category }
        } catch (e: Exception) {
            emptyMap()
        }

        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS)
                val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY)
                val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.Inbox.DATE)

                while (it.moveToNext()) {
                    scanned++
                    val sender = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    if (date > latestDateSeen) {
                        latestDateSeen = date
                    }

                    val parsed = SmsParser.parse(sender, body, date)
                    if (parsed == null) {
                        skipped++
                        continue
                    }

                    val category = AutoCategorizer.categorize(
                        merchant = parsed.merchant,
                        type = parsed.type,
                        rules = rulesMap
                    )

                    val hash = computeSmsHash(sender, date, body)
                    val transaction = Transaction(
                        amountPaise = parsed.amountPaise,
                        type = parsed.type,
                        merchant = parsed.merchant,
                        category = category.label,
                        timestamp = date,
                        bankName = parsed.bankName,
                        balanceAfterPaise = parsed.balanceAfterPaise,
                        smsHash = hash,
                        note = null
                    )

                    val rowId = repository.insert(transaction)
                    if (rowId == -1L) {
                        duplicates++
                    } else {
                        imported++
                    }
                }
            }

            // Save the latest synced timestamp only for normal syncs, not for re-scans
            if (saveTimestamp && latestDateSeen > startTime) {
                prefs.edit().putLong(KEY_LAST_SYNCED, latestDateSeen).apply()
            }

            return SyncResult(
                scanned = scanned,
                imported = imported,
                duplicates = duplicates,
                skipped = skipped
            )
        } catch (e: Exception) {
            return SyncResult(
                scanned = scanned,
                imported = imported,
                duplicates = duplicates,
                skipped = skipped,
                error = e.message ?: "Failed to read SMS inbox"
            )
        }
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun computeSmsHash(sender: String, date: Long, body: String): String {
        val input = "$sender|$date|$body"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
