package com.nalansitan.chinesepoetry.data.remote

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.security.PublicKey
import java.security.Signature

/** 所有大小均为字节；schemaVersion 表示数据包协议版本，不是应用版本。 */
data class DatabaseRelease(
    @SerializedName("data_version") val version: Long,
    @SerializedName("schema_version") val schemaVersion: Int,
    @SerializedName("download_url") val url: String,
    @SerializedName("database_sha256") val sha256: String,
    @SerializedName("database_size") val databaseSize: Long,
    @SerializedName("archive_size") val archiveSize: Long
)

class DatabaseReleasePolicy(private val publicKey: PublicKey) {
    companion object {
        const val RELEASES_URL = "https://api.github.com/repos/nalansitan/chinese-poetry-android/releases?per_page=100"
        const val DOWNLOAD_PREFIX = "https://github.com/nalansitan/chinese-poetry-android/releases/download/"
        const val MAX_DATABASE_BYTES = 2L * 1024 * 1024 * 1024
    }

    fun verify(bytes: ByteArray, signature: ByteArray): DatabaseRelease {
        require(bytes.size <= 64 * 1024 && signature.size <= 1024) { "数据清单过大" }
        require(Signature.getInstance("SHA256withRSA").run {
            initVerify(publicKey)
            update(bytes)
            verify(signature)
        }) { "数据清单签名无效" }
        val data = Gson().fromJson(bytes.toString(Charsets.UTF_8), DatabaseRelease::class.java)
        require(data.version > 0 && data.schemaVersion == 1) { "数据版本不兼容" }
        require(data.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "数据库摘要无效" }
        require(data.databaseSize in 16..MAX_DATABASE_BYTES && data.archiveSize in 1..MAX_DATABASE_BYTES) { "数据大小无效" }
        require(data.url.startsWith(DOWNLOAD_PREFIX) && !data.url.any { it.isWhitespace() }) { "数据下载地址无效" }
        return data
    }

    /** 只读取有签名清单的正式数据 Release；旧版无签名清单不会被当成动态配置。 */
    fun candidates(json: ByteArray): List<GitHubRelease> {
        val type = object : TypeToken<List<GitHubRelease>>() {}.type
        val releases: List<GitHubRelease> = Gson().fromJson(json.toString(Charsets.UTF_8), type)
        return releases.filter { release ->
            !release.draft && !release.prerelease && release.tagName?.startsWith("data-") == true &&
                assetUrl(release, "database-manifest.json") != null &&
                assetUrl(release, "database-manifest.sig") != null
        }.sortedByDescending { it.publishedAt.orEmpty() }.take(3)
    }

    suspend fun discover(
        builtin: DatabaseRelease,
        strict: Boolean = false,
        fetch: suspend (String, Int) -> ByteArray
    ): VerifiedDatabaseRelease? {
        var best: VerifiedDatabaseRelease? = null
        try {
            for (release in candidates(fetch(RELEASES_URL, 2 * 1024 * 1024))) {
                try {
                    val bytes = fetch(assetUrl(release, "database-manifest.json")!!, 64 * 1024)
                    val signature = fetch(assetUrl(release, "database-manifest.sig")!!, 1024)
                    val data = verify(bytes, signature)
                    if (data.version > (best?.data?.version ?: builtin.version) && belongsToRelease(data, release)) {
                        best = VerifiedDatabaseRelease(data, bytes, signature)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (error: Exception) {
                    if (strict) throw error
                    // 单个版本无效时继续尝试其他候选，网络预算由 fetch 统一限制。
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (error: Exception) {
            if (strict) throw error
            return null
        }
        return best
    }

    fun assetUrl(release: GitHubRelease, name: String): String? = release.assets?.firstOrNull {
        it.name == name && it.downloadUrl?.startsWith(DOWNLOAD_PREFIX) == true
    }?.downloadUrl

    fun belongsToRelease(data: DatabaseRelease, release: GitHubRelease): Boolean =
        release.assets.orEmpty().any { it.downloadUrl == data.url && it.size == data.archiveSize }
}

data class VerifiedDatabaseRelease(val data: DatabaseRelease, val bytes: ByteArray, val signature: ByteArray)

/** 最新数据失败只回退一次；协程取消必须由调用方传播，不能变成下载失败。 */
internal suspend fun <T> withDatabaseFallback(
    selected: DatabaseRelease,
    builtin: DatabaseRelease,
    attempt: suspend (DatabaseRelease) -> T,
    onFallback: (Exception) -> Unit
): T {
    try {
        return attempt(selected)
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (selected == builtin) throw error
        onFallback(error)
        return attempt(builtin)
    }
}
