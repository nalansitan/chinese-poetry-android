package com.nalansitan.chinesepoetry.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nalansitan.chinesepoetry.data.local.entity.AuthorEntity
import kotlinx.coroutines.flow.Flow

/**
 * 作者数据访问对象
 */
@Dao
interface AuthorDao {

    // ==================== 查询操作 ====================

    @Query("SELECT * FROM authors WHERE id = :id")
    suspend fun getById(id: String): AuthorEntity?

    @Query("SELECT * FROM authors WHERE name = :name")
    suspend fun getByName(name: String): AuthorEntity?

    @Query("SELECT * FROM authors ORDER BY name ASC")
    fun getAll(): PagingSource<Int, AuthorEntity>

    @Query(
        """
        SELECT * FROM authors
        WHERE dynasty = :dynastyValue OR dynasty = :dynastyDisplayName
        ORDER BY poem_count DESC, name ASC
        """
    )
    fun getByDynasty(dynastyValue: String, dynastyDisplayName: String): PagingSource<Int, AuthorEntity>

    @Query("""
        SELECT * FROM authors 
        WHERE name LIKE '%' || :query || '%'
        ORDER BY poem_count DESC, name ASC
    """)
    fun search(query: String): PagingSource<Int, AuthorEntity>

    @Query("SELECT * FROM authors ORDER BY poem_count DESC LIMIT :limit")
    suspend fun getTopAuthors(limit: Int = 20): List<AuthorEntity>

    // ==================== 统计操作 ====================

    @Query("SELECT COUNT(*) FROM authors")
    suspend fun getCount(): Int

    @Query(
        """
        SELECT COUNT(*) FROM authors
        WHERE dynasty = :dynastyValue OR dynasty = :dynastyDisplayName
        """
    )
    suspend fun getCountByDynasty(dynastyValue: String, dynastyDisplayName: String): Int

    // ==================== 插入/更新操作 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(author: AuthorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(authors: List<AuthorEntity>)

    @Update
    suspend fun update(author: AuthorEntity)

    @Query("UPDATE authors SET poem_count = :count WHERE id = :id")
    suspend fun updatePoemCount(id: String, count: Int)

    // ==================== 删除操作 ====================

    @Query("DELETE FROM authors WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM authors")
    suspend fun deleteAll()
}
