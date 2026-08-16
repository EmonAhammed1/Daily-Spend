package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Category
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.ThemePreference
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ConfirmDialog
import com.example.ui.components.parseColorHex
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WarningAmber
import com.example.utils.CurrencyFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToCategoryManagement: () -> Unit = {},
    onNavigateToBudgetSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isCurrencyDialogOpen by remember { mutableStateOf(false) }
    var isThemeDialogOpen by remember { mutableStateOf(false) }
    var isBudgetDialogOpen by remember { mutableStateOf(false) }
    var isCategoryDialogOpen by remember { mutableStateOf(false) }
    var isClearSampleDialogOpen by remember { mutableStateOf(false) }
    var isClearAllDialogOpen by remember { mutableStateOf(false) }
    var isRestoreDialogOpen by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var isBackupResultDialogOpen by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    // GitHub Repo Configuration Dialog state
    var isRepoDialogOpen by remember { mutableStateOf(false) }
    var repoInput by remember { mutableStateOf("") }

    // Selected category for editing individual budget
    var editingCategoryBudget by remember { mutableStateOf<Category?>(null) }
    var categoryBudgetInput by remember { mutableStateOf("") }

    // Category Creation Dialog
    var isAddCategoryDialogOpen by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var newCategoryColor by remember { mutableStateOf("#EF4444") }
    var newCategoryIcon by remember { mutableStateOf("food") }

    // Budget input state
    var monthlyBudgetInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.monthlyBudget) {
        if (monthlyBudgetInput.isEmpty() || !isBudgetDialogOpen) {
            monthlyBudgetInput = if (uiState.monthlyBudget % 1.0 == 0.0) {
                uiState.monthlyBudget.toLong().toString()
            } else {
                uiState.monthlyBudget.toString()
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.RequestInstallPermission -> {
                    // Handled automatically via GitHubUpdateManager / System Intent
                }
            }
        }
    }

    // Currency Selection Dialog
    if (isCurrencyDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCurrencyDialogOpen = false },
            title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(CurrencyInfo.ALL_CURRENCIES.size) { index ->
                        val curr = CurrencyInfo.ALL_CURRENCIES[index]
                        val isSelected = uiState.currency.code == curr.code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateCurrency(curr)
                                    isCurrencyDialogOpen = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateCurrency(curr)
                                    isCurrencyDialogOpen = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${curr.name} (${curr.symbol})",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isCurrencyDialogOpen = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Theme Selection Dialog
    if (isThemeDialogOpen) {
        AlertDialog(
            onDismissRequest = { isThemeDialogOpen = false },
            title = { Text("Select Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemePreference.values().forEach { theme ->
                        val isSelected = uiState.themePreference == theme
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateTheme(theme)
                                    isThemeDialogOpen = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.updateTheme(theme)
                                    isThemeDialogOpen = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isThemeDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Budget Settings Dialog
    if (isBudgetDialogOpen) {
        AlertDialog(
            onDismissRequest = { isBudgetDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Budget Settings", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                ) {
                    Text(
                        text = "Monthly Spending Budget",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set your total monthly budget to track spending limits.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = monthlyBudgetInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                monthlyBudgetInput = newValue
                            }
                        },
                        label = { Text("Monthly Budget Amount") },
                        prefix = {
                            Text(
                                text = "${uiState.currency.symbol} ",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (monthlyBudgetInput.isNotEmpty()) {
                                IconButton(onClick = { monthlyBudgetInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_monthly_budget")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Preset Chips
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10000.0, 20000.0, 30000.0, 50000.0, 75000.0, 100000.0).forEach { preset ->
                            val isCurrent = (monthlyBudgetInput.toDoubleOrNull() ?: 0.0) == preset
                            AssistChip(
                                onClick = {
                                    monthlyBudgetInput = preset.toLong().toString()
                                },
                                label = {
                                    Text(
                                        text = "${uiState.currency.symbol}${if (preset >= 1000) "${(preset / 1000).toInt()}k" else preset.toInt().toString()}",
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Quick Adjust Chips (+1k, +5k, +10k)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1000.0, 5000.0, 10000.0).forEach { addAmount ->
                            AssistChip(
                                onClick = {
                                    val current = monthlyBudgetInput.toDoubleOrNull() ?: 0.0
                                    val updated = current + addAmount
                                    monthlyBudgetInput = updated.toLong().toString()
                                },
                                label = { Text("+${addAmount.toLong()}") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Budgets Header
                    Text(
                        text = "Category Budgets (Optional)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap a category below to set or adjust its specific limit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val expenseCategories = uiState.allCategories.filter { it.type == TransactionType.EXPENSE }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(expenseCategories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .clickable {
                                        editingCategoryBudget = category
                                        categoryBudgetInput = category.budgetAmount?.let {
                                            if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                                        } ?: ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIcon(
                                        iconName = category.iconName,
                                        colorHex = category.colorHex,
                                        size = 32.dp,
                                        iconSize = 16.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (category.budgetAmount != null && category.budgetAmount > 0) {
                                        Text(
                                            text = CurrencyFormatter.format(category.budgetAmount, uiState.currency.symbol),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "No limit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit budget",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = monthlyBudgetInput.toDoubleOrNull()
                        if (amount != null && amount >= 0) {
                            viewModel.updateMonthlyBudget(amount)
                            isBudgetDialogOpen = false
                        } else {
                            Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { isBudgetDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Category Budget Quick Edit Dialog
    if (editingCategoryBudget != null) {
        val cat = editingCategoryBudget!!
        AlertDialog(
            onDismissRequest = { editingCategoryBudget = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(
                        iconName = cat.iconName,
                        colorHex = cat.colorHex,
                        size = 36.dp,
                        iconSize = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("${cat.name} Budget", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Set a monthly spending limit for ${cat.name}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = categoryBudgetInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                categoryBudgetInput = newValue
                            }
                        },
                        label = { Text("Budget Limit") },
                        prefix = { Text("${uiState.currency.symbol} ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(2000.0, 5000.0, 10000.0).forEach { preset ->
                            AssistChip(
                                onClick = { categoryBudgetInput = preset.toLong().toString() },
                                label = { Text("${uiState.currency.symbol}${preset.toInt()}") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = categoryBudgetInput.toDoubleOrNull()
                        viewModel.updateCategoryBudget(cat.id, amount)
                        editingCategoryBudget = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                if (cat.budgetAmount != null) {
                    TextButton(
                        onClick = {
                            viewModel.updateCategoryBudget(cat.id, null)
                            editingCategoryBudget = null
                        }
                    ) {
                        Text("Remove Limit", color = ExpenseRed)
                    }
                } else {
                    TextButton(onClick = { editingCategoryBudget = null }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Manage Categories Dialog
    if (isCategoryDialogOpen) {
        var selectedCategoryTab by remember { mutableStateOf(TransactionType.EXPENSE) }

        AlertDialog(
            onDismissRequest = { isCategoryDialogOpen = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Categories", fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = {
                            newCategoryName = ""
                            newCategoryType = selectedCategoryTab
                            newCategoryColor = if (selectedCategoryTab == TransactionType.EXPENSE) "#EF4444" else "#10B981"
                            newCategoryIcon = if (selectedCategoryTab == TransactionType.EXPENSE) "shopping" else "salary"
                            isAddCategoryDialogOpen = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Category",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    TabRow(
                        selectedTabIndex = if (selectedCategoryTab == TransactionType.EXPENSE) 0 else 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedCategoryTab == TransactionType.EXPENSE,
                            onClick = { selectedCategoryTab = TransactionType.EXPENSE },
                            text = { Text("Expenses") }
                        )
                        Tab(
                            selected = selectedCategoryTab == TransactionType.INCOME,
                            onClick = { selectedCategoryTab = TransactionType.INCOME },
                            text = { Text("Income") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val filteredList = uiState.allCategories.filter { it.type == selectedCategoryTab }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryIcon(
                                        iconName = category.iconName,
                                        colorHex = category.colorHex,
                                        size = 36.dp,
                                        iconSize = 18.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        if (category.budgetAmount != null && category.budgetAmount > 0) {
                                            Text(
                                                text = "Budget: ${CurrencyFormatter.format(category.budgetAmount, uiState.currency.symbol)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (!category.isDefault) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCategory(category.id)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Category",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isCategoryDialogOpen = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Add Custom Category Dialog
    if (isAddCategoryDialogOpen) {
        val colorOptions = listOf(
            "#EF4444", "#F59E0B", "#10B981", "#06B6D4", "#3B82F6",
            "#6366F1", "#8B5CF6", "#EC4899", "#D946EF", "#84CC16"
        )
        val iconOptions = listOf(
            "food", "transport", "shopping", "bills", "rent",
            "entertainment", "health", "education", "travel", "groceries",
            "personal", "salary", "freelance", "business", "gift", "investment"
        )

        AlertDialog(
            onDismissRequest = { isAddCategoryDialogOpen = false },
            title = { Text("New Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose Color:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { colorHex ->
                            val color = parseColorHex(colorHex)
                            val isSelected = newCategoryColor.equals(colorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { newCategoryColor = colorHex }
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Choose Icon:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { iconName ->
                            val isSelected = newCategoryIcon == iconName
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { newCategoryIcon = iconName }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    iconName = iconName,
                                    colorHex = newCategoryColor,
                                    size = 28.dp,
                                    iconSize = 18.dp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCustomCategory(
                                name = newCategoryName.trim(),
                                type = newCategoryType,
                                iconName = newCategoryIcon,
                                colorHex = newCategoryColor,
                                budget = null
                            )
                            isAddCategoryDialogOpen = false
                        } else {
                            Toast.makeText(context, "Please enter category name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddCategoryDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirm Clear Sample Data Dialog
    if (isClearSampleDialogOpen) {
        ConfirmDialog(
            title = "Remove Sample Data?",
            message = "This will remove all demo and sample transactions from your records.",
            confirmText = "Remove",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                isClearSampleDialogOpen = false
                viewModel.clearSampleData()
            },
            onDismiss = { isClearSampleDialogOpen = false }
        )
    }

    // Confirm Clear All Data Dialog
    if (isClearAllDialogOpen) {
        ConfirmDialog(
            title = "Clear All Transactions?",
            message = "This will permanently delete all ${uiState.transactionCount} transactions. This action cannot be undone!",
            confirmText = "Clear Everything",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                isClearAllDialogOpen = false
                viewModel.clearAllData()
            },
            onDismiss = { isClearAllDialogOpen = false }
        )
    }

    // Backup Result Dialog
    if (isBackupResultDialogOpen) {
        AlertDialog(
            onDismissRequest = { isBackupResultDialogOpen = false },
            title = { Text("Backup JSON Generated", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your backup JSON was created successfully. You can copy and save it.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = backupJsonText.take(400) + if (backupJsonText.length > 400) "..." else "",
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "DailySpend JSON Backup")
                            putExtra(android.content.Intent.EXTRA_TEXT, backupJsonText)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share JSON Backup"))
                        isBackupResultDialogOpen = false
                    }
                ) {
                    Text("Share JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { isBackupResultDialogOpen = false }) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Restore Dialog
    if (isRestoreDialogOpen) {
        AlertDialog(
            onDismissRequest = { isRestoreDialogOpen = false },
            title = { Text("Restore From JSON", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste your DailySpend JSON backup text below to restore your transactions:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        placeholder = { Text("Paste JSON here...") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            viewModel.restoreBackup(context, restoreJsonInput)
                            isRestoreDialogOpen = false
                            restoreJsonInput = ""
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRestoreDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // GitHub Repository Configuration Dialog
    if (isRepoDialogOpen) {
        AlertDialog(
            onDismissRequest = { isRepoDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GitHub Repository", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your GitHub username and repository in the format 'username/repo' (e.g. royanahmedemon3/DailySpend):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = repoInput,
                        onValueChange = { repoInput = it },
                        label = { Text("GitHub Repo (owner/repo)") },
                        placeholder = { Text("royanahmedemon3/DailySpend") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (repoInput.isNotBlank()) {
                            viewModel.updateGitHubRepo(repoInput.trim())
                            isRepoDialogOpen = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Repo")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRepoDialogOpen = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Title
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 1. General & Preferences
        item {
            SettingsSection(title = "General Preferences") {
                // Currency
                SettingsRow(
                    icon = Icons.Default.AttachMoney,
                    iconColor = PrimaryBlue,
                    title = "Currency",
                    subtitle = "${uiState.currency.name} (${uiState.currency.symbol})",
                    onClick = { isCurrencyDialogOpen = true },
                    testTag = "settings_currency_row"
                )

                // Theme
                SettingsRow(
                    icon = Icons.Default.ColorLens,
                    iconColor = WarningAmber,
                    title = "Theme",
                    subtitle = uiState.themePreference.displayName,
                    onClick = { isThemeDialogOpen = true },
                    testTag = "settings_theme_row"
                )

                // Categories
                SettingsRow(
                    icon = Icons.Default.Category,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "Manage Categories",
                    subtitle = "${uiState.allCategories.size} categories configured",
                    onClick = { isCategoryDialogOpen = true },
                    testTag = "settings_categories_row"
                )

                // Budgets
                SettingsRow(
                    icon = Icons.Default.PieChart,
                    iconColor = IncomeGreen,
                    title = "Budget Settings",
                    subtitle = "${CurrencyFormatter.format(uiState.monthlyBudget, uiState.currency.symbol)} / month",
                    onClick = {
                        monthlyBudgetInput = if (uiState.monthlyBudget % 1.0 == 0.0) {
                            uiState.monthlyBudget.toLong().toString()
                        } else {
                            uiState.monthlyBudget.toString()
                        }
                        isBudgetDialogOpen = true
                    },
                    testTag = "settings_budgets_row"
                )
            }
        }

        // 2. Data & Backup
        item {
            SettingsSection(title = "Data & Backup") {
                // Export CSV
                SettingsRow(
                    icon = Icons.Default.Share,
                    iconColor = PrimaryBlue,
                    title = "Export to CSV",
                    subtitle = "Share or export transactions to spreadsheet",
                    onClick = { viewModel.exportCsv(context) },
                    testTag = "settings_export_csv_row"
                )

                // JSON Backup
                SettingsRow(
                    icon = Icons.Default.FileUpload,
                    iconColor = IncomeGreen,
                    title = "Create JSON Backup",
                    subtitle = "Export complete local backup JSON",
                    onClick = {
                        viewModel.createBackup(context) { json ->
                            backupJsonText = json
                            isBackupResultDialogOpen = true
                        }
                    },
                    testTag = "settings_create_backup_row"
                )

                // Restore JSON
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    iconColor = WarningAmber,
                    title = "Restore from JSON",
                    subtitle = "Import transactions from backup text",
                    onClick = { isRestoreDialogOpen = true },
                    testTag = "settings_restore_backup_row"
                )
            }
        }

        // 3. Demo Data & Reset
        item {
            SettingsSection(title = "Demo & Reset") {
                // Populate Sample Data
                SettingsRow(
                    icon = Icons.Default.Dataset,
                    iconColor = PrimaryBlue,
                    title = "Load Sample Transactions",
                    subtitle = "Explore charts with realistic sample data",
                    onClick = { viewModel.populateSampleData() },
                    testTag = "settings_populate_sample_row"
                )

                // Clear Sample Data
                if (uiState.sampleDataCount > 0) {
                    SettingsRow(
                        icon = Icons.Default.Restore,
                        iconColor = WarningAmber,
                        title = "Remove Sample Data",
                        subtitle = "${uiState.sampleDataCount} sample transactions found",
                        onClick = { isClearSampleDialogOpen = true },
                        testTag = "settings_clear_sample_row"
                    )
                }

                // Clear All Data
                SettingsRow(
                    icon = Icons.Default.DeleteForever,
                    iconColor = ExpenseRed,
                    title = "Clear All Transactions",
                    subtitle = "${uiState.transactionCount} total records in database",
                    onClick = { isClearAllDialogOpen = true },
                    testTag = "settings_clear_all_row"
                )
            }
        }

        // 4. App Updates & GitHub Auto-Sync
        item {
            SettingsSection(title = "App Updates & GitHub") {
                // Check for updates button
                SettingsActionRow(
                    icon = Icons.Default.SystemUpdate,
                    iconColor = PrimaryBlue,
                    title = "Check for Updates",
                    subtitle = "Current: ${uiState.currentAppVersion} • Tap to check latest release",
                    isLoading = uiState.isCheckingUpdate,
                    onClick = { viewModel.checkForUpdates(isManual = true) },
                    testTag = "settings_check_update_row"
                )

                // GitHub Repository
                SettingsRow(
                    icon = Icons.Default.Code,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = "GitHub Repository",
                    subtitle = uiState.gitHubRepo,
                    onClick = {
                        repoInput = uiState.gitHubRepo
                        isRepoDialogOpen = true
                    },
                    testTag = "settings_github_repo_row"
                )

                // Auto-check on launch toggle
                SettingsSwitchRow(
                    icon = Icons.Default.Sync,
                    iconColor = IncomeGreen,
                    title = "Auto-Check on App Launch",
                    subtitle = "Notify automatically when new APK is pushed",
                    checked = uiState.autoCheckUpdates,
                    onCheckedChange = { viewModel.toggleAutoCheckUpdates(it) },
                    testTag = "settings_auto_update_switch"
                )
            }
        }

        // 5. App Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DailySpend",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version ${uiState.currentAppVersion} • 100% Offline & Private",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = iconColor
            )
        } else {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.surface,
                checkedTrackColor = iconColor
            )
        )
    }
}
