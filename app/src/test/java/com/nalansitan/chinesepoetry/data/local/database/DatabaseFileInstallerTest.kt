package com.nalansitan.chinesepoetry.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DatabaseFileInstallerTest {
    @Test
    fun `invalid download does not replace current database`() {
        val directory = Files.createTempDirectory("poetry-db-test").toFile()
        val target = File(directory, "poetry.db").apply { writeText("existing") }
        val downloaded = File(directory, "download.tmp").apply { writeText("not sqlite") }

        assertFalse(DatabaseFileInstaller.install(downloaded, target, downloaded.length()))
        assertEquals("existing", target.readText())
    }

    @Test
    fun `valid sqlite download atomically replaces current database`() {
        val directory = Files.createTempDirectory("poetry-db-test").toFile()
        val target = File(directory, "poetry.db").apply { writeText("existing") }
        val payload = "SQLite format 3\u0000".toByteArray() + ByteArray(128)
        val downloaded = File(directory, "download.tmp").apply { writeBytes(payload) }

        assertTrue(DatabaseFileInstaller.install(downloaded, target, payload.size.toLong()))
        assertTrue(target.readBytes().contentEquals(payload))
        assertFalse(downloaded.exists())
    }

    @Test
    fun `failed installed database validation restores current database`() {
        val directory = Files.createTempDirectory("poetry-db-test").toFile()
        val target = File(directory, "poetry.db").apply { writeText("existing") }
        val payload = "SQLite format 3\u0000".toByteArray() + ByteArray(128)
        val downloaded = File(directory, "download.tmp").apply { writeBytes(payload) }

        assertFalse(DatabaseFileInstaller.install(downloaded, target, payload.size.toLong()) {
            error("schema mismatch")
        })
        assertEquals("existing", target.readText())
    }

    @Test
    fun `orphaned backup is restored when target is missing`() {
        val directory = Files.createTempDirectory("poetry-db-test").toFile()
        val target = File(directory, "poetry.db")
        File(directory, "poetry.db.backup").writeText("existing")

        assertTrue(DatabaseFileInstaller.recoverInterruptedInstall(target))
        assertEquals("existing", target.readText())
    }

    @Test
    fun `uncommitted target is replaced by surviving backup`() {
        val directory = Files.createTempDirectory("poetry-db-test").toFile()
        val target = File(directory, "poetry.db").apply { writeText("unverified") }
        File(directory, "poetry.db.backup").writeText("existing")

        assertTrue(DatabaseFileInstaller.recoverInterruptedInstall(target))
        assertEquals("existing", target.readText())
        assertFalse(File(directory, "poetry.db.backup").exists())
    }
}
