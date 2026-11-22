package com.example.upitracker.data.category

import com.example.upitracker.data.local.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AutoCategorizerTest {

    private val emptyRules = emptyMap<String, String>()

    @Test
    fun merchantKeyNormalization_trimsLowersAndCollapsesSpaces() {
        assertEquals("swiggy food", AutoCategorizer.merchantKey("  Swiggy   Food  "))
        assertEquals("amazon", AutoCategorizer.merchantKey("AMAZON"))
        assertEquals("cafe coffee day", AutoCategorizer.merchantKey("Cafe   Coffee   Day"))
    }

    // ── Built-in groups: at least 2 matches + 1 token-length non-match (≤4 chars) ──

    @Test
    fun foodAndDining_matchesAndRejectsNonToken() {
        assertEquals(Category.FOOD_AND_DINING, AutoCategorizer.categorize("Swiggy Order", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.FOOD_AND_DINING, AutoCategorizer.categorize("Zomato Delivery", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.FOOD_AND_DINING, AutoCategorizer.categorize("Dominos Pizza", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun groceries_matches() {
        assertEquals(Category.GROCERIES, AutoCategorizer.categorize("Blinkit Commerce", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.GROCERIES, AutoCategorizer.categorize("BigBasket Online", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.GROCERIES, AutoCategorizer.categorize("Zepto Quick", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun fuel_matchesAndRejectsShortSubstring() {
        assertEquals(Category.FUEL, AutoCategorizer.categorize("IndianOil Petrol Pump", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.FUEL, AutoCategorizer.categorize("HPCL Station", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.FUEL, AutoCategorizer.categorize("HP Station", TransactionType.DEBIT, emptyRules))

        // "hp" is 2 chars: must equal whole token; "shipping" must NOT match "hp"
        assertNotEquals(Category.FUEL, AutoCategorizer.categorize("Shipping Services", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun transport_matchesAndRejectsShortSubstring() {
        assertEquals(Category.TRANSPORT, AutoCategorizer.categorize("Uber Rides", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.TRANSPORT, AutoCategorizer.categorize("Ola Cabs", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.TRANSPORT, AutoCategorizer.categorize("Rapido Bike", TransactionType.DEBIT, emptyRules))

        // "ola" is 3 chars: must NOT match "cola"
        assertNotEquals(Category.TRANSPORT, AutoCategorizer.categorize("Coca Cola Refreshment", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun recharge_matchesAndRejectsShortSubstring() {
        assertEquals(Category.RECHARGE, AutoCategorizer.categorize("Jio Prepaid Recharge", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.RECHARGE, AutoCategorizer.categorize("Airtel Mobile", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.RECHARGE, AutoCategorizer.categorize("Vi Bill Pay", TransactionType.DEBIT, emptyRules))

        // "vi" is 2 chars: must NOT match "vivek"
        assertNotEquals(Category.RECHARGE, AutoCategorizer.categorize("Vivek General Store", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun billsAndUtilities_matches() {
        assertEquals(Category.BILLS_AND_UTILITIES, AutoCategorizer.categorize("Torrent Power Limited", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.BILLS_AND_UTILITIES, AutoCategorizer.categorize("Gujarat Gas Company", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.BILLS_AND_UTILITIES, AutoCategorizer.categorize("Adani Electricity", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun entertainment_matches() {
        assertEquals(Category.ENTERTAINMENT, AutoCategorizer.categorize("Netflix Subscription", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.ENTERTAINMENT, AutoCategorizer.categorize("BookMyShow Tickets", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun health_matches() {
        assertEquals(Category.HEALTH, AutoCategorizer.categorize("Apollo Pharmacy", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.HEALTH, AutoCategorizer.categorize("Netmeds Health", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun education_matches() {
        assertEquals(Category.EDUCATION, AutoCategorizer.categorize("Udemy Online Courses", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.EDUCATION, AutoCategorizer.categorize("University Exam Fees", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun shopping_matches() {
        assertEquals(Category.SHOPPING, AutoCategorizer.categorize("Amazon Seller", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.SHOPPING, AutoCategorizer.categorize("Flipkart India", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun rentAndHousing_matches() {
        assertEquals(Category.RENT_AND_HOUSING, AutoCategorizer.categorize("House Rent Payment", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.RENT_AND_HOUSING, AutoCategorizer.categorize("Hostel Stay Accommodation", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.RENT_AND_HOUSING, AutoCategorizer.categorize("Apartment Maintenance", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun investments_matches() {
        assertEquals(Category.INVESTMENTS, AutoCategorizer.categorize("Zerodha Broking", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.INVESTMENTS, AutoCategorizer.categorize("Groww Invest", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun cash_matches() {
        assertEquals(Category.CASH, AutoCategorizer.categorize("ATM Cash Withdrawal", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.CASH, AutoCategorizer.categorize("Cash Advance", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun transfers_matches() {
        assertEquals(Category.TRANSFERS, AutoCategorizer.categorize("Paytm Wallet Topup", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.TRANSFERS, AutoCategorizer.categorize("Self Transfer Savings", TransactionType.DEBIT, emptyRules))
    }

    // ── Priority and Edge Case Tests ──

    @Test
    fun priority_userRuleOverridesKeywordMatch() {
        // "swiggy" normally maps to FOOD_AND_DINING
        val rules = mapOf("swiggy" to Category.SHOPPING.label)
        val result = AutoCategorizer.categorize("Swiggy", TransactionType.DEBIT, rules)
        assertEquals(Category.SHOPPING, result)
    }

    @Test
    fun priority_unknownOrBlank_givesUncategorizedEvenIfRulePassed() {
        val rules = mapOf("unknown" to Category.FOOD_AND_DINING.label)
        assertEquals(Category.UNCATEGORIZED, AutoCategorizer.categorize("Unknown", TransactionType.DEBIT, rules))
        assertEquals(Category.UNCATEGORIZED, AutoCategorizer.categorize("unknown", TransactionType.DEBIT, rules))
        assertEquals(Category.UNCATEGORIZED, AutoCategorizer.categorize("   ", TransactionType.DEBIT, rules))
    }

    @Test
    fun priority_tenDigitPhoneVpa_givesTransfers() {
        assertEquals(Category.TRANSFERS, AutoCategorizer.categorize("9876543210@paytm", TransactionType.DEBIT, emptyRules))
        assertEquals(Category.TRANSFERS, AutoCategorizer.categorize("Paid to 9123456780@upi", TransactionType.DEBIT, emptyRules))
        // 9 digits should NOT match transfers rule
        assertNotEquals(Category.TRANSFERS, AutoCategorizer.categorize("123456789@paytm", TransactionType.DEBIT, emptyRules))
    }

    @Test
    fun priority_incomeGroupAppliesOnlyToCredit() {
        // "Salary" in DEBIT should NOT be categorized as Income
        assertEquals(Category.UNCATEGORIZED, AutoCategorizer.categorize("Monthly Salary Disbursal", TransactionType.DEBIT, emptyRules))
        // "Salary" in CREDIT should be categorized as Income
        assertEquals(Category.INCOME, AutoCategorizer.categorize("Monthly Salary Disbursal", TransactionType.CREDIT, emptyRules))
        assertEquals(Category.INCOME, AutoCategorizer.categorize("Cashback Received", TransactionType.CREDIT, emptyRules))
        assertEquals(Category.INCOME, AutoCategorizer.categorize("Refund from store", TransactionType.CREDIT, emptyRules))
    }
}
