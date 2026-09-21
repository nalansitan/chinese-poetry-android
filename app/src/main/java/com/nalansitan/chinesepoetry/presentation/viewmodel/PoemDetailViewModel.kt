package com.nalansitan.chinesepoetry.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 诗词详情 ViewModel
 */
@HiltViewModel
class PoemDetailViewModel @Inject constructor(
    private val poemRepository: PoemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PoemDetailUiState())
    val uiState: StateFlow<PoemDetailUiState> = _uiState.asStateFlow()

    private var currentPoemId: String? = null

    /**
     * 加载诗词
     */
    fun loadPoem(poemId: String) {
        if (currentPoemId == poemId) return
        currentPoemId = poemId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // 加载诗词详情
                val poem = poemRepository.getPoemById(poemId)
                
                // 检查收藏状态
                val isFavorite = poemRepository.isFavorite(poemId).first()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        poem = poem,
                        isFavorite = isFavorite
                    )
                }

                // 添加到浏览历史
                poemRepository.addToHistory(poemId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }

        // 观察收藏状态变化
        viewModelScope.launch {
            poemRepository.isFavorite(poemId).collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        currentPoemId?.let { id ->
            viewModelScope.launch {
                try {
                    poemRepository.toggleFavorite(id)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    /**
     * 分享诗词
     */
    fun sharePoem(context: Context) {
        val poem = uiState.value.poem ?: return
        
        val shareText = buildString {
            appendLine(poem.getDisplayTitle())
            appendLine(poem.getAuthorDisplay())
            appendLine()
            appendLine(poem.getFullContent())
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, poem.title)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }

        val chooser = Intent.createChooser(intent, "分享诗词")
        context.startActivity(chooser)
    }

    /**
     * 切换横竖排显示
     */
    fun toggleLayout() {
        _uiState.update { 
            it.copy(isVerticalLayout = !it.isVerticalLayout) 
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 诗词详情 UI 状态
 */
data class PoemDetailUiState(
    val isLoading: Boolean = false,
    val poem: Poem? = null,
    val isFavorite: Boolean = false,
    val isVerticalLayout: Boolean = false,
    val error: String? = null
)
