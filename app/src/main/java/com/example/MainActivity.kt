package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.ThemePreference
import com.example.navigation.AppNavigation
import com.example.ui.components.AppUpdateDialog
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.DailySpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current

            DailySpendTheme(themePreference = settingsState.themePreference) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()

                    // Global App Update Dialog when an update is available
                    if (settingsState.updateInfo != null) {
                        AppUpdateDialog(
                            updateInfo = settingsState.updateInfo!!,
                            downloadState = settingsState.downloadState,
                            onDownloadClick = { settingsViewModel.startDownloadUpdate(context) },
                            onInstallClick = { settingsViewModel.installDownloadedApk(context) },
                            onDismiss = { settingsViewModel.dismissUpdateDialog() }
                        )
                    }
                }
            }
        }
    }
}
