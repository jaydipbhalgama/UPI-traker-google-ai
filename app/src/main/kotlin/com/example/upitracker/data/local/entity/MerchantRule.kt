package com.example.upitracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the user-chosen category for a merchant key so it can be
 * auto-applied to future transactions from the same merchant.
 *
 * merchantKey = merchant.trim().lowercase() (spaces collapsed) — matches
 * the LOWER(TRIM(merchant)) used in DAO queries.
 */
@Entity(tableName = "merchant_rules")
data class MerchantRule(
    @PrimaryKey
    val merchantKey: String,
    val category: String,
    val updatedAt: Long
)
