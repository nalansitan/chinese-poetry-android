package com.nalansitan.chinesepoetry.data.manager

import com.nalansitan.chinesepoetry.data.repository.DataUpdateRepository
import com.nalansitan.chinesepoetry.data.repository.UpdateCheckResult
import com.nalansitan.chinesepoetry.data.repository.UpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据更新管理器
 * 负责定期检查更新和管理更新流程
 */
@Singleton
class DataUpdateManager @Inject constructor(
    private val repository: DataUpdateRepository
) {
    companion object {
        const val CHECK_INTERVAL_HOURS = 24L
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /**
     * 手动检查更新
     */
    suspend fun checkForUpdate() {
        _updateState.value = UpdateState.Checking
        
        val result = repository.checkForUpdate()
        
        when (result) {
            is UpdateCheckResult.HasUpdate -> {
                _updateState.value = UpdateState.HasUpdate(
                    versionInfo = VersionInfo(
                        version = result.newVersion,
                        versionName = result.versionName,
                        changelog = result.changelog,
                        isForceUpdate = result.isForceUpdate
                    )
                )
            }
            is UpdateCheckResult.NoUpdate -> {
                _updateState.value = UpdateState.NoUpdate
            }
            is UpdateCheckResult.Error -> {
                _updateState.value = UpdateState.Error(result.message)
            }
        }
        
        repository.updateLastCheckTime()
    }

    /**
     * 执行更新
     */
    suspend fun performUpdate() {
        _updateState.value = UpdateState.Downloading(0)
        
        // 模拟进度更新（实际应根据下载进度更新）
        val result = repository.performUpdate { progress ->
            _updateState.value = UpdateState.Downloading(progress)
        }
        
        when (result) {
            is UpdateResult.Success -> {
                _updateState.value = UpdateState.Success
            }
            is UpdateResult.Error -> {
                _updateState.value = UpdateState.Error(result.message)
            }
        }
    }

    /**
     * 获取单个诗词的扩展数据
     */
    suspend fun fetchExtensionData(poemId: String): Boolean {
        return repository.fetchExtensionData(poemId)
    }

}

/**
 * 更新状态
 */
sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data object NoUpdate : UpdateState()
    data class HasUpdate(
        val versionInfo: VersionInfo
    ) : UpdateState()
    data class Downloading(
        val progress: Int
    ) : UpdateState()
    data object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * 版本信息
 */
data class VersionInfo(
    val version: Int,
    val versionName: String,
    val changelog: String,
    val isForceUpdate: Boolean = false
)
