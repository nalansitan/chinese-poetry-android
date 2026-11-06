package com.nalansitan.chinesepoetry.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.data.manager.DatabaseDownloadManager
import com.nalansitan.chinesepoetry.data.manager.DownloadState
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
    private val downloadManager: DatabaseDownloadManager
) : ViewModel() {

    val sourceDescription = downloadManager.sourceDescription

    val downloadState: StateFlow<DownloadState> = downloadManager.downloadState

    private val _databaseSize = MutableStateFlow("计算中...")
    val databaseSize: StateFlow<String> = _databaseSize.asStateFlow()

    init {
        checkDatabaseStatus()
        loadDatabaseSize()
    }

    private fun checkDatabaseStatus() {
        // 检查数据库是否已存在
        if (downloadManager.isDatabaseExists()) {
            _databaseSize.value = "已下载"
        }
    }

    private fun loadDatabaseSize() {
        viewModelScope.launch {
            if (downloadManager.isDatabaseExists()) return@launch
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

    fun importDatabase(uri: Uri) {
        viewModelScope.launch { downloadManager.importDatabase(uri) }
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
