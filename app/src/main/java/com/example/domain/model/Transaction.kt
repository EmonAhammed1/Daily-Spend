package com.example.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Category(
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String,
    val budgetAmount: Double? = null,
    val isDefault: Boolean = false
) {
    companion object {
        val DEFAULT_EXPENSE_CATEGORIES = listOf(
            Category("cat_food", "Food", TransactionType.EXPENSE, "food", "#EF4444", 6000.0, true),
            Category("cat_transport", "Transport", TransactionType.EXPENSE, "transport", "#3B82F6", 3000.0, true),
            Category("cat_shopping", "Shopping", TransactionType.EXPENSE, "shopping", "#EC4899", 5000.0, true),
            Category("cat_bills", "Bills", TransactionType.EXPENSE, "bills", "#F59E0B", 4000.0, true),
            Category("cat_rent", "Rent", TransactionType.EXPENSE, "rent", "#8B5CF6", 15000.0, true),
            Category("cat_entertainment", "Entertainment", TransactionType.EXPENSE, "entertainment", "#10B981", 2000.0, true),
            Category("cat_health", "Health", TransactionType.EXPENSE, "health", "#06B6D4", 2500.0, true),
            Category("cat_education", "Education", TransactionType.EXPENSE, "education", "#6366F1", null, true),
            Category("cat_travel", "Travel", TransactionType.EXPENSE, "travel", "#14B8A6", null, true),
            Category("cat_groceries", "Groceries", TransactionType.EXPENSE, "groceries", "#84CC16", 6000.0, true),
            Category("cat_personal", "Personal", TransactionType.EXPENSE, "personal", "#D946EF", null, true),
            Category("cat_other_expense", "Other", TransactionType.EXPENSE, "other", "#64748B", null, true)
        )

        val DEFAULT_INCOME_CATEGORIES = listOf(
            Category("cat_salary", "Salary", TransactionType.INCOME, "salary", "#10B981", null, true),
            Category("cat_freelance", "Freelance", TransactionType.INCOME, "freelance", "#06B6D4", null, true),
            Category("cat_business", "Business", TransactionType.INCOME, "business", "#3B82F6", null, true),
            Category("cat_gift", "Gift", TransactionType.INCOME, "gift", "#EC4899", null, true),
            Category("cat_investment", "Investment", TransactionType.INCOME, "investment", "#8B5CF6", null, true),
            Category("cat_other_income", "Other", TransactionType.INCOME, "other", "#64748B", null, true)
        )

        val DEFAULT_PAYMENT_METHODS = listOf(
            "Cash",
            "Bank",
            "Card",
            "bKash",
            "Nagad",
            "Rocket",
            "Other"
        )
    }
}

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val paymentMethod: String = "Cash",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
)

data class BalanceSummary(
    val totalBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double
)

data class TodaySummary(
    val todaySpent: Double,
    val todayIncome: Double,
    val transactionCount: Int,
    val comparisonPercentage: Double?, // positive for increase, negative for decrease, null if no yesterday data
    val spentMoreThanYesterday: Boolean?
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val iconName: String,
    val colorHex: String,
    val totalAmount: Double,
    val percentage: Double,
    val transactionCount: Int,
    val budgetAmount: Double? = null
)

data class DailySpending(
    val date: LocalDate,
    val amount: Double,
    val dayLabel: String
)

data class MonthlyAnalytics(
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val dailyAverage: Double,
    val highestSpendingDay: LocalDate?,
    val highestDayAmount: Double,
    val topCategory: CategorySpending?,
    val transactionCount: Int
)

enum class DateFilterType(val displayName: String) {
    ALL("All Dates"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom Range")
}

data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null, // null for ALL
    val categoryId: String? = null,    // null for ALL
    val dateFilter: DateFilterType = DateFilterType.ALL,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val paymentMethod: String? = null  // null for ALL
)
