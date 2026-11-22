package com.example.upitracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import com.example.upitracker.data.repository.TransactionRepository
import com.example.upitracker.data.sms.SmsSyncManager
import com.example.upitracker.data.sms.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val smsSyncManager: SmsSyncManager
) : ViewModel() {

    val transactionCount: StateFlow<Int> = repository.getCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val allTransactions: StateFlow<List<Transaction>> = repository.getAllTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    fun syncSms() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = smsSyncManager.syncAll()
                _syncResult.value = result
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addSampleTransaction() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val sample = Transaction(
                amountPaise = 15000L, // Rs 150.00
                type = TransactionType.DEBIT,
                merchant = "Sample Cafe",
                category = "Food & Drinks",
                timestamp = now,
                bankName = "Sample Bank",
                balanceAfterPaise = 500000L, // Rs 5000.00
                smsHash = "sample_hash_${UUID.randomUUID()}",
                note = "Test sample transaction"
            )
            repository.insert(sample)
        }
    }

    companion object {
        fun provideFactory(
            repository: TransactionRepository,
            smsSyncManager: SmsSyncManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
                        return TransactionViewModel(repository, smsSyncManager) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
