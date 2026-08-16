package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DailySpending
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal

@Composable
fun SpendingTrendChart(
    dailySpendingList: List<DailySpending>,
    currencySymbol: String = "৳",
    title: String = "Spending Trend",
    modifier: Modifier = Modifier
) {
    val maxAmount = dailySpendingList.maxOfOrNull { it.amount }?.coerceAtLeast(100.0) ?: 100.0
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(dailySpendingList) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("spending_trend_chart"),
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (dailySpendingList.size < 2 || dailySpendingList.all { it.amount == 0.0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not enough trend data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / (dailySpendingList.size - 1)

                    val points = dailySpendingList.mapIndexed { index, item ->
                        val x = index * stepX
                        val normalized = (item.amount / maxAmount).toFloat() * animationProgress.value
                        val y = height - (normalized * (height * 0.8f)) - (height * 0.1f)
                        Offset(x, y)
                    }

                    // Build smooth cubic Bezier path
                    val path = Path()
                    val fillPath = Path()

                    points.forEachIndexed { i, p ->
                        if (i == 0) {
                            path.moveTo(p.x, p.y)
                            fillPath.moveTo(p.x, height)
                            fillPath.lineTo(p.x, p.y)
                        } else {
                            val prev = points[i - 1]
                            val cx1 = (prev.x + p.x) / 2
                            val cy1 = prev.y
                            val cx2 = (prev.x + p.x) / 2
                            val cy2 = p.y
                            path.cubicTo(cx1, cy1, cx2, cy2, p.x, p.y)
                            fillPath.cubicTo(cx1, cy1, cx2, cy2, p.x, p.y)
                        }
                    }

                    fillPath.lineTo(points.last().x, height)
                    fillPath.close()

                    // Draw gradient fill under line
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                primaryColor.copy(alpha = 0.01f)
                            )
                        )
                    )

                    // Draw trend line
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(primaryColor, secondaryColor)
                        ),
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw point dots
                    points.forEach { p ->
                        drawCircle(
                            color = primaryColor,
                            radius = 4.5.dp.toPx(),
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = p
                        )
                    }
                }
            }
        }
    }
}
