package com.example.upitracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.upitracker.data.local.entity.MerchantRule
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: MerchantRule)

    @Query("SELECT * FROM merchant_rules")
    suspend fun getAll(): List<MerchantRule>

    @Query("SELECT * FROM merchant_rules")
    fun getAllFlow(): Flow<List<MerchantRule>>
}
