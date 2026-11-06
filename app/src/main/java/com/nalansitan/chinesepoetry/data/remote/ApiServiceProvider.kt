package com.nalansitan.chinesepoetry.data.remote

import com.nalansitan.chinesepoetry.BuildConfig
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.remote.api.PoetryApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API 服务提供者
 * 支持动态配置服务器地址
 */
class ApiServiceProvider(
    private val dataStore: DataStoreManager
) {
    @Volatile
    private var currentApiService: PoetryApiService? = null

    @Volatile
    private var currentPublicApiService: PoetryApiService? = null
    
    /**
     * 获取当前配置的 API 服务
     */
    fun getApiService(): PoetryApiService {
        return currentApiService ?: createApiService()
    }

    fun getPublicApiService(): PoetryApiService {
        return currentPublicApiService ?: createPublicApiService()
    }
    
    /**
     * 重新创建 API 服务（配置变更后调用）
     */
    fun recreateApiService(): PoetryApiService {
        currentApiService = null
        currentPublicApiService = null
        return createApiService()
    }
    
    private fun createApiService(): PoetryApiService {
        // 读取用户配置
        val userUrl = runBlocking { dataStore.serverUrl.first() }
        
        // 使用有效配置
        val baseUrl = dataStore.getEffectiveServerUrl(userUrl)
        
        val okHttpClient = createPublicOkHttpClient()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val service = retrofit.create(PoetryApiService::class.java)
        currentApiService = service
        return service
    }

    private fun createPublicApiService(): PoetryApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(createPublicOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(PoetryApiService::class.java)
        currentPublicApiService = service
        return service
    }
    
    private fun createPublicOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    
    /**
     * 获取当前使用的服务器地址（调试用）
     */
    fun hasUpdateServer(): Boolean = getCurrentBaseUrl() != "https://example.com/"

    fun getCurrentBaseUrl(): String {
        val userUrl = runBlocking { dataStore.serverUrl.first() }
        return dataStore.getEffectiveServerUrl(userUrl)
    }
}
