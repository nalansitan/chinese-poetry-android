package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.BuildConfig
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.manager.AppUpdateManager
import com.nalansitan.chinesepoetry.data.manager.AppUpdateState
import com.nalansitan.chinesepoetry.data.manager.DataUpdateManager
import com.nalansitan.chinesepoetry.data.manager.DatabaseDownloadManager
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import com.nalansitan.chinesepoetry.data.remote.api.AppVersionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 更新状态
 */
sealed class UpdateStatus {
    data object Idle : UpdateStatus()
    data object Checking : UpdateStatus()
    data object NoUpdate : UpdateStatus()
    data class HasUpdate(
        val newVersion: Int,
        val versionName: String,
        val changelog: String
    ) : UpdateStatus()
    data class Downloading(val progress: Int) : UpdateStatus()
    data object Success : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

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
    private val dataUpdateManager: DataUpdateManager,
    private val appUpdateManager: AppUpdateManager,
    private val apiServiceProvider: ApiServiceProvider,
    private val downloadManager: DatabaseDownloadManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
            // 加载数据版本
            dataStore.dataVersionName.collect { version ->
                _uiState.update { it.copy(dataVersion = version) }
            }
        }

        viewModelScope.launch {
            // 加载服务器配置
            dataStore.serverUrl.collect { url ->
                _uiState.update {
                    it.copy(serverUrl = if (url == BuildConfig.BASE_URL) "" else url)
                }
            }
        }

        viewModelScope.launch {
            dataStore.serverUsername.collect { username ->
                _uiState.update {
                    it.copy(serverUsername = if (username == BuildConfig.API_USERNAME) "" else username)
                }
            }
        }

        viewModelScope.launch {
            dataStore.serverPassword.collect { password ->
                _uiState.update {
                    it.copy(serverPassword = if (password == BuildConfig.API_PASSWORD) "" else password)
                }
            }
        }

        viewModelScope.launch {
            // 监听更新状态并转换为 UI 状态
            dataUpdateManager.updateState.collect { state ->
                val uiStatus = when (state) {
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.Idle -> UpdateStatus.Idle
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.Checking -> UpdateStatus.Checking
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.NoUpdate -> UpdateStatus.NoUpdate
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.HasUpdate -> UpdateStatus.HasUpdate(
                        newVersion = state.versionInfo.version,
                        versionName = state.versionInfo.versionName,
                        changelog = state.versionInfo.changelog
                    )
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.Downloading -> UpdateStatus.Downloading(
                        progress = state.progress
                    )
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.Success -> UpdateStatus.Success
                    is com.nalansitan.chinesepoetry.data.manager.UpdateState.Error -> UpdateStatus.Error(
                        message = state.message
                    )
                }
                _uiState.update { it.copy(updateStatus = uiStatus) }
            }
        }

        viewModelScope.launch {
            combine(
                dataStore.hasPendingUpdate,
                dataStore.availableDataVersion,
                dataStore.availableDataVersionName,
                dataStore.availableChangelog
            ) { hasUpdate, version, versionName, changelog ->
                PendingUpdateInfo(
                    hasUpdate = hasUpdate,
                    version = version,
                    versionName = versionName,
                    changelog = changelog
                )
            }.collect { pendingUpdate ->
                _uiState.update { currentState ->
                    val syncedStatus = when {
                        currentState.updateStatus is UpdateStatus.Downloading -> currentState.updateStatus
                        currentState.updateStatus is UpdateStatus.Checking -> currentState.updateStatus
                        pendingUpdate.hasUpdate -> UpdateStatus.HasUpdate(
                            newVersion = pendingUpdate.version,
                            versionName = pendingUpdate.versionName,
                            changelog = pendingUpdate.changelog
                        )
                        currentState.updateStatus is UpdateStatus.Success -> UpdateStatus.Success
                        else -> UpdateStatus.Idle
                    }
                    currentState.copy(
                        hasPendingUpdate = pendingUpdate.hasUpdate,
                        updateStatus = syncedStatus
                    )
                }
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

    /**
     * 检查数据更新
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            dataUpdateManager.checkForUpdate()
        }
    }

    /**
     * 开始更新数据
     */
    fun startUpdate() {
        viewModelScope.launch {
            dataUpdateManager.performUpdate()
        }
    }

    /**
     * 重置更新状态
     */
    fun resetUpdateStatus() {
        _uiState.update { it.copy(updateStatus = UpdateStatus.Idle) }
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

    /**
     * 设置服务器配置
     */
    fun setServerConfig(url: String, username: String, password: String) {
        viewModelScope.launch {
            dataStore.setServerConfig(url, username, password)
            // 重新创建 API 服务
            apiServiceProvider.recreateApiService()
        }
    }

    /**
     * 恢复默认服务器配置
     */
    fun resetServerConfig() {
        viewModelScope.launch {
            dataStore.clearServerConfig()
            // 重新创建 API 服务
            apiServiceProvider.recreateApiService()
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
    val fontSize: Int = 1,
    val isVerticalLayout: Boolean = false,
    val darkMode: Int = -1,
    val dataVersion: String = "1.0.0",
    val appVersionName: String = "1.0.0",
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    val hasPendingUpdate: Boolean = false,
    val hasPendingAppUpdate: Boolean = false,
    val appUpdateStatus: AppUpdateUiStatus = AppUpdateUiStatus.Idle,
    // 服务器配置
    val serverUrl: String = "",
    val serverUsername: String = "",
    val serverPassword: String = ""
)

private data class PendingUpdateInfo(
    val hasUpdate: Boolean,
    val version: Int,
    val versionName: String,
    val changelog: String
)

private data class PendingAppUpdateInfo(
    val hasUpdate: Boolean,
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val forceUpdate: Boolean
)
