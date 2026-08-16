package com.example.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.Category
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.DateFilterType
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionFilter
import com.example.domain.model.TransactionType
import com.example.utils.combine6
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DateGroupedTransactions(
    val date: LocalDate,
    val dateLabel: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val transactions: List<Transaction>
)

data class TransactionsUiState(
    val filter: TransactionFilter = TransactionFilter(),
    val groupedTransactions: List<DateGroupedTransactions> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val currency: CurrencyInfo = CurrencyInfo.DEFAULT,
    val totalCount: Int = 0,
    val isFilterSheetOpen: Boolean = false,
    val activeFilterCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.userPreferenceDao()
    )

    private val filterState = MutableStateFlow(TransactionFilter())
    private val isFilterSheetOpenState = MutableStateFlow(false)

    val uiState: StateFlow<TransactionsUiState> = combine6(
        filterState.flatMapLatest { filter -> repository.getFilteredTransactions(filter) },
        filterState,
        repository.allCategories,
        repository.paymentMethodsFlow,
        repository.currencyFlow,
        isFilterSheetOpenState
    ) { filteredList, filter, categories, paymentMethods, currency, isSheetOpen ->
        // Group by date
        val groupedMap = filteredList.groupBy { it.date }
        val groupedList = groupedMap.map { (date, items) ->
            val expenseSum = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val incomeSum = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            DateGroupedTransactions(
                date = date,
                dateLabel = com.example.utils.DateUtils.formatRelativeDate(date),
                totalExpense = expenseSum,
                totalIncome = incomeSum,
                transactions = items
            )
        }.sortedByDescending { it.date }

        var activeFilters = 0
        if (filter.type != null) activeFilters++
        if (filter.categoryId != null) activeFilters++
        if (filter.dateFilter != DateFilterType.ALL) activeFilters++
        if (filter.paymentMethod != null) activeFilters++

        TransactionsUiState(
            filter = filter,
            groupedTransactions = groupedList,
            allCategories = categories,
            paymentMethods = paymentMethods,
            currency = currency,
            totalCount = filteredList.size,
            isFilterSheetOpen = isSheetOpen,
            activeFilterCount = activeFilters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun onSearchQueryChanged(query: String) {
        filterState.value = filterState.value.copy(query = query)
    }

    fun onFilterApplied(newFilter: TransactionFilter) {
        filterState.value = newFilter
    }

    fun onResetFilter() {
        filterState.value = TransactionFilter()
    }

    fun openFilterSheet() {
        isFilterSheetOpenState.value = true
    }

    fun closeFilterSheet() {
        isFilterSheetOpenState.value = false
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }
}
