package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.repository.ExpenseRepository
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.Category
import com.example.domain.model.CurrencyInfo
import com.example.domain.model.StartOfWeek
import com.example.domain.model.ThemePreference
import com.example.domain.model.TransactionType
import com.example.domain.model.UpdateDownloadState
import com.example.utils.BackupRestoreHelper
import com.example.utils.CsvExporter
import com.example.utils.GitHubUpdateManager
import com.example.utils.combine9
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class SettingsUiState(
    val currency: CurrencyInfo = CurrencyInfo.DEFAULT,
    val monthlyBudget: Double = 30000.0,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val startOfWeek: StartOfWeek = StartOfWeek.SATURDAY,
    val allCategories: List<Category> = emptyList(),
    val paymentMethods: List<String> = emptyList(),
    val transactionCount: Int = 0,
    val sampleDataCount: Int = 0,
    val gitHubRepo: String = "royanahmedemon3/DailySpend",
    val autoCheckUpdates: Boolean = true,
    val currentAppVersion: String = "v1.0.0",
    val isCheckingUpdate: Boolean = false,
    val updateInfo: AppUpdateInfo? = null,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle
)

sealed class SettingsEvent {
    data class ShowToast(val message: String) : SettingsEvent()
    data class RequestInstallPermission(val apkFile: File) : SettingsEvent()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = ExpenseRepository(
        database.transactionDao(),
        database.categoryDao(),
        database.userPreferenceDao()
    )

