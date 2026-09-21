package com.nalansitan.chinesepoetry.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.nalansitan.chinesepoetry.data.local.dao.AuthorDao
import com.nalansitan.chinesepoetry.data.local.entity.AuthorEntity
import com.nalansitan.chinesepoetry.data.local.entity.Dynasty
import com.nalansitan.chinesepoetry.domain.model.Author
import com.nalansitan.chinesepoetry.domain.repository.AuthorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 作者仓库实现
 */
@Singleton
class AuthorRepositoryImpl @Inject constructor(
    private val authorDao: AuthorDao
) : AuthorRepository {

    override suspend fun getAuthorById(id: String): Author? {
        return authorDao.getById(id)?.toDomainModel()
    }

    override suspend fun getAuthorByName(name: String): Author? {
        return authorDao.getByName(name)?.toDomainModel()
    }

    override fun getAllAuthors(): Flow<PagingData<Author>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { authorDao.getAll() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getAuthorsByDynasty(dynasty: Dynasty): Flow<PagingData<Author>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { authorDao.getByDynasty(dynasty.value, dynasty.displayName) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun searchAuthors(query: String): Flow<PagingData<Author>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { authorDao.search(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override suspend fun getTopAuthors(limit: Int): List<Author> {
        return authorDao.getTopAuthors(limit).map { it.toDomainModel() }
    }

    override suspend fun getTotalCount(): Int {
        return authorDao.getCount()
    }

    override suspend fun getCountByDynasty(dynasty: Dynasty): Int {
        return authorDao.getCountByDynasty(dynasty.value, dynasty.displayName)
    }

    /**
     * 转换实体到领域模型
     */
    private fun AuthorEntity.toDomainModel(): Author {
        return Author(
            id = id,
            name = name,
            dynasty = Dynasty.fromValue(dynasty),
            intro = intro,
            shortIntro = shortIntro,
            poemCount = poemCount
        )
    }
}
