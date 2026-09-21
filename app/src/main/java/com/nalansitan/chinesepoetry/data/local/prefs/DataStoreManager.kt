package com.nalansitan.chinesepoetry.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

/**
 * DataStore 管理器
 * 用于存储应用设置和数据版本信息
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "poetry_prefs")

class DataStoreManager(private val context: Context) {

    private val securePreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "poetry_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    private val secureUsername = MutableStateFlow(securePreferences.getString("server_username", "").orEmpty())
    private val securePassword = MutableStateFlow(securePreferences.getString("server_password", "").orEmpty())

    companion object {
        // 数据版本
        val DATA_VERSION = intPreferencesKey("data_version")
        val DATA_VERSION_NAME = stringPreferencesKey("data_version_name")
        val LAST_CHECK_TIME = longPreferencesKey("last_check_time")
        val HAS_PENDING_UPDATE = intPreferencesKey("has_pending_update")
        val AVAILABLE_DATA_VERSION = intPreferencesKey("available_data_version")
        val AVAILABLE_DATA_VERSION_NAME = stringPreferencesKey("available_data_version_name")
        val AVAILABLE_CHANGELOG = stringPreferencesKey("available_changelog")
        val APP_LAST_CHECK_TIME = longPreferencesKey("app_last_check_time")
        val HAS_PENDING_APP_UPDATE = intPreferencesKey("has_pending_app_update")
        val AVAILABLE_APP_VERSION_CODE = intPreferencesKey("available_app_version_code")
        val AVAILABLE_APP_VERSION_NAME = stringPreferencesKey("available_app_version_name")
        val AVAILABLE_APP_CHANGELOG = stringPreferencesKey("available_app_changelog")
        val AVAILABLE_APP_DOWNLOAD_URL = stringPreferencesKey("available_app_download_url")
        val AVAILABLE_APP_FORCE_UPDATE = intPreferencesKey("available_app_force_update")
        
        // 用户设置
        val FONT_SIZE = intPreferencesKey("font_size") // 0=小, 1=中, 2=大
        val IS_VERTICAL_LAYOUT = intPreferencesKey("is_vertical_layout") // 0=横排, 1=竖排
        val IS_DARK_MODE = intPreferencesKey("is_dark_mode") // -1=跟随系统, 0=浅色, 1=深色
        
        // 服务器配置（用户可修改，覆盖 BuildConfig 默认值）
        val SERVER_URL = stringPreferencesKey("server_url")
        val SERVER_USERNAME = stringPreferencesKey("server_username")
        val SERVER_PASSWORD = stringPreferencesKey("server_password")
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
    }

    // ==================== 数据版本管理 ====================
    
    val dataVersion: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[DATA_VERSION] ?: 1 }
    
    val dataVersionName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DATA_VERSION_NAME] ?: "1.0.0" }
    
    val lastCheckTime: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_CHECK_TIME] ?: 0 }

    val hasPendingUpdate: Flow<Boolean> = context.dataStore.data
        .map { preferences -> (preferences[HAS_PENDING_UPDATE] ?: 0) == 1 }

    val availableDataVersion: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_DATA_VERSION] ?: 0 }

    val availableDataVersionName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_DATA_VERSION_NAME] ?: "" }

    val availableChangelog: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_CHANGELOG] ?: "" }

    val appLastCheckTime: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[APP_LAST_CHECK_TIME] ?: 0 }

    val hasPendingAppUpdate: Flow<Boolean> = context.dataStore.data
        .map { preferences -> (preferences[HAS_PENDING_APP_UPDATE] ?: 0) == 1 }

    val availableAppVersionCode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_APP_VERSION_CODE] ?: 0 }

    val availableAppVersionName: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_APP_VERSION_NAME] ?: "" }

    val availableAppChangelog: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_APP_CHANGELOG] ?: "" }

    val availableAppDownloadUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[AVAILABLE_APP_DOWNLOAD_URL] ?: "" }

    val availableAppForceUpdate: Flow<Boolean> = context.dataStore.data
        .map { preferences -> (preferences[AVAILABLE_APP_FORCE_UPDATE] ?: 0) == 1 }
    
    suspend fun updateDataVersion(version: Int, versionName: String) {
        context.dataStore.edit { preferences ->
            preferences[DATA_VERSION] = version
            preferences[DATA_VERSION_NAME] = versionName
        }
    }
    
    suspend fun updateLastCheckTime(time: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[LAST_CHECK_TIME] = time
        }
    }

    suspend fun setPendingUpdate(
        hasUpdate: Boolean,
        version: Int = 0,
        versionName: String = "",
        changelog: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[HAS_PENDING_UPDATE] = if (hasUpdate) 1 else 0
            if (hasUpdate) {
                preferences[AVAILABLE_DATA_VERSION] = version
                preferences[AVAILABLE_DATA_VERSION_NAME] = versionName
                preferences[AVAILABLE_CHANGELOG] = changelog
            } else {
                preferences.remove(AVAILABLE_DATA_VERSION)
                preferences.remove(AVAILABLE_DATA_VERSION_NAME)
                preferences.remove(AVAILABLE_CHANGELOG)
            }
        }
    }

    suspend fun updateAppLastCheckTime(time: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[APP_LAST_CHECK_TIME] = time
        }
    }

    suspend fun setPendingAppUpdate(
        hasUpdate: Boolean,
        versionCode: Int = 0,
        versionName: String = "",
        changelog: String = "",
        downloadUrl: String = "",
        forceUpdate: Boolean = false
    ) {
        context.dataStore.edit { preferences ->
            preferences[HAS_PENDING_APP_UPDATE] = if (hasUpdate) 1 else 0
            if (hasUpdate) {
                preferences[AVAILABLE_APP_VERSION_CODE] = versionCode
                preferences[AVAILABLE_APP_VERSION_NAME] = versionName
                preferences[AVAILABLE_APP_CHANGELOG] = changelog
                preferences[AVAILABLE_APP_DOWNLOAD_URL] = downloadUrl
                preferences[AVAILABLE_APP_FORCE_UPDATE] = if (forceUpdate) 1 else 0
            } else {
                preferences.remove(AVAILABLE_APP_VERSION_CODE)
                preferences.remove(AVAILABLE_APP_VERSION_NAME)
                preferences.remove(AVAILABLE_APP_CHANGELOG)
                preferences.remove(AVAILABLE_APP_DOWNLOAD_URL)
                preferences.remove(AVAILABLE_APP_FORCE_UPDATE)
            }
        }
    }
    
    // ==================== 用户设置 ====================
    
    val fontSize: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[FONT_SIZE] ?: 1 }
    
    val isVerticalLayout: Flow<Boolean> = context.dataStore.data
        .map { preferences -> (preferences[IS_VERTICAL_LAYOUT] ?: 0) == 1 }
    
    val isDarkMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[IS_DARK_MODE] ?: -1 }
    
    suspend fun setFontSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[FONT_SIZE] = size.coerceIn(0, 2)
        }
    }
    
    suspend fun setVerticalLayout(isVertical: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_VERTICAL_LAYOUT] = if (isVertical) 1 else 0
        }
    }
    
    suspend fun setDarkMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = mode.coerceIn(-1, 1)
        }
    }
    
    // ==================== 服务器配置 ====================
    
    val serverUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SERVER_URL] ?: "" }
    
    val serverUsername: Flow<String> = secureUsername
    
    val serverPassword: Flow<String> = securePassword

    val searchHistory: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[SEARCH_HISTORY] ?: return@map emptyList()
            try {
                val jsonArray = JSONArray(raw)
                buildList {
                    for (index in 0 until jsonArray.length()) {
                        add(jsonArray.optString(index))
                    }
                }.filter { it.isNotBlank() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    
    /**
     * 获取实际使用的服务器 URL（优先使用用户配置，否则使用 BuildConfig 默认值）
     */
    fun getEffectiveServerUrl(userUrl: String?): String {
        return if (!userUrl.isNullOrBlank()) userUrl else com.nalansitan.chinesepoetry.BuildConfig.BASE_URL
    }
    
    /**
     * 获取实际使用的认证信息（优先使用用户配置，否则使用 BuildConfig 默认值）
     */
    fun getEffectiveCredentials(userUsername: String?, userPassword: String?): Pair<String, String> {
        val username = if (!userUsername.isNullOrBlank()) userUsername else com.nalansitan.chinesepoetry.BuildConfig.API_USERNAME
        val password = if (!userPassword.isNullOrBlank()) userPassword else com.nalansitan.chinesepoetry.BuildConfig.API_PASSWORD
        return username to password
    }
    
    suspend fun setServerConfig(url: String, username: String, password: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
            preferences.remove(SERVER_USERNAME)
            preferences.remove(SERVER_PASSWORD)
        }
        securePreferences.edit()
            .putString("server_username", username)
            .putString("server_password", password)
            .apply()
        secureUsername.value = username
        securePassword.value = password
    }
    
    suspend fun clearServerConfig() {
        context.dataStore.edit { preferences ->
            preferences.remove(SERVER_URL)
            preferences.remove(SERVER_USERNAME)
            preferences.remove(SERVER_PASSWORD)
        }
        securePreferences.edit().clear().apply()
        secureUsername.value = ""
        securePassword.value = ""
    }

    suspend fun setSearchHistory(history: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[SEARCH_HISTORY] = JSONArray(history).toString()
        }
    }

    suspend fun clearSearchHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY)
        }
    }
}
