package com.example.upitracker.data.local

import androidx.room.TypeConverter
import com.example.upitracker.data.local.entity.TransactionType

/**
 * Room type converters for non-primitive types in entities.
 */
class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String {
        return type.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return runCatching { TransactionType.valueOf(value) }
            .getOrDefault(TransactionType.DEBIT)
    }
}
