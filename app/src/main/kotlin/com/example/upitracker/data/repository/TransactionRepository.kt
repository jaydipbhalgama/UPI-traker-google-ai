package com.example.upitracker.data.repository

import com.example.upitracker.data.local.dao.MerchantRuleDao
import com.example.upitracker.data.local.dao.TransactionDao
import com.example.upitracker.data.local.entity.MerchantRule
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insert(transaction: Transaction): Long
    suspend fun insertAll(transactions: List<Transaction>): List<Long>
    suspend fun update(transaction: Transaction)
    suspend fun delete(transaction: Transaction)
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>>
    fun getTotalByTypeBetween(type: TransactionType, start: Long, end: Long): Flow<Long?>
    fun getLatestBalancePaise(): Flow<Long?>
    fun getLatestBalanceTransaction(): Flow<Transaction?>
    fun getOldestTransactionTimestamp(): Flow<Long?>
    fun getOldestTimestamp(): Flow<Long?>
    fun getCount(): Flow<Int>
    suspend fun getAllOnce(): List<Transaction>

    // Phase 4
    suspend fun updateCategory(id: Long, category: String)
    suspend fun updateNote(id: Long, note: String?)
    suspend fun updateCategoryForMerchantKey(merchantKey: String, category: String)
    suspend fun countByMerchantKey(merchantKey: String): Int
    suspend fun getUncategorizedTransactions(): List<Transaction>
    suspend fun updateCategoryBatch(updates: List<Pair<Long, String>>)
    suspend fun upsertMerchantRule(rule: MerchantRule)
    suspend fun getAllMerchantRules(): List<MerchantRule>
    fun getAllMerchantRulesFlow(): Flow<List<MerchantRule>>

    /**
     * Atomically saves a manual edit:
     *  1. Updates [category] and [note] for the given transaction [id].
     *  2. If [applyToAll] is true and [merchantKey] is non-blank/non-unknown:
     *     upserts a MerchantRule and bulk-updates all matching transactions.
     */
    suspend fun saveTransactionEdit(
        id: Long,
        category: String,
        note: String?,
        merchantKey: String,
        applyToAll: Boolean
    )
}

class DefaultTransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantRuleDao: MerchantRuleDao
) : TransactionRepository {

    override suspend fun insert(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    override suspend fun insertAll(transactions: List<Transaction>): List<Long> =
        transactionDao.insertAll(transactions)

    override suspend fun update(transaction: Transaction) =
        transactionDao.update(transaction)

    override suspend fun delete(transaction: Transaction) =
        transactionDao.delete(transaction)

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(start, end)

    override fun getTotalByTypeBetween(type: TransactionType, start: Long, end: Long): Flow<Long?> =
        transactionDao.getTotalByTypeBetween(type, start, end)

    override fun getLatestBalancePaise(): Flow<Long?> =
        transactionDao.getLatestBalancePaise()

    override fun getLatestBalanceTransaction(): Flow<Transaction?> =
        transactionDao.getLatestBalanceTransaction()

    override fun getOldestTransactionTimestamp(): Flow<Long?> =
        transactionDao.getOldestTransactionTimestamp()

    override fun getOldestTimestamp(): Flow<Long?> =
        transactionDao.getOldestTimestamp()

    override fun getCount(): Flow<Int> =
        transactionDao.getCount()

    override suspend fun getAllOnce(): List<Transaction> =
        transactionDao.getAllOnce()

    override suspend fun updateCategory(id: Long, category: String) =
        transactionDao.updateCategory(id, category)

    override suspend fun updateNote(id: Long, note: String?) =
        transactionDao.updateNote(id, note)

    override suspend fun updateCategoryForMerchantKey(merchantKey: String, category: String) =
        transactionDao.updateCategoryForMerchantKey(merchantKey, category)

    override suspend fun countByMerchantKey(merchantKey: String): Int =
        transactionDao.countByMerchantKey(merchantKey)

    override suspend fun getUncategorizedTransactions(): List<Transaction> =
        transactionDao.getUncategorizedTransactions()

    override suspend fun updateCategoryBatch(updates: List<Pair<Long, String>>) =
        transactionDao.updateCategoryBatch(updates)

    override suspend fun upsertMerchantRule(rule: MerchantRule) =
        merchantRuleDao.upsert(rule)

    override suspend fun getAllMerchantRules(): List<MerchantRule> =
        merchantRuleDao.getAll()

    override fun getAllMerchantRulesFlow(): Flow<List<MerchantRule>> =
        merchantRuleDao.getAllFlow()

    override suspend fun saveTransactionEdit(
        id: Long,
        category: String,
        note: String?,
        merchantKey: String,
        applyToAll: Boolean
    ) {
        // 1. Always update the single transaction
        transactionDao.updateCategory(id, category)
        transactionDao.updateNote(id, note)

        // 2. Apply rule to all matching transactions (never for blank or "unknown")
        val isValidMerchant = merchantKey.isNotBlank() && merchantKey != "unknown"
        if (applyToAll && isValidMerchant) {
            merchantRuleDao.upsert(
                MerchantRule(
                    merchantKey = merchantKey,
                    category = category,
                    updatedAt = System.currentTimeMillis()
                )
            )
            transactionDao.updateCategoryForMerchantKey(merchantKey, category)
        }
    }
}
