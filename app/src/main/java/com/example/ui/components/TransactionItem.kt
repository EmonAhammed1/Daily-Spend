package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String = "৳",
    onClick: () -> Unit = {},
    iconName: String? = null,
    colorHex: String? = null,
    modifier: Modifier = Modifier
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    val effectiveIcon = iconName ?: when (transaction.categoryId.lowercase()) {
        "cat_food" -> "food"
        "cat_transport" -> "transport"
        "cat_shopping" -> "shopping"
        "cat_bills" -> "bills"
        "cat_rent" -> "rent"
        "cat_entertainment" -> "entertainment"
        "cat_health" -> "health"
        "cat_education" -> "education"
        "cat_travel" -> "travel"
        "cat_groceries" -> "groceries"
        "cat_personal" -> "personal"
        "cat_salary" -> "salary"
        "cat_freelance" -> "freelance"
        "cat_business" -> "business"
        "cat_gift" -> "gift"
        "cat_investment" -> "investment"
        else -> "other"
    }

    val effectiveColorHex = colorHex ?: when (transaction.categoryId.lowercase()) {
        "cat_food" -> "#EF4444"
        "cat_transport" -> "#3B82F6"
        "cat_shopping" -> "#EC4899"
        "cat_bills" -> "#F59E0B"
        "cat_rent" -> "#8B5CF6"
        "cat_entertainment" -> "#10B981"
        "cat_health" -> "#06B6D4"
        "cat_salary" -> "#10B981"
        "cat_freelance" -> "#06B6D4"
        else -> if (isExpense) "#EF4444" else "#10B981"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("transaction_item_${transaction.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            CategoryIcon(
                iconName = effectiveIcon,
                colorHex = effectiveColorHex,
                size = 46.dp,
                iconSize = 24.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Transaction Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.categoryName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (transaction.isSample) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Sample",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (transaction.description.isNotBlank()) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "${DateUtils.formatRelativeDate(transaction.date)} • ${DateUtils.formatTime(transaction.time)} • ${transaction.paymentMethod}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount with Sign
            val sign = if (isExpense) "- " else "+ "
            val amountColor = if (isExpense) ExpenseRed else IncomeGreen

            Text(
                text = "$sign${CurrencyFormatter.format(transaction.amount, currencySymbol)}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                modifier = Modifier.testTag("transaction_amount_${transaction.id}")
            )
        }
    }
}
