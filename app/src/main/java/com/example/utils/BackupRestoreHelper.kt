package com.example.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Category
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

object CsvExporter {
    fun shareCsv(context: Context, csvContent: String, fileName: String = "DailySpend_Transactions.csv") {
        try {
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "DailySpend Expense Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Transactions CSV"))
        } catch (e: Exception) {
            // Fallback plain text intent if file provider fails
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "DailySpend Expense Export")
                putExtra(Intent.EXTRA_TEXT, csvContent)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Transactions CSV"))
        }
    }
}

object BackupRestoreHelper {
    suspend fun createFullBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val transactions = db.transactionDao().getAllTransactions().first()
        val categories = db.categoryDao().getAllCategories().first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "DailySpend")

        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("type", tx.type.name)
            obj.put("amount", tx.amount)
            obj.put("categoryId", tx.categoryId)
            obj.put("categoryName", tx.categoryName)
            obj.put("description", tx.description)
            obj.put("date", tx.date.toString())
            obj.put("time", tx.time.toString())
            obj.put("paymentMethod", tx.paymentMethod)
            obj.put("createdAt", tx.createdAt)
            obj.put("updatedAt", tx.updatedAt)
            obj.put("isSample", tx.isSample)
            txArray.put(obj)
        }
        root.put("transactions", txArray)

        val catArray = JSONArray()
        for (cat in categories) {
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("type", cat.type.name)
            obj.put("iconName", cat.iconName)
            obj.put("colorHex", cat.colorHex)
            if (cat.budgetAmount != null) {
                obj.put("budgetAmount", cat.budgetAmount)
            }
            obj.put("isDefault", cat.isDefault)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(context: Context, jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val txArray = root.optJSONArray("transactions") ?: return@withContext Result.failure(
                IllegalArgumentException("Invalid backup format: missing transactions list")
            )

            val db = AppDatabase.getInstance(context)
            val restoredTransactions = mutableListOf<TransactionEntity>()

            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                val type = TransactionType.valueOf(obj.getString("type"))
                val amount = obj.getDouble("amount")
                val categoryId = obj.getString("categoryId")
                val categoryName = obj.getString("categoryName")
                val description = obj.optString("description", "")
                val date = LocalDate.parse(obj.getString("date"))
                val time = LocalTime.parse(obj.getString("time"))
                val paymentMethod = obj.optString("paymentMethod", "Cash")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                val isSample = obj.optBoolean("isSample", false)

                restoredTransactions.add(
                    TransactionEntity(
                        id = 0, // generate fresh local IDs to prevent collisions
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
                )
            }

            // Restore custom categories if present
            val catArray = root.optJSONArray("categories")
            if (catArray != null) {
                val restoredCategories = mutableListOf<CategoryEntity>()
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val type = TransactionType.valueOf(obj.getString("type"))
                    val iconName = obj.optString("iconName", "other")
                    val colorHex = obj.optString("colorHex", "#64748B")
                    val budget = if (obj.has("budgetAmount")) obj.getDouble("budgetAmount") else null
                    val isDefault = obj.optBoolean("isDefault", false)

                    restoredCategories.add(
                        CategoryEntity(
                            id = id,
                            name = name,
                            type = type,
                            iconName = iconName,
                            colorHex = colorHex,
                            budgetAmount = budget,
                            isDefault = isDefault
                        )
                    )
                }
                db.categoryDao().insertAll(restoredCategories)
            }

            if (restoredTransactions.isNotEmpty()) {
                db.transactionDao().insertAll(restoredTransactions)
            }

            Result.success(restoredTransactions.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
