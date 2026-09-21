package com.nalansitan.chinesepoetry.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.data.local.entity.PoemType
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * 诗词列表 ViewModel
 */
@HiltViewModel
class PoemListViewModel @Inject constructor(
    private val poemRepository: PoemRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // 从导航参数获取筛选条件
    private val typeFilter: String? = savedStateHandle["type"]
    private val dynastyFilter: String? = savedStateHandle["dynasty"]
    private val authorFilter: String? = savedStateHandle["author"]

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * 诗词列表流
     */
    val poems: Flow<PagingData<Poem>> = _searchQuery
        .flatMapLatest { query ->
            when {
                query.isNotBlank() -> poemRepository.searchPoems(query)
                typeFilter != null -> {
                    val type = PoemType.fromValue(typeFilter)
                    poemRepository.getPoemsByType(type)
                }
                dynastyFilter != null -> {
                    val dynasty = Dynasty.fromValue(dynastyFilter)
                    poemRepository.getPoemsByDynasty(dynasty)
                }
                authorFilter != null -> {
                    poemRepository.getPoemsByAuthor(authorFilter)
                }
                else -> poemRepository.getPoemsByType(PoemType.TANG_SHI)
            }
        }
        .cachedIn(viewModelScope)

    /**
     * 更新搜索关键词
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 获取页面标题
     */
    fun getTitle(): String {
        return when {
            typeFilter != null -> {
                when (PoemType.fromValue(typeFilter)) {
                    PoemType.TANG_SHI -> "唐诗"
                    PoemType.SONG_SHI -> "宋诗"
                    PoemType.SONG_CI -> "宋词"
                    PoemType.SHI_JING -> "诗经"
                    PoemType.LUN_YU -> "论语"
                    PoemType.CHU_CI -> "楚辞"
                    PoemType.YUAN_QU -> "元曲"
                    PoemType.HUA_JIAN_JI -> "花间集"
                    PoemType.NAN_TANG -> "南唐二主词"
                    PoemType.NALAN_CI -> "纳兰性德"
                    PoemType.CAO_CAO -> "曹操诗集"
                    PoemType.YOU_MENG_YING -> "幽梦影"
                    PoemType.SI_SHU -> "四书五经"
                    PoemType.UNKNOWN -> "其他"
                }
            }
            dynastyFilter != null -> {
                Dynasty.fromValue(dynastyFilter).displayName + "诗"
            }
            authorFilter != null -> "$authorFilter 的诗"
            else -> "诗词列表"
        }
    }
}
