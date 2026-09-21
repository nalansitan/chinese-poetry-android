package com.nalansitan.chinesepoetry.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import com.nalansitan.chinesepoetry.data.local.entity.PoemEntity
import kotlinx.coroutines.flow.Flow
import androidx.sqlite.db.SupportSQLiteQuery

/**
 * 诗词数据访问对象
 */
@Dao
interface PoemDao {

    // ==================== 查询操作 ====================

    @Query("SELECT * FROM poems WHERE id = :id")
    suspend fun getById(id: String): PoemEntity?

    @Query("SELECT * FROM poems WHERE id = :id")
    fun observeById(id: String): Flow<PoemEntity?>

    @Query("SELECT * FROM poems ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): PoemEntity?

    @Query("SELECT * FROM poems ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomList(limit: Int): List<PoemEntity>

    @Query("SELECT * FROM poems WHERE type = :type ORDER BY id ASC")
    fun getByType(type: String): PagingSource<Int, PoemEntity>

    @Query(
        """
        SELECT * FROM poems
        WHERE dynasty = :dynastyValue OR dynasty = :dynastyDisplayName
        ORDER BY id ASC
        """
    )
    fun getByDynasty(dynastyValue: String, dynastyDisplayName: String): PagingSource<Int, PoemEntity>

    @Query(
        """
        SELECT * FROM poems
        WHERE dynasty = :dynastyValue OR dynasty = :dynastyDisplayName
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomListByDynasty(
        dynastyValue: String,
        dynastyDisplayName: String,
        limit: Int
    ): List<PoemEntity>

    @Query("SELECT * FROM poems WHERE author_name = :authorName ORDER BY id ASC")
    fun getByAuthor(authorName: String): PagingSource<Int, PoemEntity>

    @Query("""
        SELECT * FROM poems 
        WHERE title LIKE '%' || :query || '%' 
        OR author_name LIKE '%' || :query || '%' 
        OR content LIKE '%' || :query || '%'
        ORDER BY 
            CASE WHEN title LIKE '%' || :query || '%' THEN 0 ELSE 1 END,
            CASE WHEN author_name LIKE '%' || :query || '%' THEN 0 ELSE 1 END,
            id ASC
    """)
    fun search(query: String): PagingSource<Int, PoemEntity>

    @RawQuery(observedEntities = [PoemEntity::class])
    fun searchRaw(query: SupportSQLiteQuery): PagingSource<Int, PoemEntity>

    @Query("SELECT * FROM poems WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavorites(): PagingSource<Int, PoemEntity>

    @Query("SELECT * FROM poems WHERE rhythmic = :rhythmic ORDER BY id ASC")
    fun getByRhythmic(rhythmic: String): PagingSource<Int, PoemEntity>

    // ==================== 统计操作 ====================

    @Query("SELECT COUNT(*) FROM poems")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM poems WHERE type = :type")
    suspend fun getCountByType(type: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM poems
        WHERE dynasty = :dynastyValue OR dynasty = :dynastyDisplayName
        """
    )
    suspend fun getCountByDynasty(dynastyValue: String, dynastyDisplayName: String): Int

    @Query("SELECT COUNT(*) FROM poems WHERE is_favorite = 1")
    fun getFavoriteCount(): Flow<Int>

    // ==================== 插入/更新操作 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poem: PoemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(poems: List<PoemEntity>)

    @Update
    suspend fun update(poem: PoemEntity)

    @Query("UPDATE poems SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    // ==================== 删除操作 ====================

    @Query("DELETE FROM poems WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM poems")
    suspend fun deleteAll()
}
