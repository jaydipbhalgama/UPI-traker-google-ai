package com.example.upitracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores a recurring monthly budget limit.
 *
 * [budgetKey] is either [TOTAL_BUDGET_KEY] ("__TOTAL__") for the overall monthly budget,
 * or a permanent [com.example.upitracker.data.category.Category] label.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey
    val budgetKey: String,
    val limitPaise: Long,
    val updatedAt: Long
) {
    companion object {
        const val TOTAL_BUDGET_KEY = "__TOTAL__"
    }
}
