package com.example.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Transactions : Screen("transactions")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object AddTransaction : Screen("add_transaction?type={type}&txId={txId}") {
        fun createRoute(type: String = "EXPENSE", txId: Long = 0L): String {
            return "add_transaction?type=$type&txId=$txId"
        }
    }
    object CategoryManagement : Screen("category_management")
    object BudgetSettings : Screen("budget_settings")
}
