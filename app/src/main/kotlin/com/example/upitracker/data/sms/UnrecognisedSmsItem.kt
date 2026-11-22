package com.example.upitracker.data.sms

/**
 * A single SMS that passed the bank-sender + keyword filter but was NOT
 * recognised as a complete UPI transaction by [SmsParser].
 *
 * Lives in memory only — never persisted, never logged.
 */
data class UnrecognisedSmsItem(
    val timestamp: Long,
    val sender: String,
    val body: String
)
