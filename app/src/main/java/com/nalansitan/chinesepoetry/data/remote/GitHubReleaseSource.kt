package com.nalansitan.chinesepoetry.data.remote

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nalansitan.chinesepoetry.data.remote.api.AppVersionResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 公开仓库匿名检查；不在 APK 中放置 GitHub Token。 */
@Singleton
class GitHubReleaseSource @Inject constructor(@ApplicationContext context: Context) {
    private val preferences = context.getSharedPreferences("github_release_cache", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): AppVersionResponse? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val cached = preferences.getString("body", null)
            val lastSuccess = preferences.getLong("last_success", 0)
            if (cached != null && now - lastSuccess in 0 until TimeUnit.HOURS.toMillis(1)) {
                return@withLock parse(cached)
            }
            check(now >= preferences.getLong("retry_at", 0)) { "GitHub 检查暂时不可用，请稍后重试" }
            // 失败也冷却一分钟；不能因网络失败而反复点击消耗限额。
            preferences.edit().putLong("retry_at", now + TimeUnit.MINUTES.toMillis(1)).apply()
            val request = Request.Builder()
                .url("https://api.github.com/repos/nalansitan/chinese-poetry-android/releases?per_page=100")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ChinesePoetry-Android")
                .apply {
                    if (cached != null) preferences.getString("etag", null)?.let { header("If-None-Match", it) }
                }.build()
            client.newCall(request).execute().use { response ->
                if (response.code == 304 && cached != null) {
                    val result = parse(cached)
                    preferences.edit().putLong("last_success", now).putLong("retry_at", 0).apply()
                    return@withLock result
                }
                if (response.code == 403 || response.code == 429) {
                    val retryAfter = response.header("Retry-After")?.toLongOrNull()?.let { now + it * 1000 }
                    val reset = response.header("X-RateLimit-Reset")?.toLongOrNull()?.times(1000)
                    val retryAt = maxOf(now + TimeUnit.MINUTES.toMillis(1), retryAfter ?: reset ?: (now + TimeUnit.HOURS.toMillis(1)))
                    preferences.edit().putLong("retry_at", retryAt).apply()
                    error("GitHub 暂时限制访问，请稍后重试或前往 Release 页面下载")
                }
                check(response.isSuccessful) { "GitHub 版本检查失败 (${response.code})" }
                val json = response.body?.string() ?: error("GitHub 返回空响应")
                val result = parse(json)
                preferences.edit().putString("body", json).putString("etag", response.header("ETag"))
                    .putLong("last_success", now).putLong("retry_at", 0).apply()
                result
            }
        }
    }

    private fun parse(json: String): AppVersionResponse? {
        val type = object : TypeToken<List<GitHubRelease>>() {}.type
        val releases: List<GitHubRelease> = gson.fromJson(json, type) ?: error("无效的 GitHub 版本信息")
        return GitHubReleaseResolver.latest(releases)
    }
}
