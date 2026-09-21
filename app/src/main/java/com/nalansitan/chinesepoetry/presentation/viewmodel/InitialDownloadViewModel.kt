package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.BuildConfig
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.manager.DatabaseDownloadManager
import com.nalansitan.chinesepoetry.data.manager.DownloadState
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * 初始下载 ViewModel
 */
@HiltViewModel
class InitialDownloadViewModel @Inject constructor(
    private val downloadManager: DatabaseDownloadManager,
    private val dataStore: DataStoreManager,
    private val apiServiceProvider: ApiServiceProvider
) : ViewModel() {

    val downloadState: StateFlow<DownloadState> = downloadManager.downloadState

    private val _databaseSize = MutableStateFlow("计算中...")
    val databaseSize: StateFlow<String> = _databaseSize.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _serverUsername = MutableStateFlow("")
    val serverUsername: StateFlow<String> = _serverUsername.asStateFlow()

    private val _serverPassword = MutableStateFlow("")
    val serverPassword: StateFlow<String> = _serverPassword.asStateFlow()

    val defaultServerUrl: String = BuildConfig.BASE_URL
    val defaultServerPassword: String = BuildConfig.API_PASSWORD

    init {
        checkDatabaseStatus()
        loadDatabaseSize()
        loadServerConfig()
    }

    private fun loadServerConfig() {
        viewModelScope.launch {
            dataStore.serverUrl.collect { savedUrl ->
                _serverUrl.value = if (savedUrl == BuildConfig.BASE_URL) "" else savedUrl
            }
        }
        viewModelScope.launch {
            dataStore.serverUsername.collect { savedUsername ->
                _serverUsername.value = if (savedUsername == BuildConfig.API_USERNAME) "" else savedUsername
            }
        }
        viewModelScope.launch {
            dataStore.serverPassword.collect { savedPassword ->
                _serverPassword.value = if (savedPassword == BuildConfig.API_PASSWORD) "" else savedPassword
            }
        }
    }

    fun setServerConfig(url: String, username: String, password: String) {
        viewModelScope.launch {
            dataStore.setServerConfig(url, username, password)
            apiServiceProvider.recreateApiService()
        }
    }

    fun resetServerConfig() {
        viewModelScope.launch {
            dataStore.clearServerConfig()
            apiServiceProvider.recreateApiService()
        }
    }

    private fun checkDatabaseStatus() {
        // 检查数据库是否已存在
        if (downloadManager.isDatabaseExists()) {
            _databaseSize.value = "已下载"
        }
    }

    private fun loadDatabaseSize() {
        viewModelScope.launch {
            val size = downloadManager.getRemoteDatabaseSize()
            _databaseSize.value = size?.let { formatFileSize(it) } ?: "未知"
        }
    }

    /**
     * 开始下载
     */
    fun startDownload() {
        viewModelScope.launch {
            downloadManager.downloadDatabase()
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        val df = DecimalFormat("#.00")
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> df.format(size / 1024.0) + " KB"
            size < 1024 * 1024 * 1024 -> df.format(size / (1024.0 * 1024.0)) + " MB"
            else -> df.format(size / (1024.0 * 1024.0 * 1024.0)) + " GB"
        }
    }
}
