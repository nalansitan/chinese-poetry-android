package com.nalansitan.chinesepoetry.data.local.database

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * 兼容服务端生成的 SQLite 数据库，使其在 Room 打开前就对齐客户端 schema。
 */
object DatabaseCompatibility {

    private const val TAG = "DatabaseCompatibility"
    private const val COMPAT_VERSION = 1

    fun patchDatabaseForRoom(dbFile: File) {
        if (!dbFile.exists() || dbFile.length() <= 0) return
        val markerFile = markerFile(dbFile)
        if (markerFile.exists()) {
            return
        }

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            db.beginTransaction()
            rebuildPoemsTable(db)
            rebuildAuthorsTable(db)
            rebuildUserActivitiesTable(db)
            db.version = 1
            db.setTransactionSuccessful()
            markerFile.writeText(COMPAT_VERSION.toString())
            Log.i(TAG, "Database patched successfully for Room compatibility")
        } catch (e: Exception) {
            markerFile.delete()
            Log.e(TAG, "Failed to patch database", e)
            throw IllegalStateException("数据库兼容处理失败", e)
        } finally {
            db?.endTransactionSafely()
            db?.close()
        }
    }

    fun clearPatchMarker(dbFile: File) {
        markerFile(dbFile).delete()
    }

    private fun rebuildPoemsTable(db: SQLiteDatabase) {
        if (!tableExists(db, "poems")) return

        val columns = getColumns(db, "poems")

        db.execSQL("DROP TABLE IF EXISTS poems_room_new")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS poems_room_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                author_name TEXT NOT NULL,
                author_id TEXT,
                dynasty TEXT NOT NULL,
                content TEXT NOT NULL,
                type TEXT NOT NULL,
                rhythmic TEXT,
                chapter TEXT,
                section TEXT,
                comment TEXT,
                appreciation TEXT,
                notes TEXT,
                translation TEXT,
                is_favorite INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        val selectSql = """
            INSERT OR REPLACE INTO poems_room_new (
                id, title, author_name, author_id, dynasty, content, type,
                rhythmic, chapter, section, comment, appreciation, notes,
                translation, is_favorite, created_at
            )
            SELECT
                ${requiredText(columns, "id", "rowid")},
                ${requiredText(columns, "title", "''")},
                ${requiredText(columns, "author_name", "'佚名'")},
                ${optionalText(columns, "author_id")},
                ${requiredText(columns, "dynasty", "'unknown'")},
                ${requiredText(columns, "content", "''")},
                ${requiredText(columns, "type", "'tang_shi'")},
                ${optionalText(columns, "rhythmic")},
                ${optionalText(columns, "chapter")},
                ${optionalText(columns, "section")},
                ${optionalText(columns, "comment")},
                ${optionalText(columns, "appreciation")},
                ${optionalText(columns, "notes")},
                ${optionalText(columns, "translation")},
                ${requiredInteger(columns, "is_favorite", "0")},
                ${requiredInteger(columns, "created_at", "0")}
            FROM poems
        """.trimIndent()

        db.execSQL(selectSql)
        db.execSQL("DROP TABLE poems")
        db.execSQL("ALTER TABLE poems_room_new RENAME TO poems")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_poems_author_name` ON `poems` (`author_name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_poems_dynasty` ON `poems` (`dynasty`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_poems_type` ON `poems` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_poems_is_favorite` ON `poems` (`is_favorite`)")
    }

    private fun rebuildAuthorsTable(db: SQLiteDatabase) {
        if (!tableExists(db, "authors")) return

        val columns = getColumns(db, "authors")

        db.execSQL("DROP TABLE IF EXISTS authors_room_new")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS authors_room_new (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                dynasty TEXT NOT NULL,
                intro TEXT,
                short_intro TEXT,
                poem_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        val selectSql = """
            INSERT OR REPLACE INTO authors_room_new (
                id, name, dynasty, intro, short_intro, poem_count
            )
            SELECT
                ${requiredText(columns, "id", "rowid")},
                ${requiredText(columns, "name", "'佚名'")},
                ${requiredText(columns, "dynasty", "'unknown'")},
                ${optionalText(columns, "intro")},
                ${optionalText(columns, "short_intro")},
                ${requiredInteger(columns, "poem_count", "0")}
            FROM authors
        """.trimIndent()

        db.execSQL(selectSql)
        db.execSQL("DROP TABLE authors")
        db.execSQL("ALTER TABLE authors_room_new RENAME TO authors")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_authors_name` ON `authors` (`name`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_authors_dynasty` ON `authors` (`dynasty`)")
    }

    private fun rebuildUserActivitiesTable(db: SQLiteDatabase) {
        val columns = getColumns(db, "user_activities")

        db.execSQL("DROP TABLE IF EXISTS user_activities_room_new")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_activities_room_new (
                id TEXT NOT NULL PRIMARY KEY,
                poem_id TEXT NOT NULL,
                type TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY (poem_id) REFERENCES poems(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        if (columns.isNotEmpty()) {
            val selectSql = """
                INSERT OR REPLACE INTO user_activities_room_new (
                    id, poem_id, type, timestamp
                )
                SELECT
                    ${requiredText(columns, "id", "printf('legacy_%s', rowid)")},
                    ${requiredText(columns, "poem_id", "''")},
                    ${requiredText(columns, "type", "'history'")},
                    ${requiredInteger(columns, "timestamp", "0")}
                FROM user_activities
                WHERE ${existingOrLiteral(columns, "poem_id", "''")} IN (SELECT id FROM poems)
            """.trimIndent()
            db.execSQL(selectSql)
            db.execSQL("DROP TABLE user_activities")
        }

        db.execSQL("ALTER TABLE user_activities_room_new RENAME TO user_activities")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_activities_poem_id` ON `user_activities` (`poem_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_activities_type` ON `user_activities` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_activities_timestamp` ON `user_activities` (`timestamp`)")
    }

    private fun tableExists(db: SQLiteDatabase, tableName: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun getColumns(db: SQLiteDatabase, tableName: String): Set<String> {
        if (!tableExists(db, tableName)) return emptySet()

        val columns = mutableSetOf<String>()
        db.rawQuery("PRAGMA table_info(`$tableName`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        return columns
    }

    private fun requiredText(columns: Set<String>, name: String, fallback: String): String {
        return if (name in columns) "COALESCE(CAST(`$name` AS TEXT), $fallback)" else fallback
    }

    private fun optionalText(columns: Set<String>, name: String): String {
        return if (name in columns) "CAST(`$name` AS TEXT)" else "NULL"
    }

    private fun requiredInteger(columns: Set<String>, name: String, fallback: String): String {
        return if (name in columns) "COALESCE(CAST(`$name` AS INTEGER), $fallback)" else fallback
    }

    private fun existingOrLiteral(columns: Set<String>, name: String, fallback: String): String {
        return if (name in columns) "`$name`" else fallback
    }

    private fun SQLiteDatabase.endTransactionSafely() {
        if (inTransaction()) {
            endTransaction()
        }
    }

    private inline fun <T> Cursor.use(block: (Cursor) -> T): T {
        return try {
            block(this)
        } finally {
            close()
        }
    }

    private fun markerFile(dbFile: File): File {
        return File(dbFile.parentFile, "${dbFile.name}.room_compat_v$COMPAT_VERSION")
    }
}
