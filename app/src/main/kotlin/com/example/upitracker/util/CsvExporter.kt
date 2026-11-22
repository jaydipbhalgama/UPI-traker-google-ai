package com.example.upitracker.util

import android.content.Context
import android.net.Uri
import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.domain.csv.CsvWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.ZoneId

object CsvExporter {

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext Result.failure(Exception("Could not open file output stream"))

            outputStream.use { os ->
                val writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
                writer.use { bw ->
                    // Write BOM for UTF-8 Excel compatibility
                    bw.write("\uFEFF")
                    CsvWriter.writeCsv(bw, transactions, zoneId)
                    bw.flush()
                }
            }
            Result.success(transactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
