package com.example.domain.model

data class AppUpdateInfo(
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val publishedDate: String,
    val isUpdateAvailable: Boolean
)

sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progressPercent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState()
    data class ReadyToInstall(val apkFilePath: String) : UpdateDownloadState()
    data class Error(val errorMessage: String) : UpdateDownloadState()
}
