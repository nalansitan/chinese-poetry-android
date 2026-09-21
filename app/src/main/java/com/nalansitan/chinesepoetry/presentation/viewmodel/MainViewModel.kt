package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.manager.AppUpdateManager
import com.nalansitan.chinesepoetry.data.manager.DataUpdateManager
import com.nalansitan.chinesepoetry.data.manager.DatabaseDownloadManager
import com.nalansitan.chinesepoetry.data.repository.DataUpdateRepository
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainActivity ViewModel
 * 用于检查数据库状态
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadManager: DatabaseDownloadManager,
    private val dataUpdateRepository: Lazy<DataUpdateRepository>,
    private val appUpdateManager: AppUpdateManager,
    private val dataStore: DataStoreManager
) : ViewModel() {

    val hasPendingUpdate: StateFlow<Boolean> = combine(
        dataStore.hasPendingUpdate,
        dataStore.hasPendingAppUpdate
    ) { hasDataUpdate, hasAppUpdate ->
        hasDataUpdate || hasAppUpdate
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    /**
     * 检查本地数据库是否存在
     */
    fun isDatabaseExists(): Boolean {
        return downloadManager.isDatabaseExists()
    }

    fun checkForUpdateSilently() {
        viewModelScope.launch {
            if (!isDatabaseExists()) return@launch
            val repository = dataUpdateRepository.get()
            if (!repository.shouldCheckForUpdate(DataUpdateManager.CHECK_INTERVAL_HOURS)) {
                appUpdateManager.checkForUpdateSilently()
                return@launch
            }

            when (repository.checkForUpdate()) {
                is com.nalansitan.chinesepoetry.data.repository.UpdateCheckResult.Error -> {
                    dataStore.setPendingUpdate(hasUpdate = false)
                    repository.updateLastCheckTime()
                }
                else -> {
                    repository.updateLastCheckTime()
                }
            }

            appUpdateManager.checkForUpdateSilently()
        }
    }
}
