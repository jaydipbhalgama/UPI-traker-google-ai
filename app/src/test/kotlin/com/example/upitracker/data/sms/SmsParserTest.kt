package com.example.upitracker.data.sms

import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmsParserTest {

    // ─── ORIGINAL TESTS (unchanged expectations) ──────────────────────────────

    // [SAMPLE 1] HDFC UPI Debit with VPA and merchant in brackets
    @Test
    fun sample1_hdfcUpiDebitWithVpaAndBracketMerchant() {
        val sender = "VM-HDFCBK"
        val body = "Sent Rs. 450.00 from HDFC Bank A/C *1234 to swiggy@icici (Swiggy Food) via UPI. Avl Bal: Rs 14,520.50. Ref 423156789012."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse valid HDFC debit", result)
        assertEquals(45000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("Swiggy Food", result?.merchant)
        assertEquals("HDFC", result?.bankName)
        assertEquals(1452050L, result?.balanceAfterPaise)
    }

    // [SAMPLE 2] SBI UPI Credit with VPA
    @Test
    fun sample2_sbiUpiCredit() {
        val sender = "AX-SBIUPI"
        val body = "Dear Customer, your A/C *5678 credited by Rs. 2,500.00 on 12-Mar-24 by UPI/john.doe@okaxis/Payment. Bal: Rs 25,000.00."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse valid SBI credit", result)
        assertEquals(250000L, result?.amountPaise)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals("SBI", result?.bankName)
        assertEquals(2500000L, result?.balanceAfterPaise)
    }

    // [SAMPLE 3] ICICI UPI Debit with to merchant and no brackets
    @Test
    fun sample3_iciciUpiDebit() {
        val sender = "JD-ICICIT-S"
        val body = "Dear Customer, Acct XX987 debited with INR 150.00 on 15-Mar-24. Info: UPI*chaiwala@paytm. Avl Bal is INR 5,230.10."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse valid ICICI debit", result)
        assertEquals(15000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("chaiwala@paytm", result?.merchant)
        assertEquals("ICICI", result?.bankName)
        assertEquals(523010L, result?.balanceAfterPaise)
    }

    // [SAMPLE 4] Axis UPI Debit with rupee symbol
    @Test
    fun sample4_axisUpiDebit() {
        val sender = "AD-AXISBK"
        val body = "Paid ₹85.00 via UPI to Metro Rail Services on 10-04-2024 from A/c no. XX3210. Available Balance: ₹3,410.00."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse valid Axis debit", result)
        assertEquals(8500L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("Metro Rail Services", result?.merchant)
        assertEquals("AXIS", result?.bankName)
        assertEquals(341000L, result?.balanceAfterPaise)
    }

    // [OTP SAMPLE]
    @Test
    fun otpSample_returnsNull() {
        val sender = "VM-HDFCBK"
        val body = "123456 is your secret OTP for transaction of Rs 1,500.00 via UPI. Do not share this OTP with anyone."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("OTP message must return null", result)
    }

    // [PROMO SAMPLE]
    @Test
    fun promoSample_returnsNull() {
        val sender = "BZ-HDFCBK"
        val body = "Get pre-approved Personal Loan up to Rs 5,00,000 with low interest rates! Apply now via UPI netbanking. Click here: bit.ly/bank"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("Promotional message must return null", result)
    }

    // Additional required test case: amount with commas
    @Test
    fun amountWithCommas_parsedAccuratelyToPaise() {
        val sender = "VM-HDFCBK"
        val body = "Your A/C *1234 is debited for Rs 1,250.50 via UPI to test@upi. Avl Bal: Rs 10,000.00."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(125050L, result?.amountPaise)
    }

    // Additional required test case: sender that is a phone number gives null
    @Test
    fun senderIsPhoneNumber_returnsNull() {
        val sender = "+919876543210"
        val body = "Sent Rs. 500 via UPI to friend@upi. Avl Bal: Rs 10,000."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("Plain phone number sender must return null", result)
    }

    // Additional required test case: "will be debited" gives null
    @Test
    fun willBeDebited_returnsNull() {
        val sender = "AX-SBIUPI"
        val body = "Rs 1,500.00 will be debited from your A/c *1234 for UPI autopay on 01-May-24."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("'will be debited' must return null", result)
    }

    // Additional required test case: failed transaction gives null
    @Test
    fun failedTransaction_returnsNull() {
        val sender = "VM-HDFCBK"
        val body = "UPI transaction of Rs 250.00 to shop@upi failed due to network issue."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("Failed transaction must return null", result)
    }

    // Additional required test case: credit message gives CREDIT
    @Test
    fun creditMessage_returnsCreditType() {
        val sender = "AX-KOTAKB"
        val body = "Rs. 1,000.00 credited to account XX4321 via UPI ref 123456. Bal: Rs 5,000."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals(100000L, result?.amountPaise)
        assertEquals("KOTAK", result?.bankName)
    }

    // Additional required test case: message with both debited and credited gives DEBIT
    @Test
    fun messageWithBothDebitedAndCredited_returnsDebitIfDebitAppearsFirst() {
        val sender = "VM-HDFCBK"
        val body = "Rs 500 debited from A/C *1234 and credited to X@upi. Avl Bal Rs 2000."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals(50000L, result?.amountPaise)
        assertEquals("X@upi", result?.merchant)
    }

    // Additional required test case: no balance in text gives balanceAfterPaise = null
    @Test
    fun noBalanceInText_returnsNullBalance() {
        val sender = "VM-HDFCBK"
        val body = "Sent Rs 200 via UPI to tea.stall@okicici. Ref 987654321."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(20000L, result?.amountPaise)
        assertNull(result?.balanceAfterPaise)
    }

    // ─── NEW TESTS (per spec) ─────────────────────────────────────────────────

    // Real Axis Bank UPI SMS with no currency prefix on amount
    @Test
    fun axisBankUpiDebit_noRsPrefix_parsesCorrectly() {
        val sender = "AX-AXISBK-S"
        val body = "Dear UPI user A/C X56 debited by 15000.00 on date 28Oct26 trf to ALPESH RAJUBHAI Refno 630100760006 If not u? call-1800111109 for other services-18001234-AXIS"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse Axis Bank UPI debit", result)
        assertEquals(1500000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("ALPESH RAJUBHAI", result?.merchant)
        assertNull(result?.balanceAfterPaise)
        assertEquals("AXIS", result?.bankName)
    }

    // Same message body with short sender "AXISBK" (no hyphen format) — must also parse
    @Test
    fun axisBankUpiDebit_shortSender_parsesCorrectly() {
        val sender = "AXISBK"
        val body = "Dear UPI user A/C X56 debited by 15000.00 on date 28Oct26 trf to ALPESH RAJUBHAI Refno 630100760006 If not u? call-1800111109 for other services-18001234-AXIS"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Short sender 'AXISBK' should be accepted", result)
        assertEquals(1500000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("ALPESH RAJUBHAI", result?.merchant)
        assertEquals("AXIS", result?.bankName)
    }

    // Phone number sender must still be rejected
    @Test
    fun phoneNumberSender_alwaysReturnsNull() {
        val sender = "+919876543210"
        val body = "Dear UPI user A/C X56 debited by 15000.00 on date 28Oct26 trf to ALPESH RAJUBHAI Refno 630100760006"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("Phone number sender '+919876543210' must return null", result)
    }

    // Assumed credit format (comment: "assumed format, replace with a real SMS")
    @Test
    fun axisBankUpiCredit_noRsPrefix_parsesCorrectly() {
        val sender = "AX-AXISBK-S"
        val body = "Dear UPI user A/C X56 credited by 500.00 on date 29Oct26 trf from RAHUL SHAH Refno 630100760007 If not u? call-1800111109 for other services-18001234-AXIS"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse Axis Bank UPI credit", result)
        assertEquals(50000L, result?.amountPaise)
        assertEquals(TransactionType.CREDIT, result?.type)
        assertEquals("RAHUL SHAH", result?.merchant)
    }

    // VPA in body: merchant = VPA (existing rule, explicit coverage)
    @Test
    fun rsAmountWithVpa_merchantIsVpa() {
        val sender = "VM-HDFCBK"
        val body = "Rs 500 debited from A/C XX1234 to abc@okaxis (UPI Ref 123456789012)"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull(result)
        assertEquals(50000L, result?.amountPaise)
        assertEquals(TransactionType.DEBIT, result?.type)
        assertEquals("abc@okaxis", result?.merchant)
    }

    // Only a 12-digit Refno and no valid amount → null
    @Test
    fun onlyRefnoNoValidAmount_returnsNull() {
        val sender = "VM-HDFCBK"
        val body = "UPI Ref 630100760006 via UPI."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("No valid amount → must return null", result)
    }

    // OTP / do-not-share message → null
    @Test
    fun otpDoNotShare_returnsNull() {
        val sender = "VM-HDFCBK"
        val body = "Your OTP is 123456. Do not share"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("OTP do-not-share message must return null", result)
    }

    // "will be debited tomorrow" → null
    @Test
    fun willBeDebitedTomorrow_returnsNull() {
        val sender = "AX-SBIUPI"
        val body = "Rs 500 will be debited tomorrow via UPI autopay."
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNull("'will be debited' must return null", result)
    }

    // Fallback amount with commas and decimal, merchant cut at "On"
    @Test
    fun fallbackAmountWithCommasAndDecimal_merchantCutAtOn() {
        val sender = "AX-AXISBK-S"
        val body = "debited by 1,250.50 trf to Cafe Mocha On 12 Oct"
        val timestamp = 1710000000000L

        val result = SmsParser.parse(sender, body, timestamp)
        assertNotNull("Should parse fallback amount with commas+decimal", result)
        assertEquals(125050L, result?.amountPaise)
        assertEquals("Cafe Mocha", result?.merchant)
    }
}
