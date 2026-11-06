package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.manager.AppUpdateManager
import com.nalansitan.chinesepoetry.data.manager.AppUpdateState
import com.nalansitan.chinesepoetry.data.manager.DatabaseDownloadManager
import com.nalansitan.chinesepoetry.data.remote.api.AppVersionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AppUpdateUiStatus {
    data object Idle : AppUpdateUiStatus()
    data object Checking : AppUpdateUiStatus()
    data class NoUpdate(val currentVersionName: String) : AppUpdateUiStatus()
    data class HasUpdate(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val downloadUrl: String,
        val forceUpdate: Boolean = false
    ) : AppUpdateUiStatus()
    data class Downloading(val versionName: String, val progress: Int) : AppUpdateUiStatus()
    data class InstallPermissionRequired(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val downloadUrl: String,
        val forceUpdate: Boolean = false
    ) : AppUpdateUiStatus()
    data class ReadyToInstall(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val downloadUrl: String,
        val forceUpdate: Boolean = false
    ) : AppUpdateUiStatus()
    data class Error(val message: String) : AppUpdateUiStatus()
}

/**
 * 设置 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStoreManager,
    private val appUpdateManager: AppUpdateManager,
    private val downloadManager: DatabaseDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var availableData: com.nalansitan.chinesepoetry.data.remote.DatabaseRelease? = null
    private var dataOperation: kotlinx.coroutines.Job? = null
    val dataDownloadState = downloadManager.downloadState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.update { it.copy(appVersionName = appUpdateManager.getCurrentVersionName()) }

        viewModelScope.launch {
            // 加载字体大小
            dataStore.fontSize.collect { size ->
                _uiState.update { it.copy(fontSize = size) }
            }
        }

        viewModelScope.launch {
            // 加载竖排设置
            dataStore.isVerticalLayout.collect { isVertical ->
                _uiState.update { it.copy(isVerticalLayout = isVertical) }
            }
        }

        viewModelScope.launch {
            // 加载夜间模式设置
            dataStore.isDarkMode.collect { mode ->
                _uiState.update { it.copy(darkMode = mode) }
            }
        }

        viewModelScope.launch {
            combine(
                dataStore.hasPendingAppUpdate,
                dataStore.availableAppVersionCode,
                dataStore.availableAppVersionName,
                dataStore.availableAppChangelog,
                dataStore.availableAppDownloadUrl,
                dataStore.availableAppForceUpdate
            ) { values ->
                PendingAppUpdateInfo(
                    hasUpdate = values[0] as Boolean,
                    versionCode = values[1] as Int,
                    versionName = values[2] as String,
                    changelog = values[3] as String,
                    downloadUrl = values[4] as String,
                    forceUpdate = values[5] as Boolean
                )
            }.collect { pendingUpdate ->
                _uiState.update { currentState ->
                    val syncedStatus = when {
                        currentState.appUpdateStatus is AppUpdateUiStatus.Downloading -> currentState.appUpdateStatus
                        currentState.appUpdateStatus is AppUpdateUiStatus.Checking -> currentState.appUpdateStatus
                        pendingUpdate.hasUpdate -> AppUpdateUiStatus.HasUpdate(
                            versionCode = pendingUpdate.versionCode,
                            versionName = pendingUpdate.versionName,
                            changelog = pendingUpdate.changelog,
                            downloadUrl = pendingUpdate.downloadUrl,
                            forceUpdate = pendingUpdate.forceUpdate
                        )
                        currentState.appUpdateStatus is AppUpdateUiStatus.ReadyToInstall -> {
                            if (pendingUpdate.hasUpdate) currentState.appUpdateStatus else AppUpdateUiStatus.Idle
                        }
                        else -> AppUpdateUiStatus.Idle
                    }
                    currentState.copy(
                        hasPendingAppUpdate = pendingUpdate.hasUpdate,
                        appUpdateStatus = syncedStatus
                    )
                }
            }
        }

        viewModelScope.launch {
            appUpdateManager.updateState.collect { state ->
                val uiStatus = when (state) {
                    is AppUpdateState.Idle -> AppUpdateUiStatus.Idle
                    is AppUpdateState.Checking -> AppUpdateUiStatus.Checking
                    is AppUpdateState.NoUpdate -> AppUpdateUiStatus.NoUpdate(state.currentVersionName)
                    is AppUpdateState.HasUpdate -> state.versionInfo.toHasUpdateUiStatus()
                    is AppUpdateState.Downloading -> AppUpdateUiStatus.Downloading(
                        versionName = state.versionInfo.versionName,
                        progress = state.progress
                    )
                    is AppUpdateState.InstallPermissionRequired -> state.versionInfo.toPermissionUiStatus()
                    is AppUpdateState.ReadyToInstall -> state.versionInfo.toReadyToInstallUiStatus()
                    is AppUpdateState.Error -> AppUpdateUiStatus.Error(state.message)
                }
                _uiState.update { it.copy(appUpdateStatus = uiStatus) }
            }
        }
    }

    /**
     * 设置字体大小
     */
    fun setFontSize(size: Int) {
        viewModelScope.launch {
            dataStore.setFontSize(size)
        }
    }

    /**
     * 设置竖排显示
     */
    fun setVerticalLayout(isVertical: Boolean) {
        viewModelScope.launch {
            dataStore.setVerticalLayout(isVertical)
        }
    }

    /**
     * 设置夜间模式
     */
    fun setDarkMode(mode: Int) {
        viewModelScope.launch {
            dataStore.setDarkMode(mode)
        }
    }

    fun checkForDataUpdate() {
        if (dataOperation?.isActive == true) return
        dataOperation = viewModelScope.launch {
            availableData = null
            downloadManager.resetState()
            _uiState.update { it.copy(dataUpdateMessage = "正在检查 GitHub 数据更新…", canUpdateData = false, checkingData = true) }
            try {
                val current = downloadManager.installedDataVersion()
                availableData = downloadManager.checkDataUpdate()
                _uiState.update { it.copy(
                    dataUpdateMessage = availableData?.let { data -> "发现数据版本 ${data.version}（当前 $current）" }
                        ?: "当前数据版本 $current，未发现新版",
                    canUpdateData = availableData != null
                ) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(dataUpdateMessage = "检查失败，请稍后重试：${e.message}") }
            } finally {
                _uiState.update { it.copy(checkingData = false) }
            }
        }
    }

    fun startDataUpdate() {
        val data = availableData ?: return
        if (dataOperation?.isActive == true) return
        dataOperation = viewModelScope.launch {
            _uiState.update { it.copy(checkingData = true) }
            try {
                if (downloadManager.updateDatabase(data)) {
                    availableData = null
                    _uiState.update { it.copy(dataUpdateMessage = "已更新至数据版本 ${data.version}", canUpdateData = false) }
                }
            } finally {
                _uiState.update { it.copy(checkingData = false) }
            }
        }
    }

    fun checkForAppUpdate() {
        viewModelScope.launch {
            appUpdateManager.checkForUpdate()
        }
    }

    fun startAppUpdate() {
        when (val status = _uiState.value.appUpdateStatus) {
            is AppUpdateUiStatus.HasUpdate -> {
                appUpdateManager.downloadAndInstall(status.toAppVersionResponse())
            }
            is AppUpdateUiStatus.ReadyToInstall -> {
                appUpdateManager.installDownloadedApk()
            }
            is AppUpdateUiStatus.InstallPermissionRequired -> {
                appUpdateManager.openInstallPermissionSettings()
            }
            else -> Unit
        }
    }

    fun resetAppUpdateStatus() {
        appUpdateManager.resetState()
    }

    /**
     * 清除本地数据库，返回是否成功
     */
    fun clearDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            downloadManager.deleteLocalDatabase()
            onComplete()
        }
    }

    private fun AppVersionResponse.toHasUpdateUiStatus(): AppUpdateUiStatus.HasUpdate {
        return AppUpdateUiStatus.HasUpdate(
            versionCode = versionCode,
            versionName = versionName,
            changelog = changelog,
            downloadUrl = downloadUrl,
            forceUpdate = forceUpdate
        )
    }

    private fun AppVersionResponse.toPermissionUiStatus(): AppUpdateUiStatus.InstallPermissionRequired {
        return AppUpdateUiStatus.InstallPermissionRequired(
            versionCode = versionCode,
            versionName = versionName,
            changelog = changelog,
            downloadUrl = downloadUrl,
            forceUpdate = forceUpdate
        )
    }

    private fun AppVersionResponse.toReadyToInstallUiStatus(): AppUpdateUiStatus.ReadyToInstall {
        return AppUpdateUiStatus.ReadyToInstall(
            versionCode = versionCode,
            versionName = versionName,
            changelog = changelog,
            downloadUrl = downloadUrl,
            forceUpdate = forceUpdate
        )
    }

    private fun AppUpdateUiStatus.HasUpdate.toAppVersionResponse(): AppVersionResponse {
        return AppVersionResponse(
            versionCode = versionCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            changelog = changelog,
            forceUpdate = forceUpdate,
            fileSize = 0L,
            releaseTime = ""
        )
    }

    private fun AppUpdateUiStatus.ReadyToInstall.toAppVersionResponse(): AppVersionResponse {
        return AppVersionResponse(
            versionCode = versionCode,
            versionName = versionName,
            downloadUrl = downloadUrl,
            changelog = changelog,
            forceUpdate = forceUpdate,
            fileSize = 0L,
            releaseTime = ""
        )
    }
}

/**
 * 设置 UI 状态
 */
data class SettingsUiState(
    val dataUpdateMessage: String = "从 GitHub 检查新版诗词数据",
    val canUpdateData: Boolean = false,
    val checkingData: Boolean = false,
    val fontSize: Int = 1,
    val isVerticalLayout: Boolean = false,
    val darkMode: Int = -1,
    val appVersionName: String = "1.0.0",
    val hasPendingAppUpdate: Boolean = false,
    val appUpdateStatus: AppUpdateUiStatus = AppUpdateUiStatus.Idle,
)

private data class PendingAppUpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
