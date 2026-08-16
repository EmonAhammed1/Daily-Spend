package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DailySpending
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueDark
import com.example.utils.CurrencyFormatter

@Composable
fun SpendingBarChart(
    dailySpendingList: List<DailySpending>,
    currencySymbol: String = "৳",
    title: String = "Weekly Spending",
    modifier: Modifier = Modifier
) {
    val maxAmount = dailySpendingList.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0
    val totalWeekly = dailySpendingList.sumOf { it.amount }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dailySpendingList) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val textColorArgb = onSurfaceVariant.toArgb()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_bar_chart"),
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
                    text = CurrencyFormatter.formatCompact(totalWeekly, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (dailySpendingList.isEmpty() || totalWeekly == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No spending data for this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 30.dp.toPx()
                    val chartHeight = height - bottomPadding
                    val barCount = dailySpendingList.size
                    val spacing = width / barCount
                    val barWidth = (spacing * 0.48f).coerceAtMost(36.dp.toPx())

                    // Draw subtle baseline
                    drawLine(
                        color = surfaceVariant,
                        start = Offset(0f, chartHeight),
                        end = Offset(width, chartHeight),
                        strokeWidth = 2.dp.toPx()
                    )

                    val textPaint = android.graphics.Paint().apply {
                        color = textColorArgb
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val activeTextPaint = android.graphics.Paint().apply {
                        color = primaryColor.toArgb()
                        textSize = 11.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        isAntiAlias = true
                    }

                    dailySpendingList.forEachIndexed { index, item ->
                        val centerX = (index * spacing) + (spacing / 2)
                        val barHeight = if (maxAmount > 0) {
                            ((item.amount / maxAmount) * (chartHeight * 0.85f) * animationProgress.value).toFloat()
                        } else 0f

                        val left = centerX - (barWidth / 2)
                        val top = chartHeight - barHeight

                        // Background pillar
                        drawRoundRect(
                            color = surfaceVariant.copy(alpha = 0.5f),
                            topLeft = Offset(left, chartHeight - (chartHeight * 0.85f)),
                            size = Size(barWidth, chartHeight * 0.85f),
                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        )

                        // Active Bar
                        if (barHeight > 0f) {
                            val isMax = item.amount == maxAmount && item.amount > 0
                            val barColor = if (isMax) primaryColor else primaryColor.copy(alpha = 0.75f)

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight.coerceAtLeast(6.dp.toPx())),
                                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                            )
                        }

                        // Day Label
                        drawContext.canvas.nativeCanvas.drawText(
                            item.dayLabel,
                            centerX,
                            height - 6.dp.toPx(),
                            if (item.amount == maxAmount && item.amount > 0) activeTextPaint else textPaint
                        )
                    }
                }
            }
        }
    }
}
