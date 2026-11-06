package com.nalansitan.chinesepoetry.data.remote

import org.junit.Assert.*
import org.junit.Test

class GitHubReleaseResolverTest {
    private fun release(tag: String) = GitHubRelease(
        tagName = tag,
        assets = listOf(GitHubAsset(
            name = "chinese-poetry-android-$tag-release.apk",
            downloadUrl = "https://github.com/nalansitan/chinese-poetry-android/releases/download/$tag/app.apk",
            size = 100
        ))
    )

    @Test fun selectsNumericHighestVersionAndIgnoresDataAndPrereleases() {
        val result = GitHubReleaseResolver.latest(listOf(
            release("data-2026-11-06"), release("v1.9"), release("v1.10.1"),
            release("v2.0").copy(prerelease = true), release("v3.0").copy(draft = true)
        ))
        assertEquals("1.10.1", result?.versionName)
        assertEquals(11001, result?.versionCode)
    }

    @Test fun acceptsTwoPartVersions() {
        assertEquals(10200, GitHubReleaseResolver.latest(listOf(release("v1.2")))?.versionCode)
    }

    @Test fun ignoresMissingApkAndUntrustedUrls() {
        val invalid = release("v1.2").copy(assets = listOf(GitHubAsset(
            "chinese-poetry-android-v1.2-release.apk", "https://example.com/app.apk", 100
        )))
        assertNull(GitHubReleaseResolver.latest(listOf(invalid, release("v1.3").copy(assets = emptyList()))))
    }

    @Test fun ignoresInvalidAndOverflowingVersionCodes() {
        assertNull(GitHubReleaseResolver.latest(listOf(
            release("v999999999999999999999999.1"), release("v1.100"),
            release("v1.2.100"), release("v1.2-beta"), release("v210001.0")
        )))
    }
}
