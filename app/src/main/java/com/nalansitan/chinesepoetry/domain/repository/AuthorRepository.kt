package com.nalansitan.chinesepoetry.domain.repository

import androidx.paging.PagingData
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.domain.model.Author
import kotlinx.coroutines.flow.Flow

/**
 * 作者仓库接口
 */
interface AuthorRepository {

    /**
     * 根据ID获取作者
     */
    suspend fun getAuthorById(id: String): Author?

    /**
     * 根据姓名获取作者
     */
    suspend fun getAuthorByName(name: String): Author?

    /**
     * 分页获取所有作者
     */
    fun getAllAuthors(): Flow<PagingData<Author>>

    /**
     * 按朝代分页获取作者
     */
    fun getAuthorsByDynasty(dynasty: Dynasty): Flow<PagingData<Author>>

    /**
     * 搜索作者
     */
    fun searchAuthors(query: String): Flow<PagingData<Author>>

    /**
     * 获取热门作者
     */
    suspend fun getTopAuthors(limit: Int = 20): List<Author>

    /**
     * 获取总数
     */
    suspend fun getTotalCount(): Int

    /**
     * 按朝代获取数量
     */
    suspend fun getCountByDynasty(dynasty: Dynasty): Int
}
