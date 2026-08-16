package com.example.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.CategorySpending
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.DailySpending
import com.example.domain.model.MonthlyAnalytics
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import com.example.utils.combine9
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnalyticsPeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class FinancialInsight(
    val title: String,
    val description: String,
    val iconName: String = "insight"
)

data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.MONTHLY,
    val selectedYearMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val categorySpendings: List<CategorySpending> = emptyList(),
    val weeklySpending: List<DailySpending> = emptyList(),
    val monthlyAnalytics: MonthlyAnalytics = MonthlyAnalytics(0.0, 0.0, 0.0, 0.0, null, 0.0, null, 0),
    val monthlyBudget: Double = 30000.0,
    val currency: CurrencyInfo = CurrencyInfo.DEFAULT,
    val insights: List<FinancialInsight> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val dailyTransactions: List<Transaction> = emptyList(),
    val dailyExpenseTotal: Double = 0.0,
    val dailyIncomeTotal: Double = 0.0
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.userPreferenceDao()
    )

    private val selectedPeriodState = MutableStateFlow(AnalyticsPeriod.MONTHLY)
    private val selectedYearMonthState = MutableStateFlow(YearMonth.now())
    private val selectedDateState = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<AnalyticsUiState> = combine9(
        selectedPeriodState,
        selectedYearMonthState,
        selectedDateState,
        selectedYearMonthState.flatMapLatest { ym -> repository.getCategorySpendingBreakdown(ym) },
        selectedYearMonthState.flatMapLatest { ym -> repository.getMonthlyAnalytics(ym) },
        repository.getWeeklyDailySpending(),
        repository.monthlyBudgetFlow,
        repository.currencyFlow,
        repository.allTransactions
    ) { period, yearMonth, date, catSpending, monthlyAnalytics, weeklyList, budget, currency, allTx ->
        // Daily calculations for selected date
        val dayTx = allTx.filter { it.date == date }
        val dayExpense = dayTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val dayIncome = dayTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        // Generate dynamic financial insights
        val insights = mutableListOf<FinancialInsight>()

        // Insight 1: Highest spending category
        if (monthlyAnalytics.topCategory != null && monthlyAnalytics.totalExpense > 0) {
            insights.add(
                FinancialInsight(
                    title = "Top Spending Category",
                    description = "${monthlyAnalytics.topCategory.categoryName} is your highest spending category this month (${CurrencyFormatter.format(monthlyAnalytics.topCategory.totalAmount, currency.symbol)})."
                )
            )
        }

        // Insight 2: Daily average
        if (monthlyAnalytics.dailyAverage > 0) {
            insights.add(
                FinancialInsight(
                    title = "Average Daily Spending",
                    description = "Your average daily spending is ${CurrencyFormatter.format(monthlyAnalytics.dailyAverage, currency.symbol)}."
                )
            )
        }

        // Insight 3: Highest spending day
        if (monthlyAnalytics.highestSpendingDay != null && monthlyAnalytics.highestDayAmount > 0) {
            val dayStr = monthlyAnalytics.highestSpendingDay.format(DateTimeFormatter.ofPattern("MMMM d"))
            insights.add(
                FinancialInsight(
                    title = "Peak Spending Day",
                    description = "Your highest spending day was $dayStr (${CurrencyFormatter.format(monthlyAnalytics.highestDayAmount, currency.symbol)})."
                )
            )
        }

        // Insight 4: Week comparison
        val today = LocalDate.now()
        val thisWeekExpense = allTx.filter {
            !it.date.isBefore(today.minusDays(6)) && it.type == TransactionType.EXPENSE
        }.sumOf { it.amount }
        val lastWeekExpense = allTx.filter {
            !it.date.isBefore(today.minusDays(13)) && it.date.isBefore(today.minusDays(6)) && it.type == TransactionType.EXPENSE
        }.sumOf { it.amount }

        if (thisWeekExpense > 0 && lastWeekExpense > 0) {
            val diff = thisWeekExpense - lastWeekExpense
            val pct = (diff / lastWeekExpense) * 100.0
            val formatted = String.format(Locale.US, "%.1f%%", kotlin.math.abs(pct))
            val action = if (diff >= 0) "more" else "less"
            insights.add(
                FinancialInsight(
                    title = "Weekly Comparison",
                    description = "You spent $formatted $action this week compared to last week."
                )
            )
        }

        AnalyticsUiState(
            selectedPeriod = period,
            selectedYearMonth = yearMonth,
            selectedDate = date,
            categorySpendings = catSpending,
            weeklySpending = weeklyList,
            monthlyAnalytics = monthlyAnalytics,
            monthlyBudget = budget,
            currency = currency,
            insights = insights,
            allTransactions = allTx,
            dailyTransactions = dayTx,
            dailyExpenseTotal = dayExpense,
            dailyIncomeTotal = dayIncome
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onPeriodSelected(period: AnalyticsPeriod) {
        selectedPeriodState.value = period
    }

    fun onPreviousMonth() {
        selectedYearMonthState.value = selectedYearMonthState.value.minusMonths(1)
    }

    fun onNextMonth() {
        selectedYearMonthState.value = selectedYearMonthState.value.plusMonths(1)
    }

    fun onDateSelected(date: LocalDate) {
        selectedDateState.value = date
    }

    fun onPreviousDay() {
        selectedDateState.value = selectedDateState.value.minusDays(1)
    }

    fun onNextDay() {
        selectedDateState.value = selectedDateState.value.plusDays(1)
    }
}
