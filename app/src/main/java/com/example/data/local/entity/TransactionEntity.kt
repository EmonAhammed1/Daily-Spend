package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: String,
    val categoryName: String,
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime,
    val paymentMethod: String = "Cash",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        type = type,
        amount = amount,
        categoryId = categoryId,
        categoryName = categoryName,
        description = description,
        date = date,
        time = time,
        paymentMethod = paymentMethod,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isSample = isSample
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity = TransactionEntity(
            id = transaction.id,
            type = transaction.type,
            amount = transaction.amount,
            categoryId = transaction.categoryId,
            categoryName = transaction.categoryName,
            description = transaction.description,
            date = transaction.date,
            time = transaction.time,
            paymentMethod = transaction.paymentMethod,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt,
            isSample = transaction.isSample
        )
    }
}
