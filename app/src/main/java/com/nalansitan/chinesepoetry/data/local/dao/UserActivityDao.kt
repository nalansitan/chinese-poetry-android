package com.nalansitan.chinesepoetry.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nalansitan.chinesepoetry.data.local.entity.PoemEntity
import com.nalansitan.chinesepoetry.data.local.entity.UserActivityEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户活动数据访问对象
 */
@Dao
interface UserActivityDao {

    // ==================== 查询操作 ====================

    @Query("SELECT * FROM user_activities WHERE id = :id")
    suspend fun getById(id: String): UserActivityEntity?

    @Query("SELECT * FROM user_activities WHERE poem_id = :poemId AND type = :type")
    suspend fun getByPoemAndType(poemId: String, type: String): UserActivityEntity?

    @Query("SELECT * FROM user_activities WHERE type = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentByType(type: String, limit: Int = 50): List<UserActivityEntity>

    @Query("SELECT * FROM user_activities WHERE type = :type ORDER BY timestamp DESC")
    fun getAllByType(type: String): Flow<List<UserActivityEntity>>

    @Query("""
        SELECT p.* FROM poems p
        INNER JOIN user_activities ua ON p.id = ua.poem_id
        WHERE ua.type = :type
        ORDER BY ua.timestamp DESC
        LIMIT :limit
    """)
    suspend fun getPoemsByType(type: String, limit: Int = 50): List<PoemEntity>

    @Query("""
        SELECT p.* FROM poems p
        INNER JOIN user_activities ua ON p.id = ua.poem_id
        WHERE ua.type = :type
        ORDER BY ua.timestamp DESC
    """)
    fun observePoemsByType(type: String): Flow<List<PoemEntity>>

    @Query("""
        SELECT p.* FROM poems p
        INNER JOIN user_activities ua ON p.id = ua.poem_id
        WHERE ua.type = :type
        ORDER BY ua.timestamp DESC
    """)
    fun getPagedPoemsByType(type: String): PagingSource<Int, PoemEntity>

    // ==================== 检查操作 ====================

    @Query("SELECT EXISTS(SELECT 1 FROM user_activities WHERE poem_id = :poemId AND type = 'favorite')")
    fun isFavorite(poemId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM user_activities WHERE type = :type")
    fun getCountByType(type: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_activities WHERE type = 'history'")
    fun getHistoryCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT date(timestamp / 1000, 'unixepoch', 'localtime')) FROM user_activities WHERE type = 'history'")
    fun getReadingDaysCount(): Flow<Int>

    // ==================== 插入/更新操作 ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: UserActivityEntity)

    @Transaction
    suspend fun addToHistory(poemId: String) {
        val existing = getByPoemAndType(poemId, "history")
        if (existing != null) {
            // 更新已有记录的时间戳
            insert(existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            insert(UserActivityEntity(
                poemId = poemId,
                type = "history"
            ))
        }
    }

    @Transaction
    suspend fun toggleFavorite(poemId: String): Boolean {
        val existing = getByPoemAndType(poemId, "favorite")
        return if (existing != null) {
            deleteById(existing.id)
            false
        } else {
            insert(UserActivityEntity(
                poemId = poemId,
                type = "favorite"
            ))
            true
        }
    }

    // ==================== 删除操作 ====================

    @Query("DELETE FROM user_activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM user_activities WHERE poem_id = :poemId AND type = :type")
    suspend fun deleteByPoemAndType(poemId: String, type: String)

    @Query("DELETE FROM user_activities WHERE type = :type")
    suspend fun deleteAllByType(type: String)

    @Query("DELETE FROM user_activities WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM user_activities")
    suspend fun deleteAll()
}
