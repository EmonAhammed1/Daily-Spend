package com.example.ui.addtransaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.Category
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddEditTransactionUiState(
    val isEditMode: Boolean = false,
    val transactionId: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountString: String = "",
    val selectedCategory: Category? = null,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val time: LocalTime = LocalTime.now(),
    val selectedPaymentMethod: String = "Cash",
    val availableCategories: List<Category> = emptyList(),
    val availablePaymentMethods: List<String> = emptyList(),
    val currency: CurrencyInfo = CurrencyInfo.DEFAULT,
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSavedSuccessfully: Boolean = false,
    val hasUnsavedChanges: Boolean = false
)

sealed class AddEditEvent {
    data class ShowSnackbar(val message: String) : AddEditEvent()
    object NavigateBack : AddEditEvent()
}

class AddEditTransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.userPreferenceDao()
    )

    private val _uiState = MutableStateFlow(AddEditTransactionUiState())
    val uiState: StateFlow<AddEditTransactionUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<AddEditEvent>()
    val eventFlow: SharedFlow<AddEditEvent> = _eventFlow.asSharedFlow()

    private var initialAmount: String = ""
    private var initialDesc: String = ""
    private var initialCatId: String? = null

    fun initialize(initialType: String, txId: Long) {
        viewModelScope.launch {
            val currency = repository.currencyFlow.first()
            val paymentMethods = repository.paymentMethodsFlow.first()
            val categories = repository.allCategories.first()

            val typeEnum = if (initialType.equals("INCOME", ignoreCase = true)) {
                TransactionType.INCOME
            } else {
                TransactionType.EXPENSE
            }

            if (txId > 0L) {
                // Edit Mode
                val existingTx = repository.getTransactionByIdDirect(txId)
                if (existingTx != null) {
                    val matchingCategory = categories.find { it.id == existingTx.categoryId }
                        ?: Category(
                            id = existingTx.categoryId,
                            name = existingTx.categoryName,
                            type = existingTx.type,
                            iconName = "other",
                            colorHex = "#64748B"
                        )

                    initialAmount = if (existingTx.amount % 1.0 == 0.0) {
                        existingTx.amount.toInt().toString()
                    } else {
                        existingTx.amount.toString()
                    }
                    initialDesc = existingTx.description
                    initialCatId = existingTx.categoryId

                    _uiState.update {
                        it.copy(
                            isEditMode = true,
                            transactionId = existingTx.id,
                            type = existingTx.type,
                            amountString = initialAmount,
                            selectedCategory = matchingCategory,
                            description = existingTx.description,
                            date = existingTx.date,
                            time = existingTx.time,
                            selectedPaymentMethod = existingTx.paymentMethod,
                            availableCategories = categories.filter { c -> c.type == existingTx.type },
                            availablePaymentMethods = paymentMethods,
                            currency = currency,
                            hasUnsavedChanges = false
                        )
                    }
                    return@launch
                }
            }

            // Add Mode
            val defaultCategory = categories.firstOrNull { it.type == typeEnum }
            _uiState.update {
                it.copy(
                    isEditMode = false,
                    transactionId = 0L,
                    type = typeEnum,
                    amountString = "",
                    selectedCategory = defaultCategory,
                    description = "",
                    date = LocalDate.now(),
                    time = LocalTime.now(),
                    selectedPaymentMethod = paymentMethods.firstOrNull() ?: "Cash",
                    availableCategories = categories.filter { c -> c.type == typeEnum },
                    availablePaymentMethods = paymentMethods,
                    currency = currency,
                    hasUnsavedChanges = false
                )
            }
        }
    }

    fun onTypeChanged(newType: TransactionType) {
        viewModelScope.launch {
            val categories = repository.allCategories.first()
            val filtered = categories.filter { it.type == newType }
            _uiState.update {
                it.copy(
                    type = newType,
                    availableCategories = filtered,
                    selectedCategory = filtered.firstOrNull(),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    fun onAmountChanged(newAmount: String) {
        // Allow digits and single decimal point
        val clean = newAmount.filter { it.isDigit() || it == '.' }
        _uiState.update {
            it.copy(
                amountString = clean,
                amountError = null,
                hasUnsavedChanges = true
            )
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update {
            it.copy(
                selectedCategory = category,
                categoryError = null,
                hasUnsavedChanges = true
            )
        }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update {
            it.copy(
                description = desc,
                hasUnsavedChanges = true
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update {
            it.copy(
                date = date,
                hasUnsavedChanges = true
            )
        }
    }

    fun onTimeSelected(time: LocalTime) {
        _uiState.update {
            it.copy(
                time = time,
                hasUnsavedChanges = true
            )
        }
    }

    fun onPaymentMethodSelected(method: String) {
        _uiState.update {
            it.copy(
                selectedPaymentMethod = method,
                hasUnsavedChanges = true
            )
        }
    }

    fun saveTransaction() {
        val currentState = _uiState.value
        val amount = currentState.amountString.toDoubleOrNull()

        if (amount == null || amount <= 0.0) {
            _uiState.update { it.copy(amountError = "Please enter a valid amount greater than 0") }
            return
        }

        if (currentState.selectedCategory == null) {
            _uiState.update { it.copy(categoryError = "Please select a category") }
            return
        }

        viewModelScope.launch {
            val transaction = Transaction(
                id = if (currentState.isEditMode) currentState.transactionId else 0L,
                type = currentState.type,
                amount = amount,
                categoryId = currentState.selectedCategory.id,
                categoryName = currentState.selectedCategory.name,
                description = currentState.description.trim(),
                date = currentState.date,
                time = currentState.time,
                paymentMethod = currentState.selectedPaymentMethod,
                updatedAt = System.currentTimeMillis()
            )

            if (currentState.isEditMode) {
                repository.updateTransaction(transaction)
                _eventFlow.emit(AddEditEvent.ShowSnackbar("Transaction updated successfully"))
            } else {
                repository.insertTransaction(transaction)
                val typeName = if (currentState.type == TransactionType.EXPENSE) "Expense" else "Income"
                _eventFlow.emit(AddEditEvent.ShowSnackbar("$typeName added successfully"))
            }

            _eventFlow.emit(AddEditEvent.NavigateBack)
        }
    }

    fun deleteCurrentTransaction() {
        val currentState = _uiState.value
        if (currentState.isEditMode && currentState.transactionId > 0L) {
            viewModelScope.launch {
                repository.deleteTransactionById(currentState.transactionId)
                _eventFlow.emit(AddEditEvent.ShowSnackbar("Transaction deleted"))
                _eventFlow.emit(AddEditEvent.NavigateBack)
            }
        }
    }
}
