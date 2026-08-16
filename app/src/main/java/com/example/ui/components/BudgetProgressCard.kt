package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ExpenseRedLight
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.IncomeGreenLight
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.utils.CurrencyFormatter
import kotlin.math.abs

@Composable
fun BudgetProgressCard(
    monthlyBudget: Double,
    monthlySpent: Double,
    currencySymbol: String = "৳",
    title: String = "Monthly Budget",
    modifier: Modifier = Modifier
) {
    if (monthlyBudget <= 0.0) return

    val remaining = monthlyBudget - monthlySpent
    val usageRatio = (monthlySpent / monthlyBudget).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = usageRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "budgetProgress"
    )

    val isExceeded = monthlySpent > monthlyBudget
    val isNearLimit = usageRatio >= 0.8f && !isExceeded

    val progressColor by animateColorAsState(
        targetValue = when {
            isExceeded -> ExpenseRed
            isNearLimit -> WarningAmber
            else -> PrimaryBlue
        },
        label = "progressColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_progress_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = CurrencyFormatter.format(monthlyBudget, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Spent & Remaining Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(monthlySpent, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isExceeded) "Overspent" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.format(abs(remaining), currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isExceeded) ExpenseRed else IncomeGreen
                    )
                }
            }

            // Status message
            if (isExceeded || isNearLimit) {
                Spacer(modifier = Modifier.height(12.dp))
                val bg = if (isExceeded) ExpenseRedLight else WarningAmberLight
                val fg = if (isExceeded) ExpenseRed else WarningAmber
                val icon = if (isExceeded) Icons.Default.ErrorOutline else Icons.Default.WarningAmber
                val message = if (isExceeded) {
                    "You have exceeded your monthly budget by ${CurrencyFormatter.format(abs(remaining), currencySymbol)}."
                } else {
                    "You have used ${(usageRatio * 100).toInt()}% of your monthly budget."
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = fg,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = fg
                        )
                    }
                }
            }
        }
    }
}
