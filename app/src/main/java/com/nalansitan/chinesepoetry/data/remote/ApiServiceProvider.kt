package com.nalansitan.chinesepoetry.data.remote

import com.nalansitan.chinesepoetry.BuildConfig
import com.nalansitan.chinesepoetry.data.local.prefs.DataStoreManager
import com.nalansitan.chinesepoetry.data.remote.api.PoetryApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API 服务提供者
 * 支持动态配置服务器地址和认证信息
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
        val userUsername = runBlocking { dataStore.serverUsername.first() }
        val userPassword = runBlocking { dataStore.serverPassword.first() }
        
        // 使用有效配置
        val baseUrl = dataStore.getEffectiveServerUrl(userUrl)
        val (username, password) = dataStore.getEffectiveCredentials(userUsername, userPassword)
        
        val okHttpClient = createOkHttpClient(username, password)
        
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
        val userUrl = runBlocking { dataStore.serverUrl.first() }
        val baseUrl = dataStore.getEffectiveServerUrl(userUrl)

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createPublicOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(PoetryApiService::class.java)
        currentPublicApiService = service
        return service
    }
    
    private fun createOkHttpClient(username: String, password: String): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (BuildConfig.DEBUG) {
                // 使用 HEADERS 级别，避免 BODY 级别把大文件全部缓存到内存（破坏 @Streaming）
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        
        val authInterceptor = okhttp3.Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (username.isNotBlank() || password.isNotBlank()) {
                requestBuilder.header("Authorization", Credentials.basic(username, password))
            }
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.MINUTES)  // 读取超时放宽，支持大文件下载
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
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
    fun getCurrentBaseUrl(): String {
        val userUrl = runBlocking { dataStore.serverUrl.first() }
        return dataStore.getEffectiveServerUrl(userUrl)
    }
}
