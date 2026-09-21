package com.nalansitan.chinesepoetry.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * 数据库文件信息响应
 */
data class DatabaseInfoResponse(
    val size: Long,
    val lastModified: String,
    val filename: String
)

/**
 * 诗词数据 API 服务
 * 用于检查更新和下载增量数据
 */
interface PoetryApiService {

    /**
     * 获取服务器上的数据版本信息
     */
    @GET("api/v1/poetry/version")
    suspend fun getDataVersion(): Response<DataVersionResponse>

    /**
     * 获取增量更新数据
     * @param currentVersion 客户端当前数据版本
     */
    @GET("api/v1/poetry/update")
    suspend fun getUpdateData(
        @Query("version") currentVersion: Int
    ): Response<UpdateDataResponse>

    /**
     * 获取扩展数据（评论、赏析等）
     * @param poemId 诗词ID
     */
    @GET("api/v1/poetry/extension")
    suspend fun getExtensionData(
        @Query("poem_id") poemId: String
    ): Response<ExtensionDataResponse>

    /**
     * 获取数据库文件信息（大小、修改时间等）
     */
    @GET("api/v1/poetry/database/info")
    suspend fun getDatabaseInfo(): Response<DatabaseInfoResponse>

    /**
     * 下载完整数据库文件
     * 用于客户端首次安装时初始化数据
     * @param range HTTP Range 头，用于断点续传，格式: "bytes=xxx-"
     */
    @GET("api/v1/poetry/database")
    @Streaming
    suspend fun downloadDatabase(
        @Header("Range") range: String? = null
    ): Response<ResponseBody>

    @GET("public/apk/version")
    suspend fun getLatestAppVersion(): Response<AppVersionResponse>

    @GET("public/apk/download")
    @Streaming
    suspend fun downloadAppApk(
        @Query("version") version: Int? = null
    ): Response<ResponseBody>

    companion object {
        // 基础 URL，在 build.gradle.kts 中配置
        const val BASE_URL = com.nalansitan.chinesepoetry.BuildConfig.BASE_URL
    }
}

/**
 * 数据版本响应
 */
data class DataVersionResponse(
    val version: Int,
    val versionName: String,
    val updateTime: String,
    val totalPoems: Int,
    val changelog: String,
    val isForceUpdate: Boolean = false
)

/**
 * 更新数据响应
 */
data class UpdateDataResponse(
    val hasUpdate: Boolean,
    val newVersion: Int,
    val updateType: String, // "full" | "incremental"
    val poems: List<RemotePoem>? = null,
    val authors: List<RemoteAuthor>? = null,
    val deleteIds: List<String>? = null, // 需要删除的诗词ID
    val downloadUrl: String? = null // 完整数据库下载地址
)

/**
 * 远程诗词数据
 */
data class RemotePoem(
    val id: String,
    val title: String,
    val authorName: String,
    val authorId: String?,
    val dynasty: String,
    val content: String,
    val type: String,
    val rhythmic: String?,
    val chapter: String?,
    val section: String?,
    // 扩展字段
    val comment: String?,
    val appreciation: String?,
    val notes: String?,
    val translation: String?
)

/**
 * 远程作者数据
 */
data class RemoteAuthor(
    val id: String,
    val name: String,
    val dynasty: String,
    val intro: String?,
    val shortIntro: String?,
    val poemCount: Int
)

/**
 * 扩展数据响应
 */
data class ExtensionDataResponse(
    val poemId: String,
    val comment: String?,
    val appreciation: String?,
    val notes: String?,
    val translation: String?,
    val updateTime: String
)

data class AppVersionResponse(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val changelog: String,
    val forceUpdate: Boolean = false,
    val fileSize: Long,
    val releaseTime: String
)
