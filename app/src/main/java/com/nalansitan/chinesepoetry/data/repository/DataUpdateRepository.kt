package com.nalansitan.chinesepoetry.data.repository

import com.nalansitan.chinesepoetry.data.local.database.PoetryDatabase
import com.nalansitan.chinesepoetry.data.local.entity.AuthorEntity
import com.nalansitan.chinesepoetry.data.local.entity.PoemEntity
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.remote.api.DataVersionResponse
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import com.nalansitan.chinesepoetry.data.remote.api.UpdateDataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据更新仓库
 */
@Singleton
class DataUpdateRepository @Inject constructor(
    private val apiServiceProvider: ApiServiceProvider,
    private val database: PoetryDatabase,
    private val dataStore: DataStoreManager
) {

    /**
     * 检查是否有数据更新
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val response = apiServiceProvider.getApiService().getDataVersion()
            if (response.isSuccessful) {
                val serverVersion = response.body()
                val localVersion = dataStore.dataVersion.first()
                
                if (serverVersion != null && serverVersion.version > localVersion) {
                    dataStore.setPendingUpdate(
                        hasUpdate = true,
                        version = serverVersion.version,
                        versionName = serverVersion.versionName,
                        changelog = serverVersion.changelog
                    )
                    UpdateCheckResult.HasUpdate(
                        currentVersion = localVersion,
                        newVersion = serverVersion.version,
                        versionName = serverVersion.versionName,
                        changelog = serverVersion.changelog,
                        isForceUpdate = serverVersion.isForceUpdate
                    )
                } else {
                    dataStore.setPendingUpdate(hasUpdate = false)
                    UpdateCheckResult.NoUpdate(localVersion)
                }
            } else {
                UpdateCheckResult.Error("检查更新失败: ${response.code()}")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 执行数据更新
     * @param onProgress 进度回调 (0-100)
     */
    suspend fun performUpdate(
        onProgress: suspend (Int) -> Unit = {}
    ): UpdateResult {
        return try {
            val currentVersion = dataStore.dataVersion.first()
            val response = apiServiceProvider.getApiService().getUpdateData(currentVersion)
            
            if (!response.isSuccessful) {
                return UpdateResult.Error("获取更新数据失败: ${response.code()}")
            }
            
            val updateData = response.body()
                ?: return UpdateResult.Error("更新数据为空")
            
            if (!updateData.hasUpdate) {
                return UpdateResult.Success(currentVersion, false)
            }
            
            onProgress(10)
            
            when (updateData.updateType) {
                "incremental" -> {
                    // 增量更新
                    applyIncrementalUpdate(updateData) { progress ->
                        onProgress(10 + (progress * 0.8).toInt())
                    }
                }
                "full" -> {
                    // 当前数据库同时保存用户收藏和阅读历史，运行中替换会造成数据丢失
                    // 并使已注入的 Room/DAO 失效。在用户数据拆分前明确拒绝全量替换。
                    return UpdateResult.Error("当前版本暂不支持全量数据库更新，请等待后续版本")
                }
                else -> return UpdateResult.Error("未知的更新类型")
            }
            
            onProgress(90)
            
            // 更新本地版本号
            dataStore.updateDataVersion(
                updateData.newVersion,
                "${updateData.newVersion}.0.0"
            )
            dataStore.setPendingUpdate(hasUpdate = false)
            
            onProgress(100)
            
            UpdateResult.Success(updateData.newVersion, true)
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "更新失败")
        }
    }

    /**
     * 应用增量更新
     */
    private suspend fun applyIncrementalUpdate(
        updateData: UpdateDataResponse,
        onProgress: suspend (Int) -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            val totalItems = (updateData.deleteIds?.size ?: 0) +
                    (updateData.poems?.size ?: 0) +
                    (updateData.authors?.size ?: 0)
            var processedItems = 0
            
            database.withTransaction {
                // 删除数据
                updateData.deleteIds?.forEach { id ->
                    database.poemDao().deleteById(id)
                    processedItems++
                    if (totalItems > 0) {
                        onProgress((processedItems * 100 / totalItems))
                    }
                }
                
                // 新增/更新诗词
                updateData.poems?.forEach { remotePoem ->
                    val existing = database.poemDao().getById(remotePoem.id)
                    val entity = PoemEntity(
                        id = remotePoem.id,
                        title = remotePoem.title,
                        authorName = remotePoem.authorName,
                        authorId = remotePoem.authorId,
                        dynasty = remotePoem.dynasty,
                        content = remotePoem.content,
                        type = remotePoem.type,
                        rhythmic = remotePoem.rhythmic,
                        chapter = remotePoem.chapter,
                        section = remotePoem.section,
                        comment = remotePoem.comment,
                        appreciation = remotePoem.appreciation,
                        notes = remotePoem.notes,
                        translation = remotePoem.translation,
                        isFavorite = existing?.isFavorite ?: false,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    database.poemDao().insert(entity)
                    processedItems++
                    if (totalItems > 0) {
                        onProgress((processedItems * 100 / totalItems))
                    }
                }
                
                // 新增/更新作者
                updateData.authors?.forEach { remoteAuthor ->
                    val entity = AuthorEntity(
                        id = remoteAuthor.id,
                        name = remoteAuthor.name,
                        dynasty = remoteAuthor.dynasty,
                        intro = remoteAuthor.intro,
                        shortIntro = remoteAuthor.shortIntro,
                        poemCount = remoteAuthor.poemCount
                    )
                    database.authorDao().insert(entity)
                    processedItems++
                    if (totalItems > 0) {
                        onProgress((processedItems * 100 / totalItems))
                    }
                }
            }
        }
    }

    /**
     * 获取诗词的扩展数据（评论、赏析等）
     */
    suspend fun fetchExtensionData(poemId: String): Boolean {
        return try {
            val response = apiServiceProvider.getApiService().getExtensionData(poemId)
            if (response.isSuccessful) {
                val extension = response.body()
                if (extension != null) {
                    // 更新本地数据库中的扩展字段
                    val poem = database.poemDao().getById(poemId)
                    if (poem != null) {
                        database.poemDao().update(
                            poem.copy(
                                comment = extension.comment ?: poem.comment,
                                appreciation = extension.appreciation ?: poem.appreciation,
                                notes = extension.notes ?: poem.notes,
                                translation = extension.translation ?: poem.translation
                            )
                        )
                        true
                    } else false
                } else false
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取数据版本流
     */
    fun getDataVersion(): Flow<Int> = dataStore.dataVersion

    fun hasPendingUpdate(): Flow<Boolean> = dataStore.hasPendingUpdate

    /**
     * 获取上次检查时间
     */
    fun getLastCheckTime(): Flow<Long> = dataStore.lastCheckTime

    suspend fun shouldCheckForUpdate(intervalHours: Long): Boolean {
        val lastCheckTime = dataStore.lastCheckTime.first()
        if (lastCheckTime == 0L) return true
        val intervalMillis = intervalHours * 60 * 60 * 1000
        return System.currentTimeMillis() - lastCheckTime >= intervalMillis
    }

    /**
     * 更新最后检查时间
     */
    suspend fun updateLastCheckTime() = dataStore.updateLastCheckTime()
}

/**
 * 更新检查结果
 */
sealed class UpdateCheckResult {
    data class HasUpdate(
        val currentVersion: Int,
        val newVersion: Int,
        val versionName: String,
        val changelog: String,
        val isForceUpdate: Boolean
    ) : UpdateCheckResult()
    
    data class NoUpdate(val currentVersion: Int) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * 更新结果
 */
sealed class UpdateResult {
    data class Success(val newVersion: Int, val hasUpdate: Boolean) : UpdateResult()
    data class Error(val message: String) : UpdateResult()
}
