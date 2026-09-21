package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏页 ViewModel
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val poemRepository: PoemRepository
) : ViewModel() {

    /**
     * 收藏的诗词列表
     */
    val favoritePoems: Flow<PagingData<Poem>> = poemRepository
        .getFavoritePoems()
        .cachedIn(viewModelScope)

    /**
     * 收藏数量
     */
    val favoriteCount = poemRepository
        .getFavoriteCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun removeFavorite(poemId: String) {
        viewModelScope.launch {
            poemRepository.toggleFavorite(poemId)
        }
    }
}
