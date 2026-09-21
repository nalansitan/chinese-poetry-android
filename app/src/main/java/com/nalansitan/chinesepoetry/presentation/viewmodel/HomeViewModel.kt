package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 首页 ViewModel
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val poemRepository: PoemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var dailyPoemJob: Job? = null

    init {
        observeRecentPoems()
        refreshDailyPoem()
    }

    private fun observeRecentPoems() {
        viewModelScope.launch {
            try {
                val recentPoems = poemRepository.getHistoryPoems()
                recentPoems.collect { poems ->
                    _uiState.update {
                        it.copy(
                            recentPoems = poems.take(5)
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun refreshDailyPoem() {
        dailyPoemJob?.cancel()
        dailyPoemJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingDailyPoem = true, error = null) }

            try {
                val dailyPoem = poemRepository.getDailyPoem()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshingDailyPoem = false,
                        dailyPoem = dailyPoem
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshingDailyPoem = false,
                        error = e.message
                    )
                }
            }
        }
    }
}

/**
 * 首页 UI 状态
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshingDailyPoem: Boolean = false,
    val dailyPoem: Poem? = null,
    val recentPoems: List<Poem> = emptyList(),
    val error: String? = null
)
