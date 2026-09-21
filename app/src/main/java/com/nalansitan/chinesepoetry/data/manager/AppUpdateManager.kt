package com.nalansitan.chinesepoetry.data.manager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import com.nalansitan.chinesepoetry.data.remote.api.AppVersionResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiServiceProvider: ApiServiceProvider,
    private val dataStore: DataStoreManager
) {
    companion object {
        const val CHECK_INTERVAL_HOURS = 24L
    }

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private var currentDownloadId: Long? = null
    private var currentDownloadFile: File? = null
    private var currentVersionInfo: AppVersionResponse? = null
    private var receiverRegistered = false
    private var progressJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val downloadReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId != currentDownloadId) return

            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) {
                    _updateState.value = AppUpdateState.Error("下载结果读取失败")
                    return
                }

                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    stopProgressTracking()
                    val apkFile = currentDownloadFile
                    if (apkFile != null && apkFile.exists()) {
                        currentVersionInfo?.let {
                            _updateState.value = AppUpdateState.ReadyToInstall(it)
                        }
                        installApk(apkFile)
                    } else {
                        _updateState.value = AppUpdateState.Error("安装包文件不存在")
                    }
                } else {
                    stopProgressTracking()
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    _updateState.value = AppUpdateState.Error("下载失败，错误码: $reason")
                }
            }
        }
    }

    suspend fun checkForUpdate() {
        _updateState.value = AppUpdateState.Checking

        try {
            val response = apiServiceProvider.getPublicApiService().getLatestAppVersion()
            if (!response.isSuccessful) {
                _updateState.value = AppUpdateState.Error("检查更新失败: ${response.code()}")
                return
            }

            val remoteVersion = response.body()
            if (remoteVersion == null) {
                _updateState.value = AppUpdateState.Error("版本信息为空")
                return
            }

            if (remoteVersion.versionCode > getCurrentVersionCode()) {
                dataStore.setPendingAppUpdate(
                    hasUpdate = true,
                    versionCode = remoteVersion.versionCode,
                    versionName = remoteVersion.versionName,
                    changelog = remoteVersion.changelog,
                    downloadUrl = remoteVersion.downloadUrl,
                    forceUpdate = remoteVersion.forceUpdate
                )
                dataStore.updateAppLastCheckTime()
                _updateState.value = AppUpdateState.HasUpdate(remoteVersion)
            } else {
                dataStore.setPendingAppUpdate(hasUpdate = false)
                dataStore.updateAppLastCheckTime()
                _updateState.value = AppUpdateState.NoUpdate(getCurrentVersionName())
            }
        } catch (e: Exception) {
            _updateState.value = AppUpdateState.Error(e.message ?: "检查更新失败")
        }
    }

    fun downloadAndInstall(versionInfo: AppVersionResponse) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            _updateState.value = AppUpdateState.InstallPermissionRequired(versionInfo)
            return
        }

        try {
            registerReceiverIfNeeded()
            currentVersionInfo = versionInfo

            val targetFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "chinese-poetry-android-${versionInfo.versionName}.apk"
            )
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = DownloadManager.Request(Uri.parse(resolveDownloadUrl(versionInfo.downloadUrl)))
                .setTitle("古诗词应用更新")
                .setDescription("正在下载 ${versionInfo.versionName}")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationUri(Uri.fromFile(targetFile))

            currentDownloadFile = targetFile
            currentDownloadId = downloadManager.enqueue(request)
            _updateState.value = AppUpdateState.Downloading(versionInfo, 0)
            startProgressTracking(versionInfo)
        } catch (e: Exception) {
            _updateState.value = AppUpdateState.Error(e.message ?: "启动下载失败")
        }
    }

    fun installDownloadedApk() {
        val apkFile = currentDownloadFile
        if (apkFile != null && apkFile.exists()) {
            installApk(apkFile)
            currentVersionInfo?.let {
                _updateState.value = AppUpdateState.ReadyToInstall(it)
            }
        } else {
            _updateState.value = AppUpdateState.Error("安装包文件不存在，请重新下载")
        }
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getCurrentVersionName(): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName ?: "1.0.0"
    }

    suspend fun checkForUpdateSilently() {
        if (!shouldCheckForUpdate()) return

        try {
            val response = apiServiceProvider.getPublicApiService().getLatestAppVersion()
            if (!response.isSuccessful) {
                clearPendingUpdateSilently()
                return
            }

            val remoteVersion = response.body()
            if (remoteVersion == null) {
                clearPendingUpdateSilently()
                return
            }

            if (remoteVersion.versionCode > getCurrentVersionCode()) {
                dataStore.setPendingAppUpdate(
                    hasUpdate = true,
                    versionCode = remoteVersion.versionCode,
                    versionName = remoteVersion.versionName,
                    changelog = remoteVersion.changelog,
                    downloadUrl = remoteVersion.downloadUrl,
                    forceUpdate = remoteVersion.forceUpdate
                )
            } else {
                dataStore.setPendingAppUpdate(hasUpdate = false)
            }
            dataStore.updateAppLastCheckTime()
        } catch (_: Exception) {
            clearPendingUpdateSilently()
        }
    }

    fun resetState() {
        _updateState.value = AppUpdateState.Idle
    }

    private fun startProgressTracking(versionInfo: AppVersionResponse) {
        stopProgressTracking()
        val downloadId = currentDownloadId ?: return
        progressJob = scope.launch {
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) return@use

                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (downloaded > 0 && total > 0) {
                        ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                    } else {
                        0
                    }

                    when (status) {
                        DownloadManager.STATUS_PENDING,
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PAUSED -> {
                            _updateState.value = AppUpdateState.Downloading(versionInfo, progress)
                        }
                        DownloadManager.STATUS_SUCCESSFUL,
                        DownloadManager.STATUS_FAILED -> return@launch
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            context,
            downloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    private fun installApk(apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
        currentVersionInfo?.let {
            _updateState.value = AppUpdateState.ReadyToInstall(it)
        }
    }

    private fun resolveDownloadUrl(downloadUrl: String): String {
        if (downloadUrl.startsWith("http://") || downloadUrl.startsWith("https://")) {
            return downloadUrl
        }

        val baseUrl = apiServiceProvider.getCurrentBaseUrl()
        val normalizedPath = downloadUrl.removePrefix("/")
        return baseUrl.toHttpUrlOrNull()
            ?.resolve(normalizedPath)
            ?.toString()
            ?: error("无效的下载地址: $downloadUrl")
    }

    private fun getCurrentVersionCode(): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    private suspend fun shouldCheckForUpdate(): Boolean {
        val lastCheckTime = dataStore.appLastCheckTime.first()
        if (lastCheckTime == 0L) return true
        val intervalMillis = CHECK_INTERVAL_HOURS * 60 * 60 * 1000
        return System.currentTimeMillis() - lastCheckTime >= intervalMillis
    }

    private suspend fun clearPendingUpdateSilently() {
        dataStore.setPendingAppUpdate(hasUpdate = false)
        dataStore.updateAppLastCheckTime()
    }
}

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data object Checking : AppUpdateState()
    data class NoUpdate(val currentVersionName: String) : AppUpdateState()
    data class HasUpdate(val versionInfo: AppVersionResponse) : AppUpdateState()
    data class Downloading(val versionInfo: AppVersionResponse, val progress: Int) : AppUpdateState()
    data class InstallPermissionRequired(val versionInfo: AppVersionResponse) : AppUpdateState()
    data class ReadyToInstall(val versionInfo: AppVersionResponse) : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}
