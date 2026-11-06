package com.nalansitan.chinesepoetry.data.local.database

import java.io.File

/** 在现有 Room 数据库内合并内容，不替换数据库文件，也不删除用户记录。 */
object DatabaseContentUpdater {
    private val poemColumns = listOf("title", "author_name", "author_id", "dynasty", "content", "type",
        "rhythmic", "chapter", "section", "comment", "appreciation", "notes", "translation")
    private val authorColumns = listOf("name", "dynasty", "intro", "short_intro", "poem_count")

    internal fun mergeStatements(): List<String> = listOf("authors" to authorColumns, "poems" to poemColumns).flatMap { (table, columns) ->
        listOf(
            "UPDATE $table SET " + columns.joinToString(", ") {
                "$it = (SELECT incoming.$table.$it FROM incoming.$table WHERE incoming.$table.id = $table.id)"
            } + " WHERE id IN (SELECT id FROM incoming.$table)",
            "INSERT INTO $table (id, ${columns.joinToString()}) SELECT id, ${columns.joinToString()} FROM incoming.$table WHERE id NOT IN (SELECT id FROM main.$table)"
        )
    }

    fun currentVersion(database: PoetryDatabase, fallback: Long): Long {
        val db = database.openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS installed_data_release (version INTEGER NOT NULL)")
        return db.query("SELECT version FROM installed_data_release LIMIT 1").use {
            if (it.moveToFirst()) it.getLong(0) else fallback
        }
    }

    fun merge(database: PoetryDatabase, incoming: File, version: Long) {
        val db = database.openHelper.writableDatabase
        db.execSQL("ATTACH DATABASE ? AS incoming", arrayOf(incoming.absolutePath))
        try {
            database.runInTransaction {
                mergeStatements().forEach { db.execSQL(it) }
                db.execSQL("CREATE TABLE IF NOT EXISTS installed_data_release (version INTEGER NOT NULL)")
                db.execSQL("DELETE FROM installed_data_release")
                db.execSQL("INSERT INTO installed_data_release(version) VALUES (?)", arrayOf(version))
            }
        } finally {
            db.execSQL("DETACH DATABASE incoming")
        }
    }
}
