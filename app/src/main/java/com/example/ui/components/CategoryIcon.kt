package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun getCategoryIconVector(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "food" -> Icons.Default.Fastfood
        "transport" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingCart
        "bills" -> Icons.Default.Receipt
        "rent" -> Icons.Default.Home
        "entertainment" -> Icons.Default.Movie
        "health" -> Icons.Default.LocalHospital
        "education" -> Icons.Default.School
        "travel" -> Icons.Default.Flight
        "groceries" -> Icons.Default.Store
        "personal" -> Icons.Default.CreditCard
        "salary" -> Icons.Default.Payments
        "freelance" -> Icons.Default.Work
        "business" -> Icons.Default.AccountBalance
        "gift" -> Icons.Default.CardGiftcard
        "investment" -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalanceWallet
    }
}

fun parseColorHex(colorHex: String, fallback: Color = Color(0xFF3B82F6)): Color {
    return try {
        val clean = colorHex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

@Composable
fun CategoryIcon(
    iconName: String,
    colorHex: String = "#3B82F6",
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val categoryColor = parseColorHex(colorHex)
    val backgroundColor = categoryColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getCategoryIconVector(iconName),
            contentDescription = null,
            tint = categoryColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
