package com.example.upitracker.data.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Scans the last 90 days of the SMS inbox and returns messages that look like
 * bank transaction SMS but were NOT recognised by [SmsParser].
 *
 * Filtering criteria (all must be true to include):
 *   1. Sender passes the same validity rules used by [SmsParser]:
 *      ≤15 chars, contains ≥1 letter, is NOT purely digits / '+' / spaces.
 *   2. Body contains at least one of: debited, credited, Rs, INR, ₹  (case-insensitive).
 *   3. Body does NOT look like an OTP: no "otp", "one time password", "do not share".
 *   4. [SmsParser.parse] returns null for this message.
 *
 * Results are in memory only — never saved to the database and never logged.
 * Privacy rule: Never log raw SMS body, sender, or account numbers.
 */
class UnrecognisedSmsScanner(private val context: Context) {

    companion object {
        private const val SCAN_DAYS: Long = SmsSyncManager.INITIAL_IMPORT_DAYS
        private const val MAX_RESULTS = 50

        // Mirror of SmsParser.isSenderValid (private there, replicated to avoid touching SmsParser)
        private val SENDER_HAS_LETTER = Regex("[a-zA-Z]")
        private val SENDER_ONLY_DIGITS_PLUS_SPACE = Regex("^[0-9+\\s]+$")

        private fun isSenderValid(sender: String): Boolean {
            if (sender.length > 15) return false
            if (!SENDER_HAS_LETTER.containsMatchIn(sender)) return false
            if (SENDER_ONLY_DIGITS_PLUS_SPACE.matches(sender)) return false
            return true
        }

        // Body must contain at least one bank-transaction keyword
        private val BANK_KEYWORD_REGEX = Regex(
            "debited|credited|\\bRs\\b|\\bINR\\b|₹",
            RegexOption.IGNORE_CASE
        )

        // OTP-like phrases that disqualify the message
        private val OTP_PHRASES = listOf("otp", "one time password", "do not share")
    }

    sealed interface ScanResult {
        data object NoPermission : ScanResult
        data class Success(val items: List<UnrecognisedSmsItem>) : ScanResult
    }

    /**
     * Performs the scan on [Dispatchers.IO]. Safe to call from a coroutine scope.
     */
    suspend fun scan(): ScanResult = withContext(Dispatchers.IO) {
        if (!hasSmsPermission()) {
            return@withContext ScanResult.NoPermission
        }

        val startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(SCAN_DAYS)
        val results = mutableListOf<UnrecognisedSmsItem>()

        val projection = arrayOf(
            Telephony.Sms.Inbox.ADDRESS,
            Telephony.Sms.Inbox.BODY,
            Telephony.Sms.Inbox.DATE
        )
        val selection = "${Telephony.Sms.Inbox.DATE} >= ?"
        val selectionArgs = arrayOf(startTime.toString())
        // Newest first so we naturally take the top 50 most recent
        val sortOrder = "${Telephony.Sms.Inbox.DATE} DESC"

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

                while (it.moveToNext() && results.size < MAX_RESULTS) {
                    val sender = it.getString(addressIdx) ?: ""
                    val body = it.getString(bodyIdx) ?: ""
                    val date = it.getLong(dateIdx)

                    // Filter 1: sender must pass bank-sender validation
                    if (!isSenderValid(sender)) continue

                    val lowerBody = body.lowercase()

                    // Filter 2: body must contain a bank-transaction keyword
                    if (!BANK_KEYWORD_REGEX.containsMatchIn(body)) continue

                    // Filter 3: skip OTP-like messages
                    if (OTP_PHRASES.any { phrase -> lowerBody.contains(phrase) }) continue

                    // Filter 4: SmsParser must have returned null
                    if (SmsParser.parse(sender, body, date) != null) continue

                    results.add(UnrecognisedSmsItem(timestamp = date, sender = sender, body = body))
                }
            }
        } catch (_: Exception) {
            // Silently return whatever we collected so far; screen shows partial/empty list
        }

        ScanResult.Success(results)
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
}
