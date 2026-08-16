package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.domain.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object GitHubUpdateManager {

    private const val GITHUB_API_URL = "https://api.github.com/repos"

    suspend fun checkForUpdate(
        context: Context,
        repoOwner: String,
        repoName: String,
        currentVersionName: String
    ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val endpoint = "$GITHUB_API_URL/$repoOwner/$repoName/releases/latest"
            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "DailySpend-AndroidApp")
                connectTimeout = 10000
                readTimeout = 15000
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("GitHub API returned HTTP $responseCode (${connection.responseMessage}). Make sure the repository '$repoOwner/$repoName' exists and has releases.")
                )
            }

            val jsonResponse = connection.inputStream.bufferedReader().use { it.readText() }
            val releaseJson = JSONObject(jsonResponse)

            val tagName = releaseJson.optString("tag_name", "v1.0.0")
            val releaseTitle = releaseJson.optString("name", tagName)
            val releaseBody = releaseJson.optString("body", "No changelog provided.")
            val publishedAt = releaseJson.optString("published_at", "")

            var downloadUrl = ""
            var apkName = "DailySpend-$tagName.apk"
            var apkSize = 0L

            val assets = releaseJson.optJSONArray("assets")
            if (assets != null && assets.length() > 0) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            // If no APK asset is directly attached, fallback to html_url or release asset
            if (downloadUrl.isEmpty()) {
                downloadUrl = releaseJson.optString("html_url", "")
            }

            val isNewer = isVersionNewer(tagName, currentVersionName)

            val updateInfo = AppUpdateInfo(
                latestVersion = tagName,
                currentVersion = currentVersionName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseBody,
                downloadUrl = downloadUrl,
                apkFileName = apkName,
                apkSizeBytes = apkSize,
                publishedDate = publishedAt,
                isUpdateAvailable = isNewer
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (progressPercent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetDir = context.externalCacheDir ?: context.cacheDir
            val destinationFile = File(targetDir, fileName)

            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DailySpend-AndroidApp")
                connectTimeout = 15000
                readTimeout = 30000
            }

            val responseCode = connection.responseCode
            // Follow redirects if needed (GitHub asset downloads usually redirect to AWS S3)
            val finalConnection: HttpURLConnection = if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER
            ) {
                val newUrl = connection.getHeaderField("Location")
                (URL(newUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "DailySpend-AndroidApp")
                }
            } else {
                connection
            }

            val totalLength = finalConnection.contentLengthLong
            finalConnection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalDownloaded = 0L
                    var lastReportedPercent = -1

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead
                        if (totalLength > 0) {
                            val percent = ((totalDownloaded * 100) / totalLength).toInt()
                            if (percent != lastReportedPercent) {
                                lastReportedPercent = percent
                                onProgress(percent, totalDownloaded, totalLength)
                            }
                        }
                    }
                    output.flush()
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApk(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                return Result.failure(Exception("Downloaded APK file not found"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isVersionNewer(latestTag: String, currentVersion: String): Boolean {
        try {
            val cleanLatest = latestTag.trim().removePrefix("v").removePrefix("V")
            val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")

            val latestParts = cleanLatest.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = cleanCurrent.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        } catch (e: Exception) {
            return latestTag != currentVersion
        }
    }
}
