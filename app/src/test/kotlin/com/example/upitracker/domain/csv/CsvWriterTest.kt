package com.example.upitracker.domain.csv

import com.example.upitracker.data.local.entity.Transaction
import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class CsvWriterTest {

    @Test
    fun writeCsv_verifiesHeaderAndCrlfLineEndings() {
        val out = StringBuilder()
        CsvWriter.writeCsv(out, emptyList(), ZoneId.of("Asia/Kolkata"))

        val expectedHeader = "Date,Time,Type,Amount (INR),Merchant,Category,Note,Bank,Balance After (INR)\r\n"
        assertEquals(expectedHeader, out.toString())
    }

    @Test
    fun writeCsv_verifiesExactFullRowAndFieldFormatting() {
        // 2026-10-28 23:45:00 in Asia/Kolkata (epoch: 1793211300000L)
        val zone = ZoneId.of("Asia/Kolkata")
        val timestamp = Instant.parse("2026-10-28T18:15:00Z").toEpochMilli() // 18:15 UTC = 23:45 IST

        val tx = Transaction(
            id = 1L,
            amountPaise = 15000L,
            type = TransactionType.DEBIT,
            merchant = "Sample Cafe",
            category = "Food & Dining",
            timestamp = timestamp,
            bankName = "HDFC",
            balanceAfterPaise = 4520000L,
            smsHash = "hash1",
            note = "Team lunch"
        )

        val out = StringBuilder()
        CsvWriter.writeCsv(out, listOf(tx), zone)

        val lines = out.toString().split("\r\n")
        assertEquals(3, lines.size) // header, 1 row, trailing empty due to final \r\n
        val row = lines[1]
        val expectedRow = "2026-10-28,23:45:00,Debit,150.00,Sample Cafe,Food & Dining,Team lunch,HDFC,45200.00"
        assertEquals(expectedRow, row)
    }

    @Test
    fun writeCsv_verifiesFormulaProtectionAndQuotes() {
        val zone = ZoneId.of("UTC")
        val timestamp = 1700000000000L

        // Merchant with formula =HYPERLINK("x")
        val t1 = Transaction(
            amountPaise = 25000L,
            type = TransactionType.DEBIT,
            merchant = "=HYPERLINK(\"x\")",
            category = "Other",
            timestamp = timestamp,
            smsHash = "h1"
        )

        // Merchant with comma "Cafe, Bar" and note with newline
        val t2 = Transaction(
            amountPaise = 1000L,
            type = TransactionType.CREDIT,
            merchant = "Cafe, Bar",
            category = "Other",
            timestamp = timestamp,
            smsHash = "h2",
            note = "Line 1\nLine 2",
            bankName = null,
            balanceAfterPaise = -1250L // Negative balance -12.50
        )

        val out = StringBuilder()
        CsvWriter.writeCsv(out, listOf(t1, t2), zone)
        val csv = out.toString()

        // Merchant formula prefix and double quotes: '=HYPERLINK("x") -> "'=HYPERLINK(""x"")"
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\")\""))

        // Comma in merchant: "Cafe, Bar"
        assertTrue(csv.contains("\"Cafe, Bar\""))

        // Newline in note: "Line 1\nLine 2"
        assertTrue(csv.contains("\"Line 1\nLine 2\""))

        // Null note and null bank give empty fields for t1: ,Other,,, (empty note, empty bank, empty balance)
        assertTrue(csv.contains(",Other,,,\r\n"))
        // And negative balance "-12.50" for t2
        assertTrue(csv.contains("-12.50\r\n"))
    }

    @Test
    fun writeCsv_verifiesTimeZoneNearMidnight() {
        // 2026-10-28T18:45:00Z in UTC is 2026-10-29 00:15:00 in Asia/Kolkata
        val timestamp = Instant.parse("2026-10-28T18:45:00Z").toEpochMilli()
        val tx = Transaction(
            amountPaise = 5000L,
            type = TransactionType.DEBIT,
            merchant = "Midnight Cafe",
            category = "Other",
            timestamp = timestamp,
            smsHash = "h3"
        )

        val out = StringBuilder()
        CsvWriter.writeCsv(out, listOf(tx), ZoneId.of("Asia/Kolkata"))
        val row = out.toString().split("\r\n")[1]

        assertTrue(row.startsWith("2026-10-29,00:15:00"))
    }
}
