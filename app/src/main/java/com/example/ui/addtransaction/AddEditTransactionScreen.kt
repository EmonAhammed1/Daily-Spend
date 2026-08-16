package com.example.ui.addtransaction

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TransactionType
import com.example.ui.components.AppDatePickerDialog
import com.example.ui.components.AppTimePickerDialog
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.parseColorHex
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    viewModel: AddEditTransactionViewModel,
    initialType: String = "EXPENSE",
    txId: Long = 0L,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isDatePickerOpen by remember { mutableStateOf(false) }
    var isTimePickerOpen by remember { mutableStateOf(false) }
    var isDiscardDialogOpen by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialType, txId) {
        viewModel.initialize(initialType, txId)
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AddEditEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is AddEditEvent.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    // Intercept Back Press for Unsaved Changes
    BackHandler {
        if (uiState.hasUnsavedChanges) {
            isDiscardDialogOpen = true
        } else {
            onNavigateBack()
        }
    }

    if (isDiscardDialogOpen) {
        ConfirmDialog(
            title = "Discard Changes?",
            message = "You have unsaved changes. Are you sure you want to discard them?",
            confirmText = "Discard",
            dismissText = "Keep Editing",
            isDestructive = true,
            onConfirm = {
                isDiscardDialogOpen = false
                onNavigateBack()
            },
            onDismiss = { isDiscardDialogOpen = false }
        )
    }

    if (isDeleteDialogOpen) {
        ConfirmDialog(
            title = "Delete this transaction?",
            message = "This action cannot be undone. Are you sure you want to delete this transaction?",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                isDeleteDialogOpen = false
                viewModel.deleteCurrentTransaction()
            },
            onDismiss = { isDeleteDialogOpen = false }
        )
    }

    if (isDatePickerOpen) {
        AppDatePickerDialog(
            initialDate = uiState.date,
            onDateSelected = { viewModel.onDateSelected(it) },
            onDismiss = { isDatePickerOpen = false }
        )
    }

    if (isTimePickerOpen) {
        AppTimePickerDialog(
            initialTime = uiState.time,
            onTimeSelected = { viewModel.onTimeSelected(it) },
            onDismiss = { isTimePickerOpen = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasUnsavedChanges) {
                                isDiscardDialogOpen = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        IconButton(
                            onClick = { isDeleteDialogOpen = true },
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .testTag("add_edit_transaction_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Expense | Income Type Toggle
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Expense Button
                        val isExpense = uiState.type == TransactionType.EXPENSE
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isExpense) ExpenseRed else Color.Transparent,
                            shadowElevation = if (isExpense) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.onTypeChanged(TransactionType.EXPENSE) }
                                .testTag("type_toggle_expense")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Expense",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Income Button
                        val isIncome = uiState.type == TransactionType.INCOME
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isIncome) IncomeGreen else Color.Transparent,
                            shadowElevation = if (isIncome) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clickable { viewModel.onTypeChanged(TransactionType.INCOME) }
                                .testTag("type_toggle_income")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Income",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Amount Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.amountString,
                            onValueChange = { viewModel.onAmountChanged(it) },
                            placeholder = { Text("0.00", fontSize = 28.sp) },
                            prefix = {
                                Text(
                                    text = "${uiState.currency.symbol} ",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            isError = uiState.amountError != null,
                            supportingText = uiState.amountError?.let {
                                { Text(it, color = MaterialTheme.colorScheme.error) }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_amount_input")
                        )
                    }
                }
            }

            // 3. Category Selector
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.categoryError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.categoryError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableCategories.forEach { category ->
                            val isSelected = uiState.selectedCategory?.id == category.id
                            val catColor = parseColorHex(category.colorHex)

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) catColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(2.dp, catColor)
                                } else {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                },
                                modifier = Modifier
                                    .clickable { viewModel.onCategorySelected(category) }
                                    .testTag("category_chip_${category.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIcon(
                                        iconName = category.iconName,
                                        colorHex = category.colorHex,
                                        size = 28.dp,
                                        iconSize = 16.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) catColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Description (Optional)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Description (Optional)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.onDescriptionChanged(it) },
                        placeholder = { Text("e.g. Lunch with friends, groceries...") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transaction_description_input")
                    )
                }
            }

            // 5. Date and Time Pickers Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date selector
                    OutlinedButton(
                        onClick = { isDatePickerOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("date_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatRelativeDate(uiState.date),
                            maxLines = 1
                        )
                    }

                    // Time selector
                    OutlinedButton(
                        onClick = { isTimePickerOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("time_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatTime(uiState.time),
                            maxLines = 1
                        )
                    }
                }
            }

            // 6. Payment Method Selector
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.availablePaymentMethods.forEach { method ->
                            FilterChip(
                                selected = uiState.selectedPaymentMethod == method,
                                onClick = { viewModel.onPaymentMethodSelected(method) },
                                label = { Text(method) },
                                modifier = Modifier.testTag("payment_method_$method")
                            )
                        }
                    }
                }
            }

            // 7. Save Transaction Button
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.saveTransaction() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.primary else IncomeGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEditMode) "Save Changes" else "Save Transaction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
