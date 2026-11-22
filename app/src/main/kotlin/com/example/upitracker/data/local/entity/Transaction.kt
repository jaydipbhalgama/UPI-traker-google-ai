package com.example.upitracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Indicates whether money was deducted (DEBIT) or received (CREDIT).
 */
enum class TransactionType {
    DEBIT,
    CREDIT
}

/**
 * Represents a single financial transaction parsed from bank SMS or added manually.
 *
 * Monetary values are stored in paise (1 INR = 100 paise) to avoid floating-point issues.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsHash"], unique = true)
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String = "Unknown",
    val category: String = "Uncategorized",
    val timestamp: Long,
    val bankName: String? = null,
    val balanceAfterPaise: Long? = null,
    val smsHash: String,
    val note: String? = null
)
