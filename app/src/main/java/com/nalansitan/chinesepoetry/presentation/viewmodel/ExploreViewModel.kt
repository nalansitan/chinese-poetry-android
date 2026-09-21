package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.data.local.entity.PoemType
import com.nalansitan.chinesepoetry.domain.model.Author
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.AuthorRepository
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 发现页 ViewModel
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val poemRepository: PoemRepository,
    private val authorRepository: AuthorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    // 搜索结果流
    val searchResults: Flow<PagingData<Poem>> = _uiState
        .flatMapLatest { state ->
            when {
                state.searchQuery.isNotBlank() -> {
                    poemRepository.searchPoems(state.searchQuery)
                }
                state.selectedDynasty != null -> {
                    poemRepository.getPoemsByDynasty(state.selectedDynasty)
                }
                else -> {
                    poemRepository.getPoemsByType(PoemType.TANG_SHI)
                }
            }
        }
        .cachedIn(viewModelScope)

    init {
        loadTopAuthors()
        refreshRecommendations()
    }

    private fun loadTopAuthors() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAuthors = true) }
            
            try {
                val authors = authorRepository.getTopAuthors(20)
                _uiState.update {
                    it.copy(
                        isLoadingAuthors = false,
                        topAuthors = authors
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingAuthors = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onDynastySelected(dynasty: Dynasty) {
        val selectedDynasty = if (_uiState.value.selectedDynasty == dynasty) null else dynasty
        _uiState.update { it.copy(selectedDynasty = selectedDynasty) }
        if (_uiState.value.searchQuery.isBlank()) {
            refreshRecommendations()
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingRecommendations = true,
                    recommendationsError = null
                )
            }

            try {
                val poems = _uiState.value.selectedDynasty?.let { dynasty ->
                    poemRepository.getRandomPoemsByDynasty(dynasty, RECOMMENDATION_COUNT)
                } ?: poemRepository.getRandomPoems(RECOMMENDATION_COUNT)
                _uiState.update {
                    it.copy(
                        isLoadingRecommendations = false,
                        recommendedPoems = poems
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingRecommendations = false,
                        recommendationsError = e.message,
                        recommendedPoems = emptyList()
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, recommendationsError = null) }
    }

    companion object {
        private const val RECOMMENDATION_COUNT = 10
    }
}

/**
 * 发现页 UI 状态
 */
data class ExploreUiState(
    val searchQuery: String = "",
    val selectedDynasty: Dynasty? = null,
    val topAuthors: List<Author> = emptyList(),
    val recommendedPoems: List<Poem> = emptyList(),
    val isLoadingAuthors: Boolean = false,
    val isLoadingRecommendations: Boolean = false,
    val recommendationsError: String? = null,
    val error: String? = null
)
