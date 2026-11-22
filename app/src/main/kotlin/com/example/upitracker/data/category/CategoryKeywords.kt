package com.example.upitracker.data.category

/**
 * Keyword groups used by [AutoCategorizer].
 *
 * Edit these lists to customise matching for your own merchants.
 * Rules:
 *  - Keywords of 4 characters or fewer must exactly equal a whole token
 *    (token = split on non-alphanumeric characters).
 *  - Longer keywords (5+) or keywords containing a space match as a substring
 *    of the full merchant key.
 * Groups are checked in the order they appear in [KEYWORD_RULES].
 */
object CategoryKeywords {

    // ────────────────────────────────────────────────────────────────
    // MY MERCHANTS — add your own merchant-key → Category mappings here.
    // Format: Pair("merchant key (trimmed lowercase)", Category)
    // These are checked BEFORE all keyword groups.
    // ────────────────────────────────────────────────────────────────
    val MY_MERCHANT_RULES: List<Pair<String, Category>> = listOf(
        // Example: Pair("my local grocery", Category.GROCERIES),
    )

    val FOOD_AND_DINING = listOf(
        "swiggy", "zomato", "dominos", "mcdonald", "kfc", "starbucks",
        "chaayos", "burger", "pizza", "cafe", "restaurant", "bakery",
        "sweets", "juice", "chai", "dosa", "kitchen", "eatery", "dhaba",
        "canteen", "mess", "fassos", "eatsure"
    )

    val GROCERIES = listOf(
        "bigbasket", "blinkit", "zepto", "instamart", "jiomart", "dmart",
        "grocery", "kirana", "supermarket", "mart", "vegetables", "fruits",
        "milk", "dairy"
    )

    val FUEL = listOf(
        "petrol", "fuel", "bpcl", "iocl", "indianoil", "hpcl", "nayara",
        "shell", "hp"
    )

    val TRANSPORT = listOf(
        "uber", "ola", "rapido", "metro", "gsrtc", "irctc", "redbus",
        "abhibus", "fastag", "parking", "blusmart", "bus", "cab", "auto",
        "railway"
    )

    val RECHARGE = listOf(
        "recharge", "jio", "airtel", "bsnl", "vi", "dth", "tataplay",
        "tata play"
    )

    val BILLS_AND_UTILITIES = listOf(
        "torrent power", "adani electricity", "dgvcl", "ugvcl", "pgvcl",
        "mgvcl", "electricity", "gujarat gas", "gas", "water", "broadband",
        "fibernet", "postpaid", "bill", "insurance", "lic"
    )

    val ENTERTAINMENT = listOf(
        "netflix", "spotify", "hotstar", "prime video", "youtube",
        "bookmyshow", "pvr", "inox", "gaming", "steam", "google play",
        "sonyliv", "zee5"
    )

    val HEALTH = listOf(
        "apollo", "medplus", "pharmeasy", "1mg", "netmeds", "pharmacy",
        "medical", "hospital", "clinic", "diagnostic", "lab", "doctor"
    )

    val EDUCATION = listOf(
        "college", "university", "school", "tuition", "udemy", "coursera",
        "exam", "fees", "library", "xerox", "stationery", "books", "nptel"
    )

    val SHOPPING = listOf(
        "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
        "decathlon", "croma", "reliance digital", "store", "fashion",
        "mobile"
    )

    val RENT_AND_HOUSING = listOf(
        "rent", "hostel", "landlord", "pg", "maintenance"
    )

    val INVESTMENTS = listOf(
        "zerodha", "groww", "upstox", "kuvera", "indmoney",
        "mutual fund", "sip"
    )

    val CASH = listOf("atm", "cash")

    val TRANSFERS = listOf("wallet", "add money", "self transfer")

    /** Applies ONLY when TransactionType == CREDIT */
    val INCOME = listOf(
        "salary", "stipend", "interest", "cashback", "refund"
    )

    /**
     * Ordered list of (keyword list, Category) pairs used by [AutoCategorizer].
     * Income is excluded here; it is handled separately with a type check.
     */
    val KEYWORD_RULES: List<Pair<List<String>, Category>> = listOf(
        FOOD_AND_DINING to Category.FOOD_AND_DINING,
        GROCERIES       to Category.GROCERIES,
        FUEL            to Category.FUEL,
        TRANSPORT       to Category.TRANSPORT,
        RECHARGE        to Category.RECHARGE,
        BILLS_AND_UTILITIES to Category.BILLS_AND_UTILITIES,
        ENTERTAINMENT   to Category.ENTERTAINMENT,
        HEALTH          to Category.HEALTH,
        EDUCATION       to Category.EDUCATION,
        SHOPPING        to Category.SHOPPING,
        RENT_AND_HOUSING to Category.RENT_AND_HOUSING,
        INVESTMENTS     to Category.INVESTMENTS,
        CASH            to Category.CASH,
        TRANSFERS       to Category.TRANSFERS,
    )
}
