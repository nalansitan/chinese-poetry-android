package com.nalansitan.chinesepoetry.di

import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.remote.ApiServiceProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 网络模块 - 提供 Retrofit 和 API 服务
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideApiServiceProvider(dataStore: DataStoreManager): ApiServiceProvider {
        return ApiServiceProvider(dataStore)
    }
}
