package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Category
import com.example.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: String,
    val budgetAmount: Double? = null,
    val isDefault: Boolean = false
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        type = type,
        iconName = iconName,
        colorHex = colorHex,
        budgetAmount = budgetAmount,
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            type = category.type,
            iconName = category.iconName,
            colorHex = category.colorHex,
            budgetAmount = category.budgetAmount,
            isDefault = category.isDefault
        )
    }
}
