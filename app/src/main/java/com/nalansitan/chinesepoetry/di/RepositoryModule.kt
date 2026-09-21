package com.nalansitan.chinesepoetry.di

import com.nalansitan.chinesepoetry.data.repository.AuthorRepositoryImpl
import com.nalansitan.chinesepoetry.data.repository.PoemRepositoryImpl
import com.nalansitan.chinesepoetry.domain.repository.AuthorRepository
import com.nalansitan.chinesepoetry.domain.repository.PoemRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 仓库依赖注入模块
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPoemRepository(
        impl: PoemRepositoryImpl
    ): PoemRepository

    @Binds
    @Singleton
    abstract fun bindAuthorRepository(
        impl: AuthorRepositoryImpl
    ): AuthorRepository
}
