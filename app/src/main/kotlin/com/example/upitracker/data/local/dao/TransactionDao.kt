package com.example.upitracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.upitracker.data.local.entity.Transaction as TransactionEntity
import com.example.upitracker.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * Inserts a transaction. Returns row ID, or -1 if a duplicate smsHash already exists.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amountPaise) FROM transactions WHERE type = :type AND timestamp >= :start AND timestamp < :end")
    fun getTotalByTypeBetween(type: TransactionType, start: Long, end: Long): Flow<Long?>

    @Query("SELECT balanceAfterPaise FROM transactions WHERE balanceAfterPaise IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBalancePaise(): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE balanceAfterPaise IS NOT NULL ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBalanceTransaction(): Flow<TransactionEntity?>

    @Query("SELECT MIN(timestamp) FROM transactions")
    fun getOldestTransactionTimestamp(): Flow<Long?>

    @Query("SELECT MIN(timestamp) FROM transactions")
    fun getOldestTimestamp(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getCount(): Flow<Int>

    // ── Phase 4: Category editing queries ──────────────────────────────

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE transactions SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    /**
     * Updates all transactions whose merchant (trimmed, lowercased) equals
     * [merchantKey] to the given [category].
     * Matches the LOWER(TRIM(merchant)) DAO convention used by [countByMerchantKey].
     */
    @Query("UPDATE transactions SET category = :category WHERE LOWER(TRIM(merchant)) = :merchantKey")
    suspend fun updateCategoryForMerchantKey(merchantKey: String, category: String)

    /**
     * Counts transactions matching [merchantKey] (LOWER(TRIM(merchant))).
     */
    @Query("SELECT COUNT(*) FROM transactions WHERE LOWER(TRIM(merchant)) = :merchantKey")
    suspend fun countByMerchantKey(merchantKey: String): Int

    /**
     * Returns all transactions whose category is exactly "Uncategorized".
     */
    @Query("SELECT * FROM transactions WHERE category = 'Uncategorized'")
    suspend fun getUncategorizedTransactions(): List<TransactionEntity>

    /**
     * Batch-updates a list of (id, category) pairs inside a single DB transaction.
     */
    @Transaction
    suspend fun updateCategoryBatch(updates: List<Pair<Long, String>>) {
        for ((id, category) in updates) {
            updateCategory(id, category)
        }
    }

    // ── Phase 6: CSV Export query ──────────────────────────────────────

    @Query("SELECT * FROM transactions ORDER BY timestamp ASC, id ASC")
    suspend fun getAllOnce(): List<TransactionEntity>
}
