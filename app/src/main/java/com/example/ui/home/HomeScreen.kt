package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TransactionType
import com.example.ui.components.BalanceSummaryCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SpendingBarChart
import com.example.ui.components.TodaySummaryCard
import com.example.ui.components.TransactionItem
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.DateUtils
import java.time.LocalDate

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddIncome: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToEditTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencySymbol = uiState.currency.symbol

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Top Greeting Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = DateUtils.getGreeting(),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("home_greeting_text")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateUtils.formatFullDate(LocalDate.now()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Balance Summary Card
        item {
            BalanceSummaryCard(
                summary = uiState.balanceSummary,
                currencySymbol = currencySymbol
            )
        }

        // 3. Today Summary Card
        item {
            TodaySummaryCard(
                todaySummary = uiState.todaySummary,
                currencySymbol = currencySymbol
            )
        }

        // 4. Quick Actions
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // + Expense
                    Button(
                        onClick = onNavigateToAddExpense,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_action_add_expense"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Expense", maxLines = 1)
                    }

                    // + Income
                    Button(
                        onClick = onNavigateToAddIncome,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_action_add_income"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IncomeGreen
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Income", maxLines = 1)
                    }

                    // View History
                    OutlinedButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("quick_action_view_history"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "History", maxLines = 1)
                    }
                }
            }
        }

        // 5. Spending Overview Chart Preview
        item {
            SpendingBarChart(
                dailySpendingList = uiState.weeklySpending,
                currencySymbol = currencySymbol,
                title = "Spending Overview"
            )
        }

        // 6. Recent Transactions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.recentTransactions.isNotEmpty()) {
                    TextButton(
                        onClick = onNavigateToTransactions,
                        modifier = Modifier.testTag("view_all_transactions_button")
                    ) {
                        Text("View All")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (uiState.recentTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No transactions yet",
                    subtitle = "Tap + Expense to start recording your daily expenses.",
                    buttonText = "Add Expense",
                    onButtonClick = onNavigateToAddExpense
                )
            }
        } else {
            items(
                items = uiState.recentTransactions,
                key = { it.id }
            ) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    currencySymbol = currencySymbol,
                    onClick = { onNavigateToEditTransaction(transaction.id) }
                )
            }
        }

        // Bottom spacer for FAB & Navigation bar padding
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
