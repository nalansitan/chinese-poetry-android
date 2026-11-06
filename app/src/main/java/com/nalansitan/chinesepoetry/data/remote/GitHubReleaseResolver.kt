package com.nalansitan.chinesepoetry.data.remote

import com.google.gson.annotations.SerializedName
import com.nalansitan.chinesepoetry.data.remote.api.AppVersionResponse

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val body: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAsset>? = null
)

data class GitHubAsset(
    val name: String? = null,
    @SerializedName("browser_download_url") val downloadUrl: String? = null,
    val size: Long = 0
)

object GitHubReleaseResolver {
    fun latest(releases: List<GitHubRelease>): AppVersionResponse? = releases.mapNotNull { release ->
        if (release.draft || release.prerelease) return@mapNotNull null
        val tag = release.tagName ?: return@mapNotNull null
        val match = Regex("^v([0-9]+)\\.([0-9]+)(?:\\.([0-9]+))?$").matchEntire(tag) ?: return@mapNotNull null
        val major = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val minor = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return@mapNotNull null
        if (major > 210000 || minor !in 0..99 || patch !in 0..99) return@mapNotNull null
        val code = major * 10000 + minor * 100 + patch
        if (code > 2100000000L) return@mapNotNull null
        val version = tag.removePrefix("v")
        val asset = release.assets?.firstOrNull {
            it.name == "chinese-poetry-android-v$version-release.apk" && it.size > 0 &&
                it.downloadUrl?.startsWith("https://github.com/nalansitan/chinese-poetry-android/releases/download/") == true
        } ?: return@mapNotNull null
        AppVersionResponse(
            versionCode = code.toInt(), versionName = version, downloadUrl = asset.downloadUrl!!,
            changelog = release.body.orEmpty(), forceUpdate = false,
            fileSize = asset.size, releaseTime = release.publishedAt.orEmpty()
        )
    }.maxByOrNull { it.versionCode }
}
