package com.nalansitan.chinesepoetry.data.local.database

import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/** 同时支持原始 SQLite 和 gzip 包；摘要始终针对解压后的数据库。 */
object DatabasePackage {
    fun unpack(input: InputStream, output: File, expectedSha256: String, maxBytes: Long = 2L * 1024 * 1024 * 1024, acceptedSha256: Set<String> = setOf(expectedSha256)): String {
        var actualHash = ""
        try {
            input.buffered().use { buffered ->
                buffered.mark(2)
                val gzip = buffered.read() == 0x1f && buffered.read() == 0x8b
                buffered.reset()
                val source = if (gzip) GZIPInputStream(buffered) else buffered
                source.use { stream ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    var total = 0L
                    output.outputStream().use { target ->
                        val buffer = ByteArray(8192)
                        var count = stream.read(buffer)
                        while (count != -1) {
                            total += count
                            require(total <= maxBytes) { "数据库解压后过大" }
                            digest.update(buffer, 0, count)
                            target.write(buffer, 0, count)
                            count = stream.read(buffer)
                        }
                    }
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    actualHash = actual
                    require(acceptedSha256.any { it.isBlank() || actual.equals(it, ignoreCase = true) }) {
                        "数据库 SHA-256 校验失败，请选择内置版本或已验证新版的数据包"
                    }
                }
            }
            val header = ByteArray(16)
            val size = output.inputStream().use { it.read(header) }
            require(size == 16 && header.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))) {
                "请选择诗词数据库 .db 或 .db.gz 文件"
            }
            return actualHash
        } catch (error: Exception) {
            output.delete()
            throw error
        }
    }
}
