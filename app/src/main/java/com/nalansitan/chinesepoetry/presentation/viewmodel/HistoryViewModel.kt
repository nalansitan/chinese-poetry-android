package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val poemRepository: PoemRepository
) : ViewModel() {

    val historyPoems: Flow<PagingData<Poem>> = poemRepository
        .getPagedHistoryPoems()
        .cachedIn(viewModelScope)

    val historyCount = poemRepository
        .getHistoryCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun clearHistory() {
        viewModelScope.launch {
            poemRepository.clearHistory()
        }
    }
}
