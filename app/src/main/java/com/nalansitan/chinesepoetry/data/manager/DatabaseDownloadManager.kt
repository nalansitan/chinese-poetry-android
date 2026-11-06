package com.nalansitan.chinesepoetry.data.manager

import com.nalansitan.chinesepoetry.BuildConfig
import java.security.MessageDigest
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import com.nalansitan.chinesepoetry.data.local.database.DatabasePackage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
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

    private val operationMutex = Mutex()

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
                if (BuildConfig.DATABASE_URL.isNotBlank()) {
                    val response = apiServiceProvider.getPublicApiService()
                        .getDatabaseFileInfo(BuildConfig.DATABASE_URL)
                    return@withContext if (response.isSuccessful) {
                        response.headers()["Content-Length"]?.toLongOrNull()?.takeIf { it > 0 }
                    } else null
                }
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
            if (!operationMutex.tryLock()) return@withContext false
            try {
                check(!isDatabaseExists()) { "数据库已初始化，无需重复导入" }
                _downloadState.value = DownloadState.Downloading(0)

                // 不同数据版本不共用断点文件，避免跨文件拼接。
                val tempName = if (BuildConfig.DATABASE_URL.isBlank()) "poetry.db.tmp"
                    else "poetry-${MessageDigest.getInstance("SHA-256").digest(
                        (BuildConfig.DATABASE_URL + BuildConfig.DATABASE_SHA256).toByteArray()
                    ).joinToString("") { "%02x".format(it) }}.db.tmp"
                val tempFile = File(context.cacheDir, tempName)
                var downloadedBytes = 0L

                // 检查临时文件是否存在，用于断点续传
                if (tempFile.exists() && tempFile.length() > 0) {
                    downloadedBytes = tempFile.length()
                    Log.i(TAG, "Resume download from byte $downloadedBytes")
                }

                // 发起请求，如果有已下载内容则带 Range 头
                val rangeHeader = if (downloadedBytes > 0) "bytes=$downloadedBytes-" else null
                val response = if (BuildConfig.DATABASE_URL.isNotBlank()) {
                    apiServiceProvider.getPublicApiService()
                        .downloadDatabaseFile(BuildConfig.DATABASE_URL, rangeHeader)
                } else {
                    apiServiceProvider.getApiService().downloadDatabase(rangeHeader)
                }

                if (!response.isSuccessful) {
                    val errorMsg = when (response.code()) {
                        401 -> "下载地址需要认证 (401)：请使用公开下载地址"
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
                    response.errorBody()?.close()
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
                if (isResuming && !response.headers()["Content-Range"].orEmpty()
                        .startsWith("bytes $downloadedBytes-")) {
                    body.close()
                    tempFile.delete()
                    error("续传响应不匹配，请重试")
                }
                if (!isResuming && downloadedBytes > 0) {
                    // 服务端不支持 Range，返回了完整文件，从头开始
                    Log.w(TAG, "Server does not support Range, restarting download")
                    downloadedBytes = 0
                    tempFile.delete()
                }

                // 计算总大小：续传时 = 已下载 + 剩余；全新时 = contentLength
                val contentLength = body.contentLength()
                val totalBytes = if (isResuming) {
                    if (contentLength >= 0) downloadedBytes + contentLength else -1
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

                                run {
                                    val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt().coerceIn(0, 100) else 0
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

                check(totalBytes <= 0 || downloadedBytes == totalBytes) { "下载未完成，请重试续传" }
                try {
                    installPackage(tempFile.inputStream())
                } finally {
                    // 传输已完整结束，解压或摘要失败也不能继续拼接这个包。
                    tempFile.delete()
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
                throw e
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
            } finally {
                operationMutex.unlock()
            }
        }
    }

    suspend fun importDatabase(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        if (!operationMutex.tryLock()) return@withContext false
        try {
            check(!isDatabaseExists()) { "数据库已初始化，无需重复导入" }
            val input = context.contentResolver.openInputStream(uri) ?: error("无法读取所选文件")
            installPackage(input)
        } catch (e: CancellationException) {
            _downloadState.value = DownloadState.Idle
            throw e
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error(e.message ?: "导入失败")
            false
        } finally {
            operationMutex.unlock()
        }
    }

    private fun installPackage(input: java.io.InputStream): Boolean {
        _downloadState.value = DownloadState.Installing
        val unpacked = File(context.cacheDir, "poetry-unpacked.db.tmp")
        try {
            DatabasePackage.unpack(input, unpacked, BuildConfig.DATABASE_SHA256)
            val dbFile = context.getDatabasePath(PoetryDatabase.DATABASE_NAME)
            SQLiteDatabase.openDatabase(unpacked.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("PRAGMA quick_check", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "数据库完整性检查失败" }
                }
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('poems', 'authors')", null).use {
                    check(it.count == 2) { "所选文件不是诗词数据库" }
                }
            }
            DatabaseCompatibility.clearPatchMarker(dbFile)
            check(DatabaseFileInstaller.install(unpacked, dbFile, unpacked.length()) {
                DatabaseCompatibility.patchDatabaseForRoom(it)
            }) { "数据库校验或安装失败" }
            _downloadState.value = DownloadState.Success
            return true
        } finally {
            unpacked.delete()
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
    data object Installing : DownloadState()
    data object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}
