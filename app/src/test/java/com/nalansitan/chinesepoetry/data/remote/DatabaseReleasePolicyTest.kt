package com.nalansitan.chinesepoetry.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.Signature

class DatabaseReleasePolicyTest {
    companion object {
        private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    }
    private val policy = DatabaseReleasePolicy(keys.public)
    private val builtin = DatabaseRelease(1, 1, "https://example.com/base.db.gz", "a".repeat(64), 1024, 100)
    private fun data(version: Long) = builtin.copy(version = version,
        url = DatabaseReleasePolicy.DOWNLOAD_PREFIX + "data-$version/poetry.db.gz")
    private fun bytes(data: DatabaseRelease) = Gson().toJson(data).toByteArray()
    private fun sign(bytes: ByteArray) = Signature.getInstance("SHA256withRSA").run {
        initSign(keys.private); update(bytes); sign()
    }
    private fun release(version: Long, draft: Boolean = false, prerelease: Boolean = false): GitHubRelease {
        val root = DatabaseReleasePolicy.DOWNLOAD_PREFIX + "data-$version/"
        return GitHubRelease("data-$version", draft, prerelease, publishedAt = "2026-11-07T00:00:00Z",
            assets = listOf(GitHubAsset("database-manifest.json", root + "database-manifest.json", 512),
                GitHubAsset("database-manifest.sig", root + "database-manifest.sig", 256),
                GitHubAsset("poetry.db.gz", data(version).url, 100)))
    }
    private fun responses(vararg releases: GitHubRelease): MutableMap<String, ByteArray> {
        val map = mutableMapOf(DatabaseReleasePolicy.RELEASES_URL to Gson().toJson(releases).toByteArray())
        releases.forEach {
            val version = it.tagName!!.removePrefix("data-").toLong()
            map[policy.assetUrl(it, "database-manifest.json")!!] = bytes(data(version))
            map[policy.assetUrl(it, "database-manifest.sig")!!] = sign(bytes(data(version)))
        }
        return map
    }

    @Test fun acceptsValidSignatureAndRejectsTampering() {
        val original = bytes(data(2))
        assertEquals(data(2), policy.verify(original, sign(original)))
        assertThrows(IllegalArgumentException::class.java) { policy.verify(bytes(data(3)), sign(original)) }
        val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        assertThrows(IllegalArgumentException::class.java) { DatabaseReleasePolicy(otherKey.public).verify(original, sign(original)) }
    }
    @Test fun rejectsIncompatibleAndUnsafeManifestFields() {
        for (invalid in listOf(data(2).copy(schemaVersion = 2), data(2).copy(sha256 = "bad"),
            data(2).copy(url = "http://example.com/db"), data(2).copy(version = 0),
            data(2).copy(databaseSize = Long.MAX_VALUE), data(2).copy(archiveSize = 0))) {
            assertThrows(IllegalArgumentException::class.java) { policy.verify(bytes(invalid), sign(bytes(invalid))) }
        }
    }
    @Test fun excludesAppsDraftsPrereleasesAndUnsignedData() {
        val unsigned = release(2).copy(assets = release(2).assets!!.filter { it.name != "database-manifest.sig" })
        val json = Gson().toJson(listOf(release(2).copy(tagName = "v1.2"), release(3, draft = true),
            release(4, prerelease = true), unsigned, release(5))).toByteArray()
        assertEquals(listOf("data-5"), policy.candidates(json).map { it.tagName })
    }
    @Test fun selectsHighestSignedDataVersionRatherThanListOrder() = runBlocking {
        val responses = responses(release(2), release(4), release(3))
        assertEquals(4L, policy.discover(builtin) { url, _ -> responses.getValue(url) }!!.data.version)
    }
    @Test fun invalidLatestCanUseAnotherValidCandidate() = runBlocking {
        val responses = responses(release(3), release(2))
        responses[policy.assetUrl(release(3), "database-manifest.sig")!!] = sign(bytes(data(9)))
        assertEquals(2L, policy.discover(builtin) { url, _ -> responses.getValue(url) }!!.data.version)
    }
    @Test fun offlineMissingNoNewOrMismatchedAssetsReturnBuiltinSelection() = runBlocking {
        assertNull(policy.discover(builtin) { _, _ -> throw IOException("offline") })
        assertNull(policy.discover(builtin) { _, _ -> "[]".toByteArray() })
        for (release in listOf(release(1), release(2).copy(assets = release(2).assets!!.dropLast(1)))) {
            val map = responses(release)
            assertNull(policy.discover(builtin) { url, _ -> map.getValue(url) })
        }
    }
    @Test fun manualCheckReportsNetworkFailure() {
        assertThrows(IOException::class.java) {
            runBlocking { policy.discover(builtin, strict = true) { _, _ -> throw IOException("offline") } }
        }
    }
    @Test fun manualCheckUsesInstalledVersion() = runBlocking {
        val map = responses(release(2))
        assertNull(policy.discover(data(3), strict = true) { url, _ -> map.getValue(url) })
    }
    @Test fun newerDownloadFailureFallsBackExactlyOnce() = runBlocking {
        val attempts = mutableListOf<Long>()
        var fallbackCount = 0
        val result = withDatabaseFallback(data(2), builtin, {
            attempts += it.version
            if (it.version == 2L) throw IOException("hash mismatch or network error")
            "installed"
        }, { fallbackCount++ })
        assertEquals("installed", result)
        assertEquals(listOf(2L, 1L), attempts)
        assertEquals(1, fallbackCount)
    }
    @Test fun builtinFailureDoesNotRetryOrHideError() {
        var attempts = 0
        assertThrows(IOException::class.java) {
            runBlocking { withDatabaseFallback(builtin, builtin, { attempts++; throw IOException("offline") }, { fail() }) }
        }
        assertEquals(1, attempts)
    }
    @Test fun cancellationNeverDownloadsFallback() {
        assertThrows(CancellationException::class.java) {
            runBlocking { withDatabaseFallback(data(2), builtin, { throw CancellationException() }, { fail() }) }
        }
        assertThrows(CancellationException::class.java) {
            runBlocking { policy.discover(builtin) { _, _ -> throw CancellationException() } }
        }
    }
    @Test fun newerDownloadSuccessNeverDownloadsBuiltin() = runBlocking {
        val attempts = mutableListOf<Long>()
        val result = withDatabaseFallback(data(2), builtin, { attempts += it.version; "ok" }, { fail() })
        assertEquals("ok", result)
        assertEquals(listOf(2L), attempts)
    }

    @Test fun bothDownloadFailuresPropagateFallbackError() {
        val attempts = mutableListOf<Long>()
        val error = assertThrows(IOException::class.java) {
            runBlocking {
                withDatabaseFallback(data(2), builtin, {
                    attempts += it.version
                    throw IOException("failed " + it.version)
                }, {})
            }
        }
        assertEquals("failed 1", error.message)
        assertEquals(listOf(2L, 1L), attempts)
    }

}
