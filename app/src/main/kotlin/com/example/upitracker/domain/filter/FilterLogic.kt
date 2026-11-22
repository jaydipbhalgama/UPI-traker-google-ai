package com.example.upitracker.domain.filter

import com.example.upitracker.data.category.Category
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.domain.budget.BudgetLogic

enum class TransactionTypeFilter {
    ALL,
    DEBIT,
    CREDIT
}

enum class SortOrder {
    NEWEST,
    OLDEST,
    HIGHEST,
    LOWEST
}

data class FilterState(
    val query: String = "",
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val category: Category? = null,
    val minPaise: Long? = null,
    val maxPaise: Long? = null,
    val sort: SortOrder = SortOrder.NEWEST,
    val allTime: Boolean = false
) {
    /**
     * Returns true if any filter other than default sort/allTime is active.
     */
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
                typeFilter != TransactionTypeFilter.ALL ||
                category != null ||
                minPaise != null ||
                maxPaise != null

    val activeFilterCount: Int
        get() {
            var count = 0
            if (query.isNotBlank()) count++
            if (typeFilter != TransactionTypeFilter.ALL) count++
            if (category != null) count++
            if (minPaise != null || maxPaise != null) count++
            return count
        }
}

data class FilterSummary(
    val count: Int,
    val spentPaise: Long,
    val receivedPaise: Long
)

object FilterLogic {

    /**
     * Checks if a transaction matches a search query.
     *
     * Terms are split on whitespace and combined with AND.
     * Each term matches if contained in lowercase merchant/note/category,
     * or if parsed as rupee amount matches amountPaise.
     */
    fun matchesQuery(tx: Transaction, query: String): Boolean {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return true

        val terms = trimmed.split(Regex("""\s+""")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return true

        val textToSearch = "${tx.merchant} ${tx.note.orEmpty()} ${tx.category}".lowercase()

        return terms.all { term ->
            if (textToSearch.contains(term)) {
                true
            } else {
                val parsedPaise = BudgetLogic.parseRupeesToPaise(term)
                parsedPaise != null && parsedPaise == tx.amountPaise
            }
        }
    }

    /**
     * Applies filters and sorting to the transaction list.
     */
    fun applyFilters(
        transactions: List<Transaction>,
        filterState: FilterState
    ): List<Transaction> {
        val filtered = transactions.filter { tx ->
            // 1. Type filter
            val matchesType = when (filterState.typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.DEBIT -> tx.type == TransactionType.DEBIT
                TransactionTypeFilter.CREDIT -> tx.type == TransactionType.CREDIT
            }
            if (!matchesType) return@filter false

            // 2. Category filter
            if (filterState.category != null) {
                if (Category.fromLabel(tx.category) != filterState.category) return@filter false
            }

            // 3. Min paise (inclusive)
            if (filterState.minPaise != null && tx.amountPaise < filterState.minPaise) {
                return@filter false
            }

            // 4. Max paise (inclusive)
            if (filterState.maxPaise != null && tx.amountPaise > filterState.maxPaise) {
                return@filter false
            }

            // 5. Query matching
            matchesQuery(tx, filterState.query)
        }

        val comparator = when (filterState.sort) {
            SortOrder.NEWEST -> compareByDescending<Transaction> { it.timestamp }.thenByDescending { it.id }
            SortOrder.OLDEST -> compareBy<Transaction> { it.timestamp }.thenBy { it.id }
            SortOrder.HIGHEST -> compareByDescending<Transaction> { it.amountPaise }.thenByDescending { it.timestamp }.thenByDescending { it.id }
            SortOrder.LOWEST -> compareBy<Transaction> { it.amountPaise }.thenByDescending { it.timestamp }.thenByDescending { it.id }
        }

        return filtered.sortedWith(comparator)
    }

    /**
     * Summarizes transactions into count, spentPaise, and receivedPaise.
     */
    fun summarize(transactions: List<Transaction>): FilterSummary {
        var spent = 0L
        var received = 0L
        for (tx in transactions) {
            if (tx.type == TransactionType.DEBIT) {
                spent += tx.amountPaise
            } else if (tx.type == TransactionType.CREDIT) {
                received += tx.amountPaise
            }
        }
        return FilterSummary(
            count = transactions.size,
            spentPaise = spent,
            receivedPaise = received
        )
    }
}
