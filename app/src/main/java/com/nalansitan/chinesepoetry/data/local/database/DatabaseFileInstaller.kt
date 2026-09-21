package com.nalansitan.chinesepoetry.data.local.database

import java.io.File

/** 安全校验并替换数据库文件，失败时保留或恢复原数据库。 */
object DatabaseFileInstaller {
    private val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /** 恢复在“旧库已备份、新库尚未就位”时被中断的安装。 */
    fun recoverInterruptedInstall(target: File): Boolean {
        val backup = File(target.parentFile, "${target.name}.backup")
        if (!backup.exists()) return true
        target.parentFile?.mkdirs()
        // backup 只会在安装事务提交（校验完成）后删除；只要它仍存在，
        // target 就可能是尚未完成校验的新库，保守恢复已知可用的旧库。
        if (target.exists() && !target.delete()) return false
        return backup.renameTo(target)
    }

    fun install(
        downloaded: File,
        target: File,
        expectedLength: Long = -1,
        validateInstalled: (File) -> Unit = {}
    ): Boolean {
        if (!isValidDownload(downloaded, expectedLength)) return false

        target.parentFile?.mkdirs()
        if (!recoverInterruptedInstall(target)) return false
        val backup = File(target.parentFile, "${target.name}.backup")
        if (backup.exists() && !backup.delete()) return false

        val hadTarget = target.exists()
        if (hadTarget && !target.renameTo(backup)) return false

        return try {
            if (!downloaded.renameTo(target)) error("无法安装下载的数据库")
            validateInstalled(target)
            backup.delete()
            true
        } catch (_: Exception) {
            target.delete()
            if (hadTarget) backup.renameTo(target)
            false
        }
    }

    private fun isValidDownload(file: File, expectedLength: Long): Boolean {
        if (!file.isFile || file.length() < sqliteHeader.size) return false
        if (expectedLength > 0 && file.length() != expectedLength) return false
        val actualHeader = file.inputStream().use { input ->
            ByteArray(sqliteHeader.size).also { buffer ->
                if (input.read(buffer) != buffer.size) return false
            }
        }
        return actualHeader.contentEquals(sqliteHeader)
    }
}
