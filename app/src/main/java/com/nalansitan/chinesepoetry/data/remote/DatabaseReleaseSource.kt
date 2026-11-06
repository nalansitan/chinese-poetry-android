package com.nalansitan.chinesepoetry.data.remote

import android.content.Context
import com.nalansitan.chinesepoetry.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseReleaseSource @Inject constructor(@ApplicationContext context: Context) {
    val builtin = DatabaseRelease(BuildConfig.DATABASE_VERSION, 1, BuildConfig.DATABASE_URL,
        BuildConfig.DATABASE_SHA256, 0, 0)
    private val policy by lazy {
        val certificate = context.assets.open("database-signing-cert.der").use {
            CertificateFactory.getInstance("X.509").generateCertificate(it)
        }
        DatabaseReleasePolicy(certificate.publicKey)
    }
    private val preferences = context.getSharedPreferences("database_release_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS).build()
    private val mutex = Mutex()
    @Volatile private var selected: DatabaseRelease? = null
    private val _description = MutableStateFlow("首次使用将检查最新数据，可随时导入内置版本数据包")
    val description = _description.asStateFlow()

    suspend fun select(): DatabaseRelease = withContext(Dispatchers.IO) {
        mutex.withLock {
            selected?.let { return@withLock it }
            if (builtin.url.isBlank()) return@withLock builtin.also { selected = it }
            _description.value = "正在检查最新数据（最多 8 秒）…"
            val now = System.currentTimeMillis()
            val last = preferences.getLong("last_attempt", 0)
            if (now - last in 0 until TimeUnit.HOURS.toMillis(1)) {
                return@withLock (cached() ?: builtin).also { choose(it, it == builtin) }
            }
            preferences.edit().putLong("last_attempt", now).apply()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            try {
                val verified = policy.discover(builtin) { url, limit -> fetch(url, limit, deadline) }
                val best = verified?.data ?: builtin
                if (verified != null) {
                    preferences.edit().putString("manifest", android.util.Base64.encodeToString(verified.bytes, android.util.Base64.NO_WRAP))
                        .putString("signature", android.util.Base64.encodeToString(verified.signature, android.util.Base64.NO_WRAP)).apply()
                } else {
                    preferences.edit().remove("manifest").remove("signature").apply()
                }
                choose(best, false)
                best
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 无法联网时按约定回到 APK 的固定版本，不使用未经验证的新配置。
                choose(builtin, true)
                builtin
            }
        }
    }

    /** 手动检查绕过初始化缓存，网络错误明确交给 UI，不能误报已是最新。 */
    suspend fun checkForUpdate(currentVersion: Long): DatabaseRelease? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
            policy.discover(builtin.copy(version = currentVersion), strict = true) { url, limit ->
                fetch(url, limit, deadline)
            }?.data
        }
    }

    fun fallback() { choose(builtin, true) }

    /** 本地导入允许内置包和此前通过签名验证的新包，不为导入发起网络请求。 */
    fun importCandidates(): List<DatabaseRelease> = listOfNotNull(builtin, selected, cached()).distinctBy { it.sha256 }

    private fun cached(): DatabaseRelease? = try {
        val bytes = android.util.Base64.decode(preferences.getString("manifest", ""), android.util.Base64.NO_WRAP)
        val signature = android.util.Base64.decode(preferences.getString("signature", ""), android.util.Base64.NO_WRAP)
        policy.verify(bytes, signature).takeIf { it.version > builtin.version }
    } catch (_: Exception) { null }

    private fun choose(data: DatabaseRelease, fallback: Boolean) {
        selected = data
        _description.value = when {
            fallback -> "使用内置数据版本；网络不畅时也可从本地导入"
            data == builtin -> "未发现可用新版或无法检查，使用内置版本"
            else -> "将使用最新数据版本 ${data.version}"
        }
    }

    private suspend fun fetch(url: String, limit: Int, deadline: Long): ByteArray {
        currentCoroutineContext().ensureActive()
        val remaining = deadline - System.nanoTime()
        check(remaining > 0) { "数据检查超时" }
        val call = client.newCall(Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ChinesePoetry-Android")
            .header("X-GitHub-Api-Version", "2022-11-28").build())
        call.timeout().timeout(remaining, TimeUnit.NANOSECONDS)
        val result = call.execute().use { response ->
            check(response.isSuccessful) { "GitHub 数据检查失败 (${response.code})" }
            val body = checkNotNull(response.body)
            check(body.contentLength() <= limit) { "数据清单过大" }
            body.byteStream().use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var count = input.read(buffer)
                while (count != -1) {
                    check(out.size() + count <= limit) { "数据清单过大" }
                    out.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                out.toByteArray()
            }
        }
        currentCoroutineContext().ensureActive()
        return result
    }
}
