package com.nalansitan.chinesepoetry.data.local.database

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DatabasePackageTest {
    @get:Rule val temporary = TemporaryFolder()
    private val database = "SQLite format 3\u0000test payload".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-256").digest(database)
        .joinToString("") { "%02x".format(it) }

    private fun gzip(): ByteArray = ByteArrayOutputStream().also { output ->
        GZIPOutputStream(output).use { it.write(database) }
    }.toByteArray()

    @Test fun importsRawAndGzipWithTheSameDatabaseHash() {
        for (bytes in listOf(database, gzip())) {
            val output = temporary.newFile()
            DatabasePackage.unpack(ByteArrayInputStream(bytes), output, hash)
            assertArrayEquals(database, output.readBytes())
        }
    }

    @Test fun rejectsWrongVersionAndDeletesTemporaryOutput() {
        val output = temporary.newFile()
        assertThrows(IllegalArgumentException::class.java) {
            DatabasePackage.unpack(ByteArrayInputStream(gzip()), output, "0".repeat(64))
        }
        assertFalse(output.exists())
    }

    @Test fun rejectsTruncatedGzip() {
        val output = temporary.newFile()
        assertThrows(java.io.IOException::class.java) {
            DatabasePackage.unpack(ByteArrayInputStream(gzip().dropLast(5).toByteArray()), output, hash)
        }
        assertFalse(output.exists())
    }

    @Test fun rejectsNonDatabaseEvenWithoutAPinnedHash() {
        val output = temporary.newFile()
        assertThrows(IllegalArgumentException::class.java) {
            DatabasePackage.unpack(ByteArrayInputStream("<html>error</html>".toByteArray()), output, "")
        }
        assertFalse(output.exists())
    }

    @Test fun limitsDecompressedSize() {
        val output = temporary.newFile()
        assertThrows(IllegalArgumentException::class.java) {
            DatabasePackage.unpack(ByteArrayInputStream(gzip()), output, hash, 16)
        }
        assertFalse(output.exists())
    }
    @Test fun importsPreviouslyVerifiedNewerPackageAndReturnsItsHash() {
        val output = temporary.newFile()
        assertEquals(hash, DatabasePackage.unpack(ByteArrayInputStream(gzip()), output,
            "0".repeat(64), acceptedSha256 = setOf("0".repeat(64), hash)))
        assertArrayEquals(database, output.readBytes())
    }

    @Test fun cannotImportPackageOutsideTrustedHashes() {
        val output = temporary.newFile()
        assertThrows(IllegalArgumentException::class.java) {
            DatabasePackage.unpack(ByteArrayInputStream(gzip()), output,
                "0".repeat(64), acceptedSha256 = setOf("0".repeat(64), "1".repeat(64)))
        }
        assertFalse(output.exists())
    }

}
