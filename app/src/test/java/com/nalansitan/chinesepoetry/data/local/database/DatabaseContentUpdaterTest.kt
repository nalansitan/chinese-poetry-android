package com.nalansitan.chinesepoetry.data.local.database

import org.junit.Assert.*
import org.junit.Test
import java.sql.DriverManager

class DatabaseContentUpdaterTest {
    private fun fixture(test: (java.sql.Connection) -> Unit) {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { sql ->
                sql.execute("PRAGMA foreign_keys=ON")
                sql.execute("ATTACH DATABASE ':memory:' AS incoming")
                for (schema in listOf("main", "incoming")) {
                    sql.execute("CREATE TABLE $schema.poems(id TEXT PRIMARY KEY, title TEXT NOT NULL, author_name TEXT, author_id TEXT, dynasty TEXT, content TEXT, type TEXT, rhythmic TEXT, chapter TEXT, section TEXT, comment TEXT, appreciation TEXT, notes TEXT, translation TEXT, is_favorite INTEGER DEFAULT 0, created_at INTEGER DEFAULT 0)")
                    sql.execute("CREATE TABLE $schema.authors(id TEXT PRIMARY KEY, name TEXT, dynasty TEXT, intro TEXT, short_intro TEXT, poem_count INTEGER)")
                }
                sql.execute("CREATE TABLE user_activities(id TEXT PRIMARY KEY, poem_id TEXT REFERENCES poems(id) ON DELETE CASCADE)")
                sql.execute("INSERT INTO poems(id,title,is_favorite,created_at) VALUES ('p','old',1,123), ('removed','kept',1,456)")
                sql.execute("INSERT INTO user_activities VALUES ('history','p'), ('favorite','removed')")
                sql.execute("INSERT INTO incoming.poems(id,title,is_favorite) VALUES ('p','corrected',0), ('new','added',1)")
                sql.execute("INSERT INTO incoming.authors(id,name) VALUES ('a','author')")
            }
            test(connection)
        }
    }

    @Test fun mergesContentWithoutDeletingFavoritesOrHistory() = fixture { connection ->
        connection.autoCommit = false
        connection.createStatement().use { sql ->
            DatabaseContentUpdater.mergeStatements().forEach { sql.execute(it) }
            connection.commit()
            sql.executeQuery("SELECT title,is_favorite,created_at FROM poems WHERE id='p'").use {
                assertTrue(it.next()); assertEquals("corrected", it.getString(1))
                assertEquals(1, it.getInt(2)); assertEquals(123, it.getInt(3))
            }
            sql.executeQuery("SELECT count(*) FROM user_activities").use { it.next(); assertEquals(2, it.getInt(1)) }
            sql.executeQuery("SELECT is_favorite FROM poems WHERE id='new'").use { it.next(); assertEquals(0, it.getInt(1)) }
            sql.executeQuery("SELECT count(*) FROM poems").use { it.next(); assertEquals(3, it.getInt(1)) }
        }
    }

    @Test fun failedMergeRollsBackContentAndUserRecords() = fixture { connection ->
        connection.autoCommit = false
        connection.createStatement().use { sql ->
            try {
                DatabaseContentUpdater.mergeStatements().forEach { sql.execute(it) }
                sql.execute("INSERT INTO poems(id,title) VALUES ('invalid',NULL)")
                fail("Expected constraint failure")
            } catch (_: java.sql.SQLException) {
                connection.rollback()
            }
            sql.executeQuery("SELECT title FROM poems WHERE id='p'").use { it.next(); assertEquals("old", it.getString(1)) }
            sql.executeQuery("SELECT count(*) FROM user_activities").use { it.next(); assertEquals(2, it.getInt(1)) }
            sql.executeQuery("SELECT count(*) FROM poems WHERE id='new'").use { it.next(); assertEquals(0, it.getInt(1)) }
        }
    }
}
