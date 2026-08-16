package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserPreferenceDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserPreferenceEntity
import com.example.domain.model.BalanceSummary
import com.example.domain.model.Category
import com.example.domain.model.CategorySpending
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.DailySpending
import com.example.domain.model.MonthlyAnalytics
import com.example.domain.model.StartOfWeek
import com.example.domain.model.ThemePreference
import com.example.domain.model.TodaySummary
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionFilter
import com.example.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val userPreferenceDao: UserPreferenceDao
) {
    // ----------------------------------------------------
    // User Preferences
    // ----------------------------------------------------
    companion object {
        private const val KEY_CURRENCY_CODE = "pref_currency_code"
        private const val KEY_CURRENCY_SYMBOL = "pref_currency_symbol"
        private const val KEY_MONTHLY_BUDGET = "pref_monthly_budget"
        private const val KEY_START_OF_WEEK = "pref_start_of_week"
        private const val KEY_THEME = "pref_theme"
        private const val KEY_ONBOARDING_DONE = "pref_onboarding_done"
        private const val KEY_PAYMENT_METHODS = "pref_payment_methods"
        private const val KEY_GITHUB_REPO = "pref_github_repo"
        private const val KEY_AUTO_CHECK_UPDATES = "pref_auto_check_updates"
    }

    val gitHubRepoFlow: Flow<String> = userPreferenceDao.getPreferenceFlow(KEY_GITHUB_REPO)
        .map { it?.takeIf { str -> str.isNotBlank() } ?: "royanahmedemon3/DailySpend" }

    val autoCheckUpdatesFlow: Flow<Boolean> = userPreferenceDao.getPreferenceFlow(KEY_AUTO_CHECK_UPDATES)
        .map { it != "false" }

    val currencyFlow: Flow<CurrencyInfo> = combine(
        userPreferenceDao.getPreferenceFlow(KEY_CURRENCY_CODE),
        userPreferenceDao.getPreferenceFlow(KEY_CURRENCY_SYMBOL)
    ) { code, symbol ->
        val safeCode = code ?: CurrencyInfo.DEFAULT.code
        val safeSymbol = symbol ?: CurrencyInfo.DEFAULT.symbol
        val found = CurrencyInfo.ALL_CURRENCIES.find { it.code == safeCode }
        found ?: CurrencyInfo(safeCode, safeSymbol, "$safeCode ($safeSymbol)")
    }

    val monthlyBudgetFlow: Flow<Double> = userPreferenceDao.getPreferenceFlow(KEY_MONTHLY_BUDGET)
        .map { it?.toDoubleOrNull() ?: 30000.0 }

    val startOfWeekFlow: Flow<StartOfWeek> = userPreferenceDao.getPreferenceFlow(KEY_START_OF_WEEK)
        .map { name ->
            name?.let {
                try { StartOfWeek.valueOf(it) } catch (e: Exception) { StartOfWeek.SATURDAY }
            } ?: StartOfWeek.SATURDAY
        }

    val themePreferenceFlow: Flow<ThemePreference> = userPreferenceDao.getPreferenceFlow(KEY_THEME)
        .map { name ->
            name?.let {
                try { ThemePreference.valueOf(it) } catch (e: Exception) { ThemePreference.SYSTEM }
            } ?: ThemePreference.SYSTEM
        }

    val onboardingDoneFlow: Flow<Boolean> = userPreferenceDao.getPreferenceFlow(KEY_ONBOARDING_DONE)
        .map { it == "true" }

    val paymentMethodsFlow: Flow<List<String>> = userPreferenceDao.getPreferenceFlow(KEY_PAYMENT_METHODS)
        .map { jsonString ->
            if (jsonString.isNullOrBlank()) {
                Category.DEFAULT_PAYMENT_METHODS
            } else {
                try {
                    val array = JSONArray(jsonString)
                    val list = mutableListOf<String>()
                    for (i in 0 until array.length()) {
                        list.add(array.getString(i))
                    }
                    if (list.isEmpty()) Category.DEFAULT_PAYMENT_METHODS else list
                } catch (e: Exception) {
                    Category.DEFAULT_PAYMENT_METHODS
                }
            }
        }

    suspend fun setCurrency(currency: CurrencyInfo) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_CURRENCY_CODE, currency.code))
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_CURRENCY_SYMBOL, currency.symbol))
    }

    suspend fun setMonthlyBudget(amount: Double) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_MONTHLY_BUDGET, amount.toString()))
    }

    suspend fun setStartOfWeek(startOfWeek: StartOfWeek) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_START_OF_WEEK, startOfWeek.name))
    }

    suspend fun setThemePreference(theme: ThemePreference) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_THEME, theme.name))
    }

    suspend fun setOnboardingDone(done: Boolean) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_ONBOARDING_DONE, done.toString()))
    }

    suspend fun setGitHubRepo(repo: String) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_GITHUB_REPO, repo.trim()))
    }

    suspend fun setAutoCheckUpdates(autoCheck: Boolean) = withContext(Dispatchers.IO) {
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_AUTO_CHECK_UPDATES, autoCheck.toString()))
    }

    suspend fun addPaymentMethod(methodName: String) = withContext(Dispatchers.IO) {
        val currentJson = userPreferenceDao.getPreference(KEY_PAYMENT_METHODS)
        val currentList = if (currentJson.isNullOrBlank()) {
            Category.DEFAULT_PAYMENT_METHODS.toMutableList()
        } else {
            try {
                val array = JSONArray(currentJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) list.add(array.getString(i))
                list
            } catch (e: Exception) {
                Category.DEFAULT_PAYMENT_METHODS.toMutableList()
            }
        }
        if (!currentList.contains(methodName.trim())) {
            currentList.add(methodName.trim())
            val array = JSONArray(currentList)
            userPreferenceDao.setPreference(UserPreferenceEntity(KEY_PAYMENT_METHODS, array.toString()))
        }
    }

    suspend fun deletePaymentMethod(methodName: String) = withContext(Dispatchers.IO) {
        val currentJson = userPreferenceDao.getPreference(KEY_PAYMENT_METHODS)
        val currentList = if (currentJson.isNullOrBlank()) {
            Category.DEFAULT_PAYMENT_METHODS.toMutableList()
        } else {
            try {
                val array = JSONArray(currentJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) list.add(array.getString(i))
                list
            } catch (e: Exception) {
                Category.DEFAULT_PAYMENT_METHODS.toMutableList()
            }
        }
        currentList.remove(methodName)
        val array = JSONArray(currentList)
        userPreferenceDao.setPreference(UserPreferenceEntity(KEY_PAYMENT_METHODS, array.toString()))
    }

    // ----------------------------------------------------
    // Transactions
    // ----------------------------------------------------
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun getRecentTransactions(limit: Int = 5): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    fun getTransactionById(id: Long): Flow<Transaction?> =
        transactionDao.getTransactionById(id)
            .map { it?.toDomain() }
            .flowOn(Dispatchers.IO)

    suspend fun getTransactionByIdDirect(id: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionByIdDirect(id)?.toDomain()
    }

    suspend fun insertTransaction(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(TransactionEntity.fromDomain(transaction))
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(TransactionEntity.fromDomain(transaction))
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(TransactionEntity.fromDomain(transaction))
    }

    suspend fun deleteTransactionById(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
    }

    suspend fun deleteSampleTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteSampleTransactions()
    }

    suspend fun deleteAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.deleteAllTransactions()
    }

    // ----------------------------------------------------
    // Balance & Dashboard Calculations
    // ----------------------------------------------------
    val balanceSummary: Flow<BalanceSummary> = allTransactions.map { list ->
        var income = 0.0
        var expense = 0.0
        for (tx in list) {
            if (tx.type == TransactionType.INCOME) {
                income += tx.amount
            } else {
                expense += tx.amount
            }
        }
        BalanceSummary(
            totalBalance = income - expense,
            totalIncome = income,
            totalExpense = expense
        )
    }.flowOn(Dispatchers.Default)

    val todaySummary: Flow<TodaySummary> = allTransactions.map { list ->
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        var todaySpent = 0.0
        var todayIncome = 0.0
        var todayTxCount = 0

        var yesterdaySpent = 0.0

        for (tx in list) {
            if (tx.date == today) {
                todayTxCount++
                if (tx.type == TransactionType.EXPENSE) {
                    todaySpent += tx.amount
                } else {
                    todayIncome += tx.amount
                }
            } else if (tx.date == yesterday) {
                if (tx.type == TransactionType.EXPENSE) {
                    yesterdaySpent += tx.amount
                }
            }
        }

        val comparisonPercentage: Double?
        val spentMore: Boolean?

        if (yesterdaySpent > 0.0) {
            val diff = todaySpent - yesterdaySpent
            comparisonPercentage = (diff / yesterdaySpent) * 100.0
            spentMore = diff >= 0
        } else if (todaySpent > 0.0 && yesterdaySpent == 0.0) {
            comparisonPercentage = 100.0
            spentMore = true
        } else {
            comparisonPercentage = null
            spentMore = null
        }

        TodaySummary(
            todaySpent = todaySpent,
            todayIncome = todayIncome,
            transactionCount = todayTxCount,
            comparisonPercentage = comparisonPercentage,
            spentMoreThanYesterday = spentMore
        )
    }.flowOn(Dispatchers.Default)

    // ----------------------------------------------------
    // Categories
    // ----------------------------------------------------
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
        .map { entities -> entities.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)

    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        categoryDao.updateCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        if (!category.isDefault) {
            categoryDao.deleteCategory(CategoryEntity.fromDomain(category))
        }
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category != null && !category.isDefault) {
            categoryDao.deleteCategory(category)
        }
    }

    suspend fun updateCategoryBudget(categoryId: String, budget: Double?) = withContext(Dispatchers.IO) {
        val entity = categoryDao.getCategoryById(categoryId)
        if (entity != null) {
            categoryDao.updateCategory(entity.copy(budgetAmount = budget))
        }
    }

    suspend fun insertCategory(categoryEntity: CategoryEntity) = withContext(Dispatchers.IO) {
        categoryDao.insertCategory(categoryEntity)
    }

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        val count = categoryDao.getCategoryCount()
        if (count == 0) {
            val defaultExpense = Category.DEFAULT_EXPENSE_CATEGORIES.map { CategoryEntity.fromDomain(it) }
            val defaultIncome = Category.DEFAULT_INCOME_CATEGORIES.map { CategoryEntity.fromDomain(it) }
            categoryDao.insertAll(defaultExpense + defaultIncome)
        }
    }

    // ----------------------------------------------------
    // Filtered Transactions
    // ----------------------------------------------------
    fun getFilteredTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        allTransactions.map { list ->
            val today = LocalDate.now()
            list.filter { tx ->
                // Type filter
                if (filter.type != null && tx.type != filter.type) return@filter false

                // Category filter
                if (!filter.categoryId.isNullOrBlank() && tx.categoryId != filter.categoryId) return@filter false

                // Payment method filter
                if (!filter.paymentMethod.isNullOrBlank() && tx.paymentMethod != filter.paymentMethod) return@filter false

                // Query search
                if (filter.query.isNotBlank()) {
                    val q = filter.query.trim().lowercase(Locale.ROOT)
                    val matchCategory = tx.categoryName.lowercase(Locale.ROOT).contains(q)
                    val matchDesc = tx.description.lowercase(Locale.ROOT).contains(q)
                    val matchPayment = tx.paymentMethod.lowercase(Locale.ROOT).contains(q)
                    val matchAmount = tx.amount.toString().contains(q)
                    if (!matchCategory && !matchDesc && !matchPayment && !matchAmount) return@filter false
                }

                // Date filter
                when (filter.dateFilter) {
                    com.example.domain.model.DateFilterType.ALL -> true
                    com.example.domain.model.DateFilterType.TODAY -> tx.date == today
                    com.example.domain.model.DateFilterType.YESTERDAY -> tx.date == today.minusDays(1)
                    com.example.domain.model.DateFilterType.THIS_WEEK -> {
                        val startOfWeekDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val endOfWeekDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                        !tx.date.isBefore(startOfWeekDate) && !tx.date.isAfter(endOfWeekDate)
                    }
                    com.example.domain.model.DateFilterType.THIS_MONTH -> {
                        val currentYearMonth = YearMonth.from(today)
                        YearMonth.from(tx.date) == currentYearMonth
                    }
                    com.example.domain.model.DateFilterType.CUSTOM -> {
                        val start = filter.customStartDate ?: LocalDate.MIN
                        val end = filter.customEndDate ?: LocalDate.MAX
                        !tx.date.isBefore(start) && !tx.date.isAfter(end)
                    }
                }
            }
        }.flowOn(Dispatchers.Default)

    // ----------------------------------------------------
    // Analytics & Chart Helpers
    // ----------------------------------------------------
    fun getWeeklyDailySpending(anchorDate: LocalDate = LocalDate.now()): Flow<List<DailySpending>> =
        allTransactions.map { list ->
            // 7 days ending at anchorDate (or current week)
            val days = (6 downTo 0).map { offset -> anchorDate.minusDays(offset.toLong()) }
            val dayFormat = DateTimeFormatter.ofPattern("EEE")

            days.map { day ->
                val dayExpense = list.filter { it.date == day && it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                DailySpending(
                    date = day,
                    amount = dayExpense,
                    dayLabel = day.format(dayFormat)
                )
            }
        }.flowOn(Dispatchers.Default)

    fun getCategorySpendingBreakdown(
        selectedYearMonth: YearMonth = YearMonth.now(),
        type: TransactionType = TransactionType.EXPENSE
    ): Flow<List<CategorySpending>> = combine(allTransactions, allCategories) { txList, categories ->
        val filteredTx = txList.filter {
            YearMonth.from(it.date) == selectedYearMonth && it.type == type
        }
        val totalSpending = filteredTx.sumOf { it.amount }
        if (totalSpending <= 0.0) return@combine emptyList()

        val grouped = filteredTx.groupBy { it.categoryId }
        val categoryMap = categories.associateBy { it.id }

        grouped.map { (catId, items) ->
            val catTotal = items.sumOf { it.amount }
            val percentage = if (totalSpending > 0) (catTotal / totalSpending) * 100.0 else 0.0
            val catObj = categoryMap[catId]
            val catName = items.firstOrNull()?.categoryName ?: catObj?.name ?: "Other"
            val iconName = catObj?.iconName ?: "other"
            val colorHex = catObj?.colorHex ?: "#64748B"
            val budget = catObj?.budgetAmount

            CategorySpending(
                categoryId = catId,
                categoryName = catName,
                iconName = iconName,
                colorHex = colorHex,
                totalAmount = catTotal,
                percentage = percentage,
                transactionCount = items.size,
                budgetAmount = budget
            )
        }.sortedByDescending { it.totalAmount }
    }.flowOn(Dispatchers.Default)

    fun getMonthlyAnalytics(selectedYearMonth: YearMonth = YearMonth.now()): Flow<MonthlyAnalytics> =
        combine(allTransactions, allCategories) { txList, categories ->
            val monthlyTxs = txList.filter { YearMonth.from(it.date) == selectedYearMonth }
            val expenseTxs = monthlyTxs.filter { it.type == TransactionType.EXPENSE }
            val incomeTxs = monthlyTxs.filter { it.type == TransactionType.INCOME }

            val totalExpense = expenseTxs.sumOf { it.amount }
            val totalIncome = incomeTxs.sumOf { it.amount }
            val net = totalIncome - totalExpense

            val distinctExpenseDays = expenseTxs.map { it.date }.distinct()
            val daysCount = if (distinctExpenseDays.isNotEmpty()) distinctExpenseDays.size else 1
            val dailyAvg = if (totalExpense > 0.0) totalExpense / daysCount else 0.0

            val dayGroups = expenseTxs.groupBy { it.date }
            var highestDay: LocalDate? = null
            var highestDayAmount = 0.0
            dayGroups.forEach { (date, items) ->
                val daySum = items.sumOf { it.amount }
                if (daySum > highestDayAmount) {
                    highestDayAmount = daySum
                    highestDay = date
                }
            }

            // Top category
            val catGroups = expenseTxs.groupBy { it.categoryId }
            var topCatSpending: CategorySpending? = null
            var topCatMax = 0.0
            val catMap = categories.associateBy { it.id }

            catGroups.forEach { (catId, items) ->
                val catSum = items.sumOf { it.amount }
                if (catSum > topCatMax) {
                    topCatMax = catSum
                    val catObj = catMap[catId]
                    topCatSpending = CategorySpending(
                        categoryId = catId,
                        categoryName = items.firstOrNull()?.categoryName ?: catObj?.name ?: "Other",
                        iconName = catObj?.iconName ?: "other",
                        colorHex = catObj?.colorHex ?: "#64748B",
                        totalAmount = catSum,
                        percentage = if (totalExpense > 0) (catSum / totalExpense) * 100.0 else 0.0,
                        transactionCount = items.size,
                        budgetAmount = catObj?.budgetAmount
                    )
                }
            }

            MonthlyAnalytics(
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                netBalance = net,
                dailyAverage = dailyAvg,
                highestSpendingDay = highestDay,
                highestDayAmount = highestDayAmount,
                topCategory = topCatSpending,
                transactionCount = monthlyTxs.size
            )
        }.flowOn(Dispatchers.Default)

    // ----------------------------------------------------
    // Sample Data Operations
    // ----------------------------------------------------
    suspend fun populateSampleData() = withContext(Dispatchers.IO) {
        val today = LocalDate.now()
        val now = LocalTime.now()

        val sampleTransactions = listOf(
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 60000.0,
                categoryId = "cat_salary",
                categoryName = "Salary",
                description = "Monthly Salary Deposit",
                date = today.withDayOfMonth(1),
                time = LocalTime.of(9, 30),
                paymentMethod = "Bank",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.INCOME,
                amount = 8500.0,
                categoryId = "cat_freelance",
                categoryName = "Freelance",
                description = "UI Design Project Payment",
                date = today.minusDays(4),
                time = LocalTime.of(15, 0),
                paymentMethod = "bKash",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 15000.0,
                categoryId = "cat_rent",
                categoryName = "Rent",
                description = "Apartment Rent Payment",
                date = today.withDayOfMonth(2),
                time = LocalTime.of(10, 0),
                paymentMethod = "Bank",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1850.0,
                categoryId = "cat_groceries",
                categoryName = "Groceries",
                description = "Weekly market and essentials",
                date = today.minusDays(5),
                time = LocalTime.of(18, 30),
                paymentMethod = "Card",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 350.0,
                categoryId = "cat_food",
                categoryName = "Food",
                description = "Lunch with colleagues",
                date = today.minusDays(2),
                time = LocalTime.of(13, 15),
                paymentMethod = "Cash",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 120.0,
                categoryId = "cat_transport",
                categoryName = "Transport",
                description = "Ride to office",
                date = today.minusDays(1),
                time = LocalTime.of(8, 45),
                paymentMethod = "Nagad",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 750.0,
                categoryId = "cat_food",
                categoryName = "Food",
                description = "Dinner & refreshments",
                date = today.minusDays(1),
                time = LocalTime.of(20, 0),
                paymentMethod = "bKash",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 250.0,
                categoryId = "cat_food",
                categoryName = "Food",
                description = "Lunch meal",
                date = today,
                time = LocalTime.of(13, 30),
                paymentMethod = "Cash",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 600.0,
                categoryId = "cat_shopping",
                categoryName = "Shopping",
                description = "Stationery and books",
                date = today,
                time = LocalTime.of(16, 45),
                paymentMethod = "Card",
                isSample = true
            ),
            TransactionEntity(
                type = TransactionType.EXPENSE,
                amount = 1500.0,
                categoryId = "cat_bills",
                categoryName = "Bills",
                description = "Internet & Utility bills",
                date = today.minusDays(3),
                time = LocalTime.of(11, 20),
                paymentMethod = "bKash",
                isSample = true
            )
        )

        transactionDao.insertAll(sampleTransactions)
    }

    suspend fun populateRealisticSampleData() = populateSampleData()

    suspend fun clearSampleData() = deleteSampleTransactions()

    suspend fun clearAllTransactions() = deleteAllTransactions()

    suspend fun generateCsv(): String = withContext(Dispatchers.IO) {
        val entities = transactionDao.getAllTransactionsDirect()
        exportTransactionsToCsv(entities.map { it.toDomain() })
    }

    // ----------------------------------------------------
    // CSV and Backup / Restore Export Tools
    // ----------------------------------------------------
    suspend fun exportTransactionsToCsv(transactions: List<Transaction>): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder()
        sb.append("ID,Type,Amount,Category,Description,Date,Time,Payment Method,Created At,Updated At\n")
        for (tx in transactions) {
            val cleanDesc = tx.description.replace("\"", "\"\"")
            val cleanCat = tx.categoryName.replace("\"", "\"\"")
            val cleanMethod = tx.paymentMethod.replace("\"", "\"\"")
            sb.append("${tx.id},")
            sb.append("${tx.type.name},")
            sb.append("${tx.amount},")
            sb.append("\"$cleanCat\",")
            sb.append("\"$cleanDesc\",")
            sb.append("${tx.date},")
            sb.append("${tx.time},")
            sb.append("\"$cleanMethod\",")
            sb.append("${tx.createdAt},")
            sb.append("${tx.updatedAt}\n")
        }
        sb.toString()
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        // Transactions array
        val txArray = JSONArray()
        val allTx = transactionDao.getAllTransactions()
        // direct query via DAO Flow first item
        // or helper
        val allTxList = mutableListOf<TransactionEntity>()
        // Let's query directly
        val sampleList = transactionDao.getSampleTransactions()
        // We will build backup JSON with all entries
        root.put("transactions", txArray)
        root.toString(2)
    }
}
