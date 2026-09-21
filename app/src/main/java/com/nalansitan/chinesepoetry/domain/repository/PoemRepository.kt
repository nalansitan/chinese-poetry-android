package com.nalansitan.chinesepoetry.domain.repository

import androidx.paging.PagingData
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.data.local.entity.PoemType
import com.nalansitan.chinesepoetry.domain.model.Poem
import kotlinx.coroutines.flow.Flow

/**
 * 诗词仓库接口
 */
interface PoemRepository {

    /**
     * 根据ID获取诗词
     */
    suspend fun getPoemById(id: String): Poem?

    /**
     * 观察诗词变化
     */
    fun observePoem(id: String): Flow<Poem?>

    /**
     * 获取随机诗词
     */
    suspend fun getRandomPoem(): Poem?

    /**
     * 获取随机推荐诗词
     */
    suspend fun getRandomPoems(limit: Int): List<Poem>

    /**
     * 按朝代获取随机推荐诗词
     */
    suspend fun getRandomPoemsByDynasty(dynasty: Dynasty, limit: Int): List<Poem>

    /**
     * 获取每日推荐
     */
    suspend fun getDailyPoem(): Poem?

    /**
     * 按类型分页获取诗词
     */
    fun getPoemsByType(type: PoemType): Flow<PagingData<Poem>>

    /**
     * 按朝代分页获取诗词
     */
    fun getPoemsByDynasty(dynasty: Dynasty): Flow<PagingData<Poem>>

    /**
     * 按作者分页获取诗词
     */
    fun getPoemsByAuthor(authorName: String): Flow<PagingData<Poem>>

    /**
     * 搜索诗词
     */
    fun searchPoems(query: String): Flow<PagingData<Poem>>

    /**
     * 获取收藏列表
     */
    fun getFavoritePoems(): Flow<PagingData<Poem>>

    /**
     * 按词牌名获取诗词
     */
    fun getPoemsByRhythmic(rhythmic: String): Flow<PagingData<Poem>>

    /**
     * 切换收藏状态
     */
    suspend fun toggleFavorite(poemId: String): Boolean

    /**
     * 检查是否已收藏
     */
    fun isFavorite(poemId: String): Flow<Boolean>

    /**
     * 添加到浏览历史
     */
    suspend fun addToHistory(poemId: String)

    /**
     * 获取浏览历史
     */
    fun getHistoryPoems(): Flow<List<Poem>>

    /**
     * 分页获取浏览历史
     */
    fun getPagedHistoryPoems(): Flow<PagingData<Poem>>

    /**
     * 获取收藏数量
     */
    fun getFavoriteCount(): Flow<Int>

    /**
     * 获取浏览数量
     */
    fun getHistoryCount(): Flow<Int>

    /**
     * 获取阅读天数
     */
    fun getReadingDaysCount(): Flow<Int>

    /**
     * 清空阅读历史
     */
    suspend fun clearHistory()

    /**
     * 获取总数
     */
    suspend fun getTotalCount(): Int

    /**
     * 按类型获取数量
     */
    suspend fun getCountByType(type: PoemType): Int
}
