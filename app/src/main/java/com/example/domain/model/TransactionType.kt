package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class StartOfWeek(val displayName: String, val dayOfWeekValue: Int) {
    SATURDAY("Saturday", 6),
    SUNDAY("Sunday", 7),
    MONDAY("Monday", 1)
}

enum class ThemePreference(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
) {
    companion object {
        val ALL_CURRENCIES = listOf(
            CurrencyInfo(code = "BDT", symbol = "৳", name = "Bangladeshi Taka (৳)"),
            CurrencyInfo(code = "USD", symbol = "$", name = "US Dollar ($)"),
            CurrencyInfo(code = "EUR", symbol = "€", name = "Euro (€)"),
            CurrencyInfo(code = "GBP", symbol = "£", name = "British Pound (£)"),
            CurrencyInfo(code = "INR", symbol = "₹", name = "Indian Rupee (₹)"),
            CurrencyInfo(code = "CAD", symbol = "CA$", name = "Canadian Dollar (CA$)"),
            CurrencyInfo(code = "AUD", symbol = "A$", name = "Australian Dollar (A$)"),
            CurrencyInfo(code = "SGD", symbol = "S$", name = "Singapore Dollar (S$)"),
            CurrencyInfo(code = "AED", symbol = "AED", name = "UAE Dirham (AED)"),
            CurrencyInfo(code = "MYR", symbol = "RM", name = "Malaysian Ringgit (RM)")
        )
        val DEFAULT = CurrencyInfo(code = "BDT", symbol = "৳", name = "Bangladeshi Taka (৳)")
    }
}
