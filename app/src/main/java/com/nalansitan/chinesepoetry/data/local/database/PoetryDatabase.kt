package com.nalansitan.chinesepoetry.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nalansitan.chinesepoetry.data.local.dao.AuthorDao
import com.nalansitan.chinesepoetry.data.local.dao.PoemDao
import com.nalansitan.chinesepoetry.data.local.dao.UserActivityDao
import com.nalansitan.chinesepoetry.data.local.entity.AuthorEntity
import com.nalansitan.chinesepoetry.data.local.entity.PoemEntity
import com.nalansitan.chinesepoetry.data.local.entity.UserActivityEntity

/**
 * 诗词数据库
 */
@Database(
    entities = [
        PoemEntity::class,
        AuthorEntity::class,
        UserActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PoetryDatabase : RoomDatabase() {

    abstract fun poemDao(): PoemDao
    abstract fun authorDao(): AuthorDao
    abstract fun userActivityDao(): UserActivityDao

    companion object {
        const val DATABASE_NAME = "poetry.db"
        const val ASSET_DB_PATH = "database/poetry.db"

        @Volatile
        private var INSTANCE: PoetryDatabase? = null

        fun getInstance(context: Context): PoetryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Callback 用于在数据库打开后补充服务器数据库缺失的列
         */
        private val openCallback = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                addMissingColumns(db)
            }
        }

        private fun addMissingColumns(db: SupportSQLiteDatabase) {
            try {
                val cursor = db.query("PRAGMA table_info(poems)")
                val columns = mutableSetOf<String>()
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    columns.add(cursor.getString(nameIndex))
                }
                cursor.close()

                if ("is_favorite" !in columns) {
                    db.execSQL("ALTER TABLE poems ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
                    Log.i("PoetryDatabase", "Added missing column: is_favorite")
                }
                if ("created_at" !in columns) {
                    db.execSQL("ALTER TABLE poems ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
                    Log.i("PoetryDatabase", "Added missing column: created_at")
                }

                // 确保索引存在
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poems_is_favorite` ON `poems` (`is_favorite`)")
            } catch (e: Exception) {
                Log.e("PoetryDatabase", "Failed to add missing columns", e)
            }
        }

        fun createFromAsset(context: Context): PoetryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PoetryDatabase::class.java,
                    DATABASE_NAME
                )
                    .createFromAsset(ASSET_DB_PATH)
                    .addCallback(openCallback)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PoetryDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PoetryDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(openCallback)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
