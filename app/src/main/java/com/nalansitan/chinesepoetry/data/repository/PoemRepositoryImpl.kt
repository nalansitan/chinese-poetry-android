package com.nalansitan.chinesepoetry.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.nalansitan.chinesepoetry.data.local.dao.PoemDao
import com.nalansitan.chinesepoetry.data.local.dao.UserActivityDao
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.data.local.entity.PoemEntity
import com.nalansitan.chinesepoetry.data.local.entity.PoemType
import com.nalansitan.chinesepoetry.domain.model.Poem
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import androidx.sqlite.db.SimpleSQLiteQuery

/**
 * 诗词仓库实现
 */
@Singleton
class PoemRepositoryImpl @Inject constructor(
    private val poemDao: PoemDao,
    private val userActivityDao: UserActivityDao
) : PoemRepository {

    override suspend fun getPoemById(id: String): Poem? {
        return poemDao.getById(id)?.toDomainModel()
    }

    override fun observePoem(id: String): Flow<Poem?> {
        return poemDao.observeById(id).map { it?.toDomainModel() }
    }

    override suspend fun getRandomPoem(): Poem? {
        return poemDao.getRandom()?.toDomainModel()
    }

    override suspend fun getRandomPoems(limit: Int): List<Poem> {
        return poemDao.getRandomList(limit).map { it.toDomainModel() }
    }

    override suspend fun getRandomPoemsByDynasty(dynasty: Dynasty, limit: Int): List<Poem> {
        return poemDao.getRandomListByDynasty(
            dynastyValue = dynasty.value,
            dynastyDisplayName = dynasty.displayName,
            limit = limit
        ).map { it.toDomainModel() }
    }

    override suspend fun getDailyPoem(): Poem? {
        // 根据日期生成固定的每日推荐
        val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val count = poemDao.getCount()
        if (count == 0) return null
        val seed = dayOfYear % count
        // 这里简化处理，实际可以按ID偏移获取
        return getRandomPoem()
    }

    override fun getPoemsByType(type: PoemType): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { poemDao.getByType(type.value) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getPoemsByDynasty(dynasty: Dynasty): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { poemDao.getByDynasty(dynasty.value, dynasty.displayName) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getPoemsByAuthor(authorName: String): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { poemDao.getByAuthor(authorName) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun searchPoems(query: String): Flow<PagingData<Poem>> {
        val searchQuery = buildSearchQuery(query)
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { poemDao.searchRaw(searchQuery) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getFavoritePoems(): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { userActivityDao.getPagedPoemsByType("favorite") }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getPagedHistoryPoems(): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { userActivityDao.getPagedPoemsByType("history") }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getPoemsByRhythmic(rhythmic: String): Flow<PagingData<Poem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { poemDao.getByRhythmic(rhythmic) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override suspend fun toggleFavorite(poemId: String): Boolean {
        return userActivityDao.toggleFavorite(poemId)
    }

    override fun isFavorite(poemId: String): Flow<Boolean> {
        return userActivityDao.isFavorite(poemId)
    }

    override suspend fun addToHistory(poemId: String) {
        userActivityDao.addToHistory(poemId)
    }

    override fun getHistoryPoems(): Flow<List<Poem>> {
        return userActivityDao.observePoemsByType("history").map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getFavoriteCount(): Flow<Int> {
        return userActivityDao.getCountByType("favorite")
    }

    override fun getHistoryCount(): Flow<Int> {
        return userActivityDao.getHistoryCount()
    }

    override fun getReadingDaysCount(): Flow<Int> {
        return userActivityDao.getReadingDaysCount()
    }

    override suspend fun clearHistory() {
        userActivityDao.deleteAllByType("history")
    }

    override suspend fun getTotalCount(): Int {
        return poemDao.getCount()
    }

    override suspend fun getCountByType(type: PoemType): Int {
        return poemDao.getCountByType(type.value)
    }

    /**
     * 转换实体到领域模型
     */
    private fun PoemEntity.toDomainModel(): Poem {
        return Poem(
            id = id,
            title = title,
            authorName = authorName,
            authorId = authorId,
            dynasty = Dynasty.fromValue(dynasty),
            content = content.split("|").filter { it.isNotBlank() },
            type = PoemType.fromValue(type),
            rhythmic = rhythmic,
            chapter = chapter,
            section = section,
            comment = comment,
            appreciation = appreciation,
            notes = notes,
            translation = translation,
            isFavorite = isFavorite,
            createdAt = createdAt
        )
    }

    private fun buildSearchQuery(query: String): SimpleSQLiteQuery {
        val keywords = query
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (keywords.isEmpty()) {
            return SimpleSQLiteQuery("SELECT * FROM poems ORDER BY id ASC")
        }

        val args = mutableListOf<Any>()
        val whereClauses = keywords.map { keyword ->
            val likeValue = "%$keyword%"
            repeat(3) { args += likeValue }
            "(title LIKE ? OR author_name LIKE ? OR content LIKE ?)"
        }

        val titleScore = keywords.joinToString(" + ") { "CASE WHEN title LIKE ? THEN 2 ELSE 0 END" }
        val authorScore = keywords.joinToString(" + ") { "CASE WHEN author_name LIKE ? THEN 1 ELSE 0 END" }
        keywords.forEach { keyword -> args += "%$keyword%" }
        keywords.forEach { keyword -> args += "%$keyword%" }

        val sql = buildString {
            append("SELECT * FROM poems WHERE ")
            append(whereClauses.joinToString(" AND "))
            append(" ORDER BY (")
            append(titleScore)
            append(" + ")
            append(authorScore)
            append(") DESC, id ASC")
        }

        return SimpleSQLiteQuery(sql, args.toTypedArray())
    }
}
