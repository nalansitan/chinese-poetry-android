package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 搜索 ViewModel
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val poemRepository: PoemRepository,
    private val dataStore: DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /**
     * 搜索结果流
     */
    val searchResults: Flow<PagingData<Poem>> = _uiState
        .map { it.query.trim() }
        .debounce(300) // 防抖 300ms
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(PagingData.empty())
            } else {
                addToHistory(query)
                poemRepository.searchPoems(query)
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadSearchHistory()
    }

    /**
     * 更新搜索关键词
     */
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /**
     * 清除搜索历史
     */
    fun clearHistory() {
        viewModelScope.launch {
            dataStore.clearSearchHistory()
            _uiState.update { it.copy(searchHistory = emptyList()) }
        }
    }

    private fun loadSearchHistory() {
        viewModelScope.launch {
            dataStore.searchHistory.collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    private fun addToHistory(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            val currentHistory = _uiState.value.searchHistory.toMutableList()
            // 移除重复项
            currentHistory.remove(query)
            // 添加到开头
            currentHistory.add(0, query)
            // 限制历史记录数量
            val limitedHistory = currentHistory.take(20)

            _uiState.update { it.copy(searchHistory = limitedHistory) }
            dataStore.setSearchHistory(limitedHistory)
        }
    }
}

/**
 * 搜索 UI 状态
 */
data class SearchUiState(
    val query: String = "",
    val searchHistory: List<String> = emptyList()
)
