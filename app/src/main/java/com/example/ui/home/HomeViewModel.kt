package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.BalanceSummary
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.DailySpending
import com.example.domain.model.TodaySummary
import com.example.domain.model.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val balanceSummary: BalanceSummary = BalanceSummary(0.0, 0.0, 0.0),
    val todaySummary: TodaySummary = TodaySummary(0.0, 0.0, 0, null, null),
    val recentTransactions: List<Transaction> = emptyList(),
    val weeklySpending: List<DailySpending> = emptyList(),
    val currency: CurrencyInfo = CurrencyInfo.DEFAULT,
    val isLoading: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.userPreferenceDao()
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        repository.balanceSummary,
        repository.todaySummary,
        repository.getRecentTransactions(5),
        repository.getWeeklyDailySpending(),
        repository.currencyFlow
    ) { balance, today, recent, weekly, currency ->
        HomeUiState(
            balanceSummary = balance,
            todaySummary = today,
            recentTransactions = recent,
            weeklySpending = weekly,
            currency = currency,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )
}
