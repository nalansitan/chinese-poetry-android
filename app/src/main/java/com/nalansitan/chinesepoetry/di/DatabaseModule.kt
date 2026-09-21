package com.nalansitan.chinesepoetry.di

import android.content.Context
import com.nalansitan.chinesepoetry.data.local.dao.AuthorDao
import com.nalansitan.chinesepoetry.data.local.dao.PoemDao
import com.nalansitan.chinesepoetry.data.local.dao.UserActivityDao
import com.nalansitan.chinesepoetry.data.local.database.DatabaseCompatibility
import com.nalansitan.chinesepoetry.data.local.database.PoetryDatabase
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PoetryDatabase {
        val dbFile = context.getDatabasePath(PoetryDatabase.DATABASE_NAME)
        if (dbFile.exists() && dbFile.length() > 0) {
            DatabaseCompatibility.patchDatabaseForRoom(dbFile)
        }
        return PoetryDatabase.getInstance(context)
    }

    @Provides
    fun providePoemDao(database: PoetryDatabase): PoemDao {
        return database.poemDao()
    }

    @Provides
    fun provideAuthorDao(database: PoetryDatabase): AuthorDao {
        return database.authorDao()
    }

    @Provides
    fun provideUserActivityDao(database: PoetryDatabase): UserActivityDao {
        return database.userActivityDao()
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }
}