    private val _eventFlow = MutableSharedFlow<SettingsEvent>()
    val eventFlow: SharedFlow<SettingsEvent> = _eventFlow.asSharedFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)

    private var downloadedApkFile: File? = null

    private val appVersion: String by lazy {
        try {
            val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            "v${pInfo.versionName ?: "1.0.0"}"
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

    private val basePreferencesFlow = combine9(
        repository.currencyFlow,
        repository.monthlyBudgetFlow,
        repository.themePreferenceFlow,
        repository.startOfWeekFlow,
        repository.allCategories,
        repository.paymentMethodsFlow,
        repository.allTransactions,
        repository.gitHubRepoFlow,
        repository.autoCheckUpdatesFlow
    ) { currency, budget, theme, startOfWeek, categories, paymentMethods, allTx, gitHubRepo, autoCheck ->
        val sampleCount = allTx.count { it.isSample }
        SettingsUiState(
            currency = currency,
            monthlyBudget = budget,
            themePreference = theme,
            startOfWeek = startOfWeek,
            allCategories = categories,
            paymentMethods = paymentMethods,
            transactionCount = allTx.size,
            sampleDataCount = sampleCount,
            gitHubRepo = gitHubRepo,
            autoCheckUpdates = autoCheck,
            currentAppVersion = appVersion
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        basePreferencesFlow,
        _isCheckingUpdate,
        _updateInfo,
        _downloadState
    ) { base, isChecking, updateInfo, downloadState ->
        base.copy(
            isCheckingUpdate = isChecking,
            updateInfo = updateInfo,
            downloadState = downloadState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(currentAppVersion = appVersion)
    )

    init {
        // Auto check for updates on startup if enabled
        viewModelScope.launch {
            repository.autoCheckUpdatesFlow.collect { shouldAutoCheck ->
                if (shouldAutoCheck) {
                    checkForUpdates(isManual = false)
                }
            }
        }
    }

    fun checkForUpdates(isManual: Boolean = false) {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val currentRepo = uiState.value.gitHubRepo.trim()
            val parts = currentRepo.split("/")
            if (parts.size < 2 || parts[0].isBlank() || parts[1].isBlank()) {
                _isCheckingUpdate.value = false
                if (isManual) {
                    _eventFlow.emit(SettingsEvent.ShowToast("Invalid GitHub repo format. Use 'owner/repository'"))
                }
                return@launch
            }

            val owner = parts[0].trim()
            val repo = parts[1].trim()

            val result = GitHubUpdateManager.checkForUpdate(
                context = getApplication(),
                repoOwner = owner,
                repoName = repo,
                currentVersionName = appVersion
            )

            _isCheckingUpdate.value = false

            if (result.isSuccess) {
                val info = result.getOrNull()
                if (info != null && info.isUpdateAvailable) {
                    _updateInfo.value = info
                    if (isManual) {
                        _eventFlow.emit(SettingsEvent.ShowToast("New version ${info.latestVersion} found!"))
                    }
                } else {
                    if (isManual) {
                        _eventFlow.emit(SettingsEvent.ShowToast("You are using the latest version ($appVersion)"))
                    }
                }
            } else {
                if (isManual) {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to check update"
                    _eventFlow.emit(SettingsEvent.ShowToast(errorMsg))
                }
            }
        }
    }

    fun startDownloadUpdate(context: Context) {
        val update = _updateInfo.value ?: return
        if (update.downloadUrl.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(SettingsEvent.ShowToast("Download link not available"))
            }
            return
        }

        viewModelScope.launch {
            _downloadState.value = UpdateDownloadState.Downloading(0, 0, update.apkSizeBytes)
            val result = GitHubUpdateManager.downloadApk(
                context = context,
                downloadUrl = update.downloadUrl,
                fileName = update.apkFileName.ifBlank { "DailySpend-${update.latestVersion}.apk" }
            ) { progress, downloaded, total ->
                _downloadState.value = UpdateDownloadState.Downloading(progress, downloaded, total)
            }

            if (result.isSuccess) {
                val apkFile = result.getOrThrow()
                downloadedApkFile = apkFile
                _downloadState.value = UpdateDownloadState.ReadyToInstall(apkFile.absolutePath)
                // Trigger install
                installDownloadedApk(context)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Download failed"
                _downloadState.value = UpdateDownloadState.Error(error)
                _eventFlow.emit(SettingsEvent.ShowToast("Download error: $error"))
            }
        }
    }

    fun installDownloadedApk(context: Context) {
        val file = downloadedApkFile ?: return
        val installResult = GitHubUpdateManager.installApk(context, file)
        if (installResult.isFailure) {
            viewModelScope.launch {
                _eventFlow.emit(
                    SettingsEvent.ShowToast("Install error: ${installResult.exceptionOrNull()?.message}")
                )
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun updateGitHubRepo(repo: String) {
        viewModelScope.launch {
            repository.setGitHubRepo(repo)
            _eventFlow.emit(SettingsEvent.ShowToast("GitHub repo updated to: $repo"))
        }
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoCheckUpdates(enabled)
            _eventFlow.emit(
                SettingsEvent.ShowToast(
                    if (enabled) "Auto-check for updates enabled" else "Auto-check for updates disabled"
                )
            )
        }
    }

    fun updateCurrency(currencyInfo: CurrencyInfo) {
        viewModelScope.launch {
            repository.setCurrency(currencyInfo)
            _eventFlow.emit(SettingsEvent.ShowToast("Currency updated to ${currencyInfo.name} (${currencyInfo.symbol})"))
        }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch {
            repository.setThemePreference(theme)
        }
    }

    fun updateStartOfWeek(startOfWeek: StartOfWeek) {
        viewModelScope.launch {
            repository.setStartOfWeek(startOfWeek)
            _eventFlow.emit(SettingsEvent.ShowToast("Start of week updated to ${startOfWeek.displayName}"))
        }
    }

    fun updateMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(budget)
            _eventFlow.emit(SettingsEvent.ShowToast("Monthly budget updated"))
        }
    }

    fun updateCategoryBudget(categoryId: String, budget: Double?) {
        viewModelScope.launch {
            repository.updateCategoryBudget(categoryId, budget)
            _eventFlow.emit(SettingsEvent.ShowToast("Category budget updated"))
        }
    }

    fun addCustomCategory(name: String, type: TransactionType, iconName: String, colorHex: String, budget: Double?) {
        viewModelScope.launch {
            val newCategory = CategoryEntity(
                id = "cat_custom_${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                type = type,
                iconName = iconName,
                colorHex = colorHex,
                budgetAmount = budget,
                isDefault = false
            )
            repository.insertCategory(newCategory)
            _eventFlow.emit(SettingsEvent.ShowToast("Category '${name.trim()}' added"))
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _eventFlow.emit(SettingsEvent.ShowToast("Category deleted"))
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val csv = repository.generateCsv()
            CsvExporter.shareCsv(context, csv)
        }
    }

    fun createBackup(context: Context, onBackupGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val json = BackupRestoreHelper.createFullBackupJson(context)
            onBackupGenerated(json)
        }
    }

    fun restoreBackup(context: Context, jsonString: String) {
        viewModelScope.launch {
            val result = BackupRestoreHelper.restoreBackupJson(context, jsonString)
            if (result.isSuccess) {
                _eventFlow.emit(SettingsEvent.ShowToast("Restored ${result.getOrNull()} transactions successfully"))
            } else {
                _eventFlow.emit(SettingsEvent.ShowToast("Restore failed: ${result.exceptionOrNull()?.message}"))
            }
        }
    }

    fun populateSampleData() {
        viewModelScope.launch {
            repository.populateRealisticSampleData()
            _eventFlow.emit(SettingsEvent.ShowToast("Sample transactions added"))
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            repository.clearSampleData()
            _eventFlow.emit(SettingsEvent.ShowToast("Sample data removed"))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            _eventFlow.emit(SettingsEvent.ShowToast("All transactions cleared"))
        }
    }
}
