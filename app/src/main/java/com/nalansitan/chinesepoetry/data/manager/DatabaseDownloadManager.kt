package com.nalansitan.chinesepoetry.data.manager

import android.content.Context
import android.util.Log
import com.nalansitan.chinesepoetry.data.local.database.DatabaseCompatibility
import com.nalansitan.chinesepoetry.data.local.database.DatabaseFileInstaller
import com.nalansitan.chinesepoetry.data.local.database.PoetryDatabase
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库下载管理器
 * 用于客户端首次安装时从服务端下载数据库
 */
@Singleton
class DatabaseDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiServiceProvider: ApiServiceProvider
) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /**
     * 检查本地数据库是否存在
     */
    fun isDatabaseExists(): Boolean {
        val dbFile = context.getDatabasePath(PoetryDatabase.DATABASE_NAME)
        DatabaseFileInstaller.recoverInterruptedInstall(dbFile)
        return dbFile.exists() && dbFile.length() > 0
    }

    /**
     * 获取数据库文件大小（用于显示下载进度）
     * 通过轻量级 /database/info 接口获取，避免触发文件下载
     */
    suspend fun getRemoteDatabaseSize(): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiServiceProvider.getApiService().getDatabaseInfo()
                if (response.isSuccessful) {
                    response.body()?.size?.takeIf { it > 0 }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 下载数据库文件，支持断点续传
     */
    suspend fun downloadDatabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _downloadState.value = DownloadState.Downloading(0)

                val tempFile = File(context.cacheDir, "poetry.db.tmp")
                var downloadedBytes = 0L

                // 检查临时文件是否存在，用于断点续传
                if (tempFile.exists() && tempFile.length() > 0) {
                    downloadedBytes = tempFile.length()
                    Log.i(TAG, "Resume download from byte $downloadedBytes")
                }

                // 发起请求，如果有已下载内容则带 Range 头
                val rangeHeader = if (downloadedBytes > 0) "bytes=$downloadedBytes-" else null
                val response = apiServiceProvider.getApiService().downloadDatabase(rangeHeader)

                if (!response.isSuccessful) {
                    val errorMsg = when (response.code()) {
                        401 -> "认证失败 (401)：用户名或密码错误"
                        403 -> "访问被拒绝 (403)：权限不足"
                        404 -> "数据库文件未找到 (404)：服务器未准备好数据"
                        416 -> {
                            // Range Not Satisfiable — 临时文件已过期，删除后重试
                            tempFile.delete()
                            "续传数据已过期，请重新下载"
                        }
                        in 500..599 -> "服务器内部错误 (${response.code()})"
                        else -> "下载失败: ${response.code()}"
                    }
                    _downloadState.value = DownloadState.Error(errorMsg)
                    return@withContext false
                }

                val body = response.body()
                if (body == null) {
                    _downloadState.value = DownloadState.Error("响应体为空")
                    return@withContext false
                }

                // 判断服务端是否支持续传
                val isResuming = response.code() == 206
                if (!isResuming && downloadedBytes > 0) {
                    // 服务端不支持 Range，返回了完整文件，从头开始
                    Log.w(TAG, "Server does not support Range, restarting download")
                    downloadedBytes = 0
                    tempFile.delete()
                }

                // 计算总大小：续传时 = 已下载 + 剩余；全新时 = contentLength
                val contentLength = body.contentLength()
                val totalBytes = if (isResuming) {
                    downloadedBytes + contentLength
                } else {
                    contentLength
                }

                // 写入文件：续传用追加模式，全新用覆写模式
                val raf = RandomAccessFile(tempFile, "rw")
                if (isResuming) {
                    raf.seek(downloadedBytes)
                } else {
                    raf.setLength(0)
                }

                try {
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var lastSpeedUpdateTime = System.currentTimeMillis()
                        var lastSpeedUpdateBytes = downloadedBytes
                        var currentSpeed = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            raf.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // 每 500ms 更新一次速度和进度
                            val now = System.currentTimeMillis()
                            val elapsed = now - lastSpeedUpdateTime
                            if (elapsed >= 500) {
                                val bytesDelta = downloadedBytes - lastSpeedUpdateBytes
                                currentSpeed = bytesDelta * 1000 / elapsed
                                lastSpeedUpdateTime = now
                                lastSpeedUpdateBytes = downloadedBytes

                                if (totalBytes > 0) {
                                    val progress = (downloadedBytes * 100 / totalBytes).toInt()
                                    _downloadState.value = DownloadState.Downloading(
                                        progress = progress,
                                        speedBytesPerSec = currentSpeed,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    raf.close()
                }

                // 下载完成，移动到数据库目录
                val dbFile = context.getDatabasePath(PoetryDatabase.DATABASE_NAME)
                dbFile.parentFile?.mkdirs()

                DatabaseCompatibility.clearPatchMarker(dbFile)
                val installed = DatabaseFileInstaller.install(tempFile, dbFile, totalBytes) {
                    DatabaseCompatibility.patchDatabaseForRoom(it)
                }
                if (!installed) {
                    _downloadState.value = DownloadState.Error("数据库校验或安装失败")
                    return@withContext false
                }

                _downloadState.value = DownloadState.Success
                true
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is SocketTimeoutException -> "连接超时：无法连接到服务器"
                    is ConnectException -> "连接失败：服务器不可达"
                    is UnknownHostException -> "无法解析服务器地址：请检查网络或服务器地址"
                    else -> e.message ?: "下载失败"
                }
                // 不删除临时文件，保留用于下次续传
                _downloadState.value = DownloadState.Error(errorMsg)
                false
            }
        }
    }

    /**
     * 删除本地数据库（用于重新下载）
     */
    suspend fun deleteLocalDatabase() {
        withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath(PoetryDatabase.DATABASE_NAME)
            if (dbFile.exists()) {
                DatabaseCompatibility.clearPatchMarker(dbFile)
                dbFile.delete()
            }
            // 删除相关 journal/wal 文件
            File(dbFile.parent, "${dbFile.name}-journal").delete()
            File(dbFile.parent, "${dbFile.name}-shm").delete()
            File(dbFile.parent, "${dbFile.name}-wal").delete()
        }
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
    companion object {
        private const val TAG = "DBDownloadManager"
    }
}

/**
 * 下载状态
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(
        val progress: Int,
        val speedBytesPerSec: Long = 0,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = 0
    ) : DownloadState()
    data object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}
